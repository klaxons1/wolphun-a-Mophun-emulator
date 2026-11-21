package wolphun.j2me;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;

import wolphun.decomp.rt.Pip2Runtime;

/**
 * The mophun SDK for decompiled games on J2ME (CLDC 1.1 / MIDP 2.0 / MMAPI).
 *
 * A port of the parts of desktop/src/wolphun/vm/Sdk.java that 2D games use, with the same
 * semantics (every rule there was measured against real games; the comments in that file explain
 * them). Differences: the framebuffer is the int[] of {@link WCanvas}; saves go to RMS instead of
 * files; MIDI goes through MMAPI; the system font is the bitmap font rendered at build time.
 *
 * The constant of each implemented function is its id; wolphun.decomp.j2me.J2meAssets rewrites the
 * decompiled source to call {@code sys(Sdk.vName, ...)} (it reads the list from this file:
 * keep the "public static final int vName = N;" form).
 */
public final class Sdk {
    public static final int UNIMPLEMENTED = 0;
    public static final int vCharExtent = 1;
    public static final int vClearScreen = 2;
    public static final int vDecompress = 3;
    public static final int vDisposePtr = 4;
    public static final int vDrawObject = 5;
    public static final int vFillRect = 6;
    public static final int vFlipScreen = 7;
    public static final int vGetButtonData = 8;
    public static final int vGetCaps = 9;
    public static final int vGetRandom = 10;
    public static final int vGetTickCount = 11;
    public static final int vGetTimeDate = 12;
    public static final int vMapGetAttribute = 13;
    public static final int vMapInit = 14;
    public static final int vMapSetXY = 15;
    public static final int vMsgBoxU = 16;
    public static final int vNewPtr = 17;
    public static final int vPlayResource = 18;
    public static final int vPlot = 19;
    public static final int vPrint = 20;
    public static final int vSelectFont = 21;
    public static final int vSetActiveFont = 22;
    public static final int vSetBackColor = 23;
    public static final int vSetClipWindow = 24;
    public static final int vSetForeColor = 25;
    public static final int vSetPaletteEntry = 26;
    public static final int vSetRandom = 27;
    public static final int vSetTransferMode = 28;
    public static final int vSpriteBoxCollision = 29;
    public static final int vSpriteClear = 30;
    public static final int vSpriteCollision = 31;
    public static final int vSpriteInit = 32;
    public static final int vSpriteSet = 33;
    public static final int vStrCpy = 34;
    public static final int vStrLen = 35;
    public static final int vStreamClose = 36;
    public static final int vStreamOpen = 37;
    public static final int vStreamRead = 38;
    public static final int vStreamSeek = 39;
    public static final int vStreamWrite = 40;
    public static final int vSysCtl = 41;
    public static final int vTerminateVMGP = 42;
    public static final int vTextExtent = 43;
    public static final int vTextExtentU = 44;
    public static final int vTextOut = 45;
    public static final int vTextOutU = 46;
    public static final int vUpdateMap = 47;
    public static final int vUpdateSprite = 48;
    public static final int vitoa = 49;
    public static final int vutoa = 50;
    // a few neighbours that cost nothing
    public static final int vMapGetTile = 51;
    public static final int vMapDispose = 52;
    public static final int vMapHeaderUpdate = 53;
    public static final int vUpdateSpriteMap = 54;
    public static final int vWaitVBL = 55;
    public static final int vDrawTile = 56;
    public static final int vNewPtrClear = 57;
    public static final int vStrLenU = 58;
    public static final int vMalloc = 59;
    public static final int vFreePtr = 60;

    final WCanvas host;
    final Pip2Runtime g;
    byte[] mem; int base;

    final int w, h;
    final int[] fb;
    final int[] palette = new int[256];
    final int[] rgb332 = new int[256];
    int clipX0, clipY0, clipX1, clipY1;            // [x0,x1) x [y0,y1)

    int foreColor = 0xff, backColor = 0, transferMode = 1;
    final boolean fmt7Transp;
    long rngState = 0x12345678L;
    final long tick0 = System.currentTimeMillis();
    long pausedMs = 0;
    int pollsSinceFlip = 0;

    // test hooks (off on phones): deterministic clock and scripted keys, used to compare this port
    // frame by frame with the desktop runtime (VTICK + --keys of wolphun.decomp.rt.RunDecompiled)
    public interface Script { int keys(); }
    public static int virtualTickStep = 0;
    public static Script script;
    long vtick = 0;

    public Sdk(WCanvas host, Pip2Runtime g, int w, int h, int[] fb, boolean fmt7Transp,
               int heapBase, int heapEnd, byte[][] resources){
        this.host=host; this.g=g; this.mem=g.mem; this.base=g.base;
        this.w=w; this.h=h; this.fb=fb; this.fmt7Transp=fmt7Transp;
        this.heapBase=heapBase; this.heapEnd=heapEnd; this.nextHeap=heapBase;
        this.resources=resources;
        for(int i=0;i<256;i++){
            int r=(i>>5)&7, gg=(i>>2)&7, b=i&3;
            int c=0xff000000|(((r*510+7)/14)<<16)|(((gg*510+7)/14)<<8)|((b*510+3)/6);
            palette[i]=c; rgb332[i]=c;
        }
        clipX0=0; clipY0=0; clipX1=w; clipY1=h;
    }

    // ------------------------------------------------------------------ memory
    final int rd8(int a){ int i=a-base; return (i>=0 && i<mem.length)? mem[i]&0xFF : 0; }
    final int rd16(int a){ return rd8(a)|(rd8(a+1)<<8); }
    final int rd32(int a){ return rd8(a)|(rd8(a+1)<<8)|(rd8(a+2)<<16)|(rd8(a+3)<<24); }
    final void wr8(int a, int v){ int i=a-base; if(i>=0 && i<mem.length) mem[i]=(byte)v; }
    final void wr16(int a, int v){ wr8(a, v); wr8(a+1, v>>8); }
    final void wr32(int a, int v){ wr8(a, v); wr8(a+1, v>>8); wr8(a+2, v>>16); wr8(a+3, v>>24); }
    /** true when [a, a+n) lies inside the address space (direct array access is safe) */
    final boolean inMem(int a, int n){ int i=a-base; return i>=0 && n>=0 && i+n<=mem.length; }

