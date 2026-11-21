package wolphun.decomp;

import java.util.*;

import wolphun.decomp.Ir.*;
import wolphun.decomp.Program.*;

/**
 * Turns the CFG of a function into structured Java statements -- no goto.
 *
 * <p>Algorithm: N. Ramsey, "Beyond Relooper: recursive translation of unstructured control flow
 * to structured control flow" (ICFP 2022). On the dominator tree of a reducible CFG every node
 * x becomes its own code followed by the code of the children it dominates that are merge
 * points (two or more forward predecessors); each such child y is introduced by a labeled
 * block that ends just before y, so a jump to y is {@code break B_y}. Loop headers wrap their
 * code in {@code L_x: while (true)}, and back edges are {@code continue L_x}. Java has labeled
 * blocks and labeled break/continue, so the result maps one-to-one onto Java.
 *
 * <p>The raw output is correct but full of labels; a set of semantics-preserving rewrites then
 * produces ordinary code: tail jumps are dropped, {@code if (c) { A; break B; } rest} becomes
 * {@code if (c) { A } else { rest }}, loop exits are hoisted after the loop, loops get their
 * while/do-while conditions, and unused labels disappear.
 *
 * <p>Irreducible functions (not produced by the mophun compiler in the games tested) fall back
 * to a {@code while (true) switch (state)} dispatcher, which is always correct.
 */
final class Structurer {

    // =====================================================================================
    // AST
    // =====================================================================================
    static abstract class Node {}
    static final class Seq extends Node { final List<Node> items=new ArrayList<Node>(); Seq(){} Seq(Node... ns){ for(Node n:ns) add(n); }
        void add(Node n){ if(n==null) return; if(n instanceof Seq) items.addAll(((Seq)n).items); else items.add(n); } }
    static final class SNode extends Node { final Stmt s; SNode(Stmt s){ this.s=s; } }
    static final class If extends Node { Expr cond; Node then, els; If(Expr c, Node t, Node e){ cond=c; then=t; els=e; } }
    static final class Blk extends Node { final String label; Node body; Blk(String l, Node b){ label=l; body=b; } }
    static final class Loop extends Node { final String label; Node body; Expr cond; boolean doWhile; Loop(String l, Node b){ label=l; body=b; } }
    static final class Jump extends Node { final boolean brk; String label; boolean bare;
        Jump(boolean brk, String label){ this.brk=brk; this.label=label; } }
    static final class Ret extends Node { final Expr value; Ret(Expr v){ value=v; } }
    static final class Trap extends Node { final String call; Trap(String c){ call=c; } }
    static final class Switch extends Node { Expr sel; final List<int[]> vals=new ArrayList<int[]>(); final List<Node> bodies=new ArrayList<Node>(); Node deflt; }
    static final class Cmt extends Node { final String text; Cmt(String t){ text=t; } }

    static final int STATE = 40;   // pseudo register for the dispatcher fallback

    private final Program prog;
    private Func f;
    private int[] rpo;            // block id -> reverse postorder number
    private int[] order;          // rpo number -> block id
    private int[] idom;
    private boolean[] loopHeader, merge;
    private List<List<Integer>> domKids;
    private final boolean addrComments;

    Structurer(Program prog, boolean addrComments){ this.prog=prog; this.addrComments=addrComments; }

    Node structure(Func fn){
        f=fn;
        computeOrder();
        computeDominators();
        classify();
        Node body;
        if(f.irreducible) body = dispatcher();
        else {
            body = doTree(0);
            body = prettify(body);
        }
        return body;
    }

    // ------------------------------------------------------------------ graph analyses
    private int[] succ(int b){ return Lifter.distinct(f.blocks.get(b).succ); }

