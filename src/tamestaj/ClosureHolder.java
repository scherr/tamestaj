package tamestaj;


// This class should only be accessible from instrumented and generated code!
// We cannot really guarantee this, but we can at least temporarily make sure that it only becomes
// public at run time.

import java.util.HashMap;

abstract class ClosureHolder<T extends Closure<?>> {
    private ClosureHolder() { }

    static <T extends Closure<?>> ClosureHolder<T> make(boolean isPermanent) {
        return new ClosureHolder<T>() {
            private T closure;
            private int environmentSize;

            synchronized void set(T closure, int environmentSize) {
                this.closure = closure;
                this.environmentSize = environmentSize;
            }

            synchronized T getClosure() {
                return closure;
            }

            synchronized int getEnvironmentSize() {
                return environmentSize;
            }

            public boolean isPermanent() {
                return isPermanent;
            }
        };
    }

    static <T extends Closure<?>> ClosureHolder<T> makeTraceCacheProxy(Trace trace, HashMap<Trace, ClosureHolder<T>> map) {
        return new ClosureHolder<T>() {
            private ClosureHolder<T> closureHolder;

            private void initializeClosureHolder() {
                if (closureHolder == null) {
                    closureHolder = map.get(trace);
                    if (closureHolder == null) {
                        closureHolder = ClosureHolder.make(false);
                        map.put(trace, closureHolder);
                    }
                }
            }

            void set(T closure, int environmentSize) {
                initializeClosureHolder();
                closureHolder.set(closure, environmentSize);
            }

            T getClosure() {
                initializeClosureHolder();
                return closureHolder.getClosure();
            }

            int getEnvironmentSize() {
                initializeClosureHolder();
                return closureHolder.getEnvironmentSize();
            }

            boolean isPermanent() {
                return false;
            }
        };
    }

    abstract void set(T closure, int environmentSize);
    abstract T getClosure();
    abstract int getEnvironmentSize();
    abstract boolean isPermanent();
}
