package hc2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Vector;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.rms.RecordStore;

/**
 * Honey Cave 2, native MIDP 2.0 version.
 *
 * The game runs on one thread (run()). Like the original it owns its main loop: a 75 ms tick that
 * runs the current state and shows the frame; menus, the highscore table and message boxes are
 * blocking sub-loops on the same thread. Everything is drawn with lcdui into an off-screen image of
 * the logical screen (128x128, 128x160 or 176x208, the layouts the game has), which is then shown
 * 1:1 or scaled to the canvas.
 *
 * Game objects live in parallel arrays (48 slots, slot 32 = the player, 33 = the player's sting);
 * what the original kept as function pointers are the F_* behaviour codes. Sprites are drawn from
 * 48 "sprite slots" (position + image) exactly like the original sprite engine, which also does
 * the collision tests (slot boxes are x, y, width, height of the image, not offset by the hot spot).
 */
final class Game extends GameCanvas implements Runnable {

    // ------------------------------------------------------------------ keys
    static final int K_UP=0x01, K_DOWN=0x02, K_LEFT=0x04, K_RIGHT=0x08, K_FIRE=0x10, K_SELECT=0x20, K_FIRE2=0x100;

    // ------------------------------------------------------------------ states (main loop)
    static final int ST_MENU=0, ST_NEW=1, ST_RESUME=2, ST_START=3, ST_INTRO=4, ST_PLAY=5,
                     ST_DEATH=6, ST_LEVEL_DONE=7, ST_GAME_OVER=8;

    // ------------------------------------------------------------------ object behaviours
    static final int F_NONE=0, F_PLAYER=1, F_PLAYER_DEATH=2, F_EXIT=3, F_POP=4, F_SCORE=5, F_LIFE=6,
                     F_WALKER=7, F_FLYER=8, F_WAVER=9, F_SHOOTER=10, F_BULLET=11, F_SWITCH=12,
                     F_PLATFORM=13, F_QUEEN=14, F_BOSS2=15, F_BOSS3=16, F_BOSS4=17, F_BOSS_DIE=18,
                     F_STING=19, F_EXPL_S=20, F_EXPL_B=21;
    /** boss spawners chosen by the level map (object types 6..9) */
    static final int BOSS_NONE=0, BOSS_QUEEN=1, BOSS_2=2, BOSS_3=3, BOSS_4=4;

    // ------------------------------------------------------------------ sprite table entries
    static final int SP_PLAYER=0, SP_EXPL_S0=16, SP_EXPL_S=19, SP_EXPL_B0=20, SP_EXPL_B=25,
                     SP_SUN=26, SP_MOON=27, SP_PLATFORM=28, SP_SWITCH=29, SP_HONEY=31, SP_SHOOTER=36,
                     SP_WAVER=46, SP_FLYER=48, SP_WALKER=50, SP_BULLET=52, SP_STING=53, SP_SCORE=54,
                     SP_EXIT=56, SP_LIFE=58, SP_QUEEN=61, SP_QUEEN_START=64, SP_BOSS2=67,
                     SP_BOSS2_START=71, SP_BOSS3=75, SP_BOSS3_START=79, SP_BOSS4=81;

    // ------------------------------------------------------------------ texts (language resource order)
    static final int T_SURE=0, T_NEW=1, T_RESUME=2, T_HIGHSCORE=3, T_QUIT=4, T_LEVEL_DONE=5,
                     T_SPACE=9, T_RUB=10, T_END=11, T_BACK=12, T_CLEAR=13, T_GAME_OVER=15,
                     T_GET_READY=18, T_HELP=20;

    // ------------------------------------------------------------------ colours (RGB555 of the original)
    static final int C_CYAN=0x03FF, C_WHITE=0x7FFF, C_MENU_BG=0x1800, C_BAR_OUT=0x1DEF, C_BAR_IN=0x210A,
                     C_CAROUSEL=0x7FEF;

    static final char[] NAME_CHARS="ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    static final class Quit extends RuntimeException { }

    final HC2 midlet;
    final Res res=new Res();
    Thread thread;
    volatile boolean paused, quit;
    volatile int keysHeld, keysLatched;
    String error;

    // ------------------------------------------------------------------ screen
    int W, H;                       // logical screen
    Image buf; Graphics g;          // off-screen frame
    Graphics screen;
    int[] fb, sbuf, xmap; int sw, sh, ox, oy;   // scaled output (sbuf == null: 1:1)
    Font font; int fh;              // system font and its height
    int clipX, clipY, clipW, clipH;

    // ------------------------------------------------------------------ maps
    Res.Layer menuLayer, levelLayer, hudLayer;
    int levelMap;
    int frameAnimate, framePassed;

    // ------------------------------------------------------------------ sprite slots
    static final int SLOTS=48;
    final int[] sSpr=new int[SLOTS], sX=new int[SLOTS], sY=new int[SLOTS];

    // ------------------------------------------------------------------ objects
    final int[] oSpr=new int[SLOTS], oFn=new int[SLOTS], oB4=new int[SLOTS], oB5=new int[SLOTS],
                oB7=new int[SLOTS], oV8=new int[SLOTS], oX=new int[SLOTS], oY=new int[SLOTS],
                oVX=new int[SLOTS], oVY=new int[SLOTS];
    static final int PL=32, STING=33;

    // ------------------------------------------------------------------ game state
    int state=ST_MENU, menuReq;
    boolean resumeFlag, introFlag, sound=true, vibra=true;
    int level, frameCnt, sndCool, keys, randV, hour;
    int honey, lives, score;
    int px, py, pyPrev, camX, camY, maxCamX, maxCamY, startX, startY;
    boolean bossLock;
    int bossMode, bossCnt, bossObj, bossTX, bossTY, bossX, bossY, bossHP, bossFn;
    int spawnMask, fontPal=1;
    int sunSpr, sunY;
    int pbx, pby; static final int PBW=12, PBH=14;          // player hit box
    int sbx, sby; static final int SBW=50, SBH=4;           // sting box
    final int[] dirX=new int[32], dirY=new int[32];
    long rng;

    // ------------------------------------------------------------------ menu / highscore layout
    int menuX, menuY, menuW, menuH;
    int hsX, hsY, hsW, hsH, hsTop, hsBottom, hsRowH, hsVisible, hsScroll, blinkDir, blinkIdx;
    final byte[][] hsName=new byte[6][4];
    final int[] hsScore=new int[6], hsLevel=new int[6];

    // ------------------------------------------------------------------ sound
    final Player[] players=new Player[8];
    Player playing;

    final char[] digits=new char[12];

    Game(HC2 m){
        super(false);
        midlet=m;
        setFullScreenMode(true);
    }

    void start(){
        if(thread!=null) return;
        thread=new Thread(this);
        thread.start();
    }

    // ================================================================== thread
    public void run(){
        try {
            for(int i=0;i<20 && (getWidth()<=0 || getHeight()<=0 || !isShown());i++) Thread.sleep(50);
            Thread.sleep(100);
            screen=getGraphics();
            screen.setColor(0); screen.fillRect(0, 0, getWidth(), getHeight());
            chooseScreen(getWidth(), getHeight());
            res.load();
            gameMain();
        } catch(Quit q){
            // normal exit
        } catch(Throwable t){
            if(!quit){ error=t.toString(); showError(); return; }
        } finally {
            closeSound();
        }
        if(!quit) midlet.exit();
    }

    void exitGame(){
        if(isGameState()) saveResume();
        throw new Quit();
    }

    boolean isGameState(){ return state==ST_INTRO || state==ST_PLAY; }

    /**
     * Picks the logical screen: the three layouts of the game (176x208, 128x160, 128x128); the
     * picture is scaled up (nearest neighbour) when that makes it at least 25% bigger.
     * JAD "HC2-Screen: WxH" forces a size, "HC2-Scale: off" disables scaling.
     */
    void chooseScreen(int cw, int ch){
        String forced=midlet.getAppProperty("HC2-Screen");
        W=0;
        if(forced!=null){
            try {
                int x=forced.indexOf('x');
                W=Integer.parseInt(forced.substring(0, x).trim()); H=Integer.parseInt(forced.substring(x+1).trim());
            } catch(Exception e){ W=0; }
        }
        if(W<=0 || H<=0){
            if(cw>=176 && ch>=208){ W=176; H=208; }
            else if(ch>=160 || cw<128){ W=128; H=160; }
            else { W=128; H=128; }
        }
        if(H>216) H=216;
        buf=Image.createImage(W, H);
        g=buf.getGraphics();
        int s=Math.min((cw<<8)/W, (ch<<8)/H);
        String sc=midlet.getAppProperty("HC2-Scale");
        boolean noScale = sc!=null && sc.trim().equalsIgnoreCase("off");
        if(s>=320 && !noScale){
            sw=(W*s)>>8; sh=(H*s)>>8;
            fb=new int[W*H]; sbuf=new int[sw*sh]; xmap=new int[sw];
            for(int x=0;x<sw;x++) xmap[x]=(x<<8)/s;
        } else { sw=W; sh=H; }
        ox=(cw-sw)/2; oy=(ch-sh)/2;
        if(ox<0) ox=0; if(oy<0) oy=0;
        String snd=midlet.getAppProperty("HC2-Sound");
        if(snd!=null && snd.trim().equalsIgnoreCase("off")) sound=false;
        String vib=midlet.getAppProperty("HC2-Vibra");
        if(vib!=null && vib.trim().equalsIgnoreCase("off")) vibra=false;
    }

    /** shows the frame; blocks while the MIDlet is paused / hidden */
    void flip(){
        if(quit) exitGame();
        if(paused){
            synchronized(this){ while(paused && !quit){ try { wait(500); } catch(InterruptedException e){} } }
            if(quit) exitGame();
            screen.setColor(0); screen.fillRect(0, 0, getWidth(), getHeight());
        }
        if(sbuf==null){
            screen.drawImage(buf, ox, oy, Graphics.TOP|Graphics.LEFT);
        } else {
            buf.getRGB(fb, 0, W, 0, 0, W, H);
            int[] s=sbuf, f=fb, xm=xmap; int prev=-1;
            for(int y=0;y<sh;y++){
                int row=y*sw, sy=(y*H)/sh;
                if(sy==prev){ System.arraycopy(s, row-sw, s, row, sw); continue; }
                prev=sy;
                int src=sy*W;
                for(int x=0;x<sw;x++) s[row+x]=f[src+xm[x]];
            }
            screen.drawRGB(s, 0, sw, ox, oy, sw, sh, false);
        }
        flushGraphics();
    }