    private void computeOrder(){
        int n=f.blocks.size();
        rpo=new int[n]; Arrays.fill(rpo,-1);
        List<Integer> post=new ArrayList<Integer>();
        boolean[] vis=new boolean[n];
        // iterative DFS
        Deque<int[]> st=new ArrayDeque<int[]>();
        st.push(new int[]{0,0}); vis[0]=true;
        while(!st.isEmpty()){
            int[] top=st.peek();
            int[] ss=succ(top[0]);
            if(top[1]<ss.length){
                int s=ss[top[1]++];
                if(!vis[s]){ vis[s]=true; st.push(new int[]{s,0}); }
            } else { post.add(top[0]); st.pop(); }
        }
        order=new int[post.size()];
        for(int i=0;i<post.size();i++){ int b=post.get(post.size()-1-i); order[i]=b; rpo[b]=i; }
    }

    private void computeDominators(){
        int n=f.blocks.size();
        idom=new int[n]; Arrays.fill(idom,-1); idom[0]=0;
        boolean ch=true;
        while(ch){
            ch=false;
            for(int i=1;i<order.length;i++){
                int b=order[i];
                int nd=-1;
                for(int p : f.blocks.get(b).preds){
                    if(rpo[p]<0 || idom[p]<0) continue;
                    nd = nd<0? p : intersect(p, nd);
                }
                if(nd>=0 && idom[b]!=nd){ idom[b]=nd; ch=true; }
            }
        }
        domKids=new ArrayList<List<Integer>>();
        for(int i=0;i<n;i++) domKids.add(new ArrayList<Integer>());
        for(int i=1;i<order.length;i++){ int b=order[i]; domKids.get(idom[b]).add(b); }
    }
    private int intersect(int a, int b){
        while(a!=b){
            while(rpo[a]>rpo[b]) a=idom[a];
            while(rpo[b]>rpo[a]) b=idom[b];
        }
        return a;
    }
    private boolean dominates(int a, int b){
        while(true){ if(a==b) return true; if(b==0) return false; b=idom[b]; }
    }

    private void classify(){
        int n=f.blocks.size();
        loopHeader=new boolean[n]; merge=new boolean[n];
        f.irreducible=false;
        for(int b=0;b<n;b++){
            if(rpo[b]<0) continue;
            int fwd=0;
            for(int p : f.blocks.get(b).preds){
                if(rpo[p]<0) continue;
                if(rpo[p]>=rpo[b]){
                    loopHeader[b]=true;
                    if(!dominates(b, p)) f.irreducible=true;
                } else fwd++;
            }
            merge[b] = fwd>=2;
        }
    }

    // ------------------------------------------------------------------ Ramsey translation
    private String blockLabel(int b){ return "B_"+Integer.toHexString(f.blocks.get(b).start); }
    private String loopLabel(int b){ return "L_"+Integer.toHexString(f.blocks.get(b).start); }

    private Node doTree(int x){
        List<Integer> ys=new ArrayList<Integer>();
        for(int k : domKids.get(x)) if(merge[k]) ys.add(k);
        Collections.sort(ys, new Comparator<Integer>(){ public int compare(Integer a, Integer b){ return rpo[b]-rpo[a]; } });
        Node code = nodeWithin(x, ys, 0);
        if(loopHeader[x]) return new Loop(loopLabel(x), code);
        return code;
    }
    private Node nodeWithin(int x, List<Integer> ys, int i){
        if(i==ys.size()){
            Seq s=new Seq();
            Block b=f.blocks.get(x);
            if(addrComments && !b.insns.isEmpty()) s.add(new Cmt(String.format("@%x", b.start)));
            for(Stmt st : b.stmts) s.add(st instanceof Comment? new Cmt(((Comment)st).text) : new SNode(st));
            s.add(term(x));
            return s;
        }
        int y=ys.get(i);
        return new Seq(new Blk(blockLabel(y), nodeWithin(x, ys, i+1)), doTree(y));
    }
    private Node term(int x){
        Term t=f.blocks.get(x).term;
        switch(t.kind){
            case GOTO: return branch(x, t.succ[0]);
            case IF:   return new If(t.cond, branch(x, t.succ[0]), branch(x, t.succ[1]));
            case SWITCH: {
                Switch sw=new Switch(); sw.sel=t.cond;
                LinkedHashMap<Integer,List<Integer>> groups=new LinkedHashMap<Integer,List<Integer>>();
                for(int k=0;k<t.succ.length;k++){
                    List<Integer> l=groups.get(t.succ[k]);
                    if(l==null){ l=new ArrayList<Integer>(); groups.put(t.succ[k], l); }
                    l.add(t.caseVals[k]);
                }
                for(Map.Entry<Integer,List<Integer>> e : groups.entrySet()){
                    int[] v=new int[e.getValue().size()]; for(int k=0;k<v.length;k++) v[k]=e.getValue().get(k);
                    sw.vals.add(v); sw.bodies.add(branch(x, e.getKey()));
                }
                sw.deflt=new Trap(String.format("unreachable(0x%x)", t.pc));
                return sw;
            }
            case RETURN: return new Ret(t.cond);
            case HALT:   return new Trap(t.note!=null? "terminated()" : "killTask()");
            default:     return new Trap(String.format("unreachable(0x%x)", t.pc));
        }
    }
    private Node branch(int src, int tgt){
        if(rpo[tgt]<=rpo[src]) return new Jump(false, loopLabel(tgt));
        if(merge[tgt]) return new Jump(true, blockLabel(tgt));
        return doTree(tgt);
    }

