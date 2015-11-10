package tamestaj;

import javassist.*;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.BootstrapMethodsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;

import java.util.ArrayList;

/*
 * Based on the abstract interpretation found in javassist.bytecode.analysis.Executor
 *
 * For details refer to the original in:
 *
 * www.javassist.org
 *
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 */

@SuppressWarnings("unused")
abstract class HighLevelAnalyzer<L, S, F extends Frame<L, S>> extends LowLevelAnalyzer<F> {
    protected enum ArithmeticOperation {
        ADDITION,
        SUBTRACTION,
        MULTIPLICATION,
        DIVISION,
        REMAINDER,
        AND,
        OR,
        XOR
    }

    protected enum ShiftOperation {
        SHIFT_LEFT,
        SHIFT_RIGHT,
        UNSIGNED_SHIFT_RIGHT
    }

    protected enum ComparisonOperation {
        EQUAL,
        NOT_EQUAL,
        LESS_THAN,
        LESS_OR_EQUAL,
        GREATER_THAN,
        GREATER_OR_EQUAL,
        IS_NULL,
        IS_NON_NULL
    }

    protected HighLevelAnalyzer(CtBehavior behavior) {
        super(behavior);
    }

    protected HighLevelAnalyzer(Analyzer<?> analyzer) {
        super(analyzer);
    }

    protected abstract S createNull(F state, int at) throws BadBytecode;
    protected abstract S createIntegerConstant(F state, int at, int value) throws BadBytecode;
    protected abstract S createLongConstant(F state, int at, long value) throws BadBytecode;
    protected abstract S createFloatConstant(F state, int at, float value) throws BadBytecode;
    protected abstract S createDoubleConstant(F state, int at, double value) throws BadBytecode;
    protected abstract S createByteConstant(F state, int at, byte value) throws BadBytecode;
    protected abstract S createShortConstant(F state, int at, short value) throws BadBytecode;
    protected abstract S createStringConstant(F state, int at, String value) throws BadBytecode;
    protected abstract S createClassConstant(F state, int at, CtClass value) throws BadBytecode;
    protected abstract S readLocal(F state, int at, Type type, int index, L local) throws BadBytecode;
    protected abstract S readArray(F state, int at, Type componentType, S array, int arrayOffset, S index, int indexOffset) throws BadBytecode;
    protected abstract L assignLocal(F state, int at, Type type, int index, S value, int valueOffset) throws BadBytecode;
    protected abstract void assignArray(F state, int at, Type componentType, S array, int arrayOffset, S index, int indexOffset, S value, int valueOffset) throws BadBytecode;
    protected void pop(F state, int at, S value) throws BadBytecode {

    }
    protected void pop2(F state, int at, S value0, S value1) throws BadBytecode {

    }
    protected S duplicate(F state, int at, S value) throws BadBytecode {
        return value;
    }
    protected S duplicateX1(F state, int at, S value0, S value1) throws BadBytecode {
        return value0;
    }
    protected S duplicateX2(F state, int at, S value0, S value1, S value2) throws BadBytecode {
        return value0;
    }
    protected S duplicate2_0(F state, int at, S value0, S value1) throws BadBytecode {
        return value0;
    }
    protected S duplicate2_1(F state, int at, S value0, S value1) throws BadBytecode {
        return value1;
    }
    protected S duplicate2_X1_0(F state, int at, S value0, S value1, S value2) throws BadBytecode {
        return value0;
    }
    protected S duplicate2_X1_1(F state, int at, S value0, S value1, S value2) throws BadBytecode {
        return value1;
    }
    protected S duplicate2_X2_0(F state, int at, S value0, S value1, S value2, S value3) throws BadBytecode {
        return value0;
    }
    protected S duplicate2_X2_1(F state, int at, S value0, S value1, S value2, S value3) throws BadBytecode {
        return value1;
    }
    protected S swap_0(F state, int at, S value0, S value1) throws BadBytecode {
        return value0;
    }
    protected S swap_1(F state, int at, S value0, S value1) throws BadBytecode {
        return value1;
    }
    protected abstract S performBinaryArithmetic(F state, int at, Type type, ArithmeticOperation operation, S left, int leftOffset, S right, int rightOffset) throws BadBytecode;
    protected abstract S performShift(F state, int at, Type type, ShiftOperation operation, S left, int leftOffset, S right, int rightOffset) throws BadBytecode;
    protected abstract S performNegation(F state, int at, Type type, S value, int valueOffset) throws BadBytecode;
    protected abstract L incrementLocal(F state, int at, int index, L local, int increment) throws BadBytecode;
    protected abstract S convertType(F state, int at, Type from, Type to, S value, int valueOffset) throws BadBytecode;
    protected abstract S compare(F state, int at, Type type, S left, int leftOffset, S right, int rightOffset) throws BadBytecode;
    protected abstract S compare(F state, int at, Type type, ComparisonOperation operation, S left, int leftOffset, S right, int rightOffset) throws BadBytecode;
    protected abstract void branchIf(F state, int at, Type type, ComparisonOperation operation, S value, int valueOffset, int trueTarget, int falseTarget) throws BadBytecode;
    protected abstract void branchIfCompare(F state, int at, Type type, ComparisonOperation operation, S left, int leftOffset, S right, int rightOffset, int trueTarget, int falseTarget) throws BadBytecode;
    protected abstract void branchGoto(F state, int at, int target) throws BadBytecode;
    protected abstract S callSubroutine(F state, int at, int target) throws BadBytecode;
    protected abstract void returnFromSubroutine(F state, int at, int index, L local) throws BadBytecode;
    protected abstract void branchTableSwitch(F state, int at, S index, int indexOffset, int defaultTarget, int[]
            indexedTargets) throws BadBytecode;
    protected abstract void branchLookupSwitch(F state, int at, S key, int keyOffset, int defaultTarget, int[] matches, int[] matchTargets) throws BadBytecode;
    protected abstract void returnFromMethod(F state, int at, Type type, S value, int valueOffset) throws BadBytecode;
    protected abstract void returnFromMethod(F state, int at) throws BadBytecode;
    protected abstract S readStaticField(F state, int at, Type classType, Type fieldType, CtField field) throws BadBytecode;
    protected abstract void assignStaticField(F state, int at, Type classType, Type fieldType, CtField field, S value, int valueOffset) throws BadBytecode;
    protected abstract S readField(F state, int at, Type targetType, Type fieldType, CtField field, S targetObject, int targetObjectOffset) throws BadBytecode;
    protected abstract void assignField(F state, int at, CtField field, Type targetType, Type fieldType, S targetObject, int targetObjectOffset, S value, int valueOffset) throws BadBytecode;
    protected abstract S invokeStaticMethod(F state, int at, CtMethod method, Type returnType, Type[] paramTypes, ArrayList<S> arguments, int[] argumentOffsets) throws BadBytecode;
    protected abstract S invokeMethod(F state, int at, CtMethod method, Type targetType, Type returnType, Type[] paramTypes, S targetObject, int targetObjectOffset, ArrayList<S> arguments, int[] argumentOffsets) throws BadBytecode;
    protected abstract void invokeConstructor(F state, int at, CtConstructor constructor, Type targetType, Type[] paramTypes, S targetObject, int targetObjectOffset, ArrayList<S> arguments, int[] argumentOffsets) throws BadBytecode;
    protected abstract S invokeDynamic(F state, int at, CtMethod bootstrapMethod, StaticArgument[] staticArguments, Type returnType, Type[] paramTypes, ArrayList<S> arguments, int[] argumentOffsets) throws BadBytecode;
    protected abstract S newInstance(F state, int at, Type type) throws BadBytecode;
    protected abstract S arrayLength(F state, int at, S array, int arrayOffset) throws BadBytecode;
    protected abstract S throwException(F state, int at, S throwable, int throwableOffset) throws BadBytecode;
    protected abstract S instanceOf(F state, int at, Type ofType, S value, int valueOffset) throws BadBytecode;
    protected abstract void enterSynchronized(F state, int at, S monitor, int monitorOffset) throws BadBytecode;
    protected abstract void exitSynchronized(F state, int at, S monitor, int monitorOffset) throws BadBytecode;
    protected abstract S newArray(F state, int at, Type componentType, ArrayList<S> lengths, int[] lengthOffsets) throws BadBytecode;

    protected void transferNOP(F state, int at) throws BadBytecode {

    }

    protected void transferACONST_NULL(F state, int at) throws BadBytecode {
        state.push(createNull(state, at));
    }

    protected void transferICONST_M1(F state, int at) throws BadBytecode {
        state.push(createIntegerConstant(state, at, -1));
    }

    protected void transferICONST_0(F state, int at) throws BadBytecode {
        state.push(createIntegerConstant(state, at, 0));
    }

    protected void transferICONST_1(F state, int at) throws BadBytecode {
        state.push(createIntegerConstant(state, at, 1));
    }

    protected void transferICONST_2(F state, int at) throws BadBytecode {
        state.push(createIntegerConstant(state, at, 2));
    }

    protected void transferICONST_3(F state, int at) throws BadBytecode {
        state.push(createIntegerConstant(state, at, 3));
    }

    protected void transferICONST_4(F state, int at) throws BadBytecode {
        state.push(createIntegerConstant(state, at, 4));
    }

    protected void transferICONST_5(F state, int at) throws BadBytecode {
        state.push(createIntegerConstant(state, at, 5));
    }

    protected void transferLCONST_0(F state, int at) throws BadBytecode {
        state.push2(createLongConstant(state, at, 0L));
    }

    protected void transferLCONST_1(F state, int at) throws BadBytecode {
        state.push2(createLongConstant(state, at, 1L));
    }