    /** current buttons (held, plus presses shorter than a poll) */
    int readKeys(){
        if(quit) exitGame();
        int k=keysHeld|keysLatched;
        keysLatched=0;
        return k;
    }
    /** a short sleep, then the buttons (the blocking loops of menus and screens) */
    int pollKeys(){
        try { Thread.sleep(20); } catch(InterruptedException e){}
        if(paused) flip();
        return readKeys();
    }
    void waitRelease(){ while(pollKeys()!=0) { } }
    void waitPress(){ while(pollKeys()==0) { } }
    static long now(){ return System.currentTimeMillis(); }

    // ================================================================== main loop
    void gameMain() throws IOException {
        init();
        long next=now();
        while(true){
            randV=random()&0xFFFF;
            frameCnt=(frameCnt+1)&0xFF;
            if(!sound) sndCool=1; else if(sndCool!=0) sndCool--;
            keys=readKeys();
            if((keys&K_SELECT)!=0){
                waitRelease();
                if(state!=ST_MENU && resumeFlag){
                    saveResume();
                    menuReq=1;
                    state=ST_MENU;
                }
            }
            switch(state){
                case ST_MENU:       stMenu(); break;
                case ST_NEW:        stNewGame(); break;
                case ST_RESUME:     stResume(); break;
                case ST_START:      startLevel(); break;
                case ST_INTRO:      stIntro(); break;
                case ST_PLAY:       stPlay(); break;
                case ST_DEATH:      stDeath(); break;
                case ST_LEVEL_DONE: stLevelDone(); break;
                case ST_GAME_OVER:  stGameOver(); break;
            }
            flip();
            while(now()<next){
                try { Thread.sleep(5); } catch(InterruptedException e){}
            }
            next=now()+75;
        }
    }

    void init(){
        font=Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        fh=font.getHeight();
        g.setFont(font);
        fullClip();
        loadResumeHeader();
        hsLayout(0, 0, W, H);
        loadHighscores();
        state=ST_MENU;
        menuReq=2;
        // 32 unit vectors (the queen bee steers with them)
        int x=0, y=-256, dx=32, dy=32;
        for(int i=0;i<32;i++){
            int r=isqrt(y*y+x*x);
            if(i==8) dx=-dx;
            if(i==16) dy=-dy;
            if(i==24) dx=-dx;
            int k=div(0x10000, r);
            dirX[i]=(short)((x*k)>>7);
            dirY[i]=(short)((y*k)>>7);
            x+=dx; y+=dy;
        }
    }

    static int isqrt(int v){
        int rem=0, root=0;
        for(int i=0;i<16;i++){
            root<<=1;
            rem=(rem<<2)|(v>>>30);
            v<<=2;
            int t=(root<<1)+1;
            if(rem>=t){ root++; rem-=t; }
        }
        return root;
    }
    static int div(int a, int b){ return b==0 ? 0 : a/b; }

    int random(){
        rng=rng*6364136223846793005L+1442695040888963407L;
        return (short)(rng>>>33);
    }

    // ================================================================== drawing primitives
    static int rgb555(int v){
        int r=(v>>10)&31, gg=(v>>5)&31, b=v&31;
        return (((r*510+31)/62)<<16)|(((gg*510+31)/62)<<8)|((b*510+31)/62);
    }
    void color(int c555){ g.setColor(rgb555(c555)); }
    /** inclusive clip window like the original */
    void clip(int x0, int y0, int x1, int y1){
        if(x0>x1){ int t=x0; x0=x1; x1=t; }
        if(y0>y1){ int t=y0; y0=y1; y1=t; }
        clipX=x0; clipY=y0; clipW=x1-x0+1; clipH=y1-y0+1;
        g.setClip(clipX, clipY, clipW, clipH);
    }
    void fullClip(){ clip(0, 0, W-1, H-1); }
    /** clears the whole screen whatever the clip */
    void clear(int rgb){
        g.setClip(0, 0, W, H);
        g.setColor(rgb);
        g.fillRect(0, 0, W, H);
        g.setClip(clipX, clipY, clipW, clipH);
    }
    /** inclusive rectangle */
    void fill(int x0, int y0, int x1, int y1){
        if(x0>x1){ int t=x0; x0=x1; x1=t; }
        if(y0>y1){ int t=y0; y0=y1; y1=t; }
        g.fillRect(x0, y0, x1-x0+1, y1-y0+1);
    }
    void text(String s, int x, int y){ g.drawString(s, x, y, Graphics.TOP|Graphics.LEFT); }
    int tw(String s){ return font.stringWidth(s); }
    /** white text centred horizontally */
    void centerText(int y, String s){
        g.setColor(0xffffff);
        text(s, (W-tw(s))>>1, y);
    }

    /** the game's 8x8 digit font (score, lives) */
    void printDigits(int x, int y, int n){
        int row = fontPal==1 ? 0 : 8;
        for(int i=0;i<n;i++){
            int c=digits[i];
            int gi = c<res.fontMap.length ? res.fontMap[c]&0xff : 0xff;
            if(gi!=0xff) g.drawRegion(res.font, gi*8, row, 8, 8, 0, x, y, Graphics.TOP|Graphics.LEFT);
            x+=8;
        }
    }
    /** unsigned decimal padded with '0' to len digits into digits[]; returns the length */
    int utoa(long v, int len){
        v&=0xffffffffL;
        int n=0;
        do { digits[n++]=(char)('0'+(int)(v%10)); v/=10; } while(v!=0 && n<10);
        // reverse
        for(int i=0;i<n/2;i++){ char c=digits[i]; digits[i]=digits[n-1-i]; digits[n-1-i]=c; }
        if(n<len){
            for(int i=n-1;i>=0;i--) digits[i+len-n]=digits[i];
            for(int i=0;i<len-n;i++) digits[i]='0';
            n=len;
        }
        return n;
    }
    static String pad(int v, int len){
        String s=String.valueOf(v);
        while(s.length()<len) s="0"+s;
        return s;
    }

    // ------------------------------------------------------------------ sprites
    int spr(int k){ return res.spr(k, level); }
    void setSprite(int slot, int id, int x, int y){
        slot&=0xff;
        if(slot>=SLOTS) return;
        sSpr[slot]=id; sX[slot]=(short)x; sY[slot]=(short)y;
    }
    void clearSprites(){ for(int i=0;i<SLOTS;i++) sSpr[i]=0; }
    void drawSprites(){
        for(int i=0;i<SLOTS;i++) if(sSpr[i]!=0) res.drawSprite(g, sSpr[i], sX[i], sY[i]);
    }
    /** first slot in from..to whose image box overlaps the box, -1 if none */
    int boxHit(int bx, int by, int bw, int bh, int from, int to){
        from&=0xff; to&=0xff;
        if(to>=SLOTS) to=SLOTS-1;
        for(int i=from;i<=to;i++){
            int id=sSpr[i]; if(id==0) continue;
            int x=sX[i], y=sY[i];
            if(bx<x+res.sW[id] && bx+bw>x && by<y+res.sH[id] && by+bh>y) return i;
        }
        return -1;
    }
    boolean touchesPlayer(int slot){ return boxHit(pbx, pby, PBW, PBH, slot, slot)!=-1; }
    /** collision of the player's sprite (slot 32) with a slot */
    boolean playerSpriteHits(int slot){
        int id=sSpr[PL]; if(id==0) return false;
        return boxHit(sX[PL], sY[PL], res.sW[id], res.sH[id], slot, slot)!=-1;
    }
    void showObj(int i){ setSprite(i, oSpr[i], oX[i]-camX, oY[i]-camY); }

    // ------------------------------------------------------------------ maps
    void paintLayer(Res.Layer L, int x, int y){
        if(L.count!=0) L.sync(frameAnimate);
        L.tl.setPosition(x, y);
        L.tl.paint(g);
        if(L.anim!=0 && ++framePassed>L.anim){ frameAnimate=(frameAnimate+1)&3; framePassed=0; }
    }
    /** background of the menu and highscore screens */
    void menuBg(){
        color(C_MENU_BG);
        g.fillRect(0, 0, W, H);
        for(int x=0;x<W;x+=128) paintLayer(menuLayer, x, 0);
    }
    int attr(int x, int y){ return res.attr(levelMap, x, y); }

    // ------------------------------------------------------------------ sound / vibra
    void play(int i){
        if(!sound) return;
        try {
            Player p=players[i];
            if(p==null){
                p=Manager.createPlayer(Res.open("/m"+i+".mid"), "audio/midi");
                p.realize();
                p.prefetch();
                players[i]=p;
            }
            if(playing!=null && playing!=p){ try { playing.stop(); } catch(Exception e){} }
            p.stop();
            p.setMediaTime(0);
            p.start();
            playing=p;
        } catch(Throwable t){
            sound=false;
        }
    }
    void closeSound(){
        for(int i=0;i<players.length;i++){
            if(players[i]!=null){ try { players[i].close(); } catch(Throwable t){} players[i]=null; }
        }
        playing=null;
    }
    void vibrate(){
        if(!vibra) return;
        try { Display.getDisplay(midlet).vibrate(300); } catch(Throwable t){}
    }
    void sndLife()     { if(sndCool==0){ play(3); sndCool=12; } }
    void sndScore()    { if(sndCool==0){ play(7); sndCool=4; } }
    void sndExplosion(){ if(sound) play(2); }
    void sndDeath()    { if(sndCool==0){ play(0); sndCool=12; } }
    void sndSting()    { if(sndCool==0){ play(1); sndCool=5; } }
    void sndSwitch()   { if(sndCool==0) play(4); }
    void sndExit()     { play(5); sndCool=20; }
    void sndHoney()    { if(sndCool==0){ play(6); sndCool=6; } }

