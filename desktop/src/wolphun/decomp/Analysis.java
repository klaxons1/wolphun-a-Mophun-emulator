package wolphun.decomp;

import java.util.*;

import wolphun.decomp.Ir.*;
import wolphun.decomp.Program.*;

/**
 * Data-flow analyses and IR clean-up.
 *
 * <h3>Liveness with demanded widths</h3>
 * The liveness of every register is tracked as a width: 0 (dead), 8, 16 or 32 bits. PIP2 has
 * byte/half operations that write only part of a register ({@code ADDBi r0.b = p0.b + 1}),
 * which lift to {@code r0 = setB(r0, p0 + 1)}. When only the low byte of the result is ever
 * consumed (stored with STB, compared with BEQIB, ...), the merge with the old upper bits is
 * irrelevant and the statement becomes {@code r0 = p0 + 1}.
 *
 * <h3>Interprocedural signatures</h3>
 * The parameters of a function are the registers live at its entry (normally a prefix of
 * p0..p3, but any register is allowed, so hand-written helpers stay correct). A function
 * returns a value when it may write r0 and some caller reads r0 after calling it (or its
 * address is taken). Both are solved as a fixpoint over the call graph.
 */
final class Analysis {
    private final Program prog;
    Analysis(Program p){ prog=p; }

    // per-block liveness, indexed by block id
    private final HashMap<Func, byte[][]> liveIn = new HashMap<Func, byte[][]>();
    private final HashMap<Func, byte[][]> liveOut = new HashMap<Func, byte[][]>();

    /** argument registers an indirect call can need: max over address-taken functions */
    private int indirectArity = 0;
    /** bits of p0..p3 that some address-taken function looks at */
    private final int[] indirectW = new int[4];
    /** width of every register a function reads before writing it (snapshot after the params fixpoint) */
    private final HashMap<Func, byte[]> entryW = new HashMap<Func, byte[]>();

    void run(){
        computeMayDefR0();
        for(Func f : prog.funcs.values()) f.returns = f.mayDefR0;
        for(int outer=0; outer<30; outer++){
            solveParams();
            HashSet<Integer> used = new HashSet<Integer>();
            for(Func f : prog.funcs.values()) collectResultUses(f, used);
            boolean ch=false;
            for(Func g : prog.funcs.values()){
                boolean r = g.mayDefR0 && (g.addressTaken || used.contains(g.entry));
                if(r!=g.returns){ g.returns=r; ch=true; }
            }
            if(!ch) break;
        }
        for(Func f : prog.funcs.values()) entryW.put(f, liveIn.get(f)[0].clone());
        for(Func f : prog.funcs.values()) materialize(f);
        for(Func f : prog.funcs.values()) simplify(f);
    }

    // ===================================================================== interprocedural
    private void computeMayDefR0(){
        boolean ch=true;
        while(ch){
            ch=false;
            for(Func f : prog.funcs.values()){
                if(f.mayDefR0) continue;
                boolean m=false;
                for(Block b : f.blocks) for(Stmt s : b.stmts){
                    if(s instanceof Assign && ((Assign)s).r==Ir.R0) m=true;
                    Call c = placeholder(s);
                    if(c!=null){ Func g=prog.funcAt(c.target); if(g!=null && g.mayDefR0) m=true; }
                }
                if(m){ f.mayDefR0=true; ch=true; }
            }
        }
    }

