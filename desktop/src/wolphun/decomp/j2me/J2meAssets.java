package wolphun.decomp.j2me;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import wolphun.Screen;
import wolphun.decomp.ImportSigs;
import wolphun.vm.Pip2;
import wolphun.vm.Vmgp;

/**
 * Build step of the J2ME (MIDP 2 / CLDC 1.1) port of a decompiled game.
 *
 * Everything that is cheap on the desktop but expensive or impossible on a phone KVM is done here,
 * once, at build time:
 * <ul>
 *   <li><b>g.img</b> - the game's initial address space exactly as wolphun.vm.Pip2 lays it out
 *       (code + data relocated + bss) plus the layout constants (stack top, heap range, constructor
 *       list) and the resources of the container. The phone only copies bytes, it never parses
 *       the .mpn nor decrypts it;</li>
 *   <li><b>f.fnt</b> - the "system font" used by vTextOut/vTextOutU/vTextExtent, rendered with the
 *       very same code path as the desktop Screen (so text layout matches), stored as 1-bit glyphs;</li>
 *   <li><b>gen/wolphun/game/&lt;Class&gt;.java</b> - the decompiled class made Java 1.3 / CLDC clean: annotations
 *       removed and every SDK call {@code sys("vName", a, b, c, d)} turned into
 *       {@code sys(Sdk.vName, ...)} with the byte/short argument narrowing of the prototype baked in
 *       (no string switch at run time);</li>
 *   <li><b>gen/wolphun/j2me/GameInfo.java</b> - factory + title.</li>
 * </ul>
 *
 * usage: J2meAssets rom.mpn ClassName decompiled.java j2meSdk.java outDir "Title" [stackKB] [heapKB]
 */
public final class J2meAssets {

    public static void main(String[] a) throws Exception {
        if(a.length<6){
            System.err.println("usage: J2meAssets rom.mpn ClassName decompiled.java j2me/src/wolphun/j2me/Sdk.java outDir \"Title\" [stackKB] [heapKB]");
            System.exit(2);
        }
        File rom=new File(a[0]); String cls=a[1]; File src=new File(a[2]); File sdkSrc=new File(a[3]);
        File out=new File(a[4]); String title=a[5];
        int stackKB = a.length>6 ? Integer.parseInt(a[6]) : 16;   // HC2 needs ~0.3 KB
        int heapKB  = a.length>7 ? Integer.parseInt(a[7]) : -1;

        byte[] raw=Files.readAllBytes(rom.toPath());
        if(looksEncrypted(raw)){
            byte[] dec=mophun.MophunCrypt.decrypt(raw);
            if(dec==null) throw new IllegalArgumentException("the ROM is encrypted and no known key matches");
            raw=dec;
        }
        Vmgp exe=new Vmgp(raw);
        File res=new File(out, "res"); res.mkdirs();
        File gen=new File(out, "gen"); new File(gen, "wolphun/j2me").mkdirs(); new File(gen, "wolphun/game").mkdirs();

        int heap = heapKB>=0 ? heapKB*1024 : (int)Math.max(16*1024, Math.min(exe.heapBytesRequested(), 1024*1024));
        writeImage(exe, heap, stackKB*1024, new File(res, "g.img"));
        writeFont(new File(res, "f.fnt"));
        Set<String> used=preprocess(src, cls, sdkSrc, new File(gen, "wolphun/game/"+cls+".java"));
        writeGameInfo(cls, title, new File(gen, "wolphun/j2me/GameInfo.java"));
        System.out.println("J2ME assets: heap "+heap/1024+" KB, stack "+stackKB+" KB, "+used.size()+" SDK imports");
    }