    // ================================================================== states
    /** loading picture */
    void hourglass(){
        clear(0);
        res.drawSprite(g, res.hourglass, (W>>1)-4, (H>>1)-7);
        flip();
    }

    void stMenu(){
        clear(rgb555(C_MENU_BG));
        if(menuReq!=0){
            hourglass();
            if(menuLayer==null) menuLayer=res.layer(Res.MAP_MENU);
            menuReq=0;
        }
        String[] items = resumeFlag
            ? new String[]{ res.str[T_RESUME], res.str[T_NEW], res.str[T_HIGHSCORE], res.str[T_HELP], res.str[T_QUIT] }
            : new String[]{ res.str[T_NEW], res.str[T_HIGHSCORE], res.str[T_HELP], res.str[T_QUIT] };
        menuBox(items.length);
        int sel=listMenu(items, menuX, menuY, menuW, menuH, 0);
        if(sel<0) exitGame();
        if(resumeFlag) sel--;
        switch(sel+1){
            case 0: state=ST_RESUME; break;
            case 1: state=ST_NEW; break;
            case 2: highscores(-1); break;
            case 3: msgBox(res.str[T_HELP], res.help, false); break;
            default: exitGame();
        }
    }

    /** layout of a menu of n lines at the bottom of the screen */
    void menuBox(int n){
        int h=n*fh;
        menuY=H-h-3;
        if(menuY<0){ menuY=0; menuH=H; } else menuH=h;
        if(W<=127){ menuX=0; menuW=W; } else { menuX=10; menuW=108; }
    }

    void stNewGame(){
        hourglass();
        lives=3;
        honey=39;
        score=0;
        clearObjects();
        clearSprites();
        resumeFlag=true;
        state=ST_START;
        level=0;
    }

    void stResume(){
        hourglass();
        clearObjects();
        clearSprites();
        startLevel();
        if(!loadResume()){                      // nothing to resume: plain restart of the level
            resumeFlag=true;
        }
    }

    void stLevelDone(){
        clear(0);
        centerText((H>>1)-fh, res.str[T_LEVEL_DONE+(level&3)]);
        level++;
        clearObjects();
        clearSprites();
        honey=39;
        flip();
        if(level<=3) startLevel();
        else { state=ST_GAME_OVER; clearResume(); }
        waitPress();
        waitRelease();
    }

    void stIntro(){
        if(introFlag){
            fontPal=1;
            stPlay();
            introFlag=false;
            stPlay();
        }
        fontPal = (frameCnt&4)!=0 ? 9 : 1;
        centerText(H>>1, res.str[T_GET_READY]);
        if((keys&K_FIRE)!=0){
            flip();
            waitRelease();
            state=ST_PLAY;
            fontPal=1;
        }
    }

    void stDeath(){
        lives=(lives-1)&0xff;
        state = lives==0 ? ST_GAME_OVER : ST_INTRO;
        sndDeath();
        introFlag=true;
        if(bossObj!=0){
            oSpr[bossObj]=0;
            setSprite(bossObj, 0, 0, 0);
            bossObj=0;
        }
        resetPlayer();
    }

    void stGameOver(){
        clearResume();
        clear(0);
        centerText((H-fh)>>1, res.str[T_GAME_OVER]);
        flip();
        while(pollKeys()==K_FIRE) { }
        while(pollKeys()!=K_FIRE) { }
        if(menuLayer==null) menuLayer=res.layer(Res.MAP_MENU);
        menuReq=0;
        int row=insertHighscore(score, level+1);
        highscores(row);
        saveHighscores();
        state=ST_MENU;
        level=0;
    }

    // ================================================================== levels
    void startLevel(){
        hour=0;
        try { res.loadBosses(level); } catch(IOException e){ throw new RuntimeException(e.toString()); }
        levelMap=Res.MAP_LEVEL+level;
        levelLayer=null;
        System.gc();
        levelLayer=res.layer(levelMap);
        if(hudLayer==null) hudLayer=res.layer(W>127 ? Res.MAP_HUD_WIDE : Res.MAP_HUD_SMALL);
        spawnMask = level==0 ? 31 : level==1 ? 15 : level==2 ? 7 : 3;
        maxCamX=(short)((res.mW[levelMap]<<3)-W);
        maxCamY=(short)((res.mH[levelMap]<<3)-H+16);
        rng=now()&0xffffffffL;
        spawnObjects();
        resetPlayer();
        state=ST_INTRO;
        introFlag=true;
        fullClip();
    }

    void clearObjects(){
        for(int i=0;i<SLOTS;i++){ oSpr[i]=0; setSprite(i, 0, 0, 0); }
    }
    int freeLow(){ for(int i=1;i<=31;i++) if(oSpr[i]==0) return i; return 0; }
    int freeHigh(){ for(int i=34;i<=47;i++) if(oSpr[i]==0) return i; return 0; }
    int freeTop(){ for(int i=47;i>33;i--) if(oSpr[i]==0) return i; return 0; }

    /** creates the objects of the level map */
    void spawnObjects(){
        bossX=0; bossY=0;
        int m=levelMap;
        int[] kind=res.oKind[m], tx=res.oX[m], ty=res.oY[m];
        for(int k=0;k<kind.length;k++){
            int t=kind[k]&0x1F, x=tx[k]<<3, y=ty[k]<<3, s;
            switch(t){
                case 0:
                    startX=(short)x; startY=(short)(y-4);
                    break;
                case 1: case 2: {
                    s=freeLow(); if(s==0) break;
                    oSpr[s]=spr(SP_PLATFORM); oFn[s]=F_PLATFORM;
                    oX[s]=(short)x; oY[s]=(short)y; oB4[s]=0;
                    oB5[s]=(kind[k]>>>28)&0xff; oB7[s]=(oB5[s]<<3)&0xff;
                    switch((kind[k]>>>24)&3){
                        case 0: oVX[s]=0; oVY[s]=2; break;
                        case 1: oVX[s]=2; oVY[s]=0; break;
                        case 2: oVX[s]=0; oVY[s]=-2; break;
                        default: oVX[s]=-2; oVY[s]=0; break;
                    }
                    if((kind[k]&0xFFFF00)==0){ oB4[s]=1; break; }       // always moving
                    int sw=freeLow(); if(sw==0) break;
                    oSpr[sw]=spr(SP_SWITCH); oFn[sw]=F_SWITCH;
                    oX[sw]=((kind[k]>>>8)&0xFF)<<3; oY[sw]=((kind[k]>>>16)&0xFF)<<3;
                    oB4[sw]=0; oB5[sw]=s; oB7[sw]=0;
                    break;
                }
                case 3:
                    s=freeLow(); if(s==0) break;
                    oSpr[s]=spr(SP_SCORE); oFn[s]=F_SCORE; oX[s]=(short)x; oY[s]=(short)y; oV8[s]=100;
                    setSprite(s, oSpr[s], x, y);
                    break;
                case 4:
                    s=freeLow(); if(s==0) break;
                    oSpr[s]=spr(SP_LIFE); oFn[s]=F_LIFE; oX[s]=(short)x; oY[s]=(short)y; oB7[s]=0;
                    setSprite(s, oSpr[s], x, y);
                    break;
                case 5:
                    s=freeHigh(); if(s==0) break;
                    oSpr[s]=spr(SP_WALKER); oFn[s]=F_WALKER; oX[s]=(short)x; oY[s]=(short)y;
                    oB7[s]=0; oB4[s]=0; oV8[s]=12;
                    setSprite(s, oSpr[s], x, y);
                    break;
                case 6: bossX=x; bossY=y; bossHP=200; bossFn=BOSS_2; break;
                case 7: bossX=x; bossY=y; bossHP=100; bossFn=BOSS_QUEEN; break;
                case 8: bossX=x; bossY=y; bossHP=300; bossFn=BOSS_3; break;
                case 9: bossX=x; bossY=y+14; bossHP=400; bossFn=BOSS_4; break;
                case 10:
                    s=freeHigh(); if(s==0) break;
                    oSpr[s]=spr(SP_SHOOTER); oFn[s]=F_SHOOTER;
                    oX[s]=(short)(x-8); oY[s]=(short)(y-8);
                    oB7[s]=0; oB4[s]=0; oB5[s]=0; oV8[s]=20;
                    setSprite(s, oSpr[s], oX[s], oY[s]);
                    break;
                case 15:
                    s=freeLow(); if(s==0) break;
                    oSpr[s]=spr(SP_EXIT); oFn[s]=F_EXIT; oX[s]=(short)x; oY[s]=(short)y; oB4[s]=0;
                    break;
                default: break;
            }
        }
    }

    /** player back at the start of the level (new level, after a death) */
    void resetPlayer(){
        honey=39;
        px=startX; py=startY;
        bossLock=false;
        pyPrev=startY;
        camera();
        skyBody();
        oFn[PL]=F_PLAYER;
        oSpr[PL]=spr(SP_PLAYER);
        oVY[PL]=0; oB4[PL]=0; oB5[PL]=30;
    }

    /** sun by day (height from the hour), moon at night */
    void skyBody(){
        Calendar c=Calendar.getInstance();
        hour=c.get(Calendar.HOUR_OF_DAY);
        sunY=20;
        if(((hour+250)&0xFF)>12) sunSpr=spr(SP_MOON);
        else {
            int d=(byte)(hour-12);
            if(d<0) d=-d;
            sunY=d<<3;
            sunSpr=spr(SP_SUN);
        }
    }

    // ================================================================== playing
    void stPlay(){
        clear(rgb555(res.sky[hour%24]));
        if((frameCnt&0x1F)==31) skyBody();
        randomSpawns();
        frame();
    }

    void frame(){
        update(PL);
        int pop=0;
        for(int i=0;i<SLOTS;i++){
            if(oSpr[i]==0) continue;
            if(oFn[i]==F_POP) pop=i;
            else if(i!=PL) update(i);
        }
        // sky body, moving across the screen with the camera
        int r=div((maxCamX-camX)<<16, maxCamX);
        res.drawSprite(g, sunSpr, ((W-20)*r)>>16, sunY);
        // level map above the status bar, sprites, status bar
        clip(0, 0, W, H-17);
        paintLayer(levelLayer, -camX, -camY);
        clip(0, 0, W, H);
        drawSprites();
        clip(0, H-24, W, H);
        paintLayer(hudLayer, 0, H-24);
        clip(0, 0, W, H);
        if(pop!=0) update(pop);
        if((honey&0xff)!=0) res.drawSprite(g, spr(SP_HONEY+((byte)honey>>3)), 4, H-20);
        int n=utoa(((byte)lives)-1, 2);
        printDigits(W>127 ? 44 : 33, H-10, n);
        n=utoa(score&0xFFFF, 5);
        if(W>127) printDigits(80, H-14, n); else printDigits(W-41, H-10, n);
    }