    private void solveParams(){
        for(Func f : prog.funcs.values()) f.params=0;
        boolean ch=true;
        while(ch){
            ch=false;
            for(Func f : prog.funcs.values()){
                liveness(f);
                long p = paramsFromLive(liveIn.get(f)[0]);
                if(p!=f.params){ f.params=p; ch=true; }
            }
            int ar=0;
            for(Func f : prog.funcs.values()) if(f.addressTaken)
                for(int k=0;k<4;k++) if((f.params & (1L<<(Ir.P0+k)))!=0) ar=Math.max(ar, k+1);
            if(ar!=indirectArity){ indirectArity=ar; ch=true; }
            for(int k=0;k<4;k++){
                int w=0;
                for(Func f : prog.funcs.values()) if(f.addressTaken) w=Math.max(w, liveIn.get(f)[0][Ir.P0+k]);
                if(w!=indirectW[k]){ indirectW[k]=w; ch=true; }
            }
        }
    }
    private static long paramsFromLive(byte[] in){
        long m=0;
        for(int r=0;r<32;r++) if(Ir.isLocal(r) && in[r]>0) m|=1L<<r;
        // keep the argument registers contiguous (p1 used => p0 is a parameter too)
        for(int k=3;k>0;k--) if((m & (1L<<(Ir.P0+k)))!=0) m |= (1L<<(Ir.P0+k))-(1L<<Ir.P0);
        return m;
    }
    /** parameter registers in call order: p0..p3, then the rest by register index */
    static int[] paramOrder(long mask){
        List<Integer> l=new ArrayList<Integer>();
        for(int r=Ir.P0;r<=Ir.P3;r++) if((mask&(1L<<r))!=0) l.add(r);
        for(int r=0;r<32;r++) if((r<Ir.P0||r>Ir.P3) && (mask&(1L<<r))!=0) l.add(r);
        int[] a=new int[l.size()]; for(int i=0;i<a.length;i++) a[i]=l.get(i); return a;
    }

    private void collectResultUses(Func f, Set<Integer> used){
        byte[][] out = liveOut.get(f);
        for(Block b : f.blocks){
            byte[] live = out[b.id].clone();
            applyTerm(f, b.term, live);
            for(int i=b.stmts.size()-1;i>=0;i--){
                Stmt s=b.stmts.get(i);
                Call c=placeholder(s);
                if(c!=null && live[Ir.R0]>0) used.add(c.target);
                applyStmt(s, live);
            }
        }
    }

    private void materialize(Func f){
        for(Block b : f.blocks){
            for(int i=0;i<b.stmts.size();i++){
                Stmt s=b.stmts.get(i);
                if(s instanceof Assign && ((Assign)s).e instanceof Call && ((Call)((Assign)s).e).kind==CallKind.INDIRECT){
                    Call ic=(Call)((Assign)s).e;
                    if(ic.args.length>indirectArity) ic.args=Arrays.copyOf(ic.args, indirectArity);
                }
                Call c=placeholder(s);
                if(c==null) continue;
                Func g=prog.funcAt(c.target);
                int[] ps=paramOrder(g.params);
                Expr[] args=new Expr[ps.length];
                for(int k=0;k<ps.length;k++) args[k]=Ir.reg(ps[k]);
                Call nc=new Call(CallKind.LOCAL, c.target, null, null, args);
                Stmt ns = g.returns? new Assign(Ir.R0, nc) : new Eval(nc);
                ns.pc=s.pc; b.stmts.set(i, ns);
                f.callees.add(g.entry); g.callers.add(f.entry);
            }
            if(b.term.kind==TermKind.RETURN && f.returns) b.term.cond = new Reg(Ir.R0);
        }
    }
    /** a local call whose arguments are not materialised yet */
    private static Call placeholder(Stmt s){
        if(s instanceof Eval && ((Eval)s).e instanceof Call){
            Call c=(Call)((Eval)s).e;
            if(c.kind==CallKind.LOCAL && c.args.length==0) return c;  // (zero-parameter calls are re-materialised identically)
        }
        return null;
    }

    /** how many low bits of argument k the callee can observe */
    private int argWidth(Call c, int k){
        switch(c.kind){
            case IMPORT: return ImportSigs.argBits(c.name, k);
            case INDIRECT: return k<4? Math.max(8, indirectW[k]) : 32;
            case LOCAL: {
                Func g=prog.funcAt(c.target);
                if(g==null) return 32;
                int[] ps=paramOrder(g.params);
                if(k>=ps.length) return 32;
                byte[] w=entryW.get(g);
                if(w==null) w=liveIn.containsKey(g)? liveIn.get(g)[0] : null;
                return w==null? 32 : Math.max(8, w[ps[k]]);
            }
            default: return 32;
        }
    }