    protected void transferFCONST_0(F state, int at) throws BadBytecode {
        state.push(createFloatConstant(state, at, 0.0f));
    }

    protected void transferFCONST_1(F state, int at) throws BadBytecode {
        state.push(createFloatConstant(state, at, 1.0f));
    }

    protected void transferFCONST_2(F state, int at) throws BadBytecode {
        state.push(createFloatConstant(state, at, 2.0f));
    }

    protected void transferDCONST_0(F state, int at) throws BadBytecode {
        state.push2(createDoubleConstant(state, at, 0.0));
    }

    protected void transferDCONST_1(F state, int at) throws BadBytecode {
        state.push2(createDoubleConstant(state, at, 1.0));
    }

    protected void transferBIPUSH(F state, int at) throws BadBytecode {
        state.push(createByteConstant(state, at, (byte) codeIterator.byteAt(at + 1)));
    }

    protected void transferSIPUSH(F state, int at) throws BadBytecode {
        state.push(createShortConstant(state, at, (short) codeIterator.u16bitAt(at + 1)));
    }

    protected void transferLDC(F state, int at) throws BadBytecode {
        int index = codeIterator.byteAt(at + 1);
        int tag = constPool.getTag(index);
        S value;

        switch (tag) {
            case ConstPool.CONST_String:
                value = createStringConstant(state, at, constPool.getStringInfo(index));
                break;
            case ConstPool.CONST_Integer:
                value = createIntegerConstant(state, at, constPool.getIntegerInfo(index));
                break;
            case ConstPool.CONST_Float:
                value = createFloatConstant(state, at, constPool.getFloatInfo(index));
                break;
            case ConstPool.CONST_Class:
                value = createClassConstant(state, at, resolveClassInfo(constPool.getClassInfo(index), at).getCtClass());
                break;
            default:
                throw new BadBytecode("Bad LDC [position = " + at + "]: " + tag);
        }

        state.push(value);
    }

    protected void transferLDC_W(F state, int at) throws BadBytecode {
        int index = codeIterator.u16bitAt(at + 1);
        int tag = constPool.getTag(index);
        S value;

        switch (tag) {
            case ConstPool.CONST_String:
                value = createStringConstant(state, at, constPool.getStringInfo(index));
                break;
            case ConstPool.CONST_Integer:
                value = createIntegerConstant(state, at, constPool.getIntegerInfo(index));
                break;
            case ConstPool.CONST_Float:
                value = createFloatConstant(state, at, constPool.getFloatInfo(index));
                break;
            case ConstPool.CONST_Class:
                value = createClassConstant(state, at, resolveClassInfo(constPool.getClassInfo(index), at).getCtClass());
                break;
            default:
                throw new BadBytecode("Bad LDC [position = " + at + "]: " + tag);
        }

        state.push(value);
    }

    protected void transferLDC2_W(F state, int at) throws BadBytecode {
        int index = codeIterator.u16bitAt(at + 1);
        int tag = constPool.getTag(index);
        S value;

        switch (tag) {
            case ConstPool.CONST_Long:
                value = createLongConstant(state, at, constPool.getLongInfo(index));
                break;
            case ConstPool.CONST_Double:
                value = createDoubleConstant(state, at, constPool.getDoubleInfo(index));
                break;
            default:
                throw new BadBytecode("Bad LDC_W [position = " + at + "]: " + tag);
        }

        state.push2(value);
    }

    protected void transferILOAD(F state, int at, boolean isWide) throws BadBytecode {
        int index = isWide ? codeIterator.u16bitAt(at + 2) : codeIterator.byteAt(at + 1);
        state.push(readLocal(state, at, Type.INT, index, state.getLocal(index)));
    }

    protected void transferLLOAD(F state, int at, boolean isWide) throws BadBytecode {
        int index = isWide ? codeIterator.u16bitAt(at + 2) : codeIterator.byteAt(at + 1);
        state.push2(readLocal(state, at, Type.LONG, index, state.getLocal2(index)));
    }

    protected void transferFLOAD(F state, int at, boolean isWide) throws BadBytecode {
        int index = isWide ? codeIterator.u16bitAt(at + 2) : codeIterator.byteAt(at + 1);
        state.push(readLocal(state, at, Type.FLOAT, index, state.getLocal(index)));
    }

    protected void transferDLOAD(F state, int at, boolean isWide) throws BadBytecode {
        int index = isWide ? codeIterator.u16bitAt(at + 2) : codeIterator.byteAt(at + 1);
        state.push2(readLocal(state, at, Type.DOUBLE, index, state.getLocal2(index)));
    }

    protected void transferALOAD(F state, int at, boolean isWide) throws BadBytecode {
        int index = isWide ? codeIterator.u16bitAt(at + 2) : codeIterator.byteAt(at + 1);
        state.push(readLocal(state, at, Type.OBJECT, index, state.getLocal(index)));
    }

    protected void transferILOAD_0(F state, int at) throws BadBytecode {
        state.push(readLocal(state, at, Type.INT, 0, state.getLocal(0)));
    }

    protected void transferILOAD_1(F state, int at) throws BadBytecode {
        state.push(readLocal(state, at, Type.INT, 1, state.getLocal(1)));
    }

    protected void transferILOAD_2(F state, int at) throws BadBytecode {
        state.push(readLocal(state, at, Type.INT, 2, state.getLocal(2)));
    }

    protected void transferILOAD_3(F state, int at) throws BadBytecode {
        state.push(readLocal(state, at, Type.INT, 3, state.getLocal(3)));
    }

    protected void transferLLOAD_0(F state, int at) throws BadBytecode {
        state.push2(readLocal(state, at, Type.LONG, 0, state.getLocal2(0)));
    }

    protected void transferLLOAD_1(F state, int at) throws BadBytecode {
        state.push2(readLocal(state, at, Type.LONG, 1, state.getLocal2(1)));
    }

    protected void transferLLOAD_2(F state, int at) throws BadBytecode {
        state.push2(readLocal(state, at, Type.LONG, 2, state.getLocal2(2)));
    }

    protected void transferLLOAD_3(F state, int at) throws BadBytecode {
        state.push2(readLocal(state, at, Type.LONG, 3, state.getLocal2(3)));
    }

    protected void transferFLOAD_0(F state, int at) throws BadBytecode {
        state.push(readLocal(state, at, Type.FLOAT, 0, state.getLocal(0)));
    }

    protected void transferFLOAD_1(F state, int at) throws BadBytecode {
        state.push(readLocal(state, at, Type.FLOAT, 1, state.getLocal(1)));
    }

    protected void transferFLOAD_2(F state, int at) throws BadBytecode {
        state.push(readLocal(state, at, Type.FLOAT, 2, state.getLocal(2)));
    }

    protected void transferFLOAD_3(F state, int at) throws BadBytecode {
        state.push(readLocal(state, at, Type.FLOAT, 3, state.getLocal(3)));
    }

    protected void transferDLOAD_0(F state, int at) throws BadBytecode {
        state.push2(readLocal(state, at, Type.DOUBLE, 0, state.getLocal2(0)));
    }

    protected void transferDLOAD_1(F state, int at) throws BadBytecode {
        state.push2(readLocal(state, at, Type.DOUBLE, 1, state.getLocal2(1)));
    }

    protected void transferDLOAD_2(F state, int at) throws BadBytecode {
        state.push2(readLocal(state, at, Type.DOUBLE, 2, state.getLocal2(2)));
    }

    protected void transferDLOAD_3(F state, int at) throws BadBytecode {
        state.push2(readLocal(state, at, Type.DOUBLE, 3, state.getLocal2(3)));
    }

    protected void transferALOAD_0(F state, int at) throws BadBytecode {
        state.push(readLocal(state, at, Type.OBJECT, 0, state.getLocal(0)));
    }

    protected void transferALOAD_1(F state, int at) throws BadBytecode {
        state.push(readLocal(state, at, Type.OBJECT, 1, state.getLocal(1)));
    }

    protected void transferALOAD_2(F state, int at) throws BadBytecode {
        state.push(readLocal(state, at, Type.OBJECT, 2, state.getLocal(2)));
    }

    protected void transferALOAD_3(F state, int at) throws BadBytecode {
        state.push(readLocal(state, at, Type.OBJECT, 3, state.getLocal(3)));
    }

    protected void transferIALOAD(F state, int at) throws BadBytecode {
        S index = state.pop();
        S array = state.pop();

        state.push(readArray(state, at, Type.INT, array, 1, index, 0));
    }

    protected void transferLALOAD(F state, int at) throws BadBytecode {
        S index = state.pop();
        S array = state.pop();

        state.push2(readArray(state, at, Type.LONG, array, 1, index, 0));
    }

    protected void transferFALOAD(F state, int at) throws BadBytecode {
        S index = state.pop();
        S array = state.pop();

        state.push(readArray(state, at, Type.FLOAT, array, 1, index, 0));
    }

    protected void transferDALOAD(F state, int at) throws BadBytecode {
        S index = state.pop();
        S array = state.pop();

        state.push2(readArray(state, at, Type.DOUBLE, array, 1, index, 0));
    }

    protected void transferAALOAD(F state, int at) throws BadBytecode {
        S index = state.pop();
        S array = state.pop();

        state.push(readArray(state, at, Type.OBJECT, array, 1, index, 0));
    }

    protected void transferBALOAD(F state, int at) throws BadBytecode {
        S index = state.pop();
        S array = state.pop();

        state.push(readArray(state, at, Type.BYTE, array, 1, index, 0));
    }

    protected void transferCALOAD(F state, int at) throws BadBytecode {
        S index = state.pop();
        S array = state.pop();

        state.push(readArray(state, at, Type.CHAR, array, 1, index, 0));
    }

