package tamestaj;

import javassist.*;
import javassist.bytecode.analysis.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

final class CarrierTransformer {
    private final static HashSet<CtClass> transformedCarriers = new HashSet<>();

    private CarrierTransformer() { }

    static void transformCarrierChecked(CtClass clazz) {
        if (!Util.isCarrier(clazz) || transformedCarriers.contains(clazz)) { return; }

        ArrayList<CtClass> classChain = new ArrayList<>();
        while (!clazz.equals(Type.OBJECT.getCtClass())) {
            classChain.add(clazz);
            try {
                clazz = clazz.getSuperclass();
            } catch (NotFoundException e) {
                break;
            }

            if (transformedCarriers.contains(clazz)) {
                break;
            }
        }

        Collections.reverse(classChain);

        for (CtClass c : classChain) {
            c.setModifiers(Modifier.PUBLIC);

            String body;
            if (c.equals(Util.LOCAL_CARRIER_CLASS) || c.equals(Util.GLOBAL_CARRIER_CLASS)) {
                body = "this(payload);";
            } else {
                body = "super((" + Util.DISAMBIGUATION_PARAMETER_CLASS.getName() + ") null, payload);";
            }

            try {
                CtConstructor constructor = CtNewConstructor.make("public constructor(" + Util.DISAMBIGUATION_PARAMETER_CLASS.getName() + " d, " + Util.EXPRESSION_CLASS.getName() + " payload) { " + body + " }", c);
                c.addConstructor(constructor);
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }

            // We have to make all abstract methods non-abstract
            for (CtMethod m : c.getDeclaredMethods()) {
                if (Modifier.isAbstract(m.getModifiers())) {
                    m.setModifiers(m.getModifiers() & ~Modifier.ABSTRACT);
                    try {
                        m.setBody(null);
                    } catch (CannotCompileException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            transformedCarriers.add(c);
        }
    }
}
