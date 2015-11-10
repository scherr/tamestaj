package tamestaj;

public interface ObjectClosure<V> extends Closure<V> {
    V evaluate(Environment environment) throws Throwable;
    default V evaluateToObject(Environment environment) throws Throwable {
        return evaluate(environment);
    }
}
