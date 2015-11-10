package tamestaj;

import com.google.common.collect.ImmutableSet;
import javassist.*;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.Descriptor;
import javassist.bytecode.MethodInfo;

import java.util.*;
import java.util.stream.IntStream;

@SuppressWarnings("unused")
final class ValueFlowAnalyzer extends HighLevelAnalyzerWithBoxingUnboxing<ValueFlowAnalyzer.TrackedValue, ValueFlowAnalyzer.TrackedValue, ValueFlowAnalyzer.TrackedValueFrame> {
    private final TypeAnalyzer.Result typeAnalyzerResult;

    private final TrackedValueFrame initialState;

    private final HashMap<Integer, SourceIndex> initializationSourceIndices;

    // It should be fairly straightforward to also track virtual sources so that we could get a clearer
    // view on how values flow and get merged... (like phi nodes)
    // However, at the moment this is not necessary!
    private final HashMap<SourceIndex, HashSet<UseIndex>> sourceToUses;
    private final Partitioning<SourceIndex> mergeClassPartitioning;

    private enum InitializationStatus {
        UNINITIALIZED_NEW,
        UNINITIALIZED_THIS,
        INITIALIZED,
        NOT_INITIALIZABLE
    }

    final static class TrackedValue {
        private final ImmutableSet<SourceIndex> sources;
        private final SourceIndex virtualSource;
        private final InitializationStatus initializationStatus;

        private TrackedValue(SourceIndex source, SourceIndex virtualSource, InitializationStatus initializationStatus) {
            this.sources = ImmutableSet.of(source);
            this.virtualSource = virtualSource;
            this.initializationStatus = initializationStatus;
        }
        private TrackedValue(ImmutableSet<SourceIndex> sources, SourceIndex virtualSource, InitializationStatus initializationStatus) {
            this.sources = sources;
            this.virtualSource = virtualSource;
            this.initializationStatus = initializationStatus;
        }

        private TrackedValue merge(TrackedValue trackedValue, SourceIndex mergedVirtualSource, Partitioning<SourceIndex> mergeClassPartitioning) {
            if (trackedValue == null) {
                return null;
            }
            if (this == trackedValue) {
                return this;
            }

            if (mergedVirtualSource == null) {
                throw new RuntimeException("This should never happen!");
            }

            ImmutableSet.Builder<SourceIndex> builder = ImmutableSet.builder();
            builder.addAll(sources);
            builder.addAll(trackedValue.sources);
            ImmutableSet sources = builder.build();

            SourceIndex a = null;
            for (SourceIndex s : this.sources) {
                a = s;
                break;
            }
            if (a != null) {
                SourceIndex b = null;
                for (SourceIndex s : trackedValue.sources) {
                    b = s;
                    break;
                }

                if (b != null) {
                    mergeClassPartitioning.combinePartitions(a, b);
                }
            }

            if (initializationStatus != trackedValue.initializationStatus) {
                return new TrackedValue(sources, mergedVirtualSource, InitializationStatus.NOT_INITIALIZABLE);
            } else {
                return new TrackedValue(sources, mergedVirtualSource, initializationStatus);
            }
        }
        private ImmutableSet<SourceIndex> getSourceIndices() {
            return sources;
        }
        private SourceIndex getVirtualSource() {
            return virtualSource;
        }
        private InitializationStatus getInitializationStatus() {
            return initializationStatus;
        }

        public int hashCode() {
            return sources.hashCode() + 31 * virtualSource.hashCode() + 31 * 31 * initializationStatus.hashCode();
        }
        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (!(obj instanceof TrackedValue)) { return false; }

            TrackedValue t = (TrackedValue) obj;
            return initializationStatus == t.initializationStatus && virtualSource.equals(t.virtualSource) && sources.equals(t.sources);
        }