    // ===================================================================== liveness
    private static int roundW(int w){ return w<=0?0 : w<=8?8 : w<=16?16 : 32; }
    private static int maskW(int m){ return (m & 0xffffff00)==0? 8 : (m & 0xffff0000)==0? 16 : 32; }

    private void demand(Expr e, int w, byte[] live){
        if(w==0 && !e.hasCall()) return;
        if(e instanceof Reg){ int r=((Reg)e).r; if(Ir.isLocal(r) && live[r]<w) live[r]=(byte)w; return; }
        if(e instanceof Const) return;
        if(e instanceof Bin){
            Bin b=(Bin)e;
            switch(b.op){
                case ADD: case SUB: case MUL: case OR: case XOR:
                    demand(b.a, w, live); demand(b.b, w, live); return;
                case AND: {
                    int wa=w, wb=w;
                    if(b.b instanceof Const) wa=Math.min(w, maskW(((Const)b.b).v));
                    if(b.a instanceof Const) wb=Math.min(w, maskW(((Const)b.a).v));
                    demand(b.a, wa, live); demand(b.b, wb, live); return;
                }
                case SHL:
                    if(b.b instanceof Const) demand(b.a, roundW(w-(((Const)b.b).v&31)), live);
                    else demand(b.a, w, live);
                    demand(b.b, 8, live); return;
                case SHR: case SHRU:
                    demand(b.a, 32, live); demand(b.b, 8, live); return;
                default:
                    demand(b.a, 32, live); demand(b.b, 32, live); return;
            }
        }
        if(e instanceof Un){
            Un u=(Un)e;
            switch(u.op){
                case NOT: case NEG: demand(u.a, w, live); return;
                case SEXT8: demand(u.a, 8, live); return;
                case SEXT16: demand(u.a, 16, live); return;
                default: demand(u.a, 32, live); return;
            }
        }
        if(e instanceof Load){ demand(((Load)e).addr, 32, live); return; }
        if(e instanceof SetPart){
            SetPart sp=(SetPart)e;
            if(sp.shift==0){
                demand(sp.val, Math.min(w, sp.bits), live);
                if(w>sp.bits) demand(sp.old, w, live);
            } else { demand(sp.old, 32, live); demand(sp.val, sp.bits, live); }
            return;
        }
        if(e instanceof Call){
            Call c=(Call)e;
            if(c.fptr!=null) demand(c.fptr, 32, live);
            int n = c.kind==CallKind.INDIRECT? Math.min(indirectArity, c.args.length) : c.args.length;
            for(int k=0;k<n;k++) demand(c.args[k], argWidth(c, k), live);
        }
    }

    private void applyStmt(Stmt s, byte[] live){
        if(s instanceof Assign){
            Assign a=(Assign)s;
            int w=32;
            if(Ir.isLocal(a.r)){ w=live[a.r]; live[a.r]=0; }
            demand(a.e, w, live);
        } else if(s instanceof Store){
            Store st=(Store)s;
            demand(st.addr, 32, live); demand(st.val, st.width*8, live);
        } else if(s instanceof Eval){
            Call c=placeholder(s);
            if(c!=null){
                Func g=prog.funcAt(c.target);
                if(g.returns) live[Ir.R0]=0;
                byte[] gw=liveIn.containsKey(g)? liveIn.get(g)[0] : null;
                for(int r=0;r<32;r++) if((g.params&(1L<<r))!=0){
                    int w = gw!=null? Math.max(8, gw[r]) : 32;
                    if(live[r]<w) live[r]=(byte)w;
                }
            } else demand(((Eval)s).e, 0, live);
        }
    }
    private void applyTerm(Func f, Term t, byte[] live){
        switch(t.kind){
            case RETURN:
                if(t.cond!=null) demand(t.cond, 32, live);
                else if(f.returns) live[Ir.R0]=32;
                break;
            case IF: case SWITCH: demand(t.cond, 32, live); break;
            default: break;
        }
    }

