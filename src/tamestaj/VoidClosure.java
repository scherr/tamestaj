package tamestaj;

public interface VoidClosure extends Closure<Void> {
    void evaluate(Environment environment) throws Throwable;
    default Void evaluateToObject(Environment environment) throws Throwable {
        evaluate(environment);
        return null;
    }
    default void evaluateToVoid(Environment environment) throws Throwable {
        evaluate(environment);
    }
}
