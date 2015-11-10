package tamestaj;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import javassist.*;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Descriptor;
import javassist.bytecode.MethodInfo;

import java.util.*;

@SuppressWarnings("unused")
final class StageAnalyzer extends HighLevelAnalyzerWithBoxingUnboxing<Object, Object, StageAnalyzer.StageFrame> {
    // Internal sentinel values!
    private final static Node ENTRY_MARKER = new Node() {
        ImmutableSet<Flow.Control> getInControl()                 { throw new UnsupportedOperationException(); }
        void setInControl(ImmutableSet<Flow.Control> inControl)   { throw new UnsupportedOperationException(); }
        ImmutableSet<Flow.Control> getOutControl()                { throw new UnsupportedOperationException(); }
        void setOutControl(ImmutableSet<Flow.Control> outControl) { throw new UnsupportedOperationException(); }
    };
    private final static Node EXIT_MARKER = new Node() {
        ImmutableSet<Flow.Control> getInControl()                 { throw new UnsupportedOperationException(); }
        void setInControl(ImmutableSet<Flow.Control> inControl)   { throw new UnsupportedOperationException(); }
        ImmutableSet<Flow.Control> getOutControl()                { throw new UnsupportedOperationException(); }
        void setOutControl(ImmutableSet<Flow.Control> outControl) { throw new UnsupportedOperationException(); }
    };

    private final TypeAnalyzer.Result typeAnalyzerResult;
    private final ValueFlowAnalyzer.Result valueFlowResult;
    private final Type returnType;
    private final StageFrame initialState;
    private final HashSet<Source> arguments;
    private final HashMap<Index, Node> allNodes;
    private final HashSet<Node> nodesOfInterest;
    private final HashMap<Node, HashSet<Flow.Control>> inControlsMap;
    private final HashMap<Node, HashSet<Flow.Control>> outControlsMap;
    private final SuppressAnnotation suppressAnn;

    private boolean initialPass;

    static class StageFrame implements tamestaj.Frame<Object, Object> {
        private ImmutableSet<Node> predecessors;

        private StageFrame(ImmutableSet<Node> predecessors) {
            this.predecessors = predecessors;
        }

        // This essentially do nothing or return nulls...
        public Object peek() { return null; }
        public Object pop() { return null; }
        public void push(Object value) { }
        public void setStack(int offset, Object value) { }
        public Object getStack(int offset) { return null; }
        public void setLocal(int index, Object value) { }
        public Object getLocal(int index) { return null; }
        public void clearStack() { }
        public Object pop2() { return null; }
        public void push2(Object value) { }
        public void setLocal2(int index, Object value) { }
        public Object getLocal2(int index) { return null; }

        private ImmutableSet<Node> getPredecessors() {
            return predecessors;
        }
        private void setPredecessors(ImmutableSet<Node> predecessors) {
            this.predecessors = predecessors;
        }

        private StageFrame copy() {
            return new StageFrame(predecessors);
        }

        private void merge(StageFrame withFrame) {
            if (this.equals(withFrame)) { return; }

            predecessors = ImmutableSet.<Node>builder().addAll(predecessors).addAll(withFrame.predecessors).build();
        }

        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (!(obj instanceof StageFrame)) { return false; }

