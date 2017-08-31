package tamestaj;

import javassist.*;

import java.util.IdentityHashMap;
import java.util.Optional;

@SuppressWarnings("unused")
public abstract class Expression {
    public interface Visitor extends Staged.Visitor, Value.Visitor { }

    private Expression() { }

    public abstract void accept(Visitor visitor);

    abstract int isomorphicHashCode();
    abstract boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression);
    abstract Expression cacheClone(IdentityHashMap<Expression, Expression> identityMap);

    abstract boolean isAcceptedBy(CtClass language);
    abstract Expression asValueIfEvaluated();
    abstract Expression getRaw();

    // Hidden! Access via Dispatcher which is made public before first run.
    abstract void evaluate();

    abstract Object materializeAsObject();
    abstract boolean materializeAsBoolean();
    abstract int materializeAsInteger();
    abstract long materializeAsLong();
    abstract float materializeAsFloat();
    abstract double materializeAsDouble();
    abstract byte materializeAsByte();
    abstract char materializeAsCharacter();
    abstract short materializeAsShort();

    Expression convertToBoolean() {
        return ToBooleanConversion.make(this);
    }
    Expression convertToInteger() {
        return ToIntegerConversion.make(this.getRaw());
    }
    Expression convertToLong() {
        return ToLongConversion.make(this.getRaw());
    }
    Expression convertToFloat() {
        return ToFloatConversion.make(this.getRaw());
    }
    Expression convertToDouble() {
        return ToDoubleConversion.make(this.getRaw());
    }
    Expression convertToByte() {
        return ToByteConversion.make(this.getRaw());
    }
    Expression convertToCharacter() {
        return ToCharacterConversion.make(this.getRaw());
    }
    Expression convertToShort() {
        return ToShortConversion.make(this.getRaw());
    }

    public abstract static class Staged extends Expression {
        public interface Visitor {
            void visit(FieldRead staged);
            void visit(FieldAssignment staged);
            void visit(MethodInvocation staged);
        }

        volatile boolean isomorphicHashCodeHasBeenCalculated;
        int isomorphicHashCode;

        private final Expression[] arguments;

        final StaticInfo staticInfo;
        final ClosureHolder<?> closureHolder;

        Value<?> value;

        private Staged(Expression[] arguments, StaticInfo staticInfo, ClosureHolder<?> closureHolder) {
            this.arguments = arguments;
            this.staticInfo = staticInfo;
            this.closureHolder = closureHolder;
        }

        int isomorphicHashCode(int memberHashCode) {
            // if (!isomorphicHashCodeHasBeenCalculated) {
                int isomorphicHashCode = memberHashCode;
                isomorphicHashCode += 31 * getStaticInfo().hashCode();
                for (Expression argument : arguments) {
                    isomorphicHashCode = 31 * isomorphicHashCode + argument.isomorphicHashCode();
                }

                this.isomorphicHashCode = isomorphicHashCode;
                isomorphicHashCodeHasBeenCalculated = true;
            // }

            return isomorphicHashCode;
        }

        final Expression asValueIfEvaluated() {
            if (value == null) {
                return this;
            } else {
                return value;
            }
        }
        final Expression getRaw() { return this; }

        final Object materializeAsObject() {
            evaluate();
            return value.materializeAsObject();
        }
        final boolean materializeAsBoolean() {
            evaluate();
            return value.materializeAsBoolean();
        }
        final int materializeAsInteger() {
            evaluate();
            return value.materializeAsInteger();
        }
        final long materializeAsLong() {
            evaluate();
            return value.materializeAsLong();
        }
        final float materializeAsFloat() {
            evaluate();
            return value.materializeAsFloat();
        }
        final double materializeAsDouble() {
            evaluate();
            return value.materializeAsDouble();
        }
        final byte materializeAsByte() {
            evaluate();
            return value.materializeAsByte();
        }
        final char materializeAsCharacter() {
            evaluate();
            return value.materializeAsCharacter();
        }
        final short materializeAsShort() {
            evaluate();
            return value.materializeAsShort();
        }

        public abstract CtMember getMember();

        public final Optional<StaticInfo> getStaticInfo() { return Optional.ofNullable(staticInfo); }

        void setStaticInfo(StaticInfo staticInfo) { }
        <T extends Closure<?>> void setClosureHolder(ClosureHolder<T> closureHolder) { }

        public final int getArgumentCount() { return arguments.length; }
        public final Expression getArgument(int index) { return arguments[index]; }

        /*
        boolean argumentsEqual(Staged staged) {
            if (arguments == staged.arguments) { return true; }
            if (arguments.length != staged.arguments.length) { return false; }
            for (int i = 0; i < arguments.length; i++) {
                if (!getArgument(i).equals(staged.getArgument(i))) {
                    return false;
                }
            }
            return true;
        }
        */

        boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression) {
            // Specific behavior in generated classes!

            if (isomorphicHashCode() != expression.isomorphicHashCode()) { return false; }

            Object o = identityMap.get(this);
            if (o != null) {
                return o == expression;
            } else {
                // if (this != expression) {
                    Staged staged = (Staged) expression;
                    if (!getStaticInfo().equals(staged.getStaticInfo())) {
                        return false;
                    }
                    /*
                    if (arguments == staged.arguments) {
                        identityMap.put(this, staged);
                        return true;
                    }
                    if (arguments.length != staged.arguments.length) {
                        return false;
                    }
                    */

                    for (int i = 0; i < arguments.length; i++) {
                        if (!arguments[i].isIsomorphicTo(identityMap, staged.arguments[i])) {
                            return false;
                        }
                    }
                // }

                identityMap.put(this, expression);
                return true;
            }
        }

        Expression[] cacheCloneArguments(IdentityHashMap<Expression, Expression> identityMap) {
            boolean doClone = false;
            Expression[] clonedArguments = new Expression[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                clonedArguments[i] = arguments[i].cacheClone(identityMap);
                if (clonedArguments[i] != arguments[i]) {
                    doClone = true;
                }
            }

            if (doClone) {
                return clonedArguments;
            } else {
                return null;
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(getMember().getName());
            sb.append("(");
            for (int i = 0; i < arguments.length; i++) {
                sb.append(arguments[i].toString());
                if (i < arguments.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")");
            return sb.toString();
        }
    }
    public abstract static class FieldRead extends Staged {
        FieldRead(Expression[] arguments, StaticInfo staticInfo, ClosureHolder<?> closureHolder) {
            super(arguments, staticInfo, closureHolder);
        }

        public abstract CtField getMember();
        /*
        // If at all, this ought to be done without reflection, and only in the second stage... with binder etc.
        public Object readField(Object target) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
            // TODO: Find run-time member only once for concrete generated class
            Class<?> c = Class.forName(getMember().getDeclaringClass().getName());
            Field f = c.getDeclaredField(getMember().getName());
            f.setAccessible(true);

            return f.get(target);
        }
        */

        public final void accept(Expression.Visitor visitor) {
            visitor.visit(this);
        }
        public final void accept(Staged.Visitor visitor) {
            visitor.visit(this);
        }
    }
    public abstract static class FieldAssignment extends Staged {
        FieldAssignment(Expression[] arguments, StaticInfo staticInfo, ClosureHolder<?> closureHolder) {
            super(arguments, staticInfo, closureHolder);
        }

        public abstract CtField getMember();
        /*
        // If at all, this ought to be done without reflection, and only in the second stage... with binder etc.
        public void assignField(Object target, Object value) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
            // TODO: Find run-time member only once for concrete generated class
            Class<?> c = Class.forName(getMember().getDeclaringClass().getName());
            Field f = c.getDeclaredField(getMember().getName());
            f.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(f, f.getModifiers() & ~Modifier.FINAL);

            f.set(target, value);
        }
        */

        public final void accept(Expression.Visitor visitor) {
            visitor.visit(this);
        }
        public final void accept(Staged.Visitor visitor) {
            visitor.visit(this);
        }
    }
    public abstract static class MethodInvocation extends Staged {
        MethodInvocation(Expression[] arguments, StaticInfo staticInfo, ClosureHolder<?> closureHolder) {
            super(arguments, staticInfo, closureHolder);
        }

        public abstract CtMethod getMember();
        /*
        // If at all, this ought to be done without reflection, and only in the second stage... with binder etc.
        public Object invokeMethod(Object target, Object... arguments) throws ClassNotFoundException, NoSuchFieldException, InvocationTargetException, IllegalAccessException, NotFoundException, NoSuchMethodException {
            // TODO: Find run-time member only once for concrete generated class
            Class<?> c = Class.forName(getMember().getDeclaringClass().getName());

            CtClass[] parameterTypes = getMember().getParameterTypes();
            Class<?>[] p = new Class<?>[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                p[i] = Class.forName(parameterTypes[i].getName());
            }
            Method m = c.getDeclaredMethod(getMember().getName(), p);
            m.setAccessible(true);

            return m.invoke(target, arguments);
        }
        */

        public final void accept(Expression.Visitor visitor) {
            visitor.visit(this);
        }
        public final void accept(Staged.Visitor visitor) {
            visitor.visit(this);
        }
    }

    // As it stands right now, conversions only exist as roots of expression DAGs!
    // Only materialization to an object needs to be treated specially because client code might depend on "instanceof"
    // This is not meant to handle actual downcasts at all. We do not handle them generally for many reasons!
    static abstract class Conversion extends Expression {
        final Expression convertee;

        private Conversion(Expression convertee) {
            this.convertee = convertee;
        }

        final public void accept(Visitor visitor) { }

        final int isomorphicHashCode() {
            return convertee.isomorphicHashCode();
        }
        final boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression) {
            return this.convertee.isIsomorphicTo(identityMap, expression);
        }
        final Expression cacheClone(IdentityHashMap<Expression, Expression> identityMap) {
            return this.convertee.cacheClone(identityMap);
        }

        final boolean isAcceptedBy(CtClass language) {
            return convertee.isAcceptedBy(language);
        }
        final Expression asValueIfEvaluated() {
            return convertee.asValueIfEvaluated();
        }
        final Expression getRaw() {
            return convertee;
        }

        final void evaluate() {
            convertee.evaluate();
        }

        final boolean materializeAsBoolean() {
            return convertee.materializeAsBoolean();
        }
        final int materializeAsInteger() {
            return convertee.materializeAsInteger();
        }
        final long materializeAsLong() {
            return convertee.materializeAsLong();
        }
        final float materializeAsFloat() {
            return convertee.materializeAsFloat();
        }
        final double materializeAsDouble() {
            return convertee.materializeAsDouble();
        }
        final byte materializeAsByte() {
            return convertee.materializeAsByte();
        }
        final char materializeAsCharacter() {
            return convertee.materializeAsCharacter();
        }
        final short materializeAsShort() {
            return convertee.materializeAsShort();
        }

        public String toString() {
            return "Converted: " + convertee.toString();
        }
    }
    static final class ToBooleanConversion extends Conversion {
        private ToBooleanConversion(Expression expression) {
            super(expression);
        }

        static ToBooleanConversion make(Expression expression) {
            return new ToBooleanConversion(expression);
        }

        Object materializeAsObject() {
            return convertee.materializeAsBoolean();
        }

        Expression convertToBoolean() {
            return this;
        }
    }
    static final class ToIntegerConversion extends Conversion {
        private ToIntegerConversion(Expression expression) {
            super(expression);
        }

        static ToIntegerConversion make(Expression expression) {
            return new ToIntegerConversion(expression);
        }

        Object materializeAsObject() {
            return convertee.materializeAsInteger();
        }

        Expression convertToInteger() {
            return this;
        }
    }
    static final class ToLongConversion extends Conversion {
        private ToLongConversion(Expression expression) {
            super(expression);
        }

        static ToLongConversion make(Expression expression) {
            return new ToLongConversion(expression);
        }

        Object materializeAsObject() {
            return convertee.materializeAsLong();
        }

        Expression convertToLong() {
            return this;
        }
    }
    static final class ToFloatConversion extends Conversion {
        private ToFloatConversion(Expression expression) {
            super(expression);
        }

        static ToFloatConversion make(Expression expression) {
            return new ToFloatConversion(expression);
        }

        Object materializeAsObject() {
            return convertee.materializeAsFloat();
        }

        Expression convertToFloat() {
            return this;
        }
    }
    static final class ToDoubleConversion extends Conversion {
        private ToDoubleConversion(Expression expression) {
            super(expression);
        }

        static ToDoubleConversion make(Expression expression) {
            return new ToDoubleConversion(expression);
        }

        Object materializeAsObject() {
            return convertee.materializeAsDouble();
        }

        Expression convertToDouble() {
            return this;
        }
    }
    static final class ToByteConversion extends Conversion {
        private ToByteConversion(Expression expression) {
            super(expression);
        }

        static ToByteConversion make(Expression expression) {
            return new ToByteConversion(expression);
        }

        Object materializeAsObject() {
            return convertee.materializeAsByte();
        }

        Expression convertToByte() {
            return this;
        }
    }
    static final class ToCharacterConversion extends Conversion {
        private ToCharacterConversion(Expression expression) {
            super(expression);
        }

        static ToCharacterConversion make(Expression expression) {
            return new ToCharacterConversion(expression);
        }

        Object materializeAsObject() {
            return convertee.materializeAsCharacter();
        }

        Expression convertToCharacter() {
            return this;
        }
    }
    static final class ToShortConversion extends Conversion {
        private ToShortConversion(Expression expression) {
            super(expression);
        }

        static ToShortConversion make(Expression expression) {
            return new ToShortConversion(expression);
        }

        Object materializeAsObject() {
            return convertee.materializeAsShort();
        }

        Expression convertToShort() {
            return this;
        }
    }

    public static abstract class Value<V> extends Expression {
        public interface Visitor {
            void visit(ObjectValue value);
            void visit(BooleanValue value);
            void visit(IntegerValue value);
            void visit(LongValue value);
            void visit(FloatValue value);
            void visit(DoubleValue value);
            void visit(ByteValue value);
            void visit(CharacterValue value);
            void visit(ShortValue value);
        }

        private Value() { }

        final boolean isAcceptedBy(CtClass language) { return true; }
        final Value<V> asValueIfEvaluated() { return this; }
        final Expression getRaw() { return this; }

        final void evaluate() { }

        public boolean isConstant() {
            return false;
        }

        abstract Value<V> makeConstant();

        BooleanValue convertToBoolean() { return BooleanValue.make(this.materializeAsBoolean()); }
        IntegerValue convertToInteger() { return IntegerValue.make(this.materializeAsInteger()); }
        LongValue convertToLong() { return LongValue.make(this.materializeAsLong()); }
        FloatValue convertToFloat() { return FloatValue.make(this.materializeAsFloat()); }
        DoubleValue convertToDouble() { return DoubleValue.make(this.materializeAsDouble()); }
        ByteValue convertToByte() { return ByteValue.make(this.materializeAsByte()); }
        CharacterValue convertToCharacter() { return CharacterValue.make(this.materializeAsCharacter()); }
        ShortValue convertToShort() { return ShortValue.make(this.materializeAsShort()); }

        /*
        public String toString() {
            return inspect().toString();
        }
        */

        final boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Value<?> value) {
            Object o = identityMap.get(this);
            if (o != null) {
                return o == value;
            } else {
                identityMap.put(this, value);
                return true;
            }
        }

        Expression cacheClone(IdentityHashMap<Expression, Expression> identityMap) {
            Expression e = identityMap.get(this);
            if (e != null) {
                return e;
            } else {
                identityMap.put(this, this);
                return this;
            }
        }
    }
    public static class ObjectValue<V> extends Value<V> {
        static final class Constant<V> extends ObjectValue<V> {
            private Constant(V value) {
                super(value);
            }

            public V inspect(Environment.Binder binder) {
                return super.value;
            }
            public ObjectClosure<V> bind(Environment.Binder binder) {
                return binder.bind(this);
            }

            Constant<V> makeConstant() {
                return this;
            }

            int isomorphicHashCode() { return System.identityHashCode(super.value); }
            boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression) {
                if (!(expression instanceof Constant)) { return false; }
                if (super.value != ((ObjectValue) expression).value) { return false; }

                return super.isIsomorphicTo(identityMap, (Value) expression);
            }
            Expression cacheClone(IdentityHashMap<Expression, Expression> identityMap) {
                return this;
            }

            public boolean isConstant() {
                return true;
            }

            BooleanValue.Constant convertToBoolean() { return BooleanValue.makeConstant(this.materializeAsBoolean()); }
            IntegerValue.Constant convertToInteger() { return IntegerValue.makeConstant(this.materializeAsInteger()); }
            LongValue.Constant convertToLong() { return LongValue.makeConstant(this.materializeAsLong()); }
            FloatValue.Constant convertToFloat() { return FloatValue.makeConstant(this.materializeAsFloat()); }
            DoubleValue.Constant convertToDouble() { return DoubleValue.makeConstant(this.materializeAsDouble()); }
            ByteValue.Constant convertToByte() { return ByteValue.makeConstant(this.materializeAsByte()); }
            CharacterValue.Constant convertToCharacter() { return CharacterValue.makeConstant(this.materializeAsCharacter()); }
            ShortValue.Constant convertToShort() { return ShortValue.makeConstant(this.materializeAsShort()); }

            public int hashCode() {
                return System.identityHashCode(super.value);
            }
            public boolean equals(Object obj) {
                if (this == obj) { return true; }
                if (obj instanceof Constant) {
                    return super.value == ((ObjectValue) obj).value;
                }

                return false;
            }

            public String toString() {
                // return Objects.toString(super.value);
                Object o = super.value;
                if (o instanceof String || o instanceof Number || o instanceof Character || o instanceof Boolean) {
                    return o.toString();
                } else if (o == null) {
                    return "null";
                } else {
                    return o.getClass().getName() + "@" + Integer.toHexString(o.hashCode());
                }
            }
        }

        private final V value;

        private ObjectValue(V value) {
            this.value = value;
        }
        static <V> ObjectValue<V> make(V value) { return new ObjectValue<>(value); }
        static <V> Constant<V> makeConstant(V value) { return new Constant<>(value); }

        final V materializeAsObject() { return value; }
        final boolean materializeAsBoolean() { return (Boolean) value; }
        final int materializeAsInteger() { return (Integer) value; }
        final long materializeAsLong() { return (Long) value; }
        final float materializeAsFloat() { return (Float) value; }
        final double materializeAsDouble() { return (Double) value; }
        final byte materializeAsByte() { return (Byte) value; }
        final char materializeAsCharacter() { return (Character) value; }
        final short materializeAsShort() { return (Short) value; }

        public final void accept(Expression.Visitor visitor) {
            visitor.visit(this);
        }
        public final void accept(Value.Visitor visitor) {
            visitor.visit(this);
        }
        public V inspect(Environment.Binder binder) {
            binder.setInspectionOccurred();
            return value;
        }
        public ObjectClosure<V> bind(Environment.Binder binder) {
            return binder.bind(this);
        }

        Constant<V> makeConstant() {
            return new Constant<>(value);
        }

        int isomorphicHashCode() {
            return 137;
        }

        /*
        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (obj instanceof ObjectValue) {
                ObjectValue c = (ObjectValue) obj;
                // return value.equals(c.value);
                return true;
            }
            return false;
        }
        */

        boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression) {
            if (!(expression instanceof ObjectValue)) { return false; }

            return super.isIsomorphicTo(identityMap, (Value) expression);
        }

        Expression cacheClone(IdentityHashMap<Expression, Expression> identityMap) {
            Expression e = identityMap.get(this);
            if (e != null) {
                return e;
            } else {
                Value<?> v = make(null);
                identityMap.put(this, make(null));
                return v;
            }
        }

        public String toString() {
            return "Object";
        }
    }
    public static class BooleanValue extends Value<Boolean> {
        static final class Constant extends BooleanValue {
            private Constant(boolean value) {
                super(value);
            }

            public boolean inspect(Environment.Binder binder) {
                return super.value;
            }
            public BooleanClosure bind(Environment.Binder binder) {
                return binder.bind(this);
            }

            Constant makeConstant() {
                return this;
            }

            int isomorphicHashCode() {
                return Boolean.hashCode(super.value);
            }
            boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression) {
                if (!(expression instanceof Constant)) { return false; }
                if (super.value != ((BooleanValue) expression).value) { return false; }

                return super.isIsomorphicTo(identityMap, (Value) expression);
            }

            public boolean isConstant() {
                return true;
            }

            BooleanValue.Constant convertToBoolean() { return this; }
            IntegerValue.Constant convertToInteger() { return IntegerValue.makeConstant(this.materializeAsInteger()); }
            LongValue.Constant convertToLong() { return LongValue.makeConstant(this.materializeAsLong()); }
            FloatValue.Constant convertToFloat() { return FloatValue.makeConstant(this.materializeAsFloat()); }
            DoubleValue.Constant convertToDouble() { return DoubleValue.makeConstant(this.materializeAsDouble()); }
            ByteValue.Constant convertToByte() { return ByteValue.makeConstant(this.materializeAsByte()); }
            CharacterValue.Constant convertToCharacter() { return CharacterValue.makeConstant(this.materializeAsCharacter()); }
            ShortValue.Constant convertToShort() { return ShortValue.makeConstant(this.materializeAsShort()); }

            public int hashCode() {
                return Boolean.hashCode(super.value);
            }
            public boolean equals(Object obj) {
                if (this == obj) { return true; }
                if (obj instanceof Constant) {
                    return super.value == ((BooleanValue) obj).value;
                }

                return false;
            }

            public String toString() {
                return Boolean.toString(super.value);
            }
        }

        private final boolean value;

        private BooleanValue(boolean value) {
            this.value = value;
        }
        static BooleanValue make(boolean value) { return new BooleanValue(value); }
        static Constant makeConstant(boolean value) { return new Constant(value); }

        final Boolean materializeAsObject() { return value; }
        final boolean materializeAsBoolean() { return value; }
        final int materializeAsInteger() { throw new UnsupportedOperationException(); }
        final long materializeAsLong() { throw new UnsupportedOperationException(); }
        final float materializeAsFloat() { throw new UnsupportedOperationException(); }
        final double materializeAsDouble() { throw new UnsupportedOperationException(); }
        final byte materializeAsByte() { throw new UnsupportedOperationException(); }
        final char materializeAsCharacter() { throw new UnsupportedOperationException(); }
        final short materializeAsShort() { throw new UnsupportedOperationException(); }

        public final void accept(Expression.Visitor visitor) {
            visitor.visit(this);
        }
        public final void accept(Value.Visitor visitor) {
            visitor.visit(this);
        }
        public boolean inspect(Environment.Binder binder) {
            binder.setInspectionOccurred();
            return value;
        }
        public BooleanClosure bind(Environment.Binder binder) {
            return binder.bind(this);
        }

        Constant makeConstant() {
            return new Constant(value);
        }

        BooleanValue convertToBoolean() { return this; }

        int isomorphicHashCode() {
            return 139;
        }

        /*
        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (obj instanceof BooleanValue) {
                BooleanValue c = (BooleanValue) obj;
                // return value == c.value;
                return true;
            }
            return false;
        }
        */

        boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression) {
            if (!(expression instanceof BooleanValue)) { return false; }

            return super.isIsomorphicTo(identityMap, (Value) expression);
        }

        public String toString() {
            return "boolean";
        }
    }
    public static class IntegerValue extends Value<Integer> {
        static final class Constant extends IntegerValue {
            private Constant(int value) {
                super(value);
            }

            public int inspect(Environment.Binder binder) {
                return super.value;
            }
            public IntegerClosure bind(Environment.Binder binder) {
                return binder.bind(this);
            }

            Constant makeConstant() {
                return this;
            }

            int isomorphicHashCode() {
                return Integer.hashCode(super.value);
            }
            boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression) {
                if (!(expression instanceof Constant)) { return false; }
                if (super.value != ((IntegerValue) expression).value) { return false; }

                return super.isIsomorphicTo(identityMap, (Value) expression);
            }

            public boolean isConstant() {
                return true;
            }

            BooleanValue.Constant convertToBoolean() { return BooleanValue.makeConstant(this.materializeAsBoolean()); }
            IntegerValue.Constant convertToInteger() { return this; }
            LongValue.Constant convertToLong() { return LongValue.makeConstant(this.materializeAsLong()); }
            FloatValue.Constant convertToFloat() { return FloatValue.makeConstant(this.materializeAsFloat()); }
            DoubleValue.Constant convertToDouble() { return DoubleValue.makeConstant(this.materializeAsDouble()); }
            ByteValue.Constant convertToByte() { return ByteValue.makeConstant(this.materializeAsByte()); }
            CharacterValue.Constant convertToCharacter() { return CharacterValue.makeConstant(this.materializeAsCharacter()); }
            ShortValue.Constant convertToShort() { return ShortValue.makeConstant(this.materializeAsShort()); }

            public int hashCode() {
                return Integer.hashCode(super.value);
            }
            public boolean equals(Object obj) {
                if (this == obj) { return true; }
                if (obj instanceof Constant) {
                    return super.value == ((IntegerValue) obj).value;
                }

                return false;
            }

            public String toString() {
                return Integer.toString(super.value);
            }
        }

        private final int value;

        private IntegerValue(int value) {
            this.value = value;
        }
        static IntegerValue make(int value) { return new IntegerValue(value); }
        static Constant makeConstant(int value) { return new Constant(value); }

        final Integer materializeAsObject() { return value; }
        final boolean materializeAsBoolean() {
            if (value == 0) { return false; }
            if (value == 1) { return true; }

            throw new UnsupportedOperationException();
        }
        final int materializeAsInteger() { return value; }
        final long materializeAsLong() { return (long) value; }
        final float materializeAsFloat() { return (float) value; }
        final double materializeAsDouble() { return (double) value; }
        final byte materializeAsByte() { return (byte) value; }
        final char materializeAsCharacter() { return (char) value; }
        final short materializeAsShort() { return (short) value; }

        public final void accept(Expression.Visitor visitor) {
            visitor.visit(this);
        }
        public final void accept(Value.Visitor visitor) {
            visitor.visit(this);
        }
        public int inspect(Environment.Binder binder) {
            binder.setInspectionOccurred();
            return value;
        }
        public IntegerClosure bind(Environment.Binder binder) {
            return binder.bind(this);
        }

        Constant makeConstant() {
            return new Constant(value);
        }

        IntegerValue convertToInteger() { return this; }

        int isomorphicHashCode() {
            return 149;
        }

        /*
        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (obj instanceof IntegerValue) {
                IntegerValue c = (IntegerValue) obj;
                // return value == c.value;
                return true;
            }
            return false;
        }
        */

        boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression) {
            if (!(expression instanceof IntegerValue)) { return false; }

            return super.isIsomorphicTo(identityMap, (Value) expression);
        }

        public String toString() {
            return "int";
        }
    }
    public static class LongValue extends Value<Long> {
        static final class Constant extends LongValue {
            private Constant(long value) {
                super(value);
            }

            public long inspect(Environment.Binder binder) {
                return super.value;
            }
            public LongClosure bind(Environment.Binder binder) {
                return binder.bind(this);
            }

            Constant makeConstant() {
                return this;
            }

            int isomorphicHashCode() {
                return Long.hashCode(super.value);
            }
            boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression) {
                if (!(expression instanceof Constant)) { return false; }
                if (super.value != ((LongValue) expression).value) { return false; }

                return super.isIsomorphicTo(identityMap, (Value) expression);
            }

            public boolean isConstant() {
                return true;
            }

            BooleanValue.Constant convertToBoolean() { return BooleanValue.makeConstant(this.materializeAsBoolean()); }
            IntegerValue.Constant convertToInteger() { return IntegerValue.makeConstant(this.materializeAsInteger()); }
            LongValue.Constant convertToLong() { return this; }
            FloatValue.Constant convertToFloat() { return FloatValue.makeConstant(this.materializeAsFloat()); }
            DoubleValue.Constant convertToDouble() { return DoubleValue.makeConstant(this.materializeAsDouble()); }
            ByteValue.Constant convertToByte() { return ByteValue.makeConstant(this.materializeAsByte()); }
            CharacterValue.Constant convertToCharacter() { return CharacterValue.makeConstant(this.materializeAsCharacter()); }
            ShortValue.Constant convertToShort() { return ShortValue.makeConstant(this.materializeAsShort()); }

            public int hashCode() {
                return Long.hashCode(super.value);
            }
            public boolean equals(Object obj) {
                if (this == obj) { return true; }
                if (obj instanceof Constant) {
                    return super.value == ((LongValue) obj).value;
                }

                return false;
            }

            public String toString() {
                return Long.toString(super.value);
            }
        }

        private final long value;

        private LongValue(long value) {
            this.value = value;
        }
        static LongValue make(long value) { return new LongValue(value); }
        static Constant makeConstant(long value) { return new Constant(value); }

        final Long materializeAsObject() { return value; }
        final boolean materializeAsBoolean() { throw new UnsupportedOperationException(); }
        final int materializeAsInteger() { return (int) value; }
        final long materializeAsLong() { return value; }
        final float materializeAsFloat() { return (float) value; }
        final double materializeAsDouble() { return (double) value; }
        final byte materializeAsByte() { return (byte) value; }
        final char materializeAsCharacter() { return (char) value; }
        final short materializeAsShort() { return (short) value; }

        public final void accept(Expression.Visitor visitor) {
            visitor.visit(this);
        }
        public final void accept(Value.Visitor visitor) {
            visitor.visit(this);
        }
        public long inspect(Environment.Binder binder) {
            binder.setInspectionOccurred();
            return value;
        }
        public LongClosure bind(Environment.Binder binder) {
            return binder.bind(this);
        }

        Constant makeConstant() {
            return new Constant(value);
        }

        LongValue convertToLong() { return this; }

        int isomorphicHashCode() {
            return 151;
        }

        /*
        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (obj instanceof LongValue) {
                LongValue c = (LongValue) obj;
                // return value == c.value;
                return true;
            }
            return false;
        }
        */

        boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression) {
            if (!(expression instanceof LongValue)) { return false; }

            return super.isIsomorphicTo(identityMap, (Value) expression);
        }

        public String toString() {
            return "long";
        }
    }
    public static class FloatValue extends Value<Float> {
        static final class Constant extends FloatValue {
            private Constant(float value) {
                super(value);
            }

            public float inspect(Environment.Binder binder) {
                return super.value;
            }
            public FloatClosure bind(Environment.Binder binder) {
                return binder.bind(this);
            }

            Constant makeConstant() {
                return this;
            }

            int isomorphicHashCode() {
                return Float.hashCode(super.value);
            }
            boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression) {
                if (!(expression instanceof Constant)) { return false; }
                if (super.value != ((FloatValue) expression).value) { return false; }

                return super.isIsomorphicTo(identityMap, (Value) expression);
            }

            public boolean isConstant() {
                return true;
            }

            BooleanValue.Constant convertToBoolean() { return BooleanValue.makeConstant(this.materializeAsBoolean()); }
            IntegerValue.Constant convertToInteger() { return IntegerValue.makeConstant(this.materializeAsInteger()); }
            LongValue.Constant convertToLong() { return LongValue.makeConstant(this.materializeAsLong()); }
            FloatValue.Constant convertToFloat() { return this; }
            DoubleValue.Constant convertToDouble() { return DoubleValue.makeConstant(this.materializeAsDouble()); }
            ByteValue.Constant convertToByte() { return ByteValue.makeConstant(this.materializeAsByte()); }
            CharacterValue.Constant convertToCharacter() { return CharacterValue.makeConstant(this.materializeAsCharacter()); }
            ShortValue.Constant convertToShort() { return ShortValue.makeConstant(this.materializeAsShort()); }

            public int hashCode() {
                return Float.hashCode(super.value);
            }
            public boolean equals(Object obj) {
                if (this == obj) { return true; }
                if (obj instanceof Constant) {
                    return super.value == ((FloatValue) obj).value;
                }

                return false;
            }

            public String toString() {
                return Float.toString(super.value);
            }
        }

        private final float value;

        private FloatValue(float value) {
            this.value = value;
        }
        static FloatValue make(float value) { return new FloatValue(value); }
        static Constant makeConstant(float value) { return new Constant(value); }

        final Float materializeAsObject() { return value; }
        final boolean materializeAsBoolean() { throw new UnsupportedOperationException(); }
        final int materializeAsInteger() { return (int) value; }
        final long materializeAsLong() { return (long) value; }
        final float materializeAsFloat() { return value; }
        final double materializeAsDouble() { return (double) value; }
        final byte materializeAsByte() { return (byte) value; }
        final char materializeAsCharacter() { return (char) value; }
        final short materializeAsShort() { return (short) value; }

        public final void accept(Expression.Visitor visitor) {
            visitor.visit(this);
        }
        public final void accept(Value.Visitor visitor) {
            visitor.visit(this);
        }
        public float inspect(Environment.Binder binder) {
            binder.setInspectionOccurred();
            return value;
        }
        public FloatClosure bind(Environment.Binder binder) {
            return binder.bind(this);
        }

        Constant makeConstant() {
            return new Constant(value);
        }

        FloatValue convertToFloat() { return this; }

        int isomorphicHashCode() {
            return 157;
        }

        /*
        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (obj instanceof FloatValue) {
                FloatValue c = (FloatValue) obj;
                // return value == c.value;
                return true;
            }
            return false;
        }
        */

        boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression) {
            if (!(expression instanceof FloatValue)) { return false; }

            return super.isIsomorphicTo(identityMap, (Value) expression);
        }

        public String toString() {
            return "float";
        }
    }
    public static class DoubleValue extends Value<Double> {
        static final class Constant extends DoubleValue {
            private Constant(double value) {
                super(value);
            }

            public double inspect(Environment.Binder binder) {
                return super.value;
            }
            public DoubleClosure bind(Environment.Binder binder) {
                return binder.bind(this);
            }

            Constant makeConstant() {
                return this;
            }

            int isomorphicHashCode() {
                return Double.hashCode(super.value);
            }
            boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression) {
                if (!(expression instanceof Constant)) { return false; }
                if (super.value != ((DoubleValue) expression).value) { return false; }

                return super.isIsomorphicTo(identityMap, (Value) expression);
            }

            public boolean isConstant() {
                return true;
            }

            BooleanValue.Constant convertToBoolean() { return BooleanValue.makeConstant(this.materializeAsBoolean()); }
            IntegerValue.Constant convertToInteger() { return IntegerValue.makeConstant(this.materializeAsInteger()); }
            LongValue.Constant convertToLong() { return LongValue.makeConstant(this.materializeAsLong()); }
            FloatValue.Constant convertToFloat() { return FloatValue.makeConstant(this.materializeAsFloat()); }
            DoubleValue.Constant convertToDouble() { return this; }
            ByteValue.Constant convertToByte() { return ByteValue.makeConstant(this.materializeAsByte()); }
            CharacterValue.Constant convertToCharacter() { return CharacterValue.makeConstant(this.materializeAsCharacter()); }
            ShortValue.Constant convertToShort() { return ShortValue.makeConstant(this.materializeAsShort()); }

            public int hashCode() {
                return Double.hashCode(super.value);
            }
            public boolean equals(Object obj) {
                if (this == obj) { return true; }
                if (obj instanceof Constant) {
                    return super.value == ((DoubleValue) obj).value;
                }

                return false;
            }

            public String toString() {
                return Double.toString(super.value);
            }
        }

        private final double value;

        private DoubleValue(double value) {
            this.value = value;
        }
        static DoubleValue make(double value) { return new DoubleValue(value); }
        static Constant makeConstant(double value) { return new Constant(value); }

        final Double materializeAsObject() { return value; }
        final boolean materializeAsBoolean() { throw new UnsupportedOperationException(); }
        final int materializeAsInteger() { return (int) value; }
        final long materializeAsLong() { return (long) value; }
        final float materializeAsFloat() { return (float) value; }
        final double materializeAsDouble() { return value; }
        final byte materializeAsByte() { return (byte) value; }
        final char materializeAsCharacter() { return (char) value; }
        final short materializeAsShort() { return (short) value; }

        public final void accept(Expression.Visitor visitor) {
            visitor.visit(this);
        }
        public final void accept(Value.Visitor visitor) {
            visitor.visit(this);
        }
        public double inspect(Environment.Binder binder) {
            binder.setInspectionOccurred();
            return value;
        }
        public DoubleClosure bind(Environment.Binder binder) {
            return binder.bind(this);
        }

        Constant makeConstant() {
            return new Constant(value);
        }

        DoubleValue convertToDouble() { return this; }

        int isomorphicHashCode() {
            return 163;
        }

        /*
        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (obj instanceof DoubleValue) {
                DoubleValue c = (DoubleValue) obj;
                // return value == c.value;
                return true;
            }
            return false;
        }
        */

        boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression) {
            if (!(expression instanceof DoubleValue)) { return false; }

            return super.isIsomorphicTo(identityMap, (Value) expression);
        }

        public String toString() {
            return "double";
        }
    }
    public static class ByteValue extends Value<Byte> {
        static final class Constant extends ByteValue {
            private Constant(byte value) {
                super(value);
            }

            public byte inspect(Environment.Binder binder) {
                return super.value;
            }
            public ByteClosure bind(Environment.Binder binder) {
                return binder.bind(this);
            }

            Constant makeConstant() {
                return this;
            }

            int isomorphicHashCode() {
                return Byte.hashCode(super.value);
            }
            boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression) {
                if (!(expression instanceof Constant)) { return false; }
                if (super.value != ((ByteValue) expression).value) { return false; }

                return super.isIsomorphicTo(identityMap, (Value) expression);
            }

            public boolean isConstant() {
                return true;
            }

            BooleanValue.Constant convertToBoolean() { return BooleanValue.makeConstant(this.materializeAsBoolean()); }
            IntegerValue.Constant convertToInteger() { return IntegerValue.makeConstant(this.materializeAsInteger()); }
            LongValue.Constant convertToLong() { return LongValue.makeConstant(this.materializeAsLong()); }
            FloatValue.Constant convertToFloat() { return FloatValue.makeConstant(this.materializeAsFloat()); }
            DoubleValue.Constant convertToDouble() { return DoubleValue.makeConstant(this.materializeAsDouble()); }
            ByteValue.Constant convertToByte() { return this; }
            CharacterValue.Constant convertToCharacter() { return CharacterValue.makeConstant(this.materializeAsCharacter()); }
            ShortValue.Constant convertToShort() { return ShortValue.makeConstant(this.materializeAsShort()); }

            public int hashCode() {
                return Byte.hashCode(super.value);
            }
            public boolean equals(Object obj) {
                if (this == obj) { return true; }
                if (obj instanceof Constant) {
                    return super.value == ((ByteValue) obj).value;
                }

                return false;
            }

            public String toString() {
                return Byte.toString(super.value);
            }
        }

        private final byte value;

        private ByteValue(byte value) {
            this.value = value;
        }
        static ByteValue make(byte value) { return new ByteValue(value); }
        static Constant makeConstant(byte value) { return new Constant(value); }

        final Byte materializeAsObject() { return value; }
        final boolean materializeAsBoolean() { throw new UnsupportedOperationException(); }
        final int materializeAsInteger() { return (int) value; }
        final long materializeAsLong() { return (long) value; }
        final float materializeAsFloat() { return (float) value; }
        final double materializeAsDouble() { return (double) value; }
        final byte materializeAsByte() { return value; }
        final char materializeAsCharacter() { return (char) value; }
        final short materializeAsShort() { return (short) value; }

        public final void accept(Expression.Visitor visitor) {
            visitor.visit(this);
        }
        public final void accept(Value.Visitor visitor) {
            visitor.visit(this);
        }
        public byte inspect(Environment.Binder binder) {
            binder.setInspectionOccurred();
            return value;
        }
        public ByteClosure bind(Environment.Binder binder) {
            return binder.bind(this);
        }

        Constant makeConstant() {
            return new Constant(value);
        }

        ByteValue convertToByte() { return this; }

        int isomorphicHashCode() {
            return 167;
        }

        /*
        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (obj instanceof ByteValue) {
                ByteValue c = (ByteValue) obj;
                // return value == c.value;
                return true;
            }
            return false;
        }
        */

        boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression) {
            if (!(expression instanceof ByteValue)) { return false; }

            return super.isIsomorphicTo(identityMap, (Value) expression);
        }

        public String toString() {
            return "byte";
        }
    }
    public static class CharacterValue extends Value<Character> {
        static final class Constant extends CharacterValue {
            private Constant(char value) {
                super(value);
            }

            public char inspect(Environment.Binder binder) {
                return super.value;
            }
            public CharacterClosure bind(Environment.Binder binder) {
                return binder.bind(this);
            }

            Constant makeConstant() {
                return this;
            }

            int isomorphicHashCode() {
                return Character.hashCode(super.value);
            }
            boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression) {
                if (!(expression instanceof Constant)) { return false; }
                if (super.value != ((CharacterValue) expression).value) { return false; }

                return super.isIsomorphicTo(identityMap, (Value) expression);
            }

            public boolean isConstant() {
                return true;
            }

            BooleanValue.Constant convertToBoolean() { return BooleanValue.makeConstant(this.materializeAsBoolean()); }
            IntegerValue.Constant convertToInteger() { return IntegerValue.makeConstant(this.materializeAsInteger()); }
            LongValue.Constant convertToLong() { return LongValue.makeConstant(this.materializeAsLong()); }
            FloatValue.Constant convertToFloat() { return FloatValue.makeConstant(this.materializeAsFloat()); }
            DoubleValue.Constant convertToDouble() { return DoubleValue.makeConstant(this.materializeAsDouble()); }
            ByteValue.Constant convertToByte() { return ByteValue.makeConstant(this.materializeAsByte()); }
            CharacterValue.Constant convertToCharacter() { return this; }
            ShortValue.Constant convertToShort() { return ShortValue.makeConstant(this.materializeAsShort()); }

            public int hashCode() {
                return Character.hashCode(super.value);
            }
            public boolean equals(Object obj) {
                if (this == obj) { return true; }
                if (obj instanceof Constant) {
                    return super.value == ((CharacterValue) obj).value;
                }

                return false;
            }

            public String toString() {
                return Character.toString(super.value);
            }
        }

        private final char value;

        private CharacterValue(char value) {
            this.value = value;
        }
        static CharacterValue make(char value) { return new CharacterValue(value); }
        static Constant makeConstant(char value) { return new Constant(value); }

        final Character materializeAsObject() { return value; }
        final boolean materializeAsBoolean() { throw new UnsupportedOperationException(); }
        final int materializeAsInteger() { return (int) value; }
        final long materializeAsLong() { return (long) value; }
        final float materializeAsFloat() { return (float) value; }
        final double materializeAsDouble() { return (double) value; }
        final byte materializeAsByte() { return (byte) value; }
        final char materializeAsCharacter() { return value; }
        final short materializeAsShort() { return (short) value; }

        public final void accept(Expression.Visitor visitor) {
            visitor.visit(this);
        }
        public final void accept(Value.Visitor visitor) {
            visitor.visit(this);
        }
        public char inspect(Environment.Binder binder) {
            binder.setInspectionOccurred();
            return value;
        }
        public CharacterClosure bind(Environment.Binder binder) {
            return binder.bind(this);
        }

        Constant makeConstant() {
            return new Constant(value);
        }

        CharacterValue convertToCharacter() { return this; }

        int isomorphicHashCode() {
            return 167;
        }

        /*
        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (obj instanceof CharacterValue) {
                CharacterValue c = (CharacterValue) obj;
                // return value == c.value;
                return true;
            }
            return false;
        }
        */

        boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression) {
            if (!(expression instanceof CharacterValue)) { return false; }

            return super.isIsomorphicTo(identityMap, (Value) expression);
        }

        public String toString() {
            return "char";
        }
    }
    public static class ShortValue extends Value<Short> {
        static final class Constant extends ShortValue {
            private Constant(short value) {
                super(value);
            }

            public short inspect(Environment.Binder binder) {
                return super.value;
            }
            public ShortClosure bind(Environment.Binder binder) {
                return binder.bind(this);
            }

            Constant makeConstant() {
                return this;
            }

            int isomorphicHashCode() {
                return Short.hashCode(super.value);
            }
            boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression) {
                if (!(expression instanceof Constant)) { return false; }
                if (super.value != ((ShortValue) expression).value) { return false; }

                return super.isIsomorphicTo(identityMap, (Value) expression);
            }

            public boolean isConstant() {
                return true;
            }

            BooleanValue.Constant convertToBoolean() { return BooleanValue.makeConstant(this.materializeAsBoolean()); }
            IntegerValue.Constant convertToInteger() { return IntegerValue.makeConstant(this.materializeAsInteger()); }
            LongValue.Constant convertToLong() { return LongValue.makeConstant(this.materializeAsLong()); }
            FloatValue.Constant convertToFloat() { return FloatValue.makeConstant(this.materializeAsFloat()); }
            DoubleValue.Constant convertToDouble() { return DoubleValue.makeConstant(this.materializeAsDouble()); }
            ByteValue.Constant convertToByte() { return ByteValue.makeConstant(this.materializeAsByte()); }
            CharacterValue.Constant convertToCharacter() { return CharacterValue.makeConstant(this.materializeAsCharacter()); }
            ShortValue.Constant convertToShort() { return this; }

            public int hashCode() {
                return Short.hashCode(super.value);
            }
            public boolean equals(Object obj) {
                if (this == obj) { return true; }
                if (obj instanceof Constant) {
                    return super.value == ((ShortValue) obj).value;
                }

                return false;
            }

            public String toString() {
                return Short.toString(super.value);
            }
        }

        private final short value;

        private ShortValue(short value) {
            this.value = value;
        }
        static ShortValue make(short value) { return new ShortValue(value); }
        static Constant makeConstant(short value) { return new Constant(value); }

        final Short materializeAsObject() { return value; }
        final boolean materializeAsBoolean() { throw new UnsupportedOperationException(); }
        final int materializeAsInteger() { return (int) value; }
        final long materializeAsLong() { return (long) value; }
        final float materializeAsFloat() { return (float) value; }
        final double materializeAsDouble() { return (double) value; }
        final byte materializeAsByte() { return (byte) value; }
        final char materializeAsCharacter() { return (char) value; }
        final short materializeAsShort() { return value; }

        public final void accept(Expression.Visitor visitor) {
            visitor.visit(this);
        }
        public final void accept(Value.Visitor visitor) {
            visitor.visit(this);
        }
        public short inspect(Environment.Binder binder) {
            binder.setInspectionOccurred();
            return value;
        }
        public ShortClosure bind(Environment.Binder binder) {
            return binder.bind(this);
        }

        Constant makeConstant() {
            return new Constant(value);
        }

        ShortValue convertToShort() { return this; }

        int isomorphicHashCode() {
            return 173;
        }

        /*
        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (obj instanceof ShortValue) {
                ShortValue c = (ShortValue) obj;
                // return value == c.value;
                return true;
            }
            return false;
        }
        */

        boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, Expression expression) {
            if (!(expression instanceof ShortValue)) { return false; }

            return super.isIsomorphicTo(identityMap, (Value) expression);
        }

        public String toString() {
            return "short";
        }
    }
}