    // ------------------------------------------------------------------ dispatch
    public int call(int id, int a0, int a1, int a2, int a3){
        switch(id){
            case vGetTickCount:
                // the games busy-wait on the clock between frames: yield the CPU instead of spinning
                if(virtualTickStep>0){ vtick+=virtualTickStep; return (int)vtick; }
                if(++pollsSinceFlip>4) host.nap();
                return tickNow();
            case vSpriteSet:
                if(slotSprite!=null && a0<spriteCount){ slotSprite[a0]=a1; slotX[a0]=(short)a2; slotY[a0]=(short)a3; }
                return 0;
            case vSpriteBoxCollision: return spriteBoxCollision(a0, a1, a2);
            case vSpriteCollision:    return spriteCollision(a0, a1, a2);
            case vMapGetAttribute:    return mapAttr(a0, a1);
            case vMapGetTile:         return mapTile(a0, a1);
            case vSetClipWindow:      setClip(a0, a1, a2, a3); return 0;
            case vMapInit:            mapHeaderPtr=a0; mhValid=false; mapSync(); return 1;
            case vMapHeaderUpdate:    mapHeaderPtr=a0; mhValid=false; mapSync(); return 1;
            case vMapDispose:         mapHeaderPtr=0; mhValid=false; return 0;
            case vUpdateMap:          drawMap(); return 0;
            case vUpdateSprite:       drawSpriteSlots(); return 0;
            case vUpdateSpriteMap:    drawMap(); drawSpriteSlots(); return 0;
            case vMapSetXY:
                if(mapHeaderPtr!=0){ wr16(mapHeaderPtr+12, a0); wr16(mapHeaderPtr+14, a1); }
                mhXPos=a0&0xffff; mhYPos=a1&0xffff; return 0;
            case vitoa: return itoa(a0, a1, true, a2, a3);
            case vutoa: return itoa(a0, a1, false, a2, a3);
            case vPrint: printFont(a0, a1, a2, a3); return 0;
            case vDrawObject: drawObject(a0, a1, a2); return 0;
            case vDrawTile: drawTile(a0, a1, (short)a2, (short)a3); return 0;
            case vGetButtonData: { int b=host.buttons(); return script!=null ? script.keys() : b; }
            case vGetRandom:
                rngState=rngState*6364136223846793005L+1442695040888963407L;
                return (short)(rngState>>>33);
            case vSetRandom: rngState=a0&0xffffffffL; return 0;
            case vFlipScreen: pollsSinceFlip=0; host.flip(); return 0;
            case vWaitVBL: return 0;
            case vClearScreen: { int c=color(a0); int[] f=fb; for(int i=f.length-1;i>=0;i--) f[i]=c; return 0; }
            case vFillRect: fillRect(a0, a1, a2, a3); return 0;
            case vPlot: put(a0, a1, color(foreColor)); return 0;
            case vSetForeColor: foreColor=a0; return 0;
            case vSetBackColor: backColor=a0; return 0;
            case vSetPaletteEntry: palette[a0&0xff]=rgb555(a1&0xffff); return 0;
            case vSetTransferMode: { int p=transferMode; transferMode=a0; return p; }
            case vSpriteInit:
                spriteCount=a0&0xff; slotSprite=new int[spriteCount]; slotX=new int[spriteCount]; slotY=new int[spriteCount];
                return 1;
            case vSpriteClear: if(slotSprite!=null) for(int i=0;i<spriteCount;i++) slotSprite[i]=0; return 0;
            case vGetTimeDate: writeDateTime(a0); return 0;
            case vGetCaps: return getCaps(a0, a1);
            case vSysCtl:
                if(a0==1) host.vibrate((a1&1)!=0 ? (a1>>>16)&0xffff : 0);     // SYSCTL_SETVIBRATE
                return 0;
            case vTerminateVMGP: throw new Pip2Runtime.Terminated("vTerminateVMGP");
            case vNewPtr: case vMalloc: case vNewPtrClear: return alloc(a0);
            case vDisposePtr: case vFreePtr: freePtr(a0); return 0;
            case vStrLen: { int n=0; while(rd8(a0+n)!=0) n++; return n; }
            case vStrLenU: { int n=0; while(rd16(a0+n*2)!=0) n++; return n; }
            case vStrCpy: { int i=0, b; while(true){ b=rd8(a1+i); wr8(a0+i, b); if(b==0) break; i++; } return a0+i; }
            case vSelectFont: selectFont(a0); return a1;
            case vSetActiveFont: return setActiveFont(a0);
            case vTextOut:  sysText(a0, a1, a2, false, color(foreColor)); return 0;
            case vTextOutU: sysText(a0, a1, a2, true, color(foreColor)); return 0;
            case vTextExtent:  return (fontH<<16) | (sysTextWidth(a0, false) & 0xffff);
            case vTextExtentU: return (fontH<<16) | (sysTextWidth(a0, true) & 0xffff);
            case vCharExtent:  return (fontH<<16) | (glyphAdv(a0&0xffff) & 0xffff);
            case vMsgBoxU: return msgBox((a0&0x80)!=0 ? rd32(g.sp) : 0, a1);
            case vDecompress: return a0==0 ? decompressStream(a2, a1) : decompress(a0, a1);
            case vStreamOpen:  return streamOpen(a0, a1);
            case vStreamRead:  return streamRead(a0, a1, a2);
            case vStreamWrite: return streamWrite(a0, a1, a2);
            case vStreamSeek:  return streamSeek(a0, a1, a2);
            case vStreamClose: streamClose(a0); return 0;
            case vPlayResource: return playResource(a0, a1, a2);
            default: return 0;                          // UNIMPLEMENTED
        }
    }

    int tickNow(){ return (int)(System.currentTimeMillis()-tick0-pausedMs); }

