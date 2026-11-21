package wolphun.j2me;

import java.io.DataInputStream;
import java.io.InputStream;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;

import wolphun.decomp.rt.Pip2Runtime;

/**
 * Full-screen canvas + the single game thread.
 *
 * The decompiled game owns its main loop (it never returns until the game quits), so it runs on
 * one thread created here; everything it draws goes into an int[] framebuffer that vFlipScreen
 * blits with drawRGB/flushGraphics from that same thread. Key events only update a bit mask.
 */
public final class WCanvas extends GameCanvas implements Runnable {
    // mophun button bits (nofun KeyCode.cs / desktop Screen)
    static final int UP=0x01, DOWN=0x02, LEFT=0x04, RIGHT=0x08, FIRE=0x10, SELECT=0x20, FIRE2=0x100;

    final WMidlet midlet;
    Thread thread;
    volatile boolean paused, quit;
    volatile int keys;
    long pauseStart;

    int vw, vh;               // virtual (game) screen
    int[] fb;                 // game framebuffer, vw*vh
    int[] sbuf; int[] xmap; int sw, sh, ox, oy;   // scaled output (null = 1:1)
    Graphics gfx;
    Sdk sdk;
    String error;

    WCanvas(WMidlet m){
        super(false);
        midlet=m;
        setFullScreenMode(true);
    }

    void start(){
        if(thread!=null) return;
        thread=new Thread(this);
        thread.start();
    }

    // ------------------------------------------------------------------ game thread
    public void run(){
        Pip2Runtime game=null;
        try {
            // wait until the full-screen size is known (some phones report it only after the first show)
            for(int i=0;i<20 && (getWidth()<=0 || getHeight()<=0 || !isShown());i++) Thread.sleep(50);
            Thread.sleep(100);
            chooseScreen(getWidth(), getHeight());
            gfx=getGraphics();
            gfx.setColor(0); gfx.fillRect(0, 0, getWidth(), getHeight());

            game=GameInfo.create();
            DataInputStream in=new DataInputStream(open("/g.img"));
            if(in.readInt()!=0x574A324D) throw new RuntimeException("bad g.img");
            in.readInt();                                       // version
            game.base=in.readInt();
            int memSize=in.readInt();
            game.sp=in.readInt();
            int heapBase=in.readInt(), heapEnd=in.readInt();
            game.ctorList=in.readInt();
            boolean fmt7=in.readInt()!=0;
            game.mem=new byte[memSize];
            in.readFully(game.mem, 0, in.readInt());
            byte[][] res=new byte[in.readInt()][];
            for(int i=0;i<res.length;i++){ res[i]=new byte[in.readInt()]; in.readFully(res[i]); }
            in.close();

            sdk=new Sdk(this, game, vw, vh, fb, fmt7, heapBase, heapEnd, res);
            DataInputStream fin=new DataInputStream(open("/f.fnt"));
            sdk.loadFont(fin); fin.close();
            String snd=midlet.getAppProperty("Wolphun-Sound");
            if(snd!=null && snd.trim().equalsIgnoreCase("off")) sdk.soundOn=false;
            game.sdk=sdk;
            System.gc();
            game.run();                                         // returns when the game quits
        } catch(Throwable t){
            if(!quit){
                error=t.toString();
                showError();
                return;
            }
        } finally {
            if(sdk!=null){ sdk.flushSaves(); sdk.closeSound(); }
        }
        if(!quit) midlet.exit();                                // the game quit by itself
    }

    static InputStream open(String name){
        InputStream in=WCanvas.class.getResourceAsStream(name);
        if(in==null) throw new RuntimeException("missing "+name);
        return in;
    }

    /**
     * Picks the game resolution reported through vGetCaps. Multiscreen mophun games lay themselves
     * out for the size they are given; 176x208 and 128x160 are the classic ones. The picture is then
     * scaled to fit the canvas (keeping the aspect ratio) when that makes it noticeably bigger.
     * JAD/manifest "Wolphun-Screen: WxH" forces a size.
     */
    void chooseScreen(int cw, int ch){
        String forced=midlet.getAppProperty("Wolphun-Screen");
        vw=0;
        if(forced!=null){
            try {
                int x=forced.indexOf('x');
                vw=Integer.parseInt(forced.substring(0, x).trim()); vh=Integer.parseInt(forced.substring(x+1).trim());
            } catch(Exception e){ vw=0; }
        }
        if(vw<=0 || vh<=0){
            // the layouts HoneyCave 2 really has (bigger sizes keep the 208-line layout)
            if(cw>=176 && ch>=208){ vw=176; vh=208; }
            else if(ch>=160 || cw<128){ vw=128; vh=160; }
            else { vw=128; vh=128; }
        }
        fb=new int[vw*vh];
        // fit: scale = min(cw/vw, ch/vh) in 1/256 units; only scale up when the gain is >= 25%
        int sx=(cw<<8)/vw, sy=(ch<<8)/vh, s=Math.min(sx, sy);
        String sc=midlet.getAppProperty("Wolphun-Scale");
        boolean noScale = sc!=null && sc.trim().equalsIgnoreCase("off");
        if(s>=320 && !noScale){
            sw=(vw*s)>>8; sh=(vh*s)>>8;
            sbuf=new int[sw*sh];
            xmap=new int[sw];
            for(int x=0;x<sw;x++) xmap[x]=(x<<8)/s;
        } else { sw=vw; sh=vh; sbuf=null; }
        ox=(cw-sw)/2; oy=(ch-sh)/2;
        if(ox<0) ox=0; if(oy<0) oy=0;
    }

