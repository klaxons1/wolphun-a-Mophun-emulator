package wolphun.decomp.rt;

import java.io.PrintWriter;

import wolphun.decomp.ImportSigs;
import wolphun.vm.Pip2;

/**
 * Log of SDK calls ("name(args) -> result"), written identically by the interpreter and by
 * decompiled code so the two can be diffed. Arguments are shown as the SDK sees them (byte and
 * short parameters truncated); runs of identical lines are folded into "... (xN)".
 */
public final class SdkTrace {
    private final PrintWriter out;
    private String last; private long repeat;
    private final StringBuilder cur = new StringBuilder();
    public long calls;

    public SdkTrace(String file) throws java.io.IOException { out = new PrintWriter(file, "UTF-8"); }

    /** call this with the arguments in p0..p3, before the SDK function runs */
    public void before(Pip2 vm, String name){
        cur.setLength(0);
        cur.append(name).append('(');
        int n = Pip2Runtime.regArgs(name);
        for(int k=0;k<n;k++){
            if(k>0) cur.append(", ");
            cur.append(ImportSigs.normArg(name, k, vm.getReg(Pip2.P0+4*k)));
        }
        cur.append(')');
    }
    /** call this after the SDK function returned */
    public void after(Pip2 vm, String name){
        calls++;
        if(ImportSigs.returns(name)) cur.append(" -> ").append(vm.getReg(Pip2.R0));
        String line = cur.toString();
        if(line.equals(last)){ repeat++; return; }
        flushRepeat();
        out.println(line);
        last = line;
    }
    private void flushRepeat(){
        if(repeat>0){ out.println("    ... previous line repeated "+repeat+" more times"); repeat=0; }
    }
    public void close(){ flushRepeat(); out.close(); }
}
