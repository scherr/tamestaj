package tamestaj.examples.immutablelist;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import javassist.*;
import tamestaj.GlobalCarrier;
import tamestaj.annotations.Accept;
import tamestaj.annotations.Stage;
import tamestaj.util.CtClassLoader;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public abstract class ImmListE<F> extends GlobalCarrier {
    private ImmListE() { }

    private static class Of<F> extends ImmListE<F> {
        final ImmutableList<F> input;

        Of(ImmutableList<F> input) { this.input = input; }

        @Override
        public ImmutableList<F> eval() { return input; }

        @Override
        int isomorphicHashCode() {
            return 1;
        }

        @Override
        boolean isIsomorphicTo(ImmListE listE) {
            if (!(listE instanceof Of)) { return false; }
            return true;
        }

        @Override
        boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, ImmListE listE) {
            if (!(listE instanceof Of)) { return false; }
            return true;
        }
    }

    private static class Map<F, T> extends ImmListE<T> {
        private final ImmListE<F> inputE;
        private final Function<? super F, T> function;
        private Map(ImmListE<F> inputE, Function<? super F, T> function) {
            this.inputE = inputE;
            this.function = function;
        }

        @Override
        int isomorphicHashCode() {
            return 31 * inputE.isomorphicHashCode();
        }

        @Override
        boolean isIsomorphicTo(ImmListE listE) {
            if (!(listE instanceof Map)) { return false; }
            return inputE.isIsomorphicTo(((Map) listE).inputE);
        }

        @Override
        boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, ImmListE listE) {
            Object o = identityMap.get(this);
            if (o != null) {
                return o == listE;
            } else {
                if (!(listE instanceof Map)) { return false; }
                if (!inputE.isIsomorphicTo(identityMap, ((Map) listE).inputE)) { return false; }

                identityMap.put(this, listE);
                return true;
            }
        }
    }
    private static class Filter<F> extends ImmListE<F> {
        private final ImmListE<F> inputE;
        private final Predicate<? super F> predicate;
        private Filter(ImmListE<F> inputE, Predicate<? super F> predicate) {
            this.inputE = inputE;
            this.predicate = predicate;
        }

        @Override
        int isomorphicHashCode() {
            return 31 * inputE.isomorphicHashCode();
        }

        @Override
        boolean isIsomorphicTo(ImmListE listE) {
            if (!(listE instanceof Filter)) { return false; }
            return inputE.isIsomorphicTo(((Filter) listE).inputE);
        }

        @Override
        boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, ImmListE listE) {
            Object o = identityMap.get(this);
            if (o != null) {
                return o == listE;
            } else {
                if (!(listE instanceof Filter)) { return false; }
                if (!inputE.isIsomorphicTo(identityMap, ((Filter) listE).inputE)) { return false; }

                identityMap.put(this, listE);
                return true;
            }
        }
    }

    private static class IsomorphismWrapper {
        private final ImmListE listE;

        private IsomorphismWrapper(ImmListE listE) {
            this.listE = listE;
        }

        @Override
        public int hashCode() {
            return listE.isomorphicHashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return ((IsomorphismWrapper) obj).listE.isIsomorphicTo(new IdentityHashMap<>(), listE);
        }
    }
    private final static ConcurrentMap<IsomorphismWrapper, Function<Object[], ImmutableList>> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(100)
            .<IsomorphismWrapper, Function<Object[], ImmutableList>>build().asMap();

    @Stage(language = ImmListL.class, isStrict = true)
    public ImmutableList<F> eval() {
        LinkedList<Object> ops = new LinkedList<>();
        ImmListE temp = this;
        while (!(temp instanceof Of)) {
            if (temp instanceof Map) {
                ops.addFirst(((Map) temp).function);
                temp = ((Map) temp).inputE;
            } else if (temp instanceof Filter) {
                ops.addFirst(((Filter) temp).predicate);
                temp = ((Filter) temp).inputE;
            }
        }

        ImmutableList input = ((Of) temp).input;

        Function<Object[], ImmutableList> cachedFun = cache.get(new IsomorphismWrapper(this));
        if (cachedFun == null) {
            ClassPool cp = ClassPool.getDefault();
            try {
                CtClass funClazz = cp.makeClass(ImmListE.class.getName() + "$FusedOps");
                funClazz.setModifiers(Modifier.PUBLIC | Modifier.FINAL);
                funClazz.setInterfaces(new CtClass[]{cp.get(Function.class.getName())});

                StringBuilder funSource = new StringBuilder();
                funSource.append("public Object apply(Object objs) {\n");

                for (int i = 0; i < ops.size(); i++) {
                    if (ops.get(i) instanceof Function) {
                        funSource.append("    " + Function.class.getName() + " f" + (i + 1) + " = (" + Function.class
                                .getName() + ") ((Object[]) objs)[" + (i + 1) + "];\n");
                    } else if (ops.get(i) instanceof Predicate) {
                        funSource.append("    " + Predicate.class.getName() + " p" + (i + 1) + " = (" + Predicate.class
                                .getName() + ") ((Object[]) objs)[" + (i + 1) + "];\n");
                    }
                }
                funSource.append("    " + ImmutableList.class.getName() + "$Builder builder = " + ImmutableList.class.getName() + ".builder();\n");
                funSource.append("    " + Iterator.class.getName() + " iterator = ((" + Iterable.class.getName() + ") " +
                        "((Object[]) objs)[0]).iterator();\n");
                funSource.append("    while (iterator.hasNext()) {\n"
                        + "        Object o = iterator.next();\n");
                for (int i = 0; i < ops.size(); i++) {
                    if (ops.get(i) instanceof Function) {
                        funSource.append("        o = f" + (i + 1) + ".apply(o);\n");
                    } else if (ops.get(i) instanceof Predicate) {
                        funSource.append("        if (!p" + (i + 1) + ".apply(o)) { continue; }\n");
                    }
                }
                funSource.append("        builder.add(o);\n"
                        + "    }\n"
                        + "    return builder.build();\n"
                        + "}");
                CtMethod funMethod = CtNewMethod.make(funSource.toString(), funClazz);
                funClazz.addMethod(funMethod);

                CtClassLoader loader = new CtClassLoader();
                Class<?> funC = loader.load(funClazz);
                funClazz.detach();

                cachedFun = (Function<Object[], ImmutableList>) funC.newInstance();
                cache.putIfAbsent(new IsomorphismWrapper(this), cachedFun);
            } catch (NotFoundException | CannotCompileException | InstantiationException | IllegalAccessException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        Object[] objs = new Object[ops.size() + 1];
        objs[0] = input;
        int i = 1;
        for (Object op : ops) {
            objs[i] = op;
            i++;
        }
        return cachedFun.apply(objs);
    }

    @Stage(language = ImmListL.class)
    public static <F> ImmListE<F> of(ImmutableList<F> list) {
        return new Of<>(list);
    }

    @Stage(language = ImmListL.class)
    public final <T> ImmListE<T> map(@Accept(languages = {}) Function<? super F, T> function) {
        return new Map<>(this, function);
    }

    @Stage(language = ImmListL.class)
    public final ImmListE<F> filter(@Accept(languages = {}) Predicate<? super F> predicate) {
        return new Filter<>(this, predicate);
    }

    abstract int isomorphicHashCode();
    abstract boolean isIsomorphicTo(ImmListE listE);
    abstract boolean isIsomorphicTo(IdentityHashMap<Object, Object> identityMap, ImmListE listE);
}
