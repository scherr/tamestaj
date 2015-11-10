package tamestaj;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import javassist.CtClass;

import java.util.*;

@SuppressWarnings("unused")
final class StageGraph {
    private final ImmutableSet<Node> entrySuccessors;
    private final ImmutableSet<Node> exitPredecessors;
    private final ImmutableMap<SourceIndex, Source.Staged> indexedStageds;
    private final ImmutableMap<SourceIndex, Source> indexedSources;
    private final ImmutableMap<UseIndex, Use> indexedUses;
    private final ImmutableMap<Index, Node> indexedNodes;

    StageGraph(ImmutableSet<Node> entrySuccessors, ImmutableSet<Node> exitPredecessors, ImmutableMap<SourceIndex, Source.Staged> indexedStageds, ImmutableMap<SourceIndex, Source> indexedSources, ImmutableMap<UseIndex, Use> indexedUses) {
        this.entrySuccessors = entrySuccessors;
        this.exitPredecessors = exitPredecessors;
        this.indexedStageds = indexedStageds;
        this.indexedSources = indexedSources;
        this.indexedUses = indexedUses;

        ImmutableMap.Builder<Index, Node> indexedNodesBuilder = ImmutableMap.builder();

        indexedNodesBuilder.putAll(indexedSources);
        indexedNodesBuilder.putAll(indexedUses);
        this.indexedNodes = indexedNodesBuilder.build();
    }

    ImmutableSet<Node> getEntrySuccessors() { return entrySuccessors; }
    ImmutableSet<Node> getExitPredecessors() { return exitPredecessors; }
    ImmutableCollection<Source.Staged> getStageds() { return indexedStageds.values(); }
    Source.Staged getStaged(SourceIndex index) { return indexedStageds.get(index); }
    ImmutableCollection<Source> getSources() { return indexedSources.values(); }
    Source getSource(SourceIndex index) { return indexedSources.get(index); }
    ImmutableCollection<Use> getUses() { return indexedUses.values(); }
    Use getUse(UseIndex index) { return indexedUses.get(index); }

    ImmutableCollection<Node> getNodes() { return indexedNodes.values(); }
    Node getNode(Index index) { return indexedNodes.get(index); }

    private static final String[] COLORS = {
        "cadetblue4", "brown4", "burlywood4", "chartreuse4", "chocolate4", "darkseagreen4", "goldenrod4",
        "tomato3", "slateblue4", "rosybrown4", "mediumseagreen", "yellow4", "olivedrab4", "pink4", "palegreen4"
    };