    // ------------------------------------------------------------------ colours / primitives
    static int rgb555(int v){
        int r=(v>>10)&31, gg=(v>>5)&31, b=v&31;
        return 0xff000000|(((r*510+31)/62)<<16)|(((gg*510+31)/62)<<8)|((b*510+31)/62);
    }
    static int rgb565(int v){
        int r=(v>>11)&31, gg=(v>>5)&63, b=v&31;
        return 0xff000000|(((r*510+31)/62)<<16)|(((gg*510+63)/126)<<8)|((b*510+31)/62);
    }
    final int color(int c){ return c<0 ? rgb555(c&0xffff) : palette[c&0xff]; }
    final void put(int x, int y, int argb){
        if(x<clipX0||x>=clipX1||y<clipY0||y>=clipY1) return;
        fb[y*w+x]=argb;
    }
    void setClip(int x0, int y0, int x1, int y1){
        if(x0>x1){ int t=x0; x0=x1; x1=t; }
        if(y0>y1){ int t=y0; y0=y1; y1=t; }
        clipX0=Math.max(0, x0); clipY0=Math.max(0, y0);
        clipX1=Math.min(w, x1+1); clipY1=Math.min(h, y1+1);     // inclusive in the SDK
    }
    void fillRect(int x0, int y0, int x1, int y1){
        if(x0>x1){ int t=x0; x0=x1; x1=t; }
        if(y0>y1){ int t=y0; y0=y1; y1=t; }
        if(x0<clipX0) x0=clipX0; if(y0<clipY0) y0=clipY0;
        if(x1>=clipX1) x1=clipX1-1; if(y1>=clipY1) y1=clipY1-1;
        int c=color(foreColor);
        for(int y=y0;y<=y1;y++){ int row=y*w; for(int x=x0;x<=x1;x++) fb[row+x]=c; }
    }

    static int bppOf(int fmt){
        switch(fmt){
            case 0: case 3: return 1;
            case 1: case 4: return 2;
            case 2: case 5: return 4;
            case 6: case 7: return 8;
            case 8: case 9: case 13: case 14: return 16;
            default: return 8;
        }
    }

    // ------------------------------------------------------------------ sprites
    int spriteCount=0; int[] slotSprite, slotX, slotY;
    boolean spriteSlotDraw=false;

    /** NativeSprite (10 bytes): palOff, format, s16 centerX, s16 centerY, u16 w, u16 h; pixels follow */
    void drawObject(int x, int y, int sp){
        if(sp==0) return;
        int palOff=rd8(sp), format=rd8(sp+1);
        int cx=(short)rd16(sp+2), cy=(short)rd16(sp+4);
        if(cx>1024||cx<-1024) cx=0;
        if(cy>1024||cy<-1024) cy=0;
        int sw=rd16(sp+6), sh=rd16(sp+8);
        if(sw==0||sh==0||sw>2048||sh>2048) return;
        int pix=sp+10, bpp=bppOf(format), mask=(1<<bpp)-1;
        int ox=x-cx, oy=y-cy;
        boolean flipX=(transferMode&2)!=0, flipY=(transferMode&4)!=0;
        boolean transp=(transferMode&1)!=0 || spriteSlotDraw || (fmt7Transp && format==7);
        int jx0=Math.max(0, clipX0-ox), jx1=Math.min(sw, clipX1-ox);
        int jy0=Math.max(0, clipY0-oy), jy1=Math.min(sh, clipY1-oy);
        if(jx0>=jx1 || jy0>=jy1) return;
        int[] f=fb; byte[] m=mem;
        // fast path: 8 bpp RGB332 / Palette256 fully inside memory
        if(bpp==8 && inMem(pix, sw*sh)){
            int[] pal = format==7 ? rgb332 : palette;
            int po = format==7||format==6 ? 0 : palOff;
            int pb=pix-base;
            for(int j=jy0;j<jy1;j++){
                int sj = flipY ? (sh-1-j) : j;
                int src=pb+sj*sw, dst=(oy+j)*w+ox;
                if(!flipX){
                    for(int i=jx0;i<jx1;i++){
                        int idx=m[src+i]&0xFF;
                        if(idx==0 && transp) continue;
                        f[dst+i]=pal[(idx+po)&0xFF];
                    }
                } else {
                    int rs=src+sw-1;
                    for(int i=jx0;i<jx1;i++){
                        int idx=m[rs-i]&0xFF;
                        if(idx==0 && transp) continue;
                        f[dst+i]=pal[(idx+po)&0xFF];
                    }
                }
            }
            return;
        }
        for(int j=jy0;j<jy1;j++){
            int sj = flipY ? (sh-1-j) : j;
            int dst=(oy+j)*w+ox;
            for(int i=jx0;i<jx1;i++){
                int si = flipX ? (sw-1-i) : i;
                int pi=sj*sw+si, argb;
                if(bpp<=8){
                    int bitPos=pi*bpp;
                    int idx=(rd8(pix+(bitPos>>3)) >> (bitPos&7)) & mask;
                    if(transp && idx==0) continue;
                    if(format==0||format==1||format==2){ int gr=(idx*510+mask)/(2*mask); argb=0xff000000|(gr<<16)|(gr<<8)|gr; }
                    else if(format==7) argb=rgb332[idx&0xff];
                    else argb=palette[(idx+(format==6?0:palOff))&0xff];
                } else {
                    int v=rd16(pix+pi*2);
                    if(transp && v==0) continue;
                    argb = format==8 ? rgb565(v) : rgb555(v);
                }
                f[dst+i]=argb;
            }
        }
    }
    void drawSpriteSlots(){
        if(slotSprite==null) return;
        boolean prev=spriteSlotDraw; spriteSlotDraw=true;
        try { for(int i=0;i<spriteCount;i++) if(slotSprite[i]!=0) drawObject(slotX[i], slotY[i], slotSprite[i]); }
        finally { spriteSlotDraw=prev; }
    }
    void drawTile(int dataPtr, int format, int x, int y){
        int pixFmt=format&7; boolean transp=(format&8)!=0; int palOff=(format>>8)&0xff;
        int bpp=bppOf(pixFmt), mask=(1<<bpp)-1;
        for(int j=0;j<8;j++){
            int dy=y+j; if(dy<clipY0||dy>=clipY1) continue;
            for(int i=0;i<8;i++){
                int dx=x+i; if(dx<clipX0||dx>=clipX1) continue;
                int argb;
                if(bpp<=8){
                    int bitPos=(j*8+i)*bpp;
                    int idx=(rd8(dataPtr+(bitPos>>3)) >> (bitPos&7)) & mask;
                    if((pixFmt==7 || transp) && idx==0) continue;
                    argb = pixFmt==7 ? rgb332[idx&0xff] : palette[(idx+palOff)&0xff];
                } else {
                    int v=rd16(dataPtr+(j*8+i)*2);
                    if(transp && v==0) continue;
                    argb = pixFmt==8 ? rgb565(v) : rgb555(v);
                }
                fb[dy*w+dx]=argb;
            }
        }
    }
    static boolean aabb(int ax,int ay,int aw,int ah, int bx,int by,int bw,int bh){
        return ax < bx+bw && ax+aw > bx && ay < by+bh && ay+ah > by;
    }
    int spriteBoxCollision(int boxPtr, int from, int to){
        if(slotSprite==null) return -1;
        int bx=(short)rd16(boxPtr), by=(short)rd16(boxPtr+2), bw=rd16(boxPtr+4), bh=rd16(boxPtr+6);
        int end=Math.min(to, spriteCount-1);
        for(int i=Math.max(0, from); i<=end; i++){
            int sp=slotSprite[i]; if(sp==0) continue;
            if(aabb(bx,by,bw,bh, slotX[i],slotY[i],rd16(sp+6),rd16(sp+8))) return i;
        }
        return -1;
    }
    int spriteCollision(int check, int from, int to){
        if(slotSprite==null || check<0 || check>=spriteCount || slotSprite[check]==0) return -1;
        int cp=slotSprite[check]; int cw=rd16(cp+6), ch=rd16(cp+8), cx=slotX[check], cy=slotY[check];
        int end=Math.min(to, spriteCount-1);
        for(int i=Math.max(0, from); i<=end; i++){
            int sp=slotSprite[i]; if(sp==0) continue;
            if(aabb(cx,cy,cw,ch, slotX[i],slotY[i],rd16(sp+6),rd16(sp+8))) return i;
        }
        return -1;
    }

