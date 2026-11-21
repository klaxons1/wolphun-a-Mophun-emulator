package wolphun.decomp;

import java.util.*;

import wolphun.decomp.Ir.*;
import wolphun.decomp.Program.*;

/**
 * Translates the instructions of each basic block into IR statements and a terminator.
 *
 * Semantics follow wolphun.vm.Pip2.step() exactly (operand fields are byte offsets into the
 * register file, so ".b"/".h" operations can address any byte/half of a register).
 *
 * Register saves: STORE/RESTORE/RET push and pop registers on the emulated stack. Callee-saved
 * registers are Java locals in the decompiled code, so they survive calls by construction, and
 * the pushes are dropped -- only the stack pointer moves, which keeps the frame layout
 * identical. Functions that RESTORE registers in the middle of their body (not via RET) get
 * the faithful push/pop sequence instead.
 *
 * Calls to local functions are emitted as placeholders: their arguments and whether they
 * produce r0 are only known after the interprocedural analysis (Analysis).
 */
final class Lifter {
    private final Program prog;
    private final Image img;
    private Func f;
    private Block blk;
    private List<Stmt> out;
    private int curPc;
    private final HashMap<Integer,Block> tailBlocks = new HashMap<Integer,Block>();

    Lifter(Program prog){ this.prog=prog; this.img=prog.img; }

    void liftAll(){
        for(Func fn : prog.funcs.values()) lift(fn);
    }

    private void lift(Func fn){
        f = fn; tailBlocks.clear();
        int n = f.blocks.size();
        for(int k=0;k<n;k++){
            blk = f.blocks.get(k);
            out = blk.stmts = new ArrayList<Stmt>();
            List<Insn> ins = blk.insns;
            for(int q=0;q<ins.size();q++){
                Insn i = ins.get(q);
                curPc = i.pc;
                boolean last = (q==ins.size()-1);
                // the table load of a recognised switch is folded into the SWITCH terminator
                // (the loaded value is the jump target itself and is dead afterwards)
                if(q==ins.size()-2 && ins.get(q+1).op==0x41 && prog.switches.containsKey(ins.get(q+1).pc)) continue;
                liftInsn(i, last);
            }
            if(blk.term==null){                    // block ended because the next address is a leader
                Insn li = ins.get(ins.size()-1);
                blk.term = gotoAddr(li.pc + li.size(), li.pc);
            }
        }
        // successor/predecessor lists
        for(Block b : f.blocks){
            b.succ = b.term.succ==null? new int[0] : b.term.succ;
            b.preds.clear();
        }
        for(Block b : f.blocks) for(int s : distinct(b.succ)) f.blocks.get(s).preds.add(b.id);
    }
    static int[] distinct(int[] a){
        LinkedHashSet<Integer> s=new LinkedHashSet<Integer>(); for(int x:a) s.add(x);
        int[] r=new int[s.size()]; int k=0; for(int x:s) r[k++]=x; return r;
    }

    // ------------------------------------------------------------------ control transfer
    /** terminator for "continue at address a" (intra-function edge or tail call) */
    private Term gotoAddr(int a, int pc){
        Term t = new Term(TermKind.GOTO); t.pc=pc;
        t.succ = new int[]{ blockFor(a, pc) };
        return t;
    }
    private int blockFor(int a, int pc){
        Block b = f.byAddr.get(a);
        if(b!=null) return b.id;
        // not part of this function: tail call to another function (or a trap)
        Block tb = tailBlocks.get(a);
        if(tb!=null) return tb.id;
        tb = new Block(); tb.start=a; tb.id=f.blocks.size();
        Term t;
        if(prog.funcs.containsKey(a)){
            Stmt cs = localCall(a); cs.pc=pc;
            tb.stmts.add(cs);
            t = new Term(TermKind.RETURN);
        } else {
            t = new Term(TermKind.TRAP); t.note=String.format("jump to 0x%x outside the code", a);
        }
        t.pc=pc; tb.term=t;
        f.blocks.add(tb); tailBlocks.put(a, tb);
        return tb.id;
    }

