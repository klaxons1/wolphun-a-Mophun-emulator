package wolphun.decomp.rt;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

import wolphun.Screen;
import wolphun.vm.Pip2;
import wolphun.vm.Sdk;
import wolphun.vm.Vmgp;

/**
 * Runs a decompiled game class -- or, with --interp, the same ROM in the PIP2 interpreter --
 * with the emulator's screen and SDK. Both modes can log every SDK call and a checksum of every
 * frame; that is how the decompiler output is checked against the original bytecode:
 *
 * <pre>
 *   java -cp out:gen wolphun.decomp.rt.RunDecompiled HoneyCave2 game.mpn                (play in a window)
 *   VTICK=20 java ... RunDecompiled HoneyCave2 game.mpn --headless --frames=600 --keys=... --hash --trace=dec.txt
 *   VTICK=20 java ... RunDecompiled --interp   game.mpn --headless --frames=600 --keys=... --hash --trace=int.txt
 * </pre>
 * VTICK makes vGetTickCount deterministic. --keys scripts the keypad clocked by the number of
 * vGetButtonData calls (not by frames: menus often poll the keys without flipping the screen):
 * "P:bits[:dur]" holds bits during polls [P, P+dur), "every:N:bits[:dur]" pulses them every N
 * polls. Bits: UP=1 DOWN=2 LEFT=4 RIGHT=8 FIRE=0x10 SELECT=0x20. The two logs must be identical;
 * give each run its own empty --saves directory, since games load and write save files.
 */
public final class RunDecompiled {
    public static void main(String[] args) throws Exception {
        if(args.length<2){
            System.out.println("usage: RunDecompiled <DecompiledClass | --interp> <game.mpn> [--headless] [--frames=N]");
            System.out.println("         [--keys=P:bits[:dur],every:N:bits[:dur],...] [--hash] [--trace=file] [--snap=file.png]");
            System.out.println("         [--saves=dir] [--w=128 --h=160 --scale=4]");
            return;
        }
        String cls=args[0], mpn=args[1];
        boolean interp = cls.equals("--interp");
        boolean headless=false, hash=false;
        int frames=0, w=128, h=160, scale=4;
        String trace=null, snap=null, keys=null, saves=null;
        for(int i=2;i<args.length;i++){
            String a=args[i];
            if(a.equals("--headless")) headless=true;
            else if(a.equals("--hash")) hash=true;
            else if(a.startsWith("--frames=")) frames=Integer.parseInt(a.substring(9));
            else if(a.startsWith("--trace=")) trace=a.substring(8);
            else if(a.startsWith("--snap=")) snap=a.substring(7);
            else if(a.startsWith("--keys=")) keys=a.substring(7);
            else if(a.startsWith("--saves=")) saves=a.substring(8);
            else if(a.startsWith("--w=")) w=Integer.parseInt(a.substring(4));
            else if(a.startsWith("--h=")) h=Integer.parseInt(a.substring(4));
            else if(a.startsWith("--scale=")) scale=Integer.parseInt(a.substring(8));
            else { System.err.println("unknown option "+a); return; }
        }

        File rom=new File(mpn);
        byte[] raw=Files.readAllBytes(rom.toPath());
        if(mophun.GameLibrary.looksEncrypted(rom)){
            byte[] dec=mophun.MophunCrypt.decrypt(raw);
            if(dec==null) throw new IllegalArgumentException("encrypted ROM without a matching key");
            raw=dec;
        }
        Vmgp exe=new Vmgp(raw);
        final Screen scr=new Screen(w, h, scale, headless, false, 1);
        int heap=(int)Math.max(4L*1024*1024, exe.heapBytesRequested());
        final Pip2 vm=new Pip2(exe, heap, 256*1024);
        final Sdk sdk=new Sdk(vm, scr);
        if(headless) sdk.soundEnabled=false;
        File dir=rom.getAbsoluteFile().getParentFile();
        sdk.romDir=dir;
        sdk.saveDir = saves!=null? new File(saves) : new File(dir, "saves");
        vm.sdk=sdk;

        final SdkTrace tr = trace!=null? new SdkTrace(trace) : null;
        final KeyScript ks = new KeyScript(keys);
        final int maxFrames=frames;
        final boolean doHash=hash;
        final int[] lastFlip={0};
        final CRC32 crc=new CRC32();
        final Runnable onFrame=new Runnable(){ public void run(){
            if(scr.flips==lastFlip[0]) return;
            lastFlip[0]=scr.flips;
            if(doHash){
                crc.reset();
                for(int px : scr.fb){ crc.update(px); crc.update(px>>8); crc.update(px>>16); }
                System.out.printf("frame %d %08x%n", scr.flips, crc.getValue());
            }
        }};
        long t0=System.currentTimeMillis();
        if(interp){
            vm.runConstructors();
            while(!vm.stopped){
                String imp = importAt(vm, vm.getReg(Pip2.PC));
                if(imp!=null){ ks.before(scr, imp); if(tr!=null) tr.before(vm, imp); }
                vm.step();
                if(imp!=null && tr!=null) tr.after(vm, imp);
                onFrame.run();
                if(maxFrames>0 && scr.flips>=maxFrames) break;
            }
            System.out.println("interpreter: "+vm.instr+" instructions, "+scr.flips+" frames, "+(System.currentTimeMillis()-t0)+" ms");
        } else {
            final Pip2Runtime game=(Pip2Runtime)Class.forName(cls).newInstance();
            game.attach(vm, sdk);
            game.tracer=tr;
            game.beforeSys=new java.util.function.Consumer<String>(){ public void accept(String n){ ks.before(scr, n); } };
            game.afterSys=new Runnable(){ public void run(){
                onFrame.run();
                if(maxFrames>0 && scr.flips>=maxFrames) throw new Pip2Runtime.Terminated("frame limit");
            }};
            game.run();
            System.out.println("decompiled: "+game.sysCalls+" SDK calls, "+scr.flips+" frames, "+(System.currentTimeMillis()-t0)+" ms");
        }
        if(tr!=null) tr.close();
        if(snap!=null) scr.savePng(snap);
        if(!headless) System.exit(0);
    }

