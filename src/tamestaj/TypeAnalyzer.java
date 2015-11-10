package tamestaj;

import javassist.*;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Descriptor;
import javassist.bytecode.MethodInfo;

import java.util.ArrayList;

// Based on javassist.bytecode.analysis.Executor
final class TypeAnalyzer extends HighLevelAnalyzer<Type, Type, TypeAnalyzer.TypeFrame> {
    static class TypeFrame extends AbstractFrame<Type, Type> {
        TypeFrame(int maxLocals, int maxStack) {
            super(maxLocals, maxStack);
        }

        public Type pop2() {
            if (getStack(0) != Type.TOP) {
                throw new RuntimeException("Two-word type expected but not found!");
            }

            pop();
            Type type = pop();

            return type;
        }

        public void push2(Type value) {
            push(value);
            push(Type.TOP);
        }

        public void setLocal2(int index, Type value) {
            setLocal(index, value);
            setLocal(index + 1, Type.TOP);

            /*
            if (index > 0) {
                Type preIndexLocal = getLocal(index - 1);
                if (preIndexLocal.getSize() == 2) {
                    setLocal(index - 1, Type.TOP);
                }
            }
            */
        }

        public Type getLocal2(int index) {
            if (getLocal(index + 1) != Type.TOP) {
                throw new RuntimeException("Two-word type expected but not found!");
            }

            return getLocal(index);
        }

        private Type mergeLocalValue(Type value, Type withValue) {
            if (withValue == null) {
                return null;
            } else {
                return value.merge(withValue);
            }
        }

        private Type mergeStackValue(Type value, Type withValue) {
            Type merged = value.merge(withValue);

            if (merged == Type.BOGUS) {
                throw new RuntimeException("Operand stacks could not be merged due to differing primitive types.");
            }

            return merged;
        }

        public boolean mergeLocals(TypeFrame withFrame) {
            boolean changed = false;

            for (int i = 0; i < getMaxLocals(); i++) {
                if (getLocal(i) != null) {
                    Type prev = getLocal(i);
                    Type merged = mergeLocalValue(prev, withFrame.getLocal(i));

                    setLocal(i, merged);

                    if (!merged.equals(prev)) {
                        changed = true;
                    }
                }
            }

            return changed;
        }

        public boolean merge(TypeFrame withFrame) {
            boolean changed = mergeLocals(withFrame);

            if (getStackSize() != withFrame.getStackSize()) {
                throw new RuntimeException("Operand stacks could not be merged, they are of different size!");
            }

            for (int i = 0; i < getStackSize(); i++) {
                if (getStack(i) != null) {
                    Type prev = getStack(i);
                    Type merged = mergeStackValue(prev, withFrame.getStack(i));

                    setStack(i, merged);

                    if (!merged.equals(prev)) {
                        changed = true;
                    }
                }
            }

            return changed;
        }

        public TypeFrame copyLocals() {
            TypeFrame frame = new TypeFrame(getMaxLocals(), getMaxStack());
            copyLocalsInto(frame);
            return frame;
        }

        public TypeFrame copy() {
            TypeFrame frame = new TypeFrame(getMaxLocals(), getMaxStack());
            copyInto(frame);
            return frame;
        }
    }

    public class Result {
        private Result() { }

        Type get(SourceIndex sourceIndex) {
            TypeFrame frame;
            if (sourceIndex.isExplicit()) {
                frame = getOutState(sourceIndex.getPosition());
            } else {
                frame = getInState(sourceIndex.getPosition());
            }

            if (sourceIndex.hasLocalIndices()) {
                return frame.getLocal(sourceIndex.getFirstLocalIndex());
            } else {
                return frame.getStack(sourceIndex.getFirstStackOffset());
            }
        }
        Type get(UseIndex useIndex) {
            return getInState(useIndex.getPosition()).getStack(useIndex.getStackOffset());
        }

        Type getStack(int at, int offset) {
            return getInState(at).getStack(offset);
        }
        Type getLocal(int at, int index) {
            return getInState(at).getLocal(index);
        }

        Type getStackAfter(int at, int offset) {
            return getOutState(at).getStack(offset);
        }
        Type getLocalAfter(int at, int index) {
            return getOutState(at).getLocal(index);
        }
    }

    public TypeAnalyzer(CtBehavior behavior) {
        super(behavior);
    }

    public Result getResult() {
        return new Result();
    }

    protected TypeFrame copyState(TypeFrame original) {
        return original.copy();
    }

    protected boolean stateEquals(TypeFrame state, TypeFrame otherState) {
        return state.equals(otherState);
    }

