package tamestaj.examples.vector.test

import tamestaj.examples.vector.Vec

trait Base {
  type Rep[+T]
}

trait BaseExp extends Base {
  type Rep[+T] = Exp[T]
  trait Exp[+T]
}

trait VecLExp {
  type Rep[+T]
  def   lit( v: Vec): Rep[Vec]
  def  plus(v1: Rep[Vec], v2: Rep[Vec]): Rep[Vec]
  def times( v: Rep[Vec],  s: Rep[Double]): Rep[Vec]
}

trait ArithExp {
  type Rep[+T]
  def add(a: Rep[Double], b: Rep[Double]): Rep[Double]
  def mul(a: Rep[Double], b: Rep[Double]): Rep[Double]
  def con(d: Double): Rep[Double]
}

trait ProgramExp { this: VecLExp with ArithExp =>
  val v1 = Vec.create(1, 2)
  val v2 = Vec.create(3, 4)
  val v3 = Vec.create(5, 6)

  def run() = plus(plus(lit(v1), times(lit(v2), mul(con(1), con(3)))), lit(v3))
}

trait ArithExpEval extends ArithExp with BaseExp {
  case class Add(a: Rep[Double], b: Rep[Double]) extends Exp[Double]
  case class Mul(a: Rep[Double], b: Rep[Double]) extends Exp[Double]
  case class Con(d: Double) extends Exp[Double]

  def add(a: Rep[Double], b: Rep[Double]): Rep[Double] = Add(a, b)
  def mul(a: Rep[Double], b: Rep[Double]): Rep[Double] = Mul(a, b)
  def con(d: Double): Rep[Double] = Con(d)
}

trait EvalExp extends VecLExp with BaseExp {
  case class Lit(v: Vec) extends Exp[Vec]
  case class Plus(v1: Rep[Vec], v2: Rep[Vec]) extends Exp[Vec]
  case class Times( v: Rep[Vec],  s: Rep[Double]) extends Exp[Vec]

  def   lit( v: Vec)                       = Lit(v)
  def  plus(v1: Rep[Vec], v2: Rep[Vec])    = Plus(v1, v2)
  def times( v: Rep[Vec],  s: Rep[Double]) = {
    this match {
      case x: ArithExpEval =>
        if (s.isInstanceOf[x.Mul]) {
          Times(v, x.con(13).asInstanceOf[Exp[Double]])
        } else {
          Times(v, s)
        }
      case _ =>
        Times(v, s)
    }
  }

}

object ExpMain {
  def main(args: Array[String]) = {
    println(new ProgramExp with EvalExp with ArithExpEval {}.run())
  }
}