package tamestaj;

import javassist.*;

final class Util {
    static final Type STRING_TYPE;
    static final Type CLASS_TYPE;

    static final Type BOOLEAN_OBJECT_TYPE;
    static final Type INTEGER_OBJECT_TYPE;
    static final Type LONG_OBJECT_TYPE;
    static final Type FLOAT_OBJECT_TYPE;
    static final Type DOUBLE_OBJECT_TYPE;
    static final Type BYTE_OBJECT_TYPE;
    static final Type CHARACTER_OBJECT_TYPE;
    static final Type SHORT_OBJECT_TYPE;
    static final Type VOID_OBJECT_TYPE;

    static final CtClass LANGUAGE_CLASS;
    static final CtClass EXPRESSION_CLASS;
    static final CtClass CONVERSION_CLASS;
    static final CtClass VALUE_CLASS;
    static final CtClass OBJECT_VALUE_CLASS;

    static final CtClass GLOBAL_CARRIER_CLASS;
    static final CtClass LOCAL_CARRIER_CLASS;

    // Run-time (compile-time invisible) support classes
    static final CtClass CLOSURE_HOLDER_CLASS;
    static final CtClass TRACE_CACHE_CLASS;
    static final CtClass TRACE_CLASS;
    static final CtClass DISPATCHER_CLASS;
    static final CtClass STATIC_INFO_CLASS;

    static final CtClass BINDER_CLASS;
    static final CtClass ENVIRONMENT_CLASS;

    static final CtClass DISAMBIGUATION_PARAMETER_CLASS;

