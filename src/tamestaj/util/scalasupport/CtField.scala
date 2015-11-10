package tamestaj.util.scalasupport

object CtField {
  def unapply(field: javassist.CtField) = Some(field.getName)
}
