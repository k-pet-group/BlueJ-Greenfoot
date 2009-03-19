package greenfoot.sound;

import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Contains a collection of sounds that are currently playing. This collections is used for stopping sounds.
 * 
 * @author Poul Henriksen
 */
public class SoundCollection implements SimulationListener, SoundPlaybackListener
{
    /** Sounds currently playing or paused by this SoundCollection. */
    private Set<Sound> playingSounds = new HashSet<Sound>();
    
    /** Sounds paused by the user code. */
    private Set<Sound> pausedSounds = new HashSet<Sound>();    
    
    private volatile boolean ignoreEvents = false;
    
    /**
     * Stop sounds when simulation is disabled (a new world is created).
     */
    public void simulationChanged(SimulationEvent e)
    {
        if (e.getType() == SimulationEvent.DISABLED) {
            stop();
        }
        else if (e.getType() == SimulationEvent.STOPPED) {
            pause();
        }
        else if (e.getType() == SimulationEvent.STARTED) {
            resume();
        }
    }
    

    /** 
     * Resumes all songs previously paused with a call to pause()
     */
    private void resume()
    {
        synchronized (this) {
            ignoreEvents = true;
        }
        for (Sound sound : playingSounds) {
            sound.resume();
        }
        synchronized (this) {
            ignoreEvents = false;
        }
    }


    /**
     * Pauses all sounds currently playing. 
     * 
     */
    private void pause()
    {
        synchronized (this) {
            ignoreEvents = true;
        }
        for (Sound sound : playingSounds) {
            sound.pause();
        }
        synchronized (this) {
            ignoreEvents = false;
        }
    }
    

    /**
     * Stops all sounds.
     * 
     */
    private void stop()
    {
        System.out.println("Sounds alive: " + playingSounds.size() + " " + pausedSounds.size());
        
        synchronized (this) {
            ignoreEvents = true;
        }
        
        Iterator<Sound> iter = playingSounds.iterator();
        while (iter.hasNext() ) {
            Sound sound = iter.next();
            iter.remove();
            sound.stop();
        }
        
        iter = pausedSounds.iterator();
        while (iter.hasNext() ) {
            Sound sound = iter.next();
            iter.remove();
            sound.stop();
        }
        playingSounds.clear();
        pausedSounds.clear();

        synchronized (this) {
            ignoreEvents = false;
        }
    }

    // Listener callbacks

    public synchronized void playbackStarted(Sound sound)
    {
        if (!ignoreEvents) {
            playingSounds.add(sound);
            pausedSounds.remove(sound);
        }
    }

    public synchronized void playbackStopped(Sound sound)
    {
        if (!ignoreEvents) {
            playingSounds.remove(sound);
            pausedSounds.remove(sound);
        }
    }

    public synchronized void playbackPaused(Sound sound)
    {
        if (!ignoreEvents) {
            pausedSounds.add(sound);
            playingSounds.remove(sound);
        }
    }
}
