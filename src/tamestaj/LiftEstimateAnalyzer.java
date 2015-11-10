package tamestaj;

import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;

final class LiftEstimateAnalyzer {
    private LiftEstimateAnalyzer() { }

    static final class Result {
        private final HashSet<SourceIndex> liftSourceIndices;

        Result(HashSet<SourceIndex> liftSourceIndices) {
            this.liftSourceIndices = liftSourceIndices;
        }

        boolean mayRequireLift(Source source) {
            return liftSourceIndices.contains(source.getSourceIndex());
        }
        boolean mayRequireLift(SourceIndex sourceIndex) {
            return liftSourceIndices.contains(sourceIndex);
        }
    }

    static Result analyze(StageGraph graph, ValueFlowAnalyzer.Result valueFlowAnalyzerResult, ConstantAnalyzer.Result constantAnalyzerResult) {
        // TODO: This is still too conservative because we merely count the global maximum (even if overestimated, i.e. without considering control flow)
        IdentityHashMap<Source, Integer> shareCount = new IdentityHashMap<>();
        for (Source.Staged staged : graph.getStageds()) {
            for (Use.Argument a : staged.getArguments()) {
                if (constantAnalyzerResult.isConstant(a.getUseIndex())) {
                    continue;
                }

                ImmutableSet<Flow.Data> inData = a.getInData();
                for (Flow.Data data : inData) {
                    if (!(data.getFrom() instanceof Source.Staged) ||
                            (data.getFrom() instanceof Source.Staged && ((Source.Staged) data.getFrom()).isStrict())) {
                        Integer count = shareCount.get(data.getFrom());
                        if (count == null) {
                            count = 0;
                        }
                        count++;
                        shareCount.put(data.getFrom(), count);
                    }
                }
            }
        }

        HashSet<SourceIndex> liftSourceIndices = new HashSet<>();
        for (Map.Entry<Source, Integer> entry : shareCount.entrySet()) {
            if (entry.getValue() > 1) {
                // If a source is shared we need to consider it for lifting
                liftSourceIndices.add(entry.getKey().getSourceIndex());

                // If that is the case we also need to consider lifting the sources (indices) in the same merge class
                liftSourceIndices.addAll(valueFlowAnalyzerResult.getMergeClass(entry.getKey().getSourceIndex()));
            }
        }

        // We need to lift the sources (indices) in the same merge class as non-strict stageds.
        // We overestimate here regarding carrier types and defer the rest of the work
        // (i.e. lifting decisions) to the weaving analysis later!
        HashSet<SourceIndex> nonStrictStagedIndices = new HashSet<>();
        for (Source.Staged staged : graph.getStageds()) {
            if (!staged.isStrict()) {
                liftSourceIndices.addAll(valueFlowAnalyzerResult.getMergeClass(staged.getSourceIndex()));
                nonStrictStagedIndices.add(staged.getSourceIndex());
            }
        }

        return new Result(liftSourceIndices);
    }
}
