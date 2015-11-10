package tamestaj;

public interface ShortClosure extends Closure<Short> {
    short evaluate(Environment environment) throws Throwable;
    default Short evaluateToObject(Environment environment) throws Throwable {
        return evaluate(environment);
    }
    default short evaluateToShort(Environment environment) throws Throwable {
        return evaluate(environment);
    }
}
