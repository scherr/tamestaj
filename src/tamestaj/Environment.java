package tamestaj;

import com.google.common.primitives.Ints;

import java.util.HashMap;
import java.util.Stack;
import java.util.function.Function;

@SuppressWarnings("unused")
public final class Environment {
    public static final class Binder {
        private final Expression.Staged staged;

        private final HashMap<Expression.Value<?>, Closure<?>> closureCache;

        private boolean inspectionOccurred;
        private int boundCount;

        Binder(Expression.Staged staged) {
            closureCache = new HashMap<>();
            this.staged = staged;
        }

        int getBoundCount() {
            return boundCount;
        }

        private int[] pathTo(Expression.Value<?> value) {
            Stack<Integer> pathStack = new Stack<>();
            pathStack.push(0);
            Stack<Expression.Staged> stack = new Stack<>();
            stack.push(staged);

            while (!stack.empty()) {
                Expression.Staged staged = stack.peek();
                int index = pathStack.peek();

                if (index < staged.getArgumentCount()) {
                    Expression arg = staged.getArgument(index);
                    if (arg instanceof Expression.Staged) {
                        stack.push((Expression.Staged) arg);
                        pathStack.push(0);
                    } else if (arg == value) {
                        return Ints.toArray(pathStack);
                    } else {
                        pathStack.push(pathStack.pop() + 1);
                    }
                } else {
                    stack.pop();
                    pathStack.pop();
                    pathStack.push(pathStack.pop() + 1);
                }
            }

            throw new RuntimeException();
        }

        // Code generation instead of interpretation does not seem to be worth it...
        /*
        private Function<Expression.Staged, Expression.Value<?>> makeValueAccessor(int[] path) {
            ClassPool cp = ClassPool.getDefault();
            try {
                CtClass funClazz = cp.makeClass(Util.ENVIRONMENT_CLASS.getName() + "$ValueAccessor");
                funClazz.setInterfaces(new CtClass[]{ cp.get("java.util.function.Function") });

                funClazz.addConstructor(CtNewConstructor.defaultConstructor(funClazz));

                StringBuilder funSource = new StringBuilder();
                funSource.append("public Object apply(Object staged) {\n");
                String code = "((" + Expression.Staged.class.getName() + ") staged)";
                for (int i = 0; i < path.length - 1; i++) {
                    code = "((" + Expression.Staged.class.getName() + ") " + code + ".getArgument(" + path[i] + "))";
                }
                funSource.append("    return " + code + ".getArgument(" + path[path.length - 1] + ");\n");
                funSource.append("}");
                CtMethod funMethod = CtNewMethod.make(funSource.toString(), funClazz);
                funClazz.addMethod(funMethod);

                CtClassLoader loader = new CtClassLoader();
                Class<?> funClass = loader.load(funClazz);
                funClazz.detach();

                return (Function<Expression.Staged, Expression.Value<?>>) funClass.newInstance();
            } catch (CannotCompileException | NotFoundException | InstantiationException | IllegalAccessException | IOException e) {
                throw new RuntimeException(e);
            }
        }
        */

        @SuppressWarnings("unchecked")
        <V> ObjectClosure<V> bind(Expression.ObjectValue<V> value) {
            ObjectClosure<V> closure = (ObjectClosure) closureCache.get(value);
            if (closure == null) {
                int[] path = pathTo(value);
                int cacheIndex = boundCount;
                boundCount++;

                closure = environment -> (V) environment.getValue(cacheIndex, path).materializeAsObject();
                closureCache.put(value, closure);
            }

            return closure;
        }
        <V> ObjectClosure<V> bind(Expression.ObjectValue.Constant<V> constant) {
            ObjectClosure<V> closure = (ObjectClosure<V>) closureCache.get(constant);
            if (closure == null) {
                V value = constant.materializeAsObject();
                closure = environment -> value;
                closureCache.put(constant, closure);
            }

            return closure;
        }

        BooleanClosure bind(Expression.BooleanValue value) {
            BooleanClosure closure = (BooleanClosure) closureCache.get(value);
            if (closure == null) {
                int[] path = pathTo(value);
                int cacheIndex = boundCount;
                boundCount++;

                closure = environment -> environment.getValue(cacheIndex, path).materializeAsBoolean();
                closureCache.put(value, closure);
            }

            return closure;
        }
        BooleanClosure bind(Expression.BooleanValue.Constant constant) {
            BooleanClosure closure = (BooleanClosure) closureCache.get(constant);
            if (closure == null) {
                Boolean value = constant.materializeAsBoolean();
                closure = environment -> value;
                closureCache.put(constant, closure);
            }

            return closure;
        }

        IntegerClosure bind(Expression.IntegerValue value) {
            IntegerClosure closure = (IntegerClosure) closureCache.get(value);
            if (closure == null) {
                int[] path = pathTo(value);
                int cacheIndex = boundCount;
                boundCount++;

                closure = environment -> environment.getValue(cacheIndex, path).materializeAsInteger();
                closureCache.put(value, closure);
            }

            return closure;
        }
        IntegerClosure bind(Expression.IntegerValue.Constant constant) {
            IntegerClosure closure = (IntegerClosure) closureCache.get(constant);
            if (closure == null) {
                Integer value = constant.materializeAsInteger();
                closure = environment -> value;
                closureCache.put(constant, closure);
            }

            return closure;
        }

