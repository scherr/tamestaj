package tamestaj;

import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.*;
import javassist.bytecode.analysis.ControlFlow;
import javassist.bytecode.analysis.Util;

import java.util.*;

@SuppressWarnings("unused")
abstract class Analyzer<T> implements Opcode {
    private final MethodInfo methodInfo;
    private final CodeAttribute codeAttribute;
    private final int codeLength;

    // States before and after an instruction
    private final Object[] inState;
    private final Object[] outState;

    // Control flow data
    private final int[][] predecessors;
    private final int[][] successors;
    private final Type[] caughtException;
    private final int[] priorities;

    // Priority queue of instructions (positions) to analyze
    private final PriorityQueue<Integer> queue;

    private int[] ignoreNext;

    private boolean analyzedControlFlow;
    private T initialState;

    protected final CtClass clazz;
    protected final CtBehavior behavior;

    /*
    private class PriorityComparator implements Comparator<Integer> {
        public int compare(Integer o1, Integer o2) {
            return Integer.compare(priorities[o2], priorities[o1]);
        }

        public boolean equals(Object obj) {
            return false;
        }
    }
    */

    protected Analyzer(CtBehavior behavior) {
        clazz = behavior.getDeclaringClass();
        this.behavior = behavior;

        methodInfo = behavior.getMethodInfo2();
        codeAttribute = methodInfo.getCodeAttribute();
        codeLength = codeAttribute.getCodeLength();

        inState = new Object[codeLength];
        outState = new Object[codeLength];

        predecessors = new int[codeLength][];
        successors = new int[codeLength][];
        caughtException = new Type[codeLength];
        priorities = new int[codeLength];

        // queue = new PriorityQueue<>(codeLength, new PriorityComparator());
        queue = new PriorityQueue<>(codeLength, (o1, o2) -> Integer.compare(priorities[o2], priorities[o1]));
    }

    protected Analyzer(Analyzer<?> analyzer) {
        clazz = analyzer.clazz;
        behavior = analyzer.behavior;

        methodInfo = analyzer.methodInfo;
        codeAttribute = analyzer.codeAttribute;
        codeLength = analyzer.codeLength;

        inState = new Object[codeLength];
        outState = new Object[codeLength];

        predecessors = analyzer.predecessors;
        successors = analyzer.successors;
        caughtException = analyzer.caughtException;
        priorities = analyzer.priorities;

        // queue = new PriorityQueue<>(codeLength, new PriorityComparator());
        queue = new PriorityQueue<>(codeLength, (o1, o2) -> Integer.compare(priorities[o2], priorities[o1]));

        analyzedControlFlow = analyzer.analyzedControlFlow;
    }

    private int getLastBlockPosition(ControlFlow.Block block, CodeIterator codeIterator) throws BadBytecode {
        int oldPos = codeIterator.lookAhead();
        codeIterator.move(block.position());
        int pos = block.position();
        while (codeIterator.lookAhead() < block.position() + block.length()) {
            pos = codeIterator.next();
        }
        codeIterator.move(oldPos);

        return pos;
    }

    protected static Type getType(String name, int position) throws BadBytecode {
        try {
            return Type.of(ClassPool.getDefault().get(name));
        } catch (NotFoundException e) {
            throw new BadBytecode("Could not find class [position = " + position + "]: " + name);
        }
    }

