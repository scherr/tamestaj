package tamestaj;

import javassist.*;
import javassist.bytecode.BadBytecode;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;

final class ConstantAnalyzer extends HighLevelAnalyzerWithBoxingUnboxing<ConstantAnalyzer.Constant, ConstantAnalyzer.Constant, ConstantAnalyzer.ConstantFrame> {
    private static final Method FIND_LOADED_CLASS_METHOD;
    private static final CtMethod LAMBDA_METAFACTORY;

    static {
        Method m = null;
        try {
            m = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            m.setAccessible(true);
        } catch (NoSuchMethodException e) {
            // e.printStackTrace();
        }
        FIND_LOADED_CLASS_METHOD = m;

        ClassPool cp = ClassPool.getDefault();
        CtMethod l = null;
        try {
            l = cp.getMethod("java.lang.invoke.LambdaMetafactory", "metafactory");
        } catch (NotFoundException e) {
            // e.printStackTrace();
        }
        LAMBDA_METAFACTORY = l;
    }

    static final class Constant {
        private final Object value;
        private final boolean isReference;
        private final boolean isConcrete;

        private Constant(Object value, boolean isReference, boolean isConcrete) {
            this.value = value;
            this.isReference = isReference;
            this.isConcrete = isConcrete;
        }

        public String toString() {
            return "(" + Objects.toString(value) + ", " + isReference + ", " + isConcrete + ")";
        }

        public int hashCode() {
            return Objects.hash(value, isReference, isConcrete);
        }

        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (!(obj instanceof Constant)) { return false; }
            Constant c = (Constant) obj;

