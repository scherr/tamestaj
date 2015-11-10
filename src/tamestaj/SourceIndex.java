package tamestaj;

import java.util.Arrays;

@SuppressWarnings("unused")
final class SourceIndex extends Index {
    private final int position;
    private final boolean isExplicit;
    private final int[] stackOffsets;
    private final int[] localIndices;
    private final int hashCode;

    private SourceIndex(int position, boolean isExplicit, int[] stackOffsets, int[] localIndices) {
        this.position = position;
        this.isExplicit = isExplicit;
        this.stackOffsets = stackOffsets;
        this.localIndices = localIndices;

        hashCode = position + 31 * (isExplicit ? -1 : 1) + 31 * 31 * Arrays.hashCode(stackOffsets) + 31 * 31 * 31 * Arrays.hashCode(localIndices);
    }

    static SourceIndex makeExplicitStackTop(int position) {
        return new SourceIndex(position, true, new int[]{ 0 }, null);
    }
    static SourceIndex makeImplicitStackTop(int position) {
        return new SourceIndex(position, false, new int[]{ 0 }, null);
    }
    static SourceIndex makeExplicitStack(int position, int stackOffset) {
        return new SourceIndex(position, true, new int[]{ stackOffset }, null);
    }
    static SourceIndex makeImplicitStack(int position, int stackOffset) {
        return new SourceIndex(position, false, new int[]{ stackOffset }, null);
    }
    static SourceIndex makeExplicitLocal(int position, int localIndex) {
        return new SourceIndex(position, true, null, new int[]{ localIndex });
    }
    static SourceIndex makeImplicitLocal(int position, int localIndex) {
        return new SourceIndex(position, false, null, new int[]{ localIndex });
    }
    static SourceIndex makeExplicit(int position, int[] stackOffsets, int[] localIndices) {
        if ((stackOffsets == null || stackOffsets.length == 0) && (localIndices == null || localIndices.length == 0)) {
            throw new IllegalArgumentException();
        }

        return new SourceIndex(position, true, copySortAndRemoveDuplicates(stackOffsets), copySortAndRemoveDuplicates(localIndices));
    }
    static SourceIndex makeImplicit(int position, int[] stackOffsets, int[] localIndices) {
        if ((stackOffsets == null || stackOffsets.length == 0) && (localIndices == null || localIndices.length == 0)) {
            throw new IllegalArgumentException();
        }

        return new SourceIndex(position, false, copySortAndRemoveDuplicates(stackOffsets), copySortAndRemoveDuplicates(localIndices));
    }

    private static int[] copySortAndRemoveDuplicates(int[] array) {
        if (array == null) {
            return array;
        }
        array = Arrays.copyOf(array, array.length);
        Arrays.sort(array);
        int dupCount = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] == array[i - 1]) {
                dupCount++;
            }
        }
        if (dupCount > 0) {
            int[] newArray = new int[array.length - dupCount];
            newArray[0] = array[0];
            int newI = 1;
            for (int i = 1; i < array.length; i++) {
                if (array[i] != array[i - 1]) {
                    newArray[newI] = array[i];
                    newI++;
                }
            }
            return newArray;
        } else {
            return array;
        }
    }

    boolean isExplicit() { return isExplicit; }
    boolean hasStackOffsets() { return stackOffsets != null && stackOffsets.length > 0; }
    boolean hasLocalIndices() { return localIndices != null && localIndices.length > 0; }
    boolean isStack() { return hasStackOffsets() && !hasLocalIndices(); }
    boolean isLocal() { return hasLocalIndices() && !hasStackOffsets(); }
    boolean isStackTop() { return isStack() && stackOffsets[0] == 0; }
    int getPosition() { return position; }
    int getFirstStackOffset() { return stackOffsets[0]; }
    int getFirstLocalIndex() { return localIndices[0]; }
    int getStackOffsetCount() { return stackOffsets == null ? 0 : stackOffsets.length; }
    int getStackOffset(int index) { return stackOffsets[index]; }
    int getLocalIndexCount() { return localIndices == null ? 0 : localIndices.length; }
    int getLocalIndex(int index) { return localIndices[index]; }

    public int hashCode() { return hashCode; }
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (!(obj instanceof SourceIndex)) { return false; }
        SourceIndex s = (SourceIndex) obj;
        return position == s.position && isExplicit == s.isExplicit && Arrays.equals(stackOffsets, s.stackOffsets) && Arrays.equals(localIndices, s.localIndices);
    }
    public String toString() {
        return "s" + (isExplicit ? "e" : "i") + position + "s" + (stackOffsets == null ? "[]" : Arrays.toString(stackOffsets)) + "l" + (localIndices == null ? "[]" : Arrays.toString(localIndices));
    }

    public int compareTo(SourceIndex sourceIndex) {
        if (position > sourceIndex.position) {
            return 1;
        } else if (position < sourceIndex.position) {
            return -1;
        } else {
            if (isExplicit == sourceIndex.isExplicit) {
                return 0;
            } else {
                if (isExplicit) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }
    }
}
