package tamestaj.examples.mini;

import tamestaj.*;
import tamestaj.util.CtClassLoader;
import javassist.*;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.analysis.Analyzer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;

final class MiniLCompiler implements Expression.Visitor {
    private final Environment.Binder binder;
    private final IdentityHashMap<Object, String> intIdentMap = new IdentityHashMap<>();
    private final IdentityHashMap<Object, String> boolIdentMap = new IdentityHashMap<>();
    private final IdentityHashMap<Expression.MethodInvocation, String> intVarIdentMap = new IdentityHashMap<>();
    private final IdentityHashMap<Expression.MethodInvocation, String> boolVarIdentMap = new IdentityHashMap<>();

    private StaticInfo.Origin startOrigin;
    private String code;
    private String indent = "  ";

    MiniLCompiler(Environment.Binder binder) {
        this.binder = binder;
    }

    private String getIdent(IntegerClosure closure) {
        String ident = intIdentMap.get(closure);
        if (ident == null) {
            ident = "int" + intIdentMap.size();
            intIdentMap.put(closure, ident);
        }
        return ident;
    }
    private String getIdent(BooleanClosure closure) {
        String ident = boolIdentMap.get(closure);
        if (ident == null) {
            ident = "bool" + boolIdentMap.size();
            boolIdentMap.put(closure, ident);
        }
        return ident;
    }
    private String getIntVarIdent(Expression.MethodInvocation variable) {
        String ident = intVarIdentMap.get(variable);
        if (ident == null) {
            ident = "intVar" + intVarIdentMap.size();
            intVarIdentMap.put(variable, ident);
        }
        return ident;
    }
    private String getBoolVarIdent(Expression.MethodInvocation variable) {
        String ident = boolVarIdentMap.get(variable);
        if (ident == null) {
            ident = "boolVar" + boolVarIdentMap.size();
            boolVarIdentMap.put(variable, ident);
        }
        return ident;
    }

