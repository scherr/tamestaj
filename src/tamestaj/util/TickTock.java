package tamestaj.util;

import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public final class TickTock {
    private TickTock() { }

    private static ThreadLocal<Stack<Integer>> tickIdStack = ThreadLocal.withInitial(Stack::new);
    private static ThreadLocal<Stack<String>> tickMessageStack = ThreadLocal.withInitial(Stack::new);
    private static ThreadLocal<Stack<Long>> tickStartStack = ThreadLocal.withInitial(Stack::new);
    private static AtomicInteger tickId = new AtomicInteger(0);
    public static void tick() {
        tick(null);
    }

    public static void tick(String message) {
        tickIdStack.get().push(tickId.getAndIncrement());
        tickMessageStack.get().push(message);
        tickStartStack.get().push(System.currentTimeMillis());
    }

    public static void tickPrint(String message) {
        if (message == null) {
            tickPrint();
        } else {
            System.out.println("Tick (" + tickStartStack.get().size() + ", " + tickId.get() + ", \"" + message + "\")!");
            tick(message);
        }
    }

    public static void tickPrint() {
        System.out.println("Tick (" + tickStartStack.get().size() + ", " + tickId.get() + ")!");
        tick();
    }

    public static long tock() {
        long c = System.currentTimeMillis();
        tickIdStack.get().pop();
        tickMessageStack.get().pop();
        return c - tickStartStack.get().pop();
    }

    public static void tockPrint() {
        long t = System.currentTimeMillis() - tickStartStack.get().pop();
        String message = tickMessageStack.get().pop();
        System.out.println("Tock (" + (tickStartStack.get().size()) + ", " + tickIdStack.get().pop() + (message == null ? "" : ", \"" + message + "\"") + "): " + t + " ms");
    }
}
