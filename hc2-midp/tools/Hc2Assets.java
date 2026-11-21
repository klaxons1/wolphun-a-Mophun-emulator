import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

import javax.imageio.ImageIO;

import wolphun.vm.Pip2;
import wolphun.vm.Vmgp;

/**
 * Build-time asset converter of the native MIDP 2.0 port of Honey Cave 2.
 *
 * Reads the original mophun container (HoneyCave2*.mpn, encrypted or not) and writes the game data
 * in formats a MIDP 2.0 phone handles natively, so the phone never sees a mophun structure:
 * <ul>
 *   <li>s.png: every shared sprite (VMGP NativeSprite, RGB332, colour 0 = transparent) in one
 *       atlas; b0..b3.png: the boss sprites of each level (they live in the level resources);</li>
 *   <li>t0.png / t1.png / t2.png: level, menu and HUD 8x8 tile sets for TiledLayer. Only the
 *       tile variants the maps use (opaque, or with colour 0 transparent), identical ones merged;</li>
 *   <li>map0..map6.bin: menu, the four levels, wide / small HUD: cells already as TiledLayer
 *       indices (128+j = animated sequence j), the animated sequences, collision attributes
 *       (levels only) and the objects. One file per map, so the game only loads what it needs;</li>
 *   <li>f.png: the 2-bit game font (score / lives digits) in its two palettes;</li>
 *   <li>m0..m7.mid: the sound effects (standard MIDI files embedded in the data section);</li>
 *   <li>d.bin: sprite table, sky colours, boss path, font map and the English texts of the
 *       language resource.</li>
 * </ul>
 * The layout of d.bin is read by hc2.Res (keep both in sync).
 *
 * usage: Hc2Assets game.mpn outDir
 */
public final class Hc2Assets {
    static byte[] all; static Vmgp exe; static Pip2 vm;

    // data section addresses of the original executable
    static final int SPR_TABLE = 0x184bc, SPR_TABLE_N = 84, SHARED_N = 61;
    static final int RES17_BASE = 0x19130, LEVEL_RES_BASE = 0x1bef4;
    static final int HOURGLASS = 0x18cc4, PALETTE = 0x18674, SKY = 0x186c4, PATH = 0x18c06;
    static final int[][] MIDI = { {0x17ea0,163}, {0x17f43,80}, {0x17f93,84}, {0x17fe7,81},
                                  {0x18038,132}, {0x180bc,288}, {0x181dc,148}, {0x18270,192} };
    /** which table entries (61..83) hold the boss sprites of each level */
    static final int[][] BOSS_RANGE = { {61,66}, {67,74}, {75,80}, {81,83} };

    static final int[] RGB332 = new int[256], PAL = new int[256];

