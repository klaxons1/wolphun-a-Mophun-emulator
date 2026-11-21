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
    static final int MAP_MENU=0, MAP_LEVEL=1, MAP_HUD_WIDE=5, MAP_HUD_SMALL=6;
    /** tile set of each map: 0 level, 1 menu, 2 HUD */
    static final int[] MAP_SET={1, 0, 0, 0, 0, 2, 2};
    final Image[] tiles=new Image[3];

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


    // ------------------------------------------------------------------ maps
    /** a map as a TiledLayer plus its animated sequences (2 or 4 frames) */
    static final class Layer {
        TiledLayer tl;
        int anim;                                   // frames between animation steps (0 = still)
        int count, last=-1; int[] idx; byte[][] frames;
        /** shows frame f (0..3) of every animated sequence */
        void sync(int f){
            if(f==last) return;
            last=f;
            for(int i=0;i<count;i++){ byte[] fr=frames[i]; tl.setAnimatedTile(idx[i], fr[f%fr.length]); }
        }
    }

    /** what the game keeps of a map: size, collision attributes (levels), objects, the layer */
    static final class Map {
        int w, h; byte[] attr; int[] kind, ox, oy; Layer layer;
    }

    /**
     * Loads /mapN.bin (written by tools/Hc2Assets: cells already numbered for the tile set) and
     * builds its TiledLayer; the cell bytes are only kept until the layer is filled.
     */
    Map loadMap(int m) throws IOException {
        DataInputStream d=new DataInputStream(open("/map"+m+".bin"));
        try {
            Map M=new Map();
            int w=d.readUnsignedByte(), h=d.readUnsignedByte();
            M.w=w; M.h=h;
            Layer L=new Layer();
            L.anim=d.readUnsignedByte();
            boolean attrs=d.readUnsignedByte()!=0;
            byte[] cell=new byte[w*h];
            d.readFully(cell);
            int ns=d.readUnsignedByte();
            TiledLayer tl=new TiledLayer(w, h, tiles[MAP_SET[m]], 8, 8);
            L.tl=tl; L.count=ns; L.idx=new int[ns]; L.frames=new byte[ns][];
            for(int j=0;j<ns;j++){
                byte[] fr=new byte[d.readUnsignedByte()];
                d.readFully(fr);
                L.frames[j]=fr;
                L.idx[j]=tl.createAnimatedTile(fr[0]);
            }
            for(int y=0, i=0;y<h;y++) for(int x=0;x<w;x++, i++){
                int c=cell[i]&0xff;
                if(c!=0) tl.setCell(x, y, c<128 ? c : L.idx[c-128]);
            }
            cell=null;
            if(attrs){ M.attr=new byte[w*h]; d.readFully(M.attr); }
            int no=d.readUnsignedShort();
            M.kind=new int[no]; M.ox=new int[no]; M.oy=new int[no];
            for(int i=0;i<no;i++){ M.kind[i]=d.readInt(); M.ox[i]=d.readUnsignedShort(); M.oy[i]=d.readUnsignedShort(); }
            M.layer=L;
            return M;
        } finally {
            d.close();
        }
    }

    static InputStream open(String name) throws IOException {
        InputStream in=Res.class.getResourceAsStream(name);
        if(in==null) throw new IOException("missing "+name);
        return in;
    }
}