    void update(int i){
        switch(oFn[i]){
            case F_PLAYER:       player(); break;
            case F_PLAYER_DEATH: playerDeath(i); break;
            case F_EXIT:         exitDoor(i); break;
            case F_POP:          pop(i); break;
            case F_SCORE:        scoreItem(i); break;
            case F_LIFE:         lifeItem(i); break;
            case F_WALKER:       walker(i); break;
            case F_FLYER:        flyer(i); break;
            case F_WAVER:        waver(i); break;
            case F_SHOOTER:      shooter(i); break;
            case F_BULLET:       bullet(i); break;
            case F_SWITCH:       lever(i); break;
            case F_PLATFORM:     platform(i); break;
            case F_QUEEN:        queen(i); break;
            case F_BOSS2:        boss2(i); break;
            case F_BOSS3:        boss3(i); break;
            case F_BOSS4:        boss4(i); break;
            case F_BOSS_DIE:     bossDie(i); break;
            case F_STING:        sting(i); break;
            case F_EXPL_S:       explosion(i, SP_EXPL_S0); break;
            case F_EXPL_B:       explosion(i, SP_EXPL_B0); break;
            default: break;
        }
    }

    /** flying enemies coming in from the sides now and then */
    void randomSpawns(){
        if((frameCnt&0x20)==0) return;
        frameCnt&=0x1F;
        if((randV&spawnMask)!=spawnMask) return;
        int r=random();
        randV=r&0xFFFF;
        if((r&3)==0){
            randV=random()&0xFFFF;
            int x=camX+W, y0=py, dy=0, n=randV&3;
            for(int k=0;k<n;k++){
                int s=freeHigh(); if(s==0) return;
                oSpr[s]=spr(SP_FLYER); oFn[s]=F_FLYER; oB7[s]=0; oB4[s]=1;
                oX[s]=(short)x; oY[s]=(short)(y0+dy); oV8[s]=14;
                if((k&1)==0){ x+=8; dy=(short)(dy+8); }
                dy=(short)-dy;
            }
        } else {
            int s=freeHigh(); if(s==0) return;
            oSpr[s]=spr(SP_WAVER); oFn[s]=F_WAVER; oB7[s]=8; oVY[s]=1;
            oX[s]=(short)(camX-16); oY[s]=(short)py; oV8[s]=16;
        }
    }

    /** camera follows the player; boss fights lock it at the end of the level */
    void camera(){
        camX=(short)(px-(W>>1)+12);
        camY=(short)(py-(H>>1)+30);
        if(camX<0) camX=0;
        if(camY<0) camY=0;
        if(camX>maxCamX) camX=maxCamX;
        if(camY>maxCamY) camY=maxCamY;
        if(!bossLock){
            if(camX==maxCamX && bossX!=0){
                bossLock=true;
                spawnBoss();
            }
        } else {
            int arena = level==3 ? 228 : 128;
            if(W>arena-1) camX=maxCamX;
            else {
                int min=(short)(maxCamX+W-arena);
                if(camX<min) camX=min;
                if(px<camX-6) px=(short)(camX-6);
            }
        }
        int x=px-camX, y=py-camY;
        setSprite(PL, oSpr[PL], x, y);
        pbx=(short)(x+4); pby=(short)(y+5);
    }

    void player(){
        int i=PL;
        oSpr[i]=spr(oB7[i]);
        int prev=oB7[i];
        oB7[i]&=0xF8;
        pyPrev=py;
        int k=keys, dx=0;
        if((k&K_RIGHT)!=0){
            dx = (prev&8)!=0 ? 5 : 3;
            oB7[i]=(frameCnt&3)+1;
            int ty=((py+8)>>>3)&0xFF, tx=((px+dx+12)>>>3)&0xFF;
            if(((attr(tx, ty)|attr(tx, ty+1))&0x1C)!=0) dx-=3;
        } else if((k&K_LEFT)!=0){
            dx = (prev&8)!=0 ? -3 : -5;
            oB7[i]=(frameCnt&3)+9;
            int ty=((py+8)>>>3)&0xFF, tx=((px+dx+8)>>>3)&0xFF;
            if(((attr(tx, ty)|attr(tx, ty+1))&0x1C)!=0) dx+=3;
        }
        if((oB4[i]&6)!=0){                                  // standing
            oVY[i]=0;
            if((k&(K_UP|K_FIRE2))==0){
                if((k&K_DOWN)!=0 && dx==0) oB7[i]|=7;      // duck
                else if(k==0){
                    if((attr(((px+8)>>>3)&0xFF, ((py+8)>>>3)&0xFF)&0xC0)!=0 && (byte)honey<=38){
                        honey=(honey+1)&0xff;            // eating honey
                        oB7[i]|=6;
                        sndHoney();
                    }
                }
            } else {                                        // jump
                oVY[i]=(short)0x9000;
                oB4[i]&=0xF9;
                py=(short)(py-7);
                oB7[i]|=5;
            }
            px=(short)(px+dx);
        } else {                                            // in the air
            int vy=oVY[i];
            py=(short)(py+(vy>>12));
            px=(short)(px+dx);
            oB7[i]=(oB7[i]&0xF8)|4;
            if(vy<=0x67FF) oVY[i]=(short)(vy+0x1800);
            if(oVY[i]<0){
                oB7[i]|=1;
                int ty=((py+4)>>3)&0xFF;
                if(((attr(((px+6)>>3)&0xFF, ty)|attr(((px+14)>>3)&0xFF, ty))&0x1C)!=0){
                    py=pyPrev;
                    oVY[i]=0;
                }
            }
        }
        // landing
        boolean clearB4=true;
        if(py>=pyPrev && (oB4[i]&4)==0){
            clearB4=false;
            oB4[i]=0;
            if((py&7)==4 || ((py+4)>>3)!=((pyPrev+4)>>3)){
                int fx = (oB7[i]&8)==0 ? px+7 : px+11;
                int ty=((py+20)>>>3)&0xFF;
                int a=attr(((short)fx>>3)&0xFF, ty)|attr(((short)(fx+4)>>3)&0xFF, ty);
                if((a&0x20)==0){
                    oB4[i]=a&2;
                    if(oB4[i]!=0) py=(short)(((py-4)&~7)|4);
                } else {
                    oB4[i]=a;
                    playerDies();
                }
            }
        }
        if(clearB4) oB4[i]=0;
        // level bounds
        int maxX=(short)(maxCamX+W-12), maxY=(short)(maxCamY+H-6);
        if(camX==maxCamX){ if(px<camX-6) px=(short)(camX-6); }
        else if(px<-6) px=-6;
        if(px>maxX) px=maxX;
        if(py>maxY) state=ST_DEATH;
        // sting
        if((keys&K_FIRE)!=0 && oSpr[STING]==0){
            oY[STING]=(short)((keys&K_DOWN)==0 ? py+6 : py+9);
            oSpr[STING]=spr(SP_BULLET);
            oX[STING] = (oB7[i]&8)==0 ? (short)(px+16) : px;
            oB7[STING]=2;
            oFn[STING]=F_STING;
            oB4[STING]=oB7[i]&8;
            sndSting();
        }
        camera();
    }

    void playerDies(){
        honey=0;
        oFn[PL]=F_PLAYER_DEATH;
        oSpr[PL]=spr(SP_EXPL_B);
        oB7[PL]=5;
        vibrate();
        sndExplosion();
    }

    void playerDeath(int i){
        sndDeath();
        if((frameCnt&1)!=0){
            oB7[i]=(oB7[i]-1)&0xff;
            if(oB7[i]==0){
                state=ST_DEATH;
                setSprite(PL, 0, 0, 0);
                return;
            }
        }
        oSpr[i]=spr(SP_EXPL_B0+oB7[i]);
        setSprite(i, oSpr[i], px-camX, py-camY);
    }

    void exitDoor(int i){
        if(bossX!=0 && bossHP!=0) return;                   // opens when the boss is beaten
        oSpr[i]=spr(SP_EXIT+((frameCnt>>1)&1));
        if(touchesPlayer(i)){
            bossLock=false;
            bossObj=0;
            state=ST_LEVEL_DONE;
            sndExit();
            oSpr[i]=0;
        }
        showObj(i);
    }

    /** sparkles of a collected item */
    void pop(int i){
        int x=(short)(oX[i]-camX), y=(short)(oY[i]-camY);
        oY[i]=(short)(oY[i]-1);
        oX[i]=(short)(oX[i]-1);
        int f=frameCnt&1;
        g.setColor(0xffffff);
        if(oB7[i]==5){
            plot(x, y); plot(x+f+6, y);
            int x2=x+(f<<1);
            plot(x2+16, y);
            plot(x+4, y+2); plot(x2+12, y+2);
        } else if(oB7[i]==4){
            plot(x, y-2);
            int x2=x+(f<<1);
            plot(x2+18, y-2);
            plot(x+4, y+1); plot(x2+12, y+1);
        } else oSpr[i]=0;
        setSprite(i, 0, 0, 0);
        if((frameCnt&1)!=0) oB7[i]=(oB7[i]-1)&0xff;
    }
    void plot(int x, int y){ g.fillRect(x, y, 1, 1); }

    void scoreItem(int i){
        oSpr[i]=spr(SP_SCORE+((frameCnt>>2)&1));
        if(touchesPlayer(i)){
            oB7[i]=5;
            score=(score+oV8[i])&0xFFFF;
            oFn[i]=F_POP;
            sndScore();
        }
        showObj(i);
    }