    // ------------------------------------------------------------------ dispatcher fallback
    private Node dispatcher(){
        Switch sw=new Switch(); sw.sel=new Reg(STATE);
        for(int i=0;i<order.length;i++){
            int b=order[i];
            Seq s=new Seq();
            for(Stmt st : f.blocks.get(b).stmts) s.add(st instanceof Comment? new Cmt(((Comment)st).text) : new SNode(st));
            Term t=f.blocks.get(b).term;
            switch(t.kind){
                case GOTO: s.add(goState(t.succ[0])); break;
                case IF: s.add(new If(t.cond, goState(t.succ[0]), goState(t.succ[1]))); break;
                case SWITCH: {
                    Switch in=new Switch(); in.sel=t.cond;
                    for(int k=0;k<t.succ.length;k++){ in.vals.add(new int[]{t.caseVals[k]}); in.bodies.add(goState(t.succ[k])); }
                    in.deflt=new Trap(String.format("unreachable(0x%x)", t.pc));
                    s.add(in); break;
                }
                case RETURN: s.add(new Ret(t.cond)); break;
                case HALT: s.add(new Trap(t.note!=null? "terminated()" : "killTask()")); break;
                default: s.add(new Trap(String.format("unreachable(0x%x)", t.pc)));
            }
            sw.vals.add(new int[]{f.blocks.get(b).start}); sw.bodies.add(s);
        }
        sw.deflt=new Trap("unreachable(state)");
        Seq body=new Seq();
        Assign init=new Assign(STATE, new Const(f.blocks.get(0).start, Ir.NUM));
        body.add(new SNode(init));
        body.add(new Loop("dispatch", sw));
        return body;
    }
    private Node goState(int b){
        return new Seq(new SNode(new Assign(STATE, new Const(f.blocks.get(b).start))), new Jump(false, "dispatch"));
    }

    // =====================================================================================
    // prettifying rewrites
    // =====================================================================================
    private Node prettify(Node n){
        for(int round=0; round<8; round++){
            String before = Fingerprint.of(n);
            n = hoistLoopExits(n);
            n = retargetBreaks(n);
            Set<String> ft=new HashSet<String>();
            if(!f.returns) ft.add("R");
            n = simp(n, ft);
            n = loops(n);
            if(Fingerprint.of(n).equals(before)) break;
        }
        bareJumps(n, new ArrayDeque<Node>());
        return n;
    }

    // ---- helpers
    static String key(Jump j){ return (j.brk?"b:":"c:")+j.label; }
    private static List<Node> items(Node n){
        if(n==null) return new ArrayList<Node>();
        if(n instanceof Seq) return ((Seq)n).items;
        List<Node> l=new ArrayList<Node>(); l.add(n); return l;
    }
    private static Node seqOf(List<Node> l){
        if(l.size()==1) return l.get(0);
        Seq s=new Seq(); for(Node x : l) s.add(x); return s;
    }
    private static boolean empty(Node n){ return n==null || (n instanceof Seq && ((Seq)n).items.isEmpty()); }
    private static Node last(Node n){ List<Node> l=items(n); return l.isEmpty()? null : l.get(l.size()-1); }