        LongClosure bind(Expression.LongValue value) {
            LongClosure closure = (LongClosure) closureCache.get(value);
            if (closure == null) {
                int[] path = pathTo(value);
                int cacheIndex = boundCount;
                boundCount++;

                closure = environment -> environment.getValue(cacheIndex, path).materializeAsLong();
                closureCache.put(value, closure);
            }

            return closure;
        }
        LongClosure bind(Expression.LongValue.Constant constant) {
            LongClosure closure = (LongClosure) closureCache.get(constant);
            if (closure == null) {
                Long value = constant.materializeAsLong();
                closure = environment -> value;
                closureCache.put(constant, closure);
            }

            return closure;
        }

        FloatClosure bind(Expression.FloatValue value) {
            FloatClosure closure = (FloatClosure) closureCache.get(value);
            if (closure == null) {
                int[] path = pathTo(value);
                int cacheIndex = boundCount;
                boundCount++;

                closure = environment -> environment.getValue(cacheIndex, path).materializeAsFloat();
                closureCache.put(value, closure);
            }

            return closure;
        }
        FloatClosure bind(Expression.FloatValue.Constant constant) {
            FloatClosure closure = (FloatClosure) closureCache.get(constant);
            if (closure == null) {
                Float value = constant.materializeAsFloat();
                closure = environment -> value;
                closureCache.put(constant, closure);
            }

            return closure;
        }

        DoubleClosure bind(Expression.DoubleValue value) {
            DoubleClosure closure = (DoubleClosure) closureCache.get(value);
            if (closure == null) {
                int[] path = pathTo(value);
                int cacheIndex = boundCount;
                boundCount++;

                closure = environment -> environment.getValue(cacheIndex, path).materializeAsDouble();
                closureCache.put(value, closure);
            }

            return closure;
        }
        DoubleClosure bind(Expression.DoubleValue.Constant constant) {
            DoubleClosure closure = (DoubleClosure) closureCache.get(constant);
            if (closure == null) {
                Double value = constant.materializeAsDouble();
                closure = environment -> value;
                closureCache.put(constant, closure);
            }

            return closure;
        }

        ByteClosure bind(Expression.ByteValue value) {
            ByteClosure closure = (ByteClosure) closureCache.get(value);
            if (closure == null) {
                int[] path = pathTo(value);
                int cacheIndex = boundCount;
                boundCount++;

                closure = environment -> environment.getValue(cacheIndex, path).materializeAsByte();
                closureCache.put(value, closure);
            }

            return closure;
        }
        ByteClosure bind(Expression.ByteValue.Constant constant) {
            ByteClosure closure = (ByteClosure) closureCache.get(constant);
            if (closure == null) {
                Byte value = constant.materializeAsByte();
                closure = environment -> value;
                closureCache.put(constant, closure);
            }

            return closure;
        }

        CharacterClosure bind(Expression.CharacterValue value) {
            CharacterClosure closure = (CharacterClosure) closureCache.get(value);
            if (closure == null) {
                int[] path = pathTo(value);
                int cacheIndex = boundCount;
                boundCount++;

                closure = environment -> environment.getValue(cacheIndex, path).materializeAsCharacter();
                closureCache.put(value, closure);
            }

            return closure;
        }
        CharacterClosure bind(Expression.CharacterValue.Constant constant) {
            CharacterClosure closure = (CharacterClosure) closureCache.get(constant);
            if (closure == null) {
                Character value = constant.materializeAsCharacter();
                closure = environment -> value;
                closureCache.put(constant, closure);
            }

            return closure;
        }

        ShortClosure bind(Expression.ShortValue value) {
            ShortClosure closure = (ShortClosure) closureCache.get(value);
            if (closure == null) {
                int[] path = pathTo(value);
                int cacheIndex = boundCount;
                boundCount++;

                closure = environment -> environment.getValue(cacheIndex, path).materializeAsShort();
                closureCache.put(value, closure);
            }

            return closure;
        }
        ShortClosure bind(Expression.ShortValue.Constant constant) {
            ShortClosure closure = (ShortClosure) closureCache.get(constant);
            if (closure == null) {
                Short value = constant.materializeAsShort();
                closure = environment -> value;
                closureCache.put(constant, closure);
            }

            return closure;
        }

        boolean inspectionOccurred() {
            return inspectionOccurred;
        }
        void setInspectionOccurred() {
            this.inspectionOccurred = true;
        }
    }

    private final Expression.Staged staged;
    private final Expression.Value<?>[] cachedValues;

    Environment(Expression.Staged staged, int size) {
        this.staged = staged;
        this.cachedValues = new Expression.Value<?>[size];
    }

    private Expression.Value<?> getValue(int cacheIndex, int[] path) {
        Expression.Value<?> value = cachedValues[cacheIndex];
        if (value != null) {
            return value;
        }

        Expression.Staged staged = this.staged;

        for (int i = 0; i < path.length - 1; i++) {
            staged = (Expression.Staged) staged.getArgument(path[i]);
        }

        value = (Expression.Value<?>) staged.getArgument(path[path.length - 1]);
        cachedValues[cacheIndex] = value;
        return value;
    }
}
