package tamestaj;

public interface DoubleClosure extends Closure<Double> {
    double evaluate(Environment environment) throws Throwable;
    default Double evaluateToObject(Environment environment) throws Throwable {
        return evaluate(environment);
    }
    default double evaluateToDouble(Environment environment) throws Throwable {
        return evaluate(environment);
    }
}
