package tamestaj;

import com.google.common.collect.ImmutableSet;
import javassist.CtClass;

@SuppressWarnings("unused")
abstract class Use extends Node {
    final private UseIndex useIndex;
    final private Type type;
    private ImmutableSet<Flow.Data> inData;

    Use(UseIndex useIndex, Type type) {
        this.useIndex = useIndex;
        this.type = type;
    }

    UseIndex getUseIndex() { return useIndex; }
    Type getType() { return type; }

    abstract void accept(Visitor visitor);

    public int hashCode() { return useIndex.hashCode(); }
    public boolean equals(Object obj) {
        if (!(obj instanceof Use)) { return false; }
        return useIndex.equals(((Use) obj).useIndex);
    }

    ImmutableSet<Flow.Data> getInData() { return inData; }
    void setInData(ImmutableSet<Flow.Data> inData) {
        if (this.inData != null) {
            throw new UnsupportedOperationException("May only be set once!");
        }
        this.inData = inData;
    }

    interface Visitor {
        void visit(Use.Opaque use);
        void visit(Argument argument);
    }
    public String toString() {
        return useIndex.toString();
    }

    final static class Opaque extends Use {
        private ImmutableSet<Flow.Control> inControl;
        private ImmutableSet<Flow.Control> outControl;

        Opaque(UseIndex useIndex, Type type) { super(useIndex, type); }

        ImmutableSet<Flow.Control> getInControl() { return inControl; }
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

        void accept(Use.Visitor visitor) { visitor.visit(this); }
    }

    final static class Argument extends Use {
        private final int index;
        private final ImmutableSet<CtClass> acceptedLanguages;

        private Source.Staged consumer;

        Argument(UseIndex useIndex, Type type, int index, ImmutableSet<CtClass> acceptedLanguages) {
            super(useIndex, type);
            this.index = index;
            this.acceptedLanguages = acceptedLanguages;
        }

        void setConsumer(Source.Staged consumer) { this.consumer = consumer; }

        ImmutableSet<Flow.Control> getInControl() { return consumer.getInControl(); }
        void setInControl(ImmutableSet<Flow.Control> inControl) { throw new UnsupportedOperationException(); }

        ImmutableSet<Flow.Control> getOutControl() { return consumer.getOutControl(); }
        void setOutControl(ImmutableSet<Flow.Control> outControl) { throw new UnsupportedOperationException(); }

        int getIndex() { return index; }
        Source.Staged getConsumer() { return consumer; }
        ImmutableSet<CtClass> getAcceptedLanguages() { return acceptedLanguages; }
        void accept(Visitor visitor) { visitor.visit(this); }
    }
}
