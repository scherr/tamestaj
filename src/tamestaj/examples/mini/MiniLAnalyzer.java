package tamestaj.examples.mini;

import javassist.*;
import tamestaj.*;

import java.util.HashSet;

final class MiniLAnalyzer implements Expression.Visitor {
    private final Environment.Binder binder;
    private final HashSet<Expression.MethodInvocation> initVars = new HashSet<>();
    private StaticInfo lastStaticInfo;

    MiniLAnalyzer(Environment.Binder binder) {
        this.binder = binder;
    }

    @Override
    public void visit(Expression.FieldRead staged) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void visit(Expression.FieldAssignment staged) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void visit(Expression.MethodInvocation staged) {
        CtMethod m = staged.getMember();
        switch (m.getName()) {
            case "add":
            case "mul":
            case "eq":
            case "leq":
            case "and":
            case "or": {
                staged.getArgument(0).accept(this);
                staged.getArgument(1).accept(this);

                break;
            }

            case "neg":
            case "not": {
                staged.getArgument(0).accept(this);

                break;
            }

            case "intVar":
            case "boolVar": {
                if (!initVars.contains(staged)) {
                    Expression.ObjectValue<String> name = (Expression.ObjectValue<String>) staged.getArgument(0);
                    if (name.isConstant()) {
                        throw new RuntimeException("Variable \"" + name.inspect(binder) + "\" not initialized near: " + lastStaticInfo.getOrigin().get());
                    } else {
                        throw new RuntimeException("Variable not initialized near: " + lastStaticInfo.getOrigin().get());
                    }
                }

                break;
            }

            case "intLit":
            case "boolLit": {
                staged.getArgument(0).accept(this);

                break;
            }

            case "intAssign":
            case "boolAssign": {
                staged.getArgument(1).accept(this);

                initVars.add((Expression.MethodInvocation) staged.getArgument(0));

                break;
            }

            case "whileDo": {
                HashSet<Expression.MethodInvocation> oldInitVars = new HashSet<>(initVars);

                staged.getArgument(0).accept(this);
                staged.getArgument(1).accept(this);

                initVars.retainAll(oldInitVars);

                break;
            }
            case "then": {
                staged.getArgument(0).accept(this);

                lastStaticInfo = staged.getStaticInfo().get();

                staged.getArgument(1).accept(this);

                break;
            }

            case "intRun":
            case "boolRun": {
                staged.getArgument(0).accept(this);

                lastStaticInfo = staged.getStaticInfo().get();

                staged.getArgument(1).accept(this);

                break;
            }
        }
    }

    @Override
    public void visit(Expression.ObjectValue value) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void visit(Expression.BooleanValue value) {

    }
    @Override
    public void visit(Expression.IntegerValue value) {

    }
    @Override
    public void visit(Expression.LongValue value) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void visit(Expression.FloatValue value) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void visit(Expression.DoubleValue value) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void visit(Expression.ByteValue value) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void visit(Expression.CharacterValue value) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void visit(Expression.ShortValue value) {
        throw new UnsupportedOperationException();
    }
}
