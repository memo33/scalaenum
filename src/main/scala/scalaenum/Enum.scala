/* Based on Enumeration.scala from Scala library, v2.11.2.
 * See http://scala-lang.org (released under BSD 3-Clause License)
 */

package io.github.memo33.scalaenum

import scala.collection.{ mutable, immutable, generic, SortedSetLike, AbstractSet }
import java.lang.reflect.{ Modifier, Method => JMethod, Field => JField }
import scala.reflect.NameTransformer._
import scala.util.matching.Regex

/** Defines a finite set of values specific to the enumeration. Typically
 *  these values enumerate all possible forms something can take and provide
 *  a lightweight alternative to case classes.
 *
 *  Each call to a `Value` method adds a new unique value to the enumeration.
 *  To be accessible, these values are usually defined as `val` members of
 *  the evaluation.
 *
 *  All values in an enumeration share a common, unique type defined as the
 *  abstract `Value` type member of the enumeration (`Value` selected on the
 *  stable identifier path of the enumeration instance).
 *  Besides, in contrast to Scala's built-in Enumeration, the `Value` type
 *  member can be extended in subclasses, such that it is possible to mix-in
 *  traits, for example. In this case, make sure to make the constructor of
 *  `Value` private. Example:
 *
 *  {{{
 *  // adding methods to Value
 *  class Day private extends Day.Val {
 *    def isWorkingDay: Boolean = this != Day.Saturday && this != Day.Sunday
 *  }
 *  object Day extends Enum {
 *    type Value = Day
 *    val Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday = new Day
 *  }
 *
 *  // usage:
 *  Day.values filter (_.isWorkingDay) foreach println
 *  // output:
 *  // Monday
 *  // Tuesday
 *  // Wednesday
 *  // Thursday
 *  // Friday
 *  }}}
 *
 *  @param initial The initial value from which to count the integers that
 *                 identifies values at run-time.
 *  @author  Matthias Zenger
 */
@SerialVersionUID(504146442575237004L)
abstract class Enum (initial: Int) extends Serializable {
  thisenum =>

  def this() = this(0)

  /* Note that `readResolve` cannot be private, since otherwise
     the JVM does not invoke it when deserializing subclasses. */
  protected def readResolve(): AnyRef = thisenum.getClass.getField(MODULE_INSTANCE_NAME).get(null)

  /** The name of this enumeration.
   */
  override def toString =
    ((getClass.getName stripSuffix MODULE_SUFFIX_STRING split '.').last split
       Regex.quote(NAME_JOIN_STRING)).last

  /** The mapping from the integer used to identify values to the actual
    * values. */
  private val vmap: mutable.Map[Int, Value] = new mutable.HashMap

  /** The cache listing all values of this enumeration. */
  @transient private var vset: ValueSet = null
  @transient @volatile private var vsetDefined = false

  /** The mapping from the integer used to identify values to their
    * names. */
  private val nmap: mutable.Map[Int, String] = new mutable.HashMap

  /** The values of this enumeration as a set.
   */
  def values: ValueSet = {
    if (!vsetDefined) {
      vset = (ValueSet.newBuilder ++= vmap.values).result()
      vsetDefined = true
    }
    vset
  }

  /** The integer to use to identify the next created value. */
  protected var nextId: Int = initial

  /** The string to use to name the next created value. */
  protected var nextName: Iterator[String] = _

  private def nextNameOrNull =
    if (nextName != null && nextName.hasNext) nextName.next() else null

  /** The highest integer amongst those used to identify values in this
    * enumeration. */
  private var topId = initial

  /** The lowest integer amongst those used to identify values in this
    * enumeration, but no higher than 0. */
  private var bottomId = if(initial < 0) initial else 0

  /** The one higher than the highest integer amongst those used to identify
    *  values in this enumeration. */
  final def maxId = topId

  /** The value of this enumeration with given id `x`
   */
  final def apply(x: Int): Value = vmap(x)

  /** Return a `Value` from this `Enum` whose name matches
   *  the argument `s`.  The names are determined automatically via reflection.
   *
   * @param  s an `Enum` name
   * @return   the `Value` of this `Enum` if its name matches `s`
   * @throws   NoSuchElementException if no `Value` with a matching
   *           name is in this `Enum`
   */
  final def withName(s: String): Value = values.find(_.toString == s).get

  private def populateNameMap() {
    val fields = getClass.getDeclaredFields
    def isValDef(m: JMethod) = fields exists (fd => fd.getName == m.getName && fd.getType == m.getReturnType)

    // The list of possible Value methods: 0-args which return a conforming type
    val methods = getClass.getMethods filter (m => m.getParameterTypes.isEmpty &&
                                                   classOf[Val].isAssignableFrom(m.getReturnType) &&
                                                   m.getDeclaringClass != classOf[Enum] &&
                                                   isValDef(m))
    methods foreach { m =>
      val name = m.getName
      // invoke method to obtain actual `Value` instance
      val value = m.invoke(this).asInstanceOf[Value]
      // verify that outer points to the correct Enum: ticket #3616.
      if (value.outerEnum eq thisenum) {
        val id = Int.unbox(classOf[Val] getMethod "id" invoke value)
        nmap += ((id, name))
      }
    }
  }