    void lifeItem(int i){
        if((frameCnt&1)!=0) oB7[i]=(oB7[i]+1)&0xff;
        if(oB7[i]>2) oB7[i]=0;
        oSpr[i]=spr(SP_LIFE+oB7[i]);
        if(touchesPlayer(i)){
            lives=(lives+1)&0xff;
            oFn[i]=F_POP;
            oB7[i]=5;
            sndLife();
        }
        showObj(i);
    }

    void walker(int i){
        oB7[i]=(oB7[i]+1)&0xff;
        oSpr[i]=spr(SP_WALKER+(oB7[i]&1));
        oX[i]=(short)((oB4[i]&0x10)==0 ? oX[i]-1 : oX[i]+1);
        if((attr(((oX[i]+oB4[i])>>3)&0xFF, ((oY[i]>>3)+1)&0xFF)&2)==0) oB4[i]^=0x10;
        contact(i);
        showObj(i);
    }

    void flyer(int i){
        oX[i]=(short)(oX[i]-3);
        if(oX[i]-camX>=-16){
            oB7[i]^=1;
            oSpr[i]=spr(SP_FLYER+oB7[i]);
            contact(i);
            showObj(i);
        } else {
            oSpr[i]=0;
            setSprite(i, 0, 0, 0);
        }
    }

    void waver(int i){
        oX[i]=(short)(oX[i]+1);
        int sx=oX[i]-camX;
        if(sx<=W+20 && sx>=-50){
            oB7[i]=(oB7[i]-1)&0xff;
            if(oB7[i]==255){ oB7[i]=16; oVY[i]=(short)-oVY[i]; }
            oSpr[i]=spr(SP_WAVER+((oB7[i]>>1)&1));
            oY[i]=(short)(oY[i]+oVY[i]);
            contact(i);
            showObj(i);
        } else {
            oSpr[i]=0;
            setSprite(i, 0, 0, 0);
        }
    }

    void shooter(int i){
        if((frameCnt&1)!=0){ showObj(i); return; }
        int face=0, dx=2;
        if((oB4[i]&8)==0){ face=5; dx=-2; }
        if(oB5[i]==0){
            oB7[i]=(oB7[i]&1)^1;
            oX[i]=(short)(oX[i]+dx);
            if((attr((((oX[i]+oB4[i])>>3)+1)&0xFF, ((oY[i]>>3)+3)&0xFF)&2)==0) oB4[i]^=8;
            if(((py-oY[i]+12)&0xFFFF)<=27){
                int d=(oB4[i]&8)==0 ? ((px-oX[i])&0xFFFF)+68 : ((px-oX[i])&0xFFFF)+16;
                if((d&0xFFFF)<=67){                         // player in front: fire
                    int b=freeHigh();
                    oSpr[b]=spr(SP_BULLET); oFn[b]=F_BULLET;
                    oX[b]=(short)(oX[i]+(oB4[i]<<1)); oY[b]=(short)(oY[i]+12);
                    oB4[b]=oB4[i]; oB7[b]=22; oV8[b]=15;
                    oB5[i]=8; oB7[i]=4;
                    oX[i]=(short)(oX[i]-dx);
                }
            }
        } else {
            oB5[i]=(oB5[i]-1)&0xff;
            oB7[i]=4;
        }
        oSpr[i]=spr(SP_SHOOTER+oB7[i]+face);
        contactBig(i);
        showObj(i);
    }

    void bullet(int i){
        oB7[i]=(oB7[i]-1)&0xff;
        if(oB7[i]==255) oSpr[i]=0;
        else {
            oX[i]=(short)((oB4[i]&8)==0 ? oX[i]-4 : oX[i]+4);
            if(playerSpriteHits(i)) hurt(i, false);
        }
        showObj(i);
    }

    /** a lever that starts / stops its platform */
    void lever(int i){
        boolean on=touchesPlayer(i);
        if(oB7[i]==0 && on){
            oB4[i]^=1;
            oB4[oB5[i]]=oB4[i];
            sndSwitch();
        }
        oB7[i]=on ? 1 : 0;
        oSpr[i]=spr(SP_SWITCH+oB4[i]);
        showObj(i);
    }

    void platform(int i){
        int mvx=0;
        boolean carry=false;
        if((oB4[i]&4)!=0) return;
        if(oB4[i]!=0){
            oB7[i]=(oB7[i]-2)&0xff;
            mvx=oVX[i];
            oX[i]=(short)(oX[i]+mvx);
            int mvy=oVY[i];
            oY[i]=(short)(oY[i]+mvy);
            if(oB7[i]==0){
                oB7[i]=(oB5[i]<<3)&0xff;
                oVX[i]=(short)-mvx;
                oVY[i]=(short)-mvy;
            }
        }
        if(oVY[PL]>=0){
            int bx=(short)(px-camX+4), by=(short)(py-camY+5), bw=12, bh=16;
            boolean left=(oB7[PL]&8)!=0;
            if(left) bx=(short)(px-camX+8);
            int hit=boxHit(bx, by, bw, bh, i, i);
            if(hit!=i){
                showObj(i);
                hit=boxHit(bx, by, bw, bh, i, i);
            }
            if(hit==i && pyPrev+14<oY[i]){
                int fx=bx+(left ? 3 : 2);
                if(boxHit(fx, by+15, 7, 1, i, i)==i){
                    oB4[PL]|=4;
                    carry=true;
                }
            }
        }
        if(carry){
            px=(short)(px+mvx);
            py=(short)(oY[i]-20);
            camera();
            pyPrev=(short)(py-1);
        }
        showObj(i);
    }

    /** the player's sting: a 50 pixel box in front of him for two frames */
    void sting(int i){
        oSpr[i]=spr(SP_STING);
        if(oB7[i]==0) oSpr[i]=0;
        oB7[i]=0;
        int x=oX[i]-camX;
        sbx=(short)(x+6);
        if((oB4[i]&8)!=0) sbx=(short)(x-50);
        sby=(short)(oY[i]-camY+2);
        stingHits();
        showObj(i);
    }

    void stingHits(){
        int s=33;
        while(true){
            s=boxHit(sbx, sby, SBW, SBH, s+1, 47);
            if(s<0) return;
            int f=oFn[s];
            if(f==F_EXPL_B || f==F_EXPL_S || f==F_BULLET) continue;
            int id=oSpr[s];
            int dmg = res.sH[id]<=16 ? 2 : 1;
            oV8[s]=(oV8[s]-dmg)&0xFFFF;
            score=(score+dmg)&0xFFFF;
            boolean dead=(short)oV8[s]==0;
            boolean left=oB4[STING]!=0;
            if(s==bossObj){
                if(dead){
                    bossHP=0;
                    oFn[s]=F_BOSS_DIE;
                    bossCnt=15;
                    sndExplosion();
                } else {
                    int y=sby+camY-2;
                    y = (randV&4)==0 ? y-(randV&3) : y+(randV&3);
                    int x=oX[s];
                    if(left) x=x+(res.sW[id]-res.sCX[id])-3;
                    smallExplosion(x, y);
                }
            } else if(dead){
                if(res.sH[id]>8){
                    oSpr[s]=spr(SP_EXPL_B); oB7[s]=6; oFn[s]=F_EXPL_B;
                    sndExplosion();
                } else {
                    oSpr[s]=spr(SP_EXPL_S); oB7[s]=4; oFn[s]=F_EXPL_S;
                }
            } else {
                int y=sby+camY-2;
                y = (randV&4)==0 ? y-(randV&1) : y+(randV&1);
                int x=oX[s];
                if(left) x=x+(res.sW[id]-res.sCX[id])-16;
                smallExplosion((left ? 8 : 0)+x, y);
            }
        }
    }

    void smallExplosion(int x, int y){
        int s=freeTop(); if(s==0) return;
        oSpr[s]=spr(SP_EXPL_S); oB7[s]=4; oFn[s]=F_EXPL_S;
        oX[s]=(short)x; oY[s]=(short)y;
        setSprite(s, oSpr[s], x-camX, y-camY);
    }
    void bigExplosion(int x, int y){
        int s=freeTop(); if(s==0) return;
        oSpr[s]=spr(SP_EXPL_B); oB7[s]=6; oFn[s]=F_EXPL_B;
        oX[s]=(short)x; oY[s]=(short)y;
        setSprite(s, oSpr[s], x, y);
    }
    /** explosion frames count down to the empty table entry, which frees the object */
    void explosion(int i, int base){
        oB7[i]=(oB7[i]-1)&0xff;
        oSpr[i]=spr(base+oB7[i]);
        showObj(i);
    }

    // ------------------------------------------------------------------ damage
    void contact(int i){ if(touchesPlayer(i)) hurt(i, false); }
    void contactBig(int i){ if(touchesPlayer(i)) hurt(i, true); }
    /** the enemy hits the player (damage = its strength) and blows up */
    void hurt(int i, boolean big){
        if(oFn[PL]==F_PLAYER_DEATH) return;
        vibrate();
        honey=((honey&0xFF)-(oV8[i]&0xFF))&0xff;
        if((byte)honey<=0) playerDies();
        if(big){
            oSpr[i]=spr(SP_EXPL_B); oB7[i]=6; oFn[i]=F_EXPL_B;
            sndExplosion();
        } else {
            oSpr[i]=spr(SP_EXPL_S); oB7[i]=4; oFn[i]=F_EXPL_S;
        }
    }
    /** bosses only cost one point of honey per touch */
    void bossContact(int i){
        if(!touchesPlayer(i) || oFn[PL]==F_PLAYER_DEATH) return;
        vibrate();
        honey=(honey-1)&0xff;
        if((byte)honey<=0) playerDies();
    }

