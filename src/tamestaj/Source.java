package tamestaj;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMember;
import javassist.CtMethod;

@SuppressWarnings("unused")
abstract class Source extends Node {
    final private SourceIndex sourceIndex;
    final private Type type;

    private ImmutableSet<Flow.Data> outData;
    private ImmutableSet<Flow.Control> inControl;
    private ImmutableSet<Flow.Control> outControl;

    Source(SourceIndex sourceIndex, Type type) {
        this.sourceIndex = sourceIndex;
        this.type = type;
    }


    ImmutableSet<Flow.Data> getOutData() { return outData; }
    void setOutData(ImmutableSet<Flow.Data> outData) {
        if (this.outData != null) {
            throw new UnsupportedOperationException("May only be set once!");
        }
        this.outData = outData;
    }

    ImmutableSet<Flow.Control> getInControl() {
        return inControl;
    }
    void setInControl(ImmutableSet<Flow.Control> inControl) {
        if (this.inControl != null) {
            throw new UnsupportedOperationException("May only be set once!");
        }
        this.inControl = inControl;
    }

    ImmutableSet<Flow.Control> getOutControl() { return outControl; }
    void setOutControl(ImmutableSet<Flow.Control> outControl) {
        if (this.outControl != null) {
            throw new UnsupportedOperationException("May only be set once!");
        }
        this.outControl = outControl;
    }

    SourceIndex getSourceIndex() { return sourceIndex; }
    Type getType() { return type; }

    abstract void accept(Visitor visitor);

    public int hashCode() { return sourceIndex.hashCode(); }
    public boolean equals(Object obj) {
        if (!(obj instanceof Source)) { return false; }
        return sourceIndex.equals(((Source) obj).sourceIndex);
    }

    interface Visitor extends Staged.Visitor, Constant.Visitor {
        void visit(Source.Opaque source);
    }

    final static class Opaque extends Source {
        Opaque(SourceIndex sourceIndex, Type type) { super(sourceIndex, type); }

        void accept(Source.Visitor visitor) { visitor.visit(this); }
        public String toString() {
            return "?[" + getSourceIndex() + "]";
        }
    }

    public abstract static class Staged extends Source {
        private final CtClass language;
        private final boolean isStrict;
        private final ImmutableSet<StaticInfo.Element> staticInfoElements;
        private final ImmutableList<Use.Argument> arguments;

        Staged(SourceIndex sourceIndex, Type type, CtClass language, boolean isStrict, ImmutableSet<StaticInfo.Element> staticInfoElements, ImmutableList<Use.Argument> arguments) {
            super(sourceIndex, type);
            this.language = language;
            this.isStrict = isStrict;
            this.staticInfoElements = staticInfoElements;
            this.arguments = arguments;
            for (Use.Argument a : arguments) {
                a.setConsumer(this);
            }
        }

        CtClass getLanguage() { return language; }
        boolean isStrict() { return isStrict; }
        ImmutableSet<StaticInfo.Element> getStaticInfoElements() {
            return staticInfoElements;
        }

        abstract CtMember getMember();

        ImmutableList<Use.Argument> getArguments() { return arguments; }

        abstract void accept(Visitor visitor);

        public String toString() {
            StringBuilder sb = new StringBuilder(getMember().getName());
            sb.append("[" + getSourceIndex() + "](");
            for (int i = 0; i < arguments.size(); i++) {
                sb.append(arguments.get(i).toString());
                if (i < arguments.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")");
            return sb.toString();
        }

        interface Visitor {
            void visit(FieldRead staged);
            void visit(FieldAssignment staged);
            void visit(MethodInvocation staged);
        }

        final static class FieldRead extends Staged {
            private final CtField field;

            FieldRead(SourceIndex sourceIndex, Type type, CtClass language, boolean isStrict, ImmutableSet<StaticInfo.Element> staticInfoElements, ImmutableList<Use.Argument> arguments, CtField field) {
                super(sourceIndex, type, language, isStrict, staticInfoElements, arguments);
                this.field = field;
            }

            CtField getMember() { return field; }
            void accept(Source.Visitor visitor) { visitor.visit(this); }
            void accept(Staged.Visitor visitor) { visitor.visit(this); }
        }
        final static class FieldAssignment extends Staged {
            private final CtField field;

            FieldAssignment(SourceIndex sourceIndex, Type type, CtClass language, ImmutableSet<StaticInfo.Element> staticInfoElements, ImmutableList<Use.Argument> arguments, CtField field) {
                // Field assignments must be strict!
                super(sourceIndex, type, language, true, staticInfoElements, arguments);
                this.field = field;
            }

            CtField getMember() { return field; }
            void accept(Source.Visitor visitor) { visitor.visit(this); }
            void accept(Staged.Visitor visitor) { visitor.visit(this); }
        }
        final static class MethodInvocation extends Staged {
            private final CtMethod method;

            MethodInvocation(SourceIndex sourceIndex, Type type, CtClass language, boolean isStrict, ImmutableSet<StaticInfo.Element> staticInfoElements, ImmutableList<Use.Argument> arguments, CtMethod method) {
                // Void methods are strict but that is checked and ensured when retrieving the stage annotation, not here!
                super(sourceIndex, type, language, isStrict, staticInfoElements, arguments);
                this.method = method;
            }

            CtMethod getMember() { return method; }
            void accept(Source.Visitor visitor) { visitor.visit(this); }
            void accept(Staged.Visitor visitor) { visitor.visit(this); }
        }
    }