    protected void analyzeControlFlow(CodeIterator codeIterator) throws BadBytecode {
        ControlFlow.Block[] blocks = new ControlFlow(clazz, methodInfo).basicBlocks();

        HashMap<Integer, List<Integer>> catchingSuccs = new HashMap<>();
        HashMap<Integer, List<Integer>> caughtPreds = new HashMap<>();

        for (ControlFlow.Block block : blocks) {
            ControlFlow.Catcher[] catchers = block.catchers();
            if (catchers.length > 0) {
                for (ControlFlow.Catcher catcher : catchers) {
                    int catcherPos = catcher.block().position();
                    caughtException[catcherPos] = getType(catcher.type(), catcherPos);

                    // We have to consider the predecessor instructions of the first actually covered instruction, too!
                    // After all, the first instruction may not even finish properly, and its "in state" (the
                    // combination of the "out states" of its predecessors) has to be considered.
                    for (int i = 0; i < block.incomings(); i++) {
                        int pos = getLastBlockPosition(block.incoming(i), codeIterator);

                        List<Integer> csList = catchingSuccs.get(pos);
                        if (csList == null) {
                            csList = new ArrayList<>();
                            catchingSuccs.put(pos, csList);
                        }
                        csList.add(catcherPos);

                        List<Integer> cpList = caughtPreds.get(catcherPos);
                        if (cpList == null) {
                            cpList = new ArrayList<>();
                            caughtPreds.put(catcherPos, cpList);
                        }
                        cpList.add(pos);
                    }


                    codeIterator.move(block.position());
                    // I believed for exception handlers the end is actually exclusive... but that appears to be just
                    // a general pattern, i.e. the last instruction is a "goto".
                    // while (codeIterator.lookAhead() < getLastBlockPosition(block, codeIterator)) {
                    while (codeIterator.lookAhead() < block.position() + block.length()) {
                        int pos = codeIterator.next();
                        List<Integer> csList = catchingSuccs.get(pos);
                        if (csList == null) {
                            csList = new ArrayList<>();
                            catchingSuccs.put(pos, csList);
                        }
                        csList.add(catcherPos);

                        List<Integer> cpList = caughtPreds.get(catcherPos);
                        if (cpList == null) {
                            cpList = new ArrayList<>();
                            caughtPreds.put(catcherPos, cpList);
                        }
                        cpList.add(pos);
                    }
                    codeIterator.begin();
                }
            }
        }

        for (ControlFlow.Block block : blocks) {
            int pos;

            pos = getLastBlockPosition(block, codeIterator);
            List<Integer> csList = catchingSuccs.get(pos);
            if (block.exits() == 0) {
                if (csList == null) {
                    successors[pos] = new int[0];
                } else {
                    successors[pos] = new int[csList.size()];
                    int i = 0;
                    for (int succPos : csList) {
                        successors[pos][i] = succPos;
                        i++;
                    }
                }
            } else if (block.exits() >= 1) {
                int csLen = 0;
                if (csList != null) {
                    csLen = csList.size();
                }
                if (successors[pos] == null) {
                    successors[pos] = new int[block.exits() + csLen];
                }
                for (int i = 0; i < block.exits(); i++) {
                    int succPos = block.exit(i).position();
                    successors[pos][i] = succPos;
                }
                if (csList != null) {
                    int i = block.exits();
                    for (int succPos : csList) {
                        successors[pos][i] = succPos;
                        i++;
                    }
                }
            }

            pos = block.position();
            List<Integer> cpList = caughtPreds.get(pos);
            if (cpList == null) {
                if (block.incomings() == 0) {
                    predecessors[pos] = new int[0];
                } else if (block.incomings() >= 1) {
                    if (predecessors[pos] == null) {
                        predecessors[pos] = new int[block.incomings()];
                    }
                    for (int i = 0; i < block.incomings(); i++) {
                        int predPos = getLastBlockPosition(block.incoming(i), codeIterator);
                        predecessors[pos][i] = predPos;
                    }
                }
            } else {
                // With a regular Java compiler this seems to work fine, but if a catching block also has "normal"
                // (i.e. code not under exception checking) predecessors this will not suffice. A workaround could
                // simply be to add these predecessors to cpList (above).
                predecessors[pos] = new int[cpList.size()];
                int i = 0;
                for (int predPos : cpList) {
                    predecessors[pos][i] = predPos;
                    i++;
                }
            }
        }

        while (codeIterator.hasNext()) {
            int pos = codeIterator.next();
            if (codeIterator.hasNext() && successors[pos] == null) {
                List<Integer> csList = catchingSuccs.get(pos);
                if (csList == null) {
                    successors[pos] = new int[] { lookAhead(codeIterator, pos) };
                } else {
                    successors[pos] = new int[csList.size() + 1];
                    successors[pos][0] = lookAhead(codeIterator, pos);
                    int i = 1;
                    for (int succPos : csList) {
                        successors[pos][i] = succPos;
                        i++;
                    }
                }

                if (!Util.isJumpInstruction(codeIterator.byteAt(pos)) && predecessors[lookAhead(codeIterator, pos)] == null) {
                    predecessors[lookAhead(codeIterator, pos)] = new int[] { pos };
                }
            }
        }
        codeIterator.begin();

        // The following is a quick hack to calculate priorities for reverse post-order traversal!
        // This requires optimization both in data structures (a non-boxing stack and priority queue etc.)
        // as well as conceptually. Doing this on a per block (do not forget catchers!) basis and linear execution
        // therein would be much faster.
        boolean[] discovered = new boolean[codeLength];
        boolean[] finished = new boolean[codeLength];
        Stack<Integer> stack = new Stack<>();
        stack.add(0);

        int time = 1;
        while (!stack.isEmpty()) {
            Integer pos = stack.peek();
            discovered[pos] = true;

            if (finished[pos]) {
                stack.pop();
                priorities[pos] = time;
                time++;
            } else {
                finished[pos] = true;
                for (int s = successors[pos].length - 1; s >= 0; s--) {
                    if (!discovered[successors[pos][s]]) {
                        stack.push(successors[pos][s]);
                        finished[pos] = false;
                    }
                }
            }
        }
    }

