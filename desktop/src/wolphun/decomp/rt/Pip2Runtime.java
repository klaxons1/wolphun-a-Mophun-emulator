package wolphun.decomp.rt;

import wolphun.vm.Pip2;
import wolphun.vm.Sdk;
import wolphun.vm.Vmgp;

/**
 * Base class of every class produced by the PIP2 -> Java decompiler.
 *
 * It supplies what the decompiled code cannot express by itself:
 * <ul>
 *   <li>the game's 32-bit address space (data, bss, heap, stack) -- the memory of a
 *       wolphun.vm.Pip2 instance laid out exactly like the interpreter does it;</li>
 *   <li>the stack pointer {@link #sp} (stack frames stay in emulated memory because games pass
 *       addresses of stack buffers to the SDK);</li>
 *   <li>the bridge to the SDK: {@link #sys} loads the arguments into the VM registers and runs
 *       the emulator's own wolphun.vm.Sdk implementation, so graphics/sound/input behave
 *       exactly as in the interpreter;</li>
 *   <li>small helpers for the PIP2 operations Java has no operator for.</li>
 * </ul>
 * Cooperative tasks (vCreateTask) are not supported by decompiled code.
 */
public abstract class Pip2Runtime {
    /** thrown to unwind the Java stack when the game ends (vTerminateVMGP, KILLTASK, flip limit) */
    public static final class Terminated extends RuntimeException {
        public Terminated(String why){ super(why, null, false, false); }
    }

    protected Pip2 vm;
    protected Sdk sdk;
    /** emulated stack pointer (register sp) */
    protected int sp;
    /** optional hook run after every SDK call (used by the runner to stop after N frames) */
    public Runnable afterSys;
    public long sysCalls;
    /** when set, every SDK call is logged as "name(args) -> result" */
    public SdkTrace tracer;
    /** optional hook run before every SDK call (the runner uses it for scripted input) */
    public java.util.function.Consumer<String> beforeSys;

    public static int regArgs(String name){ return wolphun.decomp.ImportSigs.regArgCount(name); }

    public void attach(Pip2 vm, Sdk sdk){
        this.vm=vm; this.sdk=sdk;
        this.sp=vm.getReg(Pip2.SP);
    }

    /** the program entry (start of the code section) */
    public abstract void entry();

    /** CALLr: calls the function at address fn with the argument registers */
    protected abstract int icall(int fn, int p0, int p1, int p2, int p3);
    protected final int icall(int fn){ return icall(fn, 0, 0, 0, 0); }
    protected final int icall(int fn, int p0){ return icall(fn, p0, 0, 0, 0); }
    protected final int icall(int fn, int p0, int p1){ return icall(fn, p0, p1, 0, 0); }
    protected final int icall(int fn, int p0, int p1, int p2){ return icall(fn, p0, p1, p2, 0); }

    /** runs the global constructors (pool symbol "~C", as the mophun runtime does) and then entry() */
    public void run(){
        try {
            runConstructors();
            entry();
        } catch(Terminated t){
            // normal end of the game
        }
    }
    protected void runConstructors(){
        Vmgp exe=vm.exe;
        for(int i=0;i<exe.poolCount;i++){
            Vmgp.PoolItem it=exe.pool[i];
            if(it.type==3 && "~C".equals(exe.getString(it.meta))){
                int addr=(int)vm.poolValue[i];
                for(int n=0;n<256;n++){
                    int ctor=i32(addr); addr+=4;
                    if(ctor==0) break;
                    int saved=sp;
                    icall(ctor, 0, 0, 0, 0);
                    sp=saved;
                }
                return;
            }
        }
    }

