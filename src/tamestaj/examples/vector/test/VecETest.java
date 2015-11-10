package tamestaj.examples.vector.test;

import tamestaj.util.TickTock;
import tamestaj.examples.vector.Vec;
import tamestaj.examples.vector.VecE;

import java.util.Arrays;
import java.util.Random;

public class VecETest {
    static void sink(Vec v0, Vec v1, Vec v2) { v0.equals(v2); }
    static Vec example(int n, Vec a, Vec b, Vec c) {
        VecE aE = a.toVecE().plus(b.toVecE());
        if (n > 10) {
            aE = aE.plus(aE);
            // aE = aE.plus(aE.toVec());
        }

        Vec v = aE.toVec();
        sink(v, b, v);


        VecE cE = c.toVecE();
        for (int i = 0; i < n; i++) {
            cE = cE.times(5 + i);
        }

        // VecE cE = c.toVecE().times(5 + 1).times(5 + 2).times(5 + 3).times(5 + 4).times(5 + 5);
        return cE.toVec();
    }

    public static long run(int size) {
        Random r = new Random(4);
        Vec a = Vec.create(r.doubles(size).toArray());
        Vec b = Vec.create(r.doubles(size).toArray());
        Vec c = Vec.create(r.doubles(size).toArray());

        Vec res = null;

        TickTock.tick();
        for (int i = 0; i < 100000; i++) {
            res = example(i % 20, a, b, c);
        }
        // System.out.println(res);
        return TickTock.tock();
    }

    public static void main(String[] args) {
        run(10000);

        for (int i = 0; i < 5; i++) {
            int size = (int) Math.pow(10, i);
            System.out.println(size);
            long[] times = new long[10];
            for (int j = 0; j < 10; j++) {
                times[j] = run(size);
            }
            System.out.println(Arrays.toString(times));
        }
    }
}
