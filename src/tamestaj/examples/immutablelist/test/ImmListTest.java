package tamestaj.examples.immutablelist.test;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import tamestaj.examples.immutablelist.ImmListE;
import static tamestaj.examples.immutablelist.ImmList.*;
import tamestaj.util.TickTock;

import java.util.Random;


public class ImmListTest {
    private static ImmutableList<String> test(ImmutableList<Integer> l) {
        l = map(l, x -> x + 1);
        l = map(l, x -> x / 2);
        l = map(l, x -> x + 2);
        l = filter(l, x -> x > 3);
        l = map(l, x -> x * 4);
        l = map(l, x -> x - 4);
        l = filter(l, x -> x > 3);
        l = map(l, x -> x / 10);
        l = map(l, x -> x);
        return map(l, Object::toString);
    }

    private static ImmutableList<String> testE(ImmutableList<Integer> l) {
        return ImmListE.of(l)
                .map(x -> x + 1)
                .map(x -> x / 2)
                .map(x -> x + 2)
                .filter(x -> x > 3)
                .map(x -> x * 4)
                .map(x -> x - 4)
                .filter(x -> x > 3)
                .map(x -> x / 10)
                .map(x -> x)
                .map(Object::toString)
                .eval();
    }

    private static ImmutableList<String> testFluentIterable(ImmutableList<Integer> l) {
        return FluentIterable.from(l)
                .transform(x -> x + 1)
                .transform(x -> x / 2)
                .transform(x -> x + 2)
                .filter(x -> x > 3)
                .transform(x -> x * 4)
                .transform(x -> x - 4)
                .filter(x -> x > 3)
                .transform(x -> x / 10)
                .transform(x -> x)
                .transform(Object::toString)
                .toList();
    }

    public static void main(String[] args) throws InterruptedException {
        ImmutableList.Builder<Integer> builder = ImmutableList.builder();
        Random random = new Random(5);

        for (int i = 0; i < 100000; i++) {
            Integer n = random.nextInt() % 20;
            builder.add(n);
        }

        ImmutableList<Integer> list = builder.build();

        ImmutableList<String> res = null;
        for (int i = 0; i < 1000; i++) {
            res = testE(list);
        }

        // System.out.println(res);
        System.gc();
        Thread.sleep(2000);

        for (int j = 0; j < 10; j++) {
            TickTock.tickPrint();
            for (int i = 0; i < 1000; i++) {
                res = testE(list);
            }
            TickTock.tockPrint();
        }

        System.out.println(res);
    }
}
