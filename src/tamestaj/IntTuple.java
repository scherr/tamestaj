package tamestaj;

@SuppressWarnings("unused")
final class IntTuple implements Comparable<IntTuple> {
    private final int first;
    private final int second;
    private final int hashCode;

    IntTuple(int first, int second) {
        this.first = first;
        this.second = second;
        hashCode = first * 31 * second;
    }

    int getFirst() {
        return first;
    }

    int getSecond() {
        return second;
    }

    public int hashCode() {
        return hashCode;
    }

    public boolean equals(Object obj) {
        if (this == obj) { return true; }

        if (obj instanceof IntTuple) {
            IntTuple t = (IntTuple) obj;
            return first == t.first && second == t.second;
        }

        return false;
    }

    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    public int compareTo(IntTuple o) {
        if (first > o.first) {
            return 1;
        } else if (first < o.first) {
            return -1;
        } else {
            return Integer.compare(second, o.second);
        }
    }
}