    public static void main(String[] a) throws Exception {
        if(a.length<2){ System.err.println("usage: Hc2Assets game.mpn outDir"); System.exit(2); }
        byte[] raw=Files.readAllBytes(new File(a[0]).toPath());
        if(looksEncrypted(raw)){
            byte[] dec=mophun.MophunCrypt.decrypt(raw);
            if(dec==null) throw new IllegalArgumentException("the ROM is encrypted and no known key matches");
            raw=dec;
        }
        all=raw; exe=new Vmgp(raw); vm=new Pip2(exe, 4096, 4096);
        File out=new File(a[1]); out.mkdirs();

        for(int i=0;i<256;i++){
            int r=(i>>5)&7, g=(i>>2)&7, b=i&3;
            RGB332[i]=0xff000000|(((r*510+7)/14)<<16)|(((g*510+7)/14)<<8)|((b*510+3)/6);
            PAL[i]=RGB332[i];
        }
        for(int i=0;i<40;i++) PAL[i+1]=rgb555(vm.read16(PALETTE+i*2));

        ByteArrayOutputStream bo=new ByteArrayOutputStream();
        DataOutputStream d=new DataOutputStream(bo);
        d.writeInt(0x48433244);                                  // "HC2D"

        // ---------------------------------------------------------------- sprites
        byte[] r17=res(17);
        List<Spr> sprites=new ArrayList<Spr>();
        Map<String,Integer> ids=new HashMap<String,Integer>();
        int[] tab=new int[SHARED_N];
        List<Spr> shared=new ArrayList<Spr>();
        for(int k=0;k<SHARED_N;k++){
            int p=vm.read32(SPR_TABLE+k*4);
            if(p==0){ tab[k]=0; continue; }
            String key="s"+p;
            Integer id=ids.get(key);
            if(id==null){
                Spr s=decodeSprite(r17, p-RES17_BASE); s.atlas=0;
                sprites.add(s); shared.add(s); id=sprites.size(); ids.put(key, id);
            }
            tab[k]=id;
        }
        // loading-screen hourglass (data section)
        byte[] dataImg=new byte[0x80];
        for(int i=0;i<dataImg.length;i++) dataImg[i]=(byte)vm.read8(HOURGLASS+i);
        Spr hg=decodeSprite(dataImg, 0); hg.atlas=0; sprites.add(hg); shared.add(hg);
        int hourglassId=sprites.size();
        pack(shared, new File(out, "s.png"));

        int[][] btab=new int[4][SPR_TABLE_N-SHARED_N];
        for(int L=0;L<4;L++){
            byte[] lr=res(L);
            List<Spr> bs=new ArrayList<Spr>();
            for(int k=BOSS_RANGE[L][0];k<=BOSS_RANGE[L][1];k++){
                int p=vm.read32(SPR_TABLE+k*4);
                String key="b"+L+"_"+p;
                Integer id=ids.get(key);
                if(id==null){
                    Spr s=decodeSprite(lr, p-LEVEL_RES_BASE); s.atlas=1+L;
                    sprites.add(s); bs.add(s); id=sprites.size(); ids.put(key, id);
                }
                btab[L][k-SHARED_N]=id;
            }
            pack(bs, new File(out, "b"+L+".png"));
        }
        d.writeShort(sprites.size());
        for(Spr s: sprites){
            d.writeByte(s.atlas); d.writeShort(s.ax); d.writeShort(s.ay);
            d.writeShort(s.w); d.writeShort(s.h); d.writeShort(s.cx); d.writeShort(s.cy);
        }
        for(int k=0;k<SHARED_N;k++) d.writeShort(tab[k]);
        for(int L=0;L<4;L++) for(int k=0;k<btab[L].length;k++) d.writeShort(btab[L][k]);
        d.writeShort(hourglassId);

        // ---------------------------------------------------------------- tables
        for(int i=0;i<24;i++) d.writeShort(vm.read16(SKY+i*2));
        for(int i=0;i<26*3;i++) d.writeByte(vm.read8(PATH+i));

        // ---------------------------------------------------------------- game font (res16)
        byte[] f16=res(16);
        int nGlyphs=(f16.length-91)/16;
        for(int i=0;i<91;i++) d.writeByte(f16[i]);
        BufferedImage fi=new BufferedImage(nGlyphs*8, 16, BufferedImage.TYPE_INT_ARGB);
        for(int row=0;row<2;row++){
            int pal = row==0 ? 1 : 9;
            for(int gI=0;gI<nGlyphs;gI++) for(int y=0;y<8;y++) for(int x=0;x<8;x++){
                int bit=(y*8+x)*2;
                int v=((f16[91+gI*16+(bit>>3)]&0xff)>>(bit&7))&3;
                fi.setRGB(gI*8+x, row*8+y, v==0 ? 0 : PAL[(pal+v)&0xff]);
            }
        }
        ImageIO.write(fi, "png", new File(out, "f.png"));

        // ---------------------------------------------------------------- tiles + maps
        byte[][] tiles={ res(10), res(11), res(15) };
        int[][] maps={ {12,1}, {4,0}, {5,0}, {6,0}, {7,0}, {13,2}, {14,2} };
        MapData[] md=new MapData[maps.length];
        for(int m=0;m<maps.length;m++) md[m]=readMap(res(maps[m][0]), maps[m][1]);
        int tileSlots=0;
        for(int t=0;t<3;t++) tileSlots+=writeTiles(tiles[t], t, md, new File(out, "t"+t+".png"));
        for(int m=0;m<maps.length;m++) writeMap(md[m], m>=1 && m<=4, new File(out, "map"+m+".bin"));

        // ---------------------------------------------------------------- texts (res9, UTF-16LE)
        byte[] r9=res(9);
        StringBuffer sb=new StringBuffer();
        for(int i=2;i+1<r9.length;i+=2) sb.append((char)((r9[i]&0xff)|((r9[i+1]&0xff)<<8)));
        String[] lines=sb.toString().split("\n", -1);
        int li=0;
        while(li<lines.length && lines[li].startsWith("*")) li++;   // language names
        String[] str=new String[21];
        for(int i=0;i<21;i++) str[i]=strip(lines[li++]);
        StringBuffer help=new StringBuffer();                         // a text ends at "\r\n"
        while(li<lines.length){
            String l=lines[li++];
            boolean end=l.endsWith("\r");
            if(help.length()>0) help.append('\n');
            help.append(strip(l));
            if(end) break;
        }
        d.writeShort(str.length);
        for(int i=0;i<str.length;i++) d.writeUTF(str[i]);
        d.writeUTF(help.toString());
        d.flush();
        Files.write(new File(out, "d.bin").toPath(), bo.toByteArray());

        // ---------------------------------------------------------------- sounds
        for(int i=0;i<MIDI.length;i++){
            byte[] m=new byte[MIDI[i][1]];
            for(int k=0;k<m.length;k++) m[k]=(byte)vm.read8(MIDI[i][0]+k);
            if(m[0]!='M'||m[1]!='T'||m[2]!='h'||m[3]!='d') throw new IOException("MIDI "+i+" missing");
            Files.write(new File(out, "m"+i+".mid").toPath(), m);
        }
        System.out.println("HC2 assets: "+sprites.size()+" sprites, "+nGlyphs+" glyphs, "
                +tileSlots+" tiles, "+str.length+" strings -> "+out);
    }

