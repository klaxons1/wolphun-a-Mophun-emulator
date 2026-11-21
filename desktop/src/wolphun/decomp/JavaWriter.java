package wolphun.decomp;

import java.util.*;

import wolphun.decomp.Ir.*;
import wolphun.decomp.Program.*;
import wolphun.decomp.Structurer.*;

/**
 * Prints the structured functions as one Java class extending
 * {@link wolphun.decomp.rt.Pip2Runtime}. The output is meant to be read, but it also compiles and
 * runs: memory accesses, the SDK and indirect calls go through the runtime, which is wired to
 * the emulator's own SDK implementation.
 */
final class JavaWriter {
    private final Program prog;
    private final Image img;
    private final String className, pkg;
    private final TreeSet<Integer> dataSyms = new TreeSet<Integer>();
    private final TreeSet<String> importsUsed = new TreeSet<String>();
    private StringBuilder sb;
    private int ind;
    private Func cur;

    JavaWriter(Program prog, String className, String pkg){
        this.prog=prog; this.img=prog.img; this.className=className; this.pkg=pkg;
    }

    String write(LinkedHashMap<Func,Node> bodies, List<String> notes){
        // methods first (collects the symbols they use)
        StringBuilder methods=new StringBuilder();
        for(Map.Entry<Func,Node> e : bodies.entrySet()) methods.append(method(e.getKey(), e.getValue()));

        sb=new StringBuilder(); ind=0;
        line("// Decompiled from "+img.name+" by the wolphun PIP2 -> Java decompiler (wolphun.decomp).");
        line("// Source: the PIP2 bytecode of a Mophun VMGP executable"+(img.wasEncrypted? " (decrypted first: the code section was encrypted)." : "."));
        line("//");
        line(String.format("// code  0x%05x..0x%05x  (%d bytes, %d functions)", img.codeBase, img.codeEnd, img.codeEnd-img.codeBase, prog.funcs.size()));
        line(String.format("// data  0x%05x..0x%05x   bss 0x%05x..0x%05x", img.dataBase, img.dataEnd, img.bssBase, img.bssEnd));
        line("// SDK imports: "+prog.importNames.size()+"   resources: "+img.exe.resourceCount());
        line("//");
        line("// How to read this file");
        line("//  * Registers are Java locals named after the PIP2 ABI: p0..p3 = arguments, r0 = result,");
        line("//    s0..s7/fp = callee-saved, g0..g13 = temporaries. sp is the emulated stack pointer.");
        line("//  * Memory is the game's own 32-bit address space: s8/u8/s16/u16/i32(addr) read,");
        line("//    put8/put16/put32(addr, value) write. g_XXXXX are data/bss addresses, fun_XXXXX code.");
        line("//  * ltu/leu/gtu/geu are unsigned comparisons; setB/setH replace the low byte/half of a value");
        line("//    (PIP2 .b/.h register operations).");
        line("//  * The class compiles against desktop/out and runs with wolphun.decomp.rt.RunDecompiled.");
        if(!notes.isEmpty()){
            line("//");
            line("// Decompiler notes:");
            for(String n : notes) line("//  - "+n);
        }
        line("");
        if(pkg!=null && !pkg.isEmpty()){ line("package "+pkg+";"); line(""); }
        line("import wolphun.decomp.rt.Pip2Runtime;");
        line("");
        line("@SuppressWarnings(\"unused\")");
        line("public final class "+className+" extends Pip2Runtime {");
        ind++;
        line("");
        line("// ---- address-taken functions (function pointers; dispatched by icall)");
        for(Func f : prog.funcs.values()) if(f.addressTaken) line(String.format("static final int %s = 0x%x;", f.name, f.entry));
        line("");
        line("// ---- data / bss addresses referenced by the code");
        for(int a : dataSyms){
            String s=img.stringAt(a);
            String where = img.inBss(a)? "bss" : img.inData(a)? "data" : "end";
            line(String.format("static final int g_%x = 0x%x;   // %s%s", a, a, where, s!=null? "  "+s : ""));
        }
        line("");
        {
            Func ef=prog.funcAt(img.codeBase);
            StringBuilder za=new StringBuilder();
            for(int k=0;k<Analysis.paramOrder(ef.params).length;k++) za.append(k>0? ", 0" : "0");
            line("@Override public void entry() { "+ef.name+"("+za+"); }");
        }
        line("");
        dispatcher();
        line("");
        sb.append(methods);
        line("// ==================================================================== SDK imports");
        for(String name : importsUsed) stub(name);
        ind--;
        line("}");
        return sb.toString();
    }

