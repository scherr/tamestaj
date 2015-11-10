package tamestaj;

@SuppressWarnings("unused")
final class UseIndex extends Index {
    private final int position;
    private final int stackOffset;
    private final int localIndex;
    private final int hashCode;

    private UseIndex(int position, int stackOffset, int localIndex) {
        this.position = position;
        this.stackOffset = stackOffset;
        this.localIndex = localIndex;

        hashCode = position + 31 * stackOffset + 31 * 31 * localIndex;
    }

    static UseIndex makeStack(int position, int stackOffset) {
        return new UseIndex(position, stackOffset, -1);
    }
    static UseIndex makeLocal(int position, int localIndex) {
        return new UseIndex(position, -1, localIndex);
    }

    boolean isStackless() { return stackOffset == -1; }
    int getPosition() { return position; }
    int getStackOffset() { return stackOffset; }
    int getLocalIndex() { return localIndex; }

    public int hashCode() { return hashCode; }
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (!(obj instanceof UseIndex)) { return false; }
        UseIndex u = (UseIndex) obj;
        return position == u.position && stackOffset == u.stackOffset && localIndex == u.localIndex;
    }
    public String toString() {
        return "u" + position + (!isStackless() ? "s" + stackOffset : "l" + localIndex);
    }

    public int compareTo(UseIndex useIndex) {
        if (position > useIndex.position) {
            return 1;
        } else if (position < useIndex.position) {
            return -1;
        } else {
            if (isStackless() && useIndex.isStackless()) {
                return Integer.compare(localIndex, useIndex.localIndex);
            } else {
                return -Integer.compare(stackOffset, useIndex.stackOffset);
            }
        }
    }
}
