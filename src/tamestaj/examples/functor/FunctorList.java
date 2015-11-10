package tamestaj.examples.functor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FunctorList<T> extends Functor<T> {
    private final ArrayList<T> list;

    private FunctorList(ArrayList<T> list) {
        this.list = list;
    }

    public FunctorList(List<T> list) {
        this.list = new ArrayList<>(list);
    }

    @Override
    public <B> FunctorList<B> fmap(Function<? super T, ? extends B> function) {
        return new FunctorList<>(list.stream().map(function).collect(Collectors.toCollection(ArrayList::new)));
    }

    public String toString() {
        return list.toString();
    }
}
