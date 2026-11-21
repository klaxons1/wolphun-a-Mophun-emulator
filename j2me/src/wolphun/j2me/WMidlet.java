package wolphun.j2me;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

/** MIDlet of a decompiled mophun game (see WCanvas for the game thread). */
public final class WMidlet extends MIDlet {
    WCanvas canvas;

    protected void startApp(){
        if(canvas==null){
            canvas=new WCanvas(this);
            Display.getDisplay(this).setCurrent(canvas);
            canvas.start();
        } else {
            Display.getDisplay(this).setCurrent(canvas);
            canvas.resume();
        }
    }
    protected void pauseApp(){
        if(canvas!=null) canvas.pause();
    }
    protected void destroyApp(boolean unconditional){
        if(canvas!=null){
            canvas.stop();
            if(canvas.sdk!=null){ canvas.sdk.flushSaves(); canvas.sdk.closeSound(); }
        }
    }
    void exit(){
        destroyApp(true);
        notifyDestroyed();
    }
}