    // ------------------------------------------------------------------ operands
    /** register read; the pc register reads as the address of the next word (as in the interpreter) */
    private Expr rd(int r){
        if(r==Ir.PC){ prog.warnings.add(String.format("%s: pc read as an operand at 0x%x", f.name, curPc)); return new Const(curPc+4, Ir.CODE); }
        return Ir.reg(r);
    }
    private Expr r32(int o){ return rd(o>>2); }
    private Expr r16(int o){
        int sh=(o&2)*8; Expr b=rd(o>>2);
        if(sh!=0) b=new Bin(Op.SHRU, b, new Const(sh));
        return new Bin(Op.AND, b, new Const(0xffff));
    }
    private Expr r8(int o){
        int sh=(o&3)*8; Expr b=rd(o>>2);
        if(sh!=0) b=new Bin(Op.SHRU, b, new Const(sh));
        return new Bin(Op.AND, b, new Const(0xff));
    }
    private void emit(Stmt s){ s.pc=curPc; out.add(s); }
    private void w32(int o, Expr v){
        int r=o>>2;
        if(r==Ir.ZERO) { if(v.hasCall()) emit(new Eval(v)); return; }
        if(r==Ir.PC){ trap("write to the pc register"); return; }
        emit(new Assign(r, v));
    }
    private void w16(int o, Expr v){
        int r=o>>2; if(r==Ir.ZERO) return;
        if(r==Ir.PC){ trap("write to the pc register"); return; }
        emit(new Assign(r, new SetPart(Ir.reg(r), v, (o&2)*8, 16)));
    }
    private void w8(int o, Expr v){
        int r=o>>2; if(r==Ir.ZERO) return;
        if(r==Ir.PC){ trap("write to the pc register"); return; }
        emit(new Assign(r, new SetPart(Ir.reg(r), v, (o&3)*8, 8)));
    }
    private void trap(String why){
        prog.warnings.add(String.format("%s: %s at 0x%x", f.name, why, curPc));
        emit(new Comment("unsupported: "+why));
    }

    /** the extra word as an expression (symbolic when it is a relocated address) */
    private Expr imm(Insn i){
        if(i.extIsRaw()) return new Const(i.raw());
        int idx=i.poolIdx();
        if(idx<0 || idx>=img.poolCount()) return new Const(0);
        int v = img.poolValue(idx);
        int type = img.pool(idx).type;
        if(type==7 || type==5 || type==6 || img.poolIsImport(idx)) return new Const(v);
        if(img.inCode(v)) return new Const(v, Ir.CODE);
        if(img.inData(v) || img.inBss(v) || v==img.bssEnd || v==img.dataEnd) return new Const(v, Ir.DATA);
        return new Const(v);
    }
    private Expr addr(int sOff, Insn i){
        Expr base = r32(sOff), im = imm(i);
        if(base.isConst() && ((Const)base).v==0) return im;
        if(im.isConst() && ((Const)im).v==0 && ((Const)im).kind==Ir.NUM) return base;
        return new Bin(Op.ADD, base, im);
    }
    private static Expr c(int v){ return new Const(v); }
    private static Expr bin(Op op, Expr a, Expr b){ return new Bin(op, a, b); }

    private Stmt localCall(int target){
        return new Eval(new Call(CallKind.LOCAL, target, null, null, new Expr[0]));
    }
    private Stmt importCall(String name){
        ImportSigs.Sig sig = ImportSigs.get(name);
        Expr[] args = new Expr[sig.regArgs()];
        for(int k=0;k<args.length;k++) args[k]=Ir.reg(Ir.P0+k);
        Call call = new Call(CallKind.IMPORT, 0, name, null, args);
        return sig.returnsValue? new Assign(Ir.R0, call) : new Eval(call);
    }
    private Expr rt(String name, Expr... args){ return new Call(CallKind.RUNTIME, 0, name, null, args); }
    private Expr[] p0to3(){ return new Expr[]{ Ir.reg(Ir.P0), Ir.reg(Ir.P1), Ir.reg(Ir.P2), Ir.reg(Ir.P3) }; }

