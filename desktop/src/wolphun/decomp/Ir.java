package wolphun.decomp;

import java.util.ArrayList;
import java.util.List;

/**
 * Intermediate representation the PIP2 instructions are lifted into.
 *
 * Registers are numbered by their index in the register file (byte offset / 4):
 * 0 zero, 1 sp, 2 ra, 3 fp, 4..11 s0..s7, 12..15 p0..p3, 16..29 g0..g13, 30 r0, 31 r1, 32 pc.
 * Everything except zero/sp/pc becomes a Java local of the decompiled method; sp is a field
 * of the runtime (the stack lives in emulated memory, because games pass addresses of stack
 * buffers to the SDK).
 */
final class Ir {
    private Ir(){}

    static final int ZERO=0, SP=1, RA=2, FP=3, S0=4, P0=12, P1=13, P2=14, P3=15, G0=16, R0=30, R1=31, PC=32;
    static final String[] REG_NAMES = new String[33];
    static {
        REG_NAMES[0]="zero"; REG_NAMES[1]="sp"; REG_NAMES[2]="ra"; REG_NAMES[3]="fp";
        for(int i=0;i<8;i++)  REG_NAMES[4+i]="s"+i;
        for(int i=0;i<4;i++)  REG_NAMES[12+i]="p"+i;
        for(int i=0;i<14;i++) REG_NAMES[16+i]="g"+i;
        REG_NAMES[30]="r0"; REG_NAMES[31]="r1"; REG_NAMES[32]="pc";
    }
    static String regName(int r){ return r>=0 && r<REG_NAMES.length? REG_NAMES[r] : "bad"+r; }
    /** registers that become Java locals (and may be parameters) */
    static boolean isLocal(int r){ return r>=2 && r<=31; }

    // =====================================================================================
    // Expressions
    // =====================================================================================
    enum Op {
        ADD("+",11), SUB("-",11), MUL("*",12), DIV("/",12), DIVU(null,15), AND("&",7), OR("|",5), XOR("^",6),
        SHL("<<",10), SHR(">>",10), SHRU(">>>",10),
        EQ("==",8), NE("!=",8), LT("<",9), LE("<=",9), GT(">",9), GE(">=",9),
        LTU(null,15), LEU(null,15), GTU(null,15), GEU(null,15),
        LAND("&&",4), LOR("||",3);
        final String sym; final int prec;
        Op(String sym,int prec){ this.sym=sym; this.prec=prec; }
        boolean isCompare(){ return ordinal()>=EQ.ordinal() && ordinal()<=GEU.ordinal(); }
        boolean isBool(){ return isCompare() || this==LAND || this==LOR; }
        Op negate(){
            switch(this){
                case EQ: return NE; case NE: return EQ;
                case LT: return GE; case GE: return LT; case LE: return GT; case GT: return LE;
                case LTU: return GEU; case GEU: return LTU; case LEU: return GTU; case GTU: return LEU;
                default: return null;
            }
        }
        Op swap(){
            switch(this){
                case LT: return GT; case GT: return LT; case LE: return GE; case GE: return LE;
                case LTU: return GTU; case GTU: return LTU; case LEU: return GEU; case GEU: return LEU;
                default: return this;
            }
        }
    }
    enum UOp { NOT("~"), NEG("-"), SEXT8("(byte) "), SEXT16("(short) "), LNOT("!");
        final String sym; UOp(String s){ sym=s; } }

    static abstract class Expr {
        /** bitmask of registers read by this expression */
        abstract long regs();
        abstract boolean hasLoad();
        abstract boolean hasCall();
        abstract Expr copy();
        boolean isConst(){ return this instanceof Const; }
        boolean isBool(){ return false; }
    }

    static final int NUM=0, CODE=1, DATA=2;
    static final class Const extends Expr {
        final int v; final int kind;   // NUM: plain number; CODE/DATA: an address (printed symbolically)
        Const(int v){ this(v,NUM); }
        Const(int v,int kind){ this.v=v; this.kind=kind; }
        long regs(){ return 0; } boolean hasLoad(){ return false; } boolean hasCall(){ return false; }
        Expr copy(){ return this; }
    }

