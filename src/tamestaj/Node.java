package tamestaj;

import com.google.common.collect.ImmutableSet;

abstract class Node {
    Node() { }

    abstract ImmutableSet<Flow.Control> getInControl();
    abstract void setInControl(ImmutableSet<Flow.Control> inControl);

    abstract ImmutableSet<Flow.Control> getOutControl();
    abstract void setOutControl(ImmutableSet<Flow.Control> outControl);
}
