package wolphun.decomp;

import java.util.*;

import wolphun.vm.Vmgp;

/**
 * Whole-program view: decoded instructions, function entry points, switch tables and the
 * control-flow graph of every function.
 *
 * Function discovery: the entry point (start of code), every CALLl target, and every address
 * of the code section that the program takes as a value (an instruction immediate, or a
 * relocated pointer in data), minus the targets of switch jump tables. A function body is the code
 * reachable from its entry without following calls; reaching another entry by a jump or by
 * falling through is a tail call.
 */
final class Program {

    // =====================================================================================
    // instructions
    // =====================================================================================
    static final class Insn {
        final int pc, word, op, d, s, t, ext;
        Insn(int pc, int word, int ext){
            this.pc=pc; this.word=word; this.ext=ext;
            op=word&0xff; d=(word>>>8)&0xff; s=(word>>>16)&0xff; t=(word>>>24)&0xff;
        }
        boolean hasExt(){ return op>=0x4A && op<=0x66; }
        int size(){ return hasExt()? 8 : 4; }
        boolean extIsRaw(){ return (ext & 0x80000000)!=0; }
        int raw(){ return (ext & 0x7FFFFFFF) | ((ext<<1) & 0x80000000); }
        int poolIdx(){ return ext-1; }
        boolean isCondBranch(){ return (op>=0x2C && op<=0x3F) || (op>=0x5D && op<=0x66); }
    }

    final Image img;
    final List<String> warnings = new ArrayList<String>();
    private final HashMap<Integer,Insn> insnCache = new HashMap<Integer,Insn>();

    Insn insn(int pc){
        Insn i = insnCache.get(pc);
        if(i==null){ i = new Insn(pc, img.read32(pc), img.read32(pc+4)); insnCache.put(pc, i); }
        return i;
    }
    boolean validOp(int pc){ return img.inCode(pc) && (img.read32(pc)&0xff) <= 0x66; }

    /** value of the extra word as the interpreter's fetchImm() sees it */
    int immValue(Insn i){
        if(i.extIsRaw()) return i.raw();
        int idx=i.poolIdx();
        return (idx>=0 && idx<img.poolCount())? img.poolValue(idx) : 0;
    }
    /** import name if this CALLl calls an SDK function */
    String importOf(Insn i){
        if(i.op!=0x5C || i.extIsRaw()) return null;
        int idx=i.poolIdx();
        return (idx>=0 && idx<img.poolCount() && img.poolIsImport(idx))? img.poolName(idx) : null;
    }
    /** SDK functions that never return (the game ends inside them) */
    boolean noReturn(Insn i){ return i.op==0x5C && "vTerminateVMGP".equals(importOf(i)); }
    /** absolute target of CALLl / JPl (local) */

    int jumpTarget(Insn i){
        if(i.extIsRaw()) return i.pc + i.raw();
        int idx=i.poolIdx();
        return (idx>=0 && idx<img.poolCount())? img.poolValue(idx) : 0;
    }
    /** taken target of a conditional branch */
    int branchTarget(Insn i){
        if(i.op<=0x3F) return i.pc + ((byte)i.t)*4;      // brI: offset in the t field
        return i.pc + immValue(i);                        // brR: pc-relative word
    }

    // =====================================================================================
    // switch tables:  SLLi a = b << 2 ; LDW c = [a + TABLE] ; JPr c
    // =====================================================================================
    static final class SwitchTable {
        int jprPc, table, count, indexReg, shiftReg, loadReg; boolean bounded;
        int[] targets;
    }
    final HashMap<Integer,SwitchTable> switches = new HashMap<Integer,SwitchTable>();
    private final HashSet<Integer> switchTableWords = new HashSet<Integer>();
    private final HashMap<Integer,Integer> relocCodePtr = new HashMap<Integer,Integer>(); // addr of a relocated word -> code target

    // =====================================================================================
    // functions / CFG
    // =====================================================================================
    static final class Block {
        int id, start;
        final List<Insn> insns = new ArrayList<Insn>();
        List<Ir.Stmt> stmts = new ArrayList<Ir.Stmt>();
        Ir.Term term;
        int[] succ = new int[0];
        final List<Integer> preds = new ArrayList<Integer>();
    }
    static final class Func {
        final int entry; String name;
        final List<Block> blocks = new ArrayList<Block>();
        final HashMap<Integer,Block> byAddr = new HashMap<Integer,Block>();
        boolean elideSaves = true;     // STORE/RET register saves are not materialised (see Lifter)
        boolean addressTaken;
        long params;                   // register mask of parameters
        boolean mayDefR0, returns;
        final Set<Integer> callees = new TreeSet<Integer>();
        final Set<Integer> callers = new TreeSet<Integer>();
        boolean irreducible;
        Func(int e){ entry=e; }
    }

    final TreeMap<Integer,Func> funcs = new TreeMap<Integer,Func>();
    final TreeSet<Integer> entries = new TreeSet<Integer>();
    final TreeSet<Integer> addressTaken = new TreeSet<Integer>();
    final TreeSet<Integer> importsUsed = new TreeSet<Integer>();
    final TreeSet<String> importNames = new TreeSet<String>();

