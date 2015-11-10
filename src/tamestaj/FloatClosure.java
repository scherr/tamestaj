package tamestaj;

public interface FloatClosure extends Closure<Float> {
    float evaluate(Environment environment) throws Throwable;
    default Float evaluateToObject(Environment environment) throws Throwable {
        return evaluate(environment);
    }
    default float evaluateToFloat(Environment environment) throws Throwable {
        return evaluate(environment);
    }
}
