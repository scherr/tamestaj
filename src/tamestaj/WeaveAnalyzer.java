package tamestaj;

import com.google.common.collect.ImmutableList;
import javassist.*;
import javassist.bytecode.*;

import java.util.*;

@SuppressWarnings("unused")
final class WeaveAnalyzer extends HighLevelAnalyzerWithBoxingUnboxing<WeaveAnalyzer.WeaveType, WeaveAnalyzer.WeaveType, WeaveAnalyzer.WeaveFrame> {
    private final TypeAnalyzer.Result typeAnalyzerResult;
    private final ValueFlowAnalyzer.Result valueFlowResult;
    private final ConstantAnalyzer.Result constantAnalyzerResult;
    private final StageGraph stageGraph;
    private final CachabilityAnalyzer.Result cachabilityAnalyzerResult;
    private final LiftEstimateAnalyzer.Result liftEstimateAnalyzerResult;
    private final int maxLocals;

    private final Type returnType;
    private final WeaveFrame initialState;

    private final TreeMap<Integer, Type>[] preInstructionStackAdjustments;

    private final TreeMap<Integer, Type>[] preInstructionStackMaterializations;
    private final TreeMap<Integer, Type>[] preInstructionLocalMaterializations;

    private final TreeSet<Integer> initialStateLocalLifts;
    private final boolean[] postInstructionStackTopLifts;
    private final SourceIndex[] postInitializationInstructionLifts;
    private final boolean[] postCatchStackTopLifts;
    private final TreeSet<Integer>[] postInstructionLocalLifts;

    private final Bytecode[] instructionRewrites;

    private final HashMap<Source.Staged, CtField> permCachableStagedToClosureHolderField;
    private final HashMap<Source.Staged, CtField> traceCachableStagedToCacheField;
    private final HashMap<Source.Staged, CtField> stagedToStaticInfoField;

    private final HashSet<Integer> traceRecordPositions;

    enum WeaveType {
        MAYBE_LOCALLY_CARRYING(true) {
            WeaveType merge(WeaveType with) {
                if (with == null) { return null; }
                switch (with) {
                    case MAYBE_LOCALLY_CARRYING: return MAYBE_LOCALLY_CARRYING;
                    case MAYBE_GLOBALLY_CARRYING: return MAYBE_LOCALLY_CARRYING;
                    case NOT_CARRYING: return MAYBE_LOCALLY_CARRYING;
                    case GLOBALLY_CARRYING: return MAYBE_LOCALLY_CARRYING;
                    case LOCALLY_CARRYING: return MAYBE_LOCALLY_CARRYING;
                }

                throw new RuntimeException();
            }
        },
        MAYBE_GLOBALLY_CARRYING(false) {
            WeaveType merge(WeaveType with) {
                if (with == null) { return null; }
                switch (with) {
                    case MAYBE_LOCALLY_CARRYING: return MAYBE_LOCALLY_CARRYING;
                    case MAYBE_GLOBALLY_CARRYING: return MAYBE_GLOBALLY_CARRYING;
                    case NOT_CARRYING: return MAYBE_GLOBALLY_CARRYING;
                    case GLOBALLY_CARRYING: return MAYBE_GLOBALLY_CARRYING;
                    case LOCALLY_CARRYING: return MAYBE_LOCALLY_CARRYING;
                }

                throw new RuntimeException();
            }
        },
        NOT_CARRYING(false) {
            WeaveType merge(WeaveType with) {
                if (with == null) { return null; }
                switch (with) {
                    case MAYBE_LOCALLY_CARRYING: return MAYBE_LOCALLY_CARRYING;
                    case MAYBE_GLOBALLY_CARRYING: return MAYBE_GLOBALLY_CARRYING;
                    case NOT_CARRYING: return NOT_CARRYING;
                    case GLOBALLY_CARRYING: return MAYBE_GLOBALLY_CARRYING;
                    case LOCALLY_CARRYING: return MAYBE_LOCALLY_CARRYING;
                }

                throw new RuntimeException();
            }
        },
        GLOBALLY_CARRYING(false) {
            WeaveType merge(WeaveType with) {
                if (with == null) { return null; }
                switch (with) {
                    case MAYBE_LOCALLY_CARRYING: return MAYBE_LOCALLY_CARRYING;
                    case MAYBE_GLOBALLY_CARRYING: return MAYBE_GLOBALLY_CARRYING;
                    case NOT_CARRYING: return MAYBE_LOCALLY_CARRYING;
                    case GLOBALLY_CARRYING: return GLOBALLY_CARRYING;
                    case LOCALLY_CARRYING: return MAYBE_LOCALLY_CARRYING;
                }

                throw new RuntimeException();
            }
        },
        LOCALLY_CARRYING(true) {
            WeaveType merge(WeaveType with) {
                if (with == null) { return null; }
                switch (with) {
                    case MAYBE_LOCALLY_CARRYING: return MAYBE_LOCALLY_CARRYING;
                    case MAYBE_GLOBALLY_CARRYING: return MAYBE_LOCALLY_CARRYING;
                    case NOT_CARRYING: return MAYBE_LOCALLY_CARRYING;
                    case GLOBALLY_CARRYING: return MAYBE_LOCALLY_CARRYING;
                    case LOCALLY_CARRYING: return LOCALLY_CARRYING;
                }

                throw new RuntimeException();
            }
        };

        private final boolean attemptMaterialization;

        static WeaveType forTypeOriginal(Type type) {
            if (Util.isGlobalCarrier(type)) {
                return MAYBE_GLOBALLY_CARRYING;
            } else if (Util.isLocalCarrier(type)) {
                return NOT_CARRYING;
            } else if (Util.couldBeGlobalCarrier(type)) {
                return MAYBE_GLOBALLY_CARRYING;
            } else {
                return NOT_CARRYING;
            }
        }

        static WeaveType forTypeWoven(Type type) {
            if (Util.isGlobalCarrier(type)) {
                return GLOBALLY_CARRYING;
            } else if (Util.isLocalCarrier(type)) {
                return LOCALLY_CARRYING;
            } else {
                return LOCALLY_CARRYING;
            }
        }

        private WeaveType(boolean attemptMaterialization) {
            this.attemptMaterialization = attemptMaterialization;
        }

        boolean attemptMaterialization() { return attemptMaterialization; }

        abstract WeaveType merge(WeaveType with);
    }

    class WeaveFrame extends AbstractFrame<WeaveType, WeaveType> {
        private WeaveFrame(int maxLocals, int maxStack) {
            super(maxLocals, maxStack);
        }

        public void mergeLocals(WeaveFrame withFrame, int origin, int withFrameOrigin) {
            for (int i = 0; i < getMaxLocals(); i++) {
                if (getLocal(i) != null) {
                    WeaveType prev = getLocal(i);
                    WeaveType merged = prev.merge(withFrame.getLocal(i));

                    setLocal(i, merged);
                }
            }
        }

        public void merge(WeaveFrame withFrame, int origin, int withFrameOrigin) {
            mergeLocals(withFrame, origin, withFrameOrigin);

            for (int i = 0; i < getStackSize(); i++) {
                if (getStack(i) != null) {
                    WeaveType prev = getStack(i);
                    WeaveType merged = prev.merge(withFrame.getStack(i));

                    setStack(i, merged);
                }
            }
        }

        public WeaveFrame copyLocals() {
            WeaveFrame frame = new WeaveFrame(getMaxLocals(), getMaxStack());
            copyLocalsInto(frame);
            return frame;
        }

        public WeaveFrame copy() {
            WeaveFrame frame = new WeaveFrame(getMaxLocals(), getMaxStack());
            copyInto(frame);
            return frame;
        }
    }

