package tamestaj.util.scalasupport

import tamestaj.Environment.Binder

object Expression {
  object FieldRead {
    def unapply(expression: tamestaj.Expression.FieldRead) = {
      var args = List.empty[tamestaj.Expression]
      for (i <- (expression.getArgumentCount - 1) to 0 by -1) {
        args = expression.getArgument(i) :: args
      }
      Some(expression.getMember, expression.getStaticInfo, args)
    }
  }

  object FieldAssignment {
    def unapply(expression: tamestaj.Expression.FieldAssignment) = {
      var args = List.empty[tamestaj.Expression]
      for (i <- (expression.getArgumentCount - 1) to 0 by -1) {
        args = expression.getArgument(i) :: args
      }
      Some(expression.getMember, expression.getStaticInfo, args)
    }
  }

  object MethodInvocation {
    def unapply(expression: tamestaj.Expression.MethodInvocation) = {
      var args = List.empty[tamestaj.Expression]
      for (i <- (expression.getArgumentCount - 1) to 0 by -1) {
        args = expression.getArgument(i) :: args
      }
      Some(expression.getMember, expression.getStaticInfo, args)
    }
  }

  object ObjectValue {
    object Constant {
      def unapply[V](expression: tamestaj.Expression.ObjectValue[V])(implicit binder: Binder) = if (expression.isConstant) Some(expression.bind(binder)) else None
    }

    def unapply[V](expression: tamestaj.Expression.ObjectValue[V])(implicit binder: Binder) = Some(expression.bind(binder))
  }

  object BooleanValue {
    object Constant {
      def unapply(expression: tamestaj.Expression.BooleanValue)(implicit binder: Binder) = if (expression.isConstant) Some(expression.bind(binder)) else None
    }

    def unapply(expression: tamestaj.Expression.BooleanValue)(implicit binder: Binder) = Some(expression.bind(binder))
  }

  object IntegerValue {
    object Constant {
      def unapply(expression: tamestaj.Expression.IntegerValue)(implicit binder: Binder) = if (expression.isConstant) Some(expression.bind(binder)) else None
    }

    def unapply(expression: tamestaj.Expression.IntegerValue)(implicit binder: Binder) = Some(expression.bind(binder))
  }

  object LongValue {
    object Constant {
      def unapply(expression: tamestaj.Expression.LongValue)(implicit binder: Binder) = if (expression.isConstant) Some(expression.bind(binder)) else None
    }

    def unapply(expression: tamestaj.Expression.LongValue)(implicit binder: Binder) = Some(expression.bind(binder))
  }

  object FloatValue {
    object Constant {
      def unapply(expression: tamestaj.Expression.FloatValue)(implicit binder: Binder) = if (expression.isConstant) Some(expression.bind(binder)) else None
    }

    def unapply(expression: tamestaj.Expression.FloatValue)(implicit binder: Binder) = Some(expression.bind(binder))
  }

  object DoubleValue {
    object Constant {
      def unapply(expression: tamestaj.Expression.DoubleValue)(implicit binder: Binder) = if (expression.isConstant) Some(expression.bind(binder)) else None
    }

    def unapply(expression: tamestaj.Expression.DoubleValue)(implicit binder: Binder) = Some(expression.bind(binder))
  }

  object ByteValue {
    object Constant {
      def unapply(expression: tamestaj.Expression.ByteValue)(implicit binder: Binder) = if (expression.isConstant) Some(expression.bind(binder)) else None
    }

    def unapply(expression: tamestaj.Expression.ByteValue)(implicit binder: Binder) = Some(expression.bind(binder))
  }

  object CharacterValue {
    object Constant {
      def unapply(expression: tamestaj.Expression.CharacterValue)(implicit binder: Binder) = if (expression.isConstant) Some(expression.bind(binder)) else None
    }

    def unapply(expression: tamestaj.Expression.CharacterValue)(implicit binder: Binder) = Some(expression.bind(binder))
  }

  object ShortValue {
    object Constant {
      def unapply(expression: tamestaj.Expression.ShortValue)(implicit binder: Binder) = if (expression.isConstant) Some(expression.bind(binder)) else None
    }

    def unapply(expression: tamestaj.Expression.ShortValue)(implicit binder: Binder) = Some(expression.bind(binder))
  }
}
