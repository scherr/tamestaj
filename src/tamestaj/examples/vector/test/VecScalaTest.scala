package tamestaj.examples.vector.test

import java.util
import java.util.Random

import tamestaj.examples.vector.{Vec}
import tamestaj.util.TickTock

object VecScalaTest {
  private[test] def sink(v0: Vec, v1: Vec, v2: Vec) {
    v0 == v2
  }

  private[test] def example(n: Int, a: Vec, b: Vec, c: Vec): Vec = {
    var vA: Vec = a.plus(b)
    if (n > 10) {
      vA = vA.plus(vA)
    }

    sink(vA, b, vA)

    var i = 0
    var vC: Vec = c
    while (i < n) {
      vC = vC.times(5 + i)
      i += 1
    }
    vC
  }

  def run(size: Int): Long = {
    val r: Random = new Random(4)
    val a: Vec = Vec.create(r.doubles(size).toArray:_*)
    val b: Vec = Vec.create(r.doubles(size).toArray:_*)
    val c: Vec = Vec.create(r.doubles(size).toArray:_*)
    var res: Vec = null
    TickTock.tick()
    var i = 0
    while (i < 100000) {
      res = example(i % 20, a, b, c)
      i += 1
    }
    TickTock.tock
  }

  def main(args: Array[String]) {
    run(10000)
    var i = 0
    while (i < 5) {
      val size: Int = Math.pow(10, i).toInt
      System.out.println(size)
      val times: Array[Long] = Array.ofDim(10)
      var j = 0
      while (j < 10) {
        times(j) = run(size)
        j += 1
      }
      System.out.println(util.Arrays.toString(times))

      i += 1
    }
  }
}
