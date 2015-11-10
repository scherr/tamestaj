package tamestaj;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import tamestaj.annotations.Accept;
import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.annotation.*;
import tamestaj.annotations.Stage;

import java.util.IdentityHashMap;

final class AcceptAnnotation {
    private static final IdentityHashMap<CtField, ImmutableList<AcceptAnnotation>> fieldCache = new IdentityHashMap<>();
    private static final IdentityHashMap<CtMethod, ImmutableList<AcceptAnnotation>> methodCache = new IdentityHashMap<>();

    private final ImmutableSet<CtClass> languages;

    private AcceptAnnotation(ImmutableSet<CtClass> languages) {
        this.languages = languages;
    }

    ImmutableSet<CtClass> getLanguages() {
        return languages;
    }

    static ImmutableList<AcceptAnnotation> forMethod(CtMethod method) {
        StageAnnotation stageAnnotation = StageAnnotation.forMethod(method);
        if (stageAnnotation == null) {
            throw new IllegalArgumentException("Only staged methods can have access annotations!");
        }

        CtMethod annotatedMethod = (CtMethod) stageAnnotation.getAnnotatedMember();

        ImmutableList<AcceptAnnotation> anns = methodCache.get(annotatedMethod);
        if (anns != null) {
            return anns;
        }

        ImmutableList.Builder<AcceptAnnotation> annsBuilder = ImmutableList.builder();

        if (!Modifier.isStatic(annotatedMethod.getModifiers())) {
            AnnotationsAttribute attr = (AnnotationsAttribute) annotatedMethod.getMethodInfo2().getAttribute(AnnotationsAttribute.visibleTag);
            if (attr != null) {
                Annotation ann = attr.getAnnotation(Accept.This.class.getName());
                if (ann != null) {
                    MemberValue[] memberValues = ((ArrayMemberValue) ann.getMemberValue("languages")).getValue();
                    ImmutableSet.Builder<CtClass> languagesBuilder = ImmutableSet.builder();

                    for (MemberValue memberValue : memberValues) {
                        String languageClassName = ((ClassMemberValue) memberValue).getValue();

                        try {
                            CtClass l = annotatedMethod.getDeclaringClass().getClassPool().get(languageClassName);

                            if (ConfigureAnnotation.forClass(l).isAcceptAccessibleFrom(annotatedMethod.getDeclaringClass())) {
                                languagesBuilder.add(l);
                            }
                        } catch (NotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    annsBuilder.add(new AcceptAnnotation(languagesBuilder.build()));
                } else {
                    annsBuilder.add(new AcceptAnnotation(ImmutableSet.of(stageAnnotation.getLanguage())));
                }
            } else {
                annsBuilder.add(new AcceptAnnotation(ImmutableSet.of(stageAnnotation.getLanguage())));
            }
        }

        ParameterAnnotationsAttribute attr = (ParameterAnnotationsAttribute) annotatedMethod.getMethodInfo2().getAttribute(ParameterAnnotationsAttribute.visibleTag);
        if (attr != null) {
            String name = Accept.class.getName();

            Annotation[][] annotations = attr.getAnnotations();
            for (int i = 0; i < annotations.length; i++) {
                Annotation ann = null;
                for (int j = 0; j < annotations[i].length; j++) {
                    if (annotations[i][j].getTypeName().equals(name)) {
                        ann = annotations[i][j];
                    }
                }

                if (ann == null) {
                    annsBuilder.add(new AcceptAnnotation(ImmutableSet.of(stageAnnotation.getLanguage())));
                    continue;
                }

                MemberValue[] memberValues = ((ArrayMemberValue) ann.getMemberValue("languages")).getValue();
                ImmutableSet.Builder<CtClass> languagesBuilder = ImmutableSet.builder();

                for (MemberValue memberValue : memberValues) {
                    String languageClassName = ((ClassMemberValue) memberValue).getValue();

                    try {
                        CtClass l = annotatedMethod.getDeclaringClass().getClassPool().get(languageClassName);

                        if (ConfigureAnnotation.forClass(l).isAcceptAccessibleFrom(annotatedMethod.getDeclaringClass())) {
                            languagesBuilder.add(l);
                        }
                    } catch (NotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }

                annsBuilder.add(new AcceptAnnotation(languagesBuilder.build()));
            }
        } else {
            try {
                for (int i = 0; i < annotatedMethod.getParameterTypes().length; i++) {
                    annsBuilder.add(new AcceptAnnotation(ImmutableSet.of(stageAnnotation.getLanguage())));
                }
            } catch (NotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        anns = annsBuilder.build();
        methodCache.put(annotatedMethod, anns);

        return anns;
    }

    public static ImmutableList<AcceptAnnotation> forField(CtField field) {
        StageAnnotation stageAnnotation = StageAnnotation.forField(field);
        if (stageAnnotation == null) {
            throw new IllegalArgumentException("Only staged fields can have access annotations!");
        }

        CtField annotatedField = (CtField) stageAnnotation.getAnnotatedMember();

        ImmutableList<AcceptAnnotation> anns = fieldCache.get(annotatedField);
        if (anns != null) {
            return anns;
        }

        ImmutableList.Builder<AcceptAnnotation> annsBuilder = ImmutableList.builder();

        if (!Modifier.isStatic(annotatedField.getModifiers())) {
            AnnotationsAttribute attr = (AnnotationsAttribute) annotatedField.getFieldInfo2().getAttribute(AnnotationsAttribute.visibleTag);
            if (attr != null) {
                Annotation ann = attr.getAnnotation(Accept.This.class.getName());
                if (ann == null) {
                    annsBuilder.add(new AcceptAnnotation(ImmutableSet.of(stageAnnotation.getLanguage())));
                } else {
                    MemberValue[] memberValues = ((ArrayMemberValue) ann.getMemberValue("languages")).getValue();
                    ImmutableSet.Builder<CtClass> languagesBuilder = ImmutableSet.builder();

                    for (MemberValue memberValue : memberValues) {
                        String languageClassName = ((ClassMemberValue) memberValue).getValue();

                        try {
                            CtClass l = annotatedField.getDeclaringClass().getClassPool().get(languageClassName);
                            if (ConfigureAnnotation.forClass(l).isAcceptAccessibleFrom(annotatedField.getDeclaringClass())) {
                                languagesBuilder.add(l);
                            }
                        } catch (NotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    annsBuilder.add(new AcceptAnnotation(languagesBuilder.build()));
                }
            } else {
                annsBuilder.add(new AcceptAnnotation(ImmutableSet.of(stageAnnotation.getLanguage())));
            }
        }

        AnnotationsAttribute attr = (AnnotationsAttribute) annotatedField.getFieldInfo2().getAttribute(AnnotationsAttribute.visibleTag);
        if (attr != null) {
            Annotation ann = attr.getAnnotation(Accept.class.getName());
            if (ann == null) {
                annsBuilder.add(new AcceptAnnotation(ImmutableSet.of(stageAnnotation.getLanguage())));
            } else {
                MemberValue[] memberValues = ((ArrayMemberValue) ann.getMemberValue("languages")).getValue();
                ImmutableSet.Builder<CtClass> languagesBuilder = ImmutableSet.builder();

                for (MemberValue memberValue : memberValues) {
                    String languageClassName = ((ClassMemberValue) memberValue).getValue();

                    try {
                        CtClass l = annotatedField.getDeclaringClass().getClassPool().get(languageClassName);
                        if (ConfigureAnnotation.forClass(l).isAcceptAccessibleFrom(annotatedField.getDeclaringClass())) {
                            languagesBuilder.add(l);
                        }
                    } catch (NotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }

                annsBuilder.add(new AcceptAnnotation(languagesBuilder.build()));
            }
        } else {
            annsBuilder.add(new AcceptAnnotation(ImmutableSet.of(stageAnnotation.getLanguage())));
        }

        anns = annsBuilder.build();
        fieldCache.put(annotatedField, anns);

        return anns;
    }
}