    // ------------------------------------------------------------------ class parts
    private void dispatcher(){
        line("/** indirect calls (CALLr) and constructors: function pointer -> method */");
        line("@Override protected int icall(int fn, int p0, int p1, int p2, int p3) {");
        ind++;
        line("switch (fn) {");
        ind++;
        for(Func f : prog.funcs.values()){
            if(!f.addressTaken) continue;
            int[] ps=Analysis.paramOrder(f.params);
            StringBuilder a=new StringBuilder();
            for(int k=0;k<ps.length;k++){
                if(k>0) a.append(", ");
                a.append(ps[k]>=Ir.P0 && ps[k]<=Ir.P3? Ir.regName(ps[k]) : "0");
            }
            String call=f.name+"("+a+")";
            line("case "+f.name+": "+(f.returns? "return "+call+";" : call+"; return 0;"));
        }
        line("default: throw badPointer(fn);");
        ind--;
        line("}");
        ind--;
        line("}");
    }

    private static final Set<String> KEYWORDS = new HashSet<String>(Arrays.asList(
        "abstract","assert","boolean","break","byte","case","catch","char","class","const","continue","default","do",
        "double","else","enum","extends","final","finally","float","for","goto","if","implements","import","instanceof",
        "int","interface","long","native","new","package","private","protected","public","return","short","static",
        "strictfp","super","switch","synchronized","this","throw","throws","transient","try","void","volatile","while",
        "true","false","null","fn","name"));
    private void stub(String name){
        ImportSigs.Sig s=ImportSigs.get(name);
        int n=s.regArgs();
        StringBuilder proto=new StringBuilder(), params=new StringBuilder(), args=new StringBuilder();
        for(int k=0;k<s.names.length;k++){ if(k>0) proto.append(", "); proto.append(s.types[k]).append(' ').append(s.names[k]); }
        for(int k=0;k<n;k++){
            String pn=s.names[k]; if(KEYWORDS.contains(pn)) pn=pn+"_";
            if(k>0) params.append(", ");
            params.append("int ").append(pn);
            args.append(", ").append(pn);
        }
        for(int k=n;k<4;k++) args.append(", 0");
        String doc = ImportSigs.known(name)? (s.returnsValue? "value" : "void")+" "+name+"("+proto+")" : name+"(?)  (prototype unknown: 4 register arguments assumed)";
        if(s.names.length>4) doc += "   [arguments 5+ are passed on the stack]";
        line("/** "+doc+" */");
        if(s.returnsValue) line("private int "+name+"("+params+") { return sys(\""+name+"\""+args+"); }");
        else               line("private void "+name+"("+params+") { sys(\""+name+"\""+args+"); }");
    }

