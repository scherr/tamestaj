package tamestaj.examples.mini;

import tamestaj.*;

class MiniL implements Language<MiniL> {
    public static BooleanClosure makeBooleanClosure(Expression.Staged expression, Environment.Binder binder, boolean permCached) {
        MiniLAnalyzer a = new MiniLAnalyzer(binder);
        expression.accept(a);

        MiniLCompiler c = new MiniLCompiler(binder);
        expression.accept(c);
        return c.getClosure(BooleanClosure.class);
    }

    public static IntegerClosure makeIntegerClosure(Expression.Staged expression, Environment.Binder binder, boolean permCached) {
        MiniLAnalyzer a = new MiniLAnalyzer(binder);
        expression.accept(a);

        MiniLCompiler c = new MiniLCompiler(binder);
        expression.accept(c);
        return c.getClosure(IntegerClosure.class);
    }
}
