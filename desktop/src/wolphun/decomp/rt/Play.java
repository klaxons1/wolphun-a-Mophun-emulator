package wolphun.decomp.rt;

import java.awt.GraphicsEnvironment;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Properties;

import javax.swing.JOptionPane;

import wolphun.Screen;
import wolphun.vm.Pip2;
import wolphun.vm.Sdk;
import wolphun.vm.Vmgp;

/**
 * Entry point of a standalone "playable jar": a decompiled game class plus the emulator's SDK.
 *
 * The jar carries {@code /play.properties}:
 * <pre>
 *   class=HoneyCave2                 decompiled class (extends Pip2Runtime)
 *   title=Honey Cave 2               window title
 *   rom=/game.mpn                    ROM bundled in the jar (data section + resources are read from it)
 *   width=128  height=160  scale=4   screen
 * </pre>
 * {@code java -jar game.jar [other.mpn] [--scale=N] [--mute]}: a ROM given on the command line
 * replaces the bundled one (it must be the same game). Saves go to a "saves" folder next to the
 * jar, or to ~/.wolphun/saves/&lt;class&gt; if that folder is not writable.
 */
public final class Play {
    public static void main(String[] args){
        try {
            run(args);
        } catch(Throwable t){
            t.printStackTrace();
            if(!GraphicsEnvironment.isHeadless())
                JOptionPane.showMessageDialog(null, t.toString(), "wolphun", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    static void run(String[] args) throws Exception {
        Properties p=new Properties();
        InputStream pin=Play.class.getResourceAsStream("/play.properties");
        if(pin==null) throw new IllegalStateException("play.properties missing from the jar");
        try { p.load(pin); } finally { pin.close(); }
        String cls=p.getProperty("class");
        String title=p.getProperty("title", cls);
        int w=Integer.parseInt(p.getProperty("width", "128"));
        int h=Integer.parseInt(p.getProperty("height", "160"));
        int scale=Integer.parseInt(p.getProperty("scale", "4"));
        boolean mute=false, headless=false;
        int frames=0; String snap=null, keys=null;     // headless self-test: --headless --frames=N --keys=... --snap=f.png
        String romPath=null;
        for(String a : args){
            if(a.startsWith("--scale=")) scale=Integer.parseInt(a.substring(8));
            else if(a.equals("--mute")) mute=true;
            else if(a.equals("--headless")){ headless=true; mute=true; }
            else if(a.startsWith("--frames=")) frames=Integer.parseInt(a.substring(9));
            else if(a.startsWith("--snap=")) snap=a.substring(7);
            else if(a.startsWith("--keys=")) keys=a.substring(7);
            else if(!a.startsWith("--")) romPath=a;
        }

        byte[] raw;
        if(romPath!=null) raw=Files.readAllBytes(new File(romPath).toPath());
        else {
            InputStream in=Play.class.getResourceAsStream(p.getProperty("rom", "/game.mpn"));
            if(in==null) throw new IllegalStateException("no ROM bundled in the jar; pass the .mpn file as an argument");
            raw=readAll(in);
        }
        if(looksEncrypted(raw)){
            byte[] dec=mophun.MophunCrypt.decrypt(raw);
            if(dec==null) throw new IllegalArgumentException("the ROM is encrypted and no known key matches");
            raw=dec;
        }

        Vmgp exe=new Vmgp(raw);
        final Screen scr=new Screen(w, h, scale, headless, false, 1);
        URL icon=Play.class.getResource("/wolphun.png");
        scr.setWindowTitle(title+"  -  wolphun (decompiled to Java)",
                icon!=null? java.awt.Toolkit.getDefaultToolkit().getImage(icon) : null);
        int heap=(int)Math.max(4L*1024*1024, exe.heapBytesRequested());
        Pip2 vm=new Pip2(exe, heap, 256*1024);
        Sdk sdk=new Sdk(vm, scr);
        if(mute) sdk.soundEnabled=false;
        File base=jarDir();
        sdk.romDir=base;
        sdk.saveDir=saveDir(base, cls);
        vm.sdk=sdk;

        System.out.println(title+" -- arrows/WASD move, Space/Enter fire, Q left softkey, E right softkey");
        System.out.println("saves: "+sdk.saveDir.getAbsolutePath());
        Pip2Runtime game=(Pip2Runtime)Class.forName(cls).newInstance();
        game.attach(vm, sdk);
        game.realTime=!headless;
        if(headless){
            final RunDecompiled.KeyScript ks=new RunDecompiled.KeyScript(keys);
            final int max=frames;
            game.beforeSys=new java.util.function.Consumer<String>(){ public void accept(String n){ ks.before(scr, n); } };
            game.afterSys=new Runnable(){ public void run(){
                if(max>0 && scr.flips>=max) throw new Pip2Runtime.Terminated("frame limit");
            }};
        }
        long t0=System.currentTimeMillis();
        game.run();
        if(headless) System.out.println(scr.flips+" frames, "+game.sysCalls+" SDK calls, "+(System.currentTimeMillis()-t0)+" ms");
        if(snap!=null) scr.savePng(snap);
        System.exit(0);
    }

    /** the same test as mophun.GameLibrary.looksEncrypted, on bytes: XTEA output uses almost every byte value */
    static boolean looksEncrypted(byte[] d){
        if(d.length<44 || d[0]!='V'||d[1]!='M'||d[2]!='G'||d[3]!='P') return false;
        int codeSize=(d[12]&0xff)|((d[13]&0xff)<<8)|((d[14]&0xff)<<16)|((d[15]&0xff)<<24);
        int len=Math.min(Math.min(codeSize, 4096), d.length-40);
        if(len<=64) return false;
        boolean[] seen=new boolean[256]; int distinct=0;
        for(int i=0;i<len;i++){ int b=d[40+i]&0xff; if(!seen[b]){ seen[b]=true; distinct++; } }
        return distinct>190;
    }

    static byte[] readAll(InputStream in) throws java.io.IOException {
        try {
            ByteArrayOutputStream bo=new ByteArrayOutputStream();
            byte[] buf=new byte[16384]; int n;
            while((n=in.read(buf))>0) bo.write(buf, 0, n);
            return bo.toByteArray();
        } finally { in.close(); }
    }

    static File jarDir(){
        try {
            File f=new File(Play.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return f.isFile()? f.getParentFile() : f;
        } catch(Exception e){ return new File("."); }
    }

    static File saveDir(File base, String cls){
        File d=new File(base, "saves");
        d.mkdirs();
        if(d.isDirectory() && d.canWrite()) return d;
        d=new File(new File(System.getProperty("user.home"), ".wolphun"), "saves"+File.separator+cls);
        d.mkdirs();
        return d;
    }
}
