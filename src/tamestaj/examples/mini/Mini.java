package tamestaj.examples.mini;

import tamestaj.GlobalCarrier;
import tamestaj.StaticInfo;
import tamestaj.annotations.Accept;
import tamestaj.annotations.Stage;

import java.util.IdentityHashMap;

public final class Mini {
    private Mini() { }

    public abstract static class BoolE extends GlobalCarrier {
        BoolE() { }

        abstract boolean run(IdentityHashMap<Object, Object> environment);
    }

    public abstract static class BoolV extends BoolE {
        BoolV() { }
    }

    public abstract static class IntE extends GlobalCarrier {
        IntE() { }

        abstract int run(IdentityHashMap<Object, Object> environment);
    }

    public abstract static class IntV extends IntE {
        IntV() { }
    }

    public abstract static class Stmt extends GlobalCarrier {
        Stmt() { }

        abstract IdentityHashMap<Object, Object> run(IdentityHashMap<Object, Object> environment);

        @Stage(language = MiniL.class, staticInfoElements = { StaticInfo.Element.ORIGIN })
        public Stmt then(Stmt v) {
            return new Stmt() {
                @Override
                IdentityHashMap<Object, Object> run(IdentityHashMap<Object, Object> environment) {
                    return v.run( Stmt.this.run(environment));
                }
            };
        }

        @Stage(language = MiniL.class, isStrict = true, staticInfoElements = { StaticInfo.Element.ORIGIN })
        public int intRun(IntV e) {
            return (int) this.run(new IdentityHashMap<>()).get(e);
        }
        @Stage(language = MiniL.class, isStrict = true, staticInfoElements = { StaticInfo.Element.ORIGIN })
        public boolean boolRun(BoolV e) {
            return (boolean) this.run(new IdentityHashMap<>()).get(e);
        }
    }

    @Stage(language = MiniL.class)
    public static IntE add(IntE a, IntE b) {
        return new IntE() {
            @Override
            int run(IdentityHashMap<Object, Object> environment) {
                int aR = a.run(environment);
                int bR = b.run(environment);
                return aR + bR;
            }
        };
    }
    @Stage(language = MiniL.class)
    public static IntE mul(IntE a, IntE b) {
        return new IntE() {
            @Override
            int run(IdentityHashMap<Object, Object> environment) {
                int aR = a.run(environment);
                int bR = b.run(environment);
                return aR * bR;
            }
        };
    }
    @Stage(language = MiniL.class)
    public static BoolE eq(IntE a, IntE b) {
        return new BoolE() {
            @Override
            boolean run(IdentityHashMap<Object, Object> environment) {
                int aR = a.run(environment);
                int bR = b.run(environment);
                return aR == bR;
            }
        };
    }
    @Stage(language = MiniL.class)
    public static BoolE leq(IntE a, IntE b) {
        return new BoolE() {
            @Override
            boolean run(IdentityHashMap<Object, Object> environment) {
                int aR = a.run(environment);
                int bR = b.run(environment);
                return aR <= bR;
            }
        };
    }
    @Stage(language = MiniL.class)
    public static BoolE and(BoolE a, BoolE b) {
        return new BoolE() {
            @Override
            boolean run(IdentityHashMap<Object, Object> environment) {
                boolean aR = a.run(environment);
                boolean bR = b.run(environment);
                return aR && bR;
            }
        };
    }
    @Stage(language = MiniL.class)
    public static BoolE or(BoolE a, BoolE b) {
        return new BoolE() {
            @Override
            boolean run(IdentityHashMap<Object, Object> environment) {
                boolean aR = a.run(environment);
                boolean bR = b.run(environment);
                return aR || bR;
            }
        };
    }

    @Stage(language = MiniL.class)
    public static IntE neg(IntE a) {
        return new IntE() {
            @Override
            int run(IdentityHashMap<Object, Object> environment) {
                int aR = a.run(environment);
                return -aR;
            }
        };
    }
    @Stage(language = MiniL.class)
    public static BoolE not(BoolE a) {
        return new BoolE() {
            @Override
            boolean run(IdentityHashMap<Object, Object> environment) {
                boolean aR = a.run(environment);
                return !aR;
            }
        };
    }

    @Stage(language = MiniL.class, staticInfoElements = { StaticInfo.Element.ORIGIN })
    public static IntV intVar(String name) {
        return new IntV() {
            @Override
            int run(IdentityHashMap<Object, Object> environment) {
                return (int) environment.get(this);
            }
        };
    }
    @Stage(language = MiniL.class, staticInfoElements = { StaticInfo.Element.ORIGIN })
    public static BoolV boolVar(String name) {
        return new BoolV() {
            @Override
            boolean run(IdentityHashMap<Object, Object> environment) {
                return (boolean) environment.get(this);
            }
        };
    }

    @Stage(language = MiniL.class)
    public static Stmt intAssign(IntV v, IntE e) {
        return new Stmt() {
            @Override
            IdentityHashMap<Object, Object> run(IdentityHashMap<Object, Object> environment) {
                int eR = e.run(environment);
                IdentityHashMap<Object, Object> env = new IdentityHashMap<>(environment);
                env.put(v, eR);
                return env;
            }
        };
    }
    @Stage(language = MiniL.class)
    public static Stmt boolAssign(BoolV v, BoolE e) {
        return new Stmt() {
            @Override
            IdentityHashMap<Object, Object> run(IdentityHashMap<Object, Object> environment) {
                boolean eR = e.run(environment);
                IdentityHashMap<Object, Object> env = new IdentityHashMap<>(environment);
                env.put(v, eR);
                return env;
            }
        };
    }

    @Stage(language = MiniL.class)
    public static IntE intLit(@Accept(languages = {}) int a) {
        return new IntE() {
            @Override
            int run(IdentityHashMap<Object, Object> environment) {
                return a;
            }
        };
    }
    @Stage(language = MiniL.class)
    public static BoolE boolLit(@Accept(languages = {}) boolean a) {
        return new BoolE() {
            @Override
            boolean run(IdentityHashMap<Object, Object> environment) {
                return a;
            }
        };
    }

    @Stage(language = MiniL.class)
    public static Stmt whileDo(BoolE test, Stmt s) {
        return new Stmt() {
            @Override
            IdentityHashMap<Object, Object> run(IdentityHashMap<Object, Object> environment) {
                while (test.run(environment)) {
                    environment = s.run(environment);
                }
                return environment;
            }
        };
    }
}
