package tamestaj;

public interface ByteClosure extends Closure<Byte> {
    byte evaluate(Environment environment) throws Throwable;
    default Byte evaluateToObject(Environment environment) throws Throwable {
        return evaluate(environment);
    }
    default byte evaluateToByte(Environment environment) throws Throwable {
        return evaluate(environment);
    }
}