    static String strip(String s){ return s.endsWith("\r") ? s.substring(0, s.length()-1) : s; }

    // ---------------------------------------------------------------- sprites
    static final class Spr { int atlas, ax, ay, w, h, cx, cy; int[] argb; }

    /** NativeSprite: u8 palOffset, u8 format, s16 centerX, s16 centerY, u16 w, u16 h, pixels */
    static Spr decodeSprite(byte[] b, int o){
        Spr s=new Spr();
        int palOff=b[o]&0xff, fmt=b[o+1]&0xff;
        s.cx=(short)le16(b, o+2); s.cy=(short)le16(b, o+4); s.w=le16(b, o+6); s.h=le16(b, o+8);
        if(s.w<=0 || s.h<=0 || s.w>256 || s.h>256) throw new IllegalStateException("bad sprite at "+o);
        int bpp = fmt==7||fmt==6 ? 8 : fmt==3||fmt==0 ? 1 : fmt==4||fmt==1 ? 2 : fmt==5||fmt==2 ? 4 : -1;
        if(bpp<0) throw new IllegalStateException("sprite format "+fmt);
        s.argb=new int[s.w*s.h];
        int mask=(1<<bpp)-1;
        for(int i=0;i<s.w*s.h;i++){
            int bit=i*bpp;
            int v=((b[o+10+(bit>>3)]&0xff)>>(bit&7))&mask;
            if(v==0){ s.argb[i]=0; continue; }                     // sprites are always drawn transparent
            s.argb[i] = fmt==7 ? RGB332[v] : PAL[(v+(fmt==6?0:palOff))&0xff];
        }
        return s;
    }

    /** simple shelf packer, 128 pixels wide */
    static void pack(List<Spr> list, File f) throws IOException {
        List<Spr> sorted=new ArrayList<Spr>(list);
        Collections.sort(sorted, new Comparator<Spr>(){ public int compare(Spr x, Spr y){ return y.h-x.h; } });
        int W=128, x=0, y=0, rowH=0;
        for(Spr s: sorted){
            if(x+s.w>W){ x=0; y+=rowH; rowH=0; }
            s.ax=x; s.ay=y; x+=s.w; if(s.h>rowH) rowH=s.h;
        }
        int H=Math.max(1, y+rowH);
        BufferedImage im=new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        for(Spr s: sorted) im.setRGB(s.ax, s.ay, s.w, s.h, s.argb, 0, s.w);
        ImageIO.write(im, "png", f);
    }

    // ---------------------------------------------------------------- tiles / maps
    /** layer 0 of a map resource, plus what the TiledLayer needs once the tiles are numbered */
    static final class MapData {
        int set, flag, w, h, anim; byte[] tile, attr; int[] kind, ox, oy;
        int[] cell;                               // TiledLayer cell: slot, 128+j = sequence j, 0
        List<int[]> seqs=new ArrayList<int[]>();  // animated sequences (slots of the frames)
    }

