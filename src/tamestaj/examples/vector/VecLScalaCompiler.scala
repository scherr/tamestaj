package tamestaj.examples.vector

import java.util

import tamestaj.Environment.Binder
import tamestaj.annotations.Suppress
import tamestaj.util.scalasupport.Expression
import tamestaj.util.scalasupport._
import Expression.{DoubleValue, ObjectValue, MethodInvocation}
import tamestaj._

// @Suppress(languages = Array(classOf[VecL]))
class VecLScalaCompiler(implicit binder: Binder) {
  case class PlusN(vecs: Array[ObjectClosure[Vec]]) extends ObjectClosure[Vec] {
    override def evaluate(environment: Environment): Vec = {
      var temp = vecs(0).evaluate(environment)
      val r = util.Arrays.copyOf(temp.elements, temp.elements.length)

      for (i <- 1 to vecs.length - 1) {
        temp = vecs(i).evaluate(environment)
        for (j <- 0 to r.length - 1) {
          r(j) = r(j) + temp.elements(j)
        }
      }

      new Vec(r)

      /*
      val temp = vecs(0).evaluate(environment);
      vecs.view(1, vecs.size - 1).par.foldLeft(temp)((b, c) => b.plus(c.evaluate(environment)))
      */
    }
  }

  class Plus(left: ObjectClosure[Vec], right: ObjectClosure[Vec]) extends PlusN(Array(left, right)) {
    @Suppress(languages = Array(classOf[VecL]))
    override def evaluate(environment: Environment) = {
      left.evaluate(environment).plus(right.evaluate(environment))
    }
  }

  case class Times(vec: ObjectClosure[Vec], s: DoubleClosure) extends ObjectClosure[Vec] {
    @Suppress(languages = Array(classOf[VecL]))
    override def evaluate(environment: Environment) = {
      vec.evaluate(environment).times(s.evaluate(environment))
    }
  }

  def compile[T <: Closure[_]](expression: Expression): T = {
    expression match {
      case MethodInvocation(CtMethod("plus"), _, List(leftE, rightE)) =>
        (compile[ObjectClosure[Vec]](leftE), compile[ObjectClosure[Vec]](rightE)) match {
          case (PlusN(lVecs), PlusN(rVecs)) => PlusN(lVecs ++ rVecs)
          case ( PlusN(vecs),       rightC) => PlusN(vecs :+ rightC)
          case (       leftC,  PlusN(vecs)) => PlusN(leftC +: vecs)
          case (       leftC,       rightC) => new Plus(leftC, rightC)
        }

      case MethodInvocation(CtMethod("times"), _, List(vecE, sE)) =>
        (compile[ObjectClosure[Vec]](vecE), compile[DoubleClosure](sE)) match {
          case (Times(vec, s), sC) => Times(vec, new DoubleClosure {
            override def evaluate(env: Environment): Double = sC.evaluate(env) * s.evaluate(env)
          })
          case (         vecC, sC) => Times(vecC, sC)
        }

      // For implicit conversions
      // case MethodInvocation(_, _, List(_, e)) => compile(e)
      case ObjectValue(oC) => oC
      case DoubleValue(dC) => dC
    }
  }.asInstanceOf[T]
}