    // ------------------------------------------------------------------ tile map
    int mapHeaderPtr=0;
    int mhFlag, mhFmt, mhW, mhH, mhAnim, mhXPan, mhYPan, mhXPos, mhYPos, mhMapData, mhTileData;
    boolean mhValid=false;
    int frameAnimate=0, framePassed=0;

    boolean plausiblePtr(int p){
        if(p<base || p>=base+mem.length) return false;
        return p<heapBase || p<nextHeap;
    }
    /** snapshot of the MAP_HEADER taken at vMapInit (see the desktop Sdk.mapSync comment) */
    void mapSync(){
        if(mhValid) return;
        int hp=mapHeaderPtr; if(hp==0) return;
        int mw=rd8(hp+2), mh=rd8(hp+3);
        int md=rd32(hp+16), td=rd32(hp+20);
        if(mw==0||mh==0||md==0||td==0) return;
        if(!plausiblePtr(md) || !plausiblePtr(td)) return;
        mhFlag=rd8(hp); mhFmt=rd8(hp+1); mhW=mw; mhH=mh; mhAnim=rd8(hp+4);
        mhXPan=(short)rd16(hp+8); mhYPan=(short)rd16(hp+10);
        mhXPos=rd16(hp+12); mhYPos=rd16(hp+14);
        mhMapData=md; mhTileData=td; mhValid=true;
    }
    void drawMap(){
        mapSync();
        if(!mhValid) return;
        int flag=mhFlag, mapW=mhW, mapH=mhH;
        int xPos=(short)mhXPos, yPos=(short)mhYPos;
        int mapData=mhMapData, tileData=mhTileData;
        if(!inMem(mapData, 1) || !inMem(tileData, 1)) return;
        boolean hasAttr = flag!=0; int cell = hasAttr?2:1;
        int fmt=mhFmt, bpp=bppOf(fmt), mask=(1<<bpp)-1;
        int tileBytes=(64*bpp+7)/8;
        int[] pal = fmt==7 ? rgb332 : palette;
        boolean layerTransp=(flag&1)!=0;
        int startDrawX=mhXPan-(xPos%8), startDrawY=mhYPan-(yPos%8);
        int tileStartX=xPos/8, tileStartY=yPos/8;
        int tilesAcross=(w-startDrawX+7)/8+1, tilesDown=(h-startDrawY+7)/8+1;
        boolean bankFmt=(fmt==3||fmt==4||fmt==5);
        int[] f=fb; byte[] m=mem;
        for(int ry=0; ry<tilesDown; ry++){
            int my=tileStartY+ry; if(my<0||my>=mapH) continue;
            int sy=startDrawY+ry*8;
            if(sy+8<=clipY0 || sy>=clipY1) continue;
            for(int rx=0; rx<tilesAcross; rx++){
                int mx=tileStartX+rx; if(mx<0||mx>=mapW) continue;
                int sx=startDrawX+rx*8;
                if(sx+8<=clipX0 || sx>=clipX1) continue;
                int idx=(my*mapW+mx)*cell;
                int t=rd8(mapData+idx);
                if(t==0) continue; t-=1;
                int attr = hasAttr ? rd8(mapData+idx+1) : 0;
                int palBank = bankFmt ? (attr&0xF0) : 0;
                if(!bankFmt){
                    if((attr&0x40)!=0) t+=frameAnimate%2;
                    else if((attr&0x80)!=0) t+=frameAnimate%4;
                }
                int tp=tileData+t*tileBytes;
                boolean tileTransp=(layerTransp && (!hasAttr || (attr&1)!=0)) || (hasAttr && (attr&1)!=0);
                int j0=Math.max(0, clipY0-sy), j1=Math.min(8, clipY1-sy);
                int i0=Math.max(0, clipX0-sx), i1=Math.min(8, clipX1-sx);
                if(bpp==8 && inMem(tp, 64)){                 // fast path (HC2: RGB332 tiles)
                    int src=tp-base;
                    for(int j=j0;j<j1;j++){
                        int s=src+j*8, d=(sy+j)*w+sx;
                        for(int i=i0;i<i1;i++){
                            int v=m[s+i]&0xFF;
                            if(tileTransp && v==0) continue;
                            f[d+i]=pal[(v+palBank)&0xFF];
                        }
                    }
                    continue;
                }
                for(int j=j0;j<j1;j++){
                    int d=(sy+j)*w+sx;
                    for(int i=i0;i<i1;i++){
                        int v, argb;
                        if(bpp<=8){
                            int bitPos=(j*8+i)*bpp;
                            v=(rd8(tp+(bitPos>>3)) >> (bitPos&7)) & mask;
                            if(fmt==0||fmt==1||fmt==2){ int gr=(v*510+mask)/(2*mask); argb=0xff000000|(gr<<16)|(gr<<8)|gr; }
                            else argb=pal[(v+palBank)&0xFF];
                            if(tileTransp && v==0) continue;
                        } else {
                            v=rd16(tp+(j*8+i)*2);
                            argb = fmt==8 ? rgb565(v) : rgb555(v);
                            if(tileTransp && (argb&0xffffff)==0) continue;
                        }
                        f[d+i]=argb;
                    }
                }
            }
        }
        if(mhAnim!=0 && (++framePassed>mhAnim)){ frameAnimate=(frameAnimate+1)&3; framePassed=0; }
    }
    int mapAttr(int x, int y){
        mapSync(); if(!mhValid || mhFlag==0) return 0;
        if(x<0||y<0||x>=mhW||y>=mhH) return 0;
        return rd8(mhMapData+(y*mhW+x)*2+1);
    }
    int mapTile(int x, int y){
        mapSync(); if(!mhValid) return 0;
        if(x<0||y<0||x>=mhW||y>=mhH) return 0;
        return rd8(mhMapData+(y*mhW+x)*(mhFlag!=0?2:1));
    }