    /**
     * Map resource: 8-byte header (u8 @2 = layer count, s16 @6 = object count), 24-byte layer
     * headers (flag, format, w, h, anim, firstObject, objectCount, ...), 8-byte objects
     * (u32 kind/params, u16 tileX, u16 tileY), then the cell data of every layer (1 byte = tile+1,
     * plus 1 attribute byte when flag != 0). Only layer 0 is used by the game. Attribute bit 0 =
     * draw colour 0 transparent, bits 6/7 = animate over the next 2 / 4 tiles.
     */
    static MapData readMap(byte[] m, int tileset){
        int layers=m[2]&0xff, nobj=(short)le16(m, 6);
        int lh=8, fmt=m[lh+1]&0xff;
        MapData d=new MapData();
        d.set=tileset; d.flag=m[lh]&0xff; d.w=m[lh+2]&0xff; d.h=m[lh+3]&0xff; d.anim=m[lh+4]&0xff;
        int count=m[lh+6]&0xff;
        if(fmt!=7) throw new IllegalStateException("map format "+fmt);
        int objOff=8+layers*24;
        int cellOff=objOff+Math.max(0, nobj)*8;
        int cell = d.flag!=0 ? 2 : 1, n=d.w*d.h;
        d.tile=new byte[n]; d.attr=new byte[n];
        for(int i=0;i<n;i++){
            d.tile[i]=m[cellOff+i*cell];
            if(d.flag!=0) d.attr[i]=m[cellOff+i*cell+1];
        }
        d.kind=new int[count]; d.ox=new int[count]; d.oy=new int[count];
        for(int k=0;k<count;k++){
            int o=objOff+k*8;
            d.kind[k]=le32(m, o); d.ox[k]=le16(m, o+4); d.oy[k]=le16(m, o+6);
        }
        return d;
    }

    /**
     * Tile set t as a 16-column sheet holding only the variants its maps use (tile k opaque, or
     * with colour 0 transparent), identical 8x8 pictures sharing a slot; numbers the cells of the
     * maps of this set. Returns the number of slots.
     */
    static int writeTiles(byte[] t, int set, MapData[] maps, File f) throws IOException {
        int n=t.length/64;
        Map<String,Integer> byPixels=new HashMap<String,Integer>();
        List<int[]> slots=new ArrayList<int[]>();
        int[] slotOf=new int[2*n];                       // variant (k*2 + transparent) -> slot
        for(MapData m: maps){
            if(m.set!=set) continue;
            m.cell=new int[m.w*m.h];
            Map<String,Integer> seqIdx=new HashMap<String,Integer>();
            for(int i=0;i<m.cell.length;i++){
                int k=(m.tile[i]&0xff)-1;
                if(k<0) continue;
                int a=m.attr[i]&0xff, tr=a&1;
                int frames = (a&0x40)!=0 ? 2 : (a&0x80)!=0 ? 4 : 1;
                int[] fs=new int[frames];
                for(int fr=0;fr<frames;fr++) fs[fr]=slot(t, n, k+fr, tr, slotOf, byPixels, slots);
                if(frames==1){ m.cell[i]=fs[0]; continue; }
                String key=Arrays.toString(fs);
                Integer j=seqIdx.get(key);
                if(j==null){ j=m.seqs.size(); m.seqs.add(fs); seqIdx.put(key, j); }
                m.cell[i]=128+j;
            }
            if(m.seqs.size()>127) throw new IllegalStateException("too many animated sequences");
        }
        if(slots.size()>127) throw new IllegalStateException("too many tiles: "+slots.size());
        int cols=16, rows=Math.max(1, (slots.size()+cols-1)/cols);
        BufferedImage im=new BufferedImage(cols*8, rows*8, BufferedImage.TYPE_INT_ARGB);
        for(int s=0;s<slots.size();s++) im.setRGB((s%cols)*8, (s/cols)*8, 8, 8, slots.get(s), 0, 8);
        ImageIO.write(im, "png", f);
        return slots.size();
    }
    static int slot(byte[] t, int n, int k, int tr, int[] slotOf, Map<String,Integer> byPixels, List<int[]> slots){
        if(k>=n) throw new IllegalStateException("animated tile past the end of the set");
        int v=k*2+tr;
        if(slotOf[v]!=0) return slotOf[v];
        int[] px=new int[64];
        for(int i=0;i<64;i++){ int c=t[k*64+i]&0xff; px[i] = (tr==1 && c==0) ? 0 : RGB332[c]; }
        String key=Arrays.toString(px);
        Integer s=byPixels.get(key);
        if(s==null){ slots.add(px); s=slots.size(); byPixels.put(key, s); }
        slotOf[v]=s;
        return s;
    }