    String toDotString() {
        HashMap<CtClass, String> languageToColorMap = new HashMap<>();
        int colorIndex = 0;
        for (Source.Staged staged : indexedStageds.values()) {
            String color = languageToColorMap.get(staged.getLanguage());
            if (color == null) {
                if (colorIndex < COLORS.length) {
                    color = COLORS[colorIndex];
                    colorIndex = colorIndex + 1;
                } else {
                    color = "gray";
                }
                languageToColorMap.put(staged.getLanguage(), color);
            }
        }

        StringBuilder sb = new StringBuilder("digraph {\n");
        sb.append("graph [fontname = \"Courier\"]\n");
        sb.append("node [fontname = \"Courier\"]\n");
        sb.append("edge [fontname = \"Courier\"]\n");
        sb.append("entry [shape=invhouse,label=\"ENTRY\"]\n");
        sb.append("exit [shape=house,label=\"EXIT\"]\n");

        HashSet<SourceIndex> handledSourceIndices = new HashSet<>();
        HashSet<UseIndex> handledUseIndices = new HashSet<>();

        for (Source.Staged staged : indexedStageds.values()) {
            String color = languageToColorMap.get(staged.getLanguage());

            sb.append("\"").append(staged.getSourceIndex()).append("\"").append(" [");
            sb.append("color=\"").append(color).append("\",");
            sb.append("shape=Mrecord,");
            sb.append("label=\"<name> ").append(staged.getMember().getName()).append("|");
            for (int i = 0; i < staged.getArguments().size(); i++) {
                sb.append("<arg").append(i).append("> _|");
            }
            sb.append(staged.getSourceIndex().getPosition()).append("\"]\n");
            handledSourceIndices.add(staged.getSourceIndex());

            for (Use.Argument argument : staged.getArguments()) {
                for (Flow.Data data : argument.getInData()) {
                    Source source = data.getFrom();
                    String edgeColor = color;

                    if (source instanceof Source.Staged) {
                        CtClass sourceLanguage = ((Source.Staged) source).getLanguage();
                        if (!sourceLanguage.equals(staged.getLanguage())) {
                            edgeColor = languageToColorMap.get(sourceLanguage) + ";0.5:" + color;
                        }
                        sb.append("\"").append(source.getSourceIndex()).append("\"").append(":name -> ");
                    } else if (source instanceof Source.Opaque) {
                        if (!handledSourceIndices.contains(source.getSourceIndex())) {
                            sb.append("\"").append(source.getSourceIndex()).append("\"");
                            sb.append(" [shape=circle,label=\"\"]\n");
                            handledSourceIndices.add(source.getSourceIndex());
                        }
                        sb.append("\"").append(source.getSourceIndex()).append("\"").append(" -> ");
                    } else if (source instanceof Source.Constant) {
                        if (!handledSourceIndices.contains(source.getSourceIndex())) {
                            sb.append("\"").append(source.getSourceIndex()).append("\"");
                            sb.append(" [shape=box,label=\"").append(((Source.Constant) source).getValue()).append("\"]\n");
                            handledSourceIndices.add(source.getSourceIndex());
                        }
                        sb.append("\"").append(staged.getSourceIndex()).append("\"").append(" -> ");
                    }
                    sb.append("\"").append(staged.getSourceIndex()).append("\"").append(":arg").append(argument.getIndex()).append(":c [");
                    sb.append("color=\"").append(edgeColor).append("\",");
                    sb.append("headclip=false,arrowhead=none]\n");
                }
            }

            for (Flow.Data data : staged.getOutData()) {
                Use use = data.getTo();
                if (!(use instanceof Use.Argument)) {
                    if (!handledUseIndices.contains(use.getUseIndex())) {
                        sb.append("\"").append(use.getUseIndex()).append("\"");
                        sb.append(" [shape=diamond,label=\"_\"]\n");
                        handledUseIndices.add(use.getUseIndex());

                        for (Flow.Data d : use.getInData()) {
                            sb.append("\"").append(d.getFrom().getSourceIndex()).append("\"").append(" -> ");
                            sb.append(use.getUseIndex()).append(":c [");
                            sb.append("color=\"").append(color).append("\",");
                            sb.append("headclip=false,arrowhead=none]\n");
                        }
                    }
                }
            }
        }

        HashSet<Flow.Control> handledControls = new HashSet<>();
        for (Source source : indexedSources.values()) {
            for (Flow.Control control : source.getInControl()) {
                if (!handledControls.contains(control)) {
                    sb.append(controlToString(control)).append("\n");

                    handledControls.add(control);
                }
            }

            for (Flow.Control control : source.getOutControl()) {
                if (!handledControls.contains(control)) {
                    sb.append(controlToString(control)).append("\n");

                    handledControls.add(control);
                }
            }
        }

        for (Use use : indexedUses.values()) {
            for (Flow.Control control : use.getInControl()) {
                if (!handledControls.contains(control)) {
                    sb.append(controlToString(control)).append("\n");

                    handledControls.add(control);
                }
            }

            for (Flow.Control control : use.getOutControl()) {
                if (!handledControls.contains(control)) {
                    sb.append(controlToString(control)).append("\n");

                    handledControls.add(control);
                }
            }
        }

        sb.append("}");

        return sb.toString();
    }

    private static String controlToString(Flow.Control control) {
        StringBuilder sb = new StringBuilder();
        Node from = control.getFrom();
        if (from == null) {
            sb.append("entry -> ");
        } else {
            if (from instanceof Source.Staged) {
                sb.append("\"").append(((Source) from).getSourceIndex()).append("\"").append(":name:s -> ");
            } else if (from instanceof Source) {
                sb.append("\"").append(((Source) from).getSourceIndex()).append("\"").append(" -> ");
            } else if (from instanceof Use.Opaque) {
                sb.append("\"").append(((Use) from).getUseIndex()).append("\"").append(" -> ");
            }
        }
        Node to = control.getTo();
        if (to == null) {
            sb.append("exit");
        } else {
            if (to instanceof Source.Staged) {
                sb.append("\"").append(((Source) to).getSourceIndex()).append("\"").append(":name:n");
            } else if (to instanceof Source) {
                sb.append("\"").append(((Source) to).getSourceIndex()).append("\"");
            } else if (to instanceof Use.Opaque) {
                sb.append("\"").append(((Use) to).getUseIndex()).append("\"");
            }
        }
        sb.append(" [color=\"black\",");
        sb.append("style=dashed]");

        return sb.toString();
    }
}
