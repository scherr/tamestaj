package tamestaj.util;

import javassist.CtMember;
import tamestaj.Expression;

import java.util.IdentityHashMap;

@SuppressWarnings("unused")
public final class ExpressionPrinter {
    private ExpressionPrinter() { }

    private static class PrintDotGraphVisitor implements Expression.Visitor {
        private final IdentityHashMap<Expression, Integer> idMap = new IdentityHashMap<>();
        private final StringBuilder output = new StringBuilder();

        private PrintDotGraphVisitor() {
            output.append("graph {\n");
        }

        private void visitValue(Expression.Value<?> value) {
            Integer id = idMap.get(value);
            if (id == null) {
                id = idMap.size();
                idMap.put(value, id);

                if (value.isConstant()) {
                    output.append(" node").append(id).append(" [shape=house,label=\"").append(value).append
                            ("\"]\n");
                } else {
                    output.append(" node").append(id).append(" [shape=box,label=\"").append(value).append("\"]\n");
                }
            }
        }

        private String getResult() {
            return output.toString() + "}";
        }

        public void visit(Expression.ObjectValue value) { visitValue(value); }
        public void visit(Expression.BooleanValue value) { visitValue(value); }
        public void visit(Expression.IntegerValue value) { visitValue(value); }
        public void visit(Expression.LongValue value) { visitValue(value); }
        public void visit(Expression.FloatValue value) { visitValue(value); }
        public void visit(Expression.DoubleValue value) { visitValue(value); }
        public void visit(Expression.ByteValue value) { visitValue(value); }
        public void visit(Expression.CharacterValue value) { visitValue(value); }
        public void visit(Expression.ShortValue value) { visitValue(value); }

        public void visit(Expression.FieldRead staged) {
            Integer id = idMap.get(staged);
            if (id == null) {
                id = idMap.size();
                idMap.put(staged, id);

                output.append(" node").append(id).append(" [color=\"brown4\",shape=Mrecord,label=\"").append(staged
                        .getMember().getName()).append("\"]\n");

                for (int i = 0; i < staged.getArgumentCount(); i++) {
                    staged.getArgument(i).accept(this);
                    output.append(" node").append(id).append(":s -- node").append(idMap.get(staged.getArgument(i)))
                            .append(":n\n");
                }
            }
        }

        public void visit(Expression.FieldAssignment staged) {
            Integer id = idMap.get(staged);
            if (id == null) {
                id = idMap.size();
                idMap.put(staged, id);

                output.append(" node").append(id).append(" [color=\"yellow4\",shape=Mrecord,label=\"").append(staged
                        .getMember().getName()).append("\"]\n");

                for (int i = 0; i < staged.getArgumentCount(); i++) {
                    staged.getArgument(i).accept(this);
                    output.append(" node").append(id).append(":s -- node").append(idMap.get(staged.getArgument(i)))
                            .append(":n\n");
                }
            }
        }

        public void visit(Expression.MethodInvocation staged) {
            Integer id = idMap.get(staged);
            if (id == null) {
                id = idMap.size();
                idMap.put(staged, id);

                output.append(" node").append(id).append(" [color=\"cadetblue4\",shape=Mrecord,label=\"").append
                        (staged.getMember().getName()).append("\"]\n");

                for (int i = 0; i < staged.getArgumentCount(); i++) {
                    staged.getArgument(i).accept(this);
                    output.append(" node").append(id).append(":s -- node").append(idMap.get(staged.getArgument(i)))
                            .append(":n\n");
                }
            }
        }
    }

    private static class PrintGraphVisitor implements Expression.Visitor {
        private boolean countOccurrencesMode;
        private final IdentityHashMap<Expression, Integer> occurrenceCountsMap = new IdentityHashMap<>();

        private boolean multiLine;
        private String indent = "";
        private final IdentityHashMap<Expression, Integer> idMap = new IdentityHashMap<>();
        private String output;

