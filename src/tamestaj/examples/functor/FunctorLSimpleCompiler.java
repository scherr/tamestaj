package tamestaj.examples.functor;

import javassist.*;
import tamestaj.Environment;
import tamestaj.Expression;
import tamestaj.ObjectClosure;
import tamestaj.annotations.Suppress;
import tamestaj.util.CtClassLoader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.function.Function;

@Suppress(languages = FunctorL.class)
final class FunctorLSimpleCompiler implements Expression.Visitor {
    private final Environment.Binder binder;
    private ObjectClosure<?> closure;

    private ObjectClosure<?> functor;
    private ObjectClosure combinedFunction;

    FunctorLSimpleCompiler(Environment.Binder binder) {
        this.binder = binder;
    }

    ObjectClosure<?> getClosure() {
        ObjectClosure<?> f = functor;
        return env -> {
            return ((Functor<?>) f.evaluate(env)).fmap((Function) combinedFunction.evaluate(env));
        };
    }

    @Override
    public void visit(Expression.FieldRead staged) { }
    @Override
    public void visit(Expression.FieldAssignment staged) { }
    @Override
    public void visit(Expression.MethodInvocation staged) {
        staged.getArgument(0).accept(this);
        ObjectClosure<?> arg0 = closure;

        staged.getArgument(1).accept(this);
        ObjectClosure<?> arg1 = closure;

        if (combinedFunction == null) {
            combinedFunction = arg1;

            functor = arg0;
        } else {
            ObjectClosure f = combinedFunction;
            combinedFunction = env -> ((Function) f.evaluate(env)).andThen((Function) arg1.evaluate(env));
        }
    }

    @Override
    public void visit(Expression.ObjectValue value) {
        if (value.isConstant()) {
            closure = env -> value.inspect(binder);
        } else {
            closure = value.bind(binder);
        }
    }
    @Override
    public void visit(Expression.BooleanValue value) { }
    @Override
    public void visit(Expression.IntegerValue value) { }
    @Override
    public void visit(Expression.LongValue value) { }
    @Override
    public void visit(Expression.FloatValue value) { }
    @Override
    public void visit(Expression.DoubleValue value) { }
    @Override
    public void visit(Expression.ByteValue value) { }
    @Override
    public void visit(Expression.CharacterValue value) { }
    @Override
    public void visit(Expression.ShortValue value) { }
}
