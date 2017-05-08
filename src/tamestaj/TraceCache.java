package tamestaj;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// This class should only be accessible from instrumented and generated code!
// We cannot really guarantee this, but we can at least temporarily make sure that it only becomes
// public at run time.

final class TraceCache<T extends Closure<?>> {
    private final ThreadLocal<HashMap<Trace, ClosureHolder<T>>> internalMap;

    private TraceCache(int maxSize) {
        internalMap = new ThreadLocal<>();
        internalMap.set(new LinkedHashMap<Trace, ClosureHolder<T>>(maxSize * 4 / 3, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<Trace, ClosureHolder<T>> eldest) {
                return size() > maxSize;
            }
        });
    }

    static <T extends Closure<?>> TraceCache<T> make(int maxSize) {
        return new TraceCache<>(maxSize);
    }

    ClosureHolder<T> getCachedClosureHolder(Trace trace) {
        // Caching logic contained in proxy!
        return ClosureHolder.makeTraceCacheProxy(trace, internalMap.get());
    }
}