    private void liveness(Func f){
        int n=f.blocks.size();
        byte[][] in=new byte[n][33], out=new byte[n][33];
        boolean ch=true;
        while(ch){
            ch=false;
            for(int k=n-1;k>=0;k--){
                Block b=f.blocks.get(k);
                byte[] o=out[k];
                for(int s : b.succ){ byte[] si=in[s]; for(int r=0;r<33;r++) if(si[r]>o[r]) o[r]=si[r]; }
                byte[] live=o.clone();
                applyTerm(f, b.term, live);
                for(int i=b.stmts.size()-1;i>=0;i--) applyStmt(b.stmts.get(i), live);
                if(!Arrays.equals(live, in[k])){ in[k]=live; ch=true; }
            }
        }
        liveIn.put(f, in); liveOut.put(f, out);
    }

    // ===================================================================== clean-up
    private void simplify(Func f){
        for(int round=0; round<50; round++){
            boolean ch=false;
            liveness(f);
            for(Block b : f.blocks) ch |= deadCode(f, b);
            liveness(f);
            for(Block b : f.blocks) ch |= inline(f, b);
            for(Block b : f.blocks) ch |= simplifyExprs(b);
            ch |= foldBranches(f);
            if(!ch) break;
        }
        liveness(f);
    }

    private boolean deadCode(Func f, Block b){
        boolean ch=false;
        byte[] live=liveOut.get(f)[b.id].clone();
        applyTerm(f, b.term, live);
        for(int i=b.stmts.size()-1;i>=0;i--){
            Stmt s=b.stmts.get(i);
            if(s instanceof Assign && Ir.isLocal(((Assign)s).r)){
                Assign a=(Assign)s;
                int w=live[a.r];
                if(w==0){
                    if(!a.e.hasCall()){ b.stmts.remove(i); ch=true; continue; }
                    if(a.e instanceof Call){ Stmt e=new Eval(a.e); e.pc=a.pc; b.stmts.set(i, e); s=e; ch=true; }
                } else if(a.e instanceof SetPart && ((SetPart)a.e).shift==0 && w<=((SetPart)a.e).bits){
                    a.e=((SetPart)a.e).val; ch=true;
                }
            }
            applyStmt(s, live);
        }
        // merge adjacent stack-pointer adjustments
        for(int i=0;i+1<b.stmts.size();i++){
            Integer c1=spDelta(b.stmts.get(i)), c2=spDelta(b.stmts.get(i+1));
            if(c1!=null && c2!=null){
                b.stmts.remove(i+1);
                if(c1+c2==0) b.stmts.remove(i);
                else ((Assign)b.stmts.get(i)).e=new Bin(Op.ADD, new Reg(Ir.SP), new Const(c1+c2));
                ch=true; i=Math.max(-1,i-2);
            }
        }
        return ch;
    }
    private static Integer spDelta(Stmt s){
        if(!(s instanceof Assign) || ((Assign)s).r!=Ir.SP) return null;
        Expr e=((Assign)s).e;
        if(e instanceof Bin && ((Bin)e).op==Op.ADD && ((Bin)e).a instanceof Reg && ((Reg)((Bin)e).a).r==Ir.SP && ((Bin)e).b instanceof Const)
            return ((Const)((Bin)e).b).v;
        return null;
    }

    /** live-after sets for every statement of b (index stmts.size() = before the terminator) */
    private byte[][] liveAfter(Func f, Block b){
        int n=b.stmts.size();
        byte[][] la=new byte[n+1][];
        byte[] live=liveOut.get(f)[b.id].clone();
        la[n]=live.clone();
        applyTerm(f, b.term, live);
        for(int i=n-1;i>=0;i--){ la[i]=live.clone(); applyStmt(b.stmts.get(i), live); }
        return la;
    }