    static {
        ClassPool cp = ClassPool.getDefault();
        try {
            STRING_TYPE = Type.of(cp.get("java.lang.String"));
            CLASS_TYPE = Type.of(cp.get("java.lang.Class"));

            BOOLEAN_OBJECT_TYPE = Type.of(cp.get("java.lang.Boolean"));
            INTEGER_OBJECT_TYPE = Type.of(cp.get("java.lang.Integer"));
            LONG_OBJECT_TYPE = Type.of(cp.get("java.lang.Long"));
            FLOAT_OBJECT_TYPE = Type.of(cp.get("java.lang.Float"));
            DOUBLE_OBJECT_TYPE = Type.of(cp.get("java.lang.Double"));
            BYTE_OBJECT_TYPE = Type.of(cp.get("java.lang.Byte"));
            CHARACTER_OBJECT_TYPE = Type.of(cp.get("java.lang.Character"));
            SHORT_OBJECT_TYPE = Type.of(cp.get("java.lang.Short"));
            VOID_OBJECT_TYPE = Type.of(cp.get("java.lang.Void"));

            LANGUAGE_CLASS = cp.get("tamestaj.Language");
            EXPRESSION_CLASS = cp.get("tamestaj.Expression");
            CONVERSION_CLASS = cp.get("tamestaj.Expression$Conversion");
            VALUE_CLASS = cp.get("tamestaj.Expression$Value");
            OBJECT_VALUE_CLASS = cp.get("tamestaj.Expression$ObjectValue");

            GLOBAL_CARRIER_CLASS = cp.get("tamestaj.GlobalCarrier");
            LOCAL_CARRIER_CLASS = cp.get("tamestaj.LocalCarrier");

            CLOSURE_HOLDER_CLASS = cp.get("tamestaj.ClosureHolder");
            TRACE_CACHE_CLASS = cp.get("tamestaj.TraceCache");
            TRACE_CLASS = cp.get("tamestaj.Trace");
            DISPATCHER_CLASS = cp.get("tamestaj.Dispatcher");
            STATIC_INFO_CLASS = cp.get("tamestaj.StaticInfo");

            BINDER_CLASS = cp.get("tamestaj.Environment$Binder");
            ENVIRONMENT_CLASS = cp.get("tamestaj.Environment");

            DISAMBIGUATION_PARAMETER_CLASS = cp.makeClass("tamestaj.DisambiguationParameter");
            try {
                Util.DISAMBIGUATION_PARAMETER_CLASS.toClass();
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    static boolean isSafeConversion(CtBehavior behavior, Type from, Type to) {
        if (from.equals(to)) {
            return true;
        } else if (from.isReference() && to.isReference() && to.isAssignableFrom(from, false)) {
            return true;
        } else if (!from.isReference() && to.isReference()) {
            return true;
        } else if (from.isReference() && !to.isReference()) {
            if (from.equals(BOOLEAN_OBJECT_TYPE) && to.equals(Type.BOOLEAN)) {
                return true;
            } else if (from.equals(INTEGER_OBJECT_TYPE) && to.equals(Type.INT)) {
                return true;
            } else if (from.equals(LONG_OBJECT_TYPE) && to.equals(Type.LONG)) {
                return true;
            } else if (from.equals(FLOAT_OBJECT_TYPE) && to.equals(Type.DOUBLE)) {
                return true;
            } else if (from.equals(DOUBLE_OBJECT_TYPE) && to.equals(Type.DOUBLE)) {
                return true;
            } else if (from.equals(BYTE_OBJECT_TYPE) && to.equals(Type.BYTE)) {
                return true;
            } else if (from.equals(CHARACTER_OBJECT_TYPE) && to.equals(Type.CHAR)) {
                return true;
            } else if (from.equals(SHORT_OBJECT_TYPE) && to.equals(Type.SHORT)) {
                return true;
            } else {
                return false;
            }
        } else if (from.equals(Type.BYTE) &&
                (to.equals(Type.SHORT) || to.equals(Type.INT) || to.equals(Type.LONG) || to.equals(Type.FLOAT) || to.equals(Type.DOUBLE))
                ) {
            return true;
        } else if (from.equals(Type.SHORT) &&
                (to.equals(Type.INT) || to.equals(Type.LONG) || to.equals(Type.FLOAT) || to.equals(Type.DOUBLE))
                ) {
            return true;
        } else if (from.equals(Type.CHAR) &&
                (to.equals(Type.INT) || to.equals(Type.LONG) || to.equals(Type.FLOAT) || to.equals(Type.DOUBLE))
                ) {
            return true;
        } else if (from.equals(Type.INT) &&
                (to.equals(Type.LONG) || to.equals(Type.DOUBLE))
                ) {
            return true;
        } else if (Modifier.isStrict(behavior.getModifiers()) && from.equals(Type.FLOAT) && to.equals(Type.DOUBLE)) {
            return true;
        } else {
            return false;
        }
    }

    static String getLiftMethodName(Type type) {
        if (!type.isReference()) {
            if (type.equals(Type.BOOLEAN)) {
                return "liftBoolean";
            } else if (type.equals(Type.INT)) {
                return "liftInteger";
            } else if (type.equals(Type.LONG)) {
                return "liftLong";
            } else if (type.equals(Type.FLOAT)) {
                return "liftFloat";
            } else if (type.equals(Type.DOUBLE)) {
                return "liftDouble";
            } else if (type.equals(Type.BYTE)) {
                return "liftByte";
            } else if (type.equals(Type.CHAR)) {
                return "liftCharacter";
            } else if (type.equals(Type.SHORT)) {
                return "liftShort";
            }

            throw new RuntimeException("Non-exhaustive matching!");
        } else {
            return "liftObject";
        }
    }

    static String getConstantLiftMethodName(Type type) {
        if (!type.isReference()) {
            if (type.equals(Type.BOOLEAN)) {
                return "liftConstantBoolean";
            } else if (type.equals(Type.INT)) {
                return "liftConstantInteger";
            } else if (type.equals(Type.LONG)) {
                return "liftConstantLong";
            } else if (type.equals(Type.FLOAT)) {
                return "liftConstantFloat";
            } else if (type.equals(Type.DOUBLE)) {
                return "liftConstantDouble";
            } else if (type.equals(Type.BYTE)) {
                return "liftConstantByte";
            } else if (type.equals(Type.CHAR)) {
                return "liftConstantCharacter";
            } else if (type.equals(Type.SHORT)) {
                return "liftConstantShort";
            }

            throw new RuntimeException("Non-exhaustive matching!");
        } else {
            return "liftConstantObject";
        }
    }

    static boolean isGlobalCarrier(CtClass clazz) {
        if (clazz.subclassOf(GLOBAL_CARRIER_CLASS)) {
            return true;
        } else {
            return false;
        }
    }
    static boolean isGlobalCarrier(Type type) {
        return isGlobalCarrier(type.getCtClass());
    }

    static boolean isLocalCarrier(CtClass clazz) {
        if (clazz.subclassOf(LOCAL_CARRIER_CLASS)) {
            return true;
        } else {
            return false;
        }
    }
    static boolean isLocalCarrier(Type type) {
        return isLocalCarrier(type.getCtClass());
    }

    static boolean couldBeLocalCarrier(CtClass clazz) {
        return clazz.subclassOf(LOCAL_CARRIER_CLASS) || clazz.equals(Type.OBJECT.getCtClass()) || clazz.isInterface();
    }
    static boolean couldBeLocalCarrier(Type type) {
        return couldBeLocalCarrier(type.getCtClass());
    }

    static boolean couldBeGlobalCarrier(CtClass clazz) {
        return clazz.subclassOf(GLOBAL_CARRIER_CLASS) || clazz.equals(Type.OBJECT.getCtClass()) || clazz.isInterface();
    }
    static boolean couldBeGlobalCarrier(Type type) {
        return couldBeGlobalCarrier(type.getCtClass());
    }

    static boolean isCarrier(CtClass clazz) {
        return isGlobalCarrier(clazz) || isLocalCarrier(clazz);
    }
    static boolean isCarrier(Type type) {
        return isGlobalCarrier(type) || isLocalCarrier(type);
    }

    static boolean couldBeCarrier(CtClass clazz) {
        return clazz.subclassOf(GLOBAL_CARRIER_CLASS) || clazz.subclassOf(LOCAL_CARRIER_CLASS) || clazz.equals(Type.OBJECT.getCtClass()) || clazz.isInterface();
    }
    static boolean couldBeCarrier(Type type) {
        return couldBeCarrier(type.getCtClass());
    }

    static String getConversionMethodName(Type type) {
        if (type.equals(Type.BOOLEAN)) {
            return "convertToBoolean";
        } else if (type.equals(Type.INT)) {
            return "convertToInteger";
        } else if (type.equals(Type.LONG)) {
            return "convertToLong";
        } else if (type.equals(Type.FLOAT)) {
            return "convertToFloat";
        } else if (type.equals(Type.DOUBLE)) {
            return "convertToDouble";
        } else if (type.equals(Type.BYTE)) {
            return "convertToByte";
        } else if (type.equals(Type.CHAR)) {
            return "convertToCharacter";
        } else if (type.equals(Type.SHORT)) {
            return "convertToShort";
        }

        throw new RuntimeException("Non-exhaustive matching!");
    }
}
