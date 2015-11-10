package tamestaj;

import java.util.Objects;

@SuppressWarnings("unused")
class Flow {
    public static class Data {
        private final Source from;
        private final Use to;
        private final int hashCode;

        Data(Source from, Use to) {
            this.from = from;
            this.to = to;

            hashCode = Objects.hash(from, to);
        }

        Source getFrom() {
            return from;
        }
        Use getTo() {
            return to;
        }

        public int hashCode() { return hashCode; }
        public boolean equals(Object obj) {
            if (!(obj instanceof Data)) { return false; }
            Data d = ((Data) obj);
            return from.equals(d.from) && to.equals(d.to);
        }

        public String toString() {
            return (from.getSourceIndex()) + " -> " + to.getUseIndex();
        }
    }

    public static class Control {
        private final Node from;
        private final Node to;
        private final int hashCode;

        Control(Node from, Node to) {
            this.from = from;
            this.to = to;
            hashCode = Objects.hash(from, to);
        }

        Node getFrom() { return from; }
        Node getTo() { return to; }

        boolean flowsFromEntry() { return from == null; }
        boolean flowsToExit() { return to == null; }
        boolean flowsInternally() { return from != null && to != null; }

        public int hashCode() { return hashCode; }
        public boolean equals(Object obj) {
            if (!(obj instanceof Control)) { return false; }
            Control c = ((Control) obj);
            if (from == null) { return c.from == null && to.equals(c.to); }
            if (to == null) { return c.to == null && from.equals(c.from); }
            return from.equals(c.from) && to.equals(c.to);
        }

        public String toString() {
            String fromStr;
            if (from == null) {
                fromStr = "ENTRY";
            } else if (from instanceof Source) {
                fromStr = ((Source) from).getSourceIndex().toString();
            } else if (from instanceof Use) {
                fromStr = ((Use) from).getUseIndex().toString();
            } else {
                throw new RuntimeException();
            }

            String toStr;
            if (to == null) {
                toStr = "EXIT";
            } else if (to instanceof Source) {
                toStr = ((Source) to).getSourceIndex().toString();
            } else if (to instanceof Use) {
                toStr = ((Use) to).getUseIndex().toString();
            } else {
                throw new RuntimeException();
            }

            return fromStr + " -> " + toStr;
        }
    }
}
