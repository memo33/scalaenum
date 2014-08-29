scalaenum
=========

Provides a slightly modified version of `scala.Enumeration` that can easily be extended by a custom `Value`
implementation which can be accessed as part of the public interface.

Essentially, the difference to `scala.Enumeration` is as follows: The abstract class `Value` was replaced by an
abstract type member (subclass of `Val`) to be refined in subclasses, instead of `Val` being a subclass of `Value`.
This allows adding public methods to `Value`.


Examples
--------

Simple use case:

    class Day private extends Day.Val {
      def isWorkingDay: Boolean = this != Day.Saturday && this != Day.Sunday
    }
    object Day extends Enum {
      type Value = Day
      val Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday = new Day
    }

    // usage:
    Day.values filter (_.isWorkingDay) foreach println

Note that `Value` is an abstract type member that needs to be defined in any case.
Also make sure to declare the constructor of `Value` to be private in order to assert that no instances of Value are
constructed elsewhere. More examples:

    object MathConstants extends Enum {
      case class Value private[MathConstants] (val value: Double) extends Val
      val Pi = Value(3.14)
      val E = Value(2.718)
      val Phi = Value(1.618)
    }

    // usage:
    for (c <- MathConstants.values if c.value < 2) println(c.value) // prints 1.618

The infamous Planet example:

    case class Planet private (mass: Double, radius: Double) extends Planet.Val {
      def surfaceGravity = Planet.G * mass / (radius * radius)
      def surfaceWeight(otherMass: Double) = otherMass * surfaceGravity
    }
    object Planet extends Enum {
      type Value = Planet
      val G = 6.67300E-11 // universal gravitational constant  (m3 kg-1 s-2)

      val Mercury = Planet(3.303e+23, 2.4397e6)
      val Venus   = Planet(4.869e+24, 6.0518e6)
      val Earth   = Planet(5.976e+24, 6.37814e6)
      val Mars    = Planet(6.421e+23, 3.3972e6)
      val Jupiter = Planet(1.9e+27,   7.1492e7)
      val Saturn  = Planet(5.688e+26, 6.0268e7)
      val Uranus  = Planet(8.686e+25, 2.5559e7)
      val Neptune = Planet(1.024e+26, 2.4746e7)
    }

    // usage:
    val mass = 65 // kg
    for (p <- Planet.values) {
      println(f"Your weight on $p is ${p.surfaceWeight(mass)}%f N.")
    }

More Java Enum examples:

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

The following is a possible workaround for the double-definition problem due to erasure of `scala.Enumeration` (though,
this choice needs to be made at point of API design).

    object Foo extends Enum { class Value private[Foo] extends Val; val A, B = new Value }
    object Bar extends Enum { class Value private[Bar] extends Val; val A, B = new Value }
    def func(x: Foo.Value) = 1
    def func(x: Bar.Value) = 2  // wouldn't compile with 'type Value = Val'

A current limitation is that, if you want to define an `Enum` inside of a class (instead of an object or package),
you cannot define the `Value` class as companion of the `Enum` object, but it needs to be defined as inner class of the
`Enum` object. Otherwise, a StackOverflowError is thrown during initialization.
