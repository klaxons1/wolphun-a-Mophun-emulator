package wolphun.decomp.rt;

import wolphun.j2me.Sdk;

/**
 * J2ME (CLDC 1.1) twin of desktop/src/wolphun/decomp/rt/Pip2Runtime.java.
 *
 * Same API for the decompiled class (s8/u8/s16/u16/i32/put8/put16/put32, sp, icall, ltu..., sys),
 * but the address space is a plain byte[] owned by this object and the SDK is the compact
 * wolphun.j2me.Sdk. SDK functions are numbered at build time (wolphun.decomp.j2me.J2meAssets),
 * so a call is an int switch, not a string lookup.
 */
public abstract class Pip2Runtime {
    /** thrown to unwind the Java stack when the game ends (vTerminateVMGP, KILLTASK) */
    public static final class Terminated extends RuntimeException {
        public Terminated(String why){ super(why); }
    }

    /** the game's address space: mem[0] is address {@link #base} */
    public byte[] mem;
    public int base;
    /** emulated stack pointer (register sp) */
    public int sp;
    public Sdk sdk;
    /** address of the global constructor list ("~C"), 0 = none */
    public int ctorList;

    /** the program entry (start of the code section) */
    public abstract void entry();

    /** CALLr: calls the function at address fn with the argument registers */
    protected abstract int icall(int fn, int p0, int p1, int p2, int p3);
    protected final int icall(int fn){ return icall(fn, 0, 0, 0, 0); }
    protected final int icall(int fn, int p0){ return icall(fn, p0, 0, 0, 0); }
    protected final int icall(int fn, int p0, int p1){ return icall(fn, p0, p1, 0, 0); }
    protected final int icall(int fn, int p0, int p1, int p2){ return icall(fn, p0, p1, p2, 0); }

    /** runs the global constructors and then entry(); returns normally when the game ends */
    public void run(){
        try {
            if(ctorList!=0){
                int addr=ctorList;
                for(int n=0;n<256;n++){
                    int ctor=i32(addr); addr+=4;
                    if(ctor==0) break;
                    int saved=sp;
                    icall(ctor, 0, 0, 0, 0);
                    sp=saved;
                }
            }
            entry();
        } catch(Terminated t){
            // normal end of the game
        }
    }

    // ------------------------------------------------------------------ memory (little endian)
    protected final int s8(int a){ int i=a-base; return (i>=0 && i<mem.length)? mem[i] : 0; }
    protected final int u8(int a){ int i=a-base; return (i>=0 && i<mem.length)? mem[i]&0xFF : 0; }
    protected final int s16(int a){ return (short)u16(a); }
    protected final int u16(int a){
        int i=a-base; byte[] m=mem;
        if(i>=0 && i+1<m.length) return (m[i]&0xFF)|((m[i+1]&0xFF)<<8);
        return u8(a)|(u8(a+1)<<8);
    }
    protected final int i32(int a){
        int i=a-base; byte[] m=mem;
        if(i>=0 && i+3<m.length) return (m[i]&0xFF)|((m[i+1]&0xFF)<<8)|((m[i+2]&0xFF)<<16)|(m[i+3]<<24);
        return u8(a)|(u8(a+1)<<8)|(u8(a+2)<<16)|(u8(a+3)<<24);
    }
    protected final void put8(int a, int v){ int i=a-base; if(i>=0 && i<mem.length) mem[i]=(byte)v; }
    protected final void put16(int a, int v){
        int i=a-base; byte[] m=mem;
        if(i>=0 && i+1<m.length){ m[i]=(byte)v; m[i+1]=(byte)(v>>8); return; }
        put8(a, v); put8(a+1, v>>8);
    }
    protected final void put32(int a, int v){
        int i=a-base; byte[] m=mem;
        if(i>=0 && i+3<m.length){ m[i]=(byte)v; m[i+1]=(byte)(v>>8); m[i+2]=(byte)(v>>16); m[i+3]=(byte)(v>>24); return; }
        put8(a, v); put8(a+1, v>>8); put8(a+2, v>>16); put8(a+3, v>>24);
    }
    protected final void memcpy(int dst, int src, int n){
        int d=dst-base, s=src-base;
        if(n>0 && d>=0 && s>=0 && d+n<=mem.length && s+n<=mem.length){
            // overlap: byte-forward copy, exactly like the desktop runtime (d>s replicates a pattern)
            if(d<s+n && s<d+n){ for(int i=0;i<n;i++) mem[d+i]=mem[s+i]; }
            else System.arraycopy(mem, s, mem, d, n);
            return;
        }
        for(int i=0;i<n;i++) put8(dst+i, u8(src+i));
    }
    protected final void memset(int dst, int v, int n){
        int d=dst-base; byte b=(byte)v;
        if(n>0 && d>=0 && d+n<=mem.length){ for(int i=0;i<n;i++) mem[d+i]=b; return; }
        for(int i=0;i<n;i++) put8(dst+i, v);
    }

    // ------------------------------------------------------------------ operators
    protected static int setB(int old, int v){ return (old & 0xFFFFFF00) | (v & 0xFF); }
    protected static int setH(int old, int v){ return (old & 0xFFFF0000) | (v & 0xFFFF); }
    protected static int setBits(int old, int v, int shift, int bits){
        int m=(bits>=32 ? -1 : ((1<<bits)-1)) << shift;
        return (old & ~m) | ((v << shift) & m);
    }
    protected static boolean ltu(int a, int b){ return (a ^ 0x80000000) <  (b ^ 0x80000000); }
    protected static boolean leu(int a, int b){ return (a ^ 0x80000000) <= (b ^ 0x80000000); }
    protected static boolean gtu(int a, int b){ return (a ^ 0x80000000) >  (b ^ 0x80000000); }
    protected static boolean geu(int a, int b){ return (a ^ 0x80000000) >= (b ^ 0x80000000); }
    protected static int div(int a, int b){ return b==0? 0 : a/b; }
    protected static int divu(int a, int b){ return b==0? 0 : (int)((a & 0xFFFFFFFFL)/(b & 0xFFFFFFFFL)); }

    // ------------------------------------------------------------------ control
    /** SLEEP: yield to other tasks -- give the phone's UI thread a chance */
    protected final void sleep(){ Thread.yield(); }
    protected final RuntimeException killTask(){ throw new Terminated("KILLTASK"); }
    protected final RuntimeException terminated(){ throw new Terminated("vTerminateVMGP"); }
    protected final RuntimeException unreachable(int pc){
        return new IllegalStateException("impossible path 0x"+Integer.toHexString(pc));
    }
    protected final RuntimeException badPointer(int fn){
        return new IllegalStateException("bad function pointer 0x"+Integer.toHexString(fn));
    }

    // ------------------------------------------------------------------ SDK bridge
    /** SDK function `id` (a wolphun.j2me.Sdk constant) with the argument registers p0..p3 */
    protected final int sys(int id, int a0, int a1, int a2, int a3){
        return sdk.call(id, a0, a1, a2, a3);
    }
}
