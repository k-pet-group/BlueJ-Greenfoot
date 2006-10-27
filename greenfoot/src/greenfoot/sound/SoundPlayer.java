package greenfoot.sound;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Plays sounds from a file or URL. 
 * Several sounds can be played at the same time.
 * 
 * TODO:
 *  Make it a singleton.
 *  Remove soundstreams from list of sounds when they finish playing.
 * 
 * Should sounds be paused when hitting the pause button?
 * Should compile stop all the sounds?
 * What if act-button is hit? Should sounds play to the end?
 * 
 * 
 * @author Poul Henriksen
 * 
 */
public class SoundPlayer
{
    private static List sounds = new ArrayList();
    
    public static void stop() {
        for (Iterator iter = sounds.iterator(); iter.hasNext();) {
            SoundStream element = (SoundStream) iter.next();
            synchronized (element) {
                element.stop();
                element.notifyAll();
            }
        }
        sounds.clear();
    }
    
    public static void pause() {
        for (Iterator iter = sounds.iterator(); iter.hasNext();) {
            SoundStream element = (SoundStream) iter.next();
            element.pause();
        }
    }
    
    public static void resume() {
        for (Iterator iter = sounds.iterator(); iter.hasNext();) {
            SoundStream element = (SoundStream) iter.next();
            element.resume();
        }
    }
    
    public static void play(String file)
        throws IOException, UnsupportedAudioFileException, LineUnavailableException
    {
       final SoundStream sound = new SoundStream(file);
       new Thread() {
           public void run()
           {
               try {
                   sound.play();
               }
               catch (Exception e) {
                   e.printStackTrace();
               }
           }
       }.start();
    }
}
