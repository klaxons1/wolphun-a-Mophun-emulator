package hc2;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.TiledLayer;

/**
 * Game data, as written by tools/Hc2Assets at build time (see that file for the formats):
 * sprite atlases (PNG), tile sets, the bitmap digit font, maps, tables and texts.
 */
final class Res {
    // ------------------------------------------------------------------ sprites
    /** sprite id -> atlas (0 = shared, 1+L = bosses of level L) and rectangle / hot spot */
    byte[] sAtlas; short[] sX, sY, sW, sH, sCX, sCY;
    /** the original sprite table: entries 0..60 shared, 61..83 per level */
    short[] tab; short[][] bossTab;
    int hourglass;
    final Image[] atlas=new Image[5];

    // ------------------------------------------------------------------ tables
    final int[] sky=new int[24];            // RGB555 sky colour of each hour
    final byte[] path=new byte[26*3];       // queen bee flight: dx, dy, duration
    final byte[] fontMap=new byte[91];      // char -> glyph of f.png (0xff = none)
    Image font;

    // ------------------------------------------------------------------ tiles and maps
    static final int MAP_MENU=0, MAP_LEVEL=1, MAP_HUD_WIDE=5, MAP_HUD_SMALL=6, MAPS=7;
    final int[] nTiles=new int[3];
    final Image[] tiles=new Image[3];
    final int[] mFlag=new int[MAPS], mW=new int[MAPS], mH=new int[MAPS], mAnim=new int[MAPS], mSet=new int[MAPS];
    final byte[][] mCell=new byte[MAPS][], mAttr=new byte[MAPS][];
    final int[][] oKind=new int[MAPS][], oX=new int[MAPS][], oY=new int[MAPS][];

    // ------------------------------------------------------------------ texts
    String[] str; String help;

    void load() throws IOException {
        DataInputStream d=new DataInputStream(open("/d.bin"));
        if(d.readInt()!=0x48433244) throw new IOException("bad d.bin");
        int n=d.readUnsignedShort()+1;                       // id 0 = no sprite
        sAtlas=new byte[n]; sX=new short[n]; sY=new short[n]; sW=new short[n]; sH=new short[n]; sCX=new short[n]; sCY=new short[n];
        for(int i=1;i<n;i++){
            sAtlas[i]=d.readByte(); sX[i]=d.readShort(); sY[i]=d.readShort();
            sW[i]=d.readShort(); sH[i]=d.readShort(); sCX[i]=d.readShort(); sCY[i]=d.readShort();
        }
        tab=new short[61];
        for(int i=0;i<61;i++) tab[i]=d.readShort();
        bossTab=new short[4][23];
        for(int l=0;l<4;l++) for(int i=0;i<23;i++) bossTab[l][i]=d.readShort();
        hourglass=d.readShort();
        for(int i=0;i<24;i++) sky[i]=d.readUnsignedShort();
        d.readFully(path);
        d.readFully(fontMap);
        for(int t=0;t<3;t++) nTiles[t]=d.readUnsignedShort();
        for(int m=0;m<MAPS;m++){
            mFlag[m]=d.readUnsignedByte(); mW[m]=d.readUnsignedByte(); mH[m]=d.readUnsignedByte();
            mAnim[m]=d.readUnsignedByte(); mSet[m]=d.readUnsignedByte();
            int cells=mW[m]*mH[m];
            mCell[m]=new byte[cells]; d.readFully(mCell[m]);
            if(mFlag[m]!=0){ mAttr[m]=new byte[cells]; d.readFully(mAttr[m]); }
            int no=d.readUnsignedShort();
            oKind[m]=new int[no]; oX[m]=new int[no]; oY[m]=new int[no];
            for(int i=0;i<no;i++){ oKind[m][i]=d.readInt(); oX[m][i]=d.readUnsignedShort(); oY[m][i]=d.readUnsignedShort(); }
        }
        str=new String[d.readUnsignedShort()];
        for(int i=0;i<str.length;i++) str[i]=d.readUTF();
        help=d.readUTF();
        d.close();

        atlas[0]=Image.createImage("/s.png");
        font=Image.createImage("/f.png");
        for(int t=0;t<3;t++) tiles[t]=Image.createImage("/t"+t+".png");
    }

