package tamestaj.examples.vector;

import tamestaj.*;
import tamestaj.annotations.Suppress;

import java.util.Arrays;

final class VecLCompiler implements Expression.Visitor {
    private final Environment.Binder binder;
    private ObjectClosure<Vec> closure;

    VecLCompiler(Environment.Binder binder) {
        this.binder = binder;
    }

    public ObjectClosure<Vec> getClosure() {
        return closure;
    }

    @Override
    public void visit(Expression.FieldRead staged) { }
    @Override
    public void visit(Expression.FieldAssignment staged) { }
    @Override
    public void visit(Expression.MethodInvocation staged) {
        staged.getArgument(0).accept(this);
        ObjectClosure<Vec> arg0 = closure;
        switch (staged.getMember().getName()) {
            case "plus": {
                staged.getArgument(1).accept(this);
                ObjectClosure<Vec> arg1 = closure;

                if (arg0 instanceof PlusN && arg1 instanceof PlusN) {
                    ObjectClosure<Vec>[] vecs = Arrays.copyOf(((PlusN) arg0).vecs, ((PlusN) arg0).vecs.length + ((PlusN) arg1).vecs.length);
                    System.arraycopy(((PlusN) arg1).vecs, 0, vecs, ((PlusN) arg0).vecs.length, ((PlusN) arg1).vecs.length);

                    closure = new PlusN(vecs);
                } else if (arg0 instanceof PlusN) {
                    ObjectClosure<Vec>[] vecs = Arrays.copyOf(((PlusN) arg0).vecs, ((PlusN) arg0).vecs.length + 1);
                    vecs[vecs.length - 1] = arg1;

                    closure = new PlusN(vecs);
                } else if (arg1 instanceof PlusN) {
                    ObjectClosure<Vec>[] vecs = new ObjectClosure[((PlusN) arg1).vecs.length + 1];
                    vecs[0] = arg0;
                    System.arraycopy(((PlusN) arg1).vecs, 0, vecs, 1, ((PlusN) arg1).vecs.length);

                    closure = new PlusN(vecs);
                } else {
                    closure = new Plus(arg0, arg1);
                }

                // closure = new Plus(arg0, arg1);

                break;
            }
            case "times": {
                DoubleClosure arg1 = ((Expression.DoubleValue) staged.getArgument(1)).bind(binder);

                if (arg0 instanceof Times) {
                    DoubleClosure s = ((Times) arg0).s;
                    closure = new Times(((Times) arg0).vec, env -> s.evaluate(env) * arg1.evaluate(env));
                } else {
                    closure = new Times(arg0, arg1);
                }

                // closure = new Times(arg0, arg1);

                break;
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

    public static class PlusN implements ObjectClosure<Vec> {
        final ObjectClosure<Vec>[] vecs;

        public PlusN(ObjectClosure<Vec>[] vecs) {
            this.vecs = vecs;
        }

        @Override
        public Vec evaluate(Environment env) throws Throwable {
            Vec temp = vecs[0].evaluate(env);
            double[] r = Arrays.copyOf(temp.elements, temp.elements.length);
            for (int i = 1; i < vecs.length; i++) {
                temp = vecs[i].evaluate(env);
                for (int j = 0; j < r.length; j++) {
                    r[j] += temp.elements[j];
                }
            }
            return new Vec(r);
        }
    }

    public static final class Plus extends PlusN {
        final ObjectClosure<Vec> left;
        final ObjectClosure<Vec> right;

        public Plus(ObjectClosure<Vec> left, ObjectClosure<Vec> right) {
            super(new ObjectClosure[] { left, right });
            this.left = left;
            this.right = right;
        }

        @Override
        @Suppress(languages = VecL.class)
        public Vec evaluate(Environment env) throws Throwable {
            return left.evaluate(env).plus(right.evaluate(env));
        }
    }

    public static final class Times implements ObjectClosure<Vec> {
        final ObjectClosure<Vec> vec;
        final DoubleClosure s;

        public Times(ObjectClosure<Vec> vec, DoubleClosure s) {
            this.vec = vec;
            this.s = s;
        }

        @Override
        @Suppress(languages = VecL.class)
        public Vec evaluate(Environment env) throws Throwable {
            return vec.evaluate(env).times(s.evaluate(env));
        }
    }
}
