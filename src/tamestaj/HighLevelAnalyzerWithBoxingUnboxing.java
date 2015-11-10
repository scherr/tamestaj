package tamestaj;

import javassist.*;
import javassist.bytecode.BadBytecode;

import java.util.ArrayList;

@SuppressWarnings("unused")
abstract class HighLevelAnalyzerWithBoxingUnboxing<L, S, F extends Frame<L, S>> extends HighLevelAnalyzer<L, S, F> {
    protected HighLevelAnalyzerWithBoxingUnboxing(CtBehavior behavior) {
        super(behavior);
    }

    protected HighLevelAnalyzerWithBoxingUnboxing(Analyzer<?> analyzer) {
        super(analyzer);
    }

    protected abstract S invokeStaticMethodExceptBoxing(F state, int at, CtMethod method, Type returnType, Type[] paramTypes, ArrayList<S> arguments, int[] argumentOffsets) throws BadBytecode;
    protected abstract S invokeMethodExceptUnboxing(F state, int at, CtMethod method, Type targetType, Type returnType, Type[] paramTypes, S targetObject, int targetObjectOffset, ArrayList<S> arguments, int[] argumentOffsets) throws BadBytecode;

    protected S invokeStaticMethod(F state, int at, CtMethod method, Type returnType, Type[] paramTypes, ArrayList<S> arguments, int[] argumentOffsets) throws BadBytecode {
        if (arguments.size() == 1 && method.getName().equals("valueOf")) {
            CtClass clazz = method.getDeclaringClass();

            if (Util.BOOLEAN_OBJECT_TYPE.getCtClass().equals(clazz)) {
                return convertType(state, at, Type.BOOLEAN, Util.BOOLEAN_OBJECT_TYPE, arguments.get(0), argumentOffsets[0]);
            } else if (Util.INTEGER_OBJECT_TYPE.getCtClass().equals(clazz)) {
                return convertType(state, at, Type.INT, Util.INTEGER_OBJECT_TYPE, arguments.get(0), argumentOffsets[0]);
            } else if (Util.LONG_OBJECT_TYPE.getCtClass().equals(clazz)) {
                return convertType(state, at, Type.LONG, Util.LONG_OBJECT_TYPE, arguments.get(0), argumentOffsets[0]);
            } else if (Util.FLOAT_OBJECT_TYPE.getCtClass().equals(clazz)) {
                return convertType(state, at, Type.FLOAT, Util.FLOAT_OBJECT_TYPE, arguments.get(0), argumentOffsets[0]);
            } else if (Util.DOUBLE_OBJECT_TYPE.getCtClass().equals(clazz)) {
                return convertType(state, at, Type.DOUBLE, Util.DOUBLE_OBJECT_TYPE, arguments.get(0), argumentOffsets[0]);
            } else if (Util.BYTE_OBJECT_TYPE.getCtClass().equals(clazz)) {
                return convertType(state, at, Type.BYTE, Util.BYTE_OBJECT_TYPE, arguments.get(0), argumentOffsets[0]);
            } else if (Util.CHARACTER_OBJECT_TYPE.getCtClass().equals(clazz)) {
                return convertType(state, at, Type.CHAR, Util.CHARACTER_OBJECT_TYPE, arguments.get(0), argumentOffsets[0]);
            } else if (Util.SHORT_OBJECT_TYPE.getCtClass().equals(clazz)) {
                return convertType(state, at, Type.SHORT, Util.SHORT_OBJECT_TYPE, arguments.get(0), argumentOffsets[0]);
            }
        }

        return invokeStaticMethodExceptBoxing(state, at, method, returnType, paramTypes, arguments, argumentOffsets);
    }

    protected S invokeMethod(F state, int at, CtMethod method, Type targetType, Type returnType, Type[] paramTypes, S targetObject, int targetObjectOffset, ArrayList<S> arguments, int[] argumentOffsets) throws BadBytecode {
        if (arguments.isEmpty()) {
            CtClass clazz = method.getDeclaringClass();

            if (Util.BOOLEAN_OBJECT_TYPE.getCtClass().equals(clazz) && method.getName().equals("booleanValue")) {
                return convertType(state, at, Util.BOOLEAN_OBJECT_TYPE, Type.BOOLEAN, targetObject, targetObjectOffset);
            } else if (Util.INTEGER_OBJECT_TYPE.getCtClass().equals(clazz) && method.getName().equals("intValue")) {
                return convertType(state, at, Util.INTEGER_OBJECT_TYPE, Type.INT, targetObject, targetObjectOffset);
            } else if (Util.LONG_OBJECT_TYPE.getCtClass().equals(clazz) && method.getName().equals("longValue")) {
                return convertType(state, at, Util.LONG_OBJECT_TYPE, Type.LONG, targetObject, targetObjectOffset);
            } else if (Util.FLOAT_OBJECT_TYPE.getCtClass().equals(clazz) && method.getName().equals("floatValue")) {
                return convertType(state, at, Util.FLOAT_OBJECT_TYPE, Type.FLOAT, targetObject, targetObjectOffset);
            } else if (Util.DOUBLE_OBJECT_TYPE.getCtClass().equals(clazz) && method.getName().equals("doubleValue")) {
                return convertType(state, at, Util.DOUBLE_OBJECT_TYPE, Type.DOUBLE, targetObject, targetObjectOffset);
            } else if (Util.BYTE_OBJECT_TYPE.getCtClass().equals(clazz) && method.getName().equals("byteValue")) {
                return convertType(state, at, Util.BYTE_OBJECT_TYPE, Type.BYTE, targetObject, targetObjectOffset);
            } else if (Util.CHARACTER_OBJECT_TYPE.getCtClass().equals(clazz) && method.getName().equals("charValue")) {
                return convertType(state, at, Util.CHARACTER_OBJECT_TYPE, Type.CHAR, targetObject, targetObjectOffset);
            } else if (Util.SHORT_OBJECT_TYPE.getCtClass().equals(clazz) && method.getName().equals("shortValue")) {
                return convertType(state, at, Util.SHORT_OBJECT_TYPE, Type.SHORT, targetObject, targetObjectOffset);
            }
        }

        return invokeMethodExceptUnboxing(state, at, method, targetType, returnType, paramTypes, targetObject, targetObjectOffset, arguments, argumentOffsets);
    }
}
