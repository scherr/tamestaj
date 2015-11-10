package tamestaj.examples.functor;

import tamestaj.annotations.Accept;
import tamestaj.annotations.Stage;

import java.util.function.Function;

public abstract class Functor<A> {
    @Stage(language = FunctorL.class)
    public abstract <B> Functor<B> fmap(@Accept(languages = {}) Function<? super A, ? extends B> function);
}