    private String method(Func f, Node body){
        cur=f;
        sb=new StringBuilder(); ind=1;
        int[] ps=Analysis.paramOrder(f.params);
        // javadoc
        line("/**");
        line(String.format(" * %s @ 0x%x%s", f.name, f.entry, f.addressTaken? "  (address taken)" : ""));
        if(!f.callers.isEmpty()) line(" * called from: "+names(f.callers));
        if(f.irreducible) line(" * irreducible control flow: emitted as a state machine");
        line(" */");
        StringBuilder sig=new StringBuilder();
        sig.append(f.returns? "int " : "void ").append(f.name).append('(');
        for(int k=0;k<ps.length;k++){ if(k>0) sig.append(", "); sig.append("int ").append(Ir.regName(ps[k])); }
        sig.append(") {");
        line(sig.toString());
        ind++;
        // locals
        TreeSet<Integer> regs=new TreeSet<Integer>();
        collectRegs(body, regs);
        for(int p : ps) regs.remove(p);
        regs.remove(Ir.SP); regs.remove(Ir.ZERO);
        if(!regs.isEmpty()){
            StringBuilder l=new StringBuilder("int ");
            int k=0;
            for(int r : regs){ if(k++>0) l.append(", "); l.append(rn(r)).append(" = 0"); }
            line(l.append(';').toString());
        }
        node(body);
        if(f.returns && Structurer.canComplete(body)) line("throw unreachable(0);");
        ind--;
        line("}");
        line("");
        return sb.toString();
    }
    private String names(Set<Integer> addrs){
        StringBuilder s=new StringBuilder(); int k=0;
        for(int a : addrs){ if(k++>0) s.append(", "); if(k>12){ s.append("..."); break; } s.append(prog.funcName(a)); }
        return s.toString();
    }
    private static String rn(int r){ return r==Structurer.STATE? "state" : Ir.regName(r); }

    private void collectRegs(Node n, Set<Integer> regs){
        if(n instanceof SNode){
            Stmt s=((SNode)n).s;
            if(s instanceof Assign) regs.add(((Assign)s).r);
            for(Expr e : Ir.exprsOf(s)) collectRegs(e, regs);
        } else if(n instanceof If) collectRegs(((If)n).cond, regs);
        else if(n instanceof Loop && ((Loop)n).cond!=null) collectRegs(((Loop)n).cond, regs);
        else if(n instanceof Switch) collectRegs(((Switch)n).sel, regs);
        else if(n instanceof Ret && ((Ret)n).value!=null) collectRegs(((Ret)n).value, regs);
        for(Node k : Structurer.kids(n)) collectRegs(k, regs);
    }
    private void collectRegs(Expr e, Set<Integer> regs){
        if(e instanceof Reg) regs.add(((Reg)e).r);
        for(Expr c : Ir.children(e)) collectRegs(c, regs);
    }

    // ------------------------------------------------------------------ statements
    private void line(String s){
        for(int i=0;i<ind;i++) sb.append("    ");
        sb.append(s).append('\n');
    }

    private int labelRefs(Node body, String label){
        return countLabeled(body, label);
    }
    private static int countLabeled(Node n, String label){
        if(n instanceof Jump){ Jump j=(Jump)n; return (!j.bare && label.equals(j.label))? 1:0; }
        int c=0; for(Node k : Structurer.kids(n)) c+=countLabeled(k, label); return c;
    }

    private void node(Node n){
        if(n==null) return;
        if(n instanceof Seq){ for(Node k : ((Seq)n).items) node(k); return; }
        if(n instanceof SNode){ line(stmt(((SNode)n).s)); return; }
        if(n instanceof Cmt){ line("// "+((Cmt)n).text); return; }
        if(n instanceof If){ ifChain((If)n, ""); return; }
        if(n instanceof Blk){
            Blk b=(Blk)n;
            line((labelRefs(b.body, b.label)>0? b.label+": " : "")+"{");
            ind++; node(b.body); ind--;
            line("}");
            return;
        }
        if(n instanceof Loop){
            Loop l=(Loop)n;
            String lab = labelRefs(l.body, l.label)>0? l.label+": " : "";
            if(l.doWhile){
                line(lab+"do {");
                ind++; node(l.body); ind--;
                line("} while ("+cond(l.cond)+");");
            } else {
                line(lab+"while ("+(l.cond==null? "true" : cond(l.cond))+") {");
                ind++; node(l.body); ind--;
                line("}");
            }
            return;
        }
        if(n instanceof Jump){
            Jump j=(Jump)n;
            line((j.brk? "break" : "continue")+(j.bare? "" : " "+j.label)+";");
            return;
        }
        if(n instanceof Ret){
            Ret r=(Ret)n;
            line(r.value==null? "return;" : "return "+expr(r.value, 0)+";");
            return;
        }
        if(n instanceof Trap){ line("throw "+((Trap)n).call+";"); return; }
        if(n instanceof Switch){
            Switch s=(Switch)n;
            line("switch ("+expr(s.sel, 0)+") {");
            ind++;
            for(int k=0;k<s.bodies.size();k++){
                for(int v : s.vals.get(k)) line("case "+caseNum(v)+":");
                ind++; node(s.bodies.get(k)); ind--;
            }
            if(s.deflt!=null){ line("default:"); ind++; node(s.deflt); ind--; }
            ind--;
            line("}");
        }
    }
    private String caseNum(int v){ return (v>=0 && v<4096)? Integer.toString(v) : "0x"+Integer.toHexString(v); }

