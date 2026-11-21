package hc2;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

/** Honey Cave 2 for MIDP 2.0 / CLDC 1.1. The game itself runs on the thread of {@link Game}. */
public final class HC2 extends MIDlet {
    Game game;

    protected void startApp(){
        if(game==null){
            game=new Game(this);
            Display.getDisplay(this).setCurrent(game);
            game.start();
        } else {
            Display.getDisplay(this).setCurrent(game);
            game.resume();
        }
    }
    protected void pauseApp(){
        if(game!=null) game.pause();
    }
    protected void destroyApp(boolean unconditional){
        if(game!=null) game.stop();
    }
    void exit(){
        destroyApp(true);
        notifyDestroyed();
    }
}