    Program(Image img){
        this.img = img;
        collectRelocs();
        linearScan();
        for(int e : entries) buildFunc(e);
        for(Func f : funcs.values()) f.addressTaken = addressTaken.contains(f.entry);
        nameFunctions();
    }

    private void collectRelocs(){
        for(int i=0;i<img.poolCount();i++){
            Vmgp.PoolItem p = img.pool(i);
            if(p.type==4 && p.meta==1){            // SectionRelativeReloc of a word that points into code
                int addr = ((p.itemTarget==1)? img.codeBase : img.dataBase) + p.target;
                relocCodePtr.put(addr, img.read32(addr));
            }
        }
    }

    private void linearScan(){
        entries.add(img.codeBase);
        List<Insn> lin = new ArrayList<Insn>();
        for(int pc=img.codeBase; pc<img.codeEnd; ){
            Insn i = insn(pc); lin.add(i); pc += i.size();
        }
        for(int k=0;k<lin.size();k++){
            Insn i = lin.get(k);
            if(i.op==0x5C){
                String imp = importOf(i);
                if(imp!=null){ importNames.add(imp); continue; }
                int t = jumpTarget(i);
                if(img.inCode(t)) entries.add(t);
                else warnings.add(String.format("CALLl at 0x%x to non-code address 0x%x", i.pc, t));
            } else if(i.op==0x41){
                SwitchTable st = matchSwitch(lin, k);
                if(st!=null){ switches.put(i.pc, st); for(int w=0;w<st.count;w++) switchTableWords.add(st.table+4*w); }
            }
        }
        // address-taken code: a code address used as a VALUE by an instruction (LDI, ADDi, ...;
        // CALLl/JPl/branches reference their targets through the pool too, but that is not
        // taking an address), exported code symbols, and relocated code pointers in data
        for(Insn i : lin){
            if(!i.hasExt() || i.extIsRaw() || i.op==0x5B || i.op==0x5C || i.isCondBranch()) continue;
            int idx=i.poolIdx();
            if(idx<0 || idx>=img.poolCount() || img.poolIsImport(idx)) continue;
            addTaken(img.poolValue(idx));
        }
        for(int i=0;i<img.poolCount();i++){
            Vmgp.PoolItem p = img.pool(i);
            if(p.type==3 && p.itemTarget==1) addTaken(img.poolValue(i));
        }
        for(Map.Entry<Integer,Integer> e : relocCodePtr.entrySet())
            if(!switchTableWords.contains(e.getKey())) addTaken(e.getValue());
    }
    private void addTaken(int v){
        if(!img.inCode(v) || (v&3)!=0) return;
        addressTaken.add(v); entries.add(v);
    }

    private SwitchTable matchSwitch(List<Insn> lin, int k){
        if(k<2) return null;
        Insn jp=lin.get(k), ld=lin.get(k-1), sl=lin.get(k-2);
        int c = jp.d;
        if(ld.op!=0x57 || ld.d!=c || ld.extIsRaw()) return null;
        if(sl.op!=0x1C || sl.t!=2 || sl.d!=ld.s) return null;
        int table = immValue(ld);
        if(!img.inData(table) && !img.inCode(table)) return null;
        SwitchTable st = new SwitchTable();
        st.jprPc=jp.pc; st.table=table; st.indexReg=sl.s; st.shiftReg=sl.d; st.loadReg=c;
        // bound check on the index register shortly before
        int count=-1;
        for(int j=k-3, n=0; j>=0 && n<8; j--, n++){
            Insn b=lin.get(j);
            if(b.op==0x31 && b.d==st.indexReg){ count=b.s+1; break; }          // BGTUI idx, N -> default
            if(b.op==0x2F && b.d==st.indexReg){ count=b.s; break; }            // BGEUI idx, N -> default
            if(b.op==0x62 && b.d==st.indexReg){                                 // BGTU idx, reg
                Integer kv = constBefore(lin, j, b.s); if(kv!=null){ count=kv+1; } break; }
            if(b.op==0x60 && b.d==st.indexReg){                                 // BGEU idx, reg
                Integer kv = constBefore(lin, j, b.s); if(kv!=null){ count=kv; } break; }
            if(writesReg(b, st.indexReg) || b.isCondBranch() || b.op==0x5B || b.op==0x45) break;
        }
        st.bounded = count>0;
        if(count<=0){                                   // fall back to the relocated words of the table
            count=0;
            while(count<1024 && relocCodePtr.containsKey(table+4*count)) count++;
        }
        if(count<=0 || count>1024) return null;
        st.count=count; st.targets=new int[count];
        for(int w=0;w<count;w++){
            st.targets[w]=img.read32(table+4*w);
            if(!img.inCode(st.targets[w])) return null;
        }
        return st;
    }
    private Integer constBefore(List<Insn> lin, int j, int regOff){
        for(int q=j-1, n=0; q>=0 && n<6; q--, n++){
            Insn x=lin.get(q);
            if(x.d==regOff && x.op==0x40) return (int)(short)(x.word>>>16);
            if(x.d==regOff && x.op==0x5A) return immValue(x);
            if(writesReg(x, regOff)) return null;
        }
        return null;
    }
    static boolean writesReg(Insn i, int off){
        int op=i.op;
        if(op>=0x2C && op<=0x3F) return false;
        if(op>=0x41 && op<=0x49) return op==0x44 || op==0x45;
        if(op==0x52||op==0x53||op==0x54) return false;
        if(op>=0x5B) return op==0x5C;
        return (i.d>>2)==(off>>2);
    }