    protected TypeFrame initialState() {
        CtClass clazz = behavior.getDeclaringClass();
        MethodInfo methodInfo = behavior.getMethodInfo2();

        int maxLocals = codeAttribute.getMaxLocals();
        int maxStack = codeAttribute.getMaxStack();

        TypeFrame frame = new TypeFrame(maxLocals, maxStack);

        int pos = 0;
        if (!Modifier.isStatic(behavior.getModifiers())) {
            frame.setLocal(pos, Type.of(clazz));
            pos++;
        }

        CtClass[] parameters;
        try {
            parameters = Descriptor.getParameterTypes(methodInfo.getDescriptor(), clazz.getClassPool());
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }

        for (CtClass parameter : parameters) {
            // Type type = zeroExtend(Type.get(parameters[i]));
            Type type = Type.of(parameter);
            if (type.isTwoWordPrimitive()) {
                frame.setLocal2(pos, type);
                pos += 2;
            } else {
                frame.setLocal(pos, type);
                pos++;
            }
        }

        return frame;
    }

    protected TypeFrame mergeStatesOnCatch(ArrayList<TypeFrame> states, int[] origins, int at, Type caughtException) {
        TypeFrame mergedFrame = states.get(0).copyLocals();

        for (int i = 1; i < states.size(); i++) {
            mergedFrame.mergeLocals(states.get(i));
        }

        mergedFrame.push(caughtException);

        return mergedFrame;
    }

    protected TypeFrame mergeStates(ArrayList<TypeFrame> states, int[] origins, int at) {
        TypeFrame mergedFrame = states.get(0).copy();

        for (int i = 1; i < states.size(); i++) {
            mergedFrame.merge(states.get(i));
        }

        return mergedFrame;
    }


    protected Type createNull(TypeFrame state, int at) throws BadBytecode {
        return Type.NULL;
    }

    protected Type createIntegerConstant(TypeFrame state, int at, int value) throws BadBytecode {
        return Type.INT;
    }

    protected Type createLongConstant(TypeFrame state, int at, long value) throws BadBytecode {
        return Type.LONG;
    }

    protected Type createFloatConstant(TypeFrame state, int at, float value) throws BadBytecode {
        return Type.FLOAT;
    }

    protected Type createDoubleConstant(TypeFrame state, int at, double value) throws BadBytecode {
        return Type.DOUBLE;
    }

    protected Type createByteConstant(TypeFrame state, int at, byte value) throws BadBytecode {
        return Type.INT;
    }

    protected Type createShortConstant(TypeFrame state, int at, short value) throws BadBytecode {
        return Type.INT;
    }

    protected Type createStringConstant(TypeFrame state, int at, String value) throws BadBytecode {
        return STRING_TYPE;
    }

    protected Type createClassConstant(TypeFrame state, int at, CtClass value) throws BadBytecode {
        return CLASS_TYPE;
    }

    protected Type readLocal(TypeFrame state, int at, Type type, int index, Type local) throws BadBytecode {
        verifyAssignable(type, local, at);
        return local;
    }

    protected Type readArray(TypeFrame state, int at, Type componentType, Type array, int arrayOffset, Type index, int indexOffset) throws BadBytecode {
        if (array == Type.NULL) {
            verifyAssignable(Type.INT, index, at);
            if (componentType == Type.OBJECT) {
                return Type.NULL;
            } else {
                return componentType;
            }
        }

        Type type = array.getArrayComponent();

        if (type == null) {
            throw new BadBytecode("Not an array! [position = " + at + "]");
        }

        verifyAssignable(type, type, at);
        verifyAssignable(Type.INT, index, at);

        return type;
    }

    protected Type assignLocal(TypeFrame state, int at, Type type, int index, Type value, int valueOffset) throws BadBytecode {
        if (!(type == Type.OBJECT && value == Type.RETURN_ADDRESS)) {
            verifyAssignable(type, value, at);
        }

        return value;
    }

    protected void assignArray(TypeFrame state, int at, Type type, Type array, int arrayOffset, Type index, int indexOffset, Type value, int valueOffset) throws BadBytecode {
        if (array == Type.NULL) {
            verifyAssignable(Type.INT, index, at);
            return;
        }

        Type componentType = array.getArrayComponent();

        if (componentType == null) {
            throw new BadBytecode("Not an array! [position = " + at + "]");
        }

        verifyAssignable(type, componentType, at);
        verifyAssignable(Type.INT, index, at);

        if (type == Type.OBJECT) {
            verifyAssignable(type, value, at);
        } else {
            verifyAssignable(componentType, value, at);
        }
    }

    protected void pop(TypeFrame state, int at, Type value) throws BadBytecode {
        if (value.isTwoWordPrimitive()) {
            throw new BadBytecode("DUP cannot be used with a two-word value [position = " + at + "]");
        }
    }

    protected Type duplicate(TypeFrame state, int at, Type value) throws BadBytecode {
        if (value.isTwoWordPrimitive()) {
            throw new BadBytecode("DUP cannot be used with a two-word value [position = " + at + "]");
        }

        return value;
    }

