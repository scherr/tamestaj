package tamestaj;

public interface CharacterClosure extends Closure<Character> {
    char evaluate(Environment environment) throws Throwable;
    default Character evaluateToObject(Environment environment) throws Throwable {
        return evaluate(environment);
    }
    default char evaluateToCharacter(Environment environment) throws Throwable {
        return evaluate(environment);
    }
}