    static final class Reg extends Expr {
        final int r;
        Reg(int r){ this.r=r; }
        long regs(){ return 1L<<r; } boolean hasLoad(){ return false; } boolean hasCall(){ return false; }
        Expr copy(){ return this; }
    }

    static final class Bin extends Expr {
        final Op op; Expr a, b;
        Bin(Op op, Expr a, Expr b){ this.op=op; this.a=a; this.b=b; }
        long regs(){ return a.regs()|b.regs(); }
        boolean hasLoad(){ return a.hasLoad()||b.hasLoad(); }
        boolean hasCall(){ return a.hasCall()||b.hasCall(); }
        Expr copy(){ return new Bin(op, a.copy(), b.copy()); }
        boolean isBool(){ return op.isBool(); }
    }

    static final class Un extends Expr {
        final UOp op; Expr a;
        Un(UOp op, Expr a){ this.op=op; this.a=a; }
        long regs(){ return a.regs(); }
        boolean hasLoad(){ return a.hasLoad(); }
        boolean hasCall(){ return a.hasCall(); }
        Expr copy(){ return new Un(op, a.copy()); }
        boolean isBool(){ return op==UOp.LNOT; }
    }

    /** memory read: width 1/2/4 bytes, sign- or zero-extended */
    static final class Load extends Expr {
        final int width; final boolean signed; Expr addr;
        Load(int width, boolean signed, Expr addr){ this.width=width; this.signed=signed; this.addr=addr; }
        long regs(){ return addr.regs(); }
        boolean hasLoad(){ return true; }
        boolean hasCall(){ return addr.hasCall(); }
        Expr copy(){ return new Load(width, signed, addr.copy()); }
    }

    /** write `bits` bits of val into old at bit position shift (PIP2 byte/half register ops) */
    static final class SetPart extends Expr {
        Expr old, val; final int shift, bits;
        SetPart(Expr old, Expr val, int shift, int bits){ this.old=old; this.val=val; this.shift=shift; this.bits=bits; }
        long regs(){ return old.regs()|val.regs(); }
        boolean hasLoad(){ return old.hasLoad()||val.hasLoad(); }
        boolean hasCall(){ return old.hasCall()||val.hasCall(); }
        Expr copy(){ return new SetPart(old.copy(), val.copy(), shift, bits); }
    }

    enum CallKind { LOCAL, IMPORT, INDIRECT, RUNTIME }
    static final class Call extends Expr {
        final CallKind kind;
        final int target;          // LOCAL: function address
        final String name;         // IMPORT/RUNTIME: name
        Expr fptr;                 // INDIRECT: target expression
        Expr[] args;
        Call(CallKind kind, int target, String name, Expr fptr, Expr[] args){
            this.kind=kind; this.target=target; this.name=name; this.fptr=fptr; this.args=args;
        }
        long regs(){ long m = fptr!=null? fptr.regs() : 0; for(Expr e: args) m|=e.regs(); return m; }
        boolean hasLoad(){ if(fptr!=null && fptr.hasLoad()) return true; for(Expr e: args) if(e.hasLoad()) return true; return false; }
        boolean hasCall(){ return true; }
        Expr copy(){
            Expr[] a=new Expr[args.length]; for(int i=0;i<a.length;i++) a[i]=args[i].copy();
            return new Call(kind, target, name, fptr==null?null:fptr.copy(), a);
        }
    }

    // =====================================================================================
    // Statements (straight-line, inside a basic block)
    // =====================================================================================
    static abstract class Stmt { int pc; }

    static final class Assign extends Stmt {
        final int r; Expr e;
        Assign(int r, Expr e){ this.r=r; this.e=e; }
    }
    static final class Store extends Stmt {
        final int width; Expr addr, val;
        Store(int width, Expr addr, Expr val){ this.width=width; this.addr=addr; this.val=val; }
    }
    /** expression evaluated for its side effect (a call) */
    static final class Eval extends Stmt {
        Expr e;
        Eval(Expr e){ this.e=e; }
    }
    static final class Comment extends Stmt {
        final String text;
        Comment(String t){ text=t; }
    }

