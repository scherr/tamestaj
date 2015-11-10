package tamestaj;

public interface IntegerClosure extends Closure<Integer> {
    int evaluate(Environment environment) throws Throwable;
    default Integer evaluateToObject(Environment environment) throws Throwable {
        return evaluate(environment);
    }
    default int evaluateToInteger(Environment environment) throws Throwable {
        return evaluate(environment);
    }
}
