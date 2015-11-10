package tamestaj;

import com.google.common.collect.ImmutableSet;
import tamestaj.annotations.Stage;
import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.annotation.*;

import java.util.Arrays;
import java.util.IdentityHashMap;

final class StageAnnotation {
    private static final IdentityHashMap<CtField, StageAnnotation> fieldCache = new IdentityHashMap<>();
    private static final IdentityHashMap<CtMethod, StageAnnotation> methodCache = new IdentityHashMap<>();

    private final CtMember annotatedMember;
    private final CtClass language;
    private final boolean isStrict;
    private final ImmutableSet<StaticInfo.Element> staticInfoElements;

    private StageAnnotation(CtMember annotatedMember, CtClass language, boolean isStrict, ImmutableSet<StaticInfo.Element> staticInfoElements) {
        this.annotatedMember = annotatedMember;
        this.language = language;
        this.isStrict = isStrict;
        this.staticInfoElements = staticInfoElements;
    }

    CtMember getAnnotatedMember() { return annotatedMember; }
    CtClass getLanguage() {
        return language;
    }
    boolean isStrict() { return isStrict; }
    ImmutableSet<StaticInfo.Element> getStaticInfoElements() { return staticInfoElements; }

    static StageAnnotation forMethod(CtMethod method) {
        StageAnnotation ann = methodCache.get(method);
        if (ann != null) {
            return ann;
        }

        ann = forMethodInternal(method.getDeclaringClass(), method);
        if (ann != null) {
            methodCache.put(method, ann);
        }

        return ann;
    }

    static StageAnnotation forMethod(CtClass clazz, CtMethod method) {
        StageAnnotation ann = methodCache.get(method);
        if (ann != null) {
            return ann;
        }

        ann = forMethodInternal(clazz, method);
        if (ann != null) {
            methodCache.put(method, ann);
        }

        return ann;
    }

    static StageAnnotation forMethodInternal(CtClass clazz, CtMethod method) {
        StageAnnotation cached = methodCache.get(method);
        if (cached != null) { return cached; }

        int modifiers = method.getModifiers();
        AnnotationsAttribute attr = (AnnotationsAttribute) method.getMethodInfo2().getAttribute(AnnotationsAttribute.visibleTag);
        if (attr != null) {
            Annotation ann = attr.getAnnotation(Stage.class.getName());
            if (ann != null) {
                String languageClassName = ((ClassMemberValue) ann.getMemberValue("language")).getValue();
                MemberValue isStrictValue = ann.getMemberValue("isStrict");
                ArrayMemberValue staticInfoElementsArrayMemberValue = (ArrayMemberValue) ann.getMemberValue("staticInfoElements");
                try {
                    CtClass language = clazz.getClassPool().get(languageClassName);
                    if (!ConfigureAnnotation.forClass(language).isStageAccessibleFrom(clazz)) {
                        if (!Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
                            return forMethodSuper(clazz, method);
                        }
                    } else {
                        boolean isStrict;

                        if (isStrictValue == null) {
                            /*
                            Stage stage = (Stage) ann.toAnnotationType(ClassLoader.getSystemClassLoader(), clazz.getClassPool());
                            isStrict = stage.isStrict();
                            */
                            isStrict = false;
                        } else {
                            isStrict = ((BooleanMemberValue) isStrictValue).getValue();
                        }

                        if (method.getReturnType().equals(CtClass.voidType)) {
                            isStrict = true;
                        }

                        ImmutableSet<StaticInfo.Element> staticInfoElements;
                        if (staticInfoElementsArrayMemberValue == null) {
                            staticInfoElements = ImmutableSet.of();
                        } else {
                            MemberValue[] staticInfoElementsMemberValues = staticInfoElementsArrayMemberValue.getValue();
                            if (staticInfoElementsMemberValues.length == 0) {
                                staticInfoElements = ImmutableSet.of();
                            } else {
                                ImmutableSet.Builder<StaticInfo.Element> builder = ImmutableSet.builder();
                                for (MemberValue memberValue : staticInfoElementsMemberValues) {
                                    String enumName = ((EnumMemberValue) memberValue).getValue();
                                    builder.add(Enum.valueOf(StaticInfo.Element.class, enumName));
                                }
                                staticInfoElements = builder.build();
                            }
                        }
                        return new StageAnnotation(method, language, isStrict, staticInfoElements);
                    }
                } catch (NotFoundException e) {
                    throw new RuntimeException(e);
                }
            } else {
                if (!Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
                    return forMethodSuper(clazz, method);
                }
            }
        } else {
            if (!Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
                return forMethodSuper(clazz, method);
            }
        }

        return null;
    }

    private static CtMethod findMatchingMethod(CtClass clazz, CtMethod method) {
        CtMethod bridgeMethod = BridgeMethodResolver.getBridgeMethod(method);
        if (bridgeMethod != null) {
            method = bridgeMethod;
        }

        String methodName = method.getName();
        try {
            CtClass returnType = method.getReturnType();
            CtClass[] params = method.getParameterTypes();
            for (CtMethod m : clazz.getDeclaredMethods()) {
                if (methodName.equals(m.getName()) && returnType.subtypeOf(m.getReturnType()) && Arrays.equals(params, m.getParameterTypes())) {
                    return m;
                }
            }
        } catch (NotFoundException e) {
            // throw new RuntimeException(e);
        }

        return null;
    }