    // ------------------------------------------------------------------ bosses
    void spawnBoss(){
        int s=freeHigh();
        if(s==0) return;
        bossObj=s;
        switch(bossFn){
            case BOSS_QUEEN: {
                int x=(short)(camX+W+40);
                oX[s]=x; oY[s]=80;
                oSpr[s]=spr(SP_QUEEN_START); oFn[s]=F_QUEEN;
                oV8[s]=100; oVX[s]=0; oVY[s]=0;
                bossCnt=1; bossMode=0;
                bossTX=bossX; bossTY=bossY;
                oB7[s]=0; oB5[s]=0;
                oB4[s]=dir32(x, 80, bossX, bossY);
                break;
            }
            case BOSS_2:
                oX[s]=(short)(camX+W); oY[s]=(short)bossY;
                oSpr[s]=spr(SP_BOSS2_START); oFn[s]=F_BOSS2; oB4[s]=0; oB5[s]=0; oV8[s]=200;
                break;
            case BOSS_3:
                oX[s]=(short)(camX+W); oY[s]=(short)bossY;
                oSpr[s]=spr(SP_BOSS3_START); oFn[s]=F_BOSS3; oB4[s]=0; oB5[s]=0; oV8[s]=300;
                break;
            case BOSS_4:
                oX[s]=(short)(camX+W); oY[s]=(short)bossY;
                oSpr[s]=spr(SP_BOSS4); oFn[s]=F_BOSS4; oB5[s]=0; oV8[s]=400;
                oB7[s]=25; oB4[s]=res.path[25*3+2]&0xff;
                break;
            default:
                bossObj=0;
        }
    }

    /** direction 0..31 from (x0, y0) to (x1, y1) */
    static int dir32(int x0, int y0, int x1, int y1){
        int dx=(x1&0xFFFF)-(x0&0xFFFF), dy=(y1&0xFFFF)-(y0&0xFFFF), a=0;
        boolean negY=true;
        if((short)dx<0){
            dx=-dx; a=128;
            if((short)dy>=0) negY=false; else a=-64;
        } else if((short)dy>=0){ a=64; negY=false; }
        if(negY) dy=-dy;
        int ax=(short)dx;
        if(ax==0){ if((a&0x40)!=0) a=(a&0xFF)+64; }
        else if((a&0x40)==0) a=(a&0xFF)+(div(ax<<6, ax+(short)dy)&0xFF);
        else a=((a-div(ax<<6, ax+(short)dy))&0xFF)+64;
        return ((a&0xFF)>>>3)&0xFF;
    }

    /** level 1: the queen bee, swooping at the player and back to her spot */
    void queen(int i){
        if(oB5[i]==0){
            int vx=oVX[i], vy=oVY[i];
            oX[i]=(short)(oX[i]+((short)vx>>8));
            oY[i]=(short)(oY[i]+((short)vy>>8));
            int d=oB4[i]&31;
            oVX[i]=(short)((vx&0xFF)+(dirX[d]<<1));
            oVY[i]=(short)((vy&0xFF)+(dirY[d]<<1));
        }
        oY[i]=(short)((frameCnt&4)!=0 ? oY[i]+1 : oY[i]-1);
        if(oB5[i]==0){
            if(dirX[oB4[i]&31]<0){
                oB7[i]=((frameCnt>>1)&1)+3;
                if(oX[i]<=(short)bossTX){ oB5[i]=1; oB4[i]=0; }
            } else {
                oB7[i]=(frameCnt>>1)&1;
                if(oX[i]>=(short)bossTX){ oB4[i]=0; oB5[i]=1; }
            }
        } else {
            oB5[i]=(oB5[i]+1)&0xff;
            if((frameCnt&1)!=0){
                int r=oB7[i];
                if(bossMode==0){
                    if(r>1) oB7[i] = r<=3 ? (r==2 ? 5 : 4) : 3;
                    else oB7[i]=2;
                } else {
                    if(r>2) oB7[i] = r!=5 ? 5 : 2;
                    else oB7[i]=(r^1)&1;
                }
            }
            if(oB5[i]>30){
                if(bossMode==0){
                    bossMode=1;
                    bossTX=(short)(px-30);
                    bossTY=(short)(py-10);
                } else {
                    bossMode=0;
                    bossTX=bossX;
                    bossTY=bossY;
                }
                oB5[i]=0;
                oB4[i]=dir32(oX[i], oY[i], bossTX, bossTY);
            }
        }
        oSpr[i]=spr(SP_QUEEN+oB7[i]);
        bossContact(i);
        showObj(i);
    }

    /** level 2 boss: walks to and fro, drops walkers */
    void boss2(int i){
        int face=0, dx=2;
        if((oB4[i]&8)==0){ face=4; dx=-2; }
        if(oB5[i]==0){
            if((randV&0x3F)==63){
                int s=freeHigh();
                if(s!=0){
                    oSpr[s]=spr(SP_WALKER); oFn[s]=F_WALKER;
                    oX[s]=(short)(oX[i]-oB4[i]+24); oY[s]=(short)(oY[i]+8);
                    oB7[s]=0; oB4[s]=oB4[i]&0x10; oV8[s]=12;
                }
            }
            oB7[i]=((frameCnt%6)>>1)+face;
            oX[i]=(short)(oX[i]+dx);
            if((attr(((oX[i]+oB4[i])>>3)&0xFF, ((oY[i]>>3)+2)&0xFF)&2)==0){
                oB4[i]^=0x18;
                oB7[i]=face+3;
                oB5[i]=2;
            }
        } else {
            oB5[i]=(oB5[i]-1)&0xff;
            if(oB5[i]==0) oB7[i]=face+3;
        }
        bossContact(i);
        oSpr[i]=spr(SP_BOSS2+oB7[i]);
        showObj(i);
    }

    /** level 3 boss: rolls to and fro, resting at the walls */
    void boss3(int i){
        int face=0, dx=3;
        if((oB4[i]&0x20)==0){ face=3; dx=-3; }
        if(oB5[i]==0){
            oB7[i]=((frameCnt%5)>>1)+face;
            oX[i]=(short)(oX[i]+dx);
            if((attr(((oX[i]+oB4[i])>>3)&0xFF, ((oY[i]>>3)+2)&0xFF)&2)==0){
                oB4[i]^=0x20;
                oB5[i]=20;
            }
        } else {
            oB5[i]=(oB5[i]-1)&0xff;
            oB7[i]=face;
        }
        bossContact(i);
        oSpr[i]=spr(SP_BOSS3+oB7[i]);
        showObj(i);
    }

    /** level 4 boss: flies a fixed path (dx, dy, frames) with a random shortcut */
    void boss4(int i){
        oB4[i]=(oB4[i]-1)&0xff;
        if(oB4[i]==0){
            int r=oB7[i];
            if(r==24) oB7[i]=1;
            else if(r==15) oB7[i]=0;
            else if(r==7) oB7[i] = (randV&3)==3 ? 16 : 8;
            else if(r==25) oB7[i]=8;
            else oB7[i]=r+1;
            oB4[i]=res.path[oB7[i]*3+2]&0xff;
        }
        oY[i]=(short)((frameCnt&4)==0 ? oY[i]-1 : oY[i]+1);
        int p=oB7[i]*3;
        oX[i]=(short)(oX[i]+res.path[p]);
        oY[i]=(short)(oY[i]+res.path[p+1]);
        oSpr[i]=spr(SP_BOSS4+((frameCnt%6)>>1));
        bossContact(i);
        showObj(i);
    }

    /** a beaten boss: explosions all over it for 15 frames */
    void bossDie(int i){
        bossCnt=(bossCnt-1)&0xff;
        if(bossCnt==0){ oSpr[i]=0; return; }
        int id=oSpr[i];
        int h=res.sH[id], w=res.sW[id], y=oY[i]&0xFFFF, x=oX[i]&0xFFFF;
        int rx=randV-div(randV, w)*w;
        randV=random()&0xFFFF;
        int ry=y+((randV-div(randV, h)*h)&0xFFFF);
        bigExplosion((rx&0xFF)+x, ry);
        randV=random()&0xFFFF;
        rx=randV-div(randV, w)*w;
        randV=random()&0xFFFF;
        ry=(y+(randV-div(randV, h)*h))&0xFFFF;
        bigExplosion((rx&0xFF)+x, ry);
        setSprite(i, 0, 0, 0);
    }

    // ================================================================== list menu
    /**
     * Vertical list (no title): UP/LEFT and DOWN/RIGHT move (wrapping, scrolling), FIRE returns
     * the index, SELECT returns -1.
     */
    int listMenu(String[] items, int x, int y, int w, int h, int sel){
        int n=items.length;
        int itemH=fh;
        int vis=h/itemH; if(vis<1) vis=1;
        int listY=y+((h-vis*itemH)>>1);
        int top = sel<vis ? 0 : sel-vis+1;
        drawList(items, x, w, listY, itemH, vis, top, sel);
        boolean held=true;
        while(true){
            int k=pollKeys();
            if(k!=0 && held) continue;
            held=true;
            if((k&(K_UP|K_LEFT))!=0){
                int ns = sel==0 ? n-1 : sel-1;
                if(ns>=top+vis) top=n-vis;
                else if(ns<top) top=top-1;
                sel=ns;
            } else if((k&(K_DOWN|K_RIGHT))!=0){
                int ns=(sel+1)%n;
                if(ns>=top+vis) top=top+1;
                else if(ns<top) top=0;
                sel=ns;
            } else if((k&K_FIRE)!=0) return sel;
            else if((k&K_SELECT)!=0) return -1;
            else { held=false; continue; }
            drawList(items, x, w, listY, itemH, vis, top, sel);
        }
    }

    void drawList(String[] items, int x, int w, int listY, int itemH, int vis, int top, int sel){
        fullClip();
        menuBg();
        int n=items.length;
        for(int i=0;i<vis && i<n;i++){
            color(top+i==sel ? C_CYAN : C_WHITE);
            text(items[top+i], x+3, listY+i*itemH);
        }
        if(vis<n){
            int len=vis*itemH, r=x+w;
            color(C_BAR_OUT); fill(r-9, listY+2, r-3, listY+len);
            color(C_BAR_IN);  fill(r-8, listY+3, r-4, listY+len-1);
            len-=2;
            int thumb=div(len*vis, n)-3, pos=div(len*top, n)+2;
            color(C_WHITE); fill(r-7, listY+pos+2, r-5, listY+pos+thumb+2);
        }
        flip();
    }

