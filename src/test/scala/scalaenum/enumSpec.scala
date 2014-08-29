package scalaenum

import org.scalatest.{WordSpec, Matchers}
import java.lang.reflect.{ Modifier, Method => JMethod, Field => JField }

object Examples {

  // basic use case
  object Color extends Enum {
    type Value = Val              // Value is abstract type in Enum
    val Red, Green, Blue = Value  // Value is method instantiating Val (only if Val =:= Value)
  }

  // adding methods to Value
  object Day extends Enum {
    class Value private[Day] extends Val {
      def isWorkingDay: Boolean = this != Saturday && this != Sunday
    }
    val Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday = new Value
  }

  object MathConstants extends Enum {
    case class Value private[MathConstants] (val value: Double) extends Val
    val Pi = Value(3.14)
    val E = Value(2.718)
    val Phi = Value(1.618)
  }

  object Planet extends Enum {
    type Planet = Value
    case class Value private[Planet] (mass: Double, radius: Double) extends Val {
      def surfaceGravity = G * mass / (radius * radius)
      def surfaceWeight(otherMass: Double) = otherMass * surfaceGravity
    }
    val G = 6.67300E-11 // universal gravitational constant  (m3 kg-1 s-2)

    val Mercury = Value(3.303e+23, 2.4397e6)
    val Venus   = Value(4.869e+24, 6.0518e6)
    val Earth   = Value(5.976e+24, 6.37814e6)
    val Mars    = Value(6.421e+23, 3.3972e6)
    val Jupiter = Value(1.9e+27,   7.1492e7)
    val Saturn  = Value(5.688e+26, 6.0268e7)
    val Uranus  = Value(8.686e+25, 2.5559e7)
    val Neptune = Value(1.024e+26, 2.4746e7)
  }

  // Java-style syntax
  object Operation extends Enum {
    abstract class Value private[Operation] extends Val {
      def eval(x: Double, y: Double): Double
    }
    val Plus   = new Value { def eval(x: Double, y: Double) = x + y }
    val Minus  = new Value { def eval(x: Double, y: Double) = x - y }
    val Times  = new Value { def eval(x: Double, y: Double) = x * y }
    val Divide = new Value { def eval(x: Double, y: Double) = x / y }
  }

  // the Scala way…
  object Operation2 extends Enum {
    class Value private[Operation2] (val eval: (Double, Double) => Double) extends Val
    val Plus   = new Value(_ + _)
    val Minus  = new Value(_ - _)
    val Times  = new Value(_ * _)
    val Divide = new Value(_ / _)
  }

  // solution for double-definition problem due to erasure
  object Foo extends Enum { class Value private[Foo] extends Val; val A, B = new Value }
  object Bar extends Enum { class Value private[Bar] extends Val; val A, B = new Value }
  def func(x: Foo.Value) = 1
  def func(x: Bar.Value) = 2  // wouldn't compile with 'type Value = Val'
}

class EnumSpec extends WordSpec with Matchers {
  import Examples._

  "Enum" should {
    "initialize all values" in {
      Color.values.size should be (3)
      Day.maxId should be (7)
    }
    "reflectively detect val assignments correctly" in {
      val fields = Color.getClass.getDeclaredFields
      def isValDef(m: JMethod) = fields exists (fd => fd.getName == m.getName && fd.getType == m.getReturnType)
      val methods = Color.getClass.getMethods filter { m =>
        m.getParameterTypes.isEmpty &&
        classOf[Color.Value].isAssignableFrom(m.getReturnType) &&
        m.getDeclaringClass != classOf[Enum] &&
        isValDef(m)
      }
      methods.map(_.getName).toSet should be (Set("Red", "Green", "Blue"))
    }
    "work as usual for withName and apply" in {
      Day.withName("Tuesday") should be (Day.Tuesday)
      Day.apply(1) should be (Day.Tuesday)
    }
    "allow proper access on Value methods" in {
      import Day._
      Day.values filter (_.isWorkingDay) should be (ValueSet(Monday, Tuesday, Wednesday, Thursday, Friday))
      Day.values filter (!_.isWorkingDay) should be (ValueSet(Saturday, Sunday))
      MathConstants.values filter (_.value < 2) should be (Set(MathConstants.Phi))

      val weDays = for (d <- Day.values if !d.isWorkingDay) yield d
      weDays.getClass should be (classOf[ValueSet])
      weDays should be (Set(Saturday, Sunday))
    }
  }

  object Foo extends Enumeration {
    val A, B = Value
  }
  object Bar extends Enumeration {
    class Val extends super.Val
    val A, B = new Val
    import scala.language.implicitConversions
    implicit def valueToVal(v: Value): Val = v.asInstanceOf[Val]
  }
  object Baz extends Enum {
    class Value private[Baz] extends Val
    val A, B = new Value
  }
  "Enum.map()" should {
    "be able to map ValueSets to Seqs" in {
      val foo: Seq[Foo.Value] = (Foo.A + Foo.B).map(identity)(scala.collection.breakOut)
      import Bar.valueToVal
      //val bar: Seq[Bar.Val] = (Bar.A + Bar.B).map(v => v)(scala.collection.breakOut)  // does not compile!
      val baz: Seq[Baz.Value] = (Baz.A + Baz.B).map(identity)(scala.collection.breakOut)
    }
  }

}