    abstract static class Constant<V> extends Source {
        Constant(SourceIndex sourceIndex, Type type) { super(sourceIndex, type); }

        abstract V getValue();
        abstract void accept(Visitor visitor);
        public java.lang.String toString() {
            return getValue().toString();
        }

        interface Visitor {
            void visit(Null constant);
            void visit(Integer constant);
            void visit(Long constant);
            void visit(Float constant);
            void visit(Double constant);
            void visit(Byte constant);
            void visit(Short constant);
            void visit(String constant);
            void visit(Class constant);
        }

        final static class Null extends Constant<Object> {
            Null(SourceIndex sourceIndex) { super(sourceIndex, Type.OBJECT); }

            Object getValue() { return null; }
            void accept(Source.Visitor visitor) { visitor.visit(this); }
            void accept(Constant.Visitor visitor) { visitor.visit(this); }
            public java.lang.String toString() {
                return "nüll";
            }
        }

        final static class Integer extends Constant<java.lang.Integer> {
            private final int value;

            Integer(SourceIndex sourceIndex, int value) {
                super(sourceIndex, Type.INT);
                this.value = value;
            }

            void accept(Source.Visitor visitor) { visitor.visit(this); }
            void accept(Constant.Visitor visitor) { visitor.visit(this); }
            public java.lang.Integer getValue() { return value; }
        }

        final static class Long extends Constant<java.lang.Long> {
            private final long value;

            Long(SourceIndex sourceIndex, long value) {
                super(sourceIndex, Type.LONG);
                this.value = value;
            }

            void accept(Source.Visitor visitor) { visitor.visit(this); }
            void accept(Constant.Visitor visitor) { visitor.visit(this); }
            public java.lang.Long getValue() { return value; }
        }

        final static class Float extends Constant<java.lang.Float> {
            private final float value;

            Float(SourceIndex sourceIndex, float value) {
                super(sourceIndex, Type.FLOAT);
                this.value = value;
            }

            void accept(Source.Visitor visitor) { visitor.visit(this); }
            void accept(Constant.Visitor visitor) { visitor.visit(this); }
            public java.lang.Float getValue() { return value; }
        }

        final static class Double extends Constant<java.lang.Double> {
            private final double value;

            Double(SourceIndex sourceIndex, double value) {
                super(sourceIndex, Type.DOUBLE);
                this.value = value;
            }

            void accept(Source.Visitor visitor) { visitor.visit(this); }
            void accept(Constant.Visitor visitor) { visitor.visit(this); }
            public java.lang.Double getValue() { return value; }
        }

        final static class Byte extends Constant<java.lang.Byte> {
            private final byte value;

            Byte(SourceIndex sourceIndex, byte value) {
                super(sourceIndex, Type.BYTE);
                this.value = value;
            }

            void accept(Source.Visitor visitor) { visitor.visit(this); }
            void accept(Constant.Visitor visitor) { visitor.visit(this); }
            public java.lang.Byte getValue() { return value; }
        }

        final static class Short extends Constant<java.lang.Short> {
            private final short value;

            Short(SourceIndex sourceIndex, short value) {
                super(sourceIndex, Type.SHORT);
                this.value = value;
            }

            void accept(Source.Visitor visitor) { visitor.visit(this); }
            void accept(Constant.Visitor visitor) { visitor.visit(this); }
            public java.lang.Short getValue() { return value; }
        }

        final static class String extends Constant<java.lang.String> {
            private final java.lang.String value;

            String(SourceIndex sourceIndex, java.lang.String value) {
                super(sourceIndex, Util.STRING_TYPE);
                this.value = value;
            }

            void accept(Source.Visitor visitor) { visitor.visit(this); }
            void accept(Constant.Visitor visitor) { visitor.visit(this); }
            java.lang.String getValue() { return value; }
            public java.lang.String toString() {
                return "\"" + value + "\"";
            }
        }

        final static class Class extends Constant<java.lang.String> {
            private final java.lang.String value;

            Class(SourceIndex sourceIndex, java.lang.String value) {
                super(sourceIndex, Util.CLASS_TYPE);
                this.value = value;
            }

            void accept(Source.Visitor visitor) { visitor.visit(this); }
            void accept(Constant.Visitor visitor) { visitor.visit(this); }
            public java.lang.String getValue() { return value; }
        }
    }
}
