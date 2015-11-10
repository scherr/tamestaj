package tamestaj.examples.functor;

import java.util.Stack;
import java.util.function.Function;

public abstract class FunctorBinaryTree<T> extends Functor<T> {
    private FunctorBinaryTree() { }

    public static final class Leaf<T> extends FunctorBinaryTree<T> {
        private final T value;
        public Leaf(T value) { this.value = value; }

        /*
        public <B> Leaf<B> fmap(Function<T, B> function) {
            return new Leaf<>(function.apply(value));
        }
        */

        public String toString() {
            return "(Leaf " + value + ")";
        }

        @Override
        <B> void onStackMap(Stack<FunctorBinaryTree<T>> inStack, Stack<FunctorBinaryTree<B>> outStack, Function<? super T, ? extends B> function) {
            outStack.push(new Leaf<>(function.apply(value)));
        }
    }

    public static final class Branch<T> extends FunctorBinaryTree<T> {
        private final FunctorBinaryTree<T> left;
        private final FunctorBinaryTree<T> right;

        public Branch(FunctorBinaryTree<T> left, FunctorBinaryTree<T> right) {
            this.left = left;
            this.right = right;
        }

        /*
        @Suppress(languages = FunctorLanguage.class)
        public <B> Branch<B> fmap(Function<T, B> function) {
            return new Branch<>(left.fmap(function), right.fmap(function));
        }
        */

        public String toString() {
            return "(Branch " + left + " " + right + ")";
        }

        @Override
        <B> void onStackMap(Stack<FunctorBinaryTree<T>> inStack, Stack<FunctorBinaryTree<B>> outStack, Function<? super T, ? extends B> function) {
            inStack.push(MARKER);
            inStack.push(right);
            inStack.push(left);
        }
    }

    private static final FunctorBinaryTree MARKER = new FunctorBinaryTree<Object>() {
        @Override
        <B> void onStackMap(Stack<FunctorBinaryTree<Object>> inStack, Stack<FunctorBinaryTree<B>> outStack, Function<? super Object, ? extends B> function) {
            FunctorBinaryTree<B> right = outStack.pop();
            FunctorBinaryTree<B> left = outStack.pop();
            outStack.push(new Branch<>(left, right));
        }
    };

    abstract <B> void onStackMap(Stack<FunctorBinaryTree<T>> inStack, Stack<FunctorBinaryTree<B>> outStack, Function<? super T, ? extends B> function);

    @Override
    public final <B> FunctorBinaryTree<B> fmap(Function<? super T, ? extends B> function) {
        Stack<FunctorBinaryTree<T>> inStack = new Stack<>();
        Stack<FunctorBinaryTree<B>> outStack = new Stack<>();

        inStack.push(this);
        while (!inStack.isEmpty()) {
            inStack.pop().onStackMap(inStack, outStack, function);
        }

        return outStack.pop();
    }
}