    /** references to a label (break or continue) */
    static int refs(Node n, String label){
        if(n==null) return 0;
        if(n instanceof Jump) return label.equals(((Jump)n).label)? 1:0;
        int c=0;
        for(Node k : kids(n)) c+=refs(k, label);
        return c;
    }
    static boolean hasJump(Node n, boolean brk, String label){
        if(n==null) return false;
        if(n instanceof Jump){ Jump j=(Jump)n; return j.brk==brk && label.equals(j.label); }
        for(Node k : kids(n)) if(hasJump(k, brk, label)) return true;
        return false;
    }
    static List<Node> kids(Node n){
        List<Node> l=new ArrayList<Node>();
        if(n instanceof Seq) l.addAll(((Seq)n).items);
        else if(n instanceof If){ l.add(((If)n).then); if(((If)n).els!=null) l.add(((If)n).els); }
        else if(n instanceof Blk) l.add(((Blk)n).body);
        else if(n instanceof Loop) l.add(((Loop)n).body);
        else if(n instanceof Switch){ l.addAll(((Switch)n).bodies); if(((Switch)n).deflt!=null) l.add(((Switch)n).deflt); }
        return l;
    }

    /** JLS 14.22, restricted to the constructs produced here */
    static boolean canComplete(Node n){
        if(n==null) return true;
        if(n instanceof Jump || n instanceof Ret || n instanceof Trap) return false;
        if(n instanceof Seq){ for(Node k : ((Seq)n).items) if(!canComplete(k)) return false; return true; }
        if(n instanceof If){ If i=(If)n; return i.els==null || canComplete(i.then) || canComplete(i.els); }
        if(n instanceof Blk){ Blk b=(Blk)n; return canComplete(b.body) || hasJump(b.body, true, b.label); }
        if(n instanceof Loop){
            Loop l=(Loop)n;
            if(hasJump(l.body, true, l.label) || hasBareBreak(l.body)) return true;
            if(l.cond==null) return false;
            return true;
        }
        if(n instanceof Switch){
            Switch s=(Switch)n;
            if(s.deflt==null) return true;
            for(Node b : s.bodies) if(canComplete(b)) return true;
            return canComplete(s.deflt);
        }
        return true;
    }
    private static boolean hasBareBreak(Node n){
        if(n instanceof Jump) return ((Jump)n).brk && ((Jump)n).bare;
        if(n instanceof Loop || n instanceof Switch) return false;
        for(Node k : kids(n)) if(hasBareBreak(k)) return true;
        return false;
    }

    // ---- 1. hoist loop exits:   L: while(true){ B: { inner } rest }  ->  B: { L: while(true){ inner } } rest
    private Node hoistLoopExits(Node n){
        for(Node k : kids(n)) replaceKid(n, k, hoistLoopExits(k));
        if(!(n instanceof Loop)) return n;
        Loop lp=(Loop)n;
        if(lp.cond!=null || lp.doWhile) return n;
        List<Node> it=items(lp.body);
        if(it.size()<2 || !(it.get(0) instanceof Blk)) return n;
        Blk b=(Blk)it.get(0);
        List<Node> rest=new ArrayList<Node>(it.subList(1, it.size()));
        Node restN=seqOf(rest);
        if(refs(restN, lp.label)>0 || canComplete(b.body)) return n;
        Loop inner=new Loop(lp.label, b.body);
        Node hoisted=hoistLoopExits(inner);
        Seq out=new Seq(new Blk(b.label, hoisted));
        for(Node r : rest) out.add(r);
        return out;
    }
    private static void replaceKid(Node parent, Node old, Node nw){
        if(old==nw) return;
        if(parent instanceof Seq){
            List<Node> l=((Seq)parent).items; int i=l.indexOf(old);
            if(i>=0){ l.remove(i); if(nw instanceof Seq) l.addAll(i, ((Seq)nw).items); else l.add(i, nw); }
        } else if(parent instanceof If){ If p=(If)parent; if(p.then==old) p.then=nw; else if(p.els==old) p.els=nw; }
        else if(parent instanceof Blk) ((Blk)parent).body=nw;
        else if(parent instanceof Loop) ((Loop)parent).body=nw;
        else if(parent instanceof Switch){
            Switch s=(Switch)parent; int i=s.bodies.indexOf(old);
            if(i>=0) s.bodies.set(i, nw); else if(s.deflt==old) s.deflt=nw;
        }
    }