    protected Type duplicateX1(TypeFrame state, int at, Type value0, Type value1) throws BadBytecode {
        if (value0.isTwoWordPrimitive() || value1.isTwoWordPrimitive()) {
            throw new BadBytecode("DUP_X1 cannot be used with a two-word values [position = " + at + "]");
        }

        return value0;
    }

    protected Type duplicateX2(TypeFrame state, int at, Type value0, Type value1, Type value2) throws BadBytecode {
        if (value0.isTwoWordPrimitive() || value1.isTwoWordPrimitive() || value2.isTwoWordPrimitive()) {
            throw new BadBytecode("DUP_X2 cannot be used with a two-word values [position = " + at + "]");
        }

        return value0;
    }

    protected Type swap_0(TypeFrame state, int at, Type value0, Type value1) throws BadBytecode {
        if (value0.isTwoWordPrimitive() || value1.isTwoWordPrimitive()) {
            throw new BadBytecode("SWAP cannot be used with a two-word values [position = " + at + "]");
        }

        return value0;
    }

    protected Type performBinaryArithmetic(TypeFrame state, int at, Type type, ArithmeticOperation operation, Type left, int leftOffset, Type right, int rightOffset) throws BadBytecode {
        verifyAssignable(type, left, at);
        verifyAssignable(type, right, at);

        return left;
    }

    protected Type performShift(TypeFrame state, int at, Type type, ShiftOperation operation, Type left, int leftOffset, Type right, int rightOffset) throws BadBytecode {
        verifyAssignable(type, left, at);
        verifyAssignable(Type.INT, right, at);

        return left;
    }

    protected Type performNegation(TypeFrame state, int at, Type type, Type value, int valueOffset) throws BadBytecode {
        verifyAssignable(type, value, at);

        return value;
    }

    protected Type incrementLocal(TypeFrame state, int at, int index, Type local, int increment) throws BadBytecode {
        verifyAssignable(Type.INT, local, at);

        return local;
    }

    protected Type convertType(TypeFrame state, int at, Type from, Type to, Type value, int valueOffset) throws BadBytecode {
        // Casting can actually create intersection types (when allowed), e.g. (Comparable & Iterable) or (Comparable) (Iterable)
        // This is one of the reasons (the other being statefulness) we had to use a simpler representation than
        // javassist.bytecode.analysis.Type which does not handle intersections. This is okay because the JVM verifier
        // allows any reference to be assigned to interfaces i.e. handle them like Objects (JVM specification,
        // 4.10.1.2. Verification Type System).

        verifyAssignable(from, value, at);

        return to;
    }

    protected Type compare(TypeFrame state, int at, Type type, Type left, int leftOffset, Type right, int rightOffset) throws BadBytecode {
        verifyAssignable(type, left, at);
        verifyAssignable(type, right, at);

        return Type.INT;
    }

    protected Type compare(TypeFrame state, int at, Type type, ComparisonOperation operation, Type left, int leftOffset, Type right, int rightOffset) throws BadBytecode {
        verifyAssignable(type, left, at);
        verifyAssignable(type, right, at);

        return Type.INT;
    }

    protected void branchIf(TypeFrame state, int at, Type type, ComparisonOperation operation, Type value, int valueOffset, int trueTarget, int falseTarget) throws BadBytecode {
        verifyAssignable(type, value, at);
    }

    protected void branchIfCompare(TypeFrame state, int at, Type type, ComparisonOperation operation, Type left, int leftOffset, Type right, int rightOffset, int trueTarget, int falseTarget) throws BadBytecode {
        verifyAssignable(type, left, at);
        verifyAssignable(type, right, at);
    }

    protected void branchGoto(TypeFrame state, int at, int target) throws BadBytecode {
        // Nothing
    }

    protected Type callSubroutine(TypeFrame state, int at, int target) throws BadBytecode {
        return Type.RETURN_ADDRESS;
    }

    protected void returnFromSubroutine(TypeFrame state, int at, int index, Type local) throws BadBytecode {
        verifyAssignable(Type.RETURN_ADDRESS, local, at);
    }

    protected void branchTableSwitch(TypeFrame state, int at, Type index, int indexOffset, int defaultTarget, int[]
            indexedTargets) throws BadBytecode {
        verifyAssignable(Type.INT, index, at);
    }

    protected void branchLookupSwitch(TypeFrame state, int at, Type key, int keyOffset, int defaultTarget, int[] matches, int[] matchTargets) throws BadBytecode {
        verifyAssignable(Type.INT, key, at);
    }

    protected void returnFromMethod(TypeFrame state, int at, Type type, Type value, int valueOffset) throws BadBytecode {
        verifyAssignable(type, value, at);
    }