    // ------------------------------------------------------------------ game bitmap font (vPrint)
    int aFontPtr=0, aFontData, aFontChartbl, aFontBpp, aFontW, aFontH, aFontPal;
    int setActiveFont(int ptr){
        int prev=aFontPtr; aFontPtr=ptr;
        if(ptr==0){ aFontData=aFontChartbl=aFontBpp=aFontW=aFontH=aFontPal=0; return prev; }
        fontRefresh(); return prev;
    }
    void fontRefresh(){
        if(aFontPtr==0) return;
        aFontData=rd32(aFontPtr); aFontChartbl=rd32(aFontPtr+4);
        aFontBpp=rd8(aFontPtr+8); aFontW=rd8(aFontPtr+9); aFontH=rd8(aFontPtr+10); aFontPal=rd8(aFontPtr+11);
    }
    void printFont(int mode, int x, int y, int str){
        x=(short)x; y=(short)y;
        fontRefresh();
        if(aFontPtr==0 || aFontW<=0 || aFontH<=0){ sysText(x, y, str, false, color(foreColor)); return; }
        int bpp=aFontBpp<=0?1:aFontBpp, mask=(1<<bpp)-1;
        int glyphBytes=(aFontW*aFontH*bpp+7)>>3;
        int fg=color(foreColor);
        boolean block=(mode&1)==0;
        int bg = aFontBpp>1 ? palette[aFontPal&0xff] : color(backColor);
        int penX=x;
        for(int k=0;k<4096;k++){
            int ch=rd8(str+k); if(ch==0) break;
            int gi=rd8(aFontChartbl+ch);
            if(gi!=0xff){
                int gb=aFontData+gi*glyphBytes;
                for(int row=0;row<aFontH;row++){
                    int dy=y+row; if(dy<clipY0||dy>=clipY1) continue;
                    for(int col=0;col<aFontW;col++){
                        int dx=penX+col; if(dx<clipX0||dx>=clipX1) continue;
                        int bitPos=(row*aFontW+col)*bpp;
                        int val=(rd8(gb+(bitPos>>3)) >> (bitPos&7)) & mask;
                        int argb;
                        if(val==0){ if(!block) continue; argb=bg; }
                        else argb = bpp==1 ? fg : palette[(aFontPal+val)&0xff];
                        fb[dy*w+dx]=argb;
                    }
                }
            }
            penX+=aFontW;
        }
    }