    // ================================================================== message box
    /** word-wrapped text box; confirm: FIRE = yes (true), other keys = no */
    boolean msgBox(String title, String msg, boolean confirm){
        int bx=3, by=3, bw=W-6, bh=H-6, tx=bx+4, tw=bw-8;
        Vector lines=new Vector();
        int start=0;
        while(start<=msg.length()){
            int nl=msg.indexOf('\n', start);
            if(nl<0) nl=msg.length();
            wrap(msg.substring(start, nl), tw, lines);
            start=nl+1;
        }
        int textY=by+4+(title!=null ? fh+3 : 0);
        int footY=by+bh-fh-3;
        int vis=(footY-textY-2)/fh; if(vis<1) vis=1;
        int top=0, maxTop=Math.max(0, lines.size()-vis);
        waitRelease();
        while(true){
            fullClip();
            g.setColor(0xffffff); g.fillRect(bx, by, bw, bh);
            g.setColor(0x101840); g.fillRect(bx+1, by+1, bw-2, bh-2);
            if(title!=null){ g.setColor(0x00ffff); text(title, tx, by+3); }
            g.setColor(0xffffff);
            clip(bx+1, textY, bx+bw-2, footY-2);
            for(int i=0;i<vis && top+i<lines.size();i++) text((String)lines.elementAt(top+i), tx, textY+i*fh);
            fullClip();
            if(maxTop>0){
                int len=footY-2-textY, thumb=Math.max(4, len*vis/lines.size()), pos=(len-thumb)*top/maxTop;
                g.setColor(0x405080); g.fillRect(bx+bw-5, textY, 3, len);
                g.setColor(0xffffff); g.fillRect(bx+bw-5, textY+pos, 3, thumb);
            }
            g.setColor(0x00ff00);
            if(confirm){
                text("OK", tx, footY);
                String no=res.str[T_BACK];
                text(no, bx+bw-4-tw(no), footY);
            } else text("OK", tx, footY);
            flip();
            int k;
            while((k=pollKeys())==0) { }
            if((k&K_UP)!=0 && top>0) top--;
            else if((k&K_DOWN)!=0 && top<maxTop) top++;
            else if((k&K_FIRE)!=0){ waitRelease(); return true; }
            else if((k&(K_SELECT|K_FIRE2))!=0){ waitRelease(); return false; }
            long t0=now();
            while(pollKeys()!=0 && now()-t0<250) { }
        }
    }

    void wrap(String s, int w, Vector out){
        if(s.length()==0){ out.addElement(""); return; }
        int i=0, n=s.length();
        while(i<n){
            int end=i, last=-1;
            while(end<n && font.substringWidth(s, i, end-i+1)<=w){
                if(s.charAt(end)==' ') last=end;
                end++;
            }
            if(end>=n){ out.addElement(s.substring(i)); return; }
            if(last>i) end=last;
            if(end==i) end=i+1;
            out.addElement(s.substring(i, end));
            i=end;
            while(i<n && s.charAt(i)==' ') i++;
        }
    }

    // ================================================================== highscores
    void hsLayout(int x, int y, int w, int h){
        int titleH=fh+2;
        hsRowH=fh+1;
        int used;
        if((hsRowH*3+titleH)*2<=h-3){ hsVisible=6; used=hsRowH*6; }
        else { hsVisible=div(h-titleH*2-3, hsRowH); used=hsVisible*hsRowH; }
        if(hsVisible<1){ hsVisible=1; used=hsRowH; }
        hsTop=y+((h-used)>>1);
        hsBottom=y+h-titleH;
        hsX=x; hsY=y; hsW=w; hsH=h;
        hsScroll=0; blinkDir=0; blinkIdx=0;
    }

    /** blink colour index ping-pong 0..7 */
    void blinkStep(){
        if(blinkDir==0){
            if(blinkIdx>6) blinkDir=1; else blinkIdx++;
        } else {
            if(blinkIdx<=0) blinkDir=0; else blinkIdx--;
        }
    }
    int blinkColor(){ return 0x001F+(blinkIdx<<7); }

    void hsTitle(){
        color(C_CYAN);
        String s=res.str[T_HIGHSCORE];
        text(s, hsX+((hsW-tw(s))>>1), hsY);
    }

    void hsRow(int i){
        int sp=tw(" ");
        int y=hsRowH*(i-hsScroll)+hsTop;
        int x=hsX+sp;
        String num=String.valueOf(i+1);
        text(num, x, y);
        x+=tw(num)+sp;
        text(nameOf(i), x, y);
        String sc=pad(hsScore[i], 6);
        int sx=hsX+hsW-tw(sc)-sp-7;
        text(sc, sx, y);
        String lv=pad(hsLevel[i], 2);
        text(lv, sx-(tw(lv)+sp), y);
    }

    String nameOf(int i){
        StringBuffer b=new StringBuffer();
        for(int k=0;k<3 && hsName[i][k]!=0;k++) b.append((char)(hsName[i][k]&0xff));
        return b.toString();
    }

    void hsScrollBar(){
        if(hsVisible>=6) return;
        int x=hsX+hsW-8, y=hsTop-1;
        int len=hsVisible*hsRowH, end=y+len;
        color(C_BAR_OUT); fill(x, y, x+6, end+1);
        color(C_BAR_IN);  fill(x+1, y+1, x+5, end);
        len-=2;
        int thumb=div(len*hsVisible, 6);
        int ty = hsScroll==6-hsVisible ? end-thumb-1 : y+div(len*hsScroll, 6)+2;
        color(C_WHITE); fill(x+2, ty, x+4, ty+thumb);
    }

    void hsOption(int str){
        clip(hsX, hsBottom, hsX+hsW-1, hsY+hsH);
        menuBg();
        color(C_CYAN);
        String s="< "+res.str[str]+" >";
        text(s, hsX+((hsW-tw(s))>>1), hsBottom);
        fullClip();
    }

    void hsRowBg(int i){
        int y=hsRowH*(i-hsScroll)+hsTop;
        clip(hsX, y, hsX+hsW, y+hsRowH-2);
        menuBg();
        fullClip();
    }

    /** the highscore table; sel = row to highlight (-1 none) */
    void highscores(int sel){
        hsScroll = sel>=0 && sel<=5 && sel>=hsVisible ? sel-hsVisible+1 : 0;
        int[] opts={ T_BACK, T_CLEAR };
        while(true){
            fullClip();
            menuBg();
            hsTitle();
            color(C_WHITE);
            for(int i=hsScroll;i<hsScroll+hsVisible && i<6;i++) hsRow(i);
            int opt=0;
            hsOption(opts[opt]);
            hsScrollBar();
            flip();
            boolean held=true, redraw=false; int delay=500; long last=now(), blinkT=now();
            while(!redraw){
                int k=pollKeys();
                if(k!=0){
                    if(held && now()<last+delay) { /* key repeat delay */ }
                    else {
                        delay = held ? 70 : 500;
                        held=true;
                        last=now();
                        if((k&K_LEFT)!=0){ opt=(opt+opts.length-1)%opts.length; hsOption(opts[opt]); hsScrollBar(); flip(); }
                        else if((k&K_RIGHT)!=0){ opt=(opt+1)%opts.length; hsOption(opts[opt]); hsScrollBar(); flip(); }
                        else if((k&K_UP)!=0){ hsScroll = hsScroll==0 ? 6-hsVisible : hsScroll-1; redraw=true; }
                        else if((k&K_DOWN)!=0){ hsScroll = hsScroll==6-hsVisible ? 0 : hsScroll+1; redraw=true; }
                        else if((k&K_FIRE)!=0){
                            if(opt==1){
                                if(msgBox(null, res.str[T_SURE], true)){
                                    for(int i=0;i<6;i++){ hsName[i][0]=0; hsScore[i]=0; hsLevel[i]=0; }
                                    saveHighscores();
                                }
                                sel=-1;
                                redraw=true;
                            } else return;
                        } else if((k&K_SELECT)!=0) return;
                    }
                } else { held=false; delay=500; }
                if(!redraw && sel>=hsScroll && sel<hsScroll+hsVisible && now()>blinkT+50){
                    blinkT=now();
                    color(blinkColor());
                    blinkStep();
                    hsRow(sel);
                    flip();
                }
            }
        }
    }

    /** puts the score in the table (asks for the name); returns its row or -1 */
    int insertHighscore(int sc, int lv){
        fullClip();
        for(int i=0;i<6;i++){
            if((hsScore[i]&0xffffffffL)<(sc&0xffffffffL)){
                for(int k=5;k>i;k--){
                    System.arraycopy(hsName[k-1], 0, hsName[k], 0, 4);
                    hsScore[k]=hsScore[k-1]; hsLevel[k]=hsLevel[k-1];
                }
                for(int k=0;k<4;k++) hsName[i][k]=0;
                hsScore[i]=sc;
                hsLevel[i]=lv&0xff;
                enterName(i);
                return i;
            }
        }
        return -1;
    }

    String choice(int c){
        if(c<36) return String.valueOf(NAME_CHARS[c]);
        return res.str[c==36 ? T_RUB : c==37 ? T_SPACE : T_END];
    }

    /** the letter carousel on the bottom line */
    void carousel(int sel){
        String cur=choice(sel);
        int cw=tw(cur);
        int fx=hsX+((hsW-cw)>>1);
        clip(hsX, hsBottom-2, hsX+hsW-1, hsY+hsH);
        menuBg();
        color(C_CAROUSEL);
        text(cur, fx, hsBottom);
        int x=fx+cw+5, c=sel;
        while(x<hsX+hsW){
            c=(c+1)%39;
            String s=choice(c);
            text(s, x, hsBottom);
            x+=tw(s)+5;
        }
        c=sel;
        x=fx;
        while(x>hsX){
            c = c==0 ? 38 : c-1;
            String s=choice(c);
            x=x-tw(s)-5;
            text(s, x, hsBottom);
        }
        fullClip();
    }

