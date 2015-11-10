package tamestaj;

import javassist.CtBehavior;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

public final class StaticInfo {
    public enum Element {
        ORIGIN,
        INFERRED_TYPES
    }

    // Note: The behavior (method, constructor, etc.) will be modified at the time of usage, so we cannot get the
    //       original line number from the position, so we need to save it at construction time!
    public static final class Origin {
        private final CtBehavior behavior;
        private final int position;
        private final int lineNumber;

        private final int hashCode;

        private Origin(CtBehavior behavior, int position) {
            this.behavior = behavior;
            this.position = position;
            this.lineNumber = behavior.getMethodInfo().getLineNumber(position);
            hashCode = behavior.hashCode() + 31 * position;
        }

        static Origin make(CtBehavior behavior, int position) {
            return new Origin(behavior, position);
        }

        public CtBehavior getBehavior() {
            return behavior;
        }

        public int getPosition() {
            return position;
        }

        public OptionalInt getLineNumber() {
            return lineNumber >= 0 ? OptionalInt.of(lineNumber) : OptionalInt.empty();
        }

        public int hashCode() {
            return hashCode;
        }

        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (!(obj instanceof Origin)) { return false; }

            Origin o = (Origin) obj;
            return behavior.equals(o.behavior) && position == o.position;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(Element.ORIGIN);
            sb.append("(");
            sb.append(behavior.getLongName());
            sb.append(", ");
            sb.append(position);
            if (lineNumber >= 0) {
                sb.append(", ");
                sb.append(lineNumber);
            }
            sb.append(")");

            return sb.toString();
        }
    }

    public static final class InferredTypes {
        private final Type type;
        private final Type[] argumentTypes;

        private final int hashCode;

        private InferredTypes(Type type, Type[] argumentTypes) {
            this.type = type;
            this.argumentTypes = argumentTypes;
            hashCode = Arrays.hashCode(argumentTypes) + 31 * type.hashCode();
        }

        static InferredTypes make(Type type, Type[] argumentTypes) {
            return new InferredTypes(type, argumentTypes);
        }

        /*
        public int getArgumentTypesLength() {
            return argumentTypes.length;
        }
        */

        public Type getArgumentType(int index) {
            return argumentTypes[index];
        }

        public Type getType() {
            return type;
        }

        public int hashCode() {
            return hashCode;
        }

        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (!(obj instanceof InferredTypes)) { return false; }

            InferredTypes inf = (InferredTypes) obj;
            return Arrays.equals(argumentTypes, inf.argumentTypes) && type.equals(inf.type);
        }
    }

    private final Origin origin;
    private final InferredTypes inferredTypes;

    private StaticInfo(Origin origin, InferredTypes inferredTypes) {
        this.origin = origin;
        this.inferredTypes = inferredTypes;
    }

    static StaticInfo make(Origin origin, InferredTypes inferredTypes) {
        return new StaticInfo(origin, inferredTypes);
    }

    public Optional<Origin> getOrigin() {
        return Optional.ofNullable(origin);
    }

    public Optional<InferredTypes> getInferredTypes() {
        return Optional.ofNullable(inferredTypes);
    }

    public int hashCode() {
        return Objects.hash(origin, inferredTypes);
    }

    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (!(obj instanceof StaticInfo)) { return false; }

        StaticInfo si = (StaticInfo) obj;
        return origin.equals(si.origin) && inferredTypes.equals(si.inferredTypes);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        if (origin != null) {
            sb.append(origin);
        }
        if (inferredTypes != null) {
            sb.append(", ");
            sb.append(inferredTypes);
        }
        sb.append(" }");
        return sb.toString();
    }
}
