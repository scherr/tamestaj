package tamestaj.examples.immutablelist;

import tamestaj.Environment;
import tamestaj.Expression;
import tamestaj.Language;
import tamestaj.ObjectClosure;

class ImmListL implements Language<ImmListL> {
    public static ObjectClosure makeObjectClosure(Expression expression, Environment.Binder binder, boolean permCached) {
        ImmListLCompiler compiler = new ImmListLCompiler(binder);
        expression.accept(compiler);
        return compiler.getClosure();
    }
}
