package tamestaj;

import tamestaj.annotations.Configure;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.annotation.*;

import java.util.IdentityHashMap;

final class ConfigureAnnotation {
    private static final IdentityHashMap<CtClass, ConfigureAnnotation> classCache = new IdentityHashMap<>();

    private final CtClass language;
    private final boolean hasRestrictedAcceptAccessibility;
    private final boolean hasRestrictedStageAccessibility;
    private final boolean hasRestrictedSuppressAccessibility;

    private ConfigureAnnotation(CtClass language, boolean hasRestrictedAcceptAccessibility, boolean hasRestrictedStageAccessibility, boolean hasRestrictedSuppressAccessibility) {
        this.language = language;
        this.hasRestrictedAcceptAccessibility = hasRestrictedAcceptAccessibility;
        this.hasRestrictedStageAccessibility = hasRestrictedStageAccessibility;
        this.hasRestrictedSuppressAccessibility = hasRestrictedSuppressAccessibility;
    }

    boolean isAcceptAccessibleFrom(CtClass clazz) {
        if (clazz.getPackageName().equals(language.getPackageName())) {
            return true;
        }

        return !hasRestrictedAcceptAccessibility;
    }

    boolean isStageAccessibleFrom(CtClass clazz) {
        if (clazz.getPackageName().equals(language.getPackageName())) {
            return true;
        }

        return !hasRestrictedStageAccessibility;
    }

    boolean isSuppressAccessibleFrom(CtClass clazz) {
        if (clazz.getPackageName().equals(language.getPackageName())) {
            return true;
        }

        return !hasRestrictedSuppressAccessibility;
    }

    static ConfigureAnnotation forClass(CtClass clazz) {
        ConfigureAnnotation cached = classCache.get(clazz);
        if (cached != null) {
            return cached;
        }

        try {
            if (!clazz.subtypeOf(Util.LANGUAGE_CLASS)) {
                throw new IllegalArgumentException("Only languages can have configure annotations!");
            }
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }

        // Defaults:
        boolean hasRestrictedAcceptAccessibility = false;
        boolean hasRestrictedStageAccessibility = false;
        boolean hasRestrictedSuppressAccessibility = false;

        AnnotationsAttribute attr = (AnnotationsAttribute) clazz.getClassFile2().getAttribute(AnnotationsAttribute.visibleTag);
        if (attr != null) {
            Annotation ann = attr.getAnnotation(Configure.class.getName());
            if (ann != null) {
                MemberValue hasRestrictedAcceptAccessibilityValue = ann.getMemberValue("hasRestrictedAcceptAccessibility");
                if (hasRestrictedAcceptAccessibilityValue != null) {
                    hasRestrictedAcceptAccessibility = ((BooleanMemberValue) hasRestrictedAcceptAccessibilityValue).getValue();
                }

                MemberValue hasRestrictedStageAccessibilityValue = ann.getMemberValue("hasRestrictedStageAccessibility");
                if (hasRestrictedStageAccessibilityValue != null) {
                    hasRestrictedStageAccessibility = ((BooleanMemberValue) hasRestrictedStageAccessibilityValue).getValue();
                }

                MemberValue hasRestrictedSuppressAccessibilityValue = ann.getMemberValue("hasRestrictedSuppressAccessibility");
                if (hasRestrictedSuppressAccessibilityValue != null) {
                    hasRestrictedSuppressAccessibility = ((BooleanMemberValue) hasRestrictedSuppressAccessibilityValue).getValue();
                }
            }
        }

        cached = new ConfigureAnnotation(clazz, hasRestrictedAcceptAccessibility, hasRestrictedStageAccessibility, hasRestrictedSuppressAccessibility);

        classCache.put(clazz, cached);

        return cached;
    }
}