    // ------------------------------------------------------------------ memory image
    static void writeImage(Vmgp exe, int heap, int stack, File f) throws IOException {
        Pip2 vm=new Pip2(exe, heap, stack);
        // constructor list ("~C" global), 0 = none (plain C games such as HoneyCave 2)
        int ctors=0;
        for(int i=0;i<exe.poolCount;i++){
            Vmgp.PoolItem it=exe.pool[i];
            if(it.type==3 && "~C".equals(exe.getString(it.meta))){ ctors=(int)vm.poolValue[i]; break; }
        }
        int init=vm.heapBase-vm.base;                   // code + data + bss; heap and stack start zeroed
        while(init>0 && vm.mem[init-1]==0) init--;      // trailing bss zeros are implicit
        DataOutputStream o=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
        try {
            o.writeInt(0x574A324D);                     // "WJ2M"
            o.writeInt(1);                              // version
            o.writeInt(vm.base);
            o.writeInt(vm.mem.length);
            o.writeInt(vm.getReg(Pip2.SP));             // initial sp = stack top
            o.writeInt(vm.heapBase);
            o.writeInt(vm.heapEnd);
            o.writeInt(ctors);
            o.writeInt(fmt7Transp(exe) ? 1 : 0);
            o.writeInt(init);
            o.write(vm.mem, 0, init);
            int n=exe.resourceCount();
            o.writeInt(n);
            for(int i=0;i<n;i++){
                int off=exe.resourceOffset(i), len=exe.resourceLength(i);
                o.writeInt(len);
                o.write(exe.all, off, len);
            }
        } finally { o.close(); }
    }

