package tamestaj;

import java.util.Objects;

@SuppressWarnings("unused")
final class Tuple<F, S> {
    private final F first;
    private final S second;
    private final int hashCode;

    Tuple(F first, S second) {
        this.first = first;
        this.second = second;
        hashCode = Objects.hash(first, second);
    }

    F getFirst() {
        return first;
    }

    S getSecond() {
        return second;
    }

    public int hashCode() {
        return hashCode;
    }

    public boolean equals(Object obj) {
        if (this == obj) { return true; }

        if (obj instanceof Tuple) {
            Tuple t = (Tuple) obj;
            return Objects.equals(first, t.first) && Objects.equals(second, t.second);
        }

        return false;
    }

    public String toString() {
        return "(" + first + ", " + second + ")";
    }
}