    private static void addAdjustmentCode(Type expectedType, Bytecode bytecode, WeaveType weaveType, boolean isConstant) {
        if (isConstant) {
            switch (weaveType) {
                case NOT_CARRYING: {
                    if (expectedType.isReference()) {
                        if (Util.isGlobalCarrier(expectedType)) {
                            bytecode.addInvokestatic(Util.DISPATCHER_CLASS, "liftConstantObjectToGlobalCarrier", Util.GLOBAL_CARRIER_CLASS, new CtClass[]{ Type.OBJECT.getCtClass() });
                        } else {
                            bytecode.addInvokestatic(Util.DISPATCHER_CLASS, "liftConstantObjectToLocalCarrier", Util.LOCAL_CARRIER_CLASS, new CtClass[]{ Type.OBJECT.getCtClass() });
                        }
                    } else {
                        bytecode.addInvokestatic(Util.DISPATCHER_CLASS, Util.getConstantLiftMethodName(expectedType), Util.VALUE_CLASS, new CtClass[]{ expectedType.getCtClass() });
                    }

                    break;
                }
                default: {
                    if (expectedType.isReference()) {
                        if (Util.isGlobalCarrier(expectedType)) {
                            bytecode.addInvokestatic(Util.DISPATCHER_CLASS, "makeConstantGlobalCarrierChecked", Util.GLOBAL_CARRIER_CLASS, new CtClass[]{ Util.GLOBAL_CARRIER_CLASS });
                        } else if (Util.isLocalCarrier(expectedType)) {
                            bytecode.addInvokestatic(Util.DISPATCHER_CLASS, "makeConstantLocalCarrierChecked", Util.LOCAL_CARRIER_CLASS, new CtClass[]{ Util.LOCAL_CARRIER_CLASS });
                        } else {
                            bytecode.addInvokestatic(Util.DISPATCHER_CLASS, "makeConstantMaybeCarrierChecked", Type.OBJECT.getCtClass(), new CtClass[]{ Type.OBJECT.getCtClass() });
                        }
                    } else {
                        bytecode.addInvokestatic(Util.DISPATCHER_CLASS, "makeConstant", Util.VALUE_CLASS, new CtClass[]{ Util.EXPRESSION_CLASS });
                    }
                }
            }
        } else {
            switch (weaveType) {
                case NOT_CARRYING: {
                    if (expectedType.isReference()) {
                        if (Util.isGlobalCarrier(expectedType)) {
                            bytecode.addInvokestatic(Util.DISPATCHER_CLASS, "liftObjectToGlobalCarrier", Util.GLOBAL_CARRIER_CLASS, new CtClass[]{ Type.OBJECT.getCtClass() });
                        } else {
                            bytecode.addInvokestatic(Util.DISPATCHER_CLASS, "liftObjectToLocalCarrier", Util.LOCAL_CARRIER_CLASS, new CtClass[]{ Type.OBJECT.getCtClass() });
                        }
                    } else {
                        bytecode.addInvokestatic(Util.DISPATCHER_CLASS, Util.getLiftMethodName(expectedType), Util.VALUE_CLASS, new CtClass[]{ expectedType.getCtClass() });
                    }

                    break;
                }
                default: {
                    // By current design only the above case should occur here!
                    throw new RuntimeException();
                }
            }
        }
    }

    private static void addMaterializationCode(Type expectedType, Bytecode bytecode, WeaveType weaveType) {
        if (expectedType.isReference()) {
            switch (weaveType) {
                case MAYBE_LOCALLY_CARRYING: {
                    bytecode.addInvokestatic(Util.DISPATCHER_CLASS, "materializeMaybeLocalCarrierChecked", Type.OBJECT.getCtClass(), new CtClass[]{ Type.OBJECT.getCtClass() });
                    break;
                }
                case LOCALLY_CARRYING: {
                    bytecode.addInvokestatic(Util.DISPATCHER_CLASS, "materializeLocalCarrier", Type.OBJECT.getCtClass(), new CtClass[]{ Util.LOCAL_CARRIER_CLASS });
                    break;
                }
                default: {
                    // By current design only the above cases should occur here!
                    throw new RuntimeException();
                }
            }
            if (!expectedType.getCtClass().equals(Type.OBJECT.getCtClass())) {
                bytecode.addCheckcast(expectedType.getCtClass());
            }
        } else {
            if (expectedType.equals(Type.BOOLEAN)) {
                bytecode.addInvokestatic(Util.DISPATCHER_CLASS, "materializeAsBoolean", Type.BOOLEAN.getCtClass(), new CtClass[] { Util.EXPRESSION_CLASS });
            } else if (expectedType.equals(Type.INT)) {
                bytecode.addInvokestatic(Util.DISPATCHER_CLASS, "materializeAsInteger", Type.INT.getCtClass(), new CtClass[] { Util.EXPRESSION_CLASS });
            } else if (expectedType.equals(Type.LONG)) {
                bytecode.addInvokestatic(Util.DISPATCHER_CLASS, "materializeAsLong", Type.LONG.getCtClass(), new CtClass[] { Util.EXPRESSION_CLASS });
            } else if (expectedType.equals(Type.FLOAT)) {
                bytecode.addInvokestatic(Util.DISPATCHER_CLASS, "materializeAsFloat", Type.FLOAT.getCtClass(), new CtClass[] { Util.EXPRESSION_CLASS });
            } else if (expectedType.equals(Type.DOUBLE)) {
                bytecode.addInvokestatic(Util.DISPATCHER_CLASS, "materializeAsDouble", Type.DOUBLE.getCtClass(), new CtClass[] { Util.EXPRESSION_CLASS });
            } else if (expectedType.equals(Type.BYTE)) {
                bytecode.addInvokestatic(Util.DISPATCHER_CLASS, "materializeAsByte", Type.BYTE.getCtClass(), new CtClass[]{ Util.EXPRESSION_CLASS });
            } else if (expectedType.equals(Type.CHAR)) {
                bytecode.addInvokestatic(Util.DISPATCHER_CLASS, "materializeAsCharacter", Type.CHAR.getCtClass(), new CtClass[] { Util.EXPRESSION_CLASS });
            } else if (expectedType.equals(Type.SHORT)) {
                bytecode.addInvokestatic(Util.DISPATCHER_CLASS, "materializeAsShort", Type.SHORT.getCtClass(), new CtClass[] { Util.EXPRESSION_CLASS });
            }
        }
    }

    /*
    private static void deleteInstruction(CodeIterator codeIterator, int position) throws BadBytecode {
        codeIterator.move(position);
        int next;
        if (codeIterator.hasNext()) {
            codeIterator.next();
            next = codeIterator.lookAhead() + (codeIterator.byteAt(position) == WIDE ? 1 : 0);
        } else {
            next = codeIterator.getCodeLength();
        }
        codeIterator.move(position);

        for (int i = position; i < next; i++) {
            codeIterator.writeByte(Opcode.NOP, i);
        }
    }
    */

    private static void insertAfter(CodeIterator codeIterator, int position, byte[] code) throws BadBytecode {
        codeIterator.move(position);
        int next;
        if (codeIterator.hasNext()) {
            codeIterator.next();
            next = codeIterator.lookAhead() + (codeIterator.byteAt(position) == WIDE ? 1 : 0);
        } else {
            next = codeIterator.getCodeLength();
        }
        codeIterator.move(position);

        codeIterator.insertEx(next, code);
    }

    private static void replaceInstruction(CodeIterator codeIterator, int position, byte[] code) throws BadBytecode {
        codeIterator.move(position);
        int next;
        if (codeIterator.hasNext()) {
            codeIterator.next();
            next = codeIterator.lookAhead();
        } else {
            next = codeIterator.getCodeLength() + (codeIterator.byteAt(position) == WIDE ? 1 : 0);
        }
        codeIterator.move(position);

        if (position + code.length >= next) {
            int extend = position + code.length - next;;
            codeIterator.insertGap(position, extend);
            next += extend;
        }
        int i = 0;
        for (; i < code.length && position + i < next; i++) {
            codeIterator.writeByte(code[i], position + i);
        }
        for (int j = position + i; j < next; j++) {
            codeIterator.writeByte(Opcode.NOP, j);
        }
    }

    // The order matters for precedence!
    private enum WeaveOperationType {
        INSERT_AFTER,
        REWRITE,
        INSERT_BEFORE,
        INSERT_BEFORE_EXCLUSIVE
    }

    private static class WeaveOperation implements Comparable<WeaveOperation> {
        private final int position;
        private final WeaveOperationType type;
        private final Bytecode bytecode;
        private final int precedence;

        private WeaveOperation(int position, WeaveOperationType type, Bytecode bytecode, int precedence) {
            this.position = position;
            this.type = type;
            this.bytecode = bytecode;
            this.precedence = precedence;
        }

        private WeaveOperation(int position, WeaveOperationType type, Bytecode bytecode) {
            this(position, type, bytecode, 0);
        }

        int getPosition() {
            return position;
        }

        WeaveOperationType getType() {
            return type;
        }

        Bytecode getBytecode() {
            return bytecode;
        }

        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (!(obj instanceof WeaveOperation)) { return false; }
            WeaveOperation w = (WeaveOperation) obj;
            return position == w.position && type.equals(w.type) && bytecode == w.bytecode && precedence == w.precedence;
        }

