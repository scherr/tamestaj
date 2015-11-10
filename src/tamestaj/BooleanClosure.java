package tamestaj;

public interface BooleanClosure extends Closure<Boolean> {
    boolean evaluate(Environment environment) throws Throwable;
    default Boolean evaluateToObject(Environment environment) throws Throwable {
        return evaluate(environment);
    }
    default boolean evaluateToBoolean(Environment environment) throws Throwable {
        return evaluate(environment);
    }
}