    // ---- 2. a block that ends with a loop: `break B` inside that loop == `break L`
    private Node retargetBreaks(Node n){
        for(Node k : kids(n)) replaceKid(n, k, retargetBreaks(k));
        if(n instanceof Blk){
            Blk b=(Blk)n;
            Loop tl=tailLoop(b.body);
            if(tl!=null) rename(tl.body, b.label, tl.label);
        }
        return n;
    }
    private static Loop tailLoop(Node n){
        if(n instanceof Loop) return (Loop)n;
        if(n instanceof Seq){ List<Node> l=((Seq)n).items; return l.isEmpty()? null : tailLoop(l.get(l.size()-1)); }
        if(n instanceof Blk) return tailLoop(((Blk)n).body);
        return null;
    }
    private static void rename(Node n, String from, String to){
        if(n instanceof Jump){ Jump j=(Jump)n; if(j.brk && from.equals(j.label)) j.label=to; return; }
        for(Node k : kids(n)) rename(k, from, to);
    }

    // ---- 3. tail jumps, if/else shaping, dead code, unused labels.
    //      ft = the jumps that are equivalent to falling off the end of n
    private Node simp(Node n, Set<String> ft){
        if(n instanceof Seq) return simpSeq(new ArrayList<Node>(((Seq)n).items), ft);
        if(n instanceof If){
            If i=(If)n;
            i.then=simp(i.then, ft);
            if(i.els!=null){ i.els=simp(i.els, ft); if(empty(i.els)) i.els=null; }
            if(empty(i.then) && i.els!=null){ i.cond=Ir.not(i.cond); i.then=i.els; i.els=null; }
            else if(i.els!=null && !canComplete(i.els) && canComplete(i.then)){
                i.cond=Ir.not(i.cond); Node t=i.then; i.then=i.els; i.els=t;
            }
            return i;
        }
        if(n instanceof Blk){
            Blk b=(Blk)n;
            Set<String> inner=new HashSet<String>(ft); inner.add("b:"+b.label);
            b.body=simp(b.body, inner);
            if(refs(b.body, b.label)==0) return b.body;
            return b;
        }
        if(n instanceof Loop){
            Loop l=(Loop)n;
            Set<String> inner=new HashSet<String>(); inner.add("c:"+l.label);
            l.body=simp(l.body, inner);
            return l;
        }
        if(n instanceof Switch){
            Switch s=(Switch)n;
            for(int k=0;k<s.bodies.size();k++) s.bodies.set(k, simp(s.bodies.get(k), new HashSet<String>()));
            if(s.deflt!=null) s.deflt=simp(s.deflt, new HashSet<String>());
            return s;
        }
        if(n instanceof Jump && ft.contains(key((Jump)n))) return new Seq();
        if(n instanceof Ret && ((Ret)n).value==null && ft.contains("R")) return new Seq();
        return n;
    }
    private Node simpSeq(List<Node> l, Set<String> ft){
        Seq out=new Seq();
        for(int i=0;i<l.size();i++){
            boolean isLast = i==l.size()-1;
            Node it = simp(l.get(i), isLast? ft : new HashSet<String>());
            if(it instanceof If){
                If c=(If)it;
                Node tj=last(c.then);
                boolean toEnd = (tj instanceof Jump && ft.contains(key((Jump)tj)))
                             || (tj instanceof Ret && ((Ret)tj).value==null && ft.contains("R"));
                if(!isLast && toEnd && (c.els==null || canComplete(c.els))){
                    // if (c) { A; <jump to end> } rest   ==>   if (c) { A } else { [E] rest }
                    List<Node> el=new ArrayList<Node>(items(c.els));
                    el.addAll(l.subList(i+1, l.size()));
                    c.els=seqOf(el);
                    out.add(simp(c, ft));
                    return finish(out);
                }
                if(c.els!=null && !canComplete(c.then) && !toEnd){
                    // if (c) { ...jump } else { E } rest   ==>   if (c) { ...jump } E rest
                    List<Node> nl=new ArrayList<Node>(l.subList(0, i));
                    nl.add(new If(c.cond, c.then, null));
                    nl.addAll(items(c.els));
                    nl.addAll(l.subList(i+1, l.size()));
                    l=nl;
                    out.add(l.get(i));
                    continue;
                }
            }
            out.add(it);
            if(!canComplete(it)) break;     // the rest is unreachable
        }
        return finish(out);
    }
    private static Node finish(Seq s){ return s.items.size()==1? s.items.get(0) : s; }