    // ------------------------------------------------------------------ system font (f.fnt)
    // several pixel heights rendered at build time; vSelectFont picks one with the desktop rules
    int fontH=8, fontBoxH=10, fontFirst=32, fontLast=31;
    byte[] fAdv, fBw; int[] fOff; byte[] fBits;
    int[] tPx, tH, tBoxH, tFirst, tLast; byte[][] tAdv, tBw, tBits; int[][] tOff;
    public void loadFont(DataInputStream in) throws java.io.IOException {
        if(in.readInt()!=0x574A4632) throw new java.io.IOException("bad font");
        int nt=in.readUnsignedByte();
        tPx=new int[nt]; tH=new int[nt]; tBoxH=new int[nt]; tFirst=new int[nt]; tLast=new int[nt];
        tAdv=new byte[nt][]; tBw=new byte[nt][]; tBits=new byte[nt][]; tOff=new int[nt][];
        for(int t=0;t<nt;t++){
            tPx[t]=in.readUnsignedByte(); tH[t]=in.readUnsignedByte(); tBoxH[t]=in.readUnsignedByte();
            tFirst[t]=in.readUnsignedShort(); tLast[t]=in.readUnsignedShort();
            int n=tLast[t]-tFirst[t]+1;
            byte[] adv=new byte[n], bw=new byte[n]; int[] off=new int[n];
            ByteArrayOutputStream bo=new ByteArrayOutputStream();
            for(int i=0;i<n;i++){
                adv[i]=(byte)in.readUnsignedByte(); bw[i]=(byte)in.readUnsignedByte();
                int bytes=((bw[i]&0xff)*tBoxH[t]+7)>>3;
                off[i]=bo.size();
                for(int k=0;k<bytes;k++) bo.write(in.readUnsignedByte());
            }
            tAdv[t]=adv; tBw[t]=bw; tOff[t]=off; tBits[t]=bo.toByteArray();
        }
        selectFont(11);                                // desktop Screen default
    }
    /** vSelectFont(size, flags): size decoding of the desktop Screen.selectFont (styles ignored) */
    void selectFont(int size){
        int px;
        if((size&0x80000000)!=0) px=size&0x00FFFFFF;                     // FONT_SIZE_PIXELS
        else if((size&0x40000000)!=0) px=((size&0x00FFFFFF)*8+3)/6;      // points -> pixels (round(n*4/3))
        else { px=size; if(px<6) px=8+px*2; if(px>24) px=24; }            // presets 0/1/2
        if(px<6) px=6; if(px>48) px=48;
        if(tPx==null) return;
        int best=0;
        for(int t=1;t<tPx.length;t++) if(Math.abs(tPx[t]-px)<Math.abs(tPx[best]-px)) best=t;
        fontH=tH[best]; fontBoxH=tBoxH[best]; fontFirst=tFirst[best]; fontLast=tLast[best];
        fAdv=tAdv[best]; fBw=tBw[best]; fOff=tOff[best]; fBits=tBits[best];
    }
    int glyphAdv(int c){
        if(c<fontFirst || c>fontLast) c='?';
        int a=fAdv==null?0:fAdv[c-fontFirst]&0xff;
        if(a==0 && c!='?') return glyphAdv('?');
        return a;
    }
    int sysTextWidth(int p, boolean unicode){
        if(p==0) return 0;
        int wsum=0;
        for(int k=0;k<4096;k++){
            int c = unicode ? rd16(p+k*2) : rd8(p+k);
            if(c==0) break;
            wsum+=glyphAdv(c);
        }
        return wsum;
    }
    void drawGlyph(int x, int y, int c, int argb){
        if(fBits==null) return;
        if(c<fontFirst || c>fontLast || fBw[c-fontFirst]==0) c='?';
        int gi=c-fontFirst, bw=fBw[gi]&0xff, off=fOff[gi];
        for(int yy=0;yy<fontBoxH;yy++){
            int dy=y+yy; if(dy<clipY0||dy>=clipY1) continue;
            for(int xx=0;xx<bw;xx++){
                int n=yy*bw+xx;
                if((fBits[off+(n>>3)] & (1<<(n&7)))==0) continue;
                int dx=x+xx; if(dx<clipX0||dx>=clipX1) continue;
                fb[dy*w+dx]=argb;
            }
        }
    }
    void sysText(int x, int y, int p, boolean unicode, int argb){
        if(p==0) return;
        x=(short)x; y=(short)y;
        int pen=x;
        for(int k=0;k<4096;k++){
            int c = unicode ? rd16(p+k*2) : rd8(p+k);
            if(c==0) break;
            if(c!=' ') drawGlyph(pen, y, c, argb);
            pen+=glyphAdv(c);
        }
    }
    /** vMsgBoxU: draws the box, waits for FIRE/left softkey, returns 1 */
    int msgBox(int title, int msg){
        int bgc=0xff101840;
        for(int y=3;y<h-3;y++) for(int x=3;x<w-3;x++)
            fb[y*w+x] = (x==3||x==w-4||y==3||y==h-4) ? 0xffffffff : bgc;
        int sx0=clipX0, sy0=clipY0, sx1=clipX1, sy1=clipY1;
        clipX0=4; clipY0=4; clipX1=w-4; clipY1=h-4;
        int yy=7;
        if(title!=0){ sysText(7, yy, title, true, 0xff00ffff); yy+=fontH+3; }
        // word wrap
        int pen=7, k=0;
        while(yy<=h-20){
            int c=rd16(msg+k*2); if(c==0) break;
            if(c=='\n'){ pen=7; yy+=fontH; k++; continue; }
            int wl=0, j=k;
            while(true){ int d=rd16(msg+j*2); if(d==0||d==' '||d=='\n') break; wl+=glyphAdv(d); j++; }
            if(pen>7 && pen+wl>w-7){ pen=7; yy+=fontH; if(yy>h-20) break; }
            for(int q=k;q<j;q++){ int d=rd16(msg+q*2); drawGlyph(pen, yy, d, 0xffffff00); pen+=glyphAdv(d); }
            k=j;
            if(rd16(msg+k*2)==' '){ pen+=glyphAdv(' '); k++; }
        }
        String ok="[ OK ]"; int px=7;
        for(int i=0;i<ok.length();i++){ drawGlyph(px, h-16, ok.charAt(i), 0xff00ff00); px+=glyphAdv(ok.charAt(i)); }
        clipX0=sx0; clipY0=sy0; clipX1=sx1; clipY1=sy1;
        host.flip();
        host.waitOk();
        return 1;
    }

    // ------------------------------------------------------------------ misc
    int itoa(int val, int buf, boolean signed, int len, int pad){
        len&=0xff; pad&=0xff;
        byte[] t=itoaBuf; int n=0;
        long v = signed ? val : (val & 0xffffffffL);
        boolean neg=v<0; if(neg) v=-v;
        do { t[n++]=(byte)('0'+(int)(v%10)); v/=10; } while(v!=0);
        if(neg) t[n++]='-';
        for(int i=0;i<n;i++) wr8(buf+i, t[n-1-i]);
        for(int k=n;k<len;k++) wr8(buf+k, pad);
        int nul=Math.max(n, len);
        wr8(buf+nul, 0);
        return buf+nul;
    }
    final byte[] itoaBuf=new byte[24];

    void writeDateTime(int p){
        Calendar c=Calendar.getInstance();
        wr16(p, c.get(Calendar.YEAR));
        wr16(p+2, c.get(Calendar.DAY_OF_MONTH));
        wr8(p+4, c.get(Calendar.MONTH)+1);
        wr8(p+5, c.get(Calendar.HOUR_OF_DAY));
        wr8(p+6, c.get(Calendar.MINUTE));
        wr8(p+7, c.get(Calendar.SECOND));
    }
    int getCaps(int type, int buf){
        if(buf==0) return 0;
        switch(type){
            case 0: wr16(buf, 8); wr16(buf+2, 7); wr16(buf+4, w); wr16(buf+6, h); return 1;   // Video: RGB332
            case 4: wr16(buf, 12); wr16(buf+2, 0x25); wr32(buf+4, (2<<16)|3); wr32(buf+8, 0); return 1;
            case 2: wr16(buf, 8); wr16(buf+2, 0xFFFF); wr32(buf+4, 3); return 1;
            case 3: wr16(buf, 4); wr16(buf+2, 0); return 1;
            default: wr16(buf, 8); wr16(buf+2, 0); wr32(buf+4, 0); return 1;
        }
    }