    /** same rule as wolphun.vm.Sdk.detectaFmt7Transp (HoneyCave 2: RGB332 index 0 is a colour key) */
    static boolean fmt7Transp(Vmgp exe){
        return contains(exe.all, "Level1.mec") && contains(exe.all, "Level4.mec");
    }
    static boolean contains(byte[] d, String s){
        byte[] n=s.getBytes();
        outer: for(int i=0;i+n.length<=d.length;i++){
            for(int j=0;j<n.length;j++) if(d[i+j]!=n[j]) continue outer;
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ system font
    static final int FIRST=32, LAST=0x17F;
    /** font heights (pixels) shipped: the presets 0/1/2 (8/10/12), the desktop default (11) and a few more */
    static final int[] SIZES={8, 10, 11, 12, 14, 16};
    /**
     * Renders the desktop system font (Screen.selectFont + Screen.drawText, the code path the desktop
     * runtime uses for vTextOut/vTextOutU) at several heights and stores each glyph 1-bit
     * (box = textWidth+2 x height+2). The phone picks the table vSelectFont asks for.
     */
    static void writeFont(File f) throws IOException {
        Screen scr=new Screen(64, 64, 1, true, false, 1);
        scr.setClip(0, 0, 63, 63);
        DataOutputStream o=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
        try {
            o.writeInt(0x574A4632);                     // "WJF2"
            o.writeByte(SIZES.length);
            for(int px : SIZES){
                scr.selectFont(0x80000000|px, 0);       // FONT_SIZE_PIXELS(px)
                int fh=scr.fontHeight(), bh=Math.min(fh+2, 64);
                o.writeByte(px); o.writeByte(fh); o.writeByte(bh);
                o.writeShort(FIRST); o.writeShort(LAST);
                for(int c=FIRST;c<=LAST;c++){
                    String s=String.valueOf((char)c);
                    boolean can=scr.textWidth(s)>0 && (c<0x7F || c>=0xA0);
                    int adv = can ? scr.textWidth(s) : 0;
                    int bw = can ? Math.min(adv+2, 64) : 0;
                    Arrays.fill(scr.fb, 0);
                    if(can) scr.drawText(0, 0, s, 0xffffffff);
                    o.writeByte(adv); o.writeByte(bw);
                    int bits=0, nb=0;
                    for(int y=0;y<bh;y++) for(int x=0;x<bw;x++){
                        if((scr.fb[y*64+x]&0xffffff)!=0) bits|=1<<nb;
                        if(++nb==8){ o.writeByte(bits); bits=0; nb=0; }
                    }
                    if(nb>0) o.writeByte(bits);
                }
            }
        } finally { o.close(); }
    }

    // ------------------------------------------------------------------ source
    static final Pattern SYS = Pattern.compile("sys\\(\"(\\w+)\", ([^,()]+), ([^,()]+), ([^,()]+), ([^,()]+)\\)");
    static final Pattern CONST = Pattern.compile("public static final int (v\\w+)\\s*=\\s*\\d+");

    static Set<String> preprocess(File src, String cls, File sdkSrc, File dst) throws IOException {
        String s=new String(Files.readAllBytes(src.toPath()), "UTF-8");
        Set<String> have=new HashSet<String>();
        Matcher cm=CONST.matcher(new String(Files.readAllBytes(sdkSrc.toPath()), "UTF-8"));
        while(cm.find()) have.add(cm.group(1));

        s=s.replace("@SuppressWarnings(\"unused\")\n", "").replace("@SuppressWarnings(\"unused\")\r\n", "");
        s=s.replace("@Override ", "");
        Set<String> used=new TreeSet<String>(), missing=new TreeSet<String>();
        StringBuffer sb=new StringBuffer();
        Matcher m=SYS.matcher(s);
        while(m.find()){
            String name=m.group(1); used.add(name);
            String id = have.contains(name) ? "wolphun.j2me.Sdk."+name : "wolphun.j2me.Sdk.UNIMPLEMENTED";
            if(!have.contains(name)) missing.add(name);
            StringBuilder call=new StringBuilder("sys(").append(id);
            for(int k=0;k<4;k++) call.append(", ").append(narrow(name, k, m.group(2+k).trim()));
            call.append(")");
            m.appendReplacement(sb, Matcher.quoteReplacement(call.toString()));
        }
        m.appendTail(sb);
        s=sb.toString();
        if(s.contains("sys(\"")) throw new IllegalStateException("an SDK call was not rewritten (unexpected decompiler output)");
        if(!missing.isEmpty()) System.err.println("warning: SDK functions without a J2ME implementation (they return 0): "+missing);
        // the decompiled class lives in the default package; the MIDlet needs to reach it by name
        if(!s.contains("\nimport wolphun.decomp.rt.Pip2Runtime;")) throw new IllegalStateException("unexpected decompiler output (no runtime import)");
        s=s.replace("\nimport wolphun.decomp.rt.Pip2Runtime;", "\npackage wolphun.game;\n\nimport wolphun.decomp.rt.Pip2Runtime;");
        s="// GENERATED by wolphun.decomp.j2me.J2meAssets from "+src.getName()+" -- do not edit\n"+s;
        Writer w=new OutputStreamWriter(new FileOutputStream(dst), "UTF-8");
        try { w.write(s); } finally { w.close(); }
        return used;
    }

    /** bakes the prototype's byte/short narrowing (ImportSigs.normArg) into the call site */
    static String narrow(String name, int k, String arg){
        if(arg.equals("0")) return arg;
        int t=ImportSigs.normArg(name, k, 0x1FFFF);
        if(t==0xFF) return "("+arg+") & 0xFF";
        if(t==-1) return "(short)("+arg+")";
        if(t==0xFFFF) return "("+arg+") & 0xFFFF";
        return arg;
    }

    static void writeGameInfo(String cls, String title, File f) throws IOException {
        String t=title.replace("\\", "\\\\").replace("\"", "\\\"");
        String s="// GENERATED by wolphun.decomp.j2me.J2meAssets -- do not edit\n"
            +"package wolphun.j2me;\n\n"
            +"final class GameInfo {\n"
            +"    static final String TITLE = \""+t+"\";\n"
            +"    static wolphun.decomp.rt.Pip2Runtime create(){ return new wolphun.game."+cls+"(); }\n"
            +"}\n";
        Writer w=new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
        try { w.write(s); } finally { w.close(); }
    }

    /** the same test as mophun.GameLibrary.looksEncrypted */
    static boolean looksEncrypted(byte[] d){
        if(d.length<44 || d[0]!='V'||d[1]!='M'||d[2]!='G'||d[3]!='P') return false;
        int codeSize=(d[12]&0xff)|((d[13]&0xff)<<8)|((d[14]&0xff)<<16)|((d[15]&0xff)<<24);
        int len=Math.min(Math.min(codeSize, 4096), d.length-40);
        if(len<=64) return false;
        boolean[] seen=new boolean[256]; int distinct=0;
        for(int i=0;i<len;i++){ int b=d[40+i]&0xff; if(!seen[b]){ seen[b]=true; distinct++; } }
        return distinct>190;
    }
}
