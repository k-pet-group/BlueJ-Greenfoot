package greenfoot.sound;

import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;

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
 * 
 * 
 * @author Poul Henriksen
 * 
 */
public class SoundPlayer implements SimulationListener
{
    private List sounds = new ArrayList();
    
    private static SoundPlayer instance;
    
    private SoundPlayer() {
    }
    
    public synchronized static SoundPlayer getInstance() {
        if(instance == null) {
            instance = new SoundPlayer();
        }
        return instance;
    }
    
    /**
     * Stops all sounds currently played by the soundplayer. Includes paused
     * sounds. Stopped sounds can NOT be resumed.
     * 
     */
    public synchronized void stop() {
        for (Iterator iter = sounds.iterator(); iter.hasNext();) {
            SoundStream element = (SoundStream) iter.next();
            element.stop();
        }
        sounds.clear();
    }
    
    /**
     * Pauses all sounds. Can be resumed.
     *
     */
    public synchronized void pause() {
        for (Iterator iter = sounds.iterator(); iter.hasNext();) {
            SoundStream element = (SoundStream) iter.next();
            element.pause();
        }
    }
    
    /**
     * Resumes paused sounds.
     *
     */
    public synchronized  void resume() {
        for (Iterator iter = sounds.iterator(); iter.hasNext();) {
            SoundStream element = (SoundStream) iter.next();
            element.resume();
        }
    }
    
    /**
     * Plays the sound from file.
     * @param file Name of a file or an url
     * @throws IOException
     * @throws UnsupportedAudioFileException
     * @throws LineUnavailableException
     */
    public void play(String file)
        throws IOException, UnsupportedAudioFileException, LineUnavailableException
    {
       final SoundStream sound = new SoundStream(file, this);
       sounds.add(sound);
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

    /**
     * Stop sounds when simulation is disabled (a new world is created). Pause
     * sounds when simulation is paused. Resume when simulation is started.
     */
    public void simulationChanged(SimulationEvent e)
    {
        if(e.getType() == SimulationEvent.DISABLED) {
            stop();
        } 
        else if(e.getType() == SimulationEvent.STOPPED) {
            pause();
        }
        else if(e.getType() == SimulationEvent.STARTED) {
            resume();
        }
    }
    
    /**
     * Method that should be called by a soundsStream when it is finished
     * playing.
     */
    synchronized void soundStreamFinished(SoundStream s)
    {
        sounds.remove(s);
    } 
}