    protected void transferSALOAD(F state, int at) throws BadBytecode {
        S index = state.pop();
        S array = state.pop();

        state.push(readArray(state, at, Type.SHORT, array, 1, index, 0));
    }

    protected void transferISTORE(F state, int at, boolean isWide) throws BadBytecode {
        int index = isWide ? codeIterator.u16bitAt(at + 2) : codeIterator.byteAt(at + 1);
        S value = state.pop();

        state.setLocal(index, assignLocal(state, at, Type.INT, index, value, 0));
    }

    protected void transferLSTORE(F state, int at, boolean isWide) throws BadBytecode {
        int index = isWide ? codeIterator.u16bitAt(at + 2) : codeIterator.byteAt(at + 1);
        S value = state.pop2();

        state.setLocal2(index, assignLocal(state, at, Type.LONG, index, value, 1));
    }

    protected void transferFSTORE(F state, int at, boolean isWide) throws BadBytecode {
        int index = isWide ? codeIterator.u16bitAt(at + 2) : codeIterator.byteAt(at + 1);
        S value = state.pop();

        state.setLocal(index, assignLocal(state, at, Type.FLOAT, index, value, 0));
    }

    protected void transferDSTORE(F state, int at, boolean isWide) throws BadBytecode {
        int index = isWide ? codeIterator.u16bitAt(at + 2) : codeIterator.byteAt(at + 1);
        S value = state.pop2();

        state.setLocal2(index, assignLocal(state, at, Type.DOUBLE, index, value, 1));
    }

    protected void transferASTORE(F state, int at, boolean isWide) throws BadBytecode {
        int index = isWide ? codeIterator.u16bitAt(at + 2) : codeIterator.byteAt(at + 1);
        S value = state.pop();

        state.setLocal(index, assignLocal(state, at, Type.OBJECT, index, value, 0));
    }

    protected void transferISTORE_0(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.setLocal(0, assignLocal(state, at, Type.INT, 0, value, 0));
    }

    protected void transferISTORE_1(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.setLocal(1, assignLocal(state, at, Type.INT, 1, value, 0));
    }

    protected void transferISTORE_2(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.setLocal(2, assignLocal(state, at, Type.INT, 2, value, 0));
    }

    protected void transferISTORE_3(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.setLocal(3, assignLocal(state, at, Type.INT, 3, value, 0));
    }

    protected void transferLSTORE_0(F state, int at) throws BadBytecode {
        S value = state.pop2();

        state.setLocal2(0, assignLocal(state, at, Type.LONG, 0, value, 1));
    }

    protected void transferLSTORE_1(F state, int at) throws BadBytecode {
        S value = state.pop2();

        state.setLocal2(1, assignLocal(state, at, Type.LONG, 1, value, 1));
    }

    protected void transferLSTORE_2(F state, int at) throws BadBytecode {
        S value = state.pop2();

        state.setLocal2(2, assignLocal(state, at, Type.LONG, 2, value, 1));
    }

    protected void transferLSTORE_3(F state, int at) throws BadBytecode {
        S value = state.pop2();

        state.setLocal2(3, assignLocal(state, at, Type.LONG, 3, value, 1));
    }

    protected void transferFSTORE_0(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.setLocal(0, assignLocal(state, at, Type.FLOAT, 0, value, 0));
    }

    protected void transferFSTORE_1(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.setLocal(1, assignLocal(state, at, Type.FLOAT, 1, value, 0));
    }

    protected void transferFSTORE_2(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.setLocal(2, assignLocal(state, at, Type.FLOAT, 2, value, 0));
    }

    protected void transferFSTORE_3(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.setLocal(3, assignLocal(state, at, Type.FLOAT, 3, value, 0));
    }

    protected void transferDSTORE_0(F state, int at) throws BadBytecode {
        S value = state.pop2();

        state.setLocal2(0, assignLocal(state, at, Type.DOUBLE, 0, value, 1));
    }

    protected void transferDSTORE_1(F state, int at) throws BadBytecode {
        S value = state.pop2();

        state.setLocal2(1, assignLocal(state, at, Type.DOUBLE, 1, value, 1));
    }

    protected void transferDSTORE_2(F state, int at) throws BadBytecode {
        S value = state.pop2();

        state.setLocal2(2, assignLocal(state, at, Type.DOUBLE, 2, value, 1));
    }

    protected void transferDSTORE_3(F state, int at) throws BadBytecode {
        S value = state.pop2();

        state.setLocal2(3, assignLocal(state, at, Type.DOUBLE, 3, value, 1));
    }

    protected void transferASTORE_0(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.setLocal(0, assignLocal(state, at, Type.OBJECT, 0, value, 0));
    }

    protected void transferASTORE_1(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.setLocal(1, assignLocal(state, at, Type.OBJECT, 1, value, 0));
    }

    protected void transferASTORE_2(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.setLocal(2, assignLocal(state, at, Type.OBJECT, 2, value, 0));
    }

    protected void transferASTORE_3(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.setLocal(3, assignLocal(state, at, Type.OBJECT, 3, value, 0));
    }

    protected void transferIASTORE(F state, int at) throws BadBytecode {
        S value = state.pop();
        S index = state.pop();
        S array = state.pop();

        assignArray(state, at, Type.INT, array, 2, index, 1, value, 0);
    }

    protected void transferLASTORE(F state, int at) throws BadBytecode {
        S value = state.pop2();
        S index = state.pop();
        S array = state.pop();

        assignArray(state, at, Type.LONG, array, 3, index, 2, value, 1);
    }

    protected void transferFASTORE(F state, int at) throws BadBytecode {
        S value = state.pop();
        S index = state.pop();
        S array = state.pop();

        assignArray(state, at, Type.FLOAT, array, 2, index, 1, value, 0);
    }

    protected void transferDASTORE(F state, int at) throws BadBytecode {
        S value = state.pop2();
        S index = state.pop();
        S array = state.pop();

        assignArray(state, at, Type.DOUBLE, array, 3, index, 2, value, 1);
    }

    protected void transferAASTORE(F state, int at) throws BadBytecode {
        S value = state.pop();
        S index = state.pop();
        S array = state.pop();

        assignArray(state, at, Type.OBJECT, array, 2, index, 1, value, 0);
    }

    protected void transferBASTORE(F state, int at) throws BadBytecode {
        S value = state.pop();
        S index = state.pop();
        S array = state.pop();

        assignArray(state, at, Type.BYTE, array, 2, index, 1, value, 0);
    }

    protected void transferCASTORE(F state, int at) throws BadBytecode {
        S value = state.pop();
        S index = state.pop();
        S array = state.pop();

        assignArray(state, at, Type.CHAR, array, 2, index, 1, value, 0);
    }

    protected void transferSASTORE(F state, int at) throws BadBytecode {
        S value = state.pop();
        S index = state.pop();
        S array = state.pop();

        assignArray(state, at, Type.SHORT, array, 2, index, 1, value, 0);
    }

    protected void transferPOP(F state, int at) throws BadBytecode {
        S value = state.pop();

        pop(state, at, value);
    }

    protected void transferPOP2(F state, int at) throws BadBytecode {
        S value0 = state.pop();
        S value1 = state.pop();

        pop2(state, at, value0, value1);
    }

    protected void transferDUP(F state, int at) throws BadBytecode {
        S value = state.peek();

        state.push(duplicate(state, at, value));
    }

    protected void transferDUP_X1(F state, int at) throws BadBytecode {
        S value0 = state.peek();
        S value1 = state.getStack(1);

        S dup = duplicateX1(state, at, value0, value1);

        state.push(value0);

        state.setStack(1, value1);
        state.setStack(2, dup);
    }

    protected void transferDUP_X2(F state, int at) throws BadBytecode {
        S value0 = state.peek();
        S value1 = state.getStack(1);
        S value2 = state.getStack(2);

        S dup = duplicateX2(state, at, value0, value1, value2);

        state.push(value0);

        state.setStack(1, value1);
        state.setStack(2, value2);
        state.setStack(3, dup);
    }

    protected void transferDUP2(F state, int at) throws BadBytecode {
        S value0 = state.peek();
        S value1 = state.getStack(1);

        S dup0 = duplicate2_0(state, at, value0, value1);
        S dup1 = duplicate2_1(state, at, value0, value1);

        state.push(dup1);
        state.push(dup0);
    }

    protected void transferDUP2_X1(F state, int at) throws BadBytecode {
        S value0 = state.peek();
        S value1 = state.getStack(1);
        S value2 = state.getStack(2);

        S dup0 = duplicate2_X1_0(state, at, value0, value1, value2);
        S dup1 = duplicate2_X1_1(state, at, value0, value1, value2);

        state.push(value1);
        state.push(value0);

        state.setStack(2, value2);
        state.setStack(3, dup0);
        state.setStack(4, dup1);
    }

    protected void transferDUP2_X2(F state, int at) throws BadBytecode {
        S value0 = state.peek();
        S value1 = state.getStack(1);
        S value2 = state.getStack(2);
        S value3 = state.getStack(3);

        S dup0 = duplicate2_X2_0(state, at, value0, value1, value2, value3);
        S dup1 = duplicate2_X2_1(state, at, value0, value1, value2, value3);

        state.push(value1);
        state.push(value0);

        state.setStack(2, value2);
        state.setStack(3, value3);
        state.setStack(4, dup0);
        state.setStack(5, dup1);
    }

    protected void transferSWAP(F state, int at) throws BadBytecode {
        S value0 = state.peek();
        S value1 = state.getStack(1);

        S swap0 = swap_0(state, at, value0, value1);
        S swap1 = swap_1(state, at, value0, value1);

        state.setStack(0, swap1);
        state.setStack(1, swap0);
    }

