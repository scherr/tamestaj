package tamestaj.examples.vector.test

import tamestaj.examples.vector.Vec

trait VecL {
  type Rep[+T]
  def   lit( v: Vec): Rep[Vec]
  def  plus(v1: Rep[Vec], v2: Rep[Vec]): Rep[Vec]
  def times( v: Rep[Vec],  s: Double): Rep[Vec]
}

trait Program { this: VecL =>
  val v1 = Vec.create(1, 2)
  val v2 = Vec.create(3, 4)
  val v3 = Vec.create(5, 6)

  def run() = plus(plus(lit(v1), times(lit(v2), 2)), lit(v3))
}

trait Eval extends VecL {
  type Rep[+T] = Vec
  def   lit( v: Vec)             = v
  def  plus(v1: Vec, v2: Vec)    = v1.plus(v2)
  def times( v: Vec,  s: Double) = v.times(s)
}

object Main {
  def main(args: Array[String]) = {
    println(new Program with Eval {}.run())
  }
}