    void analyze() throws BadBytecode {
        if (codeAttribute == null) {
            // Throw exception?
            return;
        }

        CodeIterator codeIterator = codeAttribute.iterator();

        if (!analyzedControlFlow) {
            analyzeControlFlow(codeIterator);
        }

        initialState = initialState();

        /*
        for (int i = 0; i < successors.length; i++) {
            System.out.println(Arrays.toString(predecessors[i]) + " - " + i + " - " + Arrays.toString(successors[i]) + (caughtException[i] != null ? " {" + caughtException[i] + "}" : ""));
        }
        */

        Arrays.fill(inState, null);
        Arrays.fill(outState, null);

        queue.clear();
        queue.offer(codeIterator.next());
        while (!queue.isEmpty()) {
            analyzeInstruction(codeIterator);
        }
    }

    protected abstract T copyState(T original);
    protected abstract boolean stateEquals(T state, T otherState);

    protected abstract T initialState();

    private ArrayList<T> statesToArrayList(T... states) {
        ArrayList<T> list = new ArrayList<>(states.length);
        for (T state : states) {
            list.add(state);
        }
        return list;
    }

    protected void handleExit(T state, int after) { }

    protected T mergeStatesOnCatch(T state, int origin, int at, Type caughtException) { return mergeStatesOnCatch(statesToArrayList(state), new int[]{origin}, at, caughtException); }
    protected T mergeStates(T state, int origin, int at) { return mergeStates(statesToArrayList(state), new
            int[]{origin}, at); }
    protected T mergeStatesOnCatch(T state0, T state1, int origin0, int origin1, int at, Type caughtException) { return mergeStatesOnCatch(statesToArrayList(state0, state1), new int[]{origin0, origin1}, at, caughtException); }
    protected T mergeStates(T state0, T state1, int origin0, int origin1, int at) { return mergeStates
            (statesToArrayList(state0, state1), new int[]{origin0, origin1}, at); }
    protected abstract T mergeStatesOnCatch(ArrayList<T> states, int[] origins, int at, Type caughtException);
    protected abstract T mergeStates(ArrayList<T> states, int[] origins, int at);

    protected abstract T transfer(T inState, int at) throws BadBytecode;

    T getInState(int position) {
        return (T) inState[position];
    }

    T getOutState(int position) {
        return (T) outState[position];
    }