    /**
     * mapN.bin (read by hc2.Res.loadMap): u8 w, h, anim, attr?; w*h cells (TiledLayer index,
     * 128+j = sequence j); u8 sequences, each u8 frames + frame slots; w*h attribute bytes if attr;
     * u16 objects, each s32 kind, u16 x, u16 y.
     */
    static void writeMap(MapData m, boolean attrs, File f) throws IOException {
        ByteArrayOutputStream bo=new ByteArrayOutputStream();
        DataOutputStream d=new DataOutputStream(bo);
        d.writeByte(m.w); d.writeByte(m.h); d.writeByte(m.anim); d.writeByte(attrs ? 1 : 0);
        for(int i=0;i<m.cell.length;i++) d.writeByte(m.cell[i]);
        d.writeByte(m.seqs.size());
        for(int[] fs: m.seqs){ d.writeByte(fs.length); for(int x: fs) d.writeByte(x); }
        if(attrs) d.write(m.attr);
        d.writeShort(m.kind.length);
        for(int k=0;k<m.kind.length;k++){ d.writeInt(m.kind[k]); d.writeShort(m.ox[k]); d.writeShort(m.oy[k]); }
        d.flush();
        Files.write(f.toPath(), bo.toByteArray());
    }

    // ---------------------------------------------------------------- container
    static byte[] res(int i){
        int off=exe.resourceOffset(i), len=exe.resourceLength(i);
        if(all[off]=='L' && all[off+1]=='Z') return lz(all, off);
        return Arrays.copyOfRange(all, off, off+len);
    }
    /** mophun LZ ("LZ", maxBits, extBits, u32 unpacked, u32 packed, ..., data at +22; MSB-first bits) */
    static byte[] lz(byte[] s, int off){
        int maxB=s[off+2]&0xff, extB=s[off+3]&0xff;
        int usize=le32(s, off+4), csize=le32(s, off+8);
        byte[] d=new byte[usize];
        src=s; base=off+22; pos=0;
        int dp=0;
        while(dp<usize && pos<csize*8){
            if(bits(1)==1){
                int v2=0; while(v2<maxB && bits(1)==1) v2++;
                int cl=2; if(v2!=0) cl=(bits(v2)|(1<<v2))+1;
                int bo = cl==2 ? bits(8)+2 : bits(extB)+cl;
                if(cl>usize-dp) cl=usize-dp;
                for(int i=0;i<cl;i++) d[dp+i]=d[dp-bo+i];
                dp+=cl;
            } else d[dp++]=(byte)bits(8);
        }
        return d;
    }
    static byte[] src; static int base, pos;
    static int bits(int n){
        int r=0;
        for(int i=0;i<n;i++){
            int k=base+(pos>>3);
            int b = k<src.length ? ((src[k]&0xff)>>(7-(pos&7)))&1 : 0;
            r=(r<<1)|b; pos++;
        }
        return r;
    }

    static int rgb555(int v){
        int r=(v>>10)&31, g=(v>>5)&31, b=v&31;
        return 0xff000000|(((r*510+31)/62)<<16)|(((g*510+31)/62)<<8)|((b*510+31)/62);
    }
    static int le16(byte[] b, int o){ return (b[o]&0xff)|((b[o+1]&0xff)<<8); }
    static int le32(byte[] b, int o){ return le16(b, o)|(le16(b, o+2)<<16); }

    /** the same test as mophun.GameLibrary.looksEncrypted */
    static boolean looksEncrypted(byte[] d){
        if(d.length<44 || d[0]!='V'||d[1]!='M'||d[2]!='G'||d[3]!='P') return false;
        int codeSize=le32(d, 12);
        int len=Math.min(Math.min(codeSize, 4096), d.length-40);
        if(len<=64) return false;
        boolean[] seen=new boolean[256]; int distinct=0;
        for(int i=0;i<len;i++){ int b=d[40+i]&0xff; if(!seen[b]){ seen[b]=true; distinct++; } }
        return distinct>190;
    }
}
