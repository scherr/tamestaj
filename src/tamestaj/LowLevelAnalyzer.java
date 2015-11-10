package tamestaj;

import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.bytecode.*;

@SuppressWarnings("unused")
abstract class LowLevelAnalyzer<T> extends Analyzer<T> implements Opcode {
    protected final CodeAttribute codeAttribute;
    protected final CodeIterator codeIterator;
    protected final ConstPool constPool;
    protected final ClassPool classPool;
    public static final Type STRING_TYPE;
    public static final Type CLASS_TYPE;
    public static final Type THROWABLE_TYPE;

    static {
        try {
            STRING_TYPE = getType("java.lang.String", -1);
            CLASS_TYPE = getType("java.lang.Class", -1);
            THROWABLE_TYPE = getType("java.lang.Throwable", -1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected LowLevelAnalyzer(CtBehavior behavior) {
        super(behavior);

        CtClass clazz = behavior.getDeclaringClass();
        MethodInfo methodInfo = behavior.getMethodInfo2();
        codeAttribute = methodInfo.getCodeAttribute();
        codeIterator = codeAttribute.iterator();
        constPool = codeAttribute.getConstPool();
        classPool = clazz.getClassPool();
    }

    protected LowLevelAnalyzer(Analyzer<?> analyzer) {
        super(analyzer);

        CtClass clazz = behavior.getDeclaringClass();
        MethodInfo methodInfo = behavior.getMethodInfo2();
        codeAttribute = methodInfo.getCodeAttribute();
        codeIterator = codeAttribute.iterator();
        constPool = codeAttribute.getConstPool();
        classPool = clazz.getClassPool();
    }

    protected T transfer(T inState, int at) throws BadBytecode {
        T state = copyState(inState);
        int opcode = codeIterator.byteAt(at);

        switch (opcode) {
            case NOP:
                transferNOP(state, at);
                break;
            case ACONST_NULL:
                transferACONST_NULL(state, at);
                break;
            case ICONST_M1:
                transferICONST_M1(state, at);
                break;
            case ICONST_0:
                transferICONST_0(state, at);
                break;
            case ICONST_1:
                transferICONST_1(state, at);
                break;
            case ICONST_2:
                transferICONST_2(state, at);
                break;
            case ICONST_3:
                transferICONST_3(state, at);
                break;
            case ICONST_4:
                transferICONST_4(state, at);
                break;
            case ICONST_5:
                transferICONST_5(state, at);
                break;
            case LCONST_0:
                transferLCONST_0(state, at);
                break;
            case LCONST_1:
                transferLCONST_1(state, at);
                break;
            case FCONST_0:
                transferFCONST_0(state, at);
                break;
            case FCONST_1:
                transferFCONST_1(state, at);
                break;
            case FCONST_2:
                transferFCONST_2(state, at);
                break;
            case DCONST_0:
                transferDCONST_0(state, at);
                break;
            case DCONST_1:
                transferDCONST_1(state, at);
                break;
            case BIPUSH:
                transferBIPUSH(state, at);
                break;
            case SIPUSH:
                transferSIPUSH(state, at);
                break;
            case LDC:
                transferLDC(state, at);
                break;
            case LDC_W :
                transferLDC_W (state, at);
                break;
            case LDC2_W :
                transferLDC2_W (state, at);
                break;
            case ILOAD:
                transferILOAD(state, at, false);
                break;
            case LLOAD:
                transferLLOAD(state, at, false);
                break;
            case FLOAD:
                transferFLOAD(state, at, false);
                break;
            case DLOAD:
                transferDLOAD(state, at, false);
                break;
            case ALOAD:
                transferALOAD(state, at, false);
                break;
            case ILOAD_0:
                transferILOAD_0(state, at);
                break;
            case ILOAD_1:
                transferILOAD_1(state, at);
                break;
            case ILOAD_2:
                transferILOAD_2(state, at);
                break;
            case ILOAD_3:
                transferILOAD_3(state, at);
                break;
            case LLOAD_0:
                transferLLOAD_0(state, at);
                break;
            case LLOAD_1:
                transferLLOAD_1(state, at);
                break;
            case LLOAD_2:
                transferLLOAD_2(state, at);
                break;
            case LLOAD_3:
                transferLLOAD_3(state, at);
                break;
            case FLOAD_0:
                transferFLOAD_0(state, at);
                break;
            case FLOAD_1:
                transferFLOAD_1(state, at);
                break;
            case FLOAD_2:
                transferFLOAD_2(state, at);
                break;
            case FLOAD_3:
                transferFLOAD_3(state, at);
                break;
            case DLOAD_0:
                transferDLOAD_0(state, at);
                break;
            case DLOAD_1:
                transferDLOAD_1(state, at);
                break;
            case DLOAD_2:
                transferDLOAD_2(state, at);
                break;
            case DLOAD_3:
                transferDLOAD_3(state, at);
                break;
            case ALOAD_0:
                transferALOAD_0(state, at);
                break;
            case ALOAD_1:
                transferALOAD_1(state, at);
                break;
            case ALOAD_2:
                transferALOAD_2(state, at);
                break;
            case ALOAD_3:
                transferALOAD_3(state, at);
                break;
            case IALOAD:
                transferIALOAD(state, at);
                break;
            case LALOAD:
                transferLALOAD(state, at);
                break;
            case FALOAD:
                transferFALOAD(state, at);
                break;
            case DALOAD:
                transferDALOAD(state, at);
                break;
            case AALOAD:
                transferAALOAD(state, at);
                break;
            case BALOAD:
                transferBALOAD(state, at);
                break;
            case CALOAD:
                transferCALOAD(state, at);
                break;
            case SALOAD:
                transferSALOAD(state, at);
                break;
            case ISTORE:
                transferISTORE(state, at, false);
                break;
            case LSTORE:
                transferLSTORE(state, at, false);
                break;
            case FSTORE:
                transferFSTORE(state, at, false);
                break;
            case DSTORE:
                transferDSTORE(state, at, false);
                break;
            case ASTORE:
                transferASTORE(state, at, false);
                break;
            case ISTORE_0:
                transferISTORE_0(state, at);
                break;
            case ISTORE_1:
                transferISTORE_1(state, at);
                break;
            case ISTORE_2:
                transferISTORE_2(state, at);
                break;
            case ISTORE_3:
                transferISTORE_3(state, at);
                break;
            case LSTORE_0:
                transferLSTORE_0(state, at);
                break;
            case LSTORE_1:
                transferLSTORE_1(state, at);
                break;
            case LSTORE_2:
                transferLSTORE_2(state, at);
                break;
            case LSTORE_3:
                transferLSTORE_3(state, at);
                break;
            case FSTORE_0:
                transferFSTORE_0(state, at);
                break;
            case FSTORE_1:
                transferFSTORE_1(state, at);
                break;
            case FSTORE_2:
                transferFSTORE_2(state, at);
                break;
            case FSTORE_3:
                transferFSTORE_3(state, at);
                break;
            case DSTORE_0:
                transferDSTORE_0(state, at);
                break;
            case DSTORE_1:
                transferDSTORE_1(state, at);
                break;
            case DSTORE_2:
                transferDSTORE_2(state, at);
                break;
            case DSTORE_3:
                transferDSTORE_3(state, at);
                break;
            case ASTORE_0:
                transferASTORE_0(state, at);
                break;
            case ASTORE_1:
                transferASTORE_1(state, at);
                break;
            case ASTORE_2:
                transferASTORE_2(state, at);
                break;
            case ASTORE_3:
                transferASTORE_3(state, at);
                break;
            case IASTORE:
                transferIASTORE(state, at);
                break;
            case LASTORE:
                transferLASTORE(state, at);
                break;
            case FASTORE:
                transferFASTORE(state, at);
                break;
            case DASTORE:
                transferDASTORE(state, at);
                break;
            case AASTORE:
                transferAASTORE(state, at);
                break;
            case BASTORE:
                transferBASTORE(state, at);
                break;
            case CASTORE:
                transferCASTORE(state, at);
                break;
            case SASTORE:
                transferSASTORE(state, at);
                break;
            case POP:
                transferPOP(state, at);
                break;
            case POP2:
                transferPOP2(state, at);
                break;
            case DUP:
                transferDUP(state, at);
                break;
            case DUP_X1:
                transferDUP_X1(state, at);
                break;
            case DUP_X2:
                transferDUP_X2(state, at);
                break;
            case DUP2:
                transferDUP2(state, at);
                break;
            case DUP2_X1:
                transferDUP2_X1(state, at);
                break;
            case DUP2_X2:
                transferDUP2_X2(state, at);
                break;
            case SWAP:
                transferSWAP(state, at);
                break;
            case IADD:
                transferIADD(state, at);
                break;
            case LADD:
                transferLADD(state, at);
                break;
            case FADD:
                transferFADD(state, at);
                break;
            case DADD:
                transferDADD(state, at);
                break;
            case ISUB:
                transferISUB(state, at);
                break;
            case LSUB:
                transferLSUB(state, at);
                break;
            case FSUB:
                transferFSUB(state, at);
                break;
            case DSUB:
                transferDSUB(state, at);
                break;
            case IMUL:
                transferIMUL(state, at);
                break;
            case LMUL:
                transferLMUL(state, at);
                break;
            case FMUL:
                transferFMUL(state, at);
                break;
            case DMUL:
                transferDMUL(state, at);
                break;
            case IDIV:
                transferIDIV(state, at);
                break;
            case LDIV:
                transferLDIV(state, at);
                break;
            case FDIV:
                transferFDIV(state, at);
                break;
            case DDIV:
                transferDDIV(state, at);
                break;
            case IREM:
                transferIREM(state, at);
                break;
            case LREM:
                transferLREM(state, at);
                break;
            case FREM:
                transferFREM(state, at);
                break;
            case DREM:
                transferDREM(state, at);
                break;
            case INEG:
                transferINEG(state, at);
                break;
            case LNEG:
                transferLNEG(state, at);
                break;
            case FNEG:
                transferFNEG(state, at);
                break;
            case DNEG:
                transferDNEG(state, at);
                break;
            case ISHL:
                transferISHL(state, at);
                break;
            case LSHL:
                transferLSHL(state, at);
                break;
            case ISHR:
                transferISHR(state, at);
                break;
            case LSHR:
                transferLSHR(state, at);
                break;
            case IUSHR:
                transferIUSHR(state, at);
                break;
            case LUSHR:
                transferLUSHR(state, at);
                break;
            case IAND:
                transferIAND(state, at);
                break;
            case LAND:
                transferLAND(state, at);
                break;
            case IOR:
                transferIOR(state, at);
                break;
            case LOR:
                transferLOR(state, at);
                break;
            case IXOR:
                transferIXOR(state, at);
                break;
            case LXOR:
                transferLXOR(state, at);
                break;
            case IINC:
                transferIINC(state, at, false);
                break;
            case I2L:
                transferI2L(state, at);
                break;
            case I2F:
                transferI2F(state, at);
                break;
            case I2D:
                transferI2D(state, at);
                break;
            case L2I:
                transferL2I(state, at);
                break;
            case L2F:
                transferL2F(state, at);
                break;
            case L2D:
                transferL2D(state, at);
                break;
            case F2I:
                transferF2I(state, at);
                break;
            case F2L:
                transferF2L(state, at);
                break;
            case F2D:
                transferF2D(state, at);
                break;
            case D2I:
                transferD2I(state, at);
                break;
            case D2L:
                transferD2L(state, at);
                break;
            case D2F:
                transferD2F(state, at);
                break;
            case I2B:
                transferI2B(state, at);
                break;
            case I2C:
                transferI2C(state, at);
                break;
            case I2S:
                transferI2S(state, at);
                break;
            case LCMP:
                transferLCMP(state, at);
                break;
            case FCMPL:
                transferFCMPL(state, at);
                break;
            case FCMPG:
                transferFCMPG(state, at);
                break;
            case DCMPL:
                transferDCMPL(state, at);
                break;
            case DCMPG:
                transferDCMPG(state, at);
                break;
            case IFEQ:
                transferIFEQ(state, at);
                break;
            case IFNE:
                transferIFNE(state, at);
                break;
            case IFLT:
                transferIFLT(state, at);
                break;
            case IFGE:
                transferIFGE(state, at);
                break;
            case IFGT:
                transferIFGT(state, at);
                break;
            case IFLE:
                transferIFLE(state, at);
                break;
            case IF_ICMPEQ:
                transferIF_ICMPEQ(state, at);
                break;
            case IF_ICMPNE:
                transferIF_ICMPNE(state, at);
                break;
            case IF_ICMPLT:
                transferIF_ICMPLT(state, at);
                break;
            case IF_ICMPGE:
                transferIF_ICMPGE(state, at);
                break;
            case IF_ICMPGT:
                transferIF_ICMPGT(state, at);
                break;
            case IF_ICMPLE:
                transferIF_ICMPLE(state, at);
                break;
            case IF_ACMPEQ:
                transferIF_ACMPEQ(state, at);
                break;
            case IF_ACMPNE:
                transferIF_ACMPNE(state, at);
                break;
            case GOTO:
                transferGOTO(state, at);
                break;
            case JSR:
                transferJSR(state, at);
                break;
            case RET:
                transferRET(state, at, false);
                break;
            case TABLESWITCH:
                transferTABLESWITCH(state, at);
                break;
            case LOOKUPSWITCH:
                transferLOOKUPSWITCH(state, at);
                break;
            case IRETURN:
                transferIRETURN(state, at);
                break;
            case LRETURN:
                transferLRETURN(state, at);
                break;
            case FRETURN:
                transferFRETURN(state, at);
                break;
            case DRETURN:
                transferDRETURN(state, at);
                break;
            case ARETURN:
                transferARETURN(state, at);
                break;
            case RETURN:
                transferRETURN(state, at);
                break;
            case GETSTATIC:
                transferGETSTATIC(state, at);
                break;
            case PUTSTATIC:
                transferPUTSTATIC(state, at);
                break;
            case GETFIELD:
                transferGETFIELD(state, at);
                break;
            case PUTFIELD:
                transferPUTFIELD(state, at);
                break;
            case INVOKEVIRTUAL:
                transferINVOKEVIRTUAL(state, at);
                break;
            case INVOKESPECIAL:
                transferINVOKESPECIAL(state, at);
                break;
            case INVOKESTATIC:
                transferINVOKESTATIC(state, at);
                break;
            case INVOKEINTERFACE:
                transferINVOKEINTERFACE(state, at);
                break;
            case INVOKEDYNAMIC:
                transferINVOKEDYNAMIC(state, at);
                break;
            case NEW:
                transferNEW(state, at);
                break;
            case NEWARRAY:
                transferNEWARRAY(state, at);
                break;
            case ANEWARRAY:
                transferANEWARRAY(state, at);
                break;
            case ARRAYLENGTH:
                transferARRAYLENGTH(state, at);
                break;
            case ATHROW:
                transferATHROW(state, at);
                break;
            case CHECKCAST:
                transferCHECKCAST(state, at);
                break;
            case INSTANCEOF:
                transferINSTANCEOF(state, at);
                break;
            case MONITORENTER:
                transferMONITORENTER(state, at);
                break;
            case MONITOREXIT:
                transferMONITOREXIT(state, at);
                break;
            case WIDE:
                transferWIDE(state, at);
                break;
            case MULTIANEWARRAY:
                transferMULTIANEWARRAY(state, at);
                break;
            case IFNULL:
                transferIFNULL(state, at);
                break;
            case IFNONNULL:
                transferIFNONNULL(state, at);
                break;
            case GOTO_W:
                transferGOTO_W(state, at);
                break;
            case JSR_W:
                transferJSR_W(state, at);
                break;
        }

        return state;
    }

    protected abstract void transferNOP(T state, int at) throws BadBytecode;
    protected abstract void transferACONST_NULL(T state, int at) throws BadBytecode;
    protected abstract void transferICONST_M1(T state, int at) throws BadBytecode;
    protected abstract void transferICONST_0(T state, int at) throws BadBytecode;
    protected abstract void transferICONST_1(T state, int at) throws BadBytecode;
    protected abstract void transferICONST_2(T state, int at) throws BadBytecode;
    protected abstract void transferICONST_3(T state, int at) throws BadBytecode;
    protected abstract void transferICONST_4(T state, int at) throws BadBytecode;
    protected abstract void transferICONST_5(T state, int at) throws BadBytecode;
    protected abstract void transferLCONST_0(T state, int at) throws BadBytecode;
    protected abstract void transferLCONST_1(T state, int at) throws BadBytecode;
    protected abstract void transferFCONST_0(T state, int at) throws BadBytecode;
    protected abstract void transferFCONST_1(T state, int at) throws BadBytecode;
    protected abstract void transferFCONST_2(T state, int at) throws BadBytecode;
    protected abstract void transferDCONST_0(T state, int at) throws BadBytecode;
    protected abstract void transferDCONST_1(T state, int at) throws BadBytecode;
    protected abstract void transferBIPUSH(T state, int at) throws BadBytecode;
    protected abstract void transferSIPUSH(T state, int at) throws BadBytecode;
    protected abstract void transferLDC(T state, int at) throws BadBytecode;
    protected abstract void transferLDC_W (T state, int at) throws BadBytecode;
    protected abstract void transferLDC2_W (T state, int at) throws BadBytecode;
    protected abstract void transferILOAD(T state, int at, boolean isWide) throws BadBytecode;
    protected abstract void transferLLOAD(T state, int at, boolean isWide) throws BadBytecode;
    protected abstract void transferFLOAD(T state, int at, boolean isWide) throws BadBytecode;
    protected abstract void transferDLOAD(T state, int at, boolean isWide) throws BadBytecode;
    protected abstract void transferALOAD(T state, int at, boolean isWide) throws BadBytecode;
    protected abstract void transferILOAD_0(T state, int at) throws BadBytecode;
    protected abstract void transferILOAD_1(T state, int at) throws BadBytecode;
    protected abstract void transferILOAD_2(T state, int at) throws BadBytecode;
    protected abstract void transferILOAD_3(T state, int at) throws BadBytecode;
    protected abstract void transferLLOAD_0(T state, int at) throws BadBytecode;
    protected abstract void transferLLOAD_1(T state, int at) throws BadBytecode;
    protected abstract void transferLLOAD_2(T state, int at) throws BadBytecode;
    protected abstract void transferLLOAD_3(T state, int at) throws BadBytecode;
    protected abstract void transferFLOAD_0(T state, int at) throws BadBytecode;
    protected abstract void transferFLOAD_1(T state, int at) throws BadBytecode;
    protected abstract void transferFLOAD_2(T state, int at) throws BadBytecode;
    protected abstract void transferFLOAD_3(T state, int at) throws BadBytecode;
    protected abstract void transferDLOAD_0(T state, int at) throws BadBytecode;
    protected abstract void transferDLOAD_1(T state, int at) throws BadBytecode;
    protected abstract void transferDLOAD_2(T state, int at) throws BadBytecode;
    protected abstract void transferDLOAD_3(T state, int at) throws BadBytecode;
    protected abstract void transferALOAD_0(T state, int at) throws BadBytecode;
    protected abstract void transferALOAD_1(T state, int at) throws BadBytecode;
    protected abstract void transferALOAD_2(T state, int at) throws BadBytecode;
    protected abstract void transferALOAD_3(T state, int at) throws BadBytecode;
    protected abstract void transferIALOAD(T state, int at) throws BadBytecode;
    protected abstract void transferLALOAD(T state, int at) throws BadBytecode;
    protected abstract void transferFALOAD(T state, int at) throws BadBytecode;
    protected abstract void transferDALOAD(T state, int at) throws BadBytecode;
    protected abstract void transferAALOAD(T state, int at) throws BadBytecode;
    protected abstract void transferBALOAD(T state, int at) throws BadBytecode;
    protected abstract void transferCALOAD(T state, int at) throws BadBytecode;
    protected abstract void transferSALOAD(T state, int at) throws BadBytecode;
    protected abstract void transferISTORE(T state, int at, boolean isWide) throws BadBytecode;
    protected abstract void transferLSTORE(T state, int at, boolean isWide) throws BadBytecode;
    protected abstract void transferFSTORE(T state, int at, boolean isWide) throws BadBytecode;
    protected abstract void transferDSTORE(T state, int at, boolean isWide) throws BadBytecode;
    protected abstract void transferASTORE(T state, int at, boolean isWide) throws BadBytecode;
    protected abstract void transferISTORE_0(T state, int at) throws BadBytecode;
    protected abstract void transferISTORE_1(T state, int at) throws BadBytecode;
    protected abstract void transferISTORE_2(T state, int at) throws BadBytecode;
    protected abstract void transferISTORE_3(T state, int at) throws BadBytecode;
    protected abstract void transferLSTORE_0(T state, int at) throws BadBytecode;
    protected abstract void transferLSTORE_1(T state, int at) throws BadBytecode;
    protected abstract void transferLSTORE_2(T state, int at) throws BadBytecode;
    protected abstract void transferLSTORE_3(T state, int at) throws BadBytecode;
    protected abstract void transferFSTORE_0(T state, int at) throws BadBytecode;
    protected abstract void transferFSTORE_1(T state, int at) throws BadBytecode;
    protected abstract void transferFSTORE_2(T state, int at) throws BadBytecode;
    protected abstract void transferFSTORE_3(T state, int at) throws BadBytecode;
    protected abstract void transferDSTORE_0(T state, int at) throws BadBytecode;
    protected abstract void transferDSTORE_1(T state, int at) throws BadBytecode;
    protected abstract void transferDSTORE_2(T state, int at) throws BadBytecode;
    protected abstract void transferDSTORE_3(T state, int at) throws BadBytecode;
    protected abstract void transferASTORE_0(T state, int at) throws BadBytecode;
    protected abstract void transferASTORE_1(T state, int at) throws BadBytecode;
    protected abstract void transferASTORE_2(T state, int at) throws BadBytecode;
    protected abstract void transferASTORE_3(T state, int at) throws BadBytecode;
    protected abstract void transferIASTORE(T state, int at) throws BadBytecode;
    protected abstract void transferLASTORE(T state, int at) throws BadBytecode;
    protected abstract void transferFASTORE(T state, int at) throws BadBytecode;
    protected abstract void transferDASTORE(T state, int at) throws BadBytecode;
    protected abstract void transferAASTORE(T state, int at) throws BadBytecode;
    protected abstract void transferBASTORE(T state, int at) throws BadBytecode;
    protected abstract void transferCASTORE(T state, int at) throws BadBytecode;
    protected abstract void transferSASTORE(T state, int at) throws BadBytecode;
    protected abstract void transferPOP(T state, int at) throws BadBytecode;
    protected abstract void transferPOP2(T state, int at) throws BadBytecode;
    protected abstract void transferDUP(T state, int at) throws BadBytecode;
    protected abstract void transferDUP_X1(T state, int at) throws BadBytecode;
    protected abstract void transferDUP_X2(T state, int at) throws BadBytecode;
    protected abstract void transferDUP2(T state, int at) throws BadBytecode;
    protected abstract void transferDUP2_X1(T state, int at) throws BadBytecode;
    protected abstract void transferDUP2_X2(T state, int at) throws BadBytecode;
    protected abstract void transferSWAP(T state, int at) throws BadBytecode;
    protected abstract void transferIADD(T state, int at) throws BadBytecode;
    protected abstract void transferLADD(T state, int at) throws BadBytecode;
    protected abstract void transferFADD(T state, int at) throws BadBytecode;
    protected abstract void transferDADD(T state, int at) throws BadBytecode;
    protected abstract void transferISUB(T state, int at) throws BadBytecode;
    protected abstract void transferLSUB(T state, int at) throws BadBytecode;
    protected abstract void transferFSUB(T state, int at) throws BadBytecode;
    protected abstract void transferDSUB(T state, int at) throws BadBytecode;
    protected abstract void transferIMUL(T state, int at) throws BadBytecode;
    protected abstract void transferLMUL(T state, int at) throws BadBytecode;
    protected abstract void transferFMUL(T state, int at) throws BadBytecode;
    protected abstract void transferDMUL(T state, int at) throws BadBytecode;
    protected abstract void transferIDIV(T state, int at) throws BadBytecode;
    protected abstract void transferLDIV(T state, int at) throws BadBytecode;
    protected abstract void transferFDIV(T state, int at) throws BadBytecode;
    protected abstract void transferDDIV(T state, int at) throws BadBytecode;
    protected abstract void transferIREM(T state, int at) throws BadBytecode;
    protected abstract void transferLREM(T state, int at) throws BadBytecode;
    protected abstract void transferFREM(T state, int at) throws BadBytecode;
    protected abstract void transferDREM(T state, int at) throws BadBytecode;
    protected abstract void transferINEG(T state, int at) throws BadBytecode;
    protected abstract void transferLNEG(T state, int at) throws BadBytecode;
    protected abstract void transferFNEG(T state, int at) throws BadBytecode;
    protected abstract void transferDNEG(T state, int at) throws BadBytecode;
    protected abstract void transferISHL(T state, int at) throws BadBytecode;
    protected abstract void transferLSHL(T state, int at) throws BadBytecode;
    protected abstract void transferISHR(T state, int at) throws BadBytecode;
    protected abstract void transferLSHR(T state, int at) throws BadBytecode;
    protected abstract void transferIUSHR(T state, int at) throws BadBytecode;
    protected abstract void transferLUSHR(T state, int at) throws BadBytecode;
    protected abstract void transferIAND(T state, int at) throws BadBytecode;
    protected abstract void transferLAND(T state, int at) throws BadBytecode;
    protected abstract void transferIOR(T state, int at) throws BadBytecode;
    protected abstract void transferLOR(T state, int at) throws BadBytecode;
    protected abstract void transferIXOR(T state, int at) throws BadBytecode;
    protected abstract void transferLXOR(T state, int at) throws BadBytecode;
    protected abstract void transferIINC(T state, int at, boolean isWide) throws BadBytecode;
    protected abstract void transferI2L(T state, int at) throws BadBytecode;
    protected abstract void transferI2F(T state, int at) throws BadBytecode;
    protected abstract void transferI2D(T state, int at) throws BadBytecode;
    protected abstract void transferL2I(T state, int at) throws BadBytecode;
    protected abstract void transferL2F(T state, int at) throws BadBytecode;
    protected abstract void transferL2D(T state, int at) throws BadBytecode;
    protected abstract void transferF2I(T state, int at) throws BadBytecode;
    protected abstract void transferF2L(T state, int at) throws BadBytecode;
    protected abstract void transferF2D(T state, int at) throws BadBytecode;
    protected abstract void transferD2I(T state, int at) throws BadBytecode;
    protected abstract void transferD2L(T state, int at) throws BadBytecode;
    protected abstract void transferD2F(T state, int at) throws BadBytecode;
    protected abstract void transferI2B(T state, int at) throws BadBytecode;
    protected abstract void transferI2C(T state, int at) throws BadBytecode;
    protected abstract void transferI2S(T state, int at) throws BadBytecode;
    protected abstract void transferLCMP(T state, int at) throws BadBytecode;
    protected abstract void transferFCMPL(T state, int at) throws BadBytecode;
    protected abstract void transferFCMPG(T state, int at) throws BadBytecode;
    protected abstract void transferDCMPL(T state, int at) throws BadBytecode;
    protected abstract void transferDCMPG(T state, int at) throws BadBytecode;
    protected abstract void transferIFEQ(T state, int at) throws BadBytecode;
    protected abstract void transferIFNE(T state, int at) throws BadBytecode;
    protected abstract void transferIFLT(T state, int at) throws BadBytecode;
    protected abstract void transferIFGE(T state, int at) throws BadBytecode;
    protected abstract void transferIFGT(T state, int at) throws BadBytecode;
    protected abstract void transferIFLE(T state, int at) throws BadBytecode;
    protected abstract void transferIF_ICMPEQ(T state, int at) throws BadBytecode;
    protected abstract void transferIF_ICMPNE(T state, int at) throws BadBytecode;
    protected abstract void transferIF_ICMPLT(T state, int at) throws BadBytecode;
    protected abstract void transferIF_ICMPGE(T state, int at) throws BadBytecode;
    protected abstract void transferIF_ICMPGT(T state, int at) throws BadBytecode;
    protected abstract void transferIF_ICMPLE(T state, int at) throws BadBytecode;
    protected abstract void transferIF_ACMPEQ(T state, int at) throws BadBytecode;
    protected abstract void transferIF_ACMPNE(T state, int at) throws BadBytecode;
    protected abstract void transferGOTO(T state, int at) throws BadBytecode;
    protected abstract void transferJSR(T state, int at) throws BadBytecode;
    protected abstract void transferRET(T state, int at, boolean isWide) throws BadBytecode;
    protected abstract void transferTABLESWITCH(T state, int at) throws BadBytecode;
    protected abstract void transferLOOKUPSWITCH(T state, int at) throws BadBytecode;
    protected abstract void transferIRETURN(T state, int at) throws BadBytecode;
    protected abstract void transferLRETURN(T state, int at) throws BadBytecode;
    protected abstract void transferFRETURN(T state, int at) throws BadBytecode;
    protected abstract void transferDRETURN(T state, int at) throws BadBytecode;
    protected abstract void transferARETURN(T state, int at) throws BadBytecode;
    protected abstract void transferRETURN(T state, int at) throws BadBytecode;
    protected abstract void transferGETSTATIC(T state, int at) throws BadBytecode;
    protected abstract void transferPUTSTATIC(T state, int at) throws BadBytecode;
    protected abstract void transferGETFIELD(T state, int at) throws BadBytecode;
    protected abstract void transferPUTFIELD(T state, int at) throws BadBytecode;
    protected abstract void transferINVOKEVIRTUAL(T state, int at) throws BadBytecode;
    protected abstract void transferINVOKESPECIAL(T state, int at) throws BadBytecode;
    protected abstract void transferINVOKESTATIC(T state, int at) throws BadBytecode;
    protected abstract void transferINVOKEINTERFACE(T state, int at) throws BadBytecode;
    protected abstract void transferINVOKEDYNAMIC(T state, int at) throws BadBytecode;
    protected abstract void transferNEW(T state, int at) throws BadBytecode;
    protected abstract void transferNEWARRAY(T state, int at) throws BadBytecode;
    protected abstract void transferANEWARRAY(T state, int at) throws BadBytecode;
    protected abstract void transferARRAYLENGTH(T state, int at) throws BadBytecode;
    protected abstract void transferATHROW(T state, int at) throws BadBytecode;
    protected abstract void transferCHECKCAST(T state, int at) throws BadBytecode;
    protected abstract void transferINSTANCEOF(T state, int at) throws BadBytecode;
    protected abstract void transferMONITORENTER(T state, int at) throws BadBytecode;
    protected abstract void transferMONITOREXIT(T state, int at) throws BadBytecode;

    protected void transferWIDE(T state, int at) throws BadBytecode {
        int opcode = codeIterator.byteAt(at + 1);
        switch (opcode) {
            case ILOAD:
                transferILOAD(state, at, true);
                break;
            case LLOAD:
                transferLLOAD(state, at, true);
                break;
            case FLOAD:
                transferFLOAD(state, at, true);
                break;
            case DLOAD:
                transferDLOAD(state, at, true);
                break;
            case ALOAD:
                transferALOAD(state, at, true);
                break;
            case ISTORE:
                transferISTORE(state, at, true);
                break;
            case LSTORE:
                transferLSTORE(state, at, true);
                break;
            case FSTORE:
                transferFSTORE(state, at, true);
                break;
            case DSTORE:
                transferDSTORE(state, at, true);
                break;
            case ASTORE:
                transferASTORE(state, at, true);
                break;
            case IINC:
                transferIINC(state, at, true);
                break;
            case RET:
                transferRET(state, at, true);
                break;
            default:
                throw new BadBytecode("Invalid WIDE operand [position = " + at + "]: " + opcode);
        }
    }

    protected abstract void transferMULTIANEWARRAY(T state, int at) throws BadBytecode;
    protected abstract void transferIFNULL(T state, int at) throws BadBytecode;
    protected abstract void transferIFNONNULL(T state, int at) throws BadBytecode;
    protected abstract void transferGOTO_W(T state, int at) throws BadBytecode;
    protected abstract void transferJSR_W(T state, int at) throws BadBytecode;
}