    private void transferArithmetic(F state, int at, Type type, ArithmeticOperation operation) throws BadBytecode {
        S right = state.pop();
        S left = state.pop();

        state.push(performBinaryArithmetic(state, at, type, operation, left, 1, right, 0));
    }

    private void transferArithmetic2(F state, int at, Type type, ArithmeticOperation operation) throws BadBytecode {
        S right = state.pop2();
        S left = state.pop2();

        state.push2(performBinaryArithmetic(state, at, type, operation, left, 3, right, 1));
    }

    protected void transferIADD(F state, int at) throws BadBytecode {
        transferArithmetic(state, at, Type.INT, ArithmeticOperation.ADDITION);
    }

    protected void transferLADD(F state, int at) throws BadBytecode {
        transferArithmetic2(state, at, Type.LONG, ArithmeticOperation.ADDITION);
    }

    protected void transferFADD(F state, int at) throws BadBytecode {
        transferArithmetic(state, at, Type.FLOAT, ArithmeticOperation.ADDITION);
    }

    protected void transferDADD(F state, int at) throws BadBytecode {
        transferArithmetic2(state, at, Type.DOUBLE, ArithmeticOperation.ADDITION);
    }

    protected void transferISUB(F state, int at) throws BadBytecode {
        transferArithmetic(state, at, Type.INT, ArithmeticOperation.SUBTRACTION);
    }

    protected void transferLSUB(F state, int at) throws BadBytecode {
        transferArithmetic2(state, at, Type.LONG, ArithmeticOperation.SUBTRACTION);
    }

    protected void transferFSUB(F state, int at) throws BadBytecode {
        transferArithmetic(state, at, Type.FLOAT, ArithmeticOperation.SUBTRACTION);
    }

    protected void transferDSUB(F state, int at) throws BadBytecode {
        transferArithmetic2(state, at, Type.DOUBLE, ArithmeticOperation.SUBTRACTION);
    }

    protected void transferIMUL(F state, int at) throws BadBytecode {
        transferArithmetic(state, at, Type.INT, ArithmeticOperation.MULTIPLICATION);
    }

    protected void transferLMUL(F state, int at) throws BadBytecode {
        transferArithmetic2(state, at, Type.LONG, ArithmeticOperation.MULTIPLICATION);
    }

    protected void transferFMUL(F state, int at) throws BadBytecode {
        transferArithmetic(state, at, Type.FLOAT, ArithmeticOperation.MULTIPLICATION);
    }

    protected void transferDMUL(F state, int at) throws BadBytecode {
        transferArithmetic2(state, at, Type.DOUBLE, ArithmeticOperation.MULTIPLICATION);
    }

    protected void transferIDIV(F state, int at) throws BadBytecode {
        transferArithmetic(state, at, Type.INT, ArithmeticOperation.DIVISION);
    }

    protected void transferLDIV(F state, int at) throws BadBytecode {
        transferArithmetic2(state, at, Type.LONG, ArithmeticOperation.DIVISION);
    }

    protected void transferFDIV(F state, int at) throws BadBytecode {
        transferArithmetic(state, at, Type.FLOAT, ArithmeticOperation.DIVISION);
    }

    protected void transferDDIV(F state, int at) throws BadBytecode {
        transferArithmetic2(state, at, Type.DOUBLE, ArithmeticOperation.DIVISION);
    }

    protected void transferIREM(F state, int at) throws BadBytecode {
        transferArithmetic(state, at, Type.INT, ArithmeticOperation.REMAINDER);
    }

    protected void transferLREM(F state, int at) throws BadBytecode {
        transferArithmetic2(state, at, Type.LONG, ArithmeticOperation.REMAINDER);
    }

    protected void transferFREM(F state, int at) throws BadBytecode {
        transferArithmetic(state, at, Type.FLOAT, ArithmeticOperation.REMAINDER);
    }

    protected void transferDREM(F state, int at) throws BadBytecode {
        transferArithmetic2(state, at, Type.DOUBLE, ArithmeticOperation.REMAINDER);
    }

    protected void transferINEG(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.push(performNegation(state, at, Type.INT, value, 0));
    }

    protected void transferLNEG(F state, int at) throws BadBytecode {
        S value = state.pop2();

        state.push2(performNegation(state, at, Type.LONG, value, 1));
    }

    protected void transferFNEG(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.push(performNegation(state, at, Type.FLOAT, value, 0));
    }

    protected void transferDNEG(F state, int at) throws BadBytecode {
        S value = state.pop2();

        state.push2(performNegation(state, at, Type.DOUBLE, value, 1));
    }

    protected void transferISHL(F state, int at) throws BadBytecode {
        S right = state.pop();
        S left = state.pop();

        state.push(performShift(state, at, Type.INT, ShiftOperation.SHIFT_LEFT, left, 1, right, 0));
    }

    protected void transferLSHL(F state, int at) throws BadBytecode {
        S right = state.pop();
        S left = state.pop2();

        state.push2(performShift(state, at, Type.LONG, ShiftOperation.SHIFT_LEFT, left, 2, right, 0));
    }

    protected void transferISHR(F state, int at) throws BadBytecode {
        S right = state.pop();
        S left = state.pop();

        state.push(performShift(state, at, Type.INT, ShiftOperation.SHIFT_RIGHT, left, 1, right, 0));
    }

    protected void transferLSHR(F state, int at) throws BadBytecode {
        S right = state.pop();
        S left = state.pop2();

        state.push2(performShift(state, at, Type.LONG, ShiftOperation.SHIFT_RIGHT, left, 2, right, 0));
    }

    protected void transferIUSHR(F state, int at) throws BadBytecode {
        S right = state.pop();
        S left = state.pop();

        state.push(performShift(state, at, Type.INT, ShiftOperation.UNSIGNED_SHIFT_RIGHT, left, 1, right, 0));
    }

    protected void transferLUSHR(F state, int at) throws BadBytecode {
        S right = state.pop();
        S left = state.pop2();

        state.push2(performShift(state, at, Type.LONG, ShiftOperation.UNSIGNED_SHIFT_RIGHT, left, 2, right, 0));
    }

    protected void transferIAND(F state, int at) throws BadBytecode {
        transferArithmetic(state, at, Type.INT, ArithmeticOperation.AND);
    }

    protected void transferLAND(F state, int at) throws BadBytecode {
        transferArithmetic2(state, at, Type.LONG, ArithmeticOperation.AND);
    }

    protected void transferIOR(F state, int at) throws BadBytecode {
        transferArithmetic(state, at, Type.INT, ArithmeticOperation.OR);
    }

    protected void transferLOR(F state, int at) throws BadBytecode {
        transferArithmetic2(state, at, Type.LONG, ArithmeticOperation.OR);
    }

    protected void transferIXOR(F state, int at) throws BadBytecode {
        transferArithmetic(state, at, Type.INT, ArithmeticOperation.XOR);
    }

    protected void transferLXOR(F state, int at) throws BadBytecode {
        transferArithmetic2(state, at, Type.LONG, ArithmeticOperation.XOR);
    }

    protected void transferIINC(F state, int at, boolean isWide) throws BadBytecode {
        int index = isWide ? codeIterator.u16bitAt(at + 2) : codeIterator.byteAt(at + 1);
        int increment = isWide ? codeIterator.byteAt(at + 4) : codeIterator.byteAt(at + 2);
        state.setLocal(index, incrementLocal(state, at, index, state.getLocal(index), increment));
    }

    protected void transferI2L(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.push2(convertType(state, at, Type.INT, Type.LONG, value, 0));
    }

    protected void transferI2F(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.push(convertType(state, at, Type.INT, Type.FLOAT, value, 0));
    }

    protected void transferI2D(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.push2(convertType(state, at, Type.INT, Type.DOUBLE, value, 0));
    }

    protected void transferL2I(F state, int at) throws BadBytecode {
        S value = state.pop2();

        state.push(convertType(state, at, Type.LONG, Type.INT, value, 1));
    }

    protected void transferL2F(F state, int at) throws BadBytecode {
        S value = state.pop2();

        state.push(convertType(state, at, Type.LONG, Type.FLOAT, value, 1));
    }

    protected void transferL2D(F state, int at) throws BadBytecode {
        S value = state.pop2();

        state.push2(convertType(state, at, Type.LONG, Type.DOUBLE, value, 1));
    }

    protected void transferF2I(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.push(convertType(state, at, Type.FLOAT, Type.INT, value, 0));
    }

    protected void transferF2L(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.push2(convertType(state, at, Type.FLOAT, Type.LONG, value, 0));
    }

    protected void transferF2D(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.push2(convertType(state, at, Type.FLOAT, Type.DOUBLE, value, 0));
    }

    protected void transferD2I(F state, int at) throws BadBytecode {
        S value = state.pop2();

        state.push(convertType(state, at, Type.DOUBLE, Type.INT, value, 1));
    }

    protected void transferD2L(F state, int at) throws BadBytecode {
        S value = state.pop2();

        state.push2(convertType(state, at, Type.DOUBLE, Type.LONG, value, 1));
    }

    protected void transferD2F(F state, int at) throws BadBytecode {
        S value = state.pop2();

        state.push(convertType(state, at, Type.DOUBLE, Type.FLOAT, value, 1));
    }

    protected void transferI2B(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.push(convertType(state, at, Type.INT, Type.BYTE, value, 0));
    }

    protected void transferI2C(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.push(convertType(state, at, Type.INT, Type.CHAR, value, 0));
    }

    protected void transferI2S(F state, int at) throws BadBytecode {
        S value = state.pop();

        state.push(convertType(state, at, Type.INT, Type.SHORT, value, 0));
    }

