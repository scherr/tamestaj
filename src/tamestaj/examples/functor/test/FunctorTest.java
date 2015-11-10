package tamestaj.examples.functor.test;

import tamestaj.annotations.Suppress;
import tamestaj.examples.functor.*;
import tamestaj.util.TickTock;

import java.util.ArrayList;
import java.util.Random;

// @Suppress(languages = FunctorL.class)
public class FunctorTest {
    private static Functor<String> test(Functor<Integer> functor) {
        return functor
                .fmap(x -> x + 1)
                .fmap(x -> x / 2)
                .fmap(x -> x + 2)
                .fmap(x -> x + 2)
                .fmap(x -> x + 2)
                .fmap(x -> x + 2)
                .fmap(x -> x + 2)
                .fmap(x -> x + 2)
                .fmap(x -> x)
                .fmap(x -> x)
                .fmap(Object::toString);
    }

    private static Functor<String> testE(Functor<Integer> functor) {
        return FunctorE.of(functor)
                .fmap(x -> x + 1)
                .fmap(x -> x / 2)
                .fmap(x -> x + 2)
                .fmap(x -> x + 2)
                .fmap(x -> x + 2)
                .fmap(x -> x + 2)
                .fmap(x -> x + 2)
                .fmap(x -> x + 2)
                .fmap(x -> x)
                .fmap(x -> x)
                .fmap(Object::toString).toFunctor();
    }

    private static Functor<Integer> makeTree() {
        Random random = new Random(42);
        FunctorBinaryTree<Integer> treeL = new FunctorBinaryTree.Leaf<>(4);
        for (int i = 0; i < 10000; i++) {
            Integer n = random.nextInt() % 20;
            treeL = new FunctorBinaryTree.Branch<>(new FunctorBinaryTree.Leaf<>(n), treeL);
        }

        FunctorBinaryTree<Integer> treeR = new FunctorBinaryTree.Leaf<>(9);
        for (int i = 0; i < 10000; i++) {
            Integer n = random.nextInt() % 20;
            treeR = new FunctorBinaryTree.Branch<>(new FunctorBinaryTree.Leaf<>(n), treeR);
        }

        return new FunctorBinaryTree.Branch<>(treeL, treeR);
    }

    private static Functor<Integer> makeList() {
        Random random = new Random(42);
        ArrayList<Integer> l = new ArrayList<>();

        for (int i = 0; i < 100000; i++) {
            Integer n = random.nextInt() % 20;
            l.add(n);
        }

        return new FunctorList<>(l);
    }

    public static void main(String[] args) {
        Functor<Integer> functor = makeList();
        // Functor<Integer> functor = makeTree();

        Functor<String> res = null;
        for (int i = 0; i < 1000; i++) {
            res = test(functor);
        }

        System.out.println(res.equals(null));

        TickTock.tickPrint();
        for (int i = 0; i < 1000; i++) {
            res = test(functor);
        }
        TickTock.tockPrint();

        System.out.println(res.equals(null));
    }
}
