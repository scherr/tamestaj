package tamestaj.util;

import javassist.CannotCompileException;
import javassist.CtClass;

import java.io.IOException;

@SuppressWarnings("unused")
public class CtClassLoader extends ClassLoader {
    public Class<?> load(CtClass clazz) throws IOException, CannotCompileException {
        byte[] bytecode = clazz.toBytecode();

        return defineClass(clazz.getName(), bytecode, 0, bytecode.length);
    }
}