    protected void transferLCMP(F state, int at) throws BadBytecode {
        S right = state.pop2();
        S left = state.pop2();

        state.push(compare(state, at, Type.LONG, left, 3, right, 1));
    }

    protected void transferFCMPL(F state, int at) throws BadBytecode {
        S right = state.pop();
        S left = state.pop();

        state.push(compare(state, at, Type.FLOAT, ComparisonOperation.LESS_THAN, left, 1, right, 0));
    }

    protected void transferFCMPG(F state, int at) throws BadBytecode {
        S right = state.pop();
        S left = state.pop();

        state.push(compare(state, at, Type.FLOAT, ComparisonOperation.GREATER_THAN, left, 1, right, 0));
    }

    protected void transferDCMPL(F state, int at) throws BadBytecode {
        S right = state.pop2();
        S left = state.pop2();

        state.push(compare(state, at, Type.DOUBLE, ComparisonOperation.LESS_THAN, left, 3, right, 1));
    }

    protected void transferDCMPG(F state, int at) throws BadBytecode {
        S right = state.pop2();
        S left = state.pop2();

        state.push(compare(state, at, Type.DOUBLE, ComparisonOperation.GREATER_THAN, left, 3, right, 1));
    }

    protected void transferIFEQ(F state, int at) throws BadBytecode {
        S value = state.pop();
        int trueTarget = at + codeIterator.s16bitAt(at + 1);
        int falseTarget = at + 3;

        branchIf(state, at, Type.INT, ComparisonOperation.EQUAL, value, 0, trueTarget, falseTarget);
    }

    protected void transferIFNE(F state, int at) throws BadBytecode {
        S value = state.pop();
        int trueTarget = at + codeIterator.s16bitAt(at + 1);
        int falseTarget = at + 3;

        branchIf(state, at, Type.INT, ComparisonOperation.NOT_EQUAL, value, 0, trueTarget, falseTarget);
    }

    protected void transferIFLT(F state, int at) throws BadBytecode {
        S value = state.pop();
        int trueTarget = at + codeIterator.s16bitAt(at + 1);
        int falseTarget = at + 3;

        branchIf(state, at, Type.INT, ComparisonOperation.LESS_THAN, value, 0, trueTarget, falseTarget);
    }

    protected void transferIFGE(F state, int at) throws BadBytecode {
        S value = state.pop();
        int trueTarget = at + codeIterator.s16bitAt(at + 1);
        int falseTarget = at + 3;

        branchIf(state, at, Type.INT, ComparisonOperation.GREATER_OR_EQUAL, value, 0, trueTarget, falseTarget);
    }

    protected void transferIFGT(F state, int at) throws BadBytecode {
        S value = state.pop();
        int trueTarget = at + codeIterator.s16bitAt(at + 1);
        int falseTarget = at + 3;

        branchIf(state, at, Type.INT, ComparisonOperation.GREATER_THAN, value, 0, trueTarget, falseTarget);
    }

    protected void transferIFLE(F state, int at) throws BadBytecode {
        S value = state.pop();
        int trueTarget = at + codeIterator.s16bitAt(at + 1);
        int falseTarget = at + 3;

        branchIf(state, at, Type.INT, ComparisonOperation.LESS_OR_EQUAL, value, 0, trueTarget, falseTarget);
    }

    protected void transferIF_ICMPEQ(F state, int at) throws BadBytecode {
        S right = state.pop();
        S left = state.pop();
        int trueTarget = at + codeIterator.s16bitAt(at + 1);
        int falseTarget = at + 3;

        branchIfCompare(state, at, Type.INT, ComparisonOperation.EQUAL, left, 1, right, 0, trueTarget, falseTarget);
    }

    protected void transferIF_ICMPNE(F state, int at) throws BadBytecode {
        S right = state.pop();
        S left = state.pop();
        int trueTarget = at + codeIterator.s16bitAt(at + 1);
        int falseTarget = at + 3;

        branchIfCompare(state, at, Type.INT, ComparisonOperation.NOT_EQUAL, left, 1, right, 0, trueTarget, falseTarget);
    }

    protected void transferIF_ICMPLT(F state, int at) throws BadBytecode {
        S right = state.pop();
        S left = state.pop();
        int trueTarget = at + codeIterator.s16bitAt(at + 1);
        int falseTarget = at + 3;

        branchIfCompare(state, at, Type.INT, ComparisonOperation.LESS_THAN, left, 1, right, 0, trueTarget, falseTarget);
    }

    protected void transferIF_ICMPGE(F state, int at) throws BadBytecode {
        S right = state.pop();
        S left = state.pop();
        int trueTarget = at + codeIterator.s16bitAt(at + 1);
        int falseTarget = at + 3;

        branchIfCompare(state, at, Type.INT, ComparisonOperation.GREATER_OR_EQUAL, left, 1, right, 0, trueTarget, falseTarget);
    }

    protected void transferIF_ICMPGT(F state, int at) throws BadBytecode {
        S right = state.pop();
        S left = state.pop();
        int trueTarget = at + codeIterator.s16bitAt(at + 1);
        int falseTarget = at + 3;

        branchIfCompare(state, at, Type.INT, ComparisonOperation.GREATER_THAN, left, 1, right, 0, trueTarget, falseTarget);
    }

    protected void transferIF_ICMPLE(F state, int at) throws BadBytecode {
        S right = state.pop();
        S left = state.pop();
        int trueTarget = at + codeIterator.s16bitAt(at + 1);
        int falseTarget = at + 3;

        branchIfCompare(state, at, Type.INT, ComparisonOperation.LESS_OR_EQUAL, left, 1, right, 0, trueTarget, falseTarget);
    }

    protected void transferIF_ACMPEQ(F state, int at) throws BadBytecode {
        S right = state.pop();
        S left = state.pop();
        int trueTarget = at + codeIterator.s16bitAt(at + 1);
        int falseTarget = at + 3;

        branchIfCompare(state, at, Type.OBJECT, ComparisonOperation.EQUAL, left, 1, right, 0,trueTarget, falseTarget);
    }

    protected void transferIF_ACMPNE(F state, int at) throws BadBytecode {
        S right = state.pop();
        S left = state.pop();
        int trueTarget = at + codeIterator.s16bitAt(at + 1);
        int falseTarget = at + 3;

        branchIfCompare(state, at, Type.OBJECT, ComparisonOperation.NOT_EQUAL, left, 1, right, 0, trueTarget,
                falseTarget);
    }

    protected void transferGOTO(F state, int at) throws BadBytecode {
        int target = at + codeIterator.s16bitAt(at + 1);

        branchGoto(state, at, target);
    }

    protected void transferJSR(F state, int at) throws BadBytecode {
        int target = at + codeIterator.s16bitAt(at + 1);

        state.push(callSubroutine(state, at, target));
    }

    protected void transferRET(F state, int at, boolean isWide) throws BadBytecode {
        int index = isWide ? codeIterator.u16bitAt(at + 2) : codeIterator.byteAt(at + 1);
        L local = state.getLocal(index);

        returnFromSubroutine(state, at, index, local);
    }

    protected void transferTABLESWITCH(F state, int at) throws BadBytecode {
        int start = (at & ~3) + 4;
        int defaultTarget = at + codeIterator.s32bitAt(start);

        int low = codeIterator.s32bitAt(start + 4);
        int high = codeIterator.s32bitAt(start + 8);

        int jumpOffsetCount = high - low + 1;
        int jumpOffsetStart = start + 12;
        int[] indexedTargets = new int[jumpOffsetCount];

        for (int i = 0; i < jumpOffsetCount; i++) {
            indexedTargets[i] = at + codeIterator.s32bitAt(jumpOffsetStart + i * 4);
        }

        S index = state.pop();

        branchTableSwitch(state, at, index, 0, defaultTarget, indexedTargets);
    }

    protected void transferLOOKUPSWITCH(F state, int at) throws BadBytecode {
        int start = (at & ~3) + 4;
        int defaultTarget = at + codeIterator.s32bitAt(start);

        int pairCount = codeIterator.s32bitAt(start + 4);
        int pairStart = start + 8;

        int[] matches = new int[pairCount];
        int[] matchTargets = new int[pairCount];

        for (int i = 0; i < pairCount; i++) {
            matches[i] = codeIterator.s32bitAt(pairStart + i * 8);
            matchTargets[i] = at + codeIterator.s32bitAt(pairStart + i * 8 + 4);
        }

        S key = state.pop();

        branchLookupSwitch(state, at, key, 0, defaultTarget, matches, matchTargets);
    }

    protected void transferIRETURN(F state, int at) throws BadBytecode {
        S value = state.pop();

        returnFromMethod(state, at, Type.INT, value, 0);
    }

    protected void transferLRETURN(F state, int at) throws BadBytecode {
        S value = state.pop2();

        returnFromMethod(state, at, Type.LONG, value, 1);
    }

    protected void transferFRETURN(F state, int at) throws BadBytecode {
        S value = state.pop();

        returnFromMethod(state, at, Type.FLOAT,  value, 0);
    }

    protected void transferDRETURN(F state, int at) throws BadBytecode {
        S value = state.pop2();

        returnFromMethod(state, at, Type.DOUBLE,  value, 1);
    }

    protected void transferARETURN(F state, int at) throws BadBytecode {
        S value = state.pop();

        returnFromMethod(state, at, Type.OBJECT,  value, 0);
    }

    protected void transferRETURN(F state, int at) throws BadBytecode {
        returnFromMethod(state, at);
    }