    // ------------------------------------------------------------------ STORE / RESTORE
    private static int pushBytes(int start, int count){
        if(start==0) return 4;
        int n=0; for(int i=start;i<start+count;i+=4) n++;
        return 4*n;
    }
    private void spAdd(int delta){ if(delta!=0) emit(new Assign(Ir.SP, bin(Op.ADD, Ir.reg(Ir.SP), c(delta)))); }
    private void store(int start, int count){
        if(f.elideSaves){ spAdd(-pushBytes(start,count)); return; }
        if(start==0){ spAdd(-4); emit(new Store(4, Ir.reg(Ir.SP), c(0))); return; }
        for(int i=start;i<start+count;i+=4){
            spAdd(-4);
            int r=i>>2;
            emit(new Store(4, Ir.reg(Ir.SP), r==Ir.RA? c(0) : Ir.reg(r)));
        }
    }
    private void restore(int start, int count){
        if(f.elideSaves){ spAdd(pushBytes(start,count)); return; }
        if(start==0){ spAdd(4); return; }
        for(int i=start;i>start-count;i-=4){
            int r=i>>2;
            if(r!=Ir.RA && r!=Ir.ZERO && r!=Ir.SP) emit(new Assign(r, new Load(4, false, Ir.reg(Ir.SP))));
            spAdd(4);
        }
    }

    // ------------------------------------------------------------------ the instructions
    private void condTerm(Insn i, Expr cond){
        Term t = new Term(TermKind.IF); t.pc=i.pc; t.cond=cond;
        int taken = blockFor(prog.branchTarget(i), i.pc);
        int fall  = blockFor(i.pc + i.size(), i.pc);
        t.succ = new int[]{ taken, fall };
        blk.term = t;
    }

