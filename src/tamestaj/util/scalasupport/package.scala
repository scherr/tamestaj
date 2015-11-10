package tamestaj.util

import tamestaj._

package object scalasupport {
  implicit def functionToClosure[V](f: Environment => V): ObjectClosure[V] = new ObjectClosure[V] {
    override def evaluate(environment: Environment): V = f.apply(environment)
  }
  implicit def functionToClosure(f: Environment => Boolean): BooleanClosure = new BooleanClosure {
    override def evaluate(environment: Environment): Boolean = f.apply(environment)
  }
  implicit def functionToClosure(f: Environment => Int): IntegerClosure = new IntegerClosure {
    override def evaluate(environment: Environment): Int = f.apply(environment)
  }
  implicit def functionToClosure(f: Environment => Long): LongClosure = new LongClosure {
    override def evaluate(environment: Environment): Long = f.apply(environment)
  }
  implicit def functionToClosure(f: Environment => Float): FloatClosure = new FloatClosure {
    override def evaluate(environment: Environment): Float = f.apply(environment)
  }
  implicit def functionToClosure(f: Environment => Double): DoubleClosure = new DoubleClosure {
    override def evaluate(environment: Environment): Double = f.apply(environment)
  }
  implicit def functionToClosure(f: Environment => Byte): ByteClosure = new ByteClosure {
    override def evaluate(environment: Environment): Byte = f.apply(environment)
  }
  implicit def functionToClosure(f: Environment => Char): CharacterClosure = new CharacterClosure {
    override def evaluate(environment: Environment): Char = f.apply(environment)
  }
  implicit def functionToClosure(f: Environment => Short): ShortClosure = new ShortClosure {
    override def evaluate(environment: Environment): Short = f.apply(environment)
  }
}