    protected void transferGETSTATIC(F state, int at) throws BadBytecode {
        int index = codeIterator.u16bitAt(at + 1);
        String desc = constPool.getFieldrefType(index);
        Type classType = resolveClassInfo(constPool.getFieldrefClassName(index), at);
        String fieldName = constPool.getFieldrefName(index);
        CtField field;

        try {
            field = classType.getCtClass().getField(fieldName);
        } catch (NotFoundException e) {
            throw new BadBytecode("Could not find field [position = " + at + "]: " + e.getMessage());
        }

        Type fieldType = typeFromDescriptor(desc, at);

        if (fieldType.isTwoWordPrimitive()) {
            state.push2(readStaticField(state, at, classType, fieldType, field));
        } else {
            state.push(readStaticField(state, at, classType, fieldType, field));
        }
    }

    protected void transferPUTSTATIC(F state, int at) throws BadBytecode {
        int index = codeIterator.u16bitAt(at + 1);
        String desc = constPool.getFieldrefType(index);
        Type classType = resolveClassInfo(constPool.getFieldrefClassName(index), at);
        String fieldName = constPool.getFieldrefName(index);
        CtField field;

        try {
            field = classType.getCtClass().getField(fieldName);
        } catch (NotFoundException e) {
            throw new BadBytecode("Could not find field [position = " + at + "]: " + e.getMessage());
        }

        Type fieldType = typeFromDescriptor(desc, at);

        if (fieldType.isTwoWordPrimitive()) {
            S value = state.pop2();

            assignStaticField(state, at, classType, fieldType, field, value, 1);
        } else {
            S value = state.pop();

            assignStaticField(state, at, classType, fieldType, field, value, 0);
        }
    }

    protected void transferGETFIELD(F state, int at) throws BadBytecode {
        int index = codeIterator.u16bitAt(at + 1);
        String desc = constPool.getFieldrefType(index);
        Type targetType = resolveClassInfo(constPool.getFieldrefClassName(index), at);
        String fieldName = constPool.getFieldrefName(index);
        CtField field;

        try {
            field = targetType.getCtClass().getField(fieldName);
        } catch (NotFoundException e) {
            throw new BadBytecode("Could not find field [position = " + at + "]: " + e.getMessage());
        }

        Type fieldType = typeFromDescriptor(desc, at);

        S targetObject = state.pop();

        if (fieldType.isTwoWordPrimitive()) {
            state.push2(readField(state, at, targetType, fieldType, field, targetObject, 1));
        } else {
            state.push(readField(state, at, targetType, fieldType, field, targetObject, 0));
        }
    }

    protected void transferPUTFIELD(F state, int at) throws BadBytecode {
        int index = codeIterator.u16bitAt(at + 1);
        String desc = constPool.getFieldrefType(index);
        Type targetType = resolveClassInfo(constPool.getFieldrefClassName(index), at);
        String fieldName = constPool.getFieldrefName(index);
        CtField field;

        try {
            field = targetType.getCtClass().getField(fieldName);
        } catch (NotFoundException e) {
            throw new BadBytecode("Could not find field [position = " + at + "]: " + e.getMessage());
        }

        Type fieldType = typeFromDescriptor(desc, at);

        if (fieldType.isTwoWordPrimitive()) {
            S value = state.pop2();
            S targetObject = state.pop();

            assignField(state, at, field, targetType, fieldType, targetObject, 2, value, 1);
        } else {
            S value = state.pop();
            S targetObject = state.pop();

            assignField(state, at, field, targetType, fieldType, targetObject, 1, value, 0);
        }
    }

    protected void transferINVOKEVIRTUAL(F state, int at) throws BadBytecode {
        int index = codeIterator.u16bitAt(at + 1);
        String desc = constPool.getMethodrefType(index);
        Type[] paramTypes = paramTypesFromDescriptor(desc, at);
        int i = paramTypes.length;

        ArrayList<S> arguments = new ArrayList<>(i);
        for (int j = 0; j < i; j++) {
            arguments.add(null);
        }

        int[] argumentOffsets = new int[i];
        int j = 0;
        while (i > 0) {
            i--;

            if (paramTypes[i].isTwoWordPrimitive()) {
                arguments.set(i, state.pop2());
                argumentOffsets[i] = j + 1;
                j += 2;
            } else {
                arguments.set(i, state.pop());
                argumentOffsets[i] = j;
                j += 1;
            }
        }

        Type targetType = resolveClassInfo(constPool.getMethodrefClassName(index), at);

        String methodName = constPool.getMethodrefName(index);
        CtMethod method;
        try {
            method = targetType.getCtClass().getMethod(methodName, desc);
        } catch (NotFoundException e) {
            throw new BadBytecode("Could not find method [position = " + at + "]: " + e.getMessage());
        }

        S targetObject = state.pop();

        Type returnType = returnTypeFromDescriptor(desc, at);

        S returnValue = invokeMethod(state, at, method, targetType, returnType, paramTypes, targetObject, j, arguments, argumentOffsets);
        if (returnType != Type.VOID) {
            if (returnType.isTwoWordPrimitive()) {
                state.push2(returnValue);
            } else {
                state.push(returnValue);
            }
        }
    }

    protected void transferINVOKESPECIAL(F state, int at) throws BadBytecode {
        int index = codeIterator.u16bitAt(at + 1);
        String desc = constPool.getMethodrefType(index);
        Type[] paramTypes = paramTypesFromDescriptor(desc, at);
        int i = paramTypes.length;

        ArrayList<S> arguments = new ArrayList<>(i);
        for (int j = 0; j < i; j++) {
            arguments.add(null);
        }

        int[] argumentOffsets = new int[i];
        int j = 0;
        while (i > 0) {
            i--;

            if (paramTypes[i].isTwoWordPrimitive()) {
                arguments.set(i, state.pop2());
                argumentOffsets[i] = j + 1;
                j += 2;
            } else {
                arguments.set(i, state.pop());
                argumentOffsets[i] = j;
                j += 1;
            }
        }

        Type targetType = resolveClassInfo(constPool.getMethodrefClassName(index), at);

        if (constPool.isConstructor(targetType.getCtClass().getName(), index) > 0) {
            CtConstructor constructor;
            try {
                constructor = targetType.getCtClass().getConstructor(desc);
            } catch (NotFoundException e) {
                throw new BadBytecode("Could not find constructor [position = " + at + "]: " + e.getMessage());
            }

            S targetObject = state.pop();

            invokeConstructor(state, at, constructor, targetType, paramTypes, targetObject, j, arguments, argumentOffsets);
        } else {
            String methodName = constPool.getMethodrefName(index);
            CtMethod method;
            try {
                method = targetType.getCtClass().getMethod(methodName, desc);
            } catch (NotFoundException e) {
                throw new BadBytecode("Could not find method [position = " + at + "]: " + e.getMessage());
            }

            S targetObject = state.pop();

            Type returnType = returnTypeFromDescriptor(desc, at);

            S returnValue = invokeMethod(state, at, method, targetType, returnType, paramTypes, targetObject, j, arguments, argumentOffsets);
            if (returnType != Type.VOID) {
                if (returnType.isTwoWordPrimitive()) {
                    state.push2(returnValue);
                } else {
                    state.push(returnValue);
                }
            }
        }
    }

    protected void transferINVOKESTATIC(F state, int at) throws BadBytecode {
        int index = codeIterator.u16bitAt(at + 1);
        String desc = constPool.getMethodrefType(index);
        Type[] paramTypes = paramTypesFromDescriptor(desc, at);
        int i = paramTypes.length;

        ArrayList<S> arguments = new ArrayList<>(i);
        for (int j = 0; j < i; j++) {
            arguments.add(null);
        }

        int[] argumentOffsets = new int[i];
        int j = 0;
        while (i > 0) {
            i--;

            if (paramTypes[i].isTwoWordPrimitive()) {
                arguments.set(i, state.pop2());
                argumentOffsets[i] = j + 1;
                j += 2;
            } else {
                arguments.set(i, state.pop());
                argumentOffsets[i] = j;
                j += 1;
            }
        }

        Type targetType = resolveClassInfo(constPool.getMethodrefClassName(index), at);

        String methodName = constPool.getMethodrefName(index);
        CtMethod method;
        try {
            method = targetType.getCtClass().getMethod(methodName, desc);
        } catch (NotFoundException e) {
            throw new BadBytecode("Could not find method [position = " + at + "]: " + e.getMessage());
        }

        Type returnType = returnTypeFromDescriptor(desc, at);

        S returnValue = invokeStaticMethod(state, at, method, returnType, paramTypes, arguments, argumentOffsets);
        if (returnType != Type.VOID) {
            if (returnType.isTwoWordPrimitive()) {
                state.push2(returnValue);
            } else {
                state.push(returnValue);
            }
        }
    }

    protected void transferINVOKEINTERFACE(F state, int at) throws BadBytecode {
        int index = codeIterator.u16bitAt(at + 1);
        String desc = constPool.getInterfaceMethodrefType(index);
        Type[] paramTypes = paramTypesFromDescriptor(desc, at);
        int i = paramTypes.length;

        ArrayList<S> arguments = new ArrayList<>(i);
        for (int j = 0; j < i; j++) {
            arguments.add(null);
        }

        int[] argumentOffsets = new int[i];
        int j = 0;
        while (i > 0) {
            i--;

            if (paramTypes[i].isTwoWordPrimitive()) {
                arguments.set(i, state.pop2());
                argumentOffsets[i] = j + 1;
                j += 2;
            } else {
                arguments.set(i, state.pop());
                argumentOffsets[i] = j;
                j += 1;
            }
        }

        Type targetType = resolveClassInfo(constPool.getInterfaceMethodrefClassName(index), at);

        String methodName = constPool.getInterfaceMethodrefName(index);
        CtMethod method;
        try {
            method = targetType.getCtClass().getMethod(methodName, desc);
        } catch (NotFoundException e) {
            throw new BadBytecode("Could not find method [position = " + at + "]: " + e.getMessage());
        }

        S targetObject = state.pop();

        Type returnType = returnTypeFromDescriptor(desc, at);

        S returnValue = invokeMethod(state, at, method, targetType, returnType, paramTypes, targetObject, j, arguments, argumentOffsets);
        if (returnType != Type.VOID) {
            if (returnType.isTwoWordPrimitive()) {
                state.push2(returnValue);
            } else {
                state.push(returnValue);
            }
        }
    }