    // ------------------------------------------------------------------ memory
    protected final int s8(int a){ return (byte)vm.read8(a); }
    protected final int u8(int a){ return vm.read8(a); }
    protected final int s16(int a){ return (short)vm.read16(a); }
    protected final int u16(int a){ return vm.read16(a); }
    protected final int i32(int a){ return vm.read32(a); }
    protected final void put8(int a, int v){ vm.write8(a, v); }
    protected final void put16(int a, int v){ vm.write16(a, v); }
    protected final void put32(int a, int v){ vm.write32(a, v); }
    protected final void memcpy(int dst, int src, int n){ for(int i=0;i<n;i++) vm.write8(dst+i, vm.read8(src+i)); }
    protected final void memset(int dst, int v, int n){ for(int i=0;i<n;i++) vm.write8(dst+i, v); }

    // ------------------------------------------------------------------ operators
    /** replace the low byte of old by v (PIP2 ".b" destination) */
    protected static int setB(int old, int v){ return (old & 0xFFFFFF00) | (v & 0xFF); }
    /** replace the low half of old by v (PIP2 ".h" destination) */
    protected static int setH(int old, int v){ return (old & 0xFFFF0000) | (v & 0xFFFF); }
    protected static int setBits(int old, int v, int shift, int bits){
        int m=(int)((1L<<bits)-1) << shift;
        return (old & ~m) | ((v << shift) & m);
    }
    protected static boolean ltu(int a, int b){ return (a ^ 0x80000000) <  (b ^ 0x80000000); }
    protected static boolean leu(int a, int b){ return (a ^ 0x80000000) <= (b ^ 0x80000000); }
    protected static boolean gtu(int a, int b){ return (a ^ 0x80000000) >  (b ^ 0x80000000); }
    protected static boolean geu(int a, int b){ return (a ^ 0x80000000) >= (b ^ 0x80000000); }
    /** signed division; x/0 = 0 as in the VM */
    protected static int div(int a, int b){ return b==0? 0 : a/b; }
    /** unsigned division; x/0 = 0 as in the VM */
    protected static int divu(int a, int b){ return b==0? 0 : (int)((a & 0xFFFFFFFFL)/(b & 0xFFFFFFFFL)); }

    // ------------------------------------------------------------------ control
    /** SLEEP: yield to other tasks -- there are none in decompiled code */
    protected final void sleep(){ }
    protected final RuntimeException killTask(){ throw new Terminated("KILLTASK"); }
    /** after vTerminateVMGP (which already throws Terminated through sys) */
    protected final RuntimeException terminated(){ throw new Terminated("vTerminateVMGP"); }
    protected final RuntimeException unreachable(int pc){
        return new IllegalStateException(String.format("decompiled code reached an impossible path (0x%x)", pc));
    }
    protected final RuntimeException badPointer(int fn){
        return new IllegalStateException(String.format("indirect call to 0x%x, which is not a function entry", fn));
    }

    // ------------------------------------------------------------------ SDK bridge
    /** calls SDK function `name` with the argument registers p0..p3 (more arguments are on the stack at sp) */
    protected final int sys(String name, int a0, int a1, int a2, int a3){
        // byte/short parameters: the callee only sees the low bits (the bytecode may leave garbage above)
        a0=wolphun.decomp.ImportSigs.normArg(name, 0, a0); a1=wolphun.decomp.ImportSigs.normArg(name, 1, a1);
        a2=wolphun.decomp.ImportSigs.normArg(name, 2, a2); a3=wolphun.decomp.ImportSigs.normArg(name, 3, a3);
        vm.setReg(Pip2.SP, sp);
        vm.setReg(Pip2.P0, a0); vm.setReg(Pip2.P1, a1); vm.setReg(Pip2.P2, a2); vm.setReg(Pip2.P3, a3);
        sysCalls++;
        if(beforeSys!=null) beforeSys.accept(name);
        if(tracer!=null) tracer.before(vm, name);
        sdk.invoke(name);
        if(tracer!=null) tracer.after(vm, name);
        if(vm.stopped) throw new Terminated(name);
        if(afterSys!=null) afterSys.run();
        return vm.getReg(Pip2.R0);
    }
}
