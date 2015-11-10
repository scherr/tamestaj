package tamestaj.examples.functor;

import tamestaj.Environment;
import tamestaj.Expression;
import tamestaj.Language;
import tamestaj.ObjectClosure;

public class FunctorL implements Language<FunctorL> {
    public static ObjectClosure makeObjectClosure(Expression expression, Environment.Binder binder, boolean permCached) {
        // FunctorLSimpleCompiler compiler = new FunctorLSimpleCompiler(binder);
        FunctorLCompiler compiler = new FunctorLCompiler(binder);
        expression.accept(compiler);
        return compiler.getClosure();
    }
}
