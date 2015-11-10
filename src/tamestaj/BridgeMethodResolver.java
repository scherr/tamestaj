package tamestaj;

import javassist.*;
import javassist.CtMethod;
import javassist.bytecode.*;

// A crude resolver for bridge methods using a quick bytecode scan

@SuppressWarnings("unused")
final class BridgeMethodResolver {
    private BridgeMethodResolver() { }

    static CtMethod getBridgedMethod(CtMethod method) {
        // Bridge method check via isVolatile... alternatively: (method.getMethodInfo2().getAccessFlags() & AccessFlag.BRIDGE) == 0
        if (Modifier.isVolatile(method.getModifiers())) {
            try {
                String methodName = method.getName();
                CtClass[] params = method.getParameterTypes();
                CtClass returnType = method.getReturnType();

                CtClass clazz = method.getDeclaringClass();
                ClassPool classPool = clazz.getClassPool();
                ConstPool constPool = method.getMethodInfo2().getConstPool();
                CodeIterator codeIt = method.getMethodInfo2().getCodeAttribute().iterator();
                while (codeIt.hasNext()) {
                    int pos = codeIt.next();

                    switch (codeIt.byteAt(pos)) {
                        case Opcode.INVOKEVIRTUAL: {
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
                                CtMethod m = c.getMethod(name, desc);

                                if (clazz.equals(m.getDeclaringClass()) && methodName.equals(m.getName())
                                        && m.getReturnType().subtypeOf(returnType) && params.length == m.getParameterTypes().length) {
                                    return m;
                                }
                            }

                            break;
                        }
                    }
                }
            } catch (NotFoundException | BadBytecode e) {
                e.printStackTrace();
            }
        }

        // TODO: Throw error or warn when resolution fails, e.g. when a synthetic bridge method has been tampered with...
        // Frankly, this should be possible without byte code inspection by going through the type hierarchies like
        // Spring's resolver does but fresh implementation of porting to load-time reflection is very cumbersome.

        return null;
    }
    static CtMethod getBridgeMethod(CtMethod method) {
        CtClass clazz = method.getDeclaringClass();
        String methodName = method.getName();
        for (CtMethod m : clazz.getDeclaredMethods()) {
            // Bridge method check via isVolatile... alternatively: (m.getMethodInfo2().getAccessFlags() & AccessFlag.BRIDGE) == 0
            if (Modifier.isVolatile(m.getModifiers()) && m.getName().equals(methodName) && method.equals(getBridgedMethod(m))) {
                return m;
            }
        }

        return null;
    }
}
