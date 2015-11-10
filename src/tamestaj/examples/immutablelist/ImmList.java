package tamestaj.examples.immutablelist;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import tamestaj.annotations.Accept;
import tamestaj.annotations.Stage;

public final class ImmList {
    private ImmList() { }

    @Stage(language = ImmListL.class)
    public static <F, T> ImmutableList<T> map(ImmutableList<F> list, @Accept(languages = {}) Function<? super F, T> function) {
        // return FluentIterable.from(list).transform(function).toList();
        ImmutableList.Builder<T> builder = ImmutableList.builder();
        for (F f : list) {
            builder.add(function.apply(f));
        }
        return builder.build();
    }

    @Stage(language = ImmListL.class)
    public static <T> ImmutableList<T> filter(ImmutableList<T> list, @Accept(languages = {}) Predicate<? super T> predicate) {
        // return FluentIterable.from(list).filter(predicate).toList();
        ImmutableList.Builder<T> builder = ImmutableList.builder();
        for (T t : list) {
            if (predicate.apply(t)) {
                builder.add(t);
            }
        }
        return builder.build();
    }
}