    /** vFlipScreen: shows the frame; blocks while the MIDlet is paused/hidden */
    void flip(){
        if(quit) throw new Pip2Runtime.Terminated("exit");
        if(paused){
            synchronized(this){
                while(paused && !quit){ try { wait(500); } catch(InterruptedException e){} }
            }
            if(quit) throw new Pip2Runtime.Terminated("exit");
            gfx.setColor(0); gfx.fillRect(0, 0, getWidth(), getHeight());
        }
        if(sbuf==null){
            gfx.drawRGB(fb, 0, vw, ox, oy, vw, vh, false);
        } else {
            int[] s=sbuf, f=fb, xm=xmap; int prev=-1;
            for(int y=0;y<sh;y++){
                int row=y*sw, sy=(y*vh)/sh;
                if(sy==prev){ System.arraycopy(s, row-sw, s, row, sw); continue; }
                prev=sy;
                int src=sy*vw;
                for(int x=0;x<sw;x++) s[row+x]=f[src+xm[x]];
            }
            gfx.drawRGB(s, 0, sw, ox, oy, sw, sh, false);
        }
        flushGraphics();
    }

    /** the game is polling the clock between frames: let the phone breathe */
    void nap(){
        try { Thread.sleep(1); } catch(InterruptedException e){}
        if(quit) throw new Pip2Runtime.Terminated("exit");
    }

    int buttons(){
        if(quit) throw new Pip2Runtime.Terminated("exit");
        return keys;
    }

    void vibrate(int ms){
        try { Display.getDisplay(midlet).vibrate(ms); } catch(Throwable t){}
    }

    /** vMsgBox: wait for release, press of FIRE/SELECT, release */
    void waitOk(){
        try {
            long t0=System.currentTimeMillis();
            while(keys!=0 && System.currentTimeMillis()-t0<3000) Thread.sleep(20);
            while((keys&(FIRE|SELECT))==0 && !quit) Thread.sleep(20);
            while(keys!=0 && !quit) Thread.sleep(20);
        } catch(InterruptedException e){}
    }

    // ------------------------------------------------------------------ lifecycle
    void pause(){
        if(paused) return;
        pauseStart=System.currentTimeMillis();
        paused=true;
        keys=0;
        if(sdk!=null){ sdk.stopSound(); sdk.flushSaves(); }
    }
    void resume(){
        if(!paused) return;
        if(sdk!=null) sdk.pausedMs+=System.currentTimeMillis()-pauseStart;
        synchronized(this){ paused=false; notifyAll(); }
    }
    void stop(){
        quit=true;
        synchronized(this){ notifyAll(); }
    }
    protected void hideNotify(){ pause(); }
    protected void showNotify(){ resume(); if(error!=null) showError(); }

    void showError(){
        try {
            Graphics gr=getGraphics();
            gr.setColor(0x000040); gr.fillRect(0, 0, getWidth(), getHeight());
            gr.setColor(0xffffff);
            gr.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
            String s="Error: "+error;
            int y=2, lh=gr.getFont().getHeight(), cw=getWidth()-4;
            int i=0;
            while(i<s.length() && y<getHeight()){
                int j=i+1;
                while(j<=s.length() && gr.getFont().substringWidth(s, i, j-i)<=cw) j++;
                gr.drawSubstring(s, i, j-1-i, 2, y, Graphics.TOP|Graphics.LEFT);
                y+=lh; i=j-1;
            }
            flushGraphics();
        } catch(Throwable t){}
    }

    // ------------------------------------------------------------------ keys
    protected void keyPressed(int k){
        if(error!=null){ midlet.exit(); return; }
        keys|=map(k);
    }
    protected void keyReleased(int k){ keys&=~map(k); }

    int map(int k){
        switch(k){
            case KEY_NUM2: return UP;
            case KEY_NUM8: return DOWN;
            case KEY_NUM4: return LEFT;
            case KEY_NUM6: return RIGHT;
            case KEY_NUM5: return FIRE;
            case KEY_NUM1: return UP|LEFT;
            case KEY_NUM3: return UP|RIGHT;
            case KEY_NUM7: return DOWN|LEFT;
            case KEY_NUM9: return DOWN|RIGHT;
            case KEY_NUM0: return FIRE2;
            case KEY_STAR: return SELECT;
            case KEY_POUND: return FIRE2;
            case -6: case -21: case 21: case 57345: return SELECT;   // left softkey (Nokia/SE/Samsung, Motorola, Siemens)
            case -7: case -22: case 22: case 57346: return FIRE2;    // right softkey
            case -5: case -20: case -26: case 20: return FIRE;       // centre / OK
            default: break;
        }
        int ga=0;
        try { ga=getGameAction(k); } catch(Throwable t){}
        switch(ga){
            case UP_ACTION:    // GameCanvas.UP etc. are Canvas game actions; avoid name clash with our bits
                return UP;
            case DOWN_ACTION:  return DOWN;
            case LEFT_ACTION:  return LEFT;
            case RIGHT_ACTION: return RIGHT;
            case FIRE_ACTION:  return FIRE;
            default: return 0;
        }
    }
    static final int UP_ACTION=1, DOWN_ACTION=6, LEFT_ACTION=2, RIGHT_ACTION=5, FIRE_ACTION=8;
}