    // ------------------------------------------------------------------ heap
    final int heapBase, heapEnd; int nextHeap;
    final Hashtable liveBlocks=new Hashtable();     // Integer ptr -> Integer size
    final Vector freeList=new Vector();             // int[]{ptr,size}
    int alloc(int size){
        if(size<0) return 0;
        int sz=(size+7)&~7; if(sz==0) sz=8;
        for(int i=0;i<freeList.size();i++){
            int[] b=(int[])freeList.elementAt(i);
            if(b[1]>=sz){
                int p=b[0];
                if(b[1]==sz) freeList.removeElementAt(i); else { b[0]+=sz; b[1]-=sz; }
                liveBlocks.put(new Integer(p), new Integer(sz));
                zero(p, sz); return p;
            }
        }
        int p=nextHeap;
        if(p+sz>heapEnd) return 0;
        nextHeap+=sz; liveBlocks.put(new Integer(p), new Integer(sz));
        zero(p, sz); return p;
    }
    void zero(int p, int n){ for(int i=0;i<n;i++) wr8(p+i, 0); }
    void freePtr(int ptr){
        Integer so=(Integer)liveBlocks.remove(new Integer(ptr));
        if(so==null) return;
        int sz=so.intValue();
        if(ptr+sz==nextHeap){
            nextHeap=ptr;
            boolean merged=true;
            while(merged){
                merged=false;
                for(int i=0;i<freeList.size();i++){
                    int[] b=(int[])freeList.elementAt(i);
                    if(b[0]+b[1]==nextHeap){ nextHeap=b[0]; freeList.removeElementAt(i); merged=true; break; }
                }
            }
        } else freeList.addElement(new int[]{ptr, sz});
    }

    // ------------------------------------------------------------------ streams + saves (RMS)
    static final int SF_WRITE=0x200, SF_BINARY=0x400, SF_CREATE=0x800, SF_TRUNC=0x1000, SF_EXCL=0x2000;
    static final class Stream { byte[] data; int pos; String rec; boolean dirty; }
    final byte[][] resources;
    final Hashtable streams=new Hashtable();        // Integer fd -> Stream
    int nextFd=1;

    int streamOpen(int namePtr, int mode){
        int type=mode&0xFF;
        Stream s=new Stream();
        if(type==7){                                  // resource of the container (writable overlay in RMS)
            int resNum=mode>>>16;
            boolean write=(mode&SF_WRITE)!=0, zap=(mode&(SF_TRUNC|SF_CREATE))!=0;
            String rec="r:"+resNum;
            byte[] ov = (write && zap) ? null : Saves.load(rec);
            if(write && zap) s.data=new byte[0];
            else if(ov!=null) s.data=ov;
            else if(resNum<resources.length){ byte[] r=resources[resNum]; s.data=new byte[r.length]; System.arraycopy(r, 0, s.data, 0, r.length); }
            else if(write) s.data=new byte[0];
            else return -1;
            if(write) s.rec=rec;
        } else {                                      // named file (save game)
            StringBuffer sb=new StringBuffer();
            for(int k=0;k<64;k++){ int c=rd8(namePtr+k); if(c==0) break; sb.append((char)c); }
            String rec="f:"+sb.toString();
            boolean write=(mode&SF_WRITE)!=0, create=(mode&SF_CREATE)!=0, trunc=(mode&SF_TRUNC)!=0;
            byte[] old = (create||trunc) ? null : Saves.load(rec);
            if(create||trunc) s.data=new byte[0];
            else if(old!=null) s.data=old;
            else if(write) s.data=new byte[0];
            else if((mode&SF_BINARY)!=0 && (mode&SF_EXCL)==0) s.data=new byte[0];
            else return -1;
            if(write){ s.rec=rec; s.dirty=true; flush(s); }
        }
        int fd=nextFd++;
        streams.put(new Integer(fd), s);
        return fd;
    }
    Stream stream(int fd){ return (Stream)streams.get(new Integer(fd)); }
    int streamRead(int fd, int buf, int count){
        Stream s=stream(fd); if(s==null) return -1;
        int n=Math.min(count, s.data.length-s.pos); if(n<0) n=0;
        for(int i=0;i<n;i++) wr8(buf+i, s.data[s.pos+i]);
        s.pos+=n; return n;
    }
    int streamWrite(int fd, int buf, int count){
        Stream s=stream(fd); if(s==null || count<0) return -1;
        if(s.pos+count>s.data.length){
            byte[] nd=new byte[s.pos+count]; System.arraycopy(s.data, 0, nd, 0, s.data.length); s.data=nd;
        }
        for(int i=0;i<count;i++) s.data[s.pos+i]=(byte)rd8(buf+i);
        s.pos+=count;
        if(s.rec!=null) s.dirty=true;                 // written to RMS on close / pause / exit
        return count;
    }
    int streamSeek(int fd, int where, int whence){
        Stream s=stream(fd); if(s==null) return -1;
        int pos=s.pos;
        switch(whence){ case 0: pos=where; break; case 1: pos+=where; break; case 2: pos=s.data.length+where; break; }
        if(pos<0) pos=0; if(pos>s.data.length) pos=s.data.length;
        s.pos=pos; return pos;
    }
    void streamClose(int fd){
        Stream s=(Stream)streams.remove(new Integer(fd));
        if(s!=null) flush(s);
    }
    void flush(Stream s){
        if(s.rec!=null && s.dirty){ Saves.save(s.rec, s.data); s.dirty=false; }
    }
    /** persists every open writable stream (called when the MIDlet is paused or closed) */
    public void flushSaves(){
        for(Enumeration e=streams.elements(); e.hasMoreElements();) flush((Stream)e.nextElement());
    }

