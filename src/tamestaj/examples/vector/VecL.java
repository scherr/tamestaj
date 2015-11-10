package tamestaj.examples.vector;

import tamestaj.Environment;
import tamestaj.Expression;
import tamestaj.Language;
import tamestaj.ObjectClosure;
import tamestaj.annotations.Stage;
import tamestaj.util.TreePrinterMaker;

class VecL implements Language<VecL> {
    public static ObjectClosure makeObjectClosure(Expression.Staged staged, Environment.Binder binder, boolean permCached) {
        if (staged.getMember().getName().equals("print")) {
            TreePrinterMaker treePrinterMaker = new TreePrinterMaker(binder);
            staged.getArgument(0).accept(treePrinterMaker);
            return treePrinterMaker.getObjectClosure();
        } else if (staged.getMember().getName().equals("inspect")) {
            Expression e = staged.getArgument(0);
            return env -> e;
        }

        VecLCompiler compiler = new VecLCompiler(binder);
        staged.accept(compiler);
        return compiler.getClosure();

        /*
        return (ObjectClosure) new VecLScalaCompiler(binder).compile(staged);
        */
    }

    @Stage(language = VecL.class, isStrict = true)
    public static String print(Object obj) { throw new UnsupportedOperationException(); }

    @Stage(language = VecL.class, isStrict = true)
    public static Expression inspect(Object obj) { throw new UnsupportedOperationException(); }
}