    private void ifChain(If i, String prefix){
        line(prefix+"if ("+cond(i.cond)+") {");
        ind++; node(i.then); ind--;
        Node e=i.els;
        if(e instanceof Seq && ((Seq)e).items.size()==1) e=((Seq)e).items.get(0);
        if(e==null || (e instanceof Seq && ((Seq)e).items.isEmpty())){ line("}"); return; }
        if(e instanceof If){ ifChain((If)e, "} else "); return; }   // "} else if (...) {"
        line("} else {");
        ind++; node(e); ind--;
        line("}");
    }

    private String stmt(Stmt s){
        if(s instanceof Assign){
            Assign a=(Assign)s;
            String lhs=rn(a.r);
            Expr e=a.e;
            if(e instanceof Bin){
                Bin b=(Bin)e;
                if(b.a instanceof Reg && ((Reg)b.a).r==a.r && b.op.sym!=null && !b.op.isBool() && b.op!=Op.DIV){
                    if(b.op==Op.ADD && b.b instanceof Const && ((Const)b.b).kind==Ir.NUM){
                        int v=((Const)b.b).v;
                        if(v==1) return lhs+"++;";
                        if(v==-1) return lhs+"--;";
                        if(v<0 && v!=Integer.MIN_VALUE) return lhs+" -= "+num(-v)+";";
                    }
                    return lhs+" "+b.op.sym+"= "+expr(b.b, b.op.prec+1)+";";
                }
            }
            return lhs+" = "+expr(e, 0)+";";
        }
        if(s instanceof Store){
            Store st=(Store)s;
            return "put"+(st.width*8)+"("+expr(st.addr,0)+", "+expr(st.val,0)+");";
        }
        if(s instanceof Eval) return expr(((Eval)s).e, 0)+";";
        if(s instanceof Comment) return "// "+((Comment)s).text;
        return "/* ? */";
    }

    // ------------------------------------------------------------------ expressions
    private String cond(Expr c){
        if(c.isBool()) return expr(c, 0);
        return expr(c, 9)+" != 0";
    }

    static String num(int v){
        if(v>=-4096 && v<=4096) return Integer.toString(v);
        return "0x"+Integer.toHexString(v).toUpperCase();
    }
    static String hexNum(int v){
        if(v>=0 && v<10) return Integer.toString(v);
        return "0x"+Integer.toHexString(v).toUpperCase();
    }

    private String konst(Const c, boolean hexHint){
        if(c.kind==Ir.CODE){
            String fn=prog.funcName(c.v);
            return fn!=null? fn : String.format("0x%x /*code*/", c.v);
        }
        if(c.kind==Ir.DATA){
            dataSyms.add(c.v);
            return String.format("g_%x", c.v);
        }
        return hexHint? hexNum(c.v) : num(c.v);
    }

    private static final int PREC_UNARY=14;

