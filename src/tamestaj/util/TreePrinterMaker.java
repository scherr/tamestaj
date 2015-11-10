package tamestaj.util;

import javassist.CtMember;
import tamestaj.*;
import tamestaj.Expression;

public class TreePrinterMaker implements Expression.Visitor {
    private final Environment.Binder binder;

    private ObjectClosure<String> closure;
    private String indent = "";

    public TreePrinterMaker(Environment.Binder binder) {
        this.binder = binder;
    }

    public ObjectClosure<String> getObjectClosure() {
        return closure;
    }

    public void visit(Expression.FieldRead staged) {
        CtMember m = staged.getMember();
        visitStaged(staged, m.getDeclaringClass().getName() + "." + m.getName() + "<");
    }
    public void visit(Expression.FieldAssignment staged) {
        CtMember m = staged.getMember();
        visitStaged(staged, m.getDeclaringClass().getName() + "." + m.getName() + ">");
    }
    public void visit(Expression.MethodInvocation staged) {
        CtMember m = staged.getMember();
        visitStaged(staged, m.getDeclaringClass().getName() + "." + m.getName() + "!");
    }

    protected void visitStaged(Expression.Staged staged, String name) {
        String indent = this.indent;

        if (staged.getArgumentCount() == 0) {
            String n = indent + name + "()";
            closure = env -> n;
            return;
        }

        ObjectClosure<String> args[] = (ObjectClosure<String>[]) new ObjectClosure[staged.getArgumentCount()];

        String open = indent + name + "(\n";
        String close = "\n" + indent + ")";

        this.indent = this.indent + " ";

        for (int i = 0; i < args.length; i++) {
            staged.getArgument(i).accept(this);
            args[i] = closure;
        }

        this.indent = indent;

        closure = env -> {
            StringBuilder sb = new StringBuilder();
            sb.append(open);
            for (int i = 0; i < args.length; i++) {
                sb.append(args[i].evaluate(env));
                if (i < args.length - 1) {
                    sb.append(",\n");
                }
            }
            sb.append(close);

            return sb.toString();
        };
    }

    public void visit(Expression.ObjectValue value) {
        String indent = this.indent;
        ObjectClosure c = value.bind(binder);
        closure = env -> {
            Object o = c.evaluate(env);
            if (o instanceof String || o instanceof Number || o instanceof Character || o instanceof Boolean) {
                return indent + o.toString();
            } else if (o == null) {
                return indent + "null";
            } else {
                return indent + o.getClass().getName() + "@" + Integer.toHexString(o.hashCode());
                // return indent + o.toString();
            }
        };
    }
    public void visit(Expression.BooleanValue value) {
        String indent = this.indent;
        BooleanClosure c = value.bind(binder);
        closure = env -> indent + Boolean.toString(c.evaluate(env));
    }
    public void visit(Expression.IntegerValue value) {
        String indent = this.indent;
        IntegerClosure c = value.bind(binder);
        closure = env -> indent + Integer.toString(c.evaluate(env));
    }
    public void visit(Expression.LongValue value) {
        String indent = this.indent;
        LongClosure c = value.bind(binder);
        closure = env -> indent + Long.toString(c.evaluate(env));
    }
    public void visit(Expression.FloatValue value) {
        String indent = this.indent;
        FloatClosure c = value.bind(binder);
        closure = env -> indent + Float.toString(c.evaluate(env));
    }
    public void visit(Expression.DoubleValue value) {
        String indent = this.indent;
        DoubleClosure c = value.bind(binder);
        closure = env -> indent + Double.toString(c.evaluate(env));
    }
    public void visit(Expression.ByteValue value) {
        String indent = this.indent;
        ByteClosure c = value.bind(binder);
        closure = env -> indent + Byte.toString(c.evaluate(env));
    }
    public void visit(Expression.CharacterValue value) {
        String indent = this.indent;
        CharacterClosure c = value.bind(binder);
        closure = env -> indent + Character.toString(c.evaluate(env));
    }
    public void visit(Expression.ShortValue value) {
        String indent = this.indent;
        ShortClosure c = value.bind(binder);
        closure = env -> indent + Short.toString(c.evaluate(env));
    }
}
