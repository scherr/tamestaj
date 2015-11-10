package tamestaj.examples.functor;

import java.util.function.Function;

public final class FunctorE<T> {
    private final Functor functor;
    private final Function combinedFunction;

    private FunctorE(Functor functor, Function combinedFunction) {
        this.functor = functor;
        this.combinedFunction = combinedFunction;
    }

    public static <T> FunctorE<T> of(Functor<T> f) {
        return new FunctorE<>(f, null);
    }

    public <U> FunctorE<U> fmap(Function<? super T, ? extends U> function) {
        if (this.combinedFunction == null) {
            return new FunctorE<>(functor, function);
        } else {
            return new FunctorE<>(functor, this.combinedFunction.andThen(function));
        }
    }

    public Functor<T> toFunctor() {
        if (combinedFunction == null) {
            return (Functor<T>) functor;
        } else {
            return (Functor<T>) functor.fmap(combinedFunction);
        }
    }
}
