package greenfoot;

import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import greenfoot.sound.Sound;
import greenfoot.sound.SoundFactory;

/**
 * Represents audio that can be played in Greenfoot. GreenfootSound represent
 * one sound that can be played, stopped, looped etc.
 * 
 * TODO:
 *  
 * Midi (playback to start with, maybe creation later)
 * 
 * What happens if playback is started from a static method? Should it depends on whether
 * simulation is running?
 * Should we allow playback if the simulation is not running? Probably not * 
 * 
 * Add support MP3, OGG etc
 * 
 * Maybe don't throw exceptions in constructor?
 * 
 * @author Poul Henriksen
 * @version 2.0
 */
public class GreenfootSound
{

    private Sound sound;

    /**
     * Creates a new sound from the given file. The following formats are
     * supported: AIFF, AU and WAV.
     * 
     * @param filename Typically the name of a file in the sounds directory in
     *            the project directory.
     * @throws LineUnavailableException 
     * @throws UnsupportedAudioFileException 
     * @throws IOException 
     */
    public GreenfootSound(String filename) throws IOException, UnsupportedAudioFileException, LineUnavailableException
    {
        
         sound = SoundFactory.getInstance().createSound(filename);
       
    }

    /**
     * Stop playback of this sound if it is currently playing. If the sound is
     * played again later, it will start playing from the beginning. If the
     * sound is currently paused it will now be stopped instead.
     */
    public void stop()
    {
        sound.stop();
    }

    /**
     * Start playback of this sound if it is not currently playing. If it is
     * playing already, it will restart playback from the beginning. It will
     * play the sound once.
     * 
     */
    public void play()
    {
        // TODO: check if simulation is running
        try {
            sound.play();
        }
        catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (LineUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (UnsupportedAudioFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Play this sound in a loop until it is explicitly stopped, or the current
     * execution is stopped.
     * 
     * NOT IMPLEMENTED YET
     */
    public void loop()
    {
    // TODO: check if simulation is running
    // If it is currently playing, should it begin looping instead? Probably.
    // Should it restart from the beginning or just continue form where it is?
    }

    /**
     * Pauses the current playback of this sound. If the sound playback is
     * started again later, it will resume from the point where it was paused.
     */
    public void pause()
    {
        sound.pause();
    }

    /**
     * True if the sound is currently playing.
     * 
     */
    public boolean isPlaying()
    {
        return sound.isPlaying();
    }

    /**
     * True if the sound is currently paused.
     * 
     */
    public boolean isPaused()
    {
        return sound.isPlaying();
    }
}