        public int compareTo(WeaveOperation o) {
            if (position > o.position) {
                return -1;
            } else if (position < o.position) {
                return 1;
            } else {
                if (type.compareTo(o.type) == 0) {
                    if (precedence > o.precedence) {
                        return -1;
                    } else {
                        return 1;
                    }
                } else {
                    return type.compareTo(o.type);
                }
            }
        }
    }

    final class Result {
        private final TreeSet<WeaveOperation> weaveOperations;

        private int additionalLocals;
        private int tempLocalsStart;

        private Result() {
            tempLocalsStart = maxLocals;

            weaveOperations = new TreeSet<>();

            if (!traceRecordPositions.isEmpty()) {
                int traceIndex = maxLocals;
                tempLocalsStart++;

                Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());

                bytecode.add(ACONST_NULL);
                bytecode.addStore(traceIndex, Util.TRACE_CLASS);

                weaveOperations.add(new WeaveOperation(0, WeaveOperationType.INSERT_BEFORE_EXCLUSIVE, bytecode, -1));

                for (int position : traceRecordPositions) {
                    bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());
                    bytecode.addLoad(traceIndex, Util.TRACE_CLASS);
                    bytecode.addIconst(position);
                    bytecode.addInvokestatic(Util.DISPATCHER_CLASS, "traceRecord", Util.TRACE_CLASS, new CtClass[]{ Util.TRACE_CLASS, CtClass.intType });
                    bytecode.addStore(traceIndex, Util.TRACE_CLASS);

                    WeaveOperationType weaveOperationType;
                    weaveOperationType = WeaveOperationType.INSERT_BEFORE;

                    weaveOperations.add(new WeaveOperation(position, weaveOperationType, bytecode));
                }
            }

            // Handling the special case for adjusting the initial state, i.e. the arguments
            if (initialStateLocalLifts != null && !initialStateLocalLifts.isEmpty()) {
                Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());

                for (Integer index : initialStateLocalLifts) {
                    Type originalType = typeAnalyzerResult.getLocal(0, index);
                    WeaveType weaveType = WeaveType.forTypeOriginal(originalType);

                    bytecode.addLoad(index, originalType.getCtClass());
                    addAdjustmentCode(originalType, bytecode, weaveType, false);
                    bytecode.addStore(index, Type.OBJECT.getCtClass());
                }

                weaveOperations.add(new WeaveOperation(0, WeaveOperationType.INSERT_BEFORE_EXCLUSIVE, bytecode));
            }

            for (int position = 0; position < WeaveAnalyzer.this.instructionRewrites.length; position++) {
                // Handling instruction rewrites

                Bytecode rewriteBytecode = WeaveAnalyzer.this.instructionRewrites[position];
                if (rewriteBytecode != null) {
                    weaveOperations.add(new WeaveOperation(position, WeaveOperationType.REWRITE, rewriteBytecode));
                }

                // Handling lifts...

                TreeSet<Integer> postInstructionLocalLiftIndices = postInstructionLocalLifts[position];
                if (postInstructionLocalLiftIndices != null) {
                    Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());

                    for (Integer index : postInstructionLocalLiftIndices) {
                        Type originalType = typeAnalyzerResult.getLocalAfter(position, index);

                        bytecode.addLoad(index, originalType.getCtClass());
                        addAdjustmentCode(originalType, bytecode, WeaveType.NOT_CARRYING, false);
                        bytecode.addStore(index, Type.OBJECT.getCtClass());
                    }

                    weaveOperations.add(new WeaveOperation(position, WeaveOperationType.INSERT_AFTER, bytecode));
                }

                if (postInstructionStackTopLifts[position]) {
                    Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());

                    Type originalType = typeAnalyzerResult.getStackAfter(position, 0);
                    if (originalType == Type.TOP) {
                        originalType = typeAnalyzerResult.getStackAfter(position, 1);
                    }

                    addAdjustmentCode(originalType, bytecode, WeaveType.NOT_CARRYING, false);

                    weaveOperations.add(new WeaveOperation(position, WeaveOperationType.INSERT_AFTER, bytecode));
                }

                SourceIndex postInitializationInstructionLiftSourceIndex = postInitializationInstructionLifts[position];
                if (postInitializationInstructionLiftSourceIndex != null) {
                    Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());

                    // We only lift one and copy its reference instead of lifting the other occurrences
                    // We store the first occurrence at the end, i.e. after the local variables
                    int initializedObjectLocalIndex = -1;
                    boolean initializedObjectStored = false;

                    if (postInitializationInstructionLiftSourceIndex.hasLocalIndices()) {
                        // Handle local lifting
                        if (postInitializationInstructionLiftSourceIndex.getLocalIndexCount() == 1) {
                            int index = postInitializationInstructionLiftSourceIndex.getLocalIndex(0);

                            bytecode.addLoad(index, Type.OBJECT.getCtClass());
                            addAdjustmentCode(Type.OBJECT, bytecode, WeaveType.NOT_CARRYING, false);
                            bytecode.add(DUP);
                            bytecode.addStore(index, Type.OBJECT.getCtClass());
                            initializedObjectStored = true;
                            initializedObjectLocalIndex = index;
                        }

                        for (int i = 1; i < postInitializationInstructionLiftSourceIndex.getLocalIndexCount(); i++) {
                            int index = postInitializationInstructionLiftSourceIndex.getLocalIndex(i);

                            bytecode.addLoad(initializedObjectLocalIndex, Type.OBJECT.getCtClass());
                            bytecode.addStore(index, Type.OBJECT.getCtClass());
                        }
                    }

                    if (postInitializationInstructionLiftSourceIndex.hasStackOffsets()) {
                        // Handle stack lifting
                        int lastOffset = postInitializationInstructionLiftSourceIndex.getStackOffset(postInitializationInstructionLiftSourceIndex.getStackOffsetCount() - 1);
                        if (lastOffset == 0) {
                            if (initializedObjectStored) {
                                bytecode.add(POP);
                                bytecode.addLoad(initializedObjectLocalIndex, Type.OBJECT.getCtClass());
                            } else {
                                addAdjustmentCode(Type.OBJECT, bytecode, WeaveType.NOT_CARRYING, false);
                            }
                        } else if (lastOffset == 1) {
                            Type originalType1 = typeAnalyzerResult.getStackAfter(position, 1);
                            Type originalType0 = typeAnalyzerResult.getStackAfter(position, 0);

                            if (initializedObjectStored) {
                                bytecode.add(POP);
                                bytecode.add(POP);
                                bytecode.addLoad(initializedObjectLocalIndex, Type.OBJECT.getCtClass());
                                bytecode.addLoad(initializedObjectLocalIndex, Type.OBJECT.getCtClass());
                            } else {
                                bytecode.add(POP);
                                bytecode.add(POP);
                                addAdjustmentCode(Type.OBJECT, bytecode, WeaveType.NOT_CARRYING, false);
                                bytecode.add(DUP);
                            }
                        } else {
                            int[] helperLocalIndices = new int[lastOffset + 1];
                            Type[] helperLocalTypes = new Type[lastOffset + 1];
                            int logicalIndex = 0;
                            int realIndex = 0;

                            int nextOffsetIndex = 0;

                            for (int i = 0; i <= lastOffset; i++) {
                                Type originalType = typeAnalyzerResult.getStackAfter(position, i);
                                if (originalType.equals(Type.TOP)) {
                                    continue;
                                }

                                int nextOffset = postInitializationInstructionLiftSourceIndex.getStackOffset(nextOffsetIndex);
                                if (i == nextOffset) {
                                    if (initializedObjectStored) {
                                        helperLocalIndices[logicalIndex] = initializedObjectLocalIndex - maxLocals;
                                        helperLocalTypes[logicalIndex] = originalType;

                                        bytecode.add(POP);
                                    } else {
                                        helperLocalIndices[logicalIndex] = realIndex;
                                        helperLocalTypes[logicalIndex] = originalType;

                                        addAdjustmentCode(Type.OBJECT, bytecode, WeaveType.NOT_CARRYING, false);

                                        initializedObjectStored = true;
                                        // We know that will be the index
                                        initializedObjectLocalIndex = maxLocals + realIndex;

                                        bytecode.addStore(tempLocalsStart + realIndex, originalType.getCtClass());
                                    }

                                    nextOffsetIndex++;
                                } else {
                                    helperLocalIndices[logicalIndex] = realIndex;
                                    helperLocalTypes[logicalIndex] = originalType;

                                    bytecode.addStore(tempLocalsStart + realIndex, originalType.getCtClass());

                                    realIndex += originalType.isTwoWordPrimitive() ? 2 : 1;
                                }

                                logicalIndex++;
                            }

                            additionalLocals = Math.max(additionalLocals, realIndex);

                            for (int i = logicalIndex - 1; i >= 0; i--) {
                                Type type = helperLocalTypes[i];

                                bytecode.addLoad(tempLocalsStart + helperLocalIndices[i], type.getCtClass());
                            }
                        }
                    }

                    weaveOperations.add(new WeaveOperation(position, WeaveOperationType.INSERT_AFTER, bytecode));
                }

                if (postCatchStackTopLifts[position]) {
                    Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());

                    Type originalType = typeAnalyzerResult.getStack(position, 0);
                    if (originalType == Type.TOP) {
                        originalType = typeAnalyzerResult.getStack(position, 1);
                    }

                    addAdjustmentCode(originalType, bytecode, WeaveType.NOT_CARRYING, false);

                    weaveOperations.add(new WeaveOperation(position, WeaveOperationType.INSERT_BEFORE, bytecode));
                }


                // Handling pre-instruction adjustments and materializations...

                TreeMap<Integer, Type> preInstructionLocalMaterializationIndicesAndTypes = preInstructionLocalMaterializations[position];
                if (preInstructionLocalMaterializationIndicesAndTypes != null) {
                    Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());

                    for (Map.Entry<Integer, Type> entry : preInstructionLocalMaterializationIndicesAndTypes.entrySet()) {
                        int index = entry.getKey();
                        Type originalType = entry.getValue();

                        WeaveType weaveType = getInState(position).getLocal(index);

                        bytecode.addLoad(index, Type.OBJECT.getCtClass());
                        addMaterializationCode(originalType, bytecode, weaveType);
                        bytecode.addStore(index, originalType.getCtClass());
                    }

                    weaveOperations.add(new WeaveOperation(position, WeaveOperationType.INSERT_BEFORE, bytecode));
                }

                TreeMap<Integer, Type> preInstructionStackAdjustmentOffsetsAndTypes = preInstructionStackAdjustments[position];
                if (preInstructionStackAdjustmentOffsetsAndTypes != null) {
                    Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());

                    Map.Entry<Integer, Type> lastEntry = preInstructionStackAdjustmentOffsetsAndTypes.lastEntry();

                    int lastOffset = lastEntry.getKey();
                    if (lastOffset == 0) {
                        Type originalType = lastEntry.getValue();
                        boolean isConstant = constantAnalyzerResult.isConstantStack(position, 0);
                        WeaveType weaveType = getInState(position).getStack(0);

                        addAdjustmentCode(originalType, bytecode, weaveType, isConstant);
                    } else if (lastOffset == 1) {
                        Type originalType1 = lastEntry.getValue();
                        WeaveType weaveType1 = getInState(position).getStack(1);
                        boolean isConstant1 = constantAnalyzerResult.isConstantStack(position, 1);
                        if (originalType1.isTwoWordPrimitive()) {
                            addAdjustmentCode(originalType1, bytecode, weaveType1, isConstant1);
                        } else {
                            Type originalType0 = preInstructionStackAdjustmentOffsetsAndTypes.get(0);
                            if (originalType0 != null) {
                                WeaveType weaveType0 = getInState(position).getStack(0);
                                boolean isConstant0 = constantAnalyzerResult.isConstantStack(position, 0);

                                addAdjustmentCode(originalType0, bytecode, weaveType0, isConstant0);
                            }
                            bytecode.add(SWAP);
                            addAdjustmentCode(originalType1, bytecode, weaveType1, isConstant1);
                            bytecode.add(SWAP);
                        }
                    } else {
                        int[] helperLocalIndices = new int[lastOffset + 1];
                        Type[] helperLocalTypes = new Type[lastOffset + 1];
                        int logicalIndex = 0;
                        int realIndex = 0;

                        for (int i = 0; i <= lastOffset; i++) {
                            boolean adjust = false;

                            Type originalType = preInstructionStackAdjustmentOffsetsAndTypes.get(i);
                            if (originalType == null) {
                                originalType = typeAnalyzerResult.getStack(position, i);
                                if (originalType.equals(Type.TOP)) {
                                    continue;
                                }
                            } else {
                                adjust = true;
                            }

                            helperLocalIndices[logicalIndex] = realIndex;

                            if (adjust) {
                                helperLocalTypes[logicalIndex] = Type.OBJECT;

                                WeaveType weaveType = getInState(position).getStack(i);
                                boolean isConstant = constantAnalyzerResult.isConstantStack(position, i);

                                addAdjustmentCode(originalType, bytecode, weaveType, isConstant);
                                bytecode.addStore(tempLocalsStart + realIndex, Type.OBJECT.getCtClass());

                                realIndex++;
                            } else {
                                helperLocalTypes[logicalIndex] = originalType;

                                bytecode.addStore(tempLocalsStart + realIndex, originalType.getCtClass());

                                realIndex += originalType.isTwoWordPrimitive() ? 2 : 1;
                            }

                            logicalIndex++;
                        }

                        additionalLocals = Math.max(additionalLocals, realIndex);

                        for (int i = logicalIndex - 1; i >= 0; i--) {
                            Type type = helperLocalTypes[i];

                            bytecode.addLoad(tempLocalsStart + helperLocalIndices[i], type.getCtClass());
                        }
                    }

                    weaveOperations.add(new WeaveOperation(position, WeaveOperationType.INSERT_BEFORE, bytecode));
                }

                TreeMap<Integer, Type> preInstructionStackMaterializationOffsetsAndTypes = preInstructionStackMaterializations[position];
                if (preInstructionStackMaterializationOffsetsAndTypes != null) {
                    Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());

                    Map.Entry<Integer, Type> lastEntry = preInstructionStackMaterializationOffsetsAndTypes.lastEntry();

                    int lastOffset = lastEntry.getKey();
                    if (lastOffset == 0) {
                        Type originalType = lastEntry.getValue();
                        WeaveType weaveType = getInState(position).getStack(0);

                        addMaterializationCode(originalType, bytecode, weaveType);
                    } else if (lastOffset == 1) {
                        Type originalType1 = lastEntry.getValue();
                        WeaveType weaveType1 = getInState(position).getStack(1);
                        if (originalType1.isTwoWordPrimitive()) {
                            addMaterializationCode(originalType1, bytecode, weaveType1);
                        } else {
                            Type originalType0 = preInstructionStackMaterializationOffsetsAndTypes.get(0);
                            WeaveType weaveType0 = getInState(position).getStack(0);
                            if (originalType0 != null) {
                                addMaterializationCode(originalType0, bytecode, weaveType0);
                            }
                            bytecode.add(SWAP);
                            addMaterializationCode(originalType1, bytecode, weaveType1);
                            bytecode.add(SWAP);
                        }
                    } else {
                        int[] helperLocalIndices = new int[lastOffset + 1];
                        Type[] helperLocalTypes = new Type[lastOffset + 1];
                        int logicalIndex = 0;
                        int realIndex = 0;

                        for (int i = 0; i <= lastOffset; i++) {
                            boolean materialize = false;

                            Type originalType = preInstructionStackMaterializationOffsetsAndTypes.get(i);
                            if (originalType == null) {
                                originalType = typeAnalyzerResult.getStack(position, i);
                                if (originalType.equals(Type.TOP)) {
                                    continue;
                                }
                            } else {
                                materialize = true;
                            }

                            helperLocalIndices[logicalIndex] = realIndex;
                            helperLocalTypes[logicalIndex] = originalType;

                            if (materialize) {
                                WeaveType weaveType = getInState(position).getStack(i);
                                addMaterializationCode(originalType, bytecode, weaveType);
                            }

                            bytecode.addStore(tempLocalsStart + realIndex, originalType.getCtClass());

                            realIndex += originalType.isTwoWordPrimitive() ? 2 : 1;

                            logicalIndex++;
                        }

                        additionalLocals = Math.max(additionalLocals, realIndex);

                        for (int i = logicalIndex - 1; i >= 0; i--) {
                            Type type = helperLocalTypes[i];

                            bytecode.addLoad(tempLocalsStart + helperLocalIndices[i], type.getCtClass());
                        }
                    }

                    weaveOperations.add(new WeaveOperation(position, WeaveOperationType.INSERT_BEFORE, bytecode));
                }
            }
        }

        void weave() throws BadBytecode {
            CodeAttribute codeAttr = behavior.getMethodInfo().getCodeAttribute();
            CodeIterator codeIt = codeAttr.iterator();

            for (WeaveOperation weaveOperation : weaveOperations) {
                switch (weaveOperation.getType()) {
                    case INSERT_BEFORE: {
                        codeIt.insert(weaveOperation.getPosition(), weaveOperation.getBytecode().get());
                        break;
                    }
                    case INSERT_BEFORE_EXCLUSIVE: {
                        codeIt.insertEx(weaveOperation.getPosition(), weaveOperation.getBytecode().get());
                        break;
                    }
                    case INSERT_AFTER: {
                        insertAfter(codeIt, weaveOperation.getPosition(), weaveOperation.getBytecode().get());
                        break;
                    }
                    case REWRITE: {
                        replaceInstruction(codeIt, weaveOperation.getPosition(), weaveOperation.getBytecode().get());
                        // deleteInstruction(codeIt, weaveOperation.getPosition());
                        // codeIt.insert(weaveOperation.getPosition(), weaveOperation.getBytecode().get());
                        break;
                    }
                }
            }

            try {
                for (CtField field : stagedToStaticInfoField.values()) {
                    clazz.addField(field);
                }
                for (CtField field : permCachableStagedToClosureHolderField.values()) {
                    clazz.addField(field);
                }
                for (CtField field : traceCachableStagedToCacheField.values()) {
                    clazz.addField(field);
                }
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }

            behavior.getMethodInfo().getCodeAttribute().setMaxLocals(tempLocalsStart + additionalLocals);
            behavior.getMethodInfo().getCodeAttribute().computeMaxStack();
            behavior.getMethodInfo().rebuildStackMap(ClassPool.getDefault());
        }
    }

    WeaveAnalyzer(TypeAnalyzer typeAnalyzer, StageGraph stageGraph, ValueFlowAnalyzer.Result valueFlowResult, ConstantAnalyzer.Result constantAnalyzerResult, CachabilityAnalyzer.Result cachabilityAnalyzerResult, LiftEstimateAnalyzer.Result liftEstimateAnalyzerResult) {
        super(typeAnalyzer);

        this.typeAnalyzerResult = typeAnalyzer.getResult();
        this.stageGraph = stageGraph;
        this.valueFlowResult = valueFlowResult;
        this.constantAnalyzerResult = constantAnalyzerResult;
        this.cachabilityAnalyzerResult = cachabilityAnalyzerResult;
        this.liftEstimateAnalyzerResult = liftEstimateAnalyzerResult;

        MethodInfo methodInfo = behavior.getMethodInfo2();

        maxLocals = codeAttribute.getMaxLocals();
        int maxStack = codeAttribute.getMaxStack();
        int codeLength = codeAttribute.getCodeLength();


        preInstructionStackAdjustments = (TreeMap<Integer, Type>[]) new TreeMap[codeLength];
        preInstructionStackMaterializations = (TreeMap<Integer, Type>[]) new TreeMap[codeLength];
        preInstructionLocalMaterializations = (TreeMap<Integer, Type>[]) new TreeMap[codeLength];

        initialStateLocalLifts = new TreeSet<>();
        postInstructionStackTopLifts = new boolean[codeLength];
        postInitializationInstructionLifts = new SourceIndex[codeLength];
        postCatchStackTopLifts = new boolean[codeLength];
        postInstructionLocalLifts = (TreeSet<Integer>[]) new TreeSet[codeLength];

        instructionRewrites = new Bytecode[codeLength];

        permCachableStagedToClosureHolderField = new HashMap<>();
        traceCachableStagedToCacheField = new HashMap<>();
        stagedToStaticInfoField = new HashMap<>();

        traceRecordPositions = new HashSet<>();


        if (behavior instanceof CtMethod) {
            try {
                returnType = Type.of(((CtMethod) behavior).getReturnType());
            } catch (NotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            returnType = Type.VOID;
        }

        initialState = new WeaveFrame(maxLocals, maxStack);

        int pos = 0;
        if (!Modifier.isStatic(behavior.getModifiers())) {
            if (behavior instanceof CtConstructor) {
                initialState.setLocal(pos, WeaveType.NOT_CARRYING);
            } else {
                initialState.setLocal(pos, liftInitialStateLocal(pos, WeaveType.forTypeOriginal(Type.of(clazz)), Type.of(clazz)));
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
                initialState.setLocal2(pos, liftInitialStateLocal(pos, WeaveType.forTypeOriginal(type), type));
                pos += 2;
            } else {
                initialState.setLocal(pos, liftInitialStateLocal(pos, WeaveType.forTypeOriginal(type), type));
                pos++;
            }
        }
    }

    Result getResult() {
        return new Result();
    }

    private void resetPreInstruction(int at) {
        preInstructionStackAdjustments[at] = null;
        preInstructionStackMaterializations[at] = null;
        preInstructionLocalMaterializations[at] = null;
        instructionRewrites[at] = null;
    }

    private WeaveType liftInitialStateLocal(int localIndex, WeaveType value, Type originalType) {
        if (value == WeaveType.NOT_CARRYING && !Util.isCarrier(originalType)) {
            if (liftEstimateAnalyzerResult.mayRequireLift(SourceIndex.makeImplicitLocal(0, localIndex))) {
                initialStateLocalLifts.add(localIndex);

                return WeaveType.LOCALLY_CARRYING;
            }
        }

        return value;
    }
    private WeaveType liftStackTopPostInstruction(int at, WeaveType value, Type originalType) {
        if (originalType.equals(Type.VOID)) {
            return value;
        }
        if (value == WeaveType.NOT_CARRYING && !Util.isCarrier(originalType)) {
            if (liftEstimateAnalyzerResult.mayRequireLift(SourceIndex.makeExplicitStackTop(at))) {
                postInstructionStackTopLifts[at] = true;

                return WeaveType.LOCALLY_CARRYING;
            }
        }

        return value;
    }
    private WeaveType liftPostInitializationInstruction(SourceIndex sourceIndex, WeaveType value, Type originalType) {
        if (value == WeaveType.NOT_CARRYING && !Util.isCarrier(originalType)) {
            if (liftEstimateAnalyzerResult.mayRequireLift(sourceIndex)) {
                postInitializationInstructionLifts[sourceIndex.getPosition()] = sourceIndex;

                return WeaveType.LOCALLY_CARRYING;
            }
        }

        return value;
    }
    private WeaveType liftStackTopPostCatch(int at, WeaveType value, Type originalType) {
        if (value == WeaveType.NOT_CARRYING && !Util.isCarrier(originalType)) {
            if (liftEstimateAnalyzerResult.mayRequireLift(SourceIndex.makeImplicitStackTop(at))) {
                postCatchStackTopLifts[at] = true;

                return WeaveType.LOCALLY_CARRYING;
            }
        }

        return value;
    }
    private WeaveType liftLocalPostInstruction(int at, WeaveType local, int localIndex, Type originalType) {
        if (local == WeaveType.NOT_CARRYING && !Util.isCarrier(originalType)) {
            if (liftEstimateAnalyzerResult.mayRequireLift(SourceIndex.makeExplicitLocal(at, localIndex))) {
                TreeSet<Integer> l = postInstructionLocalLifts[at];
                if (l == null) {
                    l = new TreeSet<>();
                    postInstructionLocalLifts[at] = l;
                }

                l.add(localIndex);

                return WeaveType.LOCALLY_CARRYING;
            }
        }

        return local;
    }

    private void adjustStackPreInstruction(int at, WeaveType value, int valueOffset, Type expectedType) {
        if ((value == WeaveType.NOT_CARRYING && !Util.isCarrier(expectedType)) ||
                constantAnalyzerResult.isConstantStack(at, valueOffset)) {
            if (preInstructionStackMaterializations[at] != null ||
                    preInstructionLocalMaterializations[at] != null) {
                throw new RuntimeException();
            }

            TreeMap<Integer, Type> l = preInstructionStackAdjustments[at];
            if (l == null) {
                l = new TreeMap<>();
                preInstructionStackAdjustments[at] = l;
            }

            l.put(valueOffset, expectedType);
        }
    }
    private void materializeStackPreInstruction(int at, WeaveType value, int valueOffset, Type expectedType) {
        if (value.attemptMaterialization()) {
            if (preInstructionStackAdjustments[at] != null ||
                    preInstructionLocalMaterializations[at] != null) {
                throw new RuntimeException();
            }

            TreeMap<Integer, Type> l = preInstructionStackMaterializations[at];
            if (l == null) {
                l = new TreeMap<>();
                preInstructionStackMaterializations[at] = l;
            }

            l.put(valueOffset, expectedType);
        }
    }
    private void materializeLocalPreInstruction(int at, WeaveType local, int localIndex, Type expectedType) {
        if (local.attemptMaterialization()) {
            if (preInstructionStackAdjustments[at] != null ||
                    preInstructionStackMaterializations[at] != null) {
                throw new RuntimeException();
            }

            TreeMap<Integer, Type> l = preInstructionLocalMaterializations[at];
            if (l == null) {
                l = new TreeMap<>();
                preInstructionLocalMaterializations[at] = l;
            }

            l.put(localIndex, expectedType);
        }
    }

    private void rewriteToAdjustedConversion(int at, Type from, Type to) {
        // TODO: Only add expression conversion when necessary (depending on uses)...

        Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());

        if (!to.isReference()) {
            bytecode.addInvokestatic(Util.DISPATCHER_CLASS, Util.getConversionMethodName(to), Util.EXPRESSION_CLASS, new CtClass[]{ Util.EXPRESSION_CLASS });
        }

        rewriteInstruction(at, bytecode);
    }
    private void rewriteInstruction(int at, Bytecode bytecode) {
        instructionRewrites[at] = bytecode;
    }

    private void addArgumentPassingAndStaticInfoAndClosureHolderCode(Source.Staged staged, Type targetType, Bytecode bytecode) {
        if (staged.getArguments().size() > 253) {
            bytecode.addAnewarray(Type.OBJECT.getCtClass(), staged.getArguments().size());
            for (int i = staged.getArguments().size() - 1; i >= 0; i--) {
                bytecode.add(DUP_X1);
                bytecode.add(SWAP);
                bytecode.addIconst(i);
                bytecode.add(SWAP);
                bytecode.add(AASTORE);
            }
        }

        CtField staticInfoField = stagedToStaticInfoField.get(staged);
        if (!staged.getStaticInfoElements().isEmpty()) {
            if (staticInfoField == null) {
                StaticInfo.Origin origin = null;
                if (staged.getStaticInfoElements().contains(StaticInfo.Element.ORIGIN)) {
                    origin = StaticInfo.Origin.make(behavior, staged.getSourceIndex().getPosition());
                }

                StaticInfo.InferredTypes inferredTypes = null;
                if (staged.getStaticInfoElements().contains(StaticInfo.Element.INFERRED_TYPES)) {
                    ImmutableList<Use.Argument> args = staged.getArguments();
                    Type[] inferredArgumentTypes = new Type[args.size()];

                    for (Use.Argument a : args) {
                        inferredArgumentTypes[a.getIndex()] = typeAnalyzerResult.get(a.getUseIndex());
                    }

                    Type inferredType = staged.getType();
                    for (Flow.Data outData : staged.getOutData()) {
                        if (inferredType.isAssignableFrom(outData.getTo().getType())) {
                            inferredType = outData.getTo().getType();
                        }
                    }

                    for (Flow.Data outData : staged.getOutData()) {
                        if (!outData.getTo().getType().isAssignableFrom(inferredType)) {
                            // This would mean that the only way to adhere to all types at uses is by being null.
                            // Note that it is very much possible that the conflicting path is never taken...
                            inferredType = Type.NULL;
                            break;
                        }
                    }

                    inferredTypes = StaticInfo.InferredTypes.make(inferredType, inferredArgumentTypes);
                }

                StaticInfo staticInfo = StaticInfo.make(origin, inferredTypes);

                int id = Dispatcher.addPersistent(staticInfo);

                // This gives us a nice name referring to the original position of the staged
                String fieldName = clazz.makeUniqueName("staticInfo") + "$" + behavior.getName() + "$" + staged.getSourceIndex().getPosition();

                String fieldSource = "private static final " + Util.STATIC_INFO_CLASS.getName() + " " + fieldName + " = (" + Util.STATIC_INFO_CLASS.getName() + ") " + Util.DISPATCHER_CLASS.getName() + ".removePersistent(" + id + ");";
                try {
                    staticInfoField = CtField.make(fieldSource, clazz);
                    stagedToStaticInfoField.put(staged, staticInfoField);
                } catch (CannotCompileException e) {
                    throw new RuntimeException(e);
                }
            }

            bytecode.addGetstatic(clazz, staticInfoField.getName(), staticInfoField.getFieldInfo2().getDescriptor());
        }

        if (cachabilityAnalyzerResult.isPermCachable(staged)) {
            CtField closureHolderField = permCachableStagedToClosureHolderField.get(staged);
            if (closureHolderField == null) {
                // This gives us a nice name referring to the original position of the staged
                String fieldName = clazz.makeUniqueName("closureHolder") + "$" + behavior.getName() + "$" + staged.getSourceIndex().getPosition();

                String fieldSource = "private static final " + Util.CLOSURE_HOLDER_CLASS.getName() + " " + fieldName + " = " + Util.DISPATCHER_CLASS.getName() + ".makeClosureHolder(true);";
                try {
                    closureHolderField = CtField.make(fieldSource, clazz);
                    permCachableStagedToClosureHolderField.put(staged, closureHolderField);
                } catch (CannotCompileException e) {
                    throw new RuntimeException(e);
                }
            }

            bytecode.addGetstatic(clazz, closureHolderField.getName(), closureHolderField.getFieldInfo2().getDescriptor());
        } else if (cachabilityAnalyzerResult.isTraceCachable(staged)) {
            CtField cacheField = traceCachableStagedToCacheField.get(staged);
            if (cacheField == null) {
                // This gives us a nice name referring to the original position of the staged
                String fieldName = clazz.makeUniqueName("traceCache") + "$" + behavior.getName() + "$" + staged.getSourceIndex().getPosition();

                String fieldSource = "private static final " + Util.TRACE_CACHE_CLASS.getName() + " " + fieldName + " = " + Util.DISPATCHER_CLASS.getName() + ".makeTraceCache(32);";
                try {
                    cacheField = CtField.make(fieldSource, clazz);
                    traceCachableStagedToCacheField.put(staged, cacheField);
                } catch (CannotCompileException e) {
                    throw new RuntimeException(e);
                }
            }

            bytecode.addGetstatic(clazz, cacheField.getName(), cacheField.getFieldInfo2().getDescriptor());
            bytecode.addLoad(maxLocals, Util.TRACE_CLASS);
            bytecode.addInvokestatic(Util.DISPATCHER_CLASS, "getTraceCachedClosureHolder", Util.CLOSURE_HOLDER_CLASS, new CtClass[]{Util.TRACE_CACHE_CLASS, Util.TRACE_CLASS});
        } else {
            bytecode.add(ACONST_NULL);
        }
    }


    protected WeaveFrame copyState(WeaveFrame original) { return original.copy(); }

    protected boolean stateEquals(WeaveFrame state, WeaveFrame otherState) { return state.equals(otherState); }

    protected WeaveFrame initialState() {
        return initialState;
    }

    protected WeaveFrame mergeStatesOnCatch(ArrayList<WeaveFrame> states, int[] origins, int at, Type caughtException) {
        // TODO: Only handle those merge points that matter! Track contributing merge points (or better, the origins) with tracked values earlier!
        if (origins.length > 1 && cachabilityAnalyzerResult.hasTracheCachableStageds()) {
            for (int position : origins) {
                traceRecordPositions.add(position);
            }
        }

        WeaveFrame mergedFrame = states.get(0).copyLocals();

        for (int i = 1; i < states.size(); i++) {
            mergedFrame.mergeLocals(states.get(i), origins[0], origins[i]);
        }

        mergedFrame.push(liftStackTopPostCatch(at, WeaveType.NOT_CARRYING, caughtException));

        return mergedFrame;
    }

    protected WeaveFrame mergeStates(ArrayList<WeaveFrame> states, int[] origins, int at) {
        // TODO: Only handle those merge points that matter...
        if (origins.length > 1 && cachabilityAnalyzerResult.hasTracheCachableStageds()) {
            for (int position : origins) {
                traceRecordPositions.add(position);
            }
        }

        resetPreInstruction(at);

        WeaveFrame mergedFrame = states.get(0).copy();

        for (int i = 1; i < states.size(); i++) {
            mergedFrame.merge(states.get(i), origins[0], origins[i]);
        }

        return mergedFrame;
    }

    protected WeaveType createNull(WeaveFrame state, int at) throws BadBytecode {
        return liftStackTopPostInstruction(at, WeaveType.NOT_CARRYING, Type.OBJECT);
    }

    protected WeaveType createIntegerConstant(WeaveFrame state, int at, int value) throws BadBytecode {
        return liftStackTopPostInstruction(at, WeaveType.NOT_CARRYING, Type.INT);
    }

    protected WeaveType createLongConstant(WeaveFrame state, int at, long value) throws BadBytecode {
        return liftStackTopPostInstruction(at, WeaveType.NOT_CARRYING, Type.LONG);
    }

    protected WeaveType createFloatConstant(WeaveFrame state, int at, float value) throws BadBytecode {
        return liftStackTopPostInstruction(at, WeaveType.NOT_CARRYING, Type.FLOAT);
    }

    protected WeaveType createDoubleConstant(WeaveFrame state, int at, double value) throws BadBytecode {
        return liftStackTopPostInstruction(at, WeaveType.NOT_CARRYING, Type.DOUBLE);
    }

    protected WeaveType createByteConstant(WeaveFrame state, int at, byte value) throws BadBytecode {
        return liftStackTopPostInstruction(at, WeaveType.NOT_CARRYING, Type.BYTE);
    }

    protected WeaveType createShortConstant(WeaveFrame state, int at, short value) throws BadBytecode {
        return liftStackTopPostInstruction(at, WeaveType.NOT_CARRYING, Type.SHORT);
    }

    protected WeaveType createStringConstant(WeaveFrame state, int at, String value) throws BadBytecode {
        return liftStackTopPostInstruction(at, WeaveType.NOT_CARRYING, STRING_TYPE);
    }

    protected WeaveType createClassConstant(WeaveFrame state, int at, CtClass value) throws BadBytecode {
        return liftStackTopPostInstruction(at, WeaveType.NOT_CARRYING, CLASS_TYPE);
    }

    protected WeaveType readLocal(WeaveFrame state, int at, Type type, int index, WeaveType local) throws BadBytecode {
        if (local != WeaveType.NOT_CARRYING && !type.isReference()) {
            Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());
            bytecode.addLoad(index, Util.EXPRESSION_CLASS);
            rewriteInstruction(at, bytecode);
        }
        return local;
    }

    protected WeaveType readArray(WeaveFrame state, int at, Type componentType, WeaveType array, int arrayOffset, WeaveType index, int indexOffset) throws BadBytecode {
        materializeStackPreInstruction(at, array, arrayOffset, getType(componentType.toString() + "[]", at));
        materializeStackPreInstruction(at, index, indexOffset, Type.INT);

        Type inferredType = typeAnalyzerResult.getStack(at, 0);

        return liftStackTopPostInstruction(at, WeaveType.forTypeOriginal(componentType), inferredType);
    }

    protected WeaveType assignLocal(WeaveFrame state, int at, Type type, int index, WeaveType value, int valueOffset) throws BadBytecode {
        if (value != WeaveType.NOT_CARRYING && !type.isReference()) {
            Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());
            bytecode.addStore(index, Util.EXPRESSION_CLASS);
            rewriteInstruction(at, bytecode);
        }
        return value;
    }

    protected void assignArray(WeaveFrame state, int at, Type componentType, WeaveType array, int arrayOffset, WeaveType index, int indexOffset, WeaveType value, int valueOffset) throws BadBytecode {
        materializeStackPreInstruction(at, array, arrayOffset, getType(componentType.toString() + "[]", at));
        materializeStackPreInstruction(at, index, indexOffset, Type.INT);
        materializeStackPreInstruction(at, value, valueOffset, componentType);
    }

    protected void pop2(WeaveFrame state, int at, WeaveType value0, WeaveType value1) throws BadBytecode {
       if (value1 != WeaveType.NOT_CARRYING) {
           Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());
           bytecode.add(POP);
           rewriteInstruction(at, bytecode);
       }
    }

    protected WeaveType duplicate2_0(WeaveFrame state, int at, WeaveType value0, WeaveType value1) throws BadBytecode {
        if (value1 != WeaveType.NOT_CARRYING) {
            Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());
            bytecode.add(DUP);
            rewriteInstruction(at, bytecode);
        }
        return value0;
    }

    protected WeaveType duplicate2_1(WeaveFrame state, int at, WeaveType value0, WeaveType value1) throws BadBytecode {
        return value1;
    }

    protected WeaveType duplicate2_X1_0(WeaveFrame state, int at, WeaveType value0, WeaveType value1, WeaveType value2) throws BadBytecode {
        if (value1 != WeaveType.NOT_CARRYING) {
            Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());
            bytecode.add(DUP_X1);
            rewriteInstruction(at, bytecode);
        }
        return value0;
    }

    protected WeaveType duplicate2_X1_1(WeaveFrame state, int at, WeaveType value0, WeaveType value1, WeaveType value2) throws BadBytecode {
        return value1;
    }

    protected WeaveType duplicate2_X2_0(WeaveFrame state, int at, WeaveType value0, WeaveType value1, WeaveType value2, WeaveType value3) throws BadBytecode {
        if (value1 != WeaveType.NOT_CARRYING) {
            Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());
            bytecode.add(DUP_X2);
            rewriteInstruction(at, bytecode);
        }
        return value0;
    }
    protected WeaveType duplicate2_X2_1(WeaveFrame state, int at, WeaveType value0, WeaveType value1, WeaveType value2, WeaveType value3) throws BadBytecode {
        return value1;
    }

    protected WeaveType swap_0(WeaveFrame state, int at, WeaveType value0, WeaveType value1) throws BadBytecode {
        return value0;
    }
    protected WeaveType swap_1(WeaveFrame state, int at, WeaveType value0, WeaveType value1) throws BadBytecode {
        return value1;
    }

    protected WeaveType performBinaryArithmetic(WeaveFrame state, int at, Type type, ArithmeticOperation operation, WeaveType left, int leftOffset, WeaveType right, int rightOffset) throws BadBytecode {
        materializeStackPreInstruction(at, left, leftOffset, type);
        materializeStackPreInstruction(at, right, rightOffset, type);

        return liftStackTopPostInstruction(at, WeaveType.NOT_CARRYING, type);
    }

    protected WeaveType performShift(WeaveFrame state, int at, Type type, ShiftOperation operation, WeaveType left, int leftOffset, WeaveType right, int rightOffset) throws BadBytecode {
        materializeStackPreInstruction(at, left, leftOffset, type);
        materializeStackPreInstruction(at, right, rightOffset, type);

        return liftStackTopPostInstruction(at, WeaveType.NOT_CARRYING, type);
    }

    protected WeaveType performNegation(WeaveFrame state, int at, Type type, WeaveType value, int valueOffset) throws BadBytecode {
        materializeStackPreInstruction(at, value, valueOffset, type);

        return liftStackTopPostInstruction(at, WeaveType.NOT_CARRYING, type);
    }

    protected WeaveType incrementLocal(WeaveFrame state, int at, int index, WeaveType local, int increment) throws BadBytecode {
        materializeLocalPreInstruction(at, local, index, Type.INT);

        return liftLocalPostInstruction(at, WeaveType.NOT_CARRYING, index, Type.INT);
    }

    protected WeaveType convertType(WeaveFrame state, int at, Type from, Type to, WeaveType value, int valueOffset) throws BadBytecode {
        Type inferredFrom = typeAnalyzerResult.getStack(at, valueOffset);
        if (Util.isSafeConversion(behavior, inferredFrom, to)) {
            if (value == WeaveType.LOCALLY_CARRYING) {
                rewriteToAdjustedConversion(at, from, to);
            }
            return liftStackTopPostInstruction(at, value, to);
        } else {
            materializeStackPreInstruction(at, value, valueOffset, inferredFrom);
            return liftStackTopPostInstruction(at, WeaveType.forTypeOriginal(to), to);
        }
    }

    protected WeaveType compare(WeaveFrame state, int at, Type type, WeaveType left, int leftOffset, WeaveType right, int rightOffset) throws BadBytecode {
        materializeStackPreInstruction(at, left, leftOffset, type);
        materializeStackPreInstruction(at, right, rightOffset, type);

        return liftStackTopPostInstruction(at, WeaveType.NOT_CARRYING, type);
    }

    protected WeaveType compare(WeaveFrame state, int at, Type type, ComparisonOperation operation, WeaveType left, int leftOffset, WeaveType right, int rightOffset) throws BadBytecode {
        materializeStackPreInstruction(at, left, leftOffset, type);
        materializeStackPreInstruction(at, right, rightOffset, type);

        return liftStackTopPostInstruction(at, WeaveType.NOT_CARRYING, type);
    }

    protected void branchIf(WeaveFrame state, int at, Type type, ComparisonOperation operation, WeaveType value, int valueOffset, int trueTarget, int falseTarget) throws BadBytecode {
        Type inferredType = typeAnalyzerResult.getStack(at, 0);
        materializeStackPreInstruction(at, value, valueOffset, inferredType);
    }

    protected void branchIfCompare(WeaveFrame state, int at, Type type, ComparisonOperation operation, WeaveType left, int leftOffset, WeaveType right, int rightOffset, int trueTarget, int falseTarget) throws BadBytecode {
        materializeStackPreInstruction(at, left, leftOffset, type);
        materializeStackPreInstruction(at, right, rightOffset, type);
    }

    protected void branchGoto(WeaveFrame state, int at, int target) throws BadBytecode {

    }

    protected WeaveType callSubroutine(WeaveFrame state, int at, int target) throws BadBytecode {
        return WeaveType.NOT_CARRYING;
    }

    protected void returnFromSubroutine(WeaveFrame state, int at, int index, WeaveType local) throws BadBytecode {
        // TODO: Not sure what to do...
    }

    protected void branchTableSwitch(WeaveFrame state, int at, WeaveType index, int indexOffset, int defaultTarget, int[]
            indexedTargets) throws BadBytecode {
        materializeStackPreInstruction(at, index, indexOffset, Type.INT);
    }

    protected void branchLookupSwitch(WeaveFrame state, int at, WeaveType key, int keyOffset, int defaultTarget, int[]
            matches, int[] matchTargets) throws BadBytecode {
        materializeStackPreInstruction(at, key, keyOffset, Type.INT);
    }

    protected void returnFromMethod(WeaveFrame state, int at, Type type, WeaveType value, int valueOffset) throws BadBytecode {
        materializeStackPreInstruction(at, value, valueOffset, returnType);
    }

    protected void returnFromMethod(WeaveFrame state, int at) throws BadBytecode {

    }

    protected WeaveType readStaticField(WeaveFrame state, int at, Type classType, Type fieldType, CtField field) throws BadBytecode {
        Source.Staged staged = stageGraph.getStaged(SourceIndex.makeExplicitStackTop(at));
        if (staged != null) {
            Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());

            addArgumentPassingAndStaticInfoAndClosureHolderCode(staged, fieldType, bytecode);
            CtMethod m = ExpressionClassFactory.getInvokeMethod(staged);
            bytecode.addInvokestatic(m.getDeclaringClass().getName(), m.getName(), m.getMethodInfo2().getDescriptor());

            if (staged.isStrict()) {
                addMaterializationCode(fieldType, bytecode, WeaveType.forTypeWoven(fieldType));

                rewriteInstruction(at, bytecode);
                return liftStackTopPostInstruction(at, WeaveType.forTypeOriginal(fieldType), fieldType);
            } else {
                rewriteInstruction(at, bytecode);
                return WeaveType.forTypeWoven(fieldType);
            }
        } else {
            return liftStackTopPostInstruction(at, WeaveType.forTypeOriginal(fieldType), fieldType);
        }
    }

    protected void assignStaticField(WeaveFrame state, int at, Type classType, Type fieldType, CtField field, WeaveType value, int valueOffset) throws BadBytecode {
        Source.Staged staged = stageGraph.getStaged(SourceIndex.makeExplicitStackTop(at));
        if (staged != null) {
            adjustStackPreInstruction(at, value, valueOffset, fieldType);

            Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());

            addArgumentPassingAndStaticInfoAndClosureHolderCode(staged, fieldType, bytecode);
            CtMethod m = ExpressionClassFactory.getInvokeMethod(staged);
            bytecode.addInvokestatic(m.getDeclaringClass().getName(), m.getName(), m.getMethodInfo2().getDescriptor());

            rewriteInstruction(at, bytecode);
        } else {
            materializeStackPreInstruction(at, value, valueOffset, fieldType);
        }
    }

    protected WeaveType readField(WeaveFrame state, int at, Type targetType, Type fieldType, CtField field, WeaveType targetObject, int targetObjectOffset) throws BadBytecode {
        Source.Staged staged = stageGraph.getStaged(SourceIndex.makeExplicitStackTop(at));
        if (staged != null) {
            adjustStackPreInstruction(at, targetObject, targetObjectOffset, targetType);

            Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());

            addArgumentPassingAndStaticInfoAndClosureHolderCode(staged, fieldType, bytecode);
            CtMethod m = ExpressionClassFactory.getInvokeMethod(staged);
            bytecode.addInvokestatic(m.getDeclaringClass().getName(), m.getName(), m.getMethodInfo2().getDescriptor());

            if (staged.isStrict()) {
                rewriteInstruction(at, bytecode);
                return liftStackTopPostInstruction(at, WeaveType.forTypeOriginal(fieldType), fieldType);
            } else {
                rewriteInstruction(at, bytecode);
                return WeaveType.forTypeWoven(fieldType);
            }
        } else {
            materializeStackPreInstruction(at, targetObject, targetObjectOffset, targetType);
            return liftStackTopPostInstruction(at, WeaveType.forTypeOriginal(fieldType), fieldType);
        }
    }

    protected void assignField(WeaveFrame state, int at, CtField field, Type targetType, Type fieldType, WeaveType targetObject, int targetObjectOffset, WeaveType value, int valueOffset) throws BadBytecode {
        Source.Staged staged = stageGraph.getStaged(SourceIndex.makeExplicitStackTop(at));
        if (staged != null) {
            adjustStackPreInstruction(at, targetObject, targetObjectOffset, targetType);
            adjustStackPreInstruction(at, value, valueOffset, fieldType);

            Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());

            addArgumentPassingAndStaticInfoAndClosureHolderCode(staged, fieldType, bytecode);
            CtMethod m = ExpressionClassFactory.getInvokeMethod(staged);
            bytecode.addInvokestatic(m.getDeclaringClass().getName(), m.getName(), m.getMethodInfo2().getDescriptor());

            rewriteInstruction(at, bytecode);
        } else {
            materializeStackPreInstruction(at, targetObject, targetObjectOffset, targetType);
            materializeStackPreInstruction(at, value, valueOffset, fieldType);
        }
    }

    protected WeaveType invokeStaticMethodExceptBoxing(WeaveFrame state, int at, CtMethod method, Type returnType, Type[] paramTypes, ArrayList<WeaveType> arguments, int[] argumentOffsets) throws BadBytecode {
        Source.Staged staged = stageGraph.getStaged(SourceIndex.makeExplicitStackTop(at));
        if (staged != null) {
            for (int i = 0; i < argumentOffsets.length; i++) {
                adjustStackPreInstruction(at, arguments.get(i), argumentOffsets[i], paramTypes[i]);
            }

            Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());

            addArgumentPassingAndStaticInfoAndClosureHolderCode(staged, returnType, bytecode);
            CtMethod m = ExpressionClassFactory.getInvokeMethod(staged);
            bytecode.addInvokestatic(m.getDeclaringClass().getName(), m.getName(), m.getMethodInfo2().getDescriptor());

            if (staged.isStrict()) {
                rewriteInstruction(at, bytecode);
                return liftStackTopPostInstruction(at, WeaveType.forTypeOriginal(returnType), returnType);
            } else {
                rewriteInstruction(at, bytecode);
                return WeaveType.forTypeWoven(returnType);
            }
        } else {
            for (int i = 0; i < argumentOffsets.length; i++) {
                materializeStackPreInstruction(at, arguments.get(i), argumentOffsets[i], paramTypes[i]);
            }

            return liftStackTopPostInstruction(at, WeaveType.forTypeOriginal(returnType), returnType);
        }
    }

    protected WeaveType invokeMethodExceptUnboxing(WeaveFrame state, int at, CtMethod method, Type targetType, Type returnType, Type[] paramTypes, WeaveType targetObject, int targetObjectOffset, ArrayList<WeaveType> arguments, int[] argumentOffsets) throws BadBytecode {
        Source.Staged staged = stageGraph.getStaged(SourceIndex.makeExplicitStackTop(at));
        if (staged != null) {
            adjustStackPreInstruction(at, targetObject, targetObjectOffset, targetType);
            for (int i = 0; i < argumentOffsets.length; i++) {
                adjustStackPreInstruction(at, arguments.get(i), argumentOffsets[i], paramTypes[i]);
            }

            Bytecode bytecode = new Bytecode(behavior.getMethodInfo().getConstPool());

            addArgumentPassingAndStaticInfoAndClosureHolderCode(staged, returnType, bytecode);
            CtMethod m = ExpressionClassFactory.getInvokeMethod(staged);
            bytecode.addInvokestatic(m.getDeclaringClass().getName(), m.getName(), m.getMethodInfo2().getDescriptor());

            if (staged.isStrict()) {
                rewriteInstruction(at, bytecode);
                return liftStackTopPostInstruction(at, WeaveType.forTypeOriginal(returnType), returnType);
            } else {
                rewriteInstruction(at, bytecode);
                return WeaveType.forTypeWoven(returnType);
            }
        } else {
            materializeStackPreInstruction(at, targetObject, targetObjectOffset, targetType);
            for (int i = 0; i < argumentOffsets.length; i++) {
                materializeStackPreInstruction(at, arguments.get(i), argumentOffsets[i], paramTypes[i]);
            }

            return liftStackTopPostInstruction(at, WeaveType.forTypeOriginal(returnType), returnType);
        }
    }

    protected void invokeConstructor(WeaveFrame state, int at, CtConstructor constructor, Type targetType, Type[] paramTypes, WeaveType targetObject, int targetObjectOffset, ArrayList<WeaveType> arguments, int[] argumentOffsets) throws BadBytecode {
        // materializeStackPreInstruction(at, targetObject, targetObjectOffset, targetType);
        for (int i = 0; i < argumentOffsets.length; i++) {
            materializeStackPreInstruction(at, arguments.get(i), argumentOffsets[i], paramTypes[i]);
        }

        // We cannot just generate an index here because there is special handling for constructor invocation (see ValueFlowAnalyzer)
        SourceIndex sourceIndex = valueFlowResult.getInitializationSourceIndex(at);
        if (sourceIndex == null) {
            throw new RuntimeException();
        }
        WeaveType weaveType = liftPostInitializationInstruction(sourceIndex, WeaveType.NOT_CARRYING, targetType);
        for (int i = 0; i < sourceIndex.getStackOffsetCount(); i++) {
            state.setStack(sourceIndex.getStackOffset(i), weaveType);
        }
        for (int i = 0; i < sourceIndex.getLocalIndexCount(); i++) {
            state.setLocal(sourceIndex.getLocalIndex(i), weaveType);
        }
    }

    protected WeaveType invokeDynamic(WeaveFrame state, int at, CtMethod bootstrapMethod, StaticArgument[] staticArguments, Type returnType, Type[] paramTypes, ArrayList<WeaveType> arguments, int[] argumentOffsets) throws BadBytecode {
        for (int i = 0; i < argumentOffsets.length; i++) {
            materializeStackPreInstruction(at, arguments.get(i), argumentOffsets[i], paramTypes[i]);
        }

        return liftStackTopPostInstruction(at, WeaveType.forTypeOriginal(returnType), returnType);
    }

    protected WeaveType newInstance(WeaveFrame state, int at, Type type) throws BadBytecode {
        // We simulate instantiation as if it were a method call!
        // return liftStackPostInstruction(at, WeaveType.NOT_CARRYING, type);
        return WeaveType.NOT_CARRYING;
    }

    protected WeaveType arrayLength(WeaveFrame state, int at, WeaveType array, int arrayOffset) throws BadBytecode {
        Type inferredType = typeAnalyzerResult.getStack(at, arrayOffset);
        if (array.attemptMaterialization()) {
            materializeStackPreInstruction(at, array, arrayOffset, inferredType);
        }

        return liftStackTopPostInstruction(at, WeaveType.NOT_CARRYING, inferredType);
    }

    protected WeaveType throwException(WeaveFrame state, int at, WeaveType throwable, int throwableOffset) throws BadBytecode {
        materializeStackPreInstruction(at, throwable, throwableOffset, Type.THROWABLE);

        return liftStackTopPostInstruction(at, WeaveType.NOT_CARRYING, Type.THROWABLE);
    }

    protected WeaveType instanceOf(WeaveFrame state, int at, Type ofType, WeaveType value, int valueOffset) throws BadBytecode {
        materializeStackPreInstruction(at, value, valueOffset, Type.OBJECT);

        return liftStackTopPostInstruction(at, WeaveType.NOT_CARRYING, Type.BOOLEAN);
    }

    protected void enterSynchronized(WeaveFrame state, int at, WeaveType monitor, int monitorOffset) throws BadBytecode {
        materializeStackPreInstruction(at, monitor, monitorOffset, Type.OBJECT);
    }

    protected void exitSynchronized(WeaveFrame state, int at, WeaveType monitor, int monitorOffset) throws BadBytecode {
        materializeStackPreInstruction(at, monitor, monitorOffset, Type.OBJECT);
    }

    protected WeaveType newArray(WeaveFrame state, int at, Type componentType, ArrayList<WeaveType> lengths, int[] lengthOffsets) throws BadBytecode {
        Type inferredType = typeAnalyzerResult.getStack(at, 0);
        for (int i = 0; i < lengthOffsets.length; i++) {
            materializeStackPreInstruction(at, lengths.get(i), lengthOffsets[i], Type.INT);
        }
        return liftStackTopPostInstruction(at, WeaveType.NOT_CARRYING, inferredType);
    }
}
