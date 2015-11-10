package tamestaj;

public interface Closure<V> {
    V evaluateToObject(Environment environment) throws Throwable;
    default boolean evaluateToBoolean(Environment environment) throws Throwable {
        return (Boolean) evaluateToObject(environment);
    }
    default int evaluateToInteger(Environment environment) throws Throwable {
        return (Integer) evaluateToObject(environment);
    }
    default long evaluateToLong(Environment environment) throws Throwable {
        return (Long) evaluateToObject(environment);
    }
    default float evaluateToFloat(Environment environment) throws Throwable {
        return (Float) evaluateToObject(environment);
    }
    default double evaluateToDouble(Environment environment) throws Throwable {
        return (Double) evaluateToObject(environment);
    }
    default byte evaluateToByte(Environment environment) throws Throwable {
        return (Byte) evaluateToObject(environment);
    }
    default char evaluateToCharacter(Environment environment) throws Throwable {
        return (Character) evaluateToObject(environment);
    }
    default short evaluateToShort(Environment environment) throws Throwable {
        return (Short) evaluateToObject(environment);
    }
    default void evaluateToVoid(Environment environment) throws Throwable {
        evaluateToObject(environment);
    }
}