    // ---- 4. loop conditions
    private Node loops(Node n){
        for(Node k : kids(n)) replaceKid(n, k, loops(k));
        if(!(n instanceof Loop)) return n;
        Loop lp=(Loop)n;
        if(lp.cond!=null || lp.doWhile) return n;
        List<Node> it=new ArrayList<Node>(items(lp.body));
        if(it.isEmpty()) return n;
        // while (cond):  first statement is  if (c) break L;
        Node first=it.get(0);
        if(first instanceof If && ((If)first).els==null && isBreakTo(((If)first).then, lp.label)){
            lp.cond=Ir.not(((If)first).cond);
            it.remove(0);
            lp.body=seqOf(it);
            return lp;
        }
        // do { ... } while (cond):  last statement is  if (c) break L;  and no other continue
        Node lst=it.get(it.size()-1);
        if(it.size()>1 && lst instanceof If && ((If)lst).els==null && isBreakTo(((If)lst).then, lp.label)
           && !hasJump(lp.body, false, lp.label)){
            it.remove(it.size()-1);
            Node body=seqOf(it);
            if(canComplete(body)){
                lp.cond=Ir.not(((If)lst).cond); lp.doWhile=true; lp.body=body;
            }
            return lp;
        }
        return n;
    }
    private static boolean isBreakTo(Node n, String label){
        List<Node> l=items(n);
        return l.size()==1 && l.get(0) instanceof Jump && ((Jump)l.get(0)).brk && label.equals(((Jump)l.get(0)).label);
    }

    // ---- 5. unlabeled break/continue when the target is the innermost loop
    private void bareJumps(Node n, Deque<Node> encl){
        if(n instanceof Jump){
            Jump j=(Jump)n;
            for(Node e : encl){
                if(e instanceof Loop){
                    if(((Loop)e).label.equals(j.label)) j.bare=true;
                    break;
                }
                if(e instanceof Switch && j.brk) break;      // a bare break would leave the switch
            }
            return;
        }
        boolean push = n instanceof Loop || n instanceof Switch;
        if(push) encl.push(n);
        for(Node k : kids(n)) bareJumps(k, encl);
        if(push) encl.pop();
    }

    /** structural fingerprint used to detect the fixpoint of the rewrites */
    static final class Fingerprint {
        static String of(Node n){ StringBuilder sb=new StringBuilder(); fp(n, sb); return sb.toString(); }
        private static void fp(Node n, StringBuilder sb){
            if(n==null){ sb.append('_'); return; }
            sb.append(n.getClass().getSimpleName().charAt(0));
            if(n instanceof Jump) sb.append(((Jump)n).brk?'b':'c').append(((Jump)n).label);
            if(n instanceof Blk) sb.append(((Blk)n).label);
            if(n instanceof Loop) sb.append(((Loop)n).label).append(((Loop)n).cond!=null?'w':'t').append(((Loop)n).doWhile?'d':'-');
            if(n instanceof If) sb.append(((If)n).els==null?'1':'2');
            sb.append('(');
            for(Node k : kids(n)) fp(k, sb);
            sb.append(')');
        }
    }
}