    private static List<Expr> termExprs(Term t){
        List<Expr> l=new ArrayList<Expr>(1);
        if(t.cond!=null) l.add(t.cond);
        return l;
    }
    private static int uses(List<Expr> es, int r){ int n=0; for(Expr e: es) n+=Ir.countReg(e, r); return n; }
    private static boolean defines(Stmt s, int r){ return s instanceof Assign && ((Assign)s).r==r; }

    private boolean inline(Func f, Block b){
        boolean any=false;
        byte[][] la=liveAfter(f, b);
        outer:
        for(int i=0;i<b.stmts.size();i++){
            Stmt s=b.stmts.get(i);
            if(!(s instanceof Assign)) continue;
            Assign a=(Assign)s;
            int r=a.r;
            if(!Ir.isLocal(r)) continue;
            Expr e=a.e;
            if(Ir.size(e)>24) continue;               // keep statements readable
            int n=b.stmts.size();
            int useAt=-1; boolean redefined=false;
            for(int j=i+1;j<=n;j++){
                List<Expr> es = j<n? Ir.exprsOf(b.stmts.get(j)) : termExprs(b.term);
                int u=uses(es, r);
                if(u>0){
                    if(useAt>=0 || u>1) continue outer;      // more than one use: keep the variable
                    useAt=j;
                }
                if(j<n && defines(b.stmts.get(j), r)){ redefined=true; break; }
            }
            if(useAt<0) continue;
            if(!redefined && la[n][r]>0) continue;           // the value flows out of the block
            // nothing in between may change what e reads
            long regs=e.regs();
            boolean ld=e.hasLoad(), cl=e.hasCall();
            if(cl && useAt!=i+1) continue;
            for(int j=i+1;j<useAt;j++){
                Stmt m=b.stmts.get(j);
                if(m instanceof Assign && (regs & (1L<<((Assign)m).r))!=0) continue outer;
                if(ld && Ir.writesMemory(m)) continue outer;
                if((regs & (1L<<Ir.SP))!=0 && Ir.hasCall(m)) continue outer;
            }
            // evaluation order inside the using statement
            if(ld || cl){
                List<Expr> es = useAt<n? Ir.exprsOf(b.stmts.get(useAt)) : termExprs(b.term);
                int[] state={0};   // 0 = slot not reached yet, 1 = reached, 2 = effect before slot
                for(Expr x : es){ effectBefore(x, r, cl, state); if(state[0]!=0) break; }
                if(state[0]==2) continue;
                if(cl){       // a call moved into an expression must not be reordered with other effects at all
                    int effects=0;
                    for(Expr x : es) effects+=countEffects(x);
                    if(effects>0) continue;
                }
            }
            // do it
            if(useAt<n){
                Stmt u=b.stmts.get(useAt);
                if(u instanceof Assign) ((Assign)u).e=Ir.replaceReg(((Assign)u).e, r, e);
                else if(u instanceof Store){ Store st=(Store)u; st.addr=Ir.replaceReg(st.addr, r, e); st.val=Ir.replaceReg(st.val, r, e); }
                else if(u instanceof Eval) ((Eval)u).e=Ir.replaceReg(((Eval)u).e, r, e);
            } else b.term.cond=Ir.replaceReg(b.term.cond, r, e);
            b.stmts.remove(i);
            any=true;
            la=liveAfter(f, b);
            i=-1;
        }
        return any;
    }
    /** walks x in Java evaluation order; state 2 if a call (or, when !onlyCalls, a load) completes before Reg(r) */
    private static void effectBefore(Expr x, int r, boolean calledIsCall, int[] st){
        if(st[0]!=0) return;
        if(x instanceof Reg){ if(((Reg)x).r==r) st[0]=1; return; }
        for(Expr c : Ir.children(x)){ effectBefore(c, r, calledIsCall, st); if(st[0]!=0) return; }
        if(x instanceof Call) st[0]=2;
        else if(calledIsCall && x instanceof Load) st[0]=2;
    }
    private static int countEffects(Expr x){
        int n = (x instanceof Call || x instanceof Load)? 1 : 0;
        for(Expr c : Ir.children(x)) n+=countEffects(c);
        return n;
    }