    /** prints e; parenthesised if its precedence is below `ctx` */
    private String expr(Expr e, int ctx){
        String s; int p;
        if(e instanceof Const){
            Const c=(Const)e;
            s=konst(c, false); p= (c.kind==Ir.NUM && c.v<0)? PREC_UNARY : 20;
        } else if(e instanceof Reg){ s=rn(((Reg)e).r); p=20; }
        else if(e instanceof Load){
            Load l=(Load)e;
            String fn = l.width==4? "i32" : (l.signed? "s" : "u")+(l.width*8);
            s=fn+"("+expr(l.addr,0)+")"; p=20;
        } else if(e instanceof SetPart){
            SetPart sp=(SetPart)e;
            if(sp.shift==0) s=(sp.bits==8? "setB(" : "setH(")+expr(sp.old,0)+", "+expr(sp.val,0)+")";
            else s="setBits("+expr(sp.old,0)+", "+expr(sp.val,0)+", "+sp.shift+", "+sp.bits+")";
            p=20;
        } else if(e instanceof Call){ s=call((Call)e); p=20; }
        else if(e instanceof Un){
            Un u=(Un)e;
            if(u.op==UOp.LNOT){ s="!"+expr(u.a, PREC_UNARY); }
            else s=u.op.sym+expr(u.a, PREC_UNARY);
            p=PREC_UNARY;
        } else {
            Bin b=(Bin)e;
            if(b.op.sym==null){
                String fn = b.op==Op.DIVU? "divu" : b.op.name().toLowerCase();
                s=fn+"("+expr(b.a,0)+", "+expr(b.b,0)+")"; p=20;
            } else {
                Op op=b.op; Expr rhs=b.b;
                if(op==Op.ADD && rhs instanceof Const && ((Const)rhs).kind==Ir.NUM && ((Const)rhs).v<0 && ((Const)rhs).v!=Integer.MIN_VALUE){
                    op=Op.SUB; rhs=new Const(-((Const)rhs).v);
                }
                p=op.prec;
                boolean bitwise = op==Op.AND||op==Op.OR||op==Op.XOR;
                String l=operand(b.a, op, p, false);
                String r= (bitwise && rhs instanceof Const && ((Const)rhs).kind==Ir.NUM)? konst((Const)rhs, true) : operand(rhs, op, p+1, true);
                s=l+" "+op.sym+" "+r;
            }
        }
        return p<ctx? "("+s+")" : s;
    }
    /** operand of a binary operator; adds clarifying parentheses when bitwise and arithmetic operators mix */
    private String operand(Expr x, Op parent, int need, boolean right){
        if(x instanceof Bin && ((Bin)x).op.sym!=null){
            Op c=((Bin)x).op;
            boolean pa = parent==Op.ADD||parent==Op.SUB, ca = c==Op.ADD||c==Op.SUB;
            boolean pm = parent==Op.MUL||parent==Op.DIV, cm = c==Op.MUL||c==Op.DIV;
            boolean same = c==parent && !right || (pa&&ca&&!right) || (pm&&cm&&!right);
            boolean cmpParent = parent.isCompare() && (ca||cm);
            boolean logic = (parent==Op.LAND||parent==Op.LOR) && c.isCompare();
            if(!same && !cmpParent && !logic && !(pa && cm)) return "("+expr(x,0)+")";
        }
        return expr(x, need);
    }
    private String call(Call c){
        StringBuilder a=new StringBuilder();
        if(c.kind==CallKind.INDIRECT) a.append(expr(c.fptr,0));
        for(Expr x : c.args){ if(a.length()>0) a.append(", "); a.append(expr(x,0)); }
        String name;
        switch(c.kind){
            case LOCAL: name=prog.funcName(c.target); break;
            case IMPORT: name=c.name; importsUsed.add(c.name); break;
            case INDIRECT: name="icall"; break;
            default: name=c.name;
        }
        String s=name+"("+a+")";
        // string arguments: show the text next to the call
        if(c.kind==CallKind.IMPORT){
            for(Expr x : c.args) if(x instanceof Const && ((Const)x).kind==Ir.DATA){
                String str=img.stringAt(((Const)x).v);
                if(str!=null){ s+=" /* "+str.replace("*/","* /")+" */"; break; }
            }
        }
        return s;
    }
}