    // http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-5.html#jvms-5.4.3.5
    static final class MethodHandle {
        public enum Kind {
            READ,
            ASSIGN,
            INVOKE,
            CONSTRUCT
        }

        private final Kind kind;
        private final CtMember member;

        private MethodHandle(Kind kind, CtMember member) {
            this.kind = kind;
            this.member = member;
        }

        public CtMember getMember() {
            return member;
        }

        public String toString() {
            return kind + " " + member.getDeclaringClass().getName() + "." + member.getName();
        }

        public int hashCode() {
            return kind.hashCode() + 31 * member.hashCode();
        }

        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (!(obj instanceof MethodHandle)) { return false; }

            MethodHandle m = (MethodHandle) obj;
            return kind == m.kind && member == m.member;
        }
    }

    private MethodHandle makeMethodHandle(int index, int kind, int at) throws BadBytecode {
        switch (kind) {
            case ConstPool.REF_getField:
            case ConstPool.REF_getStatic: {
                String desc = constPool.getFieldrefType(index);
                Type targetType = resolveClassInfo(constPool.getFieldrefClassName(index), at);
                String fieldName = constPool.getFieldrefName(index);
                CtField field;

                try {
                    field = targetType.getCtClass().getField(fieldName);
                } catch (NotFoundException e) {
                    throw new BadBytecode("Could not find field [position = " + at + "]: " + e.getMessage());
                }

                return new MethodHandle(MethodHandle.Kind.READ, field);
            }
            case ConstPool.REF_putField:
            case ConstPool.REF_putStatic: {
                String desc = constPool.getFieldrefType(index);
                Type targetType = resolveClassInfo(constPool.getFieldrefClassName(index), at);
                String fieldName = constPool.getFieldrefName(index);
                CtField field;

                try {
                    field = targetType.getCtClass().getField(fieldName);
                } catch (NotFoundException e) {
                    throw new BadBytecode("Could not find field [position = " + at + "]: " + e.getMessage());
                }

                return new MethodHandle(MethodHandle.Kind.ASSIGN, field);
            }
            case ConstPool.REF_invokeVirtual:
            case ConstPool.REF_invokeSpecial:
            case ConstPool.REF_invokeStatic: {
                String desc = constPool.getMethodrefType(index);
                Type targetType = resolveClassInfo(constPool.getMethodrefClassName(index), at);

                String methodName = constPool.getMethodrefName(index);
                CtMethod method;
                try {
                    method = targetType.getCtClass().getMethod(methodName, desc);
                } catch (NotFoundException e) {
                    throw new BadBytecode("Could not find method [position = " + at + "]: " + e.getMessage());
                }

                return new MethodHandle(MethodHandle.Kind.INVOKE, method);
            }
            case ConstPool.REF_newInvokeSpecial: {
                String desc = constPool.getMethodrefType(index);
                Type targetType = resolveClassInfo(constPool.getMethodrefClassName(index), at);

                String methodName = constPool.getMethodrefName(index);
                CtConstructor constructor;
                try {
                    constructor = targetType.getCtClass().getConstructor(desc);
                } catch (NotFoundException e) {
                    throw new BadBytecode("Could not find constructor [position = " + at + "]: " + e.getMessage());
                }

                return new MethodHandle(MethodHandle.Kind.CONSTRUCT, constructor);
            }
            case ConstPool.REF_invokeInterface: {
                String desc = constPool.getInterfaceMethodrefType(index);
                Type targetType = resolveClassInfo(constPool.getInterfaceMethodrefClassName(index), at);

                String methodName = constPool.getInterfaceMethodrefName(index);
                CtMethod method;
                try {
                    method = targetType.getCtClass().getMethod(methodName, desc);
                } catch (NotFoundException e) {
                    throw new BadBytecode("Could not find method [position = " + at + "]: " + e.getMessage());
                }

                return new MethodHandle(MethodHandle.Kind.INVOKE, method);
            }
        }

        return null;
    }

    /* From specification:
     * "
     *   bootstrap_arguments
     *       Each entry in the bootstrap_arguments array must be a valid index into the constant_pool table. The constant_pool entry at that index must be a CONSTANT_String_info, CONSTANT_Class_info, CONSTANT_Integer_info, CONSTANT_Long_info, CONSTANT_Float_info, CONSTANT_Double_info, CONSTANT_MethodHandle_info, or CONSTANT_MethodType_info structure (§4.4.3, §4.4.1, §4.4.4, §4.4.5), §4.4.8, §4.4.9).
     * "
     */
    static abstract class StaticArgument {
        private StaticArgument() { }

        static final class String extends StaticArgument {
            private final java.lang.String value;

            private String(java.lang.String value) {
                this.value = value;
            }

            public java.lang.String getValue() {
                return value;
            }

            public java.lang.String toString() {
                return "String: " + value;
            }

            public int hashCode() {
                return value.hashCode();
            }

            public boolean equals(Object obj) {
                if (this == obj) { return true; }
                if (!(obj instanceof String)) { return false; }

                return value.equals(((String) obj).value);
            }
        }

        static final class Class extends StaticArgument {
            private final java.lang.String className;

            private Class(java.lang.String className) {
                this.className = className;
            }

            public java.lang.String getClassName() {
                return className;
            }

            public java.lang.String toString() {
                return "Class: " + className;
            }

            public int hashCode() {
                return className.hashCode();
            }

            public boolean equals(Object obj) {
                if (this == obj) { return true; }
                if (!(obj instanceof Class)) { return false; }

                return className.equals(((Class) obj).className);
            }
        }

        static final class Integer extends StaticArgument {
            private final int value;

            private Integer(int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }

            public java.lang.String toString() {
                return "Integer: " + value;
            }

            public int hashCode() {
                return java.lang.Integer.hashCode(value);
            }

            public boolean equals(Object obj) {
                if (this == obj) { return true; }
                if (!(obj instanceof Integer)) { return false; }

                return value == ((Integer) obj).value;
            }
        }

        static final class Long extends StaticArgument {
            private final long value;

            private Long(long value) {
                this.value = value;
            }

            public long getValue() {
                return value;
            }

            public java.lang.String toString() {
                return "Long: " + value;
            }

            public int hashCode() {
                return java.lang.Long.hashCode(value);
            }

            public boolean equals(Object obj) {
                if (this == obj) { return true; }
                if (!(obj instanceof Long)) { return false; }

                return value == ((Long) obj).value;
            }
        }

        static final class Float extends StaticArgument {
            private final float value;

            private Float(float value) {
                this.value = value;
            }

            public float getValue() {
                return value;
            }

            public java.lang.String toString() {
                return "Float: " + value;
            }

            public int hashCode() {
                return java.lang.Float.hashCode(value);
            }

            public boolean equals(Object obj) {
                if (this == obj) { return true; }
                if (!(obj instanceof Float)) { return false; }

                return value == ((Float) obj).value;
            }
        }

        static final class Double extends StaticArgument {
            private final double value;

            private Double(double value) {
                this.value = value;
            }

            public double getValue() {
                return value;
            }

            public java.lang.String toString() {
                return "Double: " + value;
            }

            public int hashCode() {
                return java.lang.Double.hashCode(value);
            }

            public boolean equals(Object obj) {
                if (this == obj) { return true; }
                if (!(obj instanceof Double)) { return false; }

                return value == ((Double) obj).value;
            }
        }

        static final class MethodHandle extends StaticArgument {
            private final HighLevelAnalyzer.MethodHandle value;

            private MethodHandle(HighLevelAnalyzer.MethodHandle value) {
                this.value = value;
            }

            public HighLevelAnalyzer.MethodHandle getValue() {
                return value;
            }

            public java.lang.String toString() {
                return "MethodHandle: " + value;
            }

            public int hashCode() {
                return value.hashCode();
            }

            public boolean equals(Object obj) {
                if (this == obj) { return true; }
                if (!(obj instanceof MethodHandle)) { return false; }

                return value.equals(((MethodHandle) obj).value);
            }
        }

        static final class MethodType extends StaticArgument {
            private final java.lang.String descriptor;

            private MethodType(java.lang.String descriptor) {
                this.descriptor = descriptor;
            }

            public java.lang.String getDescriptor() {
                return descriptor;
            }

            public java.lang.String toString() {
                return "MethodType: " + descriptor;
            }

            public int hashCode() {
                return descriptor.hashCode();
            }

            public boolean equals(Object obj) {
                if (this == obj) { return true; }
                if (!(obj instanceof MethodType)) { return false; }

                return descriptor.equals(((MethodType) obj).descriptor);
            }
        }
    }

    private StaticArgument makeStaticArgument(int index, int at) throws BadBytecode {
        switch (constPool.getTag(index)) {
            case ConstPool.CONST_String:
                return new StaticArgument.String(constPool.getStringInfo(index));
            case ConstPool.CONST_Class:
                return new StaticArgument.String(constPool.getClassInfo(index));
            case ConstPool.CONST_Integer:
                return new StaticArgument.Integer(constPool.getIntegerInfo(index));
            case ConstPool.CONST_Long:
                return new StaticArgument.Long(constPool.getLongInfo(index));
            case ConstPool.CONST_Float:
                return new StaticArgument.Float(constPool.getFloatInfo(index));
            case ConstPool.CONST_Double:
                return new StaticArgument.Double(constPool.getDoubleInfo(index));
            case ConstPool.CONST_MethodHandle:
                return new StaticArgument.MethodHandle(makeMethodHandle(constPool.getMethodHandleIndex(index), constPool.getMethodHandleKind(index), at));
            case 16: // CONSTANT_MethodType_info
                return new StaticArgument.MethodType(constPool.getUtf8Info(constPool.getMethodTypeInfo(index)));
            default:
                throw new BadBytecode("Illegal static argument tag [position = " + at + "]");
        }
    }

    protected void transferINVOKEDYNAMIC(F state, int at) throws BadBytecode {
        int index = codeIterator.u16bitAt(at + 1);
        String desc = constPool.getInvokeDynamicType(index);
        Type[] paramTypes = paramTypesFromDescriptor(desc, at);
        int i = paramTypes.length;

        ArrayList<S> arguments = new ArrayList<>(i);
        for (int j = 0; j < i; j++) {
            arguments.add(null);
        }

        int[] argumentOffsets = new int[i];
        int j = 0;
        while (i > 0) {
            i--;

            if (paramTypes[i].isTwoWordPrimitive()) {
                arguments.set(i, state.pop2());
                argumentOffsets[i] = j + 1;
                j += 2;
            } else {
                arguments.set(i, state.pop());
                argumentOffsets[i] = j;
                j += 1;
            }
        }

        Type returnType = returnTypeFromDescriptor(desc, at);


        BootstrapMethodsAttribute bootstrapMethodsAttribute = (BootstrapMethodsAttribute) clazz.getClassFile2().getAttribute(BootstrapMethodsAttribute.tag);
        BootstrapMethodsAttribute.BootstrapMethod bootstrapMethod = bootstrapMethodsAttribute.getMethods()[constPool.getInvokeDynamicBootstrap(index)];

        MethodHandle methodHandle = makeMethodHandle(constPool.getMethodHandleIndex(bootstrapMethod.methodRef), constPool.getMethodHandleKind(bootstrapMethod.methodRef), at);
        StaticArgument[] staticArguments = new StaticArgument[bootstrapMethod.arguments.length];
        for (int k = 0; k < bootstrapMethod.arguments.length; k++) {
            staticArguments[k] = makeStaticArgument(bootstrapMethod.arguments[k], at);
        }

        S returnValue = invokeDynamic(state, at, (CtMethod) methodHandle.getMember(), staticArguments, returnType, paramTypes, arguments, argumentOffsets);
        if (returnType != Type.VOID) {
            if (returnType.isTwoWordPrimitive()) {
                state.push2(returnValue);
            } else {
                state.push(returnValue);
            }
        }
    }

    protected void transferNEW(F state, int at) throws BadBytecode {
        state.push(newInstance(state, at, resolveClassInfo(constPool.getClassInfo(codeIterator.u16bitAt(at + 1)), at)));
    }

    protected void transferNEWARRAY(F state, int at) throws BadBytecode {
        ArrayList<S> lengths = new ArrayList<>(1);
        lengths.add(state.pop());

        Type type;
        int typeInfo = codeIterator.byteAt(at + 1);
        switch (typeInfo) {
            case T_BOOLEAN:
                type = getType("boolean[]", at);
                break;
            case T_CHAR:
                type = getType("char[]", at);
                break;
            case T_BYTE:
                type = getType("byte[]", at);
                break;
            case T_SHORT:
                type = getType("short[]", at);
                break;
            case T_INT:
                type = getType("int[]", at);
                break;
            case T_LONG:
                type = getType("long[]", at);
                break;
            case T_FLOAT:
                type = getType("float[]", at);
                break;
            case T_DOUBLE:
                type = getType("double[]", at);
                break;
            default:
                throw new BadBytecode("Invalid array type [position = " + at + "]: " + typeInfo);
        }

        state.push(newArray(state, at, type, lengths, new int[] { 0 }));
    }

    protected void transferANEWARRAY(F state, int at) throws BadBytecode {
        Type type = resolveClassInfo(constPool.getClassInfo(codeIterator.u16bitAt(at + 1)), at);
        String name = type.getCtClass().getName().concat("[]");

        ArrayList<S> lengths = new ArrayList<>(1);
        lengths.add(state.pop());

        state.push(newArray(state, at, getType(name, at), lengths, new int[] { 0 }));
    }

    protected void transferARRAYLENGTH(F state, int at) throws BadBytecode {
        S array = state.pop();

        state.push(arrayLength(state, at, array, 0));
    }

    protected void transferATHROW(F state, int at) throws BadBytecode {
        S throwable = state.pop();

        state.push(throwException(state, at, throwable, 0));
    }

    protected void transferCHECKCAST(F state, int at) throws BadBytecode {
        S value = state.pop();

        Type toType = typeFromDescriptor(constPool.getClassInfoByDescriptor(codeIterator.u16bitAt(at + 1)), at);

        // The "from" type is actually unknown...
        state.push(convertType(state, at, Type.OBJECT, toType, value, 0));
    }

    protected void transferINSTANCEOF(F state, int at) throws BadBytecode {
        S value = state.pop();

        Type ofType = typeFromDescriptor(constPool.getClassInfoByDescriptor(codeIterator.u16bitAt(at + 1)), at);

        state.push(instanceOf(state, at, ofType, value, 0));
    }

    protected void transferMONITORENTER(F state, int at) throws BadBytecode {
        S monitor = state.pop();

        enterSynchronized(state, at, monitor, 0);
    }

    protected void transferMONITOREXIT(F state, int at) throws BadBytecode {
        S monitor = state.pop();

        exitSynchronized(state, at, monitor, 0);
    }

    protected void transferMULTIANEWARRAY(F state, int at) throws BadBytecode {
        Type type = resolveClassInfo(constPool.getClassInfo(codeIterator.u16bitAt(at + 1)), at);
        String name = type.getCtClass().getName();

        int dimensions = codeIterator.byteAt(at + 3);
        ArrayList<S> lengths = new ArrayList<>(dimensions);
        for (int i = 0; i < dimensions; i++) {
            lengths.add(null);
        }

        int i = dimensions;
        int[] lengthOffsets = new int[i];
        int j = 0;
        while (i > 0) {
            i--;
            lengths.set(i, state.pop());
            lengthOffsets[i] = j;
            j--;
        }

        state.push(newArray(state, at, getType(name, at), lengths, lengthOffsets));
    }

    protected void transferIFNULL(F state, int at) throws BadBytecode {
        S value = state.pop();
        int trueTarget = at + codeIterator.s16bitAt(at + 1);
        int falseTarget = at + 3;

        branchIf(state, at, Type.OBJECT, ComparisonOperation.IS_NULL, value, 0, trueTarget, falseTarget);
    }

    protected void transferIFNONNULL(F state, int at) throws BadBytecode {
        S value = state.pop();
        int trueTarget = at + codeIterator.s16bitAt(at + 1);
        int falseTarget = at + 3;

        branchIf(state, at, Type.OBJECT, ComparisonOperation.IS_NON_NULL, value, 0, trueTarget, falseTarget);
    }

    protected void transferGOTO_W(F state, int at) throws BadBytecode {
        int target = at + codeIterator.s32bitAt(at + 1);

        branchGoto(state, at, target);
    }

    protected void transferJSR_W(F state, int at) throws BadBytecode {
        int target = at + codeIterator.s32bitAt(at + 1);

        state.push(callSubroutine(state, at, target));
    }




    private Type resolveClassInfo(String info, int at) throws BadBytecode {
        CtClass clazz;
        try {
            if (info.charAt(0) == '[') {
                clazz = Descriptor.toCtClass(info, classPool);
            } else {
                clazz = classPool.get(info);
            }
        } catch (NotFoundException e) {
            throw new BadBytecode("Could not find class in descriptor [position = " + at + "]: " + e.getMessage());
        }

        if (clazz == null) {
            throw new BadBytecode("Could not obtain type for descriptor [position = " + at + "]: " + info);
        }

        return Type.of(clazz);
    }

    private Type typeFromDescriptor(String desc, int at) throws BadBytecode {
        CtClass clazz;
        try {
            clazz = Descriptor.toCtClass(desc, classPool);
        } catch (NotFoundException e) {
            throw new BadBytecode("Could not find class in descriptor [position = " + at + "]: " + e.getMessage());
        }

        if (clazz == null) {
            throw new BadBytecode("Could not obtain type for descriptor [position = " + at + "]: " + desc);
        }

        return Type.of(clazz);
    }

    private Type[] paramTypesFromDescriptor(String desc, int at) throws BadBytecode {
        CtClass classes[];
        try {
            classes = Descriptor.getParameterTypes(desc, classPool);
        } catch (NotFoundException e) {
            throw new BadBytecode("Could not find class in descriptor [position = " + at + "]: " + e.getMessage());
        }

        if (classes == null) {
            throw new BadBytecode("Could not obtain parameters for descriptor [position = " + at + "]: " + desc);
        }

        Type[] types = new Type[classes.length];
        for (int i = 0; i < types.length; i++) {
            types[i] = Type.of(classes[i]);
        }

        return types;
    }

    private Type returnTypeFromDescriptor(String desc, int at) throws BadBytecode {
        CtClass clazz;
        try {
            clazz = Descriptor.getReturnType(desc, classPool);
        } catch (NotFoundException e) {
            throw new BadBytecode("Could not find class in descriptor [position = " + at + "]: " + e.getMessage());
        }

        if (clazz == null) {
            throw new BadBytecode("Could not obtain return type for descriptor [position = " + at + "]: " + desc);
        }

        return Type.of(clazz);
    }
}