            if (isConcrete != c.isConcrete) { return false; }
            if (value == null && c.value == null) { return true; }
            if (isReference != c.isReference || (isConcrete && isReference)) { return false; }
            return Objects.equals(value, c.value);
        }
    }

    static final class ConstantFrame extends AbstractFrame<Constant, Constant> {
        private ConstantFrame(int maxLocals, int maxStack) {
            super(maxLocals, maxStack);
        }

        public void mergeLocals(ConstantFrame withFrame) {
            for (int i = 0; i < getMaxLocals(); i++) {
                Object prev = getLocal(i);
                if (prev != null) {
                    if (!Objects.equals(prev, withFrame.getLocal(i))) {
                        setLocal(i, null);
                    }
                }
            }
        }

        public void merge(ConstantFrame withFrame) {
            mergeLocals(withFrame);

            for (int i = 0; i < getStackSize(); i++) {
                Object prev = getStack(i);
                if (prev != null) {
                    if (!Objects.equals(prev, withFrame.getStack(i))) {
                        setStack(i, null);
                    }
                }
            }
        }

        public ConstantFrame copyLocals() {
            ConstantFrame frame = new ConstantFrame(getMaxLocals(), getMaxStack());
            copyLocalsInto(frame);
            return frame;
        }

        public ConstantFrame copy() {
            ConstantFrame frame = new ConstantFrame(getMaxLocals(), getMaxStack());
            copyInto(frame);
            return frame;
        }
    }

    class Result {
        private Result() { }

        boolean isConstant(SourceIndex sourceIndex) {
            ConstantFrame frame;
            if (sourceIndex.isExplicit()) {
                frame = getOutState(sourceIndex.getPosition());
            } else {
                frame = getInState(sourceIndex.getPosition());
            }

            if (sourceIndex.hasLocalIndices()) {
                return frame.getLocal(sourceIndex.getFirstLocalIndex()) != null;
            } else {
                return frame.getStack(sourceIndex.getFirstStackOffset()) != null;
            }
        }
        boolean isConstant(UseIndex useIndex) {
            return getInState(useIndex.getPosition()).getStack(useIndex.getStackOffset()) != null;
        }

        boolean isConstantStack(int at, int offset) {
            return getInState(at).getStack(offset) != null;
        }
        boolean isConstantLocal(int at, int index) {
            return getInState(at).getLocal(index) != null;
        }

        boolean isConstantStackAfter(int at, int offset) {
            return getOutState(at).getStack(offset) != null;
        }
        boolean isConstantLocalAfter(int at, int index) {
            return getOutState(at).getLocal(index) != null;
        }
    }

    ConstantAnalyzer(TypeAnalyzer typeAnalyzer) {
        super(typeAnalyzer);
    }

    Result getResult() {
        return new Result();
    }

    protected ConstantFrame copyState(ConstantFrame original) {
        return original.copy();
    }

    protected boolean stateEquals(ConstantFrame state, ConstantFrame otherState) {
        return state.equals(otherState);
    }

    protected ConstantFrame initialState() {
        int maxLocals = codeAttribute.getMaxLocals();
        int maxStack = codeAttribute.getMaxStack();

        return new ConstantFrame(maxLocals, maxStack);
    }

    protected ConstantFrame mergeStatesOnCatch(ArrayList<ConstantFrame> states, int[] origins, int at, Type caughtException) {
        ConstantFrame mergedFrame = states.get(0).copyLocals();

        for (int i = 1; i < states.size(); i++) {
            mergedFrame.mergeLocals(states.get(i));
        }

        mergedFrame.push(null);

        return mergedFrame;
    }

    protected ConstantFrame mergeStates(ArrayList<ConstantFrame> states, int[] origins, int at) {
        ConstantFrame mergedFrame = states.get(0).copy();

        for (int i = 1; i < states.size(); i++) {
            mergedFrame.merge(states.get(i));
        }

        return mergedFrame;
    }


    protected Constant createNull(ConstantFrame state, int at) throws BadBytecode {
        return new Constant(null, true, true);
    }

    protected Constant createIntegerConstant(ConstantFrame state, int at, int value) throws BadBytecode {
        return new Constant(value, false, true);
    }

    protected Constant createLongConstant(ConstantFrame state, int at, long value) throws BadBytecode {
        return new Constant(value, false, true);
    }

    protected Constant createFloatConstant(ConstantFrame state, int at, float value) throws BadBytecode {
        return new Constant(value, false, true);
    }

    protected Constant createDoubleConstant(ConstantFrame state, int at, double value) throws BadBytecode {
        return new Constant(value, false, true);
    }

    protected Constant createByteConstant(ConstantFrame state, int at, byte value) throws BadBytecode {
        return new Constant((int) value, false, true);
    }

    protected Constant createShortConstant(ConstantFrame state, int at, short value) throws BadBytecode {
        return new Constant((int) value, false, true);
    }

    protected Constant createStringConstant(ConstantFrame state, int at, String value) throws BadBytecode {
        return new Constant(value, false, true);
    }

    protected Constant createClassConstant(ConstantFrame state, int at, CtClass value) throws BadBytecode {
        return new Constant(value, false, true);
    }

    protected Constant readLocal(ConstantFrame state, int at, Type type, int index, Constant local) throws BadBytecode {
        return local;
    }

    protected Constant readArray(ConstantFrame state, int at, Type componentType, Constant array, int arrayOffset, Constant index, int indexOffset) throws BadBytecode {
        return null;
    }

    protected Constant assignLocal(ConstantFrame state, int at, Type type, int index, Constant value, int valueOffset) throws BadBytecode {
        return value;
    }

    protected void assignArray(ConstantFrame state, int at, Type componentType, Constant array, int arrayOffset, Constant index, int indexOffset, Constant value, int valueOffset) throws BadBytecode {

    }

    protected Constant performBinaryArithmetic(ConstantFrame state, int at, Type type, ArithmeticOperation operation, Constant left, int leftOffset, Constant right, int rightOffset) throws BadBytecode {
        if (left != null && left.isConcrete && left.value instanceof Number && right != null && right.isConcrete && right.value instanceof Number) {
            Number leftValue = (Number) left.value;
            Number rightValue = (Number) right.value;

            if (type.equals(Type.INT)) {
                int l = leftValue.intValue();
                int r = rightValue.intValue();
                switch (operation) {
                    case ADDITION:       return new Constant(l + r, false, true);
                    case SUBTRACTION:    return new Constant(l - r, false, true);
                    case MULTIPLICATION: return new Constant(l * r, false, true);
                    case DIVISION:       return r == 0 ? null : new Constant(l / r, false, true);
                    case REMAINDER:      return r == 0 ? null : new Constant(l % r, false, true);
                    case AND:            return new Constant(l & r, false, true);
                    case OR:             return new Constant(l | r, false, true);
                    case XOR:            return new Constant(l ^ r, false, true);
                }
            } else if (type.equals(Type.LONG)) {
                long l = leftValue.longValue();
                long r = rightValue.longValue();
                switch (operation) {
                    case ADDITION:       return new Constant(l + r, false, true);
                    case SUBTRACTION:    return new Constant(l - r, false, true);
                    case MULTIPLICATION: return new Constant(l * r, false, true);
                    case DIVISION:       return r == 0L ? null : new Constant(l / r, false, true);
                    case REMAINDER:      return r == 0L ? null : new Constant(l % r, false, true);
                    case AND:            return new Constant(l & r, false, true);
                    case OR:             return new Constant(l | r, false, true);
                    case XOR:            return new Constant(l ^ r, false, true);
                }
            } else if (type.equals(Type.FLOAT)) {
                float l = leftValue.floatValue();
                float r = rightValue.floatValue();
                switch (operation) {
                    case ADDITION:       return new Constant(l + r, false, true);
                    case SUBTRACTION:    return new Constant(l - r, false, true);
                    case MULTIPLICATION: return new Constant(l * r, false, true);
                    case DIVISION:       return new Constant(l / r, false, true);
                    case REMAINDER:      return new Constant(l % r, false, true);
                }
            } else if (type.equals(Type.DOUBLE)) {
                double l = leftValue.doubleValue();
                double r = rightValue.doubleValue();
                switch (operation) {
                    case ADDITION:       return new Constant(l + r, false, true);
                    case SUBTRACTION:    return new Constant(l - r, false, true);
                    case MULTIPLICATION: return new Constant(l * r, false, true);
                    case DIVISION:       return new Constant(l / r, false, true);
                    case REMAINDER:      return new Constant(l % r, false, true);
                }
            }
        }

        return null;
    }

    protected Constant performShift(ConstantFrame state, int at, Type type, ShiftOperation operation, Constant left, int leftOffset, Constant right, int rightOffset) throws BadBytecode {
        return null;
    }

    protected Constant performNegation(ConstantFrame state, int at, Type type, Constant value, int valueOffset) throws BadBytecode {
        if (value != null && value.isConcrete && value.value instanceof Number) {
            Number number = (Number) value.value;
            if (type.equals(Type.INT)) {
                int v = number.intValue();
                return new Constant(-v, false, true);
            } else if (type.equals(Type.LONG)) {
                long v = number.longValue();
                return new Constant(-v, false, true);
            } else if (type.equals(Type.FLOAT)) {
                float v = number.floatValue();
                return new Constant(-v, false, true);
            } else if (type.equals(Type.DOUBLE)) {
                double v = number.doubleValue();
                return new Constant(-v, false, true);
            }
        }

        return null;
    }

    protected Constant incrementLocal(ConstantFrame state, int at, int index, Constant local, int increment) throws BadBytecode {
        if (local != null && local.isConcrete && local.value instanceof Number) {
            int v = ((Number) local.value).intValue();
            return new Constant(v + increment, false, true);
        }

        return null;
    }

    protected Constant convertType(ConstantFrame state, int at, Type from, Type to, Constant value, int valueOffset) throws BadBytecode {
        if (to.isReference()) {
            if (value != null) {
                if (value.isReference) {
                    return value;
                } else {
                    return new Constant(value.value, true, true);
                }
            }
        } else {
            if (value != null && value.value != null) {
                if (value.isConcrete && value.value instanceof Number) {
                    Number v = (Number) value.value;
                    if (to.equals(Type.BOOLEAN)) {
                        // return new Constant(v.intValue() != 0, false, true);
                        return new Constant(v.intValue(), false, true);
                    } else if (to.equals(Type.INT)) {
                        return new Constant(v.intValue(), false, true);
                    } else if (to.equals(Type.FLOAT)) {
                        return new Constant(v.floatValue(), false, true);
                    } else if (to.equals(Type.DOUBLE)) {
                        return new Constant(v.doubleValue(), false, true);
                    } else if (to.equals(Type.BYTE)) {
                        return new Constant((int) v.byteValue(), false, true);
                    } else if (to.equals(Type.CHAR)) {
                        return new Constant((int) (char) v.intValue(), false, true);
                    } else if (to.equals(Type.SHORT)) {
                        return new Constant((int) v.shortValue(), false, true);
                    }
                } else if (value.value instanceof CtField) {
                    try {
                        if (Util.isSafeConversion(behavior, Type.of(((CtField) value.value).getType()), to)) {
                            if (value.isReference) {
                                return new Constant(value.value, false, true);
                            } else {
                                return value;
                            }
                        }
                    } catch (NotFoundException e) {
                        // e.printStackTrace();
                    }
                }
            }
        }

        return null;
    }

    protected Constant compare(ConstantFrame state, int at, Type type, Constant left, int leftOffset, Constant right, int rightOffset) throws BadBytecode {
        return null;
    }

    protected Constant compare(ConstantFrame state, int at, Type type, ComparisonOperation operation, Constant left, int leftOffset, Constant right, int rightOffset) throws BadBytecode {
        return null;
    }

    protected void branchIf(ConstantFrame state, int at, Type type, ComparisonOperation operation, Constant value, int valueOffset, int trueTarget, int falseTarget) throws BadBytecode {

    }

    protected void branchIfCompare(ConstantFrame state, int at, Type type, ComparisonOperation operation, Constant left, int leftOffset, Constant right, int rightOffset, int trueTarget, int falseTarget) throws BadBytecode {
        /*
        if (left != null && left.isConcrete && right != null && right.isConcrete) {
            Comparable<Object> leftValue = (Comparable) left.value;
            Comparable<Object> rightValue = (Comparable) right.value;;
            int result = leftValue.compareTo(rightValue);
        }
        */
    }

    protected void branchGoto(ConstantFrame state, int at, int target) throws BadBytecode {

    }

    protected Constant callSubroutine(ConstantFrame state, int at, int target) throws BadBytecode {
        return null;
    }

    protected void returnFromSubroutine(ConstantFrame state, int at, int index, Constant local) throws BadBytecode {

    }

    protected void branchTableSwitch(ConstantFrame state, int at, Constant index, int indexOffset, int defaultTarget, int[]
            indexedTargets) throws BadBytecode {
    }

    protected void branchLookupSwitch(ConstantFrame state, int at, Constant key, int keyOffset, int defaultTarget, int[] matches, int[] matchTargets) throws BadBytecode {
    }

    protected void returnFromMethod(ConstantFrame state, int at, Type type, Constant value, int valueOffset) throws BadBytecode {

    }

    protected void returnFromMethod(ConstantFrame state, int at) throws BadBytecode {

    }

    protected Constant readStaticField(ConstantFrame state, int at, Type classType, Type fieldType, CtField field) throws BadBytecode {
        if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()) && StageAnnotation.forField(field) == null) {
            /* if (!fieldType.isReference() ||
                    STRING_TYPE.isAssignableFrom(fieldType) ||
                    Util.BOOLEAN_OBJECT_TYPE.isAssignableFrom(fieldType) ||
                    Util.INTEGER_OBJECT_TYPE.isAssignableFrom(fieldType) ||
                    Util.LONG_OBJECT_TYPE.isAssignableFrom(fieldType) ||
                    Util.FLOAT_OBJECT_TYPE.isAssignableFrom(fieldType) ||
                    Util.DOUBLE_OBJECT_TYPE.isAssignableFrom(fieldType) ||
                    Util.CHARACTER_OBJECT_TYPE.isAssignableFrom(fieldType) ||
                    Util.SHORT_OBJECT_TYPE.isAssignableFrom(fieldType)) { */
                if (FIND_LOADED_CLASS_METHOD != null) {
                    try {
                        Class<?> c = null;
                        // Class<?> c = (Class) FIND_LOADED_CLASS_METHOD.invoke(ClassLoader.getSystemClassLoader(), classType.getCtClass().getName());
                        // TODO: We need to find a way to actually figure out whether a class has been initiated or not, e.g. in the agent add a registry process at the end of all static initializer
                        if (c != null) {
                            Field f = c.getDeclaredField(field.getName());
                            Object o = f.get(null);
                            if (f.getType().isPrimitive()) {
                                if (o instanceof Character) {
                                    o = (int) ((Character) o).charValue();
                                } else if (o instanceof Byte || o instanceof Short) {
                                    o = ((Number) o).intValue();
                                } else if (o instanceof Boolean) {
                                    o = (Boolean) o ? 1 : 0;
                                }
                                return new Constant(o, false, true);
                            } else {
                                return new Constant(o, true, true);
                            }
                        }
                    } catch (IllegalAccessException | NoSuchFieldException e) {
                        // e.printStackTrace();
                    }
                }

                return new Constant(field, fieldType.isReference(), false);
            // }
        }

        return null;
    }

    protected void assignStaticField(ConstantFrame state, int at, Type classType, Type fieldType, CtField field, Constant value, int valueOffset) throws BadBytecode {

    }

    protected Constant readField(ConstantFrame state, int at, Type targetType, Type fieldType, CtField field, Constant targetObject, int targetObjectOffset) throws BadBytecode {
        return null;
    }

    protected void assignField(ConstantFrame state, int at, CtField field, Type targetType, Type fieldType, Constant targetObject, int targetObjectOffset, Constant value, int valueOffset) throws BadBytecode {

    }

    protected Constant invokeStaticMethodExceptBoxing(ConstantFrame state, int at, CtMethod method, Type returnType, Type[] paramTypes, ArrayList<Constant> arguments, int[] argumentOffsets) throws BadBytecode {
        return null;
    }

    protected Constant invokeMethodExceptUnboxing(ConstantFrame state, int at, CtMethod method, Type targetType, Type returnType, Type[] paramTypes, Constant targetObject, int targetObjectOffset, ArrayList<Constant> arguments, int[] argumentOffsets) throws BadBytecode {
        return null;
    }

    protected void invokeConstructor(ConstantFrame state, int at, CtConstructor constructor, Type targetType, Type[] paramTypes, Constant targetObject, int targetObjectOffset, ArrayList<Constant> arguments, int[] argumentOffsets) throws BadBytecode {

    }

    protected Constant invokeDynamic(ConstantFrame state, int at, CtMethod bootstrapMethod, StaticArgument[] staticArguments, Type returnType, Type[] paramTypes, ArrayList<Constant> arguments, int[] argumentOffsets) throws BadBytecode {
        // Experimental!!! Lambda expressions should not be considered in terms of constant or not since they can
        // produce different instances, i.e. different identities. What we detect here are behavioral constants...
        //
        // See below for why some cases are still covered (for the current JDK!)
        if (LAMBDA_METAFACTORY != null && LAMBDA_METAFACTORY.equals(bootstrapMethod)) {
            /*
            for (Constant a : arguments) {
                if (a == null || a.isReference) {
                    // If the lambda expression takes a dynamic non-constant argument or a reference it cannot even be considered constant in behavior
                    // (the reference case is due to potential object identity checks in the expression itself which might change behavior)
                    return null;
                }
            }
            */

            if (arguments.size() > 0) {
                // It seems non-capturing lambda expressions are in fact producing identical instances (but
                // JVM-implementation dependent) so making sure there are no arguments ensures that we can consider
                // a lambda expression result as constant
                return null;
            }

            // Maybe we need to use all static arguments, but for now we just use the method handle which is found at index 1
            return new Constant(staticArguments[1], true, false);
        }

        return null;
    }

    protected Constant newInstance(ConstantFrame state, int at, Type type) throws BadBytecode {
        return null;
    }

    protected Constant arrayLength(ConstantFrame state, int at, Constant array, int arrayOffset) throws BadBytecode {
        return null;
    }

    protected Constant throwException(ConstantFrame state, int at, Constant throwable, int throwableOffset) throws BadBytecode {
        return null;
    }

    protected Constant instanceOf(ConstantFrame state, int at, Type ofType, Constant value, int valueOffset) throws BadBytecode {
        return null;
    }

    protected void enterSynchronized(ConstantFrame state, int at, Constant monitor, int monitorOffset) throws BadBytecode {

    }

    protected void exitSynchronized(ConstantFrame state, int at, Constant monitor, int monitorOffset) throws BadBytecode {

    }

    protected Constant newArray(ConstantFrame state, int at, Type componentType, ArrayList<Constant> lengths, int[] lengthOffsets) throws BadBytecode {
        return null;
    }
}
