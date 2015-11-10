package tamestaj;

import javassist.*;
import javassist.bytecode.*;
import javassist.bytecode.annotation.*;
import tamestaj.annotations.Accept;
import tamestaj.annotations.Configure;
import tamestaj.annotations.Stage;
import tamestaj.annotations.Suppress;
import tamestaj.util.TickTock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

final class Agent {
    private Agent() { }

    public static void premain(String agentArgs, Instrumentation inst) {
        /*
         * Debug dump paths...
         * TODO: Make option in agentArgs!
        if (System.getProperty("os.name").toLowerCase().contains("nux")) {
            CtClass.debugDump = ;
        } else {
            CtClass.debugDump = ;
        }
        */

        // Making the various hidden classes accessible here has caused a weird JVM bug once (critical crash)!
        // We moved it inside Transformer.

        inst.addTransformer(new Transformer());
    }

    private static final class Transformer implements ClassFileTransformer {
        private static final String[] ignore = new String[]{ "sun/", "java/", "javax/" };

        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            if (className == null) { return null; }

            for (int i = 0; i < ignore.length; i++) {
                if (className.startsWith(ignore[i])) {
                    // "If the implementing method determines that no transformations are needed,
                    // it should return null. Otherwise, it should create a new byte[] array,
                    // copy the input classfileBuffer into it, along with all desired transformations,
                    // and return the new array. The input classfileBuffer must not be modified."
                    return null;
                }
            }

            ClassPool cp = ClassPool.getDefault();
            try {
                // Here we make the ClosureHolder class accessible!
                if (!Modifier.isPublic(Util.CLOSURE_HOLDER_CLASS.getModifiers())) {
                    Util.CLOSURE_HOLDER_CLASS.setModifiers(Modifier.setPublic(Util.CLOSURE_HOLDER_CLASS.getModifiers()));
                    Util.CLOSURE_HOLDER_CLASS.toClass();
                }

                // Here we make the TraceCache class accessible!
                if (!Modifier.isPublic(Util.TRACE_CACHE_CLASS.getModifiers())) {
                    Util.TRACE_CACHE_CLASS.setModifiers(Modifier.setPublic(Util.TRACE_CACHE_CLASS.getModifiers()));
                    Util.TRACE_CACHE_CLASS.toClass();
                }

                // Here we make the Trace class accessible!
                if (!Modifier.isPublic(Util.TRACE_CLASS.getModifiers())) {
                    Util.TRACE_CLASS.setModifiers(Modifier.setPublic(Util.TRACE_CLASS.getModifiers()));
                    Util.TRACE_CLASS.toClass();
                }

                // Here we make the Dispatcher class accessible!
                if (!Modifier.isPublic(Util.DISPATCHER_CLASS.getModifiers())) {
                    Util.DISPATCHER_CLASS.setModifiers(Modifier.setPublic(Util.DISPATCHER_CLASS.getModifiers()));
                    Util.DISPATCHER_CLASS.toClass();
                }

                CtClass clazz = cp.getOrNull(Descriptor.toJavaName(className));
                if (clazz == null) {
                    InputStream stream = new ByteArrayInputStream(classfileBuffer);
                    clazz = cp.makeClass(stream);
                    stream.close();
                }

                // We are just loading the class so defrosting is allowed!
                clazz.defrost();
                // This is necessary because in client code Javassist might have been used to
                // modify and load the current class using toClass(). Of course Javassist sets it frozen
                // but in fact since we are intercepting this very loading we can still modify it and safely defrost!

                checkAnnotations(clazz);

                if (Util.isCarrier(clazz)) {
                    CarrierTransformer.transformCarrierChecked(clazz);
                }

                // Here we make the language classes accessible!
                if (clazz.subtypeOf(Util.LANGUAGE_CLASS)) {
                    if (!Modifier.isPublic(clazz.getModifiers())) {
                        clazz.setModifiers(Modifier.setPublic(clazz.getModifiers()));
                    }
                }

                if (containsStaged(cp, clazz)) {
                    // System.out.println("Class \"" + clazz.getName() + "\" contains staged references!");
                    for (CtBehavior behavior : clazz.getDeclaredBehaviors()) {
                        // Bridge method check via isVolatile... alternatively: (behavior.getMethodInfo2().getAccessFlags() & AccessFlag.BRIDGE) == 0
                        if (!Modifier.isAbstract(behavior.getModifiers()) && !Modifier.isNative(behavior.getModifiers()) && !Modifier.isVolatile(behavior.getModifiers()) && containsStaged(cp, behavior)) {
                            System.out.println("Behavior \"" + behavior.getLongName() + "\" contains (unsuppressed) staging code!");

                            TickTock.tick("Transformation");

                                TickTock.tick("Type analysis");

                                    TypeAnalyzer typeAnalyzer = new TypeAnalyzer(behavior);
                                    typeAnalyzer.analyze();

                                TickTock.tockPrint();


                                TickTock.tick("Value flow analysis");

                                    ValueFlowAnalyzer valueFlowAnalyzer = new ValueFlowAnalyzer(typeAnalyzer);
                                    valueFlowAnalyzer.analyze();

                                TickTock.tockPrint();


                                TickTock.tick("Constant analysis");

                                    ConstantAnalyzer constantAnalyzer = new ConstantAnalyzer(typeAnalyzer);
                                    constantAnalyzer.analyze();

                                TickTock.tockPrint();


                                TickTock.tick("Stage analysis");

                                    StageAnalyzer stageAnalyzer = new StageAnalyzer(typeAnalyzer, valueFlowAnalyzer.getResult());
                                    stageAnalyzer.analyze();
                                    StageGraph stageGraph = stageAnalyzer.getResult().getStageGraph();

                                TickTock.tockPrint();

                                TickTock.tick("Lift estimate analysis");

                                    LiftEstimateAnalyzer.Result liftEstimateAnalyzerResult = LiftEstimateAnalyzer.analyze(stageGraph, valueFlowAnalyzer.getResult(), constantAnalyzer.getResult());

                                TickTock.tockPrint();

                                TickTock.tick("Cachability analysis");

                                    CachabilityAnalyzer.Result cachabilityAnalyzerResult = CachabilityAnalyzer.analyze(stageGraph, valueFlowAnalyzer.getResult(), constantAnalyzer.getResult());

                                TickTock.tockPrint();

                                TickTock.tick("Weave analysis");

                                    WeaveAnalyzer weaveAnalyzer = new WeaveAnalyzer(typeAnalyzer, stageGraph, valueFlowAnalyzer.getResult(), constantAnalyzer.getResult(), cachabilityAnalyzerResult, liftEstimateAnalyzerResult);
                                    weaveAnalyzer.analyze();

                                TickTock.tockPrint();


                                TickTock.tick("Weaving");

                                    weaveAnalyzer.getResult().weave();

                                TickTock.tockPrint();

                            TickTock.tockPrint();
                        }
                    }
                }

                if (clazz.isModified()) {
                    return clazz.toBytecode();
                }
            } catch (IOException | NotFoundException | BadBytecode | CannotCompileException | RuntimeException | Error e) {
                e.printStackTrace();
                // throw new RuntimeException(e);
            }

