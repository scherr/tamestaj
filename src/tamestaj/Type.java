package tamestaj;

import java.util.*;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

/*
 * Based on javassist.bytecode.analysis.Type but simplified (without multi, i.e. intersection types)
 *
 * For details refer to the original in:
 *
 * www.javassist.org
 *
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 */

public class Type {
    private final CtClass clazz;

    private static final Map<CtClass, Type> primitiveTypeMap = new IdentityHashMap<>();

    public static final Type DOUBLE = Type.of(CtClass.doubleType);
    public static final Type BOOLEAN = Type.of(CtClass.booleanType);
    public static final Type LONG = Type.of(CtClass.longType);
    public static final Type CHAR = Type.of(CtClass.charType);
    public static final Type BYTE = Type.of(CtClass.byteType);
    public static final Type SHORT = Type.of(CtClass.shortType);
    public static final Type INT = Type.of(CtClass.intType);
    public static final Type FLOAT = Type.of(CtClass.floatType);
    public static final Type VOID = Type.of(CtClass.voidType);

    public static final Type NULL = new Type(null);
    public static final Type RETURN_ADDRESS = new Type(null);
    public static final Type TOP = new Type(null);
    public static final Type BOGUS = new Type(null);

    public static final Type OBJECT = of("java.lang.Object");
    public static final Type SERIALIZABLE = of("java.io.Serializable");
    public static final Type CLONEABLE = of("java.lang.Cloneable");
    public static final Type THROWABLE = of("java.lang.Throwable");

    static {
        primitiveTypeMap.put(CtClass.doubleType, DOUBLE);
        primitiveTypeMap.put(CtClass.longType, LONG);
        primitiveTypeMap.put(CtClass.charType, CHAR);
        primitiveTypeMap.put(CtClass.shortType, SHORT);
        primitiveTypeMap.put(CtClass.intType, INT);
        primitiveTypeMap.put(CtClass.floatType, FLOAT);
        primitiveTypeMap.put(CtClass.byteType, BYTE);
        primitiveTypeMap.put(CtClass.booleanType, BOOLEAN);
        primitiveTypeMap.put(CtClass.voidType, VOID);
    }

    private Type(CtClass clazz) {
        this.clazz = clazz;
    }

    static Type of(CtClass clazz) {
        return primitiveTypeMap.getOrDefault(clazz, new Type(clazz));
    }