        private PrintGraphVisitor(boolean multiLine) {
            countOccurrencesMode = true;
            this.multiLine = multiLine;
        }

        private void visitValue(Expression.Value<?> value) {
            if (countOccurrencesMode) {
                occurrenceCountsMap.compute(value, (k, v) -> v == null ? 1 : v + 1);
            } else {
                if (occurrenceCountsMap.getOrDefault(value, 1) > 1) {
                    Integer id = idMap.get(value);
                    if (id == null) {
                        id = idMap.size();
                        idMap.put(value, id);

                        output = (multiLine ? indent : "") + "#" + id + " := " + value;
                    } else {
                        output = (multiLine ? indent : "") + "#" + id;
                    }
                } else {
                    output = (multiLine ? indent : "") + value;
                }
            }
        }

        public void visit(Expression.ObjectValue value) { visitValue(value); }
        public void visit(Expression.BooleanValue value) { visitValue(value); }
        public void visit(Expression.IntegerValue value) { visitValue(value); }
        public void visit(Expression.LongValue value) { visitValue(value); }
        public void visit(Expression.FloatValue value) { visitValue(value); }
        public void visit(Expression.DoubleValue value) { visitValue(value); }
        public void visit(Expression.ByteValue value) { visitValue(value); }
        public void visit(Expression.CharacterValue value) { visitValue(value); }
        public void visit(Expression.ShortValue value) { visitValue(value); }

        public void visit(Expression.FieldRead staged) {
            visitStaged(staged, "<");
        }
        public void visit(Expression.FieldAssignment staged) {
            visitStaged(staged, ">");
        }
        public void visit(Expression.MethodInvocation staged) {
            visitStaged(staged, "!");
        }

        protected void visitStaged(Expression.Staged staged, String appendix) {
            if (countOccurrencesMode) {
                if (occurrenceCountsMap.compute(staged, (k, v) -> v == null ? 1 : v + 1) == 1) {
                    for (int i = 0; i < staged.getArgumentCount(); i++) {
                        staged.getArgument(i).accept(this);
                    }
                }
            } else {
                String name;

                CtMember m = staged.getMember();
                if (occurrenceCountsMap.getOrDefault(staged, 1) > 1) {
                    Integer id = idMap.get(staged);
                    if (id == null) {
                        id = idMap.size();
                        idMap.put(staged, id);

                        name = "#" + id + " := " + m.getDeclaringClass().getName() + "." + m.getName() + appendix;
                    } else {
                        output = (multiLine ? indent : "") + "#" + id;
                        return;
                    }
                } else {
                    name = m.getDeclaringClass().getName() + "." + m.getName() + appendix;
                }


                String indent = this.indent;

                if (staged.getArgumentCount() == 0) {
                    String n = (multiLine ? indent : "") + name + "()";
                    output = n;
                    return;
                }

                String open = multiLine ? indent + name + "(\n" : name + "(";
                String close = multiLine ? "\n" + indent + ")" : ")";

                this.indent = this.indent + " ";

                StringBuilder sb = new StringBuilder();
                sb.append(open);
                for (int i = 0; i < staged.getArgumentCount(); i++) {
                    staged.getArgument(i).accept(this);
                    sb.append(output);

                    if (i < staged.getArgumentCount() - 1) {
                        if (multiLine) {
                            sb.append(",\n");
                        } else {
                            sb.append(", ");
                        }
                    }
                }
                sb.append(close);

                this.indent = indent;

                output = sb.toString();
            }
        }
    }

    public static String printDotGraph(Expression expression) {
        PrintDotGraphVisitor v = new PrintDotGraphVisitor();
        expression.accept(v);
        return v.getResult();
    }

    public static String printGraph(Expression expression, boolean multiLine) {
        PrintGraphVisitor v = new PrintGraphVisitor(multiLine);
        v.countOccurrencesMode = true;
        expression.accept(v);
        v.countOccurrencesMode = false;
        expression.accept(v);
        return v.output;
    }
}
