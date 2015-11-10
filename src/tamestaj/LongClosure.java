package tamestaj;

public interface LongClosure extends Closure<Long> {
    long evaluate(Environment environment) throws Throwable;
    default Long evaluateToObject(Environment environment) throws Throwable {
        return evaluate(environment);
    }
    default long evaluateToLong(Environment environment) throws Throwable {
        return evaluate(environment);
    }
}
