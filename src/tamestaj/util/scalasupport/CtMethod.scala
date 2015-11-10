package tamestaj.util.scalasupport

object CtMethod {
  def unapply(method: javassist.CtMethod) = Some(method.getName)
}