    <T> T getClosure(Class<T> closureClass) {
        ClassPool cp = ClassPool.getDefault();
        try {
            Map.Entry<Object, String>[] intIdentEntries = new Map.Entry[intIdentMap.size()];
            intIdentMap.entrySet().toArray(intIdentEntries);
            Map.Entry<Object, String>[] boolIdentEntries = new Map.Entry[boolIdentMap.size()];
            boolIdentMap.entrySet().toArray(boolIdentEntries);

            CtClass cloClazz = cp.makeClass(Mini.class.getName()+ "$MiniClosure");
            cloClazz.setInterfaces(new CtClass[]{ cp.get( closureClass.getName()) });

            for (int i = 0; i < intIdentEntries.length; i++) {
                CtField field = CtField.make("public static " + IntegerClosure.class.getName() + " intC" + i + ";", cloClazz);
                cloClazz.addField(field);
            }
            for (int i = 0; i < boolIdentEntries.length; i++) {
                CtField field = CtField.make("public static " + BooleanClosure.class.getName() + " boolC" + i + ";", cloClazz);
                cloClazz.addField(field);
            }

            StringBuilder cloSource = new StringBuilder();
            if (closureClass.getSimpleName().startsWith("Boolean")) {
                cloSource.append("public boolean evaluate(" + Environment.class.getName() + " env) {\n");
            } else if (closureClass.getSimpleName().startsWith("Integer")) {
                cloSource.append("public int evaluate(" + Environment.class.getName() + " env) {\n");
            }

            cloSource.append(indent);
            cloSource.append("// Value retrievals\n");
            for (int i = 0; i < intIdentEntries.length; i++) {
                cloSource.append(indent);
                cloSource.append("int " + intIdentEntries[i].getValue() + " = intC" + i + ".evaluate(env);\n");
                // cloSource.append("System.out.println(" + intIdentEntries[i].getValue() + ");\n");
            }
            for (int i = 0; i < boolIdentEntries.length; i++) {
                cloSource.append(indent);
                cloSource.append("boolean " + boolIdentEntries[i].getValue() + " = boolC" + i + ".evaluate(env);\n");
                // cloSource.append("System.out.println(" + boolIdentEntries[i].getValue() + ");\n");
            }

            cloSource.append(indent);
            cloSource.append("// Variable definitions\n");
            for (String ident : intVarIdentMap.values()) {
                cloSource.append(indent);
                cloSource.append("int " + ident + ";\n");
            }
            for (String ident : boolVarIdentMap.values()) {
                cloSource.append(indent);
                cloSource.append("boolean " + ident + ";\n");
            }

            cloSource.append(code);
            cloSource.append("\n}");
            CtMethod cloMethod = CtNewMethod.make(cloSource.toString(), cloClazz);

            try {
                Analyzer analyzer = new Analyzer();
                analyzer.analyze(cloMethod);
            } catch (BadBytecode e) {
                // throw new RuntimeException(e);
                if (startOrigin.getLineNumber().isPresent()) {
                    throw new RuntimeException("The Mini program started in method \"" + startOrigin.getBehavior().getLongName() + "\" at line " + startOrigin.getLineNumber().getAsInt() + " could not be compiled: " + e);
                } else {
                    throw new RuntimeException("The Mini program started in method \"" + startOrigin.getBehavior().getLongName() + "\" at byte code position " + startOrigin.getPosition() + " could not be compiled: " + e);
                }
            }

            cloClazz.addMethod(cloMethod);

            CtClassLoader loader = new CtClassLoader();

            Class<?> cloC = loader.load(cloClazz);
            cloClazz.detach();

            for (int i = 0; i < intIdentEntries.length; i++) {
                Field f = cloC.getDeclaredField("intC" + i);
                f.set(null, intIdentEntries[i].getKey());
            }

            for (int i = 0; i < boolIdentEntries.length; i++) {
                Field f = cloC.getDeclaredField("boolC" + i);
                f.set(null, boolIdentEntries[i].getKey());
            }

            return (T) cloC.newInstance();
        } catch (CannotCompileException | NotFoundException | InstantiationException | IllegalAccessException | IOException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
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
        String indent = this.indent;
        CtMethod m = staged.getMember();
        switch (m.getName()) {
            case "add":
            case "mul":
            case "eq":
            case "leq":
            case "and":
            case "or": {
                staged.getArgument(0).accept(this);
                String arg0 = code;

                staged.getArgument(1).accept(this);
                String arg1 = code;

                String op;
                switch(m.getName()) {
                    case "add": { op = " + ";  break; }
                    case "mul": { op = " * ";  break; }
                    case "eq":  { op = " == "; break; }
                    case "leq": { op = " <= "; break; }
                    case "and": { op = " && "; break; }
                    case "or":  { op = " || "; break; }
                    default: throw new RuntimeException();
                }

                code = "(" + arg0 + op + arg1 + ")";
                break;
            }

            case "neg":
            case "not": {
                staged.getArgument(0).accept(this);
                String arg0 = code;

                String op;
                switch(m.getName()) {
                    case "neg": { op = "-"; break; }
                    case "not": { op = "!"; break; }
                    default: throw new RuntimeException();
                }

                code = "(" + op + arg0 + ")";
                break;
            }

            case "intVar": {
                code = getIntVarIdent(staged);
                break;
            }
            case "boolVar": {
                code = getBoolVarIdent(staged);
                break;
            }

            case "intLit":
            case "boolLit": {
                staged.getArgument(0).accept(this);
                break;
            }

            case "intAssign":
            case "boolAssign": {
                staged.getArgument(0).accept(this);
                String arg0 = code;

                staged.getArgument(1).accept(this);
                String arg1 = code;

                code = indent + arg0 + " = " + arg1 + ";";
                break;
            }

            case "whileDo": {
                staged.getArgument(0).accept(this);
                String arg0 = code;

                this.indent += "  ";

                staged.getArgument(1).accept(this);
                String arg1 = code;

                this.indent = indent;

                code = indent + "while (" + arg0 + ") {\n" + arg1 + "\n" + indent + "}";
                break;
            }
            case "then": {
                code = indent + "// Sequence start";

                if (startOrigin == null) {
                    startOrigin = staged.getStaticInfo().get().getOrigin().get();
                }

                staged.getArgument(0).accept(this);
                String arg0 = code;

                staged.getArgument(1).accept(this);
                String arg1 = code;

                code = arg0 + "\n" + arg1;
                break;
            }

            case "intRun":
            case "boolRun": {
                staged.getArgument(0).accept(this);
                String arg0 = code;

                staged.getArgument(1).accept(this);
                String arg1 = code;

                code = arg0 + "\n" + indent + "return " + arg1 + ";";
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
        code = getIdent(value.bind(binder));
    }
    @Override
    public void visit(Expression.IntegerValue value) {
        code = getIdent(value.bind(binder));
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
