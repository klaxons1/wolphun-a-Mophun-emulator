package wolphun.decomp;

import java.io.File;
import java.nio.file.Files;

import wolphun.vm.Pip2;
import wolphun.vm.Vmgp;

/**
 * A loaded, decrypted and relocated VMGP executable, as seen by the decompiler.
 *
 * The memory layout is produced by the emulator's own loader (wolphun.vm.Pip2), so the
 * addresses printed by the decompiler are exactly the ones the interpreter and the runtime of
 * the decompiled code use:  code @0x10000 | data | bss | heap | stack  (each 16-byte aligned).
 */
final class Image {
    final String name;
    final byte[] raw;           // the (decrypted) file
    final boolean wasEncrypted;
    final Vmgp exe;
    final Pip2 vm;              // only used as a relocated memory image + resolved pool
    final int codeBase, codeEnd, dataBase, dataEnd, bssBase, bssEnd;

    private Image(String name, byte[] raw, boolean enc){
        this.name=name; this.raw=raw; this.wasEncrypted=enc;
        this.exe = new Vmgp(raw);
        int heap = (int)Math.max(4L*1024*1024, exe.heapBytesRequested());
        this.vm  = new Pip2(exe, heap, 256*1024);
        codeBase = vm.codeBase;  codeEnd = vm.codeBase + exe.codeSize;
        dataBase = vm.dataBase;  dataEnd = vm.dataBase + exe.dataSize;
        bssBase  = vm.bssBase;   bssEnd  = vm.bssBase + exe.bssSize;
    }

    static Image load(File f) throws Exception {
        byte[] d = Files.readAllBytes(f.toPath());
        boolean enc = false;
        if(mophun.GameLibrary.looksEncrypted(f)){   // same heuristic as the emulator
            byte[] dec = mophun.MophunCrypt.decrypt(d);
            if(dec==null) throw new IllegalArgumentException("the code section is encrypted and no embedded key matches (not a Sony Ericsson title?)");
            d = dec; enc = true;
        }
        return new Image(f.getName(), d, enc);
    }

    boolean inCode(int a){ return a>=codeBase && a<codeEnd; }
    boolean inData(int a){ return a>=dataBase && a<dataEnd; }
    boolean inBss(int a){ return a>=bssBase && a<bssEnd; }
    int read32(int a){ return vm.read32(a); }
    int read8(int a){ return vm.read8(a); }

    int poolCount(){ return exe.poolCount; }
    Vmgp.PoolItem pool(int i){ return exe.pool[i]; }
    int poolValue(int i){ return (int)vm.poolValue[i]; }
    boolean poolIsImport(int i){ return vm.poolIsImport[i]; }
    String poolName(int i){ return vm.poolName[i]; }
    String symbol(int i){ Vmgp.PoolItem p=exe.pool[i]; return (p.type==2||p.type==3)? exe.getString(p.meta) : null; }

    /** printable NUL-terminated string at a (8-bit or UTF-16LE), or null */
    String stringAt(int a){
        if(!(inData(a))) return null;
        StringBuilder sb=new StringBuilder();
        // 8-bit
        for(int k=0;k<120;k++){
            int c=read8(a+k);
            if(c==0) break;
            if(c<32 || c>126){ if(c=='\n'){ sb.append("\\n"); continue; } sb.setLength(0); break; }
            if(c=='"'||c=='\\') sb.append('\\');
            sb.append((char)c);
            if(k==119) sb.append("...");
        }
        if(sb.length()>=2) return "\""+sb+"\"";
        // UTF-16LE (the ...U text functions)
        sb.setLength(0);
        for(int k=0;k<120;k++){
            int c=read8(a+2*k)|(read8(a+2*k+1)<<8);
            if(c==0) break;
            if(c<32 || c>126){ if(c<0xA0 || (c>=0xD800 && c<0xE000)){ sb.setLength(0); break; } sb.append((char)c); continue; }
            if(c=='"'||c=='\\') sb.append('\\');
            sb.append((char)c);
        }
        if(sb.length()>=2) return "u\""+sb+"\"";
        return null;
    }
}
