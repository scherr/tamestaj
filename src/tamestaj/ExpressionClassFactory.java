package tamestaj;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import javassist.*;

import java.util.*;

final class ExpressionClassFactory {
    private static int id = 0;

    private final static IdentityHashMap<CtMember, CtClass> memberToExpressionClass = new IdentityHashMap<>();

    static CtMethod getInvokeMethod(Source.Staged staged) {
        try {
            return makeExpressionClass(staged).getDeclaredMethod("invoke");
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateLanguageAcceptCheck(String expression, ImmutableSet<CtClass> acceptedLanguages, HashMap<CtClass, String> acceptedLanguageMap) {
        if (acceptedLanguages.isEmpty()) {
            return "false";
        }

        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (CtClass l : acceptedLanguages) {
            sb.append(expression).append(".isAcceptedBy(").append(acceptedLanguageMap.get(l)).append(")");
            if (i < acceptedLanguages.size() - 1) {
                sb.append(" || ");
            }

            i++;
        }

        return sb.toString();
    }

    private static Class<?> getClosureInterface(Type type) {
        if (!type.isReference()) {
            if (type.equals(Type.VOID)) {
                return VoidClosure.class;
            } else if (type.equals(Type.BOOLEAN)) {
                return BooleanClosure.class;
            } else if (type.equals(Type.INT)) {
                return IntegerClosure.class;
            } else if (type.equals(Type.LONG)) {
                return LongClosure.class;
            } else if (type.equals(Type.FLOAT)) {
                return FloatClosure.class;
            } else if (type.equals(Type.DOUBLE)) {
                return DoubleClosure.class;
            } else if (type.equals(Type.BYTE)) {
                return ByteClosure.class;
            } else if (type.equals(Type.CHAR)) {
                return CharacterClosure.class;
            } else if (type.equals(Type.SHORT)) {
                return ShortClosure.class;
            }

            throw new RuntimeException("Non-exhaustive matching!");
        } else {
            return ObjectClosure.class;
        }
    }

    private static String generateConversionSuffix(Type type) {
        if (type.isReference()) {
            return "";
        }

        return "." + Util.getConversionMethodName(type) + "()";
    }

    private static CtClass makeExpressionClass(Source.Staged staged, String superClassName, String memberClassName) {
        // Here we make the language class accessible!
        if (!Modifier.isPublic(staged.getLanguage().getModifiers())) {
            staged.getLanguage().setModifiers(Modifier.setPublic(staged.getLanguage().getModifiers()));
        }

        CtClass clazz;
        try {
            ClassPool cp = ClassPool.getDefault();

            CtClass superClass = cp.getCtClass(superClassName);
            clazz = cp.makeClass(superClass.getName() + "$Generated" + id + "$" + staged.getMember().getDeclaringClass().getName().replace(".", "_") + "$" + staged.getMember().getName().replace(".", "_"), superClass);
            id++;

            // Make accepted language static fields
            HashMap<CtClass, String> acceptedLanguageMap = new HashMap<>();
            for (Use.Argument a : staged.getArguments()) {
                for (CtClass l : a.getAcceptedLanguages()) {
                    if (!acceptedLanguageMap.containsKey(l) && !l.equals(Util.LANGUAGE_CLASS)) {
                        String lName = "acceptedLanguage" + acceptedLanguageMap.size();
                        int lPersistentId = Dispatcher.addPersistent(l);
                        CtField lField = CtField.make("private static final " + CtClass.class.getName() + " " + lName + " = ("  + CtClass.class.getName() + ") " + Util.DISPATCHER_CLASS.getName() + ".removePersistent(" + lPersistentId + ");", clazz);
                        clazz.addField(lField);

                        acceptedLanguageMap.put(l, lName);
                    }
                }
            }

            // Make language field
            String languageName = "language";
            int languagePersistentId = Dispatcher.addPersistent(staged.getLanguage());
            CtField languageField = CtField.make("private static final " + CtClass.class.getName() + " " + languageName + " = (" + CtClass.class.getName() + ") " + Util.DISPATCHER_CLASS.getName() + ".removePersistent(" + languagePersistentId + ");", clazz);
            clazz.addField(languageField);

            // Make member field
            String memberName = "member";
            int memberPersistentId = Dispatcher.addPersistent(staged.getMember());
            CtField memberField = CtField.make("private static final " + memberClassName + " " + memberName + " = (" + memberClassName + ")" + Util.DISPATCHER_CLASS.getName() + ".removePersistent(" + memberPersistentId + ");", clazz);
            clazz.addField(memberField);

            // Make constructor
            String constructorSource =
                    "private constructor(" + Util.EXPRESSION_CLASS.getName() + "[] arguments, " + Util.STATIC_INFO_CLASS.getName() + " staticInfo, " + Util.CLOSURE_HOLDER_CLASS.getName() + " closureHolder) {\n"
                  + "    super(arguments, staticInfo, closureHolder);\n"
                  + "}";
            CtConstructor constructor = CtNewConstructor.make(constructorSource, clazz);
            clazz.addConstructor(constructor);

            // Make factory-like invoke methods
            ImmutableList<Use.Argument> args = staged.getArguments();
            StringBuilder invokeSource = new StringBuilder();
            if (staged.isStrict() || Util.isCarrier(staged.getType())) {
                invokeSource.append("public static " + staged.getType().getCtClass().getName() + " invoke(");
            } else if (staged.getType().isReference()) {
                invokeSource.append("public static " + Util.LOCAL_CARRIER_CLASS.getName() + " invoke(");
            } else {
                invokeSource.append("public static " + clazz.getName() + " invoke(");
            }

            if (staged.getArguments().size() > 253) {
                invokeSource.append("Object[] objectArguments, ");
                if (!staged.getStaticInfoElements().isEmpty()) {
                    invokeSource.append(Util.STATIC_INFO_CLASS.getName() + " staticInfo, ");
                }
                invokeSource.append(Util.CLOSURE_HOLDER_CLASS.getName() + " closureHolder) {\n");
                for (int i = 0; i < args.size(); i++) {
                    if (Util.isGlobalCarrier(args.get(i).getType())) {
                        invokeSource.append("    ").append(Util.GLOBAL_CARRIER_CLASS.getName()).append(" argument").append(i).append(" = (").append(Util.GLOBAL_CARRIER_CLASS.getName()).append(") objectArguments[").append(i).append("];\n");
                    } else if (Util.isLocalCarrier(args.get(i).getType()) || (args.get(i).getType().isReference() && !Util.couldBeGlobalCarrier(args.get(i).getType()))) {
                        invokeSource.append("    ").append(Util.LOCAL_CARRIER_CLASS.getName()).append(" argument").append(i).append(" = (").append(Util.LOCAL_CARRIER_CLASS.getName()).append(") objectArguments[").append(i).append("];\n");
                    } else if (args.get(i).getType().isReference()) {
                        invokeSource.append("    ").append(Type.OBJECT.getCtClass().getName()).append(" argument").append(i).append(" = objectArguments[").append(i).append("];\n");
                    } else {
                        invokeSource.append("    ").append(Util.EXPRESSION_CLASS.getName()).append(" argument").append(i).append(" = (").append(Util.EXPRESSION_CLASS.getName()).append(") objectArguments[").append(i).append("];\n");
                    }

                }
            } else {
                for (int i = 0; i < args.size(); i++) {
                    if (Util.isGlobalCarrier(args.get(i).getType())) {
                        invokeSource.append(Util.GLOBAL_CARRIER_CLASS.getName()).append(" argument").append(i);
                    } else if (Util.isLocalCarrier(args.get(i).getType()) || (args.get(i).getType().isReference() && !Util.couldBeGlobalCarrier(args.get(i).getType()))) {
                        invokeSource.append(Util.LOCAL_CARRIER_CLASS.getName()).append(" argument").append(i);
                    } else if (args.get(i).getType().isReference()) {
                        invokeSource.append(Type.OBJECT.getCtClass().getName()).append(" argument").append(i);
                    } else {
                        invokeSource.append(Util.EXPRESSION_CLASS.getName()).append(" argument").append(i);
                    }
                    invokeSource.append(", ");
                }

                if (!staged.getStaticInfoElements().isEmpty()) {
                    invokeSource.append(Util.STATIC_INFO_CLASS.getName() + " staticInfo, ");
                }
                invokeSource.append(Util.CLOSURE_HOLDER_CLASS.getName() + " closureHolder) {\n");
            }

            invokeSource.append(
                      "    " + Util.EXPRESSION_CLASS.getName() + " payload;\n"
                    + "    " + Util.EXPRESSION_CLASS.getName() + "[] arguments = new " + Util.EXPRESSION_CLASS.getName() + "[" + args.size() + "];\n"
            );
            for (int i = 0; i < args.size(); i++) {
                if (Util.isGlobalCarrier(args.get(i).getType())) {
                    invokeSource.append(
                              "    payload = " + Util.DISPATCHER_CLASS.getName() + ".unloadGlobalCarrierChecked(argument" + i + ");\n"
                            + "    if (!(" + generateLanguageAcceptCheck("payload", args.get(i).getAcceptedLanguages(), acceptedLanguageMap) + ")) {\n"
                            + "        arguments[" + i + "] = " + Util.DISPATCHER_CLASS.getName() + ".selfLiftGlobalCarrier(argument" + i + ");\n"
                            + "    } else {\n"
                            + "        arguments[" + i + "] = payload;\n"
                            + "    }\n"
                    );
                } else if (Util.isLocalCarrier(args.get(i).getType()) || (args.get(i).getType().isReference() && !Util.couldBeGlobalCarrier(args.get(i).getType()))) {
                    invokeSource.append(
                              "    payload = " + Util.DISPATCHER_CLASS.getName() + ".unloadLocalCarrierChecked(argument" + i + ");\n"
                            + "    if (" + generateLanguageAcceptCheck("payload", args.get(i).getAcceptedLanguages(), acceptedLanguageMap) + ") {\n"
                            + "        arguments[" + i + "] = payload.getRaw();\n"
                            + "    } else {\n"
                            + "        payload.evaluate();\n"

                            // This handles the weird case when the materialized argument could be globally carried and might need unloading and checking
                            // We know that it is an ObjectValue instance
                            + "        " + Util.EXPRESSION_CLASS.getName() + " value = payload.asValueIfEvaluated();\n"
                            + "        Object obj = value.materializeAsObject(); \n"
                            + "        if (obj instanceof " + Util.GLOBAL_CARRIER_CLASS.getName() + ") {\n"
                            + "            payload = " + Util.DISPATCHER_CLASS.getName() + ".unloadGlobalCarrier((" + Util.GLOBAL_CARRIER_CLASS.getName() + ") obj);\n"
                            + "            if (payload == null || !(" + generateLanguageAcceptCheck("payload", args.get(i).getAcceptedLanguages(), acceptedLanguageMap) + ")) {\n"
                            + "                arguments[" + i + "] = value;\n"
                            + "            } else {\n"
                            + "                arguments[" + i + "] = payload;\n"
                            + "            }\n"
                            + "        } else {\n"
                            + "            arguments[" + i + "] = value;\n"
                            + "        }\n"

                            + "    }\n"
                    );
                } else if (args.get(i).getType().isReference()) {
                    invokeSource.append(
                              "    if (argument" + i + " instanceof " + Util.LOCAL_CARRIER_CLASS.getName() + ") {\n"
                            + "        payload = " + Util.DISPATCHER_CLASS.getName() + ".unloadLocalCarrierChecked((" + Util.LOCAL_CARRIER_CLASS.getName() + ") argument" + i + ");\n"
                            + "        if (" + generateLanguageAcceptCheck("payload", args.get(i).getAcceptedLanguages(), acceptedLanguageMap) + ") {\n"
                            + "            arguments[" + i + "] = payload.getRaw();\n"
                            + "        } else {\n"
                            + "            payload.evaluate();\n"

                            // This handles the weird case when the materialized argument could be globally carried and might need unloading and checking
                            // We know that it is an ObjectValue instance
                            + "            " + Util.EXPRESSION_CLASS.getName() + " value = payload.asValueIfEvaluated();\n"
                            + "            Object obj = value.materializeAsObject(); \n"
                            + "            if (obj instanceof " + Util.GLOBAL_CARRIER_CLASS.getName() + ") {\n"
                            + "                payload = " + Util.DISPATCHER_CLASS.getName() + ".unloadGlobalCarrier((" + Util.GLOBAL_CARRIER_CLASS.getName() + ") obj);\n"
                            + "                if (payload == null || !(" + generateLanguageAcceptCheck("payload", args.get(i).getAcceptedLanguages(), acceptedLanguageMap) + ")) {\n"
                            + "                    arguments[" + i + "] = value;\n"
                            + "                } else {\n"
                            + "                    arguments[" + i + "] = payload;\n"
                            + "                }\n"
                            + "            } else {\n"
                            + "                arguments[" + i + "] = value;\n"
                            + "            }\n"

                            + "        }\n"
                            + "    } else if (argument" + i + " instanceof " + Util.GLOBAL_CARRIER_CLASS.getName() + ") {\n"
                            + "        payload = " + Util.DISPATCHER_CLASS.getName() + ".unloadGlobalCarrierChecked((" + Util.GLOBAL_CARRIER_CLASS.getName() + ") argument" + i + ");\n"
                            + "        if (!(" + generateLanguageAcceptCheck("payload", args.get(i).getAcceptedLanguages(), acceptedLanguageMap) + ")) {\n"
                            + "            arguments[" + i + "] = " + Util.DISPATCHER_CLASS.getName() + ".selfLiftGlobalCarrier((" + Util.GLOBAL_CARRIER_CLASS.getName() + ") argument" + i + ");\n"
                            + "        } else {\n"
                            + "            arguments[" + i + "] = payload;\n"
                            + "        }\n"
                            + "    } else {\n"
                            + "        arguments[" + i + "] = " + Util.DISPATCHER_CLASS.getName() + "." + Util.getLiftMethodName(Type.OBJECT) + "(argument" + i + ");\n"
                            + "    }\n"
                    );
                } else {
                    invokeSource.append(
                            "    if (" + generateLanguageAcceptCheck("argument" + i, args.get(i).getAcceptedLanguages(), acceptedLanguageMap) + ") {\n"
                            + "        arguments[" + i + "] = argument" + i + generateConversionSuffix(args.get(i).getType()) + ".getRaw();\n"
                            + "    } else {\n"
                            + "        argument" + i + ".evaluate();\n"
                            + "        arguments[" + i + "] = argument" + i + ".asValueIfEvaluated();\n"
                            + "    }\n"
                    );
                }
            }

            String construction;
            if (!staged.getStaticInfoElements().isEmpty()) {
                construction = "new " + clazz.getName() + "(arguments, staticInfo, closureHolder)";
            } else {
                construction = "new " + clazz.getName() + "(arguments, null, closureHolder)";
            }
            if (staged.isStrict()) {
                Type type = staged.getType();
                if (type.isReference()) {
                    invokeSource.append("    return (" + type.getCtClass().getName() + ") (" + construction + ").materializeAsObject();\n");
                } else if (type.equals(Type.BOOLEAN)) {
                    invokeSource.append("    return (" + construction + ").materializeAsBoolean();\n");
                } else if (type.equals(Type.INT)) {
                    invokeSource.append("    return (" + construction + ").materializeAsInteger();\n");
                } else if (type.equals(Type.LONG)) {
                    invokeSource.append("    return (" + construction + ").materializeAsLong();\n");
                } else if (type.equals(Type.FLOAT)) {
                    invokeSource.append("    return (" + construction + ").materializeAsFloat();\n");
                } else if (type.equals(Type.DOUBLE)) {
                    invokeSource.append("    return (" + construction + ").materializeAsDouble();\n");
                } else if (type.equals(Type.BYTE)) {
                    invokeSource.append("    return (" + construction + ").materializeAsByte();\n");
                } else if (type.equals(Type.CHAR)) {
                    invokeSource.append("    return (" + construction + ").materializeAsCharacter();\n");
                } else if (type.equals(Type.SHORT)) {
                    invokeSource.append("    return (" + construction + ").materializeAsShort();\n");
                } else if (type.equals(Type.VOID)) {
                    invokeSource.append("    return (" + construction + ").evaluate();\n");
                }
            } else {
                if (Util.isGlobalCarrier(staged.getType())) {
                    if (staged.getType().getCtClass().equals(Util.GLOBAL_CARRIER_CLASS)) {
                        invokeSource.append("    return new " + Util.GLOBAL_CARRIER_CLASS.getName() + "(" + construction + ");\n");
                    } else {
                        CarrierTransformer.transformCarrierChecked(staged.getType().getCtClass());
                        invokeSource.append("    return new " + staged.getType().getCtClass().getName() + "((" + Util.DISAMBIGUATION_PARAMETER_CLASS.getName() + ") null, " + construction + ");\n");
                    }
                } else if (Util.isLocalCarrier(staged.getType())) {
                    if (staged.getType().getCtClass().equals(Util.LOCAL_CARRIER_CLASS)) {
                        invokeSource.append("    return new " + Util.LOCAL_CARRIER_CLASS.getName() + "(" + construction + ");\n");
                    } else {
                        CarrierTransformer.transformCarrierChecked(staged.getType().getCtClass());
                        invokeSource.append("    return new " + staged.getType().getCtClass().getName() + "((" + Util.DISAMBIGUATION_PARAMETER_CLASS.getName() + ") null, " + construction + ");\n");
                    }
                } else if (staged.getType().isReference()) {
                    invokeSource.append("    return new " + Util.LOCAL_CARRIER_CLASS.getName() + "(" + construction + ");\n");
                } else {
                    invokeSource.append("    return " + construction + ";\n");
                }
            }
            invokeSource.append("}");
            CtMethod invoke = CtMethod.make(invokeSource.toString(), clazz);
            clazz.addMethod(invoke);

            // Make isomorphic hash code method
            String isomorphicHashCodeSource =
                    "int isomorphicHashCode() {\n"
                  + "    if (!$0.isomorphicHashCodeHasBeenCalculated) {\n"
                  + "        super.isomorphicHashCode(" + staged.getMember().hashCode() + ");\n"
                  + "    }"
                  + "    return $0.isomorphicHashCode;"
                  + "}";
            CtMethod isomorphicHashCode = CtMethod.make(isomorphicHashCodeSource, clazz);
            clazz.addMethod(isomorphicHashCode);

            // Make isomorphism check method
            String isIsomorphicToSource =
                      "boolean isIsomorphicTo(java.util.IdentityHashMap identityMap, " + Util.EXPRESSION_CLASS.getName() + " expression) {\n"
                    + "    if (!(expression instanceof " + clazz.getName() + ")) { return false; }\n"
                    + "    return super.isIsomorphicTo(identityMap, expression);\n"
                    + "}";
            CtMethod isIsomorphicTo = CtMethod.make(isIsomorphicToSource, clazz);
            clazz.addMethod(isIsomorphicTo);

            // Make cache clone (with empty leaves to save memory) creation method
            String cacheCloneSource =
                    Util.EXPRESSION_CLASS.getName() + " cacheClone(java.util.IdentityHashMap identityMap) {\n"
                    + "    " + Util.EXPRESSION_CLASS.getName() + " e = (" + Util.EXPRESSION_CLASS.getName() + ") identityMap.get(this);\n"
                    + "    if (e != null) {\n"
                    + "        return e;\n"
                    + "    } else {\n"
                    + "        " + Util.EXPRESSION_CLASS.getName() + "[] clonedArguments = super.cacheCloneArguments(identityMap);\n"
                    + "        if (clonedArguments != null) {\n"
                    + "            " + clazz.getName() + " s = new " + clazz.getName() + "(clonedArguments, $0.staticInfo, null);\n"
                    + "            s.isomorphicHashCode = $0.isomorphicHashCode();\n"
                    + "            s.isomorphicHashCodeHasBeenCalculated = true;\n"
                    + "            identityMap.put(this, s);\n"
                    + "            return s;\n"
                    + "        } else {\n"
                    + "            identityMap.put(this, this);\n"
                    + "            return this;\n"
                    + "        }\n"
                    + "    }\n"
                    + "}";
            CtMethod cacheClone = CtMethod.make(cacheCloneSource, clazz);
            clazz.addMethod(cacheClone);

            // Make language acceptance check method
            String isAcceptedBySource = "boolean isAcceptedBy(" + CtClass.class.getName() + " language) { return $0.language.equals(language); }";
            CtMethod isAcceptedBy = CtMethod.make(isAcceptedBySource, clazz);
            clazz.addMethod(isAcceptedBy);

            // Make polymorphic member retrieval method
            String getMemberSource = "public " + memberClassName + " getMember() { return $0." + memberName + "; }";
            CtMethod getMember = CtMethod.make(getMemberSource, clazz);
            clazz.addMethod(getMember);

            // Make evaluation method
            Class<?> closureInterface = getClosureInterface(staged.getType());
            String evaluateSource;
            if (Util.isGlobalCarrier(staged.getType()) && !staged.isStrict()) {
                evaluateSource = "public void evaluate() { throw new UnsupportedOperationException(); }";
            } else {
                evaluateSource =
                          "public void evaluate() {\n"
                        + "    if ($0.value != null) { return; }\n"
                        + "    " + closureInterface.getName() + " closure;\n"
                        + "    " + Util.ENVIRONMENT_CLASS.getName() + " environment;\n"
                        + "    if ($0.closureHolder == null) {\n"
                        + "        " + Util.CLOSURE_HOLDER_CLASS.getName() + " cachedClosureHolder = " + GlobalCache.class.getName() + ".getCachedClosureHolder($0);\n"
                        + "        if (cachedClosureHolder == null) {\n"
                        + "            " + Util.BINDER_CLASS.getName() + " binder = new " + Util.BINDER_CLASS.getName() + "($0);\n"
                        + "            closure = " + staged.getLanguage().getName() + ".make" + closureInterface.getSimpleName() + "($0, binder, false);\n"
                        + "            environment = new " + Util.ENVIRONMENT_CLASS.getName() + "($0, binder.getBoundCount());\n"
                        + "            if (!binder.inspectionOccurred()) {\n"
                        + "                " + GlobalCache.class.getName() + ".cache($0, closure, binder.getBoundCount());\n"
                        + "            }\n"
                        + "        } else {\n"
                        + "            closure = (" + closureInterface.getName() + ") cachedClosureHolder.getClosure();\n"
                        + "            environment = new " + Util.ENVIRONMENT_CLASS.getName() + "($0, cachedClosureHolder.getEnvironmentSize());\n"
                        + "        }\n"
                        + "    } else {\n"
                        + "        synchronized ($0.closureHolder) {"
                        + "            closure = (" + closureInterface.getName() + ") $0.closureHolder.getClosure();\n"
                        + "            if (closure == null) {\n"
                        + "                " + Util.CLOSURE_HOLDER_CLASS.getName() + " cachedClosureHolder = " + GlobalCache.class.getName() + ".getCachedClosureHolder($0);\n"
                        + "                if (cachedClosureHolder == null) {\n"
                        + "                    " + Util.BINDER_CLASS.getName() + " binder = new " + Util.BINDER_CLASS.getName() + "($0);\n"
                        + "                    closure = " + staged.getLanguage().getName() + ".make" + closureInterface.getSimpleName() + "($0, binder, $0.closureHolder.isPermanent());\n"
                        + "                    environment = new " + Util.ENVIRONMENT_CLASS.getName() + "($0, binder.getBoundCount());\n"
                        + "                    if (!binder.inspectionOccurred()) {\n"
                        + "                        $0.closureHolder.set(closure, binder.getBoundCount());\n"
                        + "                        " + GlobalCache.class.getName() + ".cache($0, closure, binder.getBoundCount());\n"
                        + "                    }\n"
                        + "                } else {\n"
                        + "                    closure = (" + closureInterface.getName() + ") cachedClosureHolder.getClosure();\n"
                        + "                    environment = new " + Util.ENVIRONMENT_CLASS.getName() + "($0, cachedClosureHolder.getEnvironmentSize());\n"
                        + "                    $0.closureHolder.set(closure, cachedClosureHolder.getEnvironmentSize());\n"
                        + "                }\n"
                        + "            } else {\n"
                        + "                environment = new " + Util.ENVIRONMENT_CLASS.getName() + "($0, $0.closureHolder.getEnvironmentSize());\n"
                        + "           }\n"
                        + "        }\n"
                        + "    }\n"
                        + (
                        staged.getType().equals(Type.VOID) ?
                          "    closure.evaluate(environment);\n"
                        :
                          "    $0.value = " + Util.DISPATCHER_CLASS.getName() + "." + Util.getLiftMethodName(staged.getType()) + "(closure.evaluate(environment));\n"
                        )
                        + "}";
            }
            CtMethod evaluate = CtMethod.make(evaluateSource, clazz);
            clazz.addMethod(evaluate);

            clazz.toClass();
        } catch (CannotCompileException | NotFoundException e) {
            throw new RuntimeException(e);
        }

        return clazz;
    }

    private static CtClass makeExpressionClass(Source.Staged staged) {
        CtMember key = staged.getMember();

        CtClass clazz = memberToExpressionClass.get(key);
        if (clazz == null) {
            if (staged instanceof Source.Staged.FieldRead) {
                clazz = makeExpressionClass(staged, Expression.FieldRead.class.getName(), CtField.class.getName());
            } else if (staged instanceof Source.Staged.FieldAssignment) {
                clazz = makeExpressionClass(staged, Expression.FieldAssignment.class.getName(), CtField.class.getName());
            } else if (staged instanceof Source.Staged.MethodInvocation) {
                clazz = makeExpressionClass(staged, Expression.MethodInvocation.class.getName(), CtMethod.class.getName());
            } else {
                // This should never happen!
                throw new RuntimeException();
            }
            memberToExpressionClass.put(key, clazz);
        }

        return clazz;
    }
}
