package tamestaj;

// This class should only be accessible by instrumented and generated code!
// We cannot really guarantee this, but we can at least temporarily make sure that it only becomes
// public at run time.

final class Trace {
    private final int position;
    private final int repetitions;
    private final int hashCode;
    private final Trace previous;

    private Trace(Trace trace, int position) {
        if (trace == null) {
            this.position = position;
            this.repetitions = 0;
            this.hashCode = position;
            this.previous = null;
        } else {
            if (position == trace.position) {
                this.position = position;
                this.repetitions = trace.repetitions + 1;
                this.hashCode = trace.hashCode + 31;
                this.previous = trace.previous;
            } else {
                this.position = position;
                this.repetitions = 0;
                this.hashCode = trace.hashCode + 31 * position;
                this.previous = trace;
            }
        }
    }

    static Trace record(Trace trace, int position) {
        return new Trace(trace, position);
    }

    public int hashCode() {
        return hashCode;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Trace) {
            Trace u = this;
            Trace t = (Trace) obj;

            while (u != null && t != null) {
                if (t == u) { return true; }

                // if (t.hashCode != u.hashCode) { return false; }

                if (u.position != t.position) { return false; }
                if (u.repetitions != t.repetitions) { return false; }

                t = t.previous;
                u = u.previous;
            }

            return t == u;
        }

        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("");

        Trace u = this;
        while (u != null) {
            sb.insert(0, "(" + u.position + ", " + u.repetitions + ")");
            if (u.previous != null) {
                sb.insert(0, " -> ");
            }
            u = u.previous;
        }
        return sb.toString();
    }
}
