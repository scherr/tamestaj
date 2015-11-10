package tamestaj.examples.mini.test;

import tamestaj.examples.mini.Mini.*;
import tamestaj.util.TickTock;

import static tamestaj.examples.mini.Mini.*;

public class MiniTest {
    public static int factorial(int x) {
        IntV a = intVar("a");
        IntV n = intVar("n");

        return  intAssign(a, intLit(1))
          .then(intAssign(n, intLit(x)))
          .then(whileDo(leq(intLit(1), n),
                  intAssign(a, mul(a, n))
            .then(intAssign(n, add(n, intLit(-1)))))
          ).intRun(a);
    }

    public static void main(String[] args) {
        TickTock.tick();
        System.out.println(factorial(2));
        System.out.println(factorial(4));
        System.out.println(factorial(6));
        TickTock.tockPrint();

        TickTock.tick();
        System.out.println(factorial(2));
        System.out.println(factorial(4));
        System.out.println(factorial(6));
        TickTock.tockPrint();
    }
}