    protected void analyzeInstruction(CodeIterator codeIterator) throws BadBytecode {
        int pos = queue.poll();
        codeIterator.move(pos);
        codeIterator.next();

        Type caught = this.caughtException[pos];
        Object state;
        if (predecessors[pos].length == 0) {
            // state = initialState;
            state = mergeStates(initialState, -1, pos);
        } else if (predecessors[pos].length == 1) {
            if (outState[predecessors[pos][0]] == null) {
                // state = initialState;
                state = mergeStates(initialState, -1, pos);
            } else {
                int pred = predecessors[pos][0];
                if (caught == null) {
                    if (pos == 0) {
                        state = mergeStates(initialState, (T) outState[pred], -1, pred, pos);
                    } else {
                        state = mergeStates((T) outState[pred], pred, pos);
                    }
                } else {
                    if (pos == 0) {
                        state = mergeStatesOnCatch(initialState, (T) outState[pred], -1, pred, pos, caught);
                    } else {
                        state = mergeStatesOnCatch((T) outState[pred], pred, pos, caught);
                    }
                }
            }
        } else {
            int[] preds = predecessors[pos];

            T state0 = pos == 0 ? initialState : null;
            T state1 = null;
            int i = 0;
            int state0p = 0;
            int state1p = 0;
            for (int p = 0; p < preds.length; p++) {
                if (outState[preds[p]] != null) {
                    if (state0 == null) {
                        state0 = (T) outState[preds[p]];
                        state0p = p;
                    } else if (state1 == null) {
                        state1 = (T) outState[preds[p]];
                        state1p = p;
                    }
                    i++;
                }
            }

            if (i == 0) {
                // state = initialState;
                state = mergeStates(initialState, -1, pos);
            } else if (i == 1) {
                if (caught == null) {
                    state = mergeStates(state0, preds[state0p], pos);
                } else {
                    state = mergeStatesOnCatch(state0, preds[state0p], pos, caught);
                }
            } else if (i == 2) {
                if (caught == null) {
                    state = mergeStates(state0, state1, preds[state0p], preds[state1p], pos);
                } else {
                    state = mergeStatesOnCatch(state0, state1, preds[state0p], preds[state1p], pos, caught);
                }
            } else {
                ArrayList<T> states = new ArrayList<>(i);
                states.add(state0);
                states.add(state1);
                int[] origins = new int[i];
                origins[0] = preds[state0p];
                origins[1] = preds[state1p];
                i = 2;
                for (int p = state1p + 1; p < preds.length; p++) {
                    if (outState[preds[p]] != null) {
                        states.add((T) outState[preds[p]]);
                        origins[i] = preds[p];
                        i++;
                    }
                }

                if (caught == null) {
                    state = mergeStates(states, origins, pos);
                } else {
                    state = mergeStatesOnCatch(states, origins, pos, caught);
                }
            }
        }
        if (state == null) {
            throw new BadBytecode("No previous state where expected! [position = " + pos + "]");
        }

        // Do nothing if the previous (in state) and merged state has not changed
        if (state.equals(inState[pos])) {
            return;
        }

        inState[pos] = state;

        try {
            state = transfer((T) state, pos);
        } catch (RuntimeException e) {
            throw new BadBytecode(e.getMessage() + "[position = " + pos + "]", e);
        }

        // Not necessary since we do not even transfer if the in state is unchanged (after merging)
        /*
        boolean changed;
        if (outState[pos] == null) {
            changed = true;
        } else {
            changed = !stateEquals((T) state, (T) outState[pos]);
        }
        */

        outState[pos] = state;

        // if (changed) {
        if (ignoreNext == null) {
            if (successors[pos] != null && successors[pos].length > 0) {
                for (int succPos : successors[pos]) {
                    queue.offer(succPos);
                }
            } else {
                handleExit((T) state, pos);
            }
        } else {
            if (successors[pos] != null && successors[pos].length > 0) {
                for (int succPos : successors[pos]) {
                    if (Arrays.binarySearch(ignoreNext, succPos) < 0) {
                        queue.offer(succPos);
                    }
                }
            } else {
                handleExit((T) state, pos);
            }

            ignoreNext = null;
        }
        // }
    }

    protected void forceVisit(int position) {
        queue.offer(position);
    }

    protected void ignoreSuccessors(int... positions) {
        if (positions == null || positions.length == 0) {
            ignoreNext = null;
        } else {
            positions = Arrays.copyOf(positions, positions.length);
            Arrays.sort(positions);
            ignoreNext = positions;
        }
    }

    private int lookAhead(CodeIterator codeIterator, int position) throws BadBytecode {
        if (!codeIterator.hasNext()) {
            throw new BadBytecode("Execution falls off end! [position = " + position + "]");
        }

        return codeIterator.lookAhead();
    }
}