    // =====================================================================================
    // Block terminators
    // =====================================================================================
    enum TermKind { GOTO, IF, SWITCH, RETURN, HALT, TRAP }
    static final class Term {
        TermKind kind;
        Expr cond;            // IF condition / SWITCH selector / RETURN value (null = void)
        int[] succ;           // successor block ids: GOTO {t}; IF {taken, notTaken}; SWITCH targets per case
        int[] caseVals;       // SWITCH: case value for succ[i]
        String note;          // TRAP: message
        int pc;
        Term(TermKind k){ kind=k; }
    }

    // =====================================================================================
    // helpers
    // =====================================================================================
    static Expr reg(int r){ return r==ZERO? new Const(0) : new Reg(r); }

    static List<Expr> children(Expr e){
        List<Expr> l = new ArrayList<Expr>(4);
        if(e instanceof Bin){ l.add(((Bin)e).a); l.add(((Bin)e).b); }
        else if(e instanceof Un) l.add(((Un)e).a);
        else if(e instanceof Load) l.add(((Load)e).addr);
        else if(e instanceof SetPart){ l.add(((SetPart)e).old); l.add(((SetPart)e).val); }
        else if(e instanceof Call){ Call c=(Call)e; if(c.fptr!=null) l.add(c.fptr); for(Expr a: c.args) l.add(a); }
        return l;
    }

    /** number of occurrences of register r in e */
    static int countReg(Expr e, int r){
        if(e instanceof Reg) return ((Reg)e).r==r? 1:0;
        int n=0; for(Expr c: children(e)) n+=countReg(c, r); return n;
    }

    /** replace every Reg(r) in e by `with` (returns the new root) */
    static Expr replaceReg(Expr e, int r, Expr with){
        if(e instanceof Reg) return ((Reg)e).r==r? with : e;
        if(e instanceof Bin){ Bin b=(Bin)e; b.a=replaceReg(b.a,r,with); b.b=replaceReg(b.b,r,with); }
        else if(e instanceof Un){ Un u=(Un)e; u.a=replaceReg(u.a,r,with); }
        else if(e instanceof Load){ Load l=(Load)e; l.addr=replaceReg(l.addr,r,with); }
        else if(e instanceof SetPart){ SetPart s=(SetPart)e; s.old=replaceReg(s.old,r,with); s.val=replaceReg(s.val,r,with); }
        else if(e instanceof Call){ Call c=(Call)e; if(c.fptr!=null) c.fptr=replaceReg(c.fptr,r,with);
            for(int i=0;i<c.args.length;i++) c.args[i]=replaceReg(c.args[i],r,with); }
        return e;
    }

    static int size(Expr e){ int n=1; for(Expr c: children(e)) n+=size(c); return n; }

    /** expressions read by a statement */
    static List<Expr> exprsOf(Stmt s){
        List<Expr> l=new ArrayList<Expr>(2);
        if(s instanceof Assign) l.add(((Assign)s).e);
        else if(s instanceof Store){ l.add(((Store)s).addr); l.add(((Store)s).val); }
        else if(s instanceof Eval) l.add(((Eval)s).e);
        return l;
    }
    static long regsRead(Stmt s){ long m=0; for(Expr e: exprsOf(s)) m|=e.regs(); return m; }
    static boolean hasCall(Stmt s){ for(Expr e: exprsOf(s)) if(e.hasCall()) return true; return false; }
    static boolean hasLoad(Stmt s){ for(Expr e: exprsOf(s)) if(e.hasLoad()) return true; return false; }
    static boolean writesMemory(Stmt s){ return s instanceof Store || hasCall(s); }

    static Expr not(Expr c){
        if(c instanceof Bin){
            Bin b=(Bin)c;
            Op n=b.op.negate();
            if(n!=null) return new Bin(n, b.a, b.b);
            if(b.op==Op.LAND) return new Bin(Op.LOR, not(b.a), not(b.b));
            if(b.op==Op.LOR)  return new Bin(Op.LAND, not(b.a), not(b.b));
        }
        if(c instanceof Un && ((Un)c).op==UOp.LNOT) return ((Un)c).a;
        return new Un(UOp.LNOT, c);
    }
}