    protected void returnFromMethod(TypeFrame state, int at) throws BadBytecode {
        // Nothing
    }

    protected Type readStaticField(TypeFrame state, int at, Type classType, Type fieldType, CtField field) throws BadBytecode {
        return fieldType;
    }

    protected void assignStaticField(TypeFrame state, int at, Type classType, Type fieldType, CtField field, Type value, int valueOffset) throws BadBytecode {
        verifyAssignable(fieldType, value, at);
    }

    protected Type readField(TypeFrame state, int at, Type targetType, Type fieldType, CtField field, Type targetObject, int targetObjectOffset) throws BadBytecode {
        verifyAssignable(targetType, targetObject, at);

        return fieldType;
    }

    protected void assignField(TypeFrame state, int at, CtField field, Type targetType, Type fieldType, Type targetObject, int targetObjectOffset, Type value, int valueOffset) throws BadBytecode {
        verifyAssignable(fieldType, value, at);
        verifyAssignable(targetType, targetObject, at);
    }

    protected Type invokeStaticMethod(TypeFrame state, int at, CtMethod method, Type returnType, Type[] paramTypes, ArrayList<Type> arguments, int[] argumentOffsets) throws BadBytecode {
        for (int i = 0; i < paramTypes.length; i++) {
            verifyAssignable(paramTypes[i], arguments.get(i), at);
        }

        return returnType;
    }

    protected Type invokeMethod(TypeFrame state, int at, CtMethod method, Type targetType, Type returnType, Type[] paramTypes, Type targetObject, int targetObjectOffset, ArrayList<Type> arguments, int[] argumentOffsets) throws BadBytecode {
        for (int i = 0; i < paramTypes.length; i++) {
            verifyAssignable(paramTypes[i], arguments.get(i), at);
        }

        verifyAssignable(targetType, targetObject, at);

        return returnType;
    }

    protected void invokeConstructor(TypeFrame state, int at, CtConstructor constructor, Type targetType, Type[] paramTypes, Type targetObject, int targetObjectOffset, ArrayList<Type> arguments, int[] argumentOffsets) throws BadBytecode {
        for (int i = 0; i < paramTypes.length; i++) {
            verifyAssignable(paramTypes[i], arguments.get(i), at);
        }

        verifyAssignable(targetType, targetObject, at);
    }

    protected Type invokeDynamic(TypeFrame state, int at, CtMethod bootstrapMethod, StaticArgument[] staticArguments, Type returnType, Type[] paramTypes, ArrayList<Type> arguments, int[] argumentOffsets) throws BadBytecode {
        for (int i = 0; i < paramTypes.length; i++) {
            verifyAssignable(paramTypes[i], arguments.get(i), at);
        }

        return returnType;
    }

    protected Type newInstance(TypeFrame state, int at, Type type) throws BadBytecode {
        return type;
    }

    protected Type arrayLength(TypeFrame state, int at, Type array, int arrayOffset) throws BadBytecode {
        if (!array.isArray() && array != Type.NULL) {
            throw new BadBytecode("ARRAYLENGTH was passed a non-array [position = " + at + "]: " + array);
        }

        return Type.INT;
    }

    protected Type throwException(TypeFrame state, int at, Type throwable, int throwableOffset) throws BadBytecode {
        verifyAssignable(THROWABLE_TYPE, throwable, at);
        return throwable;
    }

    protected Type instanceOf(TypeFrame state, int at, Type ofType, Type value, int valueOffset) throws BadBytecode {
        verifyAssignable(Type.OBJECT, value, at);
        return Type.INT;
    }

    protected void enterSynchronized(TypeFrame state, int at, Type monitor, int monitorOffset) throws BadBytecode {
        verifyAssignable(Type.OBJECT, monitor, at);
    }

    protected void exitSynchronized(TypeFrame state, int at, Type monitor, int monitorOffset) throws BadBytecode {
        verifyAssignable(Type.OBJECT, monitor, at);
    }

    protected Type newArray(TypeFrame state, int at, Type componentType, ArrayList<Type> lengths, int[] lengthOffsets) throws BadBytecode {
        for (Object length : lengths) {
            verifyAssignable(Type.INT, (Type) length, at);
        }

        return componentType;
    }

    private static Type zeroExtend(Type type) {
        if (type == Type.SHORT || type == Type.BYTE || type == Type.CHAR || type == Type.BOOLEAN) {
            return Type.INT;
        }

        return type;
    }

    private static void verifyAssignable(Type expected, Type type, int at) throws BadBytecode {
        if (!zeroExtend(expected).isAssignableFrom(zeroExtend(type))) {
            throw new BadBytecode("Expected type: " + expected + " Got: " + type + " [position = " + at + "]");
        }
    }
}