    // ------------------------------------------------------------------ expression rewriting
    private boolean changed;
    private boolean simplifyExprs(Block b){
        changed=false;
        for(Stmt s : b.stmts){
            if(s instanceof Assign) ((Assign)s).e=simp(((Assign)s).e);
            else if(s instanceof Store){
                Store st=(Store)s; st.addr=simp(st.addr);
                st.val = st.width<4? simp(narrow(st.val, st.width*8)) : simp(st.val);
            } else if(s instanceof Eval) ((Eval)s).e=simp(((Eval)s).e);
        }
        Term t=b.term;
        if(t.cond!=null){
            t.cond=simp(t.cond);
            if(t.kind==TermKind.SWITCH && t.cond instanceof Bin){
                Bin sb=(Bin)t.cond;   // (x << 2) >>> 2  ==  x  for every index the bound check lets through
                if(sb.op==Op.SHRU && isC(sb.b,2) && sb.a instanceof Bin && ((Bin)sb.a).op==Op.SHL && isC(((Bin)sb.a).b,2)){
                    t.cond=((Bin)sb.a).a; changed=true;
                }
            }
        }
        return changed;
    }
    private static boolean isC(Expr e, int v){ return e instanceof Const && ((Const)e).v==v && ((Const)e).kind==Ir.NUM; }
    private static boolean isNum(Expr e){ return e instanceof Const && ((Const)e).kind==Ir.NUM; }

    /** low `w` bits of the result are all that matter */
    private Expr narrow(Expr e, int w){
        if(e instanceof SetPart){
            SetPart sp=(SetPart)e;
            if(sp.shift==0 && w<=sp.bits){ changed=true; return narrow(sp.val, w); }
            return e;
        }
        if(e instanceof Bin){
            Bin b=(Bin)e;
            switch(b.op){
                case AND:
                    if(isNum(b.b)){
                        int m=((Const)b.b).v, need=(int)((1L<<w)-1);
                        if((m & need)==need){ changed=true; return narrow(b.a, w); }
                        b.a=narrow(b.a, w); return b;
                    }
                    b.a=narrow(b.a,w); b.b=narrow(b.b,w); return b;
                case ADD: case SUB: case MUL: case OR: case XOR:
                    b.a=narrow(b.a,w); b.b=narrow(b.b,w); return b;
                case SHL:
                    if(isNum(b.b)){ int c=((Const)b.b).v&31; if(w-c>0) b.a=narrow(b.a, roundW(w-c)); }
                    return b;
                default: return b;
            }
        }
        if(e instanceof Un){
            Un u=(Un)e;
            if((u.op==UOp.SEXT8 && w<=8) || (u.op==UOp.SEXT16 && w<=16)){ changed=true; return narrow(u.a, w); }
            if(u.op==UOp.NOT || u.op==UOp.NEG) u.a=narrow(u.a, w);
            return u;
        }
        return e;
    }

    private static boolean nonNeg(Expr e){
        if(e instanceof Const) return ((Const)e).v>=0;
        if(e instanceof Load) return !((Load)e).signed && ((Load)e).width<4;
        if(e instanceof Bin){
            Bin b=(Bin)e;
            if(b.op==Op.AND) return (isNum(b.b) && ((Const)b.b).v>=0) || (isNum(b.a) && ((Const)b.a).v>=0);
            if(b.op==Op.SHRU) return isNum(b.b) && (((Const)b.b).v&31)!=0;
        }
        return false;
    }

