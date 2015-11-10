package tamestaj;

@SuppressWarnings("unused")
interface Frame<L, S> {
    S peek();
    S pop();
    void push(S value);
    void setStack(int offset, S value);
    S getStack(int offset);
    void setLocal(int index, L value);
    L getLocal(int index);
    void clearStack();
    S pop2();
    void push2(S value);
    void setLocal2(int index, L value);
    L getLocal2(int index);
}