    /** keeps only the boss atlas of the level being played (saves heap on small phones) */
    void loadBosses(int level) throws IOException {
        for(int l=0;l<4;l++) if(l!=level) atlas[1+l]=null;
        if(atlas[1+level]==null){
            System.gc();
            atlas[1+level]=Image.createImage("/b"+level+".png");
        }
    }

    /** sprite id of entry k of the original sprite table */
    int spr(int k, int level){ return k<61 ? tab[k] : bossTab[level][k-61]; }

    /** draws sprite id with its hot spot at (x, y) (colour 0 is transparent in the atlas) */
    void drawSprite(Graphics g, int id, int x, int y){
        if(id<=0) return;
        Image im=atlas[sAtlas[id]];
        if(im==null) return;
        g.drawRegion(im, sX[id], sY[id], sW[id], sH[id], 0, x-sCX[id], y-sCY[id], Graphics.TOP|Graphics.LEFT);
    }

    /** map attribute byte; coordinates are bytes in the original (vMapGetAttribute(byte, byte)) */
    int attr(int m, int x, int y){
        x&=0xff; y&=0xff;
        byte[] a=mAttr[m];
        if(a==null || x>=mW[m] || y>=mH[m]) return 0;
        return a[y*mW[m]+x]&0xff;
    }

    // ------------------------------------------------------------------ TiledLayer
    /** a map as a TiledLayer plus its animated cells (attribute bits 6/7: 2 or 4 frame cycles) */
    static final class Layer {
        TiledLayer tl;
        int anim;                                   // frames between animation steps (0 = still)
        int count; int[] idx=new int[8], base=new int[8], mode=new int[8];
        /** sets every animated tile to frame f (0..3) of its cycle */
        void sync(int f){ for(int i=0;i<count;i++) tl.setAnimatedTile(idx[i], base[i]+f%mode[i]); }
    }

    /**
     * Builds map m as a TiledLayer. Tile k of the set is static tile k+1 (opaque) or n+k+1 (colour
     * 0 transparent, attribute bit 0); animated cells step over consecutive tiles.
     */
    Layer layer(int m){
        int n=nTiles[mSet[m]], w=mW[m], h=mH[m];
        Layer L=new Layer();
        L.anim=mAnim[m];
        TiledLayer tl=new TiledLayer(w, h, tiles[mSet[m]], 8, 8);
        L.tl=tl;
        byte[] cell=mCell[m], at=mAttr[m];
        int[] lookup=null;
        for(int y=0;y<h;y++) for(int x=0;x<w;x++){
            int i=y*w+x, t=cell[i]&0xff;
            if(t==0) continue;
            int a = at!=null ? at[i]&0xff : 0;
            int base = (a&1)!=0 ? n+t : t;
            int mode = (a&0x40)!=0 ? 2 : (a&0x80)!=0 ? 4 : 0;
            if(mode==0){ tl.setCell(x, y, base); continue; }
            if(lookup==null) lookup=new int[(2*n+1)*2];
            int key=base*2+(mode>>2);
            if(lookup[key]==0){
                if(L.count==L.idx.length){
                    int c=L.count;
                    int[] a1=new int[c*2], a2=new int[c*2], a3=new int[c*2];
                    System.arraycopy(L.idx, 0, a1, 0, c); System.arraycopy(L.base, 0, a2, 0, c);
                    System.arraycopy(L.mode, 0, a3, 0, c);
                    L.idx=a1; L.base=a2; L.mode=a3;
                }
                lookup[key]=tl.createAnimatedTile(base);
                L.idx[L.count]=lookup[key]; L.base[L.count]=base; L.mode[L.count]=mode; L.count++;
            }
            tl.setCell(x, y, lookup[key]);
        }
        return L;
    }

    static InputStream open(String name) throws IOException {
        InputStream in=Res.class.getResourceAsStream(name);
        if(in==null) throw new IOException("missing "+name);
        return in;
    }
}
