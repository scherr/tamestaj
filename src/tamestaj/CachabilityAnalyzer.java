package tamestaj;

import com.google.common.collect.ImmutableSet;
import javassist.CtClass;

import java.util.*;

final class CachabilityAnalyzer {
    private CachabilityAnalyzer() { }

    private static boolean isAccepted(Source.Staged staged, Use.Argument argument) {
        for (CtClass language : argument.getAcceptedLanguages()) {
            if (staged.getLanguage().equals(language)) {
                return true;
            }
        }

        return false;
    }

    private static boolean couldBeMaterialized(Source.Staged staged) {
        if (staged.isStrict()) {
            // Special case: The staged is strict!

            return true;
        }

        if (Util.isGlobalCarrier(staged.getType())) {
            // Precondition:  The staged is not strict and not globally carrying

            return false;
        }

        ImmutableSet<Flow.Data> outData = staged.getOutData();
        for (Flow.Data data : outData) {
            if (data.getTo() instanceof Use.Opaque || (data.getTo() instanceof Use.Argument && !isAccepted(staged, (Use.Argument) data.getTo()))) {
                // Conditions for being considered to be evaluated on demand (1) and (2):
                //   (1) The (non-strict, non-globally-carrying) staged is handed to at least one opaque use
                //   (2) The (non-strict, non-globally-carrying) staged is handed to at least one non-accepting argument

                return true;
            }
        }

        return false;
    }

    private enum Dynamicity {
        STATIC,
        LOCALLY_DYNAMIC,
        GLOBALLY_DYNAMIC
    }

    static final class Result {
        private final HashSet<Source.Staged> permCachableStageds;
        private final HashSet<Source.Staged> traceCachableStageds;

        Result(HashSet<Source.Staged> permCachableStageds, HashSet<Source.Staged> traceCachableStageds) {
            this.permCachableStageds = permCachableStageds;
            this.traceCachableStageds = traceCachableStageds;
        }

        boolean hasPermCachableStageds() {
            return !permCachableStageds.isEmpty();
        }
        boolean isPermCachable(Source.Staged staged) {
            return permCachableStageds.contains(staged);
        }
        boolean hasTracheCachableStageds() {
            return !traceCachableStageds.isEmpty();
        }
        boolean isTraceCachable(Source.Staged staged) {
            return traceCachableStageds.contains(staged);
        }
    }

