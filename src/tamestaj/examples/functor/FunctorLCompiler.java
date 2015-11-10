package tamestaj.examples.functor;

import tamestaj.Environment;
import tamestaj.Expression;
import tamestaj.ObjectClosure;
import tamestaj.annotations.Suppress;
import tamestaj.util.CtClassLoader;
import javassist.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.function.Function;

@Suppress(languages = FunctorL.class)
class FunctorLCompiler implements Expression.Visitor {
    private final Environment.Binder binder;
    private ObjectClosure<?> closure;

    private ObjectClosure<?> functor;
    private ArrayList<ObjectClosure> ops;

    FunctorLCompiler(Environment.Binder binder) {
        this.binder = binder;
    }

    private ObjectClosure getCombinedFunction() {
        if (ops.size() > 1) {
            ClassPool cp = ClassPool.getDefault();
            try {
                CtClass funClazz = cp.makeClass(FunctorLCompiler.class.getName()+ "$CombinedFunction");
                funClazz.setInterfaces(new CtClass[]{ cp.get("java.util.function.Function") });

                funClazz.addConstructor(CtNewConstructor.defaultConstructor(funClazz));

                for (int i = 0; i < ops.size(); i++) {
                    CtField field = CtField.make("public java.util.function.Function f" + i + ";", funClazz);
                    funClazz.addField(field);
                }

                StringBuilder funSource = new StringBuilder();
                funSource.append("public Object apply(Object o) {\n");
                for (int i = 0; i < ops.size(); i++) {
                    funSource.append("    o = f" + i + ".apply(o);\n");
                }
                funSource.append("    return o;\n");
                funSource.append("}");
                CtMethod funMethod = CtNewMethod.make(funSource.toString(), funClazz);
                funClazz.addMethod(funMethod);


                CtClass cloClazz = cp.makeClass(FunctorLCompiler.class.getName()+ "$CombinedFunctionObjectClosure");
                cloClazz.setInterfaces(new CtClass[]{ cp.get(ObjectClosure.class.getName()) });

                for (int i = 0; i < ops.size(); i++) {
                    CtField field = CtField.make("public static " + ObjectClosure.class.getName() + " c" + i + ";", cloClazz);
                    cloClazz.addField(field);
                }

                StringBuilder cloSource = new StringBuilder();
                cloSource.append("public Object evaluate(" + Environment.class.getName() + " env) {\n");
                cloSource.append("    " + funClazz.getName() + " fun = new " + funClazz.getName() + "();\n");
                for (int i = 0; i < ops.size(); i++) {
                    cloSource.append("    fun.f" + i + " = (java.util.function.Function) c" + i + ".evaluate(env);\n");
                }
                cloSource.append("    return fun;\n");
                cloSource.append("}");
                CtMethod cloMethod = CtNewMethod.make(cloSource.toString(), cloClazz);
                cloClazz.addMethod(cloMethod);


                CtClassLoader loader = new CtClassLoader();
                loader.load(funClazz);
                funClazz.detach();

                Class<?> cloC = loader.load(cloClazz);
                cloClazz.detach();

                for (int i = 0; i < ops.size(); i++) {
                    Field f = cloC.getDeclaredField("c" + i);
                    f.set(null, ops.get(i));
                }

                return (ObjectClosure) cloC.newInstance();
            } catch (NotFoundException | CannotCompileException | InstantiationException | IOException | NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            return ops.get(0);
        }
    }

    ObjectClosure<?> getClosure() {
        ObjectClosure<?> f = functor;
        ObjectClosure<?> combinedFunction = getCombinedFunction();
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

        if (ops == null) {
            ops = new ArrayList<>();

            functor = arg0;
        }

        ops.add(arg1);
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