    int decompressStream(int fd, int dest){
        Stream st=stream(fd); if(st==null) return -1;
        byte[] s=st.data; int off=st.pos;
        if(s.length-off<22 || s[off]!='L' || s[off+1]!='Z') return -1;
        int maxB=s[off+2]&0xff, extB=s[off+3]&0xff;
        int usize=le32(s, off+4), csize=le32(s, off+8);
        bitSrc=s; bitBase=off+22; bitLen=csize; bitP=0; bitMem=false;
        int dp=lz(dest, usize, maxB, extB);
        st.pos=off+22+csize;
        return dp;
    }
    int decompress(int source, int dest){
        if(source==0 || rd8(source)!='L' || rd8(source+1)!='Z') return -1;
        int maxB=rd8(source+2), extB=rd8(source+3);
        int usize=rd32(source+4), csize=rd32(source+8);
        bitSrc=null; bitBase=source+22; bitLen=csize; bitP=0; bitMem=true;
        return lz(dest, usize, maxB, extB);
    }
    int lz(int dest, int usize, int maxB, int extB){
        int dp=0;
        while(dp<usize && bitP<bitLen*8){
            if(bits(1)==1){
                int v2=0; while(v2<maxB && bits(1)==1) v2++;
                int cl=2; if(v2!=0) cl=(bits(v2)|(1<<v2))+1;
                int bo = cl==2 ? bits(8)+2 : bits(extB)+cl;
                if(cl>usize-dp) cl=usize-dp;
                for(int i=0;i<cl;i++) wr8(dest+dp+i, rd8(dest+dp-bo+i));
                dp+=cl;
            } else { wr8(dest+dp, bits(8)); dp++; }
        }
        return dp;
    }
    byte[] bitSrc; int bitBase, bitLen, bitP; boolean bitMem;
    int bitByte(int i){
        if(bitMem) return rd8(bitBase+i);
        int idx=bitBase+i; return (idx>=0 && idx<bitSrc.length) ? bitSrc[idx]&0xff : 0;
    }
    /** MSB-first bit reader (same as Sdk.LZBits / ByteBits on the desktop) */
    int bits(int count){
        int result=0, left=count, pp=bitP;
        if((pp&7)!=0){
            int toE=Math.min(8-(pp&7), left);
            result=(bitByte(pp>>3)>>(8-((pp&7)+toE)))&(0xFF>>(8-toE)); left-=toE; pp+=toE;
        }
        if(left==0){ bitP=pp; return result; }
        while(left>=8){ result=(result<<8)|bitByte(pp>>3); left-=8; pp+=8; }
        if(left==0){ bitP=pp; return result; }
        result=(result<<left)|(bitByte(pp>>3)>>(8-left)); pp+=left; bitP=pp; return result;
    }
    static int le32(byte[] b, int o){ return (b[o]&0xff)|((b[o+1]&0xff)<<8)|((b[o+2]&0xff)<<16)|((b[o+3]&0xff)<<24); }

    // ------------------------------------------------------------------ sound (MMAPI, MIDI only)
    public boolean soundOn=true;
    Player current;
    final Hashtable players=new Hashtable();        // Integer key -> Player (small in-memory effects)
    Player streamPlayer;

    int playResource(int data, int length, int flags){
        try {
            if((flags&0x400)!=0){ stopSound(); return 1; }
            if(!soundOn) return 1;
            boolean loop=(flags&0x100)!=0;
            Player p;
            if((flags&0x200)!=0){                    // stream handle: music, not cached
                Stream s=stream(data); if(s==null) return 0;
                int n=Math.min(length, s.data.length-s.pos); if(n<4) return 1;
                byte[] b=new byte[n]; System.arraycopy(s.data, s.pos, b, 0, n); s.pos+=n;
                if(!isMidi(b, 0)) return 1;
                stopSound();
                if(streamPlayer!=null){ try { streamPlayer.close(); } catch(Throwable t){} streamPlayer=null; }
                p=streamPlayer=Manager.createPlayer(new ByteArrayInputStream(b), "audio/midi");
                p.realize();
            } else {
                if(length<4 || length>1<<20 || !inMem(data, length) || !isMidi(mem, data-base)) return 1;
                Integer key=new Integer(data^(length<<20));
                p=(Player)players.get(key);
                if(p==null){
                    if(players.size()>=8) releasePlayers();
                    byte[] b=new byte[length]; System.arraycopy(mem, data-base, b, 0, length);
                    p=Manager.createPlayer(new ByteArrayInputStream(b), "audio/midi");
                    p.realize();
                    players.put(key, p);
                }
                stopSound();
            }
            p.setLoopCount(loop ? -1 : 1);
            try { p.setMediaTime(0); } catch(Throwable t){}
            p.start();
            current=p;
            return 1;
        } catch(Throwable e){
            releasePlayers();                          // some phones allow few players: start over next time
            return 0;
        }
    }
    static boolean isMidi(byte[] b, int o){ return b[o]=='M' && b[o+1]=='T' && b[o+2]=='h' && b[o+3]=='d'; }
    public void stopSound(){
        Player p=current; current=null;
        if(p!=null){ try { if(p.getState()==Player.STARTED) p.stop(); } catch(Throwable t){} }
    }
    void releasePlayers(){
        stopSound();
        for(Enumeration e=players.elements(); e.hasMoreElements();){
            try { ((Player)e.nextElement()).close(); } catch(Throwable t){}
        }
        players.clear();
    }
    public void closeSound(){
        releasePlayers();
        if(streamPlayer!=null){ try { streamPlayer.close(); } catch(Throwable t){} streamPlayer=null; }
    }

    /** RMS-backed save files: one record per logical file ("r:9" = resource overlay, "f:name" = file) */
    static final class Saves {
        static Hashtable ids;                         // String -> Integer record id
        static RecordStore open() throws Exception {
            RecordStore rs=RecordStore.openRecordStore("wolphun", true);
            if(ids==null){
                ids=new Hashtable();
                RecordEnumeration en=rs.enumerateRecords(null, null, false);
                while(en.hasNextElement()){
                    int id=en.nextRecordId();
                    try {
                        DataInputStream in=new DataInputStream(new ByteArrayInputStream(rs.getRecord(id)));
                        ids.put(in.readUTF(), new Integer(id));
                    } catch(Exception ignore){}
                }
                en.destroy();
            }
            return rs;
        }
        static byte[] load(String name){
            RecordStore rs=null;
            try {
                rs=open();
                Integer id=(Integer)ids.get(name); if(id==null) return null;
                DataInputStream in=new DataInputStream(new ByteArrayInputStream(rs.getRecord(id.intValue())));
                in.readUTF();
                byte[] d=new byte[in.readInt()]; in.readFully(d);
                return d;
            } catch(Throwable t){ return null; }
            finally { if(rs!=null) try { rs.closeRecordStore(); } catch(Exception e){} }
        }
        static void save(String name, byte[] data){
            RecordStore rs=null;
            try {
                rs=open();
                ByteArrayOutputStream bo=new ByteArrayOutputStream();
                DataOutputStream o=new DataOutputStream(bo);
                o.writeUTF(name); o.writeInt(data.length); o.write(data); o.flush();
                byte[] rec=bo.toByteArray();
                Integer id=(Integer)ids.get(name);
                if(id!=null) rs.setRecord(id.intValue(), rec, 0, rec.length);
                else ids.put(name, new Integer(rs.addRecord(rec, 0, rec.length)));
            } catch(Throwable t){
                // storage full / denied: the game keeps running without the save
            } finally { if(rs!=null) try { rs.closeRecordStore(); } catch(Exception e){} }
        }
    }
}
