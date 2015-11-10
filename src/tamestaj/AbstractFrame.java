package tamestaj;

@SuppressWarnings("unused")
class AbstractFrame<L, S> implements Frame<L, S> {
    private final Object[] locals;
    private final Object[] stack;
    private int stackSize;

    protected AbstractFrame(int maxLocals, int maxStack) {
        locals = new Object[maxLocals];
        stack = new Object[maxStack];
    }

    protected AbstractFrame(L[] locals, S[] stack) {
        this.locals = locals;
        this.stack = stack;
    }

    final int getMaxLocals() {
        return locals.length;
    }

    final int getMaxStack() {
        return stack.length;
    }

    final int getStackSize() {
        return stackSize;
    }

    public S peek() {
        return (S) stack[stackSize - 1];
    }

    public S pop() {
        stackSize--;
        return (S) stack[stackSize];
    }

    public void push(S value) {
        stack[stackSize] = value;
        stackSize++;
    }

    public void setStack(int offset, S value) {
        stack[stackSize - 1 - offset] = value;
    }

    public S getStack(int offset) {
        return (S) stack[stackSize - 1 - offset];
    }

    public void setLocal(int index, L value) {
        locals[index] = value;
    }

    public L getLocal(int index) {
        return (L) locals[index];
    }

    public void clearStack() {
        stackSize = 0;
    }

    public S pop2() {
        pop();
        S v = pop();

        return v;
    }

    public void push2(S value) {
        push(value);
        push(null);
    }

    public void setLocal2(int index, L value) {
        setLocal(index, value);
        setLocal(index + 1, null);
    }

    public L getLocal2(int index) {
        return getLocal(index);
    }

    void copyLocalsInto(AbstractFrame<L, S> frame) {
        System.arraycopy(locals, 0, frame.locals, 0, locals.length);
    }

    void copyStackInto(AbstractFrame<L, S> frame) {
        System.arraycopy(stack, 0, frame.stack, 0, stack.length);
        frame.stackSize = stackSize;
    }

    void copyInto(AbstractFrame<L, S> frame) {
        copyLocalsInto(frame);
        copyStackInto(frame);
    }

    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (!(obj instanceof AbstractFrame)) { return false; }
        AbstractFrame frame = (AbstractFrame) obj;

        for (int i = 0; i < locals.length; i++) {
            if (locals[i] != null && frame.locals[i] != null) {
                if (!locals[i].equals(frame.locals[i])) {
                    return false;
                }
            } else if (locals[i] == null && frame.locals[i] != null) {
                return false;
            } else if (locals[i] != null && frame.locals[i] == null) {
                return false;
            }
        }

        for (int i = 0; i < stackSize; i++) {
            if (stack[i] != null) {
                if (!stack[i].equals(frame.stack[i])) {
                    return false;
                }
            }
        }

        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("locals = [");
        for (int i = 0; i < locals.length; i++) {
            sb.append(locals[i] == null ? "empty" : locals[i].toString());
            if (i < locals.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("] stack = [");
        for (int i = 0; i < stackSize; i++) {
            sb.append(stack[i]);
            if (i < stackSize - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");

        return sb.toString();
    }
}