    static Result analyze(StageGraph graph, ValueFlowAnalyzer.Result valueFlowAnalyzerResult, ConstantAnalyzer.Result constantAnalyzerResult) {
        Queue<Source.Staged> stagedQueue = new LinkedList<>(graph.getStageds());

        IdentityHashMap<Source.Staged, Dynamicity> stagedToDynamicity = new IdentityHashMap<>();
        IdentityHashMap<Source.Staged, ImmutableSet<Use.Argument>> stagedToNonConstantArguments = new IdentityHashMap<>();
        while (!stagedQueue.isEmpty()) {
            Source.Staged staged = stagedQueue.poll();
            Dynamicity oldDynamicty = stagedToDynamicity.get(staged);

            if (oldDynamicty == Dynamicity.GLOBALLY_DYNAMIC) {
                continue;
            }

            Dynamicity dynamicity = Dynamicity.STATIC;

            ImmutableSet.Builder<Use.Argument> nonConstantArguments = ImmutableSet.builder();

            outer:
            for (Use.Argument a : staged.getArguments()) {
                ImmutableSet<Flow.Data> inData = a.getInData();
                for (Flow.Data data : inData) {
                    if (data.getFrom() instanceof Source.Staged) {
                        Source.Staged fromStaged = (Source.Staged) data.getFrom();
                        if (isAccepted(fromStaged, a) && !fromStaged.isStrict()) {
                            // Precondition: The incoming staged needs to be accepted and non-strict

                            // Mark as dynamic conditions (1) and (2):
                            //   (1) One argument comes from a non-strict accepted staged that is marked as dynamic
                            //   (2) One argument comes from several sources one of which is a non-strict accepted staged (i.e. fromStaged)

                            Dynamicity fromStagedDynamicity = stagedToDynamicity.get(fromStaged);
                            if (fromStagedDynamicity != null) {
                                if (fromStagedDynamicity == Dynamicity.GLOBALLY_DYNAMIC) {
                                    dynamicity = Dynamicity.GLOBALLY_DYNAMIC;
                                    break outer;
                                } else if (fromStagedDynamicity == Dynamicity.LOCALLY_DYNAMIC || inData.size() > 1) {
                                    dynamicity = Dynamicity.LOCALLY_DYNAMIC;
                                    // We do not break here because it could still be classified as globally dynamic
                                } else {
                                    nonConstantArguments.addAll(stagedToNonConstantArguments.get(fromStaged));
                                }
                            }
                        } else {
                            if (Util.couldBeGlobalCarrier(fromStaged.getType()) && !a.getAcceptedLanguages().isEmpty()) {
                                // Mark as dynamic conditions (3):
                                //   (3) One argument comes from a non-accepted or strict staged that could be globally carrying and the set of accepted languages is non-empty

                                dynamicity = Dynamicity.GLOBALLY_DYNAMIC;
                                break outer;
                            }

                            if (!constantAnalyzerResult.isConstant(a.getUseIndex())) {
                                nonConstantArguments.add(a);
                            }
                        }
                    } else {
                        if (Util.couldBeGlobalCarrier(data.getFrom().getType()) && !a.getAcceptedLanguages().isEmpty()) {
                            // Mark as dynamic conditions (4):
                            //   (4) One argument comes from an opaque source that could be globally carrying and the set of accepted languages is non-empty

                            dynamicity = Dynamicity.GLOBALLY_DYNAMIC;
                            break outer;
                        }

                        if (!constantAnalyzerResult.isConstant(a.getUseIndex())) {
                            nonConstantArguments.add(a);
                        }
                    }
                }
            }

            stagedToDynamicity.put(staged, dynamicity);
            if (dynamicity == Dynamicity.STATIC) {
                stagedToNonConstantArguments.put(staged, nonConstantArguments.build());
            }

            if (dynamicity != oldDynamicty) {
                for (Flow.Data d : staged.getOutData()) {
                    if (d.getTo() instanceof Use.Argument) {
                        stagedQueue.offer(((Use.Argument) d.getTo()).getConsumer());
                    }
                }
            }
        }

        HashSet<Source.Staged> permCachableStageds = new HashSet<>();
        HashSet<Source.Staged> traceCachableStageds = new HashSet<>();

        for (Map.Entry<Source.Staged, Dynamicity> entry : stagedToDynamicity.entrySet()) {
            boolean isFullyStatic = false;

            if (entry.getValue() == Dynamicity.STATIC) {
                isFullyStatic = true;

                // Check if the (flattened) arguments can yield different "shapes", i.e. if there are argument pairs
                // that in some executions could be the same value (in terms of object identity) but in others could be
                // different.
                //
                //                   disqualified <=> there exists an argument pair (A, B) where
                //                                      virtualSource(A) != virtualSource(B)
                //                                         &&
                //                                      intersection (sources(A), sources(B)) not empty
                //
                ImmutableSet<Use.Argument> nonConstantArguments = stagedToNonConstantArguments.get(entry.getKey());
                outer:
                for (Use.Argument a : nonConstantArguments) {
                    Set<SourceIndex> aSourceIndices = valueFlowAnalyzerResult.getSources(a.getUseIndex());
                    for (Use.Argument selfA : nonConstantArguments) {
                        if (!valueFlowAnalyzerResult.getVirtualSource(a.getUseIndex()).equals(valueFlowAnalyzerResult.getVirtualSource(selfA.getUseIndex()))) {
                            Set<SourceIndex> selfASourceIndices = valueFlowAnalyzerResult.getSources(selfA.getUseIndex());

                            for (SourceIndex sI : aSourceIndices) {
                                if (selfASourceIndices.contains(sI)) {
                                    isFullyStatic = false;
                                    break outer;
                                }
                            }
                        }
                    }
                }
            }

            if (couldBeMaterialized(entry.getKey())) {
                // Precondition: The staged could actually be materialized

                if (isFullyStatic) {
                    // Mark as perm cachable condition: The staged is fully static (including non-constant arguments)

                    permCachableStageds.add(entry.getKey());
                } else if (entry.getValue() != Dynamicity.GLOBALLY_DYNAMIC) {
                    // Mark as trace cachable condition: The staged is non-globally carrying or has dynamic arguments

                    traceCachableStageds.add(entry.getKey());
                }
            }
        }

        return new Result(permCachableStageds, traceCachableStageds);
    }
}