            return predecessors.equals(((StageFrame) obj).predecessors);
        }
    }

    class Result {
        private final ImmutableSet<Node> entrySuccessors;
        private final ImmutableSet<Node> exitPredecessors;
        private final ImmutableMap<SourceIndex, Source.Staged> sourceIndexToStaged;
        private final ImmutableMap<SourceIndex, Source> sourceIndexToSource;
        private final ImmutableMap<UseIndex, Use> useIndexToUse;

        private Result() {
            HashMap<SourceIndex, HashMap<UseIndex, Flow.Data>> outDataMap = new HashMap<>();

            ImmutableSet.Builder<Node> entrySuccessorsBuilder = ImmutableSet.builder();
            ImmutableSet.Builder<Node> exitPredecessorsBuilder = ImmutableSet.builder();

            ImmutableMap.Builder<SourceIndex, Source.Staged> sourceIndexToStagedBuilder = ImmutableMap.builder();
            ImmutableMap.Builder<SourceIndex, Source> sourceIndexToSourceBuilder = ImmutableMap.builder();
            ImmutableMap.Builder<UseIndex, Use> useIndexToUseBuilder = ImmutableMap.builder();

            // Fixing up control-flow information and basic indexing
            for (Node node : nodesOfInterest) {
                if (!(node instanceof Use.Argument)) {
                    HashSet<Flow.Control> inC = inControlsMap.get(node);
                    node.setInControl(ImmutableSet.copyOf(inC));

                    for (Flow.Control c : inC) {
                        if (c.flowsFromEntry()) {
                            entrySuccessorsBuilder.add(node);
                            break;
                        }
                    }

                    HashSet<Flow.Control> outC = outControlsMap.get(node);
                    node.setOutControl(ImmutableSet.copyOf(outC));

                    for (Flow.Control c : outC) {
                        if (c.flowsToExit()) {
                            exitPredecessorsBuilder.add(node);
                            break;
                        }
                    }
                }

                if (node instanceof Source) {
                    Source source = (Source) node;
                    sourceIndexToSourceBuilder.put(source.getSourceIndex(), source);

                    if (source instanceof Source.Staged) {
                        Source.Staged staged = (Source.Staged) source;
                        sourceIndexToStagedBuilder.put(staged.getSourceIndex(), staged);
                    }

                    outDataMap.put(source.getSourceIndex(), new HashMap<>());
                } else if (node instanceof Use) {
                    Use use = (Use) node;
                    useIndexToUseBuilder.put(use.getUseIndex(), use);
                }
            }

            entrySuccessors = entrySuccessorsBuilder.build();
            exitPredecessors = exitPredecessorsBuilder.build();

            sourceIndexToStaged = sourceIndexToStagedBuilder.build();
            sourceIndexToSource = sourceIndexToSourceBuilder.build();
            useIndexToUse = useIndexToUseBuilder.build();


            // Fixing up data-flow information in two passes using indirection over use and source indices

            // Pass 1:
            //   - Every use is provided its incoming data-flow information
            //   - Register data-flow information to be considered later (as outgoing) on sources
            for (Map.Entry<UseIndex, Use> entry : useIndexToUse.entrySet()) {
                UseIndex useIndex = entry.getKey();
                Use use = entry.getValue();

                Set<SourceIndex> sourceIndices = valueFlowResult.getSources(useIndex);
                ImmutableSet.Builder<Flow.Data> inDataBuilder = ImmutableSet.builder();
                for (SourceIndex sourceIndex : sourceIndices) {
                    Source source = sourceIndexToSource.get(sourceIndex);
                    if (source == null) {
                        // This means it is not a node of interest!
                        continue;
                    }

                    Flow.Data d = new Flow.Data(source, use);
                    inDataBuilder.add(d);

                    outDataMap.get(sourceIndex).put(useIndex, d);
                }

                use.setInData(inDataBuilder.build());
            }

            // Pass 2:
            //   - Every source is provided its outgoing data-flow information
            //   - Since we have at this point already considered all (potentially non-staged) uses, we must have
            //     created and stored the relevant data-flow information already (as incoming data flow)
            for (Map.Entry<SourceIndex, Source> entry : sourceIndexToSource.entrySet()) {
                SourceIndex sourceIndex = entry.getKey();
                Source source = entry.getValue();

                Set<UseIndex> useIndices = valueFlowResult.getUses(sourceIndex);
                ImmutableSet.Builder<Flow.Data> outDataBuilder = ImmutableSet.builder();
                for (UseIndex useIndex : useIndices) {
                    Use use = useIndexToUse.get(useIndex);
                    if (use == null) {
                        // This means it is not a node of interest!
                        continue;
                    }

                    Flow.Data data = outDataMap.get(sourceIndex).get(useIndex);
                    outDataBuilder.add(data);
                }

                source.setOutData(outDataBuilder.build());
            }
        }

        StageGraph getStageGraph() {
            return new StageGraph(entrySuccessors, exitPredecessors, sourceIndexToStaged, sourceIndexToSource, useIndexToUse);
        }
    }

    StageAnalyzer(TypeAnalyzer typeAnalyzer, ValueFlowAnalyzer.Result valueFlowResult) {
        super(typeAnalyzer);

        this.typeAnalyzerResult = typeAnalyzer.getResult();
        this.valueFlowResult = valueFlowResult;

        if (behavior instanceof CtMethod) {
            try {
                returnType = Type.of(((CtMethod) behavior).getReturnType());
            } catch (NotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            returnType = Type.VOID;
        }

        initialState = new StageFrame(ImmutableSet.of(ENTRY_MARKER));

        arguments = new HashSet<>();
        allNodes = new HashMap<>();
        nodesOfInterest = new HashSet<>();
        inControlsMap = new HashMap<>();
        outControlsMap = new HashMap<>();

        suppressAnn = SuppressAnnotation.forBehavior(behavior);


        MethodInfo methodInfo = behavior.getMethodInfo2();

        int maxLocals = codeAttribute.getMaxLocals();
        int maxStack = codeAttribute.getMaxStack();
        int codeLength = codeAttribute.getCodeLength();

        int pos = 0;
        if (!Modifier.isStatic(behavior.getModifiers())) {
            Source.Opaque source = new Source.Opaque(SourceIndex.makeImplicitLocal(0, pos), Type.of(clazz));
            allNodes.put(source.getSourceIndex(), source);
            arguments.add(source);
            pos++;
        }

        CtClass[] parameters;
        try {
            parameters = Descriptor.getParameterTypes(methodInfo.getDescriptor(), clazz.getClassPool());
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }

        for (CtClass parameter : parameters) {
            Type type = Type.of(parameter);
            if (type.isTwoWordPrimitive()) {
                Source.Opaque source = new Source.Opaque(SourceIndex.makeImplicitLocal(0, pos), type);
                allNodes.put(source.getSourceIndex(), source);
                arguments.add(source);
                pos += 2;
            } else {
                Source.Opaque source = new Source.Opaque(SourceIndex.makeImplicitLocal(0, pos), type);
                allNodes.put(source.getSourceIndex(), source);
                arguments.add(source);
                pos++;
            }
        }
    }

    Result getResult() {
        return new Result();
    }

    void analyze() throws BadBytecode {
        // Pass 1: We first traverse the program to create and index all the potential graph nodes
        initialPass = true;

        super.analyze();

        initialPass = false;

        // Pass 2: We find all nodes of interest
        for (Node node : allNodes.values()) {
            if (node instanceof Source.Staged) {
                Source.Staged staged = (Source.Staged) node;

                nodesOfInterest.add(staged);

                for (Use.Argument arg : staged.getArguments()) {
                    nodesOfInterest.add(arg);

                    for (SourceIndex sourceIndex : valueFlowResult.getSources(arg.getUseIndex())) {
                        nodesOfInterest.add(allNodes.get(sourceIndex));
                    }
                }

                for (UseIndex useIndex : valueFlowResult.getUses(staged.getSourceIndex())) {
                    nodesOfInterest.add(allNodes.get(useIndex));
                }
            }
        }
        for (Node node : allNodes.values()) {
            if (node instanceof Use.Opaque) {
                UseIndex useIndex = ((Use.Opaque) node).getUseIndex();

                for (SourceIndex sourceIndex : valueFlowResult.getSources(useIndex)) {
                    Node n = allNodes.get(sourceIndex);
                    if (nodesOfInterest.contains(n)) {
                        nodesOfInterest.add(allNodes.get(sourceIndex));
                    }
                }
            }
        }

        // Pass 3: We traverse the program again, now registering control flow
        nodesOfInterest.add(ENTRY_MARKER);
        nodesOfInterest.add(EXIT_MARKER);

        for (Source arg : arguments) {
            if (nodesOfInterest.contains(arg)) {
                registerControl(initialState, arg.getSourceIndex());
            }
        }

        super.analyze();

        nodesOfInterest.remove(ENTRY_MARKER);
        nodesOfInterest.remove(EXIT_MARKER);
    }

    private void registerControlToExit(StageFrame frame) {
        if (!(frame.getPredecessors().size() == 1 && frame.getPredecessors().contains(ENTRY_MARKER))) {
            for (Node from : frame.getPredecessors()) {
                Flow.Control control = new Flow.Control(from, null);

                HashSet<Flow.Control> outControls = outControlsMap.get(from);
                if (outControls == null) {
                    outControls = new HashSet<>();
                    outControlsMap.put(from, outControls);
                }
                outControls.add(control);
            }
        }
    }
    private void registerControl(StageFrame frame, Index toIndex) {
        Node to = allNodes.get(toIndex);
        if (to instanceof Use.Argument) { return; }
        if (!nodesOfInterest.contains(to)) { return; }

        for (Node from : frame.getPredecessors()) {
            Flow.Control control;
            if (from == ENTRY_MARKER) {
                control = new Flow.Control(null, to);
            } else {
                control = new Flow.Control(from, to);
            }

            HashSet<Flow.Control> inControls = inControlsMap.get(to);
            if (inControls == null) {
                inControls = new HashSet<>();
                inControlsMap.put(to, inControls);
            }
            inControls.add(control);

            if (from != ENTRY_MARKER) {
                HashSet<Flow.Control> outControls = outControlsMap.get(from);
                if (outControls == null) {
                    outControls = new HashSet<>();
                    outControlsMap.put(from, outControls);
                }
                outControls.add(control);
            }
        }

        frame.setPredecessors(ImmutableSet.of(to));
    }

    protected StageFrame copyState(StageFrame original) {
        return original.copy();
    }

    protected boolean stateEquals(StageFrame state, StageFrame otherState) {
        return state.equals(otherState);
    }

    protected StageFrame initialState() {
        return initialState;
    }

    protected StageFrame mergeStatesOnCatch(ArrayList<StageFrame> states, int[] origins, int at, Type caughtException) {
        return mergeStates(states, origins, at);
    }

    protected StageFrame mergeStates(ArrayList<StageFrame> states, int[] origins, int at) {
        StageFrame mergedFrame = states.get(0).copy();

        for (int i = 1; i < states.size(); i++) {
            mergedFrame.merge(states.get(i));
        }

        return mergedFrame;
    }

    protected Object createNull(StageFrame state, int at) throws BadBytecode {
        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        if (initialPass) {
            allNodes.computeIfAbsent(sourceIndex, (k) -> new Source.Constant.Null(sourceIndex));
        } else {
            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected Object createIntegerConstant(StageFrame state, int at, int value) throws BadBytecode {
        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        if (initialPass) {
            allNodes.computeIfAbsent(sourceIndex, (k) -> new Source.Constant.Integer(sourceIndex, value));
        } else {
            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected Object createLongConstant(StageFrame state, int at, long value) throws BadBytecode {
        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        if (initialPass) {
            allNodes.computeIfAbsent(sourceIndex, (k) -> new Source.Constant.Long(sourceIndex, value));
        } else {
            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected Object createFloatConstant(StageFrame state, int at, float value) throws BadBytecode {
        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        if (initialPass) {
            allNodes.computeIfAbsent(sourceIndex, (k) -> new Source.Constant.Float(sourceIndex, value));
        } else {
            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected Object createDoubleConstant(StageFrame state, int at, double value) throws BadBytecode {
        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        if (initialPass) {
            allNodes.computeIfAbsent(sourceIndex, (k) -> new Source.Constant.Double(sourceIndex, value));
        } else {
            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected Object createByteConstant(StageFrame state, int at, byte value) throws BadBytecode {
        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        if (initialPass) {
            allNodes.computeIfAbsent(sourceIndex, (k) -> new Source.Constant.Byte(sourceIndex, value));
        } else {
            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected Object createShortConstant(StageFrame state, int at, short value) throws BadBytecode {
        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        if (initialPass) {
            allNodes.computeIfAbsent(sourceIndex, (k) -> new Source.Constant.Short(sourceIndex, value));
        } else {
            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected Object createStringConstant(StageFrame state, int at, String value) throws BadBytecode {
        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        if (initialPass) {
            allNodes.computeIfAbsent(sourceIndex, (k) -> new Source.Constant.String(sourceIndex, value));
        } else {
            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected Object createClassConstant(StageFrame state, int at, CtClass value) throws BadBytecode {
        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        if (initialPass) {
            allNodes.computeIfAbsent(sourceIndex, (k) -> new Source.Constant.Class(sourceIndex, value.getName()));
        } else {
            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected Object readLocal(StageFrame state, int at, Type type, int index, Object local) throws BadBytecode {
        return null;
    }

    protected Object readArray(StageFrame state, int at, Type componentType, Object array, int arrayOffset, Object index, int indexOffset) throws BadBytecode {
        UseIndex arrayIndex = UseIndex.makeStack(at, arrayOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(arrayIndex, (k) -> {
                try {
                    return new Use.Opaque(arrayIndex, Type.of(classPool.get(componentType.getCtClass().getName() + "[]")));
                } catch (NotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            registerControl(state, arrayIndex);
        }

        UseIndex indexIndex = UseIndex.makeStack(at, indexOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(indexIndex, (k) -> new Use.Opaque(indexIndex, Type.INT));
        } else {
            registerControl(state, indexIndex);
        }

        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        if (initialPass) {
            allNodes.computeIfAbsent(sourceIndex, (k) -> new Source.Opaque(sourceIndex, componentType));
        } else {
            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected Object assignLocal(StageFrame state, int at, Type type, int index, Object value, int valueOffset) throws BadBytecode {
        return null;
    }

    protected void assignArray(StageFrame state, int at, Type componentType, Object array, int arrayOffset, Object index, int indexOffset, Object value, int valueOffset) throws BadBytecode {
        UseIndex arrayIndex = UseIndex.makeStack(at, arrayOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(arrayIndex, (k) -> {
                try {
                    return new Use.Opaque(arrayIndex, Type.of(classPool.get(componentType.getCtClass().getName() + "[]")));
                } catch (NotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            registerControl(state, arrayIndex);
        }

        UseIndex indexIndex = UseIndex.makeStack(at, indexOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(indexIndex, (k) -> new Use.Opaque(indexIndex, Type.INT));
        } else {
            registerControl(state, arrayIndex);
        }

        UseIndex valueIndex = UseIndex.makeStack(at, valueOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(valueIndex, (k) -> new Use.Opaque(valueIndex, componentType));
        } else {
            registerControl(state, valueIndex);
        }
    }

    protected Object performBinaryArithmetic(StageFrame state, int at, Type type, ArithmeticOperation operation, Object left, int leftOffset, Object right, int rightOffset) throws BadBytecode {
        UseIndex leftIndex = UseIndex.makeStack(at, leftOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(leftIndex, (k) -> new Use.Opaque(leftIndex, type));
        } else {
            registerControl(state, leftIndex);
        }

        UseIndex rightIndex = UseIndex.makeStack(at, rightOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(rightIndex, (k) -> new Use.Opaque(rightIndex, type));
        } else {
            registerControl(state, rightIndex);
        }

        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        if (initialPass && !allNodes.containsKey(sourceIndex)) {
            allNodes.put(sourceIndex, new Source.Opaque(sourceIndex, type));
        } else {
            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected Object performShift(StageFrame state, int at, Type type, ShiftOperation operation, Object left, int leftOffset, Object right, int rightOffset) throws BadBytecode {
        UseIndex leftIndex = UseIndex.makeStack(at, leftOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(leftIndex, (k) -> new Use.Opaque(leftIndex, type));
        } else {
            registerControl(state, leftIndex);
        }

        UseIndex rightIndex = UseIndex.makeStack(at, rightOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(rightIndex, (k) -> new Use.Opaque(rightIndex, type));
        } else {
            registerControl(state, rightIndex);
        }

        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        if (initialPass) {
            allNodes.computeIfAbsent(sourceIndex, (k) -> new Source.Opaque(sourceIndex, type));
        } else {
            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected Object performNegation(StageFrame state, int at, Type type, Object value, int valueOffset) throws BadBytecode {
        UseIndex valueIndex = UseIndex.makeStack(at, valueOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(valueIndex, (k) -> new Use.Opaque(valueIndex, type));
        } else {
            registerControl(state, valueIndex);
        }

        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        if (initialPass) {
            allNodes.computeIfAbsent(sourceIndex, (k) -> new Source.Opaque(sourceIndex, type));
        } else {
            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected Object incrementLocal(StageFrame state, int at, int index, Object local, int increment) throws BadBytecode {
        UseIndex localIndex = UseIndex.makeLocal(at, index);
        if (initialPass) {
            allNodes.computeIfAbsent(localIndex, (k) -> new Use.Opaque(localIndex, Type.INT));
        } else {
            registerControl(state, localIndex);
        }

        SourceIndex sourceIndex = SourceIndex.makeExplicitLocal(at, index);
        if (initialPass) {
            allNodes.computeIfAbsent(sourceIndex, (k) -> new Source.Opaque(sourceIndex, Type.INT));
        } else {
            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected Object convertType(StageFrame state, int at, Type from, Type to, Object value, int valueOffset) throws BadBytecode {
        Type inferredFrom = typeAnalyzerResult.getStack(at, valueOffset);
        if (!Util.isSafeConversion(behavior, inferredFrom, to)) {
            UseIndex valueIndex = UseIndex.makeStack(at, valueOffset);
            if (initialPass) {
                allNodes.computeIfAbsent(valueIndex, (k) -> new Use.Opaque(valueIndex, to));
            } else {
                registerControl(state, valueIndex);
            }

            SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
            if (initialPass) {
                allNodes.computeIfAbsent(sourceIndex, (k) -> new Source.Opaque(sourceIndex, to));
            } else {
                registerControl(state, sourceIndex);
            }
        }

        return null;
    }

    protected Object compare(StageFrame state, int at, Type type, Object left, int leftOffset, Object right, int rightOffset) throws BadBytecode {
        UseIndex leftIndex = UseIndex.makeStack(at, leftOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(leftIndex, (k) -> new Use.Opaque(leftIndex, type));
        } else {
            registerControl(state, leftIndex);
        }

        UseIndex rightIndex = UseIndex.makeStack(at, rightOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(rightIndex, (k) -> new Use.Opaque(rightIndex, type));
        } else {
            registerControl(state, rightIndex);
        }

        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        if (initialPass) {
            allNodes.computeIfAbsent(sourceIndex, (k) -> new Source.Opaque(sourceIndex, type));
        } else {
            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected Object compare(StageFrame state, int at, Type type, ComparisonOperation operation, Object left, int leftOffset, Object right, int rightOffset) throws BadBytecode {
        UseIndex leftIndex = UseIndex.makeStack(at, leftOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(leftIndex, (k) -> new Use.Opaque(leftIndex, type));
        } else {
            registerControl(state, leftIndex);
        }

        UseIndex rightIndex = UseIndex.makeStack(at, rightOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(rightIndex, (k) -> new Use.Opaque(rightIndex, type));
        } else {
            registerControl(state, rightIndex);
        }

        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        if (initialPass) {
            allNodes.computeIfAbsent(sourceIndex, (k) -> new Source.Opaque(sourceIndex, type));
        } else {
            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected void branchIf(StageFrame state, int at, Type type, ComparisonOperation operation, Object value, int valueOffset, int trueTarget, int falseTarget) throws BadBytecode {
        UseIndex valueIndex = UseIndex.makeStack(at, valueOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(valueIndex, (k) -> new Use.Opaque(valueIndex, type));
        } else {
            registerControl(state, valueIndex);
        }
    }

    protected void branchIfCompare(StageFrame state, int at, Type type, ComparisonOperation operation, Object left, int leftOffset, Object right, int rightOffset, int trueTarget, int falseTarget) throws BadBytecode {
        UseIndex leftIndex = UseIndex.makeStack(at, leftOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(leftIndex, (k) -> new Use.Opaque(leftIndex, type));
        } else {
            registerControl(state, leftIndex);
        }

        UseIndex rightIndex = UseIndex.makeStack(at, rightOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(rightIndex, (k) -> new Use.Opaque(rightIndex, type));
        } else {
            registerControl(state, rightIndex);
        }
    }

    protected void branchGoto(StageFrame state, int at, int target) throws BadBytecode {

    }

    protected Object callSubroutine(StageFrame state, int at, int target) throws BadBytecode {
        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        if (initialPass) {
            allNodes.computeIfAbsent(sourceIndex, (k) -> new Source.Opaque(sourceIndex, Type.RETURN_ADDRESS));
        } else {
            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected void returnFromSubroutine(StageFrame state, int at, int index, Object local) throws BadBytecode {
        UseIndex localIndex = UseIndex.makeLocal(at, index);
        if (initialPass) {
            allNodes.computeIfAbsent(localIndex, (k) -> new Use.Opaque(localIndex, Type.RETURN_ADDRESS));
        } else {
            registerControl(state, localIndex);
        }
    }

    protected void branchTableSwitch(StageFrame state, int at, Object index, int indexOffset, int defaultTarget, int[]
            indexedTargets) throws BadBytecode {
        UseIndex valueIndex = UseIndex.makeStack(at, indexOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(valueIndex, (k) -> new Use.Opaque(valueIndex, Type.INT));
        } else {
            registerControl(state, valueIndex);
        }
    }

    protected void branchLookupSwitch(StageFrame state, int at, Object key, int keyOffset, int defaultTarget, int[] matches, int[] matchTargets) throws BadBytecode {
        UseIndex valueIndex = UseIndex.makeStack(at, keyOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(valueIndex, (k) -> new Use.Opaque(valueIndex, Type.INT));
        } else {
            registerControl(state, valueIndex);
        }
    }

    protected void returnFromMethod(StageFrame state, int at, Type type, Object value, int valueOffset) throws BadBytecode {
        UseIndex valueIndex = UseIndex.makeStack(at, valueOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(valueIndex, (k) -> new Use.Opaque(valueIndex, returnType));
        } else {
            registerControl(state, valueIndex);
        }
    }

    protected void handleExit(StageFrame state, int after) {
        registerControlToExit(state);
    }
    protected void returnFromMethod(StageFrame state, int at) throws BadBytecode {
    }

    protected Object readStaticField(StageFrame state, int at, Type classType, Type fieldType, CtField field) throws BadBytecode {
        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        if (initialPass && !allNodes.containsKey(sourceIndex)) {
            StageAnnotation ann = StageAnnotation.forField(field);

            if (ann != null && (suppressAnn == null || !suppressAnn.isSurpressed(ann.getLanguage()))) {
                allNodes.put(sourceIndex, new Source.Staged.FieldRead(sourceIndex, fieldType, ann.getLanguage(), ann.isStrict(), ann.getStaticInfoElements(), ImmutableList.of(), field));
            } else {
                allNodes.put(sourceIndex, new Source.Opaque(sourceIndex, fieldType));
            }
        } else {
            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected void assignStaticField(StageFrame state, int at, Type classType, Type fieldType, CtField field, Object value, int valueOffset) throws BadBytecode {
        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        UseIndex valueIndex = UseIndex.makeStack(at, valueOffset);
        if (initialPass && !allNodes.containsKey(sourceIndex)) {
            StageAnnotation ann = StageAnnotation.forField(field);

            if (ann != null && (suppressAnn == null || !suppressAnn.isSurpressed(ann.getLanguage()))) {
                Use.Argument arg = new Use.Argument(valueIndex, fieldType, 0, AcceptAnnotation.forField(field).get(0).getLanguages());
                allNodes.put(valueIndex, arg);

                allNodes.put(sourceIndex, new Source.Staged.FieldAssignment(sourceIndex, Type.VOID, ann.getLanguage(), ann.getStaticInfoElements(), ImmutableList.of(arg), field));
            } else {
                allNodes.put(valueIndex, new Use.Opaque(valueIndex, fieldType));

                allNodes.put(sourceIndex, new Source.Opaque(sourceIndex, fieldType));
            }
        } else {
            registerControl(state, valueIndex);

            registerControl(state, sourceIndex);
        }
    }

    protected Object readField(StageFrame state, int at, Type targetType, Type fieldType, CtField field, Object targetObject, int targetObjectOffset) throws BadBytecode {
        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        UseIndex targetObjectIndex = UseIndex.makeStack(at, targetObjectOffset);
        if (initialPass && !allNodes.containsKey(sourceIndex)) {
            StageAnnotation ann = StageAnnotation.forField(field);

            if (ann != null && (suppressAnn == null || !suppressAnn.isSurpressed(ann.getLanguage()))) {
                field = (CtField) ann.getAnnotatedMember();
                targetType = Type.of(field.getDeclaringClass());

                Use.Argument arg = new Use.Argument(targetObjectIndex, targetType, 0, AcceptAnnotation.forField(field).get(0).getLanguages());
                allNodes.put(targetObjectIndex, arg);

                try {
                    allNodes.put(sourceIndex, new Source.Staged.FieldRead(sourceIndex, Type.of(field.getType()), ann.getLanguage(), ann.isStrict(), ann.getStaticInfoElements(), ImmutableList.of(arg), field));
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                allNodes.put(targetObjectIndex, new Use.Opaque(targetObjectIndex, targetType));

                allNodes.put(sourceIndex, new Source.Opaque(sourceIndex, fieldType));
            }
        } else {
            registerControl(state, targetObjectIndex);

            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected void assignField(StageFrame state, int at, CtField field, Type targetType, Type fieldType, Object targetObject, int targetObjectOffset, Object value, int valueOffset) throws BadBytecode {
        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        UseIndex targetObjectIndex = UseIndex.makeStack(at, targetObjectOffset);
        UseIndex valueIndex = UseIndex.makeStack(at, valueOffset);
        if (initialPass && !allNodes.containsKey(sourceIndex)) {
            StageAnnotation ann = StageAnnotation.forField(field);

            if (ann != null && (suppressAnn == null || !suppressAnn.isSurpressed(ann.getLanguage()))) {
                if (valueFlowResult.isUninitializedThis(targetObjectIndex)) {
                    throw new RuntimeException("Staged field access not permitted before initialization!");
                    // See JVM specification under "4.10.2.4. Instance Initialization Methods and Newly Created Objects"...
                    // Field accesses might be allowed on an uninitialized this ("the only operation the method can
                    // perform on this is assigning fields declared within myClass), but we cannot make this possible
                    // for staged field accesses.
                }

                field = (CtField) ann.getAnnotatedMember();
                targetType = Type.of(field.getDeclaringClass());
                try {
                    fieldType = Type.of(field.getType());
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }

                ImmutableList<AcceptAnnotation> acceptAnnotations = AcceptAnnotation.forField(field);
                Use.Argument[] args = new Use.Argument[]{
                        new Use.Argument(targetObjectIndex, targetType, 0, acceptAnnotations.get(0).getLanguages()),
                        new Use.Argument(valueIndex, fieldType, 1, acceptAnnotations.get(1).getLanguages())
                };
                allNodes.put(valueIndex, args[0]);
                allNodes.put(valueIndex, args[1]);

                allNodes.put(sourceIndex, new Source.Staged.FieldAssignment(sourceIndex, Type.VOID, ann.getLanguage(), ann.getStaticInfoElements(), ImmutableList.copyOf(args), field));
            } else {
                allNodes.put(targetObjectIndex, new Use.Opaque(targetObjectIndex, targetType));
                allNodes.put(valueIndex, new Use.Opaque(valueIndex, fieldType));

                allNodes.put(sourceIndex, new Source.Opaque(sourceIndex, fieldType));
            }
        } else {
            registerControl(state, targetObjectIndex);
            registerControl(state, valueIndex);

            registerControl(state, sourceIndex);
        }
    }

    protected Object invokeStaticMethodExceptBoxing(StageFrame state, int at, CtMethod method, Type returnType, Type[] paramTypes, ArrayList<Object> arguments, int[] argumentOffsets) throws BadBytecode {
        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        UseIndex[] argumentIndices = new UseIndex[argumentOffsets.length];
        for (int i = 0; i < argumentOffsets.length; i++) {
            argumentIndices[i] = UseIndex.makeStack(at, argumentOffsets[i]);
        }

        if (initialPass && !allNodes.containsKey(sourceIndex)) {
            StageAnnotation ann = StageAnnotation.forMethod(method);

            if (ann != null && (suppressAnn == null || !suppressAnn.isSurpressed(ann.getLanguage()))) {
                ImmutableList<AcceptAnnotation> acceptAnnotations = AcceptAnnotation.forMethod(method);
                Use.Argument[] args = new Use.Argument[argumentOffsets.length];
                for (int i = 0; i < argumentIndices.length; i++) {
                    args[i] = new Use.Argument(argumentIndices[i], paramTypes[i], i, acceptAnnotations.get(i).getLanguages());
                    allNodes.put(argumentIndices[i], args[i]);
                }

                allNodes.put(sourceIndex, new Source.Staged.MethodInvocation(sourceIndex, returnType, ann.getLanguage(), ann.isStrict(), ann.getStaticInfoElements(), ImmutableList.copyOf(args), method));
            } else {
                for (int i = 0; i < argumentIndices.length; i++) {
                    allNodes.put(argumentIndices[i], new Use.Opaque(argumentIndices[i], paramTypes[i]));
                }

                allNodes.put(sourceIndex, new Source.Opaque(sourceIndex, returnType));
            }
        } else {
            for (int i = 0; i < argumentIndices.length; i++) {
                registerControl(state, argumentIndices[i]);
            }

            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected Object invokeMethodExceptUnboxing(StageFrame state, int at, CtMethod method, Type targetType, Type returnType, Type[] paramTypes, Object targetObject, int targetObjectOffset, ArrayList<Object> arguments, int[] argumentOffsets) throws BadBytecode {
        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        UseIndex targetObjectIndex = UseIndex.makeStack(at, targetObjectOffset);
        UseIndex[] argumentIndices = new UseIndex[argumentOffsets.length];
        for (int i = 0; i < argumentOffsets.length; i++) {
            argumentIndices[i] = UseIndex.makeStack(at, argumentOffsets[i]);
        }

        if (initialPass && !allNodes.containsKey(sourceIndex)) {
            StageAnnotation ann = StageAnnotation.forMethod(method);

            if (ann != null && (suppressAnn == null || !suppressAnn.isSurpressed(ann.getLanguage()))) {
                method = (CtMethod) ann.getAnnotatedMember();
                targetType = Type.of(method.getDeclaringClass());
                try {
                    CtClass[] pTypes = method.getParameterTypes();
                    for (int i = 0; i < pTypes.length; i++) {
                        paramTypes[i] = Type.of(pTypes[i]);
                    }
                    returnType = Type.of(method.getReturnType());
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }

                ImmutableList<AcceptAnnotation> acceptAnnotations = AcceptAnnotation.forMethod(method);
                Use.Argument[] args = new Use.Argument[argumentOffsets.length + 1];
                args[0] = new Use.Argument(targetObjectIndex, targetType, 0, acceptAnnotations.get(0).getLanguages());
                allNodes.put(targetObjectIndex, args[0]);
                for (int i = 0; i < argumentIndices.length; i++) {
                    args[i + 1] = new Use.Argument(argumentIndices[i], paramTypes[i], i + 1, acceptAnnotations.get(i + 1).getLanguages());
                    allNodes.put(argumentIndices[i], args[i + 1]);
                }

                allNodes.put(sourceIndex, new Source.Staged.MethodInvocation(SourceIndex.makeExplicitStackTop(at), returnType, ann.getLanguage(), ann.isStrict(), ann.getStaticInfoElements(), ImmutableList.copyOf(args), method));
            } else {
                allNodes.put(targetObjectIndex, new Use.Opaque(targetObjectIndex, targetType));
                for (int i = 0; i < argumentIndices.length; i++) {
                    allNodes.put(argumentIndices[i], new Use.Opaque(argumentIndices[i], paramTypes[i]));
                }

                allNodes.put(sourceIndex, new Source.Opaque(sourceIndex, returnType));
            }
        } else {
            registerControl(state, targetObjectIndex);
            for (int i = 0; i < argumentIndices.length; i++) {
                registerControl(state, argumentIndices[i]);
            }

            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected void invokeConstructor(StageFrame state, int at, CtConstructor constructor, Type targetType, Type[] paramTypes, Object targetObject, int targetObjectOffset, ArrayList<Object> arguments, int[] argumentOffsets) throws BadBytecode {
        UseIndex targetObjectIndex = UseIndex.makeStack(at, targetObjectOffset);
        // We cannot just generate an index here because there is special handling for constructor invocation (see ValueFlowAnalyzer)
        SourceIndex sourceIndex = valueFlowResult.getInitializationSourceIndex(at);
        if (sourceIndex == null) {
            throw new RuntimeException();
        }

        UseIndex[] argumentIndices = new UseIndex[argumentOffsets.length];
        for (int i = 0; i < argumentOffsets.length; i++) {
            argumentIndices[i] = UseIndex.makeStack(at, argumentOffsets[i]);
        }

        if (initialPass && !allNodes.containsKey(sourceIndex)) {
            allNodes.put(targetObjectIndex, new Use.Opaque(targetObjectIndex, targetType));
            for (int i = 0; i < argumentIndices.length; i++) {
                allNodes.put(argumentIndices[i], new Use.Opaque(argumentIndices[i], paramTypes[i]));
            }

            allNodes.put(sourceIndex, new Source.Opaque(sourceIndex, Type.VOID));
        } else {
            registerControl(state, targetObjectIndex);
            for (int i = 0; i < argumentIndices.length; i++) {
                registerControl(state, argumentIndices[i]);
            }

            registerControl(state, sourceIndex);
        }
    }

    protected Object invokeDynamic(StageFrame state, int at, CtMethod bootstrapMethod, StaticArgument[] staticArguments, Type returnType, Type[] paramTypes, ArrayList<Object> arguments, int[] argumentOffsets) throws BadBytecode {
        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        UseIndex[] argumentIndices = new UseIndex[argumentOffsets.length];
        for (int i = 0; i < argumentOffsets.length; i++) {
            argumentIndices[i] = UseIndex.makeStack(at, argumentOffsets[i]);
        }

        if (initialPass && !allNodes.containsKey(sourceIndex)) {
            for (int i = 0; i < argumentIndices.length; i++) {
                allNodes.put(argumentIndices[i], new Use.Opaque(argumentIndices[i], paramTypes[i]));
            }

            allNodes.put(sourceIndex, new Source.Opaque(sourceIndex, returnType));
        } else {
            for (int i = 0; i < argumentIndices.length; i++) {
                registerControl(state, argumentIndices[i]);
            }

            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected Object newInstance(StageFrame state, int at, Type type) throws BadBytecode {
        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        if (initialPass) {
            allNodes.computeIfAbsent(sourceIndex, (k) -> new Source.Opaque(sourceIndex, type));
        } else {
            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected Object arrayLength(StageFrame state, int at, Object array, int arrayOffset) throws BadBytecode {
        // We have no choice but to use the inferred type
        Type inferredType = typeAnalyzerResult.getStack(at, arrayOffset);

        UseIndex arrayIndex = UseIndex.makeStack(at, arrayOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(arrayIndex, (k) -> new Use.Opaque(arrayIndex, inferredType));
        } else {
            registerControl(state, arrayIndex);
        }

        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        if (!allNodes.containsKey(sourceIndex)) {
            allNodes.put(sourceIndex, new Source.Opaque(sourceIndex, Type.INT));
        } else {
            registerControl(state, sourceIndex);
        }

        return null;
    }

    protected Object throwException(StageFrame state, int at, Object throwable, int throwableOffset) throws BadBytecode {
        UseIndex throwableIndex = UseIndex.makeStack(at, throwableOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(throwableIndex, (k) -> new Use.Opaque(throwableIndex, Type.THROWABLE));
        } else {
            registerControl(state, throwableIndex);
        }

        return null;
    }

    protected Object instanceOf(StageFrame state, int at, Type ofType, Object value, int valueOffset) throws BadBytecode {
        UseIndex valueIndex = UseIndex.makeStack(at, valueOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(valueIndex, (k) -> new Use.Opaque(valueIndex, Type.OBJECT));
        } else {
            registerControl(state, valueIndex);
        }

        return null;
    }

    protected void enterSynchronized(StageFrame state, int at, Object monitor, int monitorOffset) throws BadBytecode {
        UseIndex monitorIndex = UseIndex.makeStack(at, monitorOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(monitorIndex, (k) -> new Use.Opaque(monitorIndex, Type.OBJECT));
        } else {
            registerControl(state, monitorIndex);
        }
    }

    protected void exitSynchronized(StageFrame state, int at, Object monitor, int monitorOffset) throws BadBytecode {
        UseIndex monitorIndex = UseIndex.makeStack(at, monitorOffset);
        if (initialPass) {
            allNodes.computeIfAbsent(monitorIndex, (k) -> new Use.Opaque(monitorIndex, Type.OBJECT));
        } else {
            registerControl(state, monitorIndex);
        }
    }

    protected Object newArray(StageFrame state, int at, Type componentType, ArrayList<Object> lengths, int[] lengthOffsets) throws BadBytecode {
        SourceIndex sourceIndex = SourceIndex.makeExplicitStackTop(at);
        UseIndex[] lengthIndices = new UseIndex[lengthOffsets.length];
        for (int i = 0; i < lengthOffsets.length; i++) {
            lengthIndices[i] = UseIndex.makeStack(at, lengthOffsets[i]);
        }

        if (initialPass && !allNodes.containsKey(sourceIndex)) {
            for (int i = 0; i < lengthIndices.length; i++) {
                allNodes.put(lengthIndices[i], new Use.Opaque(lengthIndices[i], Type.INT));
            }

            // We are lazy here...
            Type inferredType = typeAnalyzerResult.getStack(at, 0);

            allNodes.put(sourceIndex, new Source.Opaque(sourceIndex, inferredType));
        } else {
            for (int i = 0; i < lengthIndices.length; i++) {
                registerControl(state, lengthIndices[i]);
            }

            registerControl(state, sourceIndex);
        }

        return null;
    }
}