    private void liftInsn(Insn i, boolean last){
        int d=i.d, s=i.s, t=i.t, op=i.op;
        int sx8s=(byte)s, sx8t=(byte)t;
        switch(op){
            case 0x00: case 0x01: break;                                       // BREAKPOINT, NOP
            case 0x02: w32(d, bin(Op.ADD, r32(s), r32(t))); break;
            case 0x03: w32(d, bin(Op.AND, r32(s), r32(t))); break;
            case 0x04: w32(d, bin(Op.MUL, r32(s), r32(t))); break;
            case 0x05: w32(d, rt("div", r32(s), r32(t))); break;
            case 0x06: w32(d, rt("divu", r32(s), r32(t))); break;
            case 0x07: w32(d, bin(Op.OR,  r32(s), r32(t))); break;
            case 0x08: w32(d, bin(Op.XOR, r32(s), r32(t))); break;
            case 0x09: w32(d, bin(Op.SUB, r32(s), r32(t))); break;
            case 0x0A: w32(d, bin(Op.SHL, r32(s), r32(t))); break;              // Java masks the count with 31, as the interpreter does
            case 0x0B: w32(d, bin(Op.SHR, r32(s), r32(t))); break;
            case 0x0C: w32(d, bin(Op.SHRU,r32(s), r32(t))); break;
            case 0x0D: w32(d, new Un(UOp.NOT, r32(s))); break;
            case 0x0E: w32(d, new Un(UOp.NEG, r32(s))); break;
            case 0x0F: w32(d, new Un(UOp.SEXT8,  r8(s))); break;
            case 0x10: w32(d, new Un(UOp.SEXT16, r16(s))); break;
            case 0x11: w32(d, r32(s)); break;
            case 0x12: w8(d, bin(Op.ADD, r8(s), r8(t))); break;
            case 0x13: w8(d, bin(Op.SUB, r8(s), r8(t))); break;
            case 0x14: w8(d, bin(Op.AND, r8(s), r8(t))); break;
            case 0x15: w8(d, bin(Op.OR,  r8(s), r8(t))); break;
            case 0x16: w8(d, r8(s)); break;
            case 0x17: w16(d, bin(Op.ADD, r16(s), r16(t))); break;
            case 0x18: w16(d, bin(Op.SUB, r16(s), r16(t))); break;
            case 0x19: w16(d, bin(Op.AND, r16(s), r16(t))); break;
            case 0x1A: w16(d, bin(Op.OR,  r16(s), r16(t))); break;
            case 0x1B: w16(d, r16(s)); break;
            case 0x1C: w32(d, bin(Op.SHL, r32(s), c(t))); break;
            case 0x1D: w32(d, bin(Op.SHR, r32(s), c(t))); break;
            case 0x1E: w32(d, bin(Op.SHRU,r32(s), c(t))); break;
            case 0x1F: w32(d, bin(Op.ADD, r32(s), c(sx8t))); break;
            case 0x20: w32(d, bin(Op.MUL, r32(s), c(t))); break;
            case 0x21: w8(d, bin(Op.ADD, r8(s), c(t))); break;
            case 0x22: w8(d, bin(Op.AND, r8(s), c(t))); break;
            case 0x23: w8(d, bin(Op.OR,  r8(s), c(t))); break;
            case 0x24: w8(d, bin(Op.SHL, r8(s), c(t))); break;
            case 0x25: w8(d, bin(Op.SHRU,r8(s), c(t))); break;
            case 0x26: w8(d, bin(Op.SHR, new Un(UOp.SEXT8, r8(s)), c(t))); break;
            case 0x27: w16(d, bin(Op.ADD, r16(s), c(sx8t))); break;
            case 0x28: w16(d, bin(Op.AND, r16(s), c(t))); break;
            case 0x29: w16(d, bin(Op.SHL, r16(s), c(t))); break;
            case 0x2A: w16(d, bin(Op.SHRU,r16(s), c(t))); break;
            case 0x2B: w16(d, bin(Op.SHR, new Un(UOp.SEXT16, r16(s)), c(t))); break;
            // ---- compare with an immediate, branch offset in t
            case 0x2C: condTerm(i, bin(Op.EQ,  r32(d), c(sx8s))); break;
            case 0x2D: condTerm(i, bin(Op.NE,  r32(d), c(sx8s))); break;
            case 0x2E: condTerm(i, bin(Op.GE,  r32(d), c(sx8s))); break;
            case 0x2F: condTerm(i, bin(Op.GEU, r32(d), c(s))); break;
            case 0x30: condTerm(i, bin(Op.GT,  r32(d), c(sx8s))); break;
            case 0x31: condTerm(i, bin(Op.GTU, r32(d), c(s))); break;
            case 0x32: condTerm(i, bin(Op.LE,  r32(d), c(sx8s))); break;
            case 0x33: condTerm(i, bin(Op.LEU, r32(d), c(s))); break;
            case 0x34: condTerm(i, bin(Op.LT,  r32(d), c(sx8s))); break;
            case 0x35: condTerm(i, bin(Op.LTU, r32(d), c(s))); break;
            case 0x36: condTerm(i, bin(Op.EQ, r8(d), c(s))); break;
            case 0x37: condTerm(i, bin(Op.NE, r8(d), c(s))); break;
            case 0x38: condTerm(i, bin(Op.GE, new Un(UOp.SEXT8, r8(d)), c(sx8s))); break;
            case 0x39: condTerm(i, bin(Op.GE, r8(d), c(s))); break;
            case 0x3A: condTerm(i, bin(Op.GT, new Un(UOp.SEXT8, r8(d)), c(sx8s))); break;
            case 0x3B: condTerm(i, bin(Op.GT, r8(d), c(s))); break;
            case 0x3C: condTerm(i, bin(Op.LE, new Un(UOp.SEXT8, r8(d)), c(sx8s))); break;
            case 0x3D: condTerm(i, bin(Op.LE, r8(d), c(s))); break;
            case 0x3E: condTerm(i, bin(Op.LT, new Un(UOp.SEXT8, r8(d)), c(sx8s))); break;
            case 0x3F: condTerm(i, bin(Op.LT, r8(d), c(s))); break;
            case 0x40: w32(d, c((short)(i.word>>>16))); break;                // LDQ
            case 0x41: jpr(i); break;                                          // JPr
            case 0x42: emit(new Assign(Ir.R0, new Call(CallKind.INDIRECT, 0, null, r32(d), p0to3()))); break; // CALLr
            case 0x43: store(d, s); break;
            case 0x44: restore(d, s); break;
            case 0x45: {                                                       // RET
                restore(d, s);
                Term tm = new Term(TermKind.RETURN); tm.pc=i.pc; blk.term=tm; break;
            }
            case 0x46: { Term tm=new Term(TermKind.HALT); tm.pc=i.pc; blk.term=tm; break; } // KILLTASK
            case 0x47: emit(new Eval(rt("sleep"))); break;                    // SLEEP (yield)
            case 0x48: emit(new Eval(rt("memcpy", r32(d), r32(s), r32(t)))); break;
            case 0x49: emit(new Eval(rt("memset", r32(d), r8(s), r32(t)))); break;
            case 0x4A: w32(d, bin(Op.ADD, r32(s), imm(i))); break;
            case 0x4B: w32(d, bin(Op.AND, r32(s), imm(i))); break;
            case 0x4C: w32(d, bin(Op.MUL, r32(s), imm(i))); break;
            case 0x4D: { int v=prog.immValue(i); w32(d, v==0? c(0) : bin(Op.DIV, r32(s), imm(i))); break; }
            case 0x4E: { int v=prog.immValue(i); w32(d, v==0? c(0) : rt("divu", r32(s), imm(i))); break; }
            case 0x4F: w32(d, bin(Op.OR,  r32(s), imm(i))); break;
            case 0x50: w32(d, bin(Op.XOR, r32(s), imm(i))); break;
            case 0x51: w32(d, bin(Op.SUB, r32(s), imm(i))); break;
            case 0x52: emit(new Store(1, addr(s,i), r8(d))); break;
            case 0x53: emit(new Store(2, addr(s,i), r16(d))); break;
            case 0x54: emit(new Store(4, addr(s,i), r32(d))); break;
            case 0x55: w32(d, new Load(1, true,  addr(s,i))); break;
            case 0x56: w32(d, new Load(2, true,  addr(s,i))); break;
            case 0x57: w32(d, new Load(4, false, addr(s,i))); break;
            case 0x58: w32(d, new Load(1, false, addr(s,i))); break;
            case 0x59: w32(d, new Load(2, false, addr(s,i))); break;
            case 0x5A: w32(d, imm(i)); break;                                  // LDI
            case 0x5B: blk.term = gotoAddr(prog.jumpTarget(i), i.pc); break;   // JPl
            case 0x5C: {                                                       // CALLl
                String imp = prog.importOf(i);
                if(imp!=null){
                    emit(importCall(imp));
                    if(prog.noReturn(i)){ Term tm=new Term(TermKind.HALT); tm.pc=i.pc; tm.note=imp; blk.term=tm; }
                }
                else {
                    int tg = prog.jumpTarget(i);
                    if(prog.funcs.containsKey(tg)) emit(localCall(tg));
                    else trap(String.format("call to unknown address 0x%x", tg));
                }
                break;
            }
            case 0x5D: condTerm(i, bin(Op.EQ,  r32(d), r32(s))); break;
            case 0x5E: condTerm(i, bin(Op.NE,  r32(d), r32(s))); break;
            case 0x5F: condTerm(i, bin(Op.GE,  r32(d), r32(s))); break;
            case 0x60: condTerm(i, bin(Op.GEU, r32(d), r32(s))); break;
            case 0x61: condTerm(i, bin(Op.GT,  r32(d), r32(s))); break;
            case 0x62: condTerm(i, bin(Op.GTU, r32(d), r32(s))); break;
            case 0x63: condTerm(i, bin(Op.LE,  r32(d), r32(s))); break;
            case 0x64: condTerm(i, bin(Op.LEU, r32(d), r32(s))); break;
            case 0x65: condTerm(i, bin(Op.LT,  r32(d), r32(s))); break;
            case 0x66: condTerm(i, bin(Op.LTU, r32(d), r32(s))); break;
            default: {
                trap(String.format("invalid opcode 0x%02x", op));
                Term tm=new Term(TermKind.TRAP); tm.pc=i.pc; tm.note=String.format("invalid opcode 0x%02x at 0x%x", op, i.pc);
                blk.term=tm;
            }
        }
    }

    private void jpr(Insn i){
        SwitchTable st = prog.switches.get(i.pc);
        if(st!=null){
            Term tm = new Term(TermKind.SWITCH); tm.pc=i.pc;
            // selector: the index before it was scaled -- the SLLi result is shifted back so the
            // statement stays exact even when the scaled register is reused (see Analysis).
            tm.cond = new Bin(Op.SHRU, Ir.reg(st.shiftReg>>2), new Const(2));
            tm.succ = new int[st.count]; tm.caseVals = new int[st.count];
            for(int k=0;k<st.count;k++){ tm.succ[k]=blockFor(st.targets[k], i.pc); tm.caseVals[k]=k; }
            blk.term = tm;
            return;
        }
        int r = i.d>>2;
        if(r==Ir.RA){ Term tm=new Term(TermKind.RETURN); tm.pc=i.pc; blk.term=tm; return; }
        // computed jump: an indirect tail call
        emit(new Assign(Ir.R0, new Call(CallKind.INDIRECT, 0, null, r32(i.d), p0to3())));
        Term tm=new Term(TermKind.RETURN); tm.pc=i.pc; blk.term=tm;
        prog.warnings.add(String.format("%s: computed jump at 0x%x treated as an indirect tail call", f.name, i.pc));
    }
}