  /* Obtains the name for the value with id `i`. If no name is cached
   * in `nmap`, it populates `nmap` using reflection.
   */
  private def nameOf(i: Int): String = synchronized { nmap.getOrElse(i, { populateNameMap() ; nmap(i) }) }

  /** The type of the enumerated values. */
  type Value <: Val

  /** The superclass of the `scala.Enum.Value` type. This class
   *  can be overridden to change the enumeration's naming and integer
   *  identification behaviour, as well as to add additional public
   *  functionality.
   */
  @SerialVersionUID(0 - 5747769270401950006L)
  class Val protected (i: Int, name: String) extends Ordered[Value] with Serializable { this: Value =>
    protected def this(i: Int)       = this(i, nextNameOrNull)
    protected def this(name: String) = this(nextId, name)
    protected def this()             = this(nextId)

    assert(!vmap.isDefinedAt(i), "Duplicate id: " + i)
    vmap(i) = this
    vsetDefined = false
    nextId = i + 1
    if (nextId > topId) topId = nextId
    if (i < bottomId) bottomId = i
    /** the id and bit location of this enumeration value */
    def id: Int = i
    /** a marker so we can tell whose values belong to whom come reflective-naming time */
    private[Enum] val outerEnum = thisenum

    override def compare(that: Value): Int =
      if (this.id < that.id) -1
      else if (this.id == that.id) 0
      else 1
    override def equals(other: Any) = other match {
      case that: Enum#Val  => (outerEnum eq that.outerEnum) && (id == that.id)
      case _               => false
    }
    override def hashCode: Int = id.##

    /** Create a ValueSet which contains this value and another one */
    def + (v: Value) = ValueSet(this, v)

    override def toString() =
      if (name != null) name
      else try thisenum.nameOf(i)
      catch { case _: NoSuchElementException => "<Invalid enum: no field for #" + i + ">" }

    protected def readResolve(): AnyRef = {
      val enum = thisenum.readResolve().asInstanceOf[Enum]
      if (enum.vmap == null) this
      else enum.vmap(i)
    }
  }

  /** An ordering by id for values of this set */
  implicit object ValueOrdering extends Ordering[Value] {
    def compare(x: Value, y: Value): Int = x compare y
  }

  /** A class for sets of values.
   *  Iterating through this set will yield values in increasing order of their ids.
   *
   *  @param nnIds The set of ids of values (adjusted so that the lowest value does
   *    not fall below zero), organized as a `BitSet`.
   */
  class ValueSet private[ValueSet] (private[this] var nnIds: immutable.BitSet)
  extends AbstractSet[Value]
     with immutable.SortedSet[Value]
     with SortedSetLike[Value, ValueSet]
     with Serializable {

    implicit def ordering: Ordering[Value] = ValueOrdering
    def rangeImpl(from: Option[Value], until: Option[Value]): ValueSet =
      new ValueSet(nnIds.rangeImpl(from.map(_.id - bottomId), until.map(_.id - bottomId)))

    override def empty = ValueSet.empty
    def contains(v: Value) = nnIds contains (v.id - bottomId)
    def + (value: Value) = new ValueSet(nnIds + (value.id - bottomId))
    def - (value: Value) = new ValueSet(nnIds - (value.id - bottomId))
    def iterator = nnIds.iterator map (id => thisenum.apply(bottomId + id))
    override def keysIteratorFrom(start: Value) = nnIds keysIteratorFrom start.id  map (id => thisenum.apply(bottomId + id))
    override def stringPrefix = thisenum + ".ValueSet"
    /** Creates a bit mask for the zero-adjusted ids in this set as a
     *  new array of longs */
    def toBitMask: Array[Long] = nnIds.toBitMask
  }

  /** A factory object for value sets */
  object ValueSet {
    import generic.CanBuildFrom

    /** The empty value set */
    val empty = new ValueSet(immutable.BitSet.empty)
    /** A value set consisting of given elements */
    def apply(elems: Value*): ValueSet = (newBuilder ++= elems).result()
    /** A value set containing all the values for the zero-adjusted ids
     *  corresponding to the bits in an array */
    def fromBitMask(elems: Array[Long]): ValueSet = new ValueSet(immutable.BitSet.fromBitMask(elems))
    /** A builder object for value sets */
    def newBuilder: mutable.Builder[Value, ValueSet] = new mutable.Builder[Value, ValueSet] {
      private[this] val b = new mutable.BitSet
      def += (x: Value) = { b += (x.id - bottomId); this }
      def clear() = b.clear()
      def result() = new ValueSet(b.toImmutable)
    }
    /** The implicit builder for value sets */
    implicit def canBuildFrom: CanBuildFrom[ValueSet, Value, ValueSet] =
      new CanBuildFrom[ValueSet, Value, ValueSet] {
        def apply(from: ValueSet) = newBuilder
        def apply() = newBuilder
      }
  }
}