        public String toString() {
            return "<" + sources + ", " + virtualSource + ", " + initializationStatus + ">";
        }
    }

    final class TrackedValueFrame extends AbstractFrame<TrackedValue, TrackedValue> {
        private TrackedValueFrame(int maxLocals, int maxStack) {
            super(maxLocals, maxStack);
        }

        private SourceIndex[] computeMergedVirtualSources(TrackedValueFrame withFrame, int at, boolean ignoreStack) {
            HashMap<Tuple<SourceIndex, SourceIndex>, Integer> correspondenceMap = new HashMap<>();

            SourceIndex[] mergedVirtualSources = new SourceIndex[getMaxLocals() + getStackSize()];
            int[] fixUpIndices = IntStream.range(0, mergedVirtualSources.length).toArray();

            for (int i = 0; i < getMaxLocals(); i++) {
                if (getLocal(i) != null && typeAnalyzerResult.getLocal(at, i) != Type.BOGUS) {
                    SourceIndex s = getLocal(i).getVirtualSource();
                    SourceIndex withS = withFrame.getLocal(i).getVirtualSource();

                    if (s.equals(withS)) {
                        mergedVirtualSources[i] = s;
                    } else {
                        Tuple<SourceIndex, SourceIndex> t = new Tuple<>(s, withS);
                        Integer index = correspondenceMap.get(t);
                        if (index == null) {
                            SourceIndex mergedVirtualSource = SourceIndex.makeImplicitLocal(at, i);

                            correspondenceMap.put(t, i);
                            mergedVirtualSources[i] = mergedVirtualSource;
                            fixUpIndices[i] = i;
                        } else {
                            SourceIndex mergedVirtualSource = mergedVirtualSources[index];
                            int[] localIndices = new int[mergedVirtualSource.getLocalIndexCount() + 1];
                            for (int j = 0; j < localIndices.length - 1; j++) {
                                localIndices[j] = mergedVirtualSource.getLocalIndex(j);
                            }
                            localIndices[localIndices.length - 1] = i;

                            mergedVirtualSource = SourceIndex.makeImplicit(at, null, localIndices);
                            mergedVirtualSources[index] = mergedVirtualSource;
                            fixUpIndices[i] = index;
                        }
                    }
                }
            }

            if (!ignoreStack) {
                for (int i = 0; i < getStackSize(); i++) {
                    if (getStack(i) != null) {
                        SourceIndex s = getStack(i).getVirtualSource();
                        SourceIndex withS = withFrame.getStack(i).getVirtualSource();

                        if (s.equals(withS)) {
                            mergedVirtualSources[getMaxLocals() + i] = s;
                        } else {
                            Tuple<SourceIndex, SourceIndex> t = new Tuple<>(s, withS);
                            Integer index = correspondenceMap.get(t);
                            if (index == null) {
                                SourceIndex mergedVirtualSource = SourceIndex.makeImplicitStack(at, i);

                                correspondenceMap.put(t, getMaxLocals() + i);
                                mergedVirtualSources[getMaxLocals() + i] = mergedVirtualSource;
                                fixUpIndices[getMaxLocals() + i] = getMaxLocals() + i;
                            } else {
                                SourceIndex mergedVirtualSource = mergedVirtualSources[index];
                                int[] localIndices = null;
                                if (mergedVirtualSource.getLocalIndexCount() > 0) {
                                    localIndices = new int[mergedVirtualSource.getLocalIndexCount()];
                                    for (int j = 0; j < localIndices.length; j++) {
                                        localIndices[j] = mergedVirtualSource.getLocalIndex(j);
                                    }
                                }
                                int[] stackOffsets = new int[mergedVirtualSource.getStackOffsetCount() + 1];
                                for (int j = 0; j < stackOffsets.length - 1; j++) {
                                    stackOffsets[j] = mergedVirtualSource.getLocalIndex(j);
                                }
                                stackOffsets[stackOffsets.length - 1] = i;

                                mergedVirtualSource = SourceIndex.makeImplicit(at, localIndices, stackOffsets);
                                mergedVirtualSources[index] = mergedVirtualSource;
                                fixUpIndices[getMaxLocals() + i] = index;
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < fixUpIndices.length; i++) {
                mergedVirtualSources[i] = mergedVirtualSources[fixUpIndices[i]];
            }

            return mergedVirtualSources;
        }

        public void mergeLocals(TrackedValueFrame withFrame, int at, Partitioning<SourceIndex> mergeClassPartitioning, SourceIndex[] mergedVirtualSources) {
            for (int i = 0; i < getMaxLocals(); i++) {
                if (getLocal(i) != null) {
                    if (typeAnalyzerResult.getLocal(at, i) == Type.BOGUS) {
                        setLocal(i, null);
                    } else {
                        TrackedValue prev = getLocal(i);
                        TrackedValue merged = prev.merge(withFrame.getLocal(i), mergedVirtualSources[i], mergeClassPartitioning);

                        setLocal(i, merged);
                    }
                }
            }
        }

        public void mergeLocals(TrackedValueFrame withFrame, int at, Partitioning<SourceIndex> mergeClassPartitioning) {
            mergeLocals(withFrame, at, mergeClassPartitioning, computeMergedVirtualSources(withFrame, at, true));
        }

        public void merge(TrackedValueFrame withFrame, int at, Partitioning<SourceIndex> mergeClassPartitioning) {
            SourceIndex[] mergedVirtualSources = computeMergedVirtualSources(withFrame, at, false);

            mergeLocals(withFrame, at, mergeClassPartitioning, mergedVirtualSources);

            for (int i = 0; i < getStackSize(); i++) {
                if (getStack(i) != null) {
                    TrackedValue prev = getStack(i);
                    TrackedValue merged = prev.merge(withFrame.getStack(i), mergedVirtualSources[getMaxLocals() + i], mergeClassPartitioning);

                    setStack(i, merged);
                }
            }
        }

        public TrackedValueFrame copyLocals() {
            TrackedValueFrame frame = new TrackedValueFrame(getMaxLocals(), getMaxStack());
            copyLocalsInto(frame);
            return frame;
        }

        public TrackedValueFrame copy() {
            TrackedValueFrame frame = new TrackedValueFrame(getMaxLocals(), getMaxStack());
            copyInto(frame);
            return frame;
        }

        String printVirtualSources() {
            StringBuilder sb = new StringBuilder();

            sb.append("locals = [");
            for (int i = 0; i < getMaxLocals(); i++) {
                if (getLocal(i) == null) {
                    sb.append("empty");
                } else {
                    sb.append(getLocal(i).getVirtualSource());
                }
                if (i < getMaxLocals() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("] stack = [");
            for (int i = 0; i < getStackSize(); i++) {
                if (getStack(i) != null) {
                    sb.append(getStack(i).getVirtualSource());
                }
                if (i < getStackSize() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");

            return sb.toString();
        }

        public String toString() {
            return printVirtualSources();
        }
    }

    final class Result {
        private Result() { }

        ImmutableSet<SourceIndex> getMergeClass(SourceIndex sourceIndex) {
            return mergeClassPartitioning.getPartition(sourceIndex);
        }
        ImmutableSet<UseIndex> getUses(SourceIndex sourceIndex) {
            if (sourceToUses.containsKey(sourceIndex)) {
                return ImmutableSet.copyOf(sourceToUses.get(sourceIndex));
            } else {
                return ImmutableSet.of();
            }
        }
        ImmutableSet<SourceIndex> getSources(UseIndex useIndex) {
            return getTrackedValue(useIndex).getSourceIndices();
        }
        SourceIndex getVirtualSource(UseIndex useIndex) {
            return getTrackedValue(useIndex).getVirtualSource();
        }
        boolean isUninitializedThis(UseIndex useIndex) { return getTrackedValue(useIndex).getInitializationStatus() == InitializationStatus.UNINITIALIZED_THIS; }
        private TrackedValue getTrackedValue(UseIndex useIndex) {
            if (!useIndex.isStackless()) {
                return getInState(useIndex.getPosition()).getStack(useIndex.getStackOffset());
            } else {
                return getInState(useIndex.getPosition()).getLocal(useIndex.getLocalIndex());
            }
        }
        SourceIndex getInitializationSourceIndex(int position) {
            return initializationSourceIndices.get(position);
        }
    }

    ValueFlowAnalyzer(TypeAnalyzer typeAnalyzer) {
        super(typeAnalyzer);

        this.typeAnalyzerResult = typeAnalyzer.getResult();

        MethodInfo methodInfo = behavior.getMethodInfo2();

        int maxLocals = codeAttribute.getMaxLocals();
        int maxStack = codeAttribute.getMaxStack();
        int codeLength = codeAttribute.getCodeLength();

        initializationSourceIndices = new HashMap<>();

        sourceToUses = new HashMap<>();
        mergeClassPartitioning = new Partitioning<>();

        initialState = new TrackedValueFrame(maxLocals, maxStack);

        int pos = 0;
        if (!Modifier.isStatic(behavior.getModifiers())) {
            if (behavior instanceof CtConstructor) {
                initialState.setLocal(pos, trackValue(initialState, SourceIndex.makeImplicitLocal(0, pos), InitializationStatus.UNINITIALIZED_THIS));
            } else {
                initialState.setLocal(pos, trackValue(initialState, SourceIndex.makeImplicitLocal(0, pos), InitializationStatus.INITIALIZED));
            }
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
                initialState.setLocal2(pos, trackValue(initialState, SourceIndex.makeImplicitLocal(0, pos), InitializationStatus.INITIALIZED));
                pos += 2;
            } else {
                initialState.setLocal(pos, trackValue(initialState, SourceIndex.makeImplicitLocal(0, pos), InitializationStatus.INITIALIZED));
                pos++;
            }
        }
    }

    Result getResult() {
        return new Result();
    }

    private TrackedValue trackValue(TrackedValueFrame frame, SourceIndex source, InitializationStatus initializationStatus) {
        return new TrackedValue(source, source, initializationStatus);
    }

    private void useStack(TrackedValueFrame frame, TrackedValue value, int at, int stackOffset) {
        UseIndex use = UseIndex.makeStack(at, stackOffset);
        for (SourceIndex source : value.getSourceIndices()) {
            HashSet<UseIndex> uses = sourceToUses.get(source);
            if (uses == null) {
                uses = new HashSet<>();
                sourceToUses.put(source, uses);
            }
            uses.add(use);
        }
    }

    private void useStackless(TrackedValueFrame frame, TrackedValue value, int at, int localIndex) {
        UseIndex use = UseIndex.makeLocal(at, localIndex);
        for (SourceIndex source : value.getSourceIndices()) {
            HashSet<UseIndex> uses = sourceToUses.get(source);
            if (uses == null) {
                uses = new HashSet<>();
                sourceToUses.put(source, uses);
            }
            uses.add(use);
        }
    }

    protected TrackedValueFrame copyState(TrackedValueFrame original) { return original.copy(); }

    protected boolean stateEquals(TrackedValueFrame state, TrackedValueFrame otherState) { return state.equals(otherState); }

    protected TrackedValueFrame initialState() { return initialState; }

    protected TrackedValueFrame mergeStatesOnCatch(ArrayList<TrackedValueFrame> states, int[] origins, int at, Type caughtException) {
        TrackedValueFrame mergedFrame = states.get(0).copyLocals();

        if (states.size() > 1) {
            for (int i = 1; i < states.size(); i++) {
                mergedFrame.mergeLocals(states.get(i), at, mergeClassPartitioning);
            }
        }

        mergedFrame.push(trackValue(mergedFrame, SourceIndex.makeImplicitStackTop(at), InitializationStatus.INITIALIZED));

        return mergedFrame;
    }

    protected TrackedValueFrame mergeStates(ArrayList<TrackedValueFrame> states, int[] origins, int at) {
        TrackedValueFrame mergedFrame = states.get(0).copy();

        if (states.size() > 1) {
            for (int i = 1; i < states.size(); i++) {
                mergedFrame.merge(states.get(i), at, mergeClassPartitioning);
            }
        }

        return mergedFrame;
    }

    protected TrackedValue createNull(TrackedValueFrame state, int at) throws BadBytecode {
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected TrackedValue createIntegerConstant(TrackedValueFrame state, int at, int value) throws BadBytecode {
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected TrackedValue createLongConstant(TrackedValueFrame state, int at, long value) throws BadBytecode {
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected TrackedValue createFloatConstant(TrackedValueFrame state, int at, float value) throws BadBytecode {
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected TrackedValue createDoubleConstant(TrackedValueFrame state, int at, double value) throws BadBytecode {
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected TrackedValue createByteConstant(TrackedValueFrame state, int at, byte value) throws BadBytecode {
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected TrackedValue createShortConstant(TrackedValueFrame state, int at, short value) throws BadBytecode {
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected TrackedValue createStringConstant(TrackedValueFrame state, int at, String value) throws BadBytecode {
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected TrackedValue createClassConstant(TrackedValueFrame state, int at, CtClass value) throws BadBytecode {
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected TrackedValue readLocal(TrackedValueFrame state, int at, Type type, int index, TrackedValue local) throws BadBytecode {
        return local;
    }

    protected TrackedValue readArray(TrackedValueFrame state, int at, Type componentType, TrackedValue array, int arrayOffset, TrackedValue index, int indexOffset) throws BadBytecode {
        useStack(state, array, at, arrayOffset);
        useStack(state, index, at, indexOffset);
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected TrackedValue assignLocal(TrackedValueFrame state, int at, Type type, int index, TrackedValue value, int valueOffset) throws BadBytecode {
        return value;
    }

    protected void assignArray(TrackedValueFrame state, int at, Type componentType, TrackedValue array, int arrayOffset, TrackedValue index, int indexOffset, TrackedValue value, int valueOffset) throws BadBytecode {
        useStack(state, array, at, arrayOffset);
        useStack(state, index, at, indexOffset);
        useStack(state, value, at, valueOffset);
    }

    protected TrackedValue performBinaryArithmetic(TrackedValueFrame state, int at, Type type, ArithmeticOperation operation, TrackedValue left, int leftOffset, TrackedValue right, int rightOffset) throws BadBytecode {
        useStack(state, left, at, leftOffset);
        useStack(state, right, at, rightOffset);
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected TrackedValue performShift(TrackedValueFrame state, int at, Type type, ShiftOperation operation, TrackedValue left, int leftOffset, TrackedValue right, int rightOffset) throws BadBytecode {
        useStack(state, left, at, leftOffset);
        useStack(state, right, at, rightOffset);
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected TrackedValue performNegation(TrackedValueFrame state, int at, Type type, TrackedValue value, int valueOffset) throws BadBytecode {
        useStack(state, value, at, valueOffset);
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected TrackedValue incrementLocal(TrackedValueFrame state, int at, int index, TrackedValue local, int increment) throws BadBytecode {
        useStackless(state, local, at, index);
        return trackValue(state, SourceIndex.makeExplicitLocal(at, index), InitializationStatus.INITIALIZED);
    }

    protected TrackedValue convertType(TrackedValueFrame state, int at, Type from, Type to, TrackedValue value, int valueOffset) throws BadBytecode {
        Type inferredFrom = typeAnalyzerResult.getStack(at, 0);
        if (Util.isSafeConversion(behavior, inferredFrom, to)) {
            // Safe conversions (e.g. upcasts) are not considered uses.

            // Note that our type model is rather inaccurate because it does not handle intersection types.
            // The safe conversion check is conservative about this, so sometimes an actually run-time safe conversion
            // is mislabeled as a potentially unsafe one.
            // TODO: The above should not cause big problems, but a more accurate type model would certainly help!

            return value;
        } else {
            useStack(state, value, at, valueOffset);
            return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
        }
    }

    protected TrackedValue compare(TrackedValueFrame state, int at, Type type, TrackedValue left, int leftOffset, TrackedValue right, int rightOffset) throws BadBytecode {
        useStack(state, left, at, leftOffset);
        useStack(state, right, at, rightOffset);
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected TrackedValue compare(TrackedValueFrame state, int at, Type type, ComparisonOperation operation, TrackedValue left, int leftOffset, TrackedValue right, int rightOffset) throws BadBytecode {
        useStack(state, left, at, leftOffset);
        useStack(state, right, at, rightOffset);
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected void branchIf(TrackedValueFrame state, int at, Type type, ComparisonOperation operation, TrackedValue value, int valueOffset, int trueTarget, int falseTarget) throws BadBytecode {
        useStack(state, value, at, valueOffset);
    }

    protected void branchIfCompare(TrackedValueFrame state, int at, Type type, ComparisonOperation operation, TrackedValue left, int leftOffset, TrackedValue right, int rightOffset, int trueTarget, int falseTarget) throws BadBytecode {
        useStack(state, left, at, leftOffset);
        useStack(state, right, at, rightOffset);
    }

    protected void branchGoto(TrackedValueFrame state, int at, int target) throws BadBytecode {

    }

    protected TrackedValue callSubroutine(TrackedValueFrame state, int at, int target) throws BadBytecode {
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected void returnFromSubroutine(TrackedValueFrame state, int at, int index, TrackedValue local) throws BadBytecode {
        // TODO: Not sure what to do...
        //       Return addresses (here in index) can be stored and loaded (ALOAD, ASTORE) but most relevant
        //       operations (i.e. not stack frame manipulation) are not supported.
        useStackless(state, local, at, index);
    }

    protected void branchTableSwitch(TrackedValueFrame state, int at, TrackedValue index, int indexOffset, int defaultTarget, int[]
            indexedTargets) throws BadBytecode {
        useStack(state, index, at, indexOffset);
    }

    protected void branchLookupSwitch(TrackedValueFrame state, int at, TrackedValue key, int keyOffset, int defaultTarget, int[]
            matches, int[] matchTargets) throws BadBytecode {
        useStack(state, key, at, keyOffset);
    }

    protected void returnFromMethod(TrackedValueFrame state, int at, Type type, TrackedValue value, int valueOffset) throws BadBytecode {
        useStack(state, value, at, valueOffset);
    }

    protected void returnFromMethod(TrackedValueFrame state, int at) throws BadBytecode {

    }

    protected TrackedValue readStaticField(TrackedValueFrame state, int at, Type classType, Type fieldType, CtField field) throws BadBytecode {
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected void assignStaticField(TrackedValueFrame state, int at, Type classType, Type fieldType, CtField field, TrackedValue value, int valueOffset) throws BadBytecode {
        useStack(state, value, at, valueOffset);
    }

    protected TrackedValue readField(TrackedValueFrame state, int at, Type targetType, Type fieldType, CtField field, TrackedValue targetObject, int targetObjectOffset) throws BadBytecode {
        useStack(state, targetObject, at, targetObjectOffset);
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected void assignField(TrackedValueFrame state, int at, CtField field, Type targetType, Type fieldType, TrackedValue targetObject, int targetObjectOffset, TrackedValue value, int valueOffset) throws BadBytecode {
        useStack(state, targetObject, at, targetObjectOffset);
        useStack(state, value, at, valueOffset);
    }

    protected TrackedValue invokeStaticMethodExceptBoxing(TrackedValueFrame state, int at, CtMethod method, Type returnType, Type[] paramTypes, ArrayList<TrackedValue> arguments, int[] argumentOffsets) throws BadBytecode {
        for (int i = 0; i < argumentOffsets.length; i++) {
            useStack(state, arguments.get(i), at, argumentOffsets[i]);
        }

        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected TrackedValue invokeMethodExceptUnboxing(TrackedValueFrame state, int at, CtMethod method, Type targetType, Type returnType, Type[] paramTypes, TrackedValue targetObject, int targetObjectOffset, ArrayList<TrackedValue> arguments, int[] argumentOffsets) throws BadBytecode {
        useStack(state, targetObject, at, targetObjectOffset);
        for (int i = 0; i < argumentOffsets.length; i++) {
            useStack(state, arguments.get(i), at, argumentOffsets[i]);
        }

        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected void invokeConstructor(TrackedValueFrame state, int at, CtConstructor constructor, Type targetType, Type[] paramTypes, TrackedValue targetObject, int targetObjectOffset, ArrayList<TrackedValue> arguments, int[] argumentOffsets) throws BadBytecode {
        // See JVM specification under "4.10.2.4. Instance Initialization Methods and Newly Created Objects"...

        useStack(state, targetObject, at, targetObjectOffset);
        for (int i = 0; i < argumentOffsets.length; i++) {
            useStack(state, arguments.get(i), at, argumentOffsets[i]);
        }

        if (targetObject.getInitializationStatus() == InitializationStatus.INITIALIZED || targetObject.getInitializationStatus() == InitializationStatus.NOT_INITIALIZABLE) {
            throw new RuntimeException();
        }

        ArrayList<Integer> stackOffsetsList = new ArrayList<>();
        for (int i = 0; i < state.getStackSize(); i++) {
            TrackedValue v = state.getStack(i);
            if (v != null) {
                if (v.equals(targetObject)) {
                    stackOffsetsList.add(i);
                }
            }
        }

        ArrayList<Integer> localIndicesList = new ArrayList<>();
        for (int i = 0; i < state.getMaxLocals(); i++) {
            TrackedValue v = state.getLocal(i);
            if (v != null) {
                if (v.equals(targetObject)) {
                    localIndicesList.add(i);
                }
            }
        }

        int[] stackOffsets = new int[stackOffsetsList.size()];
        int j = 0;
        for (Integer s : stackOffsetsList) {
            stackOffsets[j] = s;
            j++;
        }

        int[] localIndices = new int[localIndicesList.size()];
        int k = 0;
        for (Integer l : localIndicesList) {
            localIndices[k] = l;
            k++;
        }

        SourceIndex initializationSourceIndex = SourceIndex.makeExplicit(at, stackOffsets, localIndices);
        initializationSourceIndices.put(at, initializationSourceIndex);

        TrackedValue initialized = trackValue(state, initializationSourceIndex, InitializationStatus.INITIALIZED);
        for (int i = 0; i < state.getStackSize(); i++) {
            TrackedValue v = state.getStack(i);
            if (v != null) {
                if (v.equals(targetObject)) {
                    state.setStack(i, initialized);
                } else {
                    for (SourceIndex index : targetObject.getSourceIndices()) {
                        if (v.getSourceIndices().contains(index)) {
                            state.setStack(i, new TrackedValue(v.getSourceIndices(), v.getVirtualSource(), InitializationStatus.NOT_INITIALIZABLE));
                        }
                    }
                }
            }
        }

        for (int i = 0; i < state.getMaxLocals(); i++) {
            TrackedValue v = state.getLocal(i);
            if (v != null) {
                if (v.equals(targetObject)) {
                    state.setLocal(i, initialized);
                } else {
                    for (SourceIndex index : targetObject.getSourceIndices()) {
                        if (v.getSourceIndices().contains(index)) {
                            state.setLocal(i, new TrackedValue(v.getSourceIndices(), v.getVirtualSource(), InitializationStatus.NOT_INITIALIZABLE));
                        }
                    }
                }
            }
        }
    }

    protected TrackedValue invokeDynamic(TrackedValueFrame state, int at, CtMethod bootstrapMethod, StaticArgument[] staticArguments, Type returnType, Type[] paramTypes, ArrayList<TrackedValue> arguments, int[] argumentOffsets) throws BadBytecode {
        for (int i = 0; i < argumentOffsets.length; i++) {
            useStack(state, arguments.get(i), at, argumentOffsets[i]);
        }

        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected TrackedValue newInstance(TrackedValueFrame state, int at, Type type) throws BadBytecode {
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.UNINITIALIZED_NEW);
    }

    protected TrackedValue arrayLength(TrackedValueFrame state, int at, TrackedValue array, int arrayOffset) throws BadBytecode {
        useStack(state, array, at, arrayOffset);
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected TrackedValue throwException(TrackedValueFrame state, int at, TrackedValue throwable, int throwableOffset) throws BadBytecode {
        useStack(state, throwable, at, throwableOffset);
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected TrackedValue instanceOf(TrackedValueFrame state, int at, Type ofType, TrackedValue value, int valueOffset) throws BadBytecode {
        useStack(state, value, at, valueOffset);
        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }

    protected void enterSynchronized(TrackedValueFrame state, int at, TrackedValue monitor, int monitorOffset) throws BadBytecode {
        useStack(state, monitor, at, monitorOffset);
    }

    protected void exitSynchronized(TrackedValueFrame state, int at, TrackedValue monitor, int monitorOffset) throws BadBytecode {
        useStack(state, monitor, at, monitorOffset);
    }

    protected TrackedValue newArray(TrackedValueFrame state, int at, Type componentType, ArrayList<TrackedValue> lengths, int[] lengthOffsets) throws BadBytecode {
        for (int i = 0; i < lengthOffsets.length; i++) {
            useStack(state, lengths.get(i), at, lengthOffsets[i]);
        }

        return trackValue(state, SourceIndex.makeExplicitStackTop(at), InitializationStatus.INITIALIZED);
    }
}