    private Expr simp(Expr e){
        // children first
        if(e instanceof Bin){ Bin b=(Bin)e; b.a=simp(b.a); b.b=simp(b.b); }
        else if(e instanceof Un){ Un u=(Un)e; u.a=simp(u.a); }
        else if(e instanceof Load){ Load l=(Load)e; l.addr=simp(l.addr); }
        else if(e instanceof SetPart){ SetPart sp=(SetPart)e; sp.old=simp(sp.old); sp.val=simp(narrow(sp.val, sp.bits)); }
        else if(e instanceof Call){
            Call c=(Call)e; if(c.fptr!=null) c.fptr=simp(c.fptr);
            for(int i=0;i<c.args.length;i++){ int w=argWidth(c, i); c.args[i]=simp(w<32? narrow(c.args[i], w) : c.args[i]); }
        }

        if(e instanceof Un){
            Un u=(Un)e;
            if(u.op==UOp.SEXT8 || u.op==UOp.SEXT16){
                int w = u.op==UOp.SEXT8? 8 : 16;
                u.a = narrow(u.a, w);
                if(u.a instanceof Load && ((Load)u.a).width*8==w){ changed=true; Load l=(Load)u.a; return new Load(l.width, true, l.addr); }
                if(u.a instanceof Un && ((Un)u.a).op==u.op){ changed=true; return u.a; }
            }
            if(u.op==UOp.NEG && isNum(u.a)){ changed=true; return new Const(-((Const)u.a).v); }
            if(u.op==UOp.NOT && isNum(u.a)){ changed=true; return new Const(~((Const)u.a).v); }
            if(u.op==UOp.LNOT && u.a.isBool()){ changed=true; return Ir.not(u.a); }
            return u;
        }
        if(!(e instanceof Bin)) return e;
        Bin b=(Bin)e;
        // constant folding (plain numbers only; addresses keep their symbolic form)
        if(isNum(b.a) && isNum(b.b) && !b.op.isBool()){
            int x=((Const)b.a).v, y=((Const)b.b).v; Integer v=null;
            switch(b.op){
                case ADD: v=x+y; break; case SUB: v=x-y; break; case MUL: v=x*y; break;
                case AND: v=x&y; break; case OR: v=x|y; break; case XOR: v=x^y; break;
                case SHL: v=x<<y; break; case SHR: v=x>>y; break; case SHRU: v=x>>>y; break;
                case DIV: if(y!=0) v=x/y; break;
                default: break;
            }
            if(v!=null){ changed=true; return new Const(v); }
        }
        // canonical order: constant on the right of commutative ops
        if((b.op==Op.ADD||b.op==Op.MUL||b.op==Op.AND||b.op==Op.OR||b.op==Op.XOR) && b.a instanceof Const && !(b.b instanceof Const)){
            Expr t=b.a; b.a=b.b; b.b=t; changed=true;
        }
        if(b.op.isCompare() && b.a instanceof Const && !(b.b instanceof Const)){
            changed=true; return simp(new Bin(b.op.swap(), b.b, b.a));
        }
        switch(b.op){
            case ADD: case SUB: case OR: case XOR: case SHL: case SHR: case SHRU:
                if(isC(b.b,0)){ changed=true; return b.a; }
                if(b.op==Op.ADD && isNum(b.b) && b.a instanceof Bin && ((Bin)b.a).op==Op.ADD && isNum(((Bin)b.a).b)){
                    changed=true; return simp(new Bin(Op.ADD, ((Bin)b.a).a, new Const(((Const)((Bin)b.a).b).v+((Const)b.b).v)));
                }
                if(b.op==Op.SUB && isNum(b.b)){ changed=true; return simp(new Bin(Op.ADD, b.a, new Const(-((Const)b.b).v))); }
                break;
            case MUL:
                if(isC(b.b,1)){ changed=true; return b.a; }
                break;
            case AND: {
                if(isC(b.b,-1)){ changed=true; return b.a; }
                if(isNum(b.b)){
                    int m=((Const)b.b).v;
                    b.a=narrow(b.a, maskW(m)==32? 32 : maskW(m));
                    if(b.a instanceof Bin && ((Bin)b.a).op==Op.AND && isNum(((Bin)b.a).b)){
                        changed=true; return simp(new Bin(Op.AND, ((Bin)b.a).a, new Const(m & ((Const)((Bin)b.a).b).v)));
                    }
                    if(b.a instanceof Load && !((Load)b.a).signed){
                        int lw=((Load)b.a).width; int full = lw==1? 0xff : lw==2? 0xffff : -1;
                        if((m & full)==full){ changed=true; return b.a; }
                    }
                }
                break;
            }
            case GTU: if(isC(b.b,0)){ changed=true; return new Bin(Op.NE, b.a, b.b); } // fallthrough
            case LTU: case GEU: case LEU:
                if(b.op==Op.LEU && isC(b.b,0)){ changed=true; return new Bin(Op.EQ, b.a, b.b); }
                if(nonNeg(b.a) && nonNeg(b.b)){
                    changed=true;
                    Op so = b.op==Op.LTU? Op.LT : b.op==Op.LEU? Op.LE : b.op==Op.GTU? Op.GT : Op.GE;
                    return new Bin(so, b.a, b.b);
                }
                break;
            default: break;
        }
        return b;
    }