    private static StageAnnotation forMethodSuper(CtClass clazz, CtMethod method) {
        CtClass s;
        try {
            s = clazz.getSuperclass();
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
        if (s != null) {
            CtMethod m = findMatchingMethod(s, method);

            if (m == null) {
                return forMethodSuper(s, method);
            } else {
                StageAnnotation ann = forMethodInternal(s, m);
                if (ann != null) {
                    methodCache.put(m, ann);
                }

                return ann;
            }
        }

        return null;
    }

    static StageAnnotation forField(CtField field) {
        StageAnnotation ann = fieldCache.get(field);
        if (ann != null) {
            return ann;
        }

        ann = forFieldInternal(field.getDeclaringClass(), field);
        if (ann != null) {
            fieldCache.put(field, ann);
        }

        return ann;
    }

    static StageAnnotation forField(CtClass clazz, CtField field) throws NotFoundException {
        StageAnnotation ann = fieldCache.get(field);
        if (ann != null) {
            return ann;
        }

        ann = forFieldInternal(clazz, field);
        if (ann != null) {
            fieldCache.put(field, ann);
        }

        return ann;
    }

    private static StageAnnotation forFieldInternal(CtClass clazz, CtField field)  {
        StageAnnotation cached = fieldCache.get(field);
        if (cached != null) { return cached; }

        int modifiers = field.getModifiers();
        AnnotationsAttribute attr = (AnnotationsAttribute) field.getFieldInfo2().getAttribute(AnnotationsAttribute.visibleTag);
        if (attr != null) {
            Annotation ann = attr.getAnnotation(Stage.class.getName());
            if (ann != null) {
                String languageClassName = ((ClassMemberValue) ann.getMemberValue("language")).getValue();
                MemberValue isStrictValue = ann.getMemberValue("isStrict");
                ArrayMemberValue staticInfoElementsArrayMemberValue = (ArrayMemberValue) ann.getMemberValue("staticInfoElements");
                try {
                    CtClass language = clazz.getClassPool().get(languageClassName);
                    if (!ConfigureAnnotation.forClass(language).isStageAccessibleFrom(clazz)) {
                        if (!Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
                            return forFieldSuper(clazz, field);
                        }
                    } else {
                        boolean isStrict;

                        if (isStrictValue == null) {
                            /*
                            Stage stage = (Stage) ann.toAnnotationType(ClassLoader.getSystemClassLoader(), clazz.getClassPool());
                            isStrict = stage.isStrict();
                            */
                            isStrict = false;
                        } else {
                            isStrict = ((BooleanMemberValue) isStrictValue).getValue();
                        }

                        ImmutableSet<StaticInfo.Element> staticInfoElements;
                        if (staticInfoElementsArrayMemberValue == null) {
                            staticInfoElements = ImmutableSet.of();
                        } else {
                            MemberValue[] staticInfoElementsMemberValues = staticInfoElementsArrayMemberValue.getValue();
                            if (staticInfoElementsMemberValues.length == 0) {
                                staticInfoElements = ImmutableSet.of();
                            } else {
                                ImmutableSet.Builder<StaticInfo.Element> builder = ImmutableSet.builder();
                                for (MemberValue memberValue : staticInfoElementsMemberValues) {
                                    String enumName = ((EnumMemberValue) memberValue).getValue();
                                    builder.add(Enum.valueOf(StaticInfo.Element.class, enumName));
                                }
                                staticInfoElements = builder.build();
                            }
                        }
                        return new StageAnnotation(field, clazz.getClassPool().get(languageClassName), isStrict, staticInfoElements);
                    }
                } catch (NotFoundException e) {
                    throw new RuntimeException(e);
                }
            } else {
                if (!Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
                    return forFieldSuper(clazz, field);
                }
            }
        } else {
            if (!Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
                return forFieldSuper(clazz, field);
            }
        }

        return null;
    }

    private static CtField findMatchingField(CtClass clazz, CtField field) {
        String fieldName = field.getName();
        try {
            CtClass type = field.getType();
            for (CtField f : clazz.getDeclaredFields()) {
                if (fieldName.equals(f.getName()) && type.subtypeOf(f.getType())) {
                    return f;
                }
            }
        } catch (NotFoundException e) {
            // throw new RuntimeException(e);
        }

        return null;
    }

    private static StageAnnotation forFieldSuper(CtClass clazz, CtField field) {
        CtClass s;
        try {
            s = clazz.getSuperclass();
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
        if (s != null) {
            CtField f = findMatchingField(s, field);

            if (f == null) {
                return forFieldSuper(s, field);
            } else {
                StageAnnotation ann = forFieldInternal(s, f);
                if (ann != null) {
                    fieldCache.put(f, ann);
                }

                return ann;
            }
        }

        return null;
    }
}
