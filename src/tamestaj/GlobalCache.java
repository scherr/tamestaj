package tamestaj;

import com.google.common.cache.CacheBuilder;

import java.util.IdentityHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

final class GlobalCache {
    private final static class ExpressionKey {
        private final Expression.Staged staged;

        private ExpressionKey(Expression.Staged staged) {
            this.staged = staged;
        }

        public int hashCode() { return staged.isomorphicHashCode(); }

        public boolean equals(Object obj) {
            // return expression.isIsomorphicTo(new IdentityHashMap<>(), ((CachedExpression) obj).expression);

            // For whatever reason (???) changing the order here speeds up a lot!
            return ((ExpressionKey) obj).staged.isIsomorphicTo(new IdentityHashMap<>(), staged);
        }
    }

    // final static ConcurrentMap<CachedExpression, Object> closureHolderCache = new ConcurrentHashMap<>();
    // TODO: We should maybe opt for class-local / method-local / thread-local caches
    private static final ConcurrentMap<ExpressionKey, ClosureHolder<?>> map = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(100)
            .<ExpressionKey, ClosureHolder<?>>build().asMap();

    static ClosureHolder getCachedClosureHolder(Expression.Staged expression) {
        return map.get(new ExpressionKey(expression));
    }

    static <T extends Closure<?>> void cache(Expression.Staged staged, T closure, int environmentSize) {
        ClosureHolder<T> closureHolder = ClosureHolder.make(false);
        closureHolder.set(closure, environmentSize);
        map.put(new ExpressionKey((Expression.Staged) staged.cacheClone(new IdentityHashMap<>())), closureHolder);
    }
}