    void enterName(int row){
        fullClip();
        menuBg();
        hsTitle();
        hsScroll = row<=5 && row>=hsVisible ? row-hsVisible+1 : 0;
        for(int i=hsScroll;i<hsScroll+hsVisible && i<6;i++){
            color(i==row ? C_CYAN : C_WHITE);
            hsRow(i);
        }
        int sel=0, len=0;
        hsName[row][0]=0;
        carousel(0);
        flip();
        long blinkT=now(), last=now();
        boolean held=true; int delay=500;
        while(true){
            int k=pollKeys();
            if(k!=0){
                if(!(held && now()<last+delay)){
                    delay = held ? 70 : 500;
                    held=true;
                    last=now();
                    boolean done=false, draw=false;
                    if((k&K_LEFT)!=0){ sel = sel==0 ? 38 : sel-1; carousel(sel); draw=true; }
                    else if((k&K_RIGHT)!=0){ sel=(sel+1)%39; carousel(sel); draw=true; }
                    else if((k&K_FIRE)!=0){
                        if(sel<=35){
                            if(len<=2){
                                hsName[row][len++]=(byte)NAME_CHARS[sel];
                                hsName[row][len]=0;
                                hsRowBg(row); color(C_CYAN); hsRow(row); draw=true;
                            }
                        } else if(sel==36){
                            if(len!=0){ len--; hsName[row][len]=0; }
                            hsRowBg(row); color(C_WHITE); hsRow(row); draw=true;
                        } else if(sel==37){
                            if(len<=2){
                                hsName[row][len++]='_';
                                hsName[row][len]=0;
                                hsRowBg(row); color(C_CYAN); hsRow(row); draw=true;
                            }
                        } else done=true;
                    } else if((k&K_SELECT)!=0) done=true;
                    if(done){
                        for(int i=0;i<3;i++) if(hsName[row][i]=='_') hsName[row][i]=' ';
                        return;
                    }
                    if(draw) flip();
                }
            } else { held=false; delay=500; }
            if(now()>blinkT+50){
                blinkT=now();
                color(blinkColor());
                blinkStep();
                String s=choice(sel);
                clip(hsX, hsBottom-2, hsX+hsW-1, hsY+hsH);
                text(s, hsX+((hsW-tw(s))>>1), hsBottom);
                fullClip();
                flip();
            }
        }
    }

    // ================================================================== saves (RMS)
    static final String RS_HIGHSCORE="hc2-highscore", RS_RESUME="hc2-resume";
    static final int SAVE_VERSION=1;

    static byte[] readRecord(String name){
        RecordStore rs=null;
        try {
            rs=RecordStore.openRecordStore(name, false);
            if(rs.getNumRecords()<1) return null;
            return rs.getRecord(1);
        } catch(Exception e){
            return null;
        } finally {
            if(rs!=null) try { rs.closeRecordStore(); } catch(Exception e){}
        }
    }
    static void writeRecord(String name, byte[] data){
        RecordStore rs=null;
        try {
            rs=RecordStore.openRecordStore(name, true);
            if(rs.getNumRecords()<1) rs.addRecord(data, 0, data.length);
            else rs.setRecord(1, data, 0, data.length);
        } catch(Exception e){
            // full or not available: the game goes on without saving
        } finally {
            if(rs!=null) try { rs.closeRecordStore(); } catch(Exception e){}
        }
    }

    void loadHighscores(){
        byte[] d=readRecord(RS_HIGHSCORE);
        if(d==null) return;
        try {
            DataInputStream in=new DataInputStream(new ByteArrayInputStream(d));
            for(int i=0;i<6;i++){
                in.readFully(hsName[i], 0, 3); hsName[i][3]=0;
                hsScore[i]=in.readInt();
                hsLevel[i]=in.readUnsignedByte();
            }
        } catch(IOException e){}
    }
    void saveHighscores(){
        try {
            ByteArrayOutputStream bo=new ByteArrayOutputStream();
            DataOutputStream o=new DataOutputStream(bo);
            for(int i=0;i<6;i++){ o.write(hsName[i], 0, 3); o.writeInt(hsScore[i]); o.writeByte(hsLevel[i]); }
            writeRecord(RS_HIGHSCORE, bo.toByteArray());
        } catch(IOException e){}
    }

    void loadResumeHeader(){
        byte[] d=readRecord(RS_RESUME);
        if(d==null || d.length<4) return;
        resumeFlag = d[0]==SAVE_VERSION && d[1]!=0;
        level=d[2]&3;
    }
    void clearResume(){
        resumeFlag=false;
        writeRecord(RS_RESUME, new byte[]{ SAVE_VERSION, 0, (byte)level, 0 });
    }

    /** the game in progress (SELECT during the game, or leaving the application) */
    void saveResume(){
        if(!resumeFlag) return;
        try {
            ByteArrayOutputStream bo=new ByteArrayOutputStream();
            DataOutputStream o=new DataOutputStream(bo);
            o.writeByte(SAVE_VERSION); o.writeByte(1); o.writeByte(level); o.writeByte(frameCnt);
            int[] v={ sndCool, honey, lives, score, px, py, pyPrev, camX, camY, bossLock?1:0,
                      bossMode, bossCnt, bossObj, bossTX, bossTY, bossHP, sunY };
            o.writeByte(v.length);
            for(int i=0;i<v.length;i++) o.writeInt(v[i]);
            for(int i=0;i<SLOTS;i++){
                o.writeShort(oSpr[i]); o.writeByte(oFn[i]); o.writeByte(oB4[i]); o.writeByte(oB5[i]);
                o.writeByte(oB7[i]); o.writeShort(oV8[i]); o.writeShort(oX[i]); o.writeShort(oY[i]);
                o.writeShort(oVX[i]); o.writeShort(oVY[i]);
            }
            writeRecord(RS_RESUME, bo.toByteArray());
        } catch(IOException e){}
    }

    /** restores the saved game over a freshly started level */
    boolean loadResume(){
        byte[] d=readRecord(RS_RESUME);
        if(d==null || d.length<5 || d[0]!=SAVE_VERSION || d[1]==0) return false;
        try {
            DataInputStream in=new DataInputStream(new ByteArrayInputStream(d));
            in.readByte(); in.readByte(); in.readByte(); frameCnt=in.readUnsignedByte();
            int n=in.readUnsignedByte();
            int[] v=new int[17];
            for(int i=0;i<n;i++){ int x=in.readInt(); if(i<v.length) v[i]=x; }
            sndCool=v[0]; honey=v[1]; lives=v[2]; score=v[3]; px=v[4]; py=v[5]; pyPrev=v[6];
            camX=v[7]; camY=v[8]; bossLock=v[9]!=0; bossMode=v[10]; bossCnt=v[11]; bossObj=v[12];
            bossTX=v[13]; bossTY=v[14]; bossHP=v[15]; sunY=v[16];
            for(int i=0;i<SLOTS;i++){
                oSpr[i]=in.readShort(); oFn[i]=in.readUnsignedByte(); oB4[i]=in.readUnsignedByte();
                oB5[i]=in.readUnsignedByte(); oB7[i]=in.readUnsignedByte(); oV8[i]=in.readUnsignedShort();
                oX[i]=in.readShort(); oY[i]=in.readShort(); oVX[i]=in.readShort(); oVY[i]=in.readShort();
                if(oSpr[i]<0 || oSpr[i]>=res.sW.length) oSpr[i]=0;
            }
        } catch(IOException e){
            return false;
        }
        for(int i=0;i<SLOTS;i++){
            if(oSpr[i]==0) setSprite(i, 0, 0, 0);
            else if(oFn[i]!=F_EXIT) showObj(i);
        }
        camera();
        return true;
    }

    // ================================================================== lifecycle
    void pause(){
        if(paused) return;
        paused=true;
        keysHeld=0; keysLatched=0;
        if(playing!=null){ try { playing.stop(); } catch(Throwable t){} }
    }
    void resume(){
        if(!paused) return;
        synchronized(this){ paused=false; notifyAll(); }
    }
    void stop(){
        quit=true;
        synchronized(this){ notifyAll(); }
        Thread t=thread;
        if(t!=null && t!=Thread.currentThread()){
            // give the game thread a moment to save and close the sound (no Thread.join(ms) in CLDC)
            for(int i=0;i<40 && t.isAlive();i++){ try { Thread.sleep(50); } catch(InterruptedException e){} }
        }
    }
    protected void hideNotify(){ pause(); }
    protected void showNotify(){ resume(); if(error!=null) showError(); }

    void showError(){
        try {
            Graphics gr=getGraphics();
            gr.setColor(0x000040); gr.fillRect(0, 0, getWidth(), getHeight());
            gr.setColor(0xffffff);
            Font f=Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            gr.setFont(f);
            String s="Error: "+error;
            int y=2, cw=getWidth()-4, i=0;
            while(i<s.length() && y<getHeight()){
                int j=i+1;
                while(j<=s.length() && f.substringWidth(s, i, j-i)<=cw) j++;
                gr.drawSubstring(s, i, j-1-i, 2, y, Graphics.TOP|Graphics.LEFT);
                y+=f.getHeight(); i=j-1;
            }
            flushGraphics();
        } catch(Throwable t){}
    }

    // ------------------------------------------------------------------ keys
    protected void keyPressed(int k){
        if(error!=null){ midlet.exit(); return; }
        int b=map(k);
        keysHeld|=b; keysLatched|=b;
    }
    protected void keyReleased(int k){ keysHeld&=~map(k); }

    int map(int k){
        switch(k){
            case KEY_NUM2: return K_UP;
            case KEY_NUM8: return K_DOWN;
            case KEY_NUM4: return K_LEFT;
            case KEY_NUM6: return K_RIGHT;
            case KEY_NUM5: return K_FIRE;
            case KEY_NUM1: return K_UP|K_LEFT;
            case KEY_NUM3: return K_UP|K_RIGHT;
            case KEY_NUM7: return K_DOWN|K_LEFT;
            case KEY_NUM9: return K_DOWN|K_RIGHT;
            case KEY_NUM0: return K_FIRE2;
            case KEY_STAR: return K_SELECT;
            case KEY_POUND: return K_FIRE2;
            case -6: case -21: case 21: case 57345: return K_SELECT;   // left softkey
            case -7: case -22: case 22: case 57346: return K_FIRE2;    // right softkey
            case -5: case -20: case -26: case 20: return K_FIRE;       // centre / OK
            default: break;
        }
        int ga=0;
        try { ga=getGameAction(k); } catch(Throwable t){}
        switch(ga){
            case UP:    return K_UP;
            case DOWN:  return K_DOWN;
            case LEFT:  return K_LEFT;
            case RIGHT: return K_RIGHT;
            case FIRE:  return K_FIRE;
            default:    return 0;
        }
    }
}