    static Type of(String name) {
        try {
            return new Type(ClassPool.getDefault().get(name));
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public CtClass getCtClass() {
        return clazz;
    }

    boolean isTwoWordPrimitive() {
        return this == DOUBLE || this == LONG;
    }
    boolean isReference() {
        if (this == NULL) {
            return true;
        } else {
            return !isSpecial() && !clazz.isPrimitive();
        }
    }
    boolean isPrimitive() {
        return !isSpecial() && clazz.isPrimitive();
    }
    boolean isSpecial() {
        return clazz == null;
    }

    boolean isArray() {
        return clazz != null && clazz.isArray();
    }
    int getArrayDimensions() {
        if (!isArray()) {
            return 0;
        }

        String name = clazz.getName();
        int pos = name.length() - 1;
        int count = 0;
        while (name.charAt(pos) == ']' ) {
            pos -= 2;
            count++;
        }

        return count;
    }
    Type getArrayComponent() {
        if (this.clazz == null || !this.clazz.isArray()) {
            return null;
        }

        CtClass component;
        try {
            component = this.clazz.getComponentType();
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }

        Type type = primitiveTypeMap.get(component);
        if (type != null) {
            return type;
        } else {
            return Type.of(component);
        }
    }
    Type getArrayRootComponent(Type type) {
        while (type.isArray()) {
            type = type.getArrayComponent();
        }

        return type;
    }

    boolean isAssignableFrom(Type type) {
        return isAssignableFrom(type, true);
    }

    boolean isAssignableFrom(Type type, boolean interfaceAsObject) {
        if (this == type) {
            return true;
        }

        if ((type == NULL && isReference()) || (this == NULL && type.isReference())) {
            return true;
        }

        if (isSpecial() || type.isSpecial()) {
            return false;
        }

        if (interfaceAsObject && clazz.isInterface()) {
            return true;
        }

        // Should this consider primitive subtyping? Currently it does not!
        try {
            return type.clazz.subtypeOf(clazz);
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    static CtClass findCommonSuperClass(CtClass c1, CtClass c2) throws NotFoundException {
        int c1Depth = -1;
        for (CtClass temp = c1; temp != null; temp = temp.getSuperclass()) {
            c1Depth++;
        }
        int c2Depth = -1;
        for (CtClass temp = c2; temp != null; temp = temp.getSuperclass()) {
            c2Depth++;
        }

        CtClass shallow;
        CtClass deep;
        int depthDifference;
        if (c1Depth > c2Depth) {
            shallow = c2;
            deep = c1;
            depthDifference = c1Depth - c2Depth;
        } else {
            shallow = c1;
            deep = c2;
            depthDifference = c2Depth - c1Depth;
        }

        for (int i = 0; i < depthDifference; i++) {
            deep = deep.getSuperclass();
        }

        while (!Objects.equals(deep, shallow)) {
            deep = deep.getSuperclass();
            shallow = shallow.getSuperclass();
        }

        return deep;
    }

    private Type createArray(Type rootComponent, int dimensions) {
        String name = arrayName(rootComponent.clazz.getName(), dimensions);

        Type type;
        try {
            type = Type.of(rootComponent.clazz.getClassPool().get(name));
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }

        return type;
    }
    private String arrayName(String component, int dimensions) {
        int i = component.length();
        int size = i + dimensions * 2;
        char[] string = new char[size];
        component.getChars(0, i, string, 0);
        while (i < size) {
            string[i++] = '[';
            string[i++] = ']';
        }
        component = new String(string);
        return component;
    }
    private Type mergeArray(Type type) {
        Type typeRootComponent = getArrayRootComponent(type);
        Type rootComponent = getArrayRootComponent(this);
        int typeDimensions = type.getArrayDimensions();
        int dimensions = this.getArrayDimensions();

        if (typeDimensions == dimensions) {
            Type mergedComponent = rootComponent.merge(typeRootComponent);

            if (mergedComponent == Type.BOGUS) {
                return Type.OBJECT;
            }

            return createArray(mergedComponent, dimensions);
        }

        Type targetRootComponent;
        int targetDimensions;

        if (typeDimensions < dimensions) {
            targetRootComponent = typeRootComponent;
            targetDimensions = typeDimensions;
        } else {
            targetRootComponent = rootComponent;
            targetDimensions = dimensions;
        }

        if (Objects.equals(CLONEABLE.clazz, targetRootComponent.clazz)
                || Objects.equals(SERIALIZABLE.clazz, targetRootComponent.clazz)) {
            return createArray(targetRootComponent, targetDimensions);
        }

        return createArray(OBJECT, targetDimensions);
    }

    private Type mergeClasses(Type type) throws NotFoundException {
        CtClass superClass = findCommonSuperClass(this.clazz, type.clazz);
        if (superClass == null) {
            return OBJECT;
        }

        return new Type(superClass);
    }

    public Type merge(Type type) {
        if (type.equals(this)) {
            return this;
        }

        if (    type.isPrimitive() && isPrimitive() &&
                (type == INT || type == CHAR || type == SHORT) &&
                (this == INT || this == CHAR || this == SHORT)) {
            // TODO: Is this correct?
            return INT;
        }

        if (!type.isReference() || !isReference()) {
            return BOGUS;
        }

        /*
        if (type == Type.UNINIT || this == Type.UNINIT) {
            return Type.UNINIT;
        }
        */

        if (type == Type.NULL) {
            return this;
        }
        if (this == Type.NULL) {
            return type;
        }

        if (isArray() && type.isArray()) {
            return mergeArray(type);
        }

        try {
            return mergeClasses(type);
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean equals(Object obj) {
        if (! (obj instanceof Type)) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (isSpecial()) {
            return this == obj;
        }

        return Objects.equals(clazz, ((Type) obj).clazz);
    }

    public String toString() {
        if (this == BOGUS) {
            return "BOGUS";
        } else if (this == NULL) {
            return "UNINIT";
        } else if (this == RETURN_ADDRESS) {
            return "RETURN_ADDRESS";
        } else if (this == TOP) {
            return "TOP";
        }

        return clazz.getName();
    }
}