    /** IF with a constant condition becomes a GOTO */
    private boolean foldBranches(Func f){
        boolean ch=false;
        for(Block b : f.blocks){
            Term t=b.term;
            if(t.kind!=TermKind.IF) continue;
            Boolean v=constCond(t.cond);
            if(t.succ[0]==t.succ[1]) v=Boolean.TRUE;
            if(v==null) continue;
            int to = v? t.succ[0] : t.succ[1];
            t.kind=TermKind.GOTO; t.cond=null; t.succ=new int[]{to};
            ch=true;
        }
        if(ch) prune(f);
        return ch;
    }
    private static Boolean constCond(Expr c){
        if(!(c instanceof Bin)) return null;
        Bin b=(Bin)c;
        if(!b.op.isCompare() || !(b.a instanceof Const) || !(b.b instanceof Const)) return null;
        int x=((Const)b.a).v, y=((Const)b.b).v;
        long ux=x&0xffffffffL, uy=y&0xffffffffL;
        switch(b.op){
            case EQ: return x==y; case NE: return x!=y;
            case LT: return x<y; case LE: return x<=y; case GT: return x>y; case GE: return x>=y;
            case LTU: return ux<uy; case LEU: return ux<=uy; case GTU: return ux>uy; case GEU: return ux>=uy;
            default: return null;
        }
    }
    /** drop blocks that became unreachable and renumber */
    static void prune(Func f){
        boolean[] reach=new boolean[f.blocks.size()];
        ArrayDeque<Integer> w=new ArrayDeque<Integer>(); w.add(0); reach[0]=true;
        while(!w.isEmpty()){ int k=w.poll(); for(int s : f.blocks.get(k).term.succ==null? new int[0] : f.blocks.get(k).term.succ) if(!reach[s]){ reach[s]=true; w.add(s); } }
        int[] map=new int[reach.length]; List<Block> nb=new ArrayList<Block>();
        for(int k=0;k<reach.length;k++){ if(reach[k]){ map[k]=nb.size(); nb.add(f.blocks.get(k)); } else map[k]=-1; }
        for(Block b : nb){
            b.id=map[b.id];
            if(b.term.succ!=null) for(int i=0;i<b.term.succ.length;i++) b.term.succ[i]=map[b.term.succ[i]];
            b.succ = b.term.succ==null? new int[0] : b.term.succ;
        }
        f.blocks.clear(); f.blocks.addAll(nb);
        for(Block b : f.blocks) b.preds.clear();
        for(Block b : f.blocks) for(int s : Lifter.distinct(b.succ)) f.blocks.get(s).preds.add(b.id);
    }
}
