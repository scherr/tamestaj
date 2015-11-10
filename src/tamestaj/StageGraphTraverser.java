package tamestaj;

import com.google.common.collect.ImmutableSet;

import java.util.*;

@SuppressWarnings("unused")
abstract class StageGraphTraverser {
    protected enum Direction {
        FORWARD,
        BACKWARD
    }

    private final Direction direction;

    private final HashMap<Node, Integer> priorities;
    private final ImmutableSet<Node> initials;

    /*
    private class PriorityComparator implements Comparator<Node> {
        public int compare(Node o1, Node o2) {
            return Integer.compare(priorities.get(o2), priorities.get(o1));
        }

        public boolean equals(Object obj) {
            return false;
        }
    }
    */

    StageGraphTraverser(StageGraph graph, Direction direction) {
        this.direction = direction;

        priorities = new HashMap<>();
        if (direction == Direction.FORWARD) {
            initials = graph.getEntrySuccessors();
        } else {
            initials = graph.getExitPredecessors();
        }

        prepare();
    }

    private void prepare() {
        HashSet<Node> discovered = new HashSet<>();
        HashSet<Node> finished = new HashSet<>();
        Stack<Node> stack = new Stack<>();

        initials.forEach(stack::push);

        int time = 1;
        while (!stack.isEmpty()) {
            Node node = stack.peek();
            discovered.add(node);

            if (finished.contains(node)) {
                stack.pop();
                priorities.put(node, time);
                time++;
            } else {
                finished.add(node);

                switch (direction) {
                    case FORWARD:
                        for (Flow.Control control : node.getOutControl()) {
                            if (control.flowsInternally()) {
                                Node successor = control.getTo();
                                if (!discovered.contains(successor)) {
                                    stack.push(successor);
                                }
                            }
                        }
                        break;

                    case BACKWARD:
                        for (Flow.Control control : node.getInControl()) {
                            if (control.flowsInternally()) {
                                Node predecessor = control.getFrom();
                                if (!discovered.contains(predecessor)) {
                                    stack.push(predecessor);
                                }
                            }
                        }
                        break;
                }
            }
        }
    }

    void traverse() {
        // PriorityQueue<Node> queue = new PriorityQueue<>(priorities.keySet().size(), new PriorityComparator());
        PriorityQueue<Node> queue = new PriorityQueue<>(priorities.keySet().size(), (o1, o2) -> Integer.compare(priorities.get(o2), priorities.get(o1)));

        initials.forEach(queue::offer);

        while (!queue.isEmpty()) {
            Node node = queue.poll();

            // If traversal shall continue from the visited node...
            if (visitNode(node)) {
                switch (direction) {
                    case FORWARD:
                        for (Flow.Control control : node.getOutControl()) {
                            if (control.flowsInternally()) {
                                queue.add(control.getTo());
                            }
                        }
                        break;

                    case BACKWARD:
                        for (Flow.Control control : node.getInControl()) {
                            if (control.flowsInternally()) {
                                queue.add(control.getFrom());
                            }
                        }
                        break;
                }
            }
        }
    }

    abstract boolean visitNode(Node node);
}