    // ------------------------------------------------------------------ function bodies
    private void buildFunc(int entry){
        Func f = new Func(entry);
        funcs.put(entry, f);
        // 1. explore reachable instructions, collecting block leaders
        TreeSet<Integer> leaders = new TreeSet<Integer>();
        HashSet<Integer> seen = new HashSet<Integer>();
        ArrayDeque<Integer> work = new ArrayDeque<Integer>();
        leaders.add(entry); work.add(entry);
        while(!work.isEmpty()){
            int pc = work.poll();
            while(true){
                if(seen.contains(pc)) break;
                if(pc!=entry && entries.contains(pc)) break;          // tail call / fall into another function
                if(!img.inCode(pc)){ warnings.add(String.format("%s: control flow leaves the code section at 0x%x", hex(entry), pc)); break; }
                seen.add(pc);
                Insn i = insn(pc);
                if(i.op>0x66){ warnings.add(String.format("%s: invalid opcode 0x%02x at 0x%x", hex(entry), i.op, pc)); break; }
                int next = pc + i.size();
                if(i.isCondBranch()){
                    int t = branchTarget(i);
                    addEdge(entry, t, leaders, work);
                    addEdge(entry, next, leaders, work);
                    break;
                }
                if(i.op==0x5B){ addEdge(entry, jumpTarget(i), leaders, work); break; }
                if(i.op==0x41){
                    SwitchTable st = switches.get(pc);
                    if(st!=null) for(int t: st.targets) addEdge(entry, t, leaders, work);
                    break;
                }
                if(i.op==0x45 || i.op==0x46 || noReturn(i)) break;
                pc = next;
                if(entries.contains(pc)){ leaders.add(pc); break; }   // falls into the next function
            }
        }
        // 2. form blocks
        for(int l : leaders){
            if(l!=entry && entries.contains(l)) continue;     // belongs to another function (tail call target)
            if(!seen.contains(l)) continue;
            Block b = new Block(); b.start=l; b.id=f.blocks.size();
            int pc=l;
            while(true){
                Insn i = insn(pc); b.insns.add(i);
                int next = pc + i.size();
                if(i.isCondBranch() || i.op==0x5B || i.op==0x41 || i.op==0x45 || i.op==0x46 || noReturn(i)) break;
                if(i.op>0x66) break;
                if(leaders.contains(next) || !seen.contains(next)) break;
                pc = next;
            }
            if(l==entry) f.blocks.add(0, b); else f.blocks.add(b);
            f.byAddr.put(l, b);
        }
        for(int k=0;k<f.blocks.size();k++) f.blocks.get(k).id=k;
        for(Block b : f.blocks) for(Insn i : b.insns){
            if(i.op==0x44) f.elideSaves=false;
            if(i.op==0x5C && importOf(i)==null) f.callees.add(jumpTarget(i));
        }
    }
    private void addEdge(int entry, int t, TreeSet<Integer> leaders, ArrayDeque<Integer> work){
        leaders.add(t);
        if(t==entry || !entries.contains(t)) work.add(t);
    }

    // ------------------------------------------------------------------ naming
    private void nameFunctions(){
        for(Func f : funcs.values()) f.name = "fun_"+Integer.toHexString(f.entry);
        // the global symbols of the pool (rare in shipped games, but free names when present)
        for(int i=0;i<img.poolCount();i++){
            Vmgp.PoolItem p = img.pool(i);
            if(p.type==3 && p.itemTarget==1){
                Func f = funcs.get(img.poolValue(i));
                String s = img.symbol(i);
                if(f!=null && s!=null && s.matches("[A-Za-z_][A-Za-z0-9_]*")) f.name = s;
            }
        }
        Func start = funcs.get(img.codeBase);
        if(start!=null){
            start.name = "start";
            // crt0:  CALLl main ; CALLl vTerminateVMGP
            Block b0 = start.blocks.isEmpty()? null : start.blocks.get(0);
            if(b0!=null && b0.insns.size()>=2 && b0.insns.get(0).op==0x5C && importOf(b0.insns.get(0))==null
               && "vTerminateVMGP".equals(importOf(b0.insns.get(1)))){
                Func m = funcs.get(jumpTarget(b0.insns.get(0)));
                if(m!=null && m.name.startsWith("fun_")) m.name="main";
            }
        }
    }

    Func funcAt(int addr){ return funcs.get(addr); }
    String funcName(int addr){ Func f=funcs.get(addr); return f!=null? f.name : null; }
    static String hex(int v){ return "0x"+Integer.toHexString(v); }
}
