package tamestaj.examples.immutablelist;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import javassist.*;
import tamestaj.Environment;
import tamestaj.Expression;
import tamestaj.ObjectClosure;
import tamestaj.util.CtClassLoader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

final class ImmListLCompiler implements Expression.Visitor {
    private abstract static class Op { ObjectClosure<?> closure; }
    private static class TransformOp extends Op {
        private TransformOp(ObjectClosure<Function<Object, ?>> functionClosure) { this.closure = functionClosure; }
    }
    private static class FilterOp extends Op {
        private FilterOp(ObjectClosure<Predicate<Object>> predicateClosure) { this.closure = predicateClosure; }
    }

    private final Environment.Binder binder;
    private ObjectClosure<?> closure;

    private ObjectClosure<?> input;
    private CtClass returnType;
    private List<Op> ops = null;

    ImmListLCompiler(Environment.Binder binder) {
        this.binder = binder;
    }

    ObjectClosure getClosure() {
        if (ops == null) {
            return input;
        }

        ClassPool cp = ClassPool.getDefault();
        try {
            CtClass cloClazz = cp.makeClass(ImmListLCompiler.class.getName()+ "$FusedOps");
            cloClazz.setModifiers(Modifier.PUBLIC | Modifier.FINAL);
            cloClazz.setInterfaces(new CtClass[]{ cp.get(ObjectClosure.class.getName()) });

            CtField field = CtField.make("public static " + ObjectClosure.class.getName() + " c0;", cloClazz);
            cloClazz.addField(field);
            for (int i = 0; i < ops.size(); i++) {
                field = CtField.make("public static " + ObjectClosure.class.getName() + " c" + (i + 1) + ";", cloClazz);
                cloClazz.addField(field);
            }

            StringBuilder cloSource = new StringBuilder();
            cloSource.append("public Object evaluate(" + Environment.class.getName() + " env) {\n");
            for (int i = 0; i < ops.size(); i++) {
                if (ops.get(i) instanceof TransformOp) {
                    cloSource.append("    " + Function.class.getName() + " f" + (i + 1) + " = (" + Function.class.getName() + ") c" + (i + 1) + ".evaluate(env);\n");
                } else if (ops.get(i) instanceof FilterOp) {
                    cloSource.append("    " + Predicate.class.getName() + " p" + (i + 1) + " = (" + Predicate.class.getName() + ") c" + (i + 1) + ".evaluate(env);\n");
                }
            }
            cloSource.append("    " + returnType.getName() + "$Builder builder = " + returnType.getName() + ".builder();\n");
            cloSource.append("    " + Iterator.class.getName() + " iterator = ((" + Iterable.class.getName() + ") c0.evaluate(env)).iterator();\n");
            cloSource.append("    while (iterator.hasNext()) {\n"
                    + "        Object o = iterator.next();\n");
            for (int i = 0; i < ops.size(); i++) {
                if (ops.get(i) instanceof TransformOp) {
                    cloSource.append("        o = f" + (i + 1) + ".apply(o);\n");
                } else if (ops.get(i) instanceof FilterOp) {
                    cloSource.append("        if (!p" + (i + 1) + ".apply(o)) { continue; }\n");
                }
            }
            cloSource.append("        builder.add(o);\n"
                    + "    }\n"
                    + "    return builder.build();\n"
                    + "}");
            CtMethod cloMethod = CtNewMethod.make(cloSource.toString(), cloClazz);
            cloClazz.addMethod(cloMethod);

            CtClassLoader loader = new CtClassLoader();
            Class<?> cloC = loader.load(cloClazz);
            cloClazz.detach();

            Field f = cloC.getDeclaredField("c0");
            f.set(null, input);
            for (int i = 0; i < ops.size(); i++) {
                f = cloC.getDeclaredField("c" + (i + 1));
                f.set(null, ops.get(i).closure);
            }

            return (ObjectClosure) cloC.newInstance();
        } catch (NotFoundException | CannotCompileException | InstantiationException | IllegalAccessException | NoSuchFieldException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void visit(Expression.FieldRead staged) { }
    @Override
    public void visit(Expression.FieldAssignment staged) { }
    @Override
    public void visit(Expression.MethodInvocation staged) {
        CtMethod method = staged.getMember();
        if (returnType == null) {
            try {
                returnType = method.getReturnType();
            } catch (NotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        staged.getArgument(0).accept(this);
        ObjectClosure<?> arg0 = closure;
        if (input == null) {
            input = arg0;
        }

        if (staged.getArgumentCount() > 1) {
            if (ops == null) {
                ops = new LinkedList<>();
            }

            staged.getArgument(1).accept(this);
            ObjectClosure<?> arg1 = closure;

            switch (method.getName()) {
                case "map": {
                    ops.add(new TransformOp((ObjectClosure<Function<Object, ?>>) arg1));
                    break;
                }
                case "filter": {
                    ops.add(new FilterOp((ObjectClosure<Predicate<Object>>) arg1));
                    break;
                }
            }
        }
    }

    @Override
    public void visit(Expression.ObjectValue value) {
        closure = value.bind(binder);
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