    /** import called by the instruction at pc (a CALLl through an Import pool item), else null */
    static String importAt(Pip2 vm, int pc){
        int w=vm.read32(pc);
        if((w&0xff)!=0x5C) return null;
        int info=vm.read32(pc+4);
        if((info&0x80000000)!=0) return null;
        int idx=info-1;
        if(idx<0 || idx>=vm.poolValue.length || !vm.poolIsImport[idx]) return null;
        return vm.poolName[idx];
    }

    /** keypad script clocked by vGetButtonData calls */
    static final class KeyScript {
        final List<int[]> items=new ArrayList<int[]>();   // {start, bits, dur, period}
        long polls;
        KeyScript(String spec){
            if(spec==null) return;
            for(String part : spec.split(",")){
                String[] t=part.trim().split(":");
                if(t[0].equalsIgnoreCase("every"))
                    items.add(new int[]{0, Integer.decode(t[2]), t.length>3? Integer.decode(t[3]) : 3, Integer.decode(t[1])});
                else
                    items.add(new int[]{Integer.decode(t[0]), Integer.decode(t[1]), t.length>2? Integer.decode(t[2]) : 3, 0});
            }
        }
        void before(Screen scr, String name){
            if(items.isEmpty() || !name.equals("vGetButtonData")) return;
            int b=0;
            for(int[] k : items){
                if(k[3]>0){ if(polls % k[3] < k[2]) b|=k[1]; }
                else if(polls>=k[0] && polls<k[0]+k[2]) b|=k[1];
            }
            scr.clearButtons();
            if(b!=0) scr.setButtonBit(b, true);
            polls++;
        }
    }
}