            return null;
        }
    }

    private static boolean containsStaged(ClassPool classPool, CtClass clazz) {
        SuppressAnnotation suppressAnn = SuppressAnnotation.forClass(clazz);

        ConstPool constPool = clazz.getClassFile().getConstPool();
        int size = constPool.getSize();
        for (int i = 1; i < size; i++) {
            StageAnnotation ann = null;

            try {
                switch (constPool.getTag(i)) {
                    case ConstPool.CONST_Methodref: {
                        String className = constPool.getMethodrefClassName(i);

                        CtClass c;
                        if (className.charAt(0) == '[') {
                            c = Descriptor.toCtClass(className, classPool);
                        } else {
                            c = classPool.get(className);
                        }

                        if (constPool.isConstructor(className, i) <= 0) {
                            String name = constPool.getMethodrefName(i);
                            String desc = constPool.getMethodrefType(i);
                            CtMethod method = c.getMethod(name, desc);

                            ann = StageAnnotation.forMethod(c, method);
                        }
                        break;
                    }
                    case ConstPool.CONST_InterfaceMethodref: {
                        String className = constPool.getInterfaceMethodrefClassName(i);

                        CtClass c;
                        if (className.charAt(0) == '[') {
                            c = Descriptor.toCtClass(className, classPool);
                        } else {
                            c = classPool.get(className);
                        }

                        String name = constPool.getInterfaceMethodrefName(i);
                        String desc = constPool.getInterfaceMethodrefType(i);
                        CtMethod method = c.getMethod(name, desc);

                        ann = StageAnnotation.forMethod(c, method);
                        break;
                    }
                    case ConstPool.CONST_Fieldref: {
                        String className = constPool.getFieldrefClassName(i);

                        CtClass c;
                        if (className.charAt(0) == '[') {
                            c = Descriptor.toCtClass(className, classPool);
                        } else {
                            c = classPool.get(className);
                        }

                        String name = constPool.getFieldrefName(i);
                        String desc = constPool.getFieldrefType(i);
                        CtField field = c.getField(name, desc);

                        ann = StageAnnotation.forField(c, field);
                        break;
                    }
                }
            } catch (NotFoundException e) {
                // e.printStackTrace();
            }

            if (ann != null) {
                if (suppressAnn == null || !suppressAnn.isSurpressed(ann.getLanguage())) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void checkAnnotations(CtClass clazz) {
        AnnotationsAttribute classAttr = (AnnotationsAttribute) clazz.getClassFile2().getAttribute(AnnotationsAttribute.visibleTag);
        if (classAttr != null) {
            Annotation suppressAnn = classAttr.getAnnotation(Suppress.class.getName());
            if (suppressAnn != null) {
                MemberValue[] memberValues = ((ArrayMemberValue) suppressAnn.getMemberValue("languages")).getValue();

                for (MemberValue memberValue : memberValues) {
                    String languageClassName = ((ClassMemberValue) memberValue).getValue();

                    try {
                        CtClass l = clazz.getClassPool().get(languageClassName);
                        if (!ConfigureAnnotation.forClass(l).isSuppressAccessibleFrom(clazz)) {
                            System.out.println("Language \"" + languageClassName + "\" is not suppress-accessible from class \"" + clazz.getName() + "\".");
                        }
                    } catch (NotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            Annotation configureAnn = classAttr.getAnnotation(Configure.class.getName());
            if (configureAnn != null) {
                try {
                    if (clazz.isInterface()) {
                        System.out.println("The Configure annotation is ineffective on interfaces, here \"" + clazz.getName() + "\".");
                    } else {
                        if (!clazz.subtypeOf(Util.LANGUAGE_CLASS)) {
                            System.out.println("The Configure annotation is ineffective on non-language classes, here \"" + clazz.getName() + "\".");
                        }
                    }
                } catch (NotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        for (CtBehavior behavior : clazz.getDeclaredBehaviors()) {
            AnnotationsAttribute attr = (AnnotationsAttribute) behavior.getMethodInfo2().getAttribute(AnnotationsAttribute.visibleTag);
            if (attr != null) {
                Annotation ann = attr.getAnnotation(Suppress.class.getName());
                if (ann != null) {
                    MemberValue[] memberValues = ((ArrayMemberValue) ann.getMemberValue("languages")).getValue();

                    for (MemberValue memberValue : memberValues) {
                        String languageClassName = ((ClassMemberValue) memberValue).getValue();

                        try {
                            CtClass l = behavior.getDeclaringClass().getClassPool().get(languageClassName);
                            if (!ConfigureAnnotation.forClass(l).isSuppressAccessibleFrom(behavior.getDeclaringClass())) {
                                System.out.println("Language \"" + languageClassName + "\" is not suppress-accessible from behavior \"" + behavior.getLongName() + "\".");
                            }
                        } catch (NotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

            if (behavior instanceof CtMethod) {
                CtMethod method = (CtMethod) behavior;
                Annotation stageAnn = null;
                if (attr != null) {
                    stageAnn = attr.getAnnotation(Stage.class.getName());
                    if (stageAnn != null) {
                        if (!Modifier.isStatic(method.getModifiers()) && clazz.isInterface()) {
                            System.out.println("The Stage annotation is ineffective on interface methods, here \"" + method.getLongName() + "\".");
                        } else {
                            String languageClassName = ((ClassMemberValue) stageAnn.getMemberValue("language")).getValue();
                            MemberValue isStrictValue = stageAnn.getMemberValue("isStrict");
                            try {
                                CtClass language = clazz.getClassPool().get(languageClassName);
                                if (!ConfigureAnnotation.forClass(language).isStageAccessibleFrom(clazz)) {
                                    System.out.println("Language \"" + languageClassName + "\" is not stage-accessible from method \"" + method.getLongName() + "\".");
                                } else {
                                    boolean isStrict;

                                    if (isStrictValue == null) {
                                        isStrict = false;
                                    } else {
                                        isStrict = ((BooleanMemberValue) isStrictValue).getValue();
                                    }

                                    if (method.getReturnType().equals(CtClass.voidType)) {
                                        if (!isStrict) {
                                            System.out.println("Strictness setting for method \"" + method.getLongName() + "\" overridden (true) due to having void return type.");
                                        }
                                    }
                                }
                            } catch (NotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }

                    Annotation acceptThisAnn = attr.getAnnotation(Accept.This.class.getName());
                    if (acceptThisAnn != null) {
                        if (stageAnn == null) {
                            System.out.println("The Accept.This annotation is ineffective without Stage annotation, here at \"" + method.getLongName() + "\".");
                        } else {
                            if (Modifier.isStatic(method.getModifiers())) {
                                System.out.println("The Accept.This annotation is ineffective on static methods, here \"" + method.getLongName() + "\".");
                            } else {
                                MemberValue[] memberValues = ((ArrayMemberValue) acceptThisAnn.getMemberValue("languages")).getValue();

                                for (int j = 0; j < memberValues.length; j++) {
                                    String languageClassName = ((ClassMemberValue) memberValues[j]).getValue();

                                    try {
                                        CtClass l = method.getDeclaringClass().getClassPool().get(languageClassName);

                                        if (!ConfigureAnnotation.forClass(l).isAcceptAccessibleFrom(method.getDeclaringClass())) {
                                            System.out.println("Language \"" + languageClassName + "\" is not accept-accessible from method \"" + method.getLongName() + "\".");
                                        }
                                    } catch (NotFoundException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        }
                    }
                }

                ParameterAnnotationsAttribute paramAttr = (ParameterAnnotationsAttribute) method.getMethodInfo2().getAttribute(ParameterAnnotationsAttribute.visibleTag);
                if (paramAttr != null) {
                    String name = Accept.class.getName();

                    Annotation[][] annotations = paramAttr.getAnnotations();
                    for (Annotation[] annotation : annotations) {
                        Annotation ann = null;
                        for (int j = 0; j < annotation.length; j++) {
                            if (annotation[j].getTypeName().equals(name)) {
                                ann = annotation[j];
                            }
                        }

                        if (ann == null) {
                            continue;
                        }

                        if (stageAnn == null) {
                            System.out.println("The Accept annotation is ineffective without Stage annotation, here at \"" + method.getLongName() + "\".");
                            continue;
                        }

                        MemberValue[] memberValues = ((ArrayMemberValue) ann.getMemberValue("languages")).getValue();

                        for (MemberValue memberValue : memberValues) {
                            String languageClassName = ((ClassMemberValue) memberValue).getValue();

                            try {
                                CtClass l = method.getDeclaringClass().getClassPool().get(languageClassName);

                                if (!ConfigureAnnotation.forClass(l).isAcceptAccessibleFrom(method.getDeclaringClass())) {
                                    System.out.println("Language \"" + languageClassName + "\" is not accept-accessible from method \"" + method.getLongName() + "\".");
                                }
                            } catch (NotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }

        for (CtField field : clazz.getDeclaredFields()) {
            AnnotationsAttribute attr = (AnnotationsAttribute) field.getFieldInfo2().getAttribute(AnnotationsAttribute.visibleTag);
            if (attr != null) {
                Annotation stageAnn = attr.getAnnotation(Stage.class.getName());
                if (stageAnn != null) {
                    String languageClassName = ((ClassMemberValue) stageAnn.getMemberValue("language")).getValue();
                    try {
                        CtClass language = clazz.getClassPool().get(languageClassName);
                        if (!ConfigureAnnotation.forClass(language).isStageAccessibleFrom(clazz)) {
                            System.out.println("Language \"" + languageClassName + "\" is not stage-accessible from field \"" + clazz.getName() + "." + field.getName() + "\".");
                        }
                    } catch (NotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }

                Annotation acceptThisAnn = attr.getAnnotation(Accept.This.class.getName());
                if (acceptThisAnn != null) {
                    if (stageAnn == null) {
                        System.out.println("The Accept.This annotation is ineffective without Stage annotation, here at \"" + field.getDeclaringClass().getName() + "." + field.getName() + "\".");
                    } else {
                        if (Modifier.isStatic(field.getModifiers())) {
                            System.out.println("The Accept.This annotation is ineffective on static fields, here \"" + field.getDeclaringClass().getName() + "." + field.getName() + "\".");
                        } else {
                            MemberValue[] memberValues = ((ArrayMemberValue) acceptThisAnn.getMemberValue("languages")).getValue();

                            for (MemberValue memberValue : memberValues) {
                                String languageClassName = ((ClassMemberValue) memberValue).getValue();

                                try {
                                    CtClass l = field.getDeclaringClass().getClassPool().get(languageClassName);

                                    if (!ConfigureAnnotation.forClass(l).isAcceptAccessibleFrom(field.getDeclaringClass())) {
                                        System.out.println("Language \"" + languageClassName + "\" is not accept-accessible from field \"" + field.getDeclaringClass().getName() + "." + field.getName() + "\".");
                                    }
                                } catch (NotFoundException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                }

                Annotation acceptAnn = attr.getAnnotation(Accept.class.getName());
                if (acceptAnn != null) {
                    if (stageAnn == null) {
                        System.out.println("The Accept annotation is ineffective without Stage annotation, here at \"" + field.getDeclaringClass().getName() + "." + field.getName() + "\".");
                    } else {
                        MemberValue[] memberValues = ((ArrayMemberValue) acceptAnn.getMemberValue("languages")).getValue();

                        for (MemberValue memberValue : memberValues) {
                            String languageClassName = ((ClassMemberValue) memberValue).getValue();

                            try {
                                CtClass l = field.getDeclaringClass().getClassPool().get(languageClassName);

                                if (!ConfigureAnnotation.forClass(l).isAcceptAccessibleFrom(field.getDeclaringClass())) {
                                    System.out.println("Language \"" + languageClassName + "\" is not accept-accessible from field \"" + field.getDeclaringClass().getName() + "." + field.getName() + "\".");
                                }
                            } catch (NotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean containsStaged(ClassPool classPool, CtBehavior behavior) throws BadBytecode {
        SuppressAnnotation suppressAnn = SuppressAnnotation.forBehavior(behavior);

        ConstPool constPool = behavior.getMethodInfo().getConstPool();
        CodeIterator codeIt = behavior.getMethodInfo().getCodeAttribute().iterator();
        while (codeIt.hasNext()) {
            int pos = codeIt.next();

            try {
                switch (codeIt.byteAt(pos)) {
                    case Opcode.GETSTATIC:
                    case Opcode.PUTSTATIC:
                    case Opcode.GETFIELD:
                    case Opcode.PUTFIELD: {
                        int index = codeIt.u16bitAt(pos + 1);

                        String className = constPool.getFieldrefClassName(index);

                        CtClass c;
                        if (className.charAt(0) == '[') {
                            c = Descriptor.toCtClass(className, classPool);
                        } else {
                            c = classPool.get(className);
                        }

                        String name = constPool.getFieldrefName(index);
                        String desc = constPool.getFieldrefType(index);
                        CtField field = c.getField(name, desc);

                        StageAnnotation ann = StageAnnotation.forField(c, field);

                        if (ann != null) {
                            if (suppressAnn == null || !suppressAnn.isSurpressed(ann.getLanguage())) {
                                return true;
                            }
                        }

                        break;
                    }
                    case Opcode.INVOKEVIRTUAL:
                    case Opcode.INVOKESPECIAL:
                    case Opcode.INVOKESTATIC: {
                        int index = codeIt.u16bitAt(pos + 1);

                        String className = constPool.getMethodrefClassName(index);

                        CtClass c;
                        if (className.charAt(0) == '[') {
                            c = Descriptor.toCtClass(className, classPool);
                        } else {
                            c = classPool.get(className);
                        }

                        if (constPool.isConstructor(className, index) <= 0) {
                            String name = constPool.getMethodrefName(index);
                            String desc = constPool.getMethodrefType(index);
                            CtMethod method = c.getMethod(name, desc);

                            StageAnnotation ann = StageAnnotation.forMethod(c, method);

                            if (ann != null) {
                                if (suppressAnn == null || !suppressAnn.isSurpressed(ann.getLanguage())) {
                                    return true;
                                }
                            }
                        }

                        break;
                    }
                    case Opcode.INVOKEINTERFACE: {
                        int index = codeIt.u16bitAt(pos + 1);

                        String className = constPool.getInterfaceMethodrefClassName(index);

                        CtClass c;
                        if (className.charAt(0) == '[') {
                            c = Descriptor.toCtClass(className, classPool);
                        } else {
                            c = classPool.get(className);
                        }

                        String name = constPool.getInterfaceMethodrefName(index);
                        String desc = constPool.getInterfaceMethodrefType(index);
                        CtMethod method = c.getMethod(name, desc);

                        StageAnnotation ann = StageAnnotation.forMethod(c, method);

                        if (ann != null) {
                            if (suppressAnn == null || !suppressAnn.isSurpressed(ann.getLanguage())) {
                                return true;
                            }
                        }

                        break;
                    }
                }
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
        }

        return false;
    }
}
