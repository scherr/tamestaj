package tamestaj;

import com.google.common.collect.ImmutableSet;
import tamestaj.annotations.Suppress;
import javassist.*;
import javassist.bytecode.*;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.MemberValue;

import java.util.IdentityHashMap;

final class SuppressAnnotation {
    private static final IdentityHashMap<CtClass, SuppressAnnotation> classCache = new IdentityHashMap<>();
    private static final IdentityHashMap<CtBehavior, SuppressAnnotation> behaviorCache = new IdentityHashMap<>();

    private final ImmutableSet<CtClass> languages;

    private SuppressAnnotation(ImmutableSet<CtClass> languages) {
        this.languages = languages;
    }

    ImmutableSet<CtClass> getLanguages() {
        return languages;
    }

    boolean isSurpressed(CtClass language) {
        return languages.contains(language);
    }

    /*
    private static CtBehavior getEnclosingBehavior(CtClass clazz) throws NotFoundException {
        clazz.getEnclosingBehavior()

        ClassFile cf = clazz.getClassFile2();
        EnclosingMethodAttribute ema = (EnclosingMethodAttribute)cf.getAttribute(EnclosingMethodAttribute.tag);
        if (ema != null) {
            CtClass enc = clazz.getClassPool().get(ema.className());
            try {
                String name = ema.methodName();
                switch (name) {
                    case MethodInfo.nameInit:
                        return enc.getConstructor(ema.methodDescriptor());
                    case MethodInfo.nameClinit:
                        return enc.getClassInitializer();
                    default:
                        return enc.getMethod(name, ema.methodDescriptor());
                }
            } catch (NullPointerException e) {
                // There is no other way to detect the existence of a malformed enclosing method attribute, e.g. one without proper indices.
            }
        }

        return null;
    }
    */

    static SuppressAnnotation forClass(CtClass clazz) {
        SuppressAnnotation cached = classCache.get(clazz);
        if (classCache.containsKey(clazz)) {
            return cached;
        }

        ImmutableSet.Builder<CtClass> languagesBuilder = ImmutableSet.builder();

        CtClass enclosingClass = null;
        try {
            enclosingClass = clazz.getDeclaringClass();
        } catch (NotFoundException e) {
        }
        if (enclosingClass != null) {
            SuppressAnnotation enclosingAnn = forClass(enclosingClass);
            if (enclosingAnn != null) {
                languagesBuilder.addAll(enclosingAnn.languages);
            }
        }

        CtBehavior enclosingBehavior = null;
        try {
            enclosingBehavior = clazz.getEnclosingBehavior();
        } catch (NotFoundException e) {
        }
        if (enclosingBehavior != null) {
            SuppressAnnotation enclosingAnn = forBehavior(enclosingBehavior);
            if (enclosingAnn != null) {
                languagesBuilder.addAll(enclosingAnn.languages);
            }
        }

        AnnotationsAttribute attr = (AnnotationsAttribute) clazz.getClassFile2().getAttribute(AnnotationsAttribute.visibleTag);
        if (attr != null) {
            Annotation ann = attr.getAnnotation(Suppress.class.getName());
            if (ann != null) {
                MemberValue[] memberValues = ((ArrayMemberValue) ann.getMemberValue("languages")).getValue();

                for (MemberValue memberValue : memberValues) {
                    String languageClassName = ((ClassMemberValue) memberValue).getValue();

                    try {
                        CtClass l = clazz.getClassPool().get(languageClassName);
                        if (ConfigureAnnotation.forClass(l).isSuppressAccessibleFrom(clazz)) {
                            languagesBuilder.add(l);
                        }
                    } catch (NotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        ImmutableSet<CtClass> languages = languagesBuilder.build();
        if (!languages.isEmpty()) {
            cached = new SuppressAnnotation(languages);
        }

        classCache.put(clazz, cached);

        return cached;
    }

    static SuppressAnnotation forBehavior(CtBehavior behavior) {
        SuppressAnnotation cached = behaviorCache.get(behavior);
        if (behaviorCache.containsKey(behavior)) {
            return cached;
        }

        ImmutableSet.Builder<CtClass> languagesBuilder = ImmutableSet.builder();

        SuppressAnnotation classSuppressAn = forClass(behavior.getDeclaringClass());
        if (classSuppressAn != null) {
            languagesBuilder.addAll(classSuppressAn.languages);
        }

        AnnotationsAttribute attr = (AnnotationsAttribute) behavior.getMethodInfo2().getAttribute(AnnotationsAttribute.visibleTag);
        if (attr != null) {
            Annotation ann = attr.getAnnotation(Suppress.class.getName());
            if (ann != null) {
                MemberValue[] memberValues = ((ArrayMemberValue) ann.getMemberValue("languages")).getValue();

                for (MemberValue memberValue : memberValues) {
                    String languageClassName = ((ClassMemberValue) memberValue).getValue();

                    try {
                        CtClass l = behavior.getDeclaringClass().getClassPool().get(languageClassName);
                        if (ConfigureAnnotation.forClass(l).isSuppressAccessibleFrom(behavior.getDeclaringClass())) {
                            languagesBuilder.add(l);
                        }
                    } catch (NotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        ImmutableSet<CtClass> languages = languagesBuilder.build();
        if (!languages.isEmpty()) {
            cached = new SuppressAnnotation(languages);
        }

        behaviorCache.put(behavior, cached);

        return cached;
    }
}
