package tamestaj.examples.vector.test;

import tamestaj.examples.vector.Vec;

public class VectorTagless {
    interface VecL<R> {
        R lit(Vec v);
        R plus(R v1, R v2);
        R times(R v, double s);
    }

    interface Program<R> extends VecL<R> {
        default R run() {
            Vec v1 = Vec.create(1, 2);
            Vec v2 = Vec.create(3, 4);
            Vec v3 = Vec.create(5, 6);
            return plus(plus(lit(v1), times(lit(v2), 2)), lit(v3));
        }
    }

    interface Eval extends VecL<Vec> {
        @Override
        default Vec lit(Vec v) { return v; }
        @Override
        default Vec plus(Vec v1, Vec v2) { return v1.plus(v2); }
        @Override
        default Vec times(Vec v, double s) { return v.times(s); }
    }

    public static void main(String[] args) {
        class Run implements Program<Vec>, Eval {}
        System.out.println(new Run().run());
    }
}
