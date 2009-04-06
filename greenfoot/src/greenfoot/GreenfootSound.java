/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot;

import java.io.FileNotFoundException;
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

    // Whether we have handled certain exceptions. We use these flags to ensure
    // we only print an error message once to avoid flooding the error output.
    private static boolean lineUnavailableHandled;
    private static boolean illegalArgumentHandled;
    private static boolean securityHandled;
    
    private Sound sound;
    private String filename;
    
    /**
     * Creates a new sound from the given file. The following formats are
     * supported: AIFF, AU and WAV.
     * 
     * @param filename Typically the name of a file in the sounds directory in
     *            the project directory. 
     * @throws IllegalArgumentException If the file cannot be opened.
     */
    public GreenfootSound(String filename) throws IllegalArgumentException
    {
        this.filename = filename;
        try {
            sound = SoundFactory.getInstance().createSound(filename);
        }
        catch (SecurityException e) {
            handleSecurityException(e);
        }
        catch (IllegalArgumentException e) {
            handleIllegalArgumentException(e);
        }
        catch (FileNotFoundException e) {
            handleFileNotFoundException(e);
        }
        catch (IOException e) {
            handleIOException(e);
        }
        catch (UnsupportedAudioFileException e) {
            handleUnsupportedAudioFileException(e);
        }
        catch (LineUnavailableException e) {
            handleLineUnavailableException(e);
        }        
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
        catch (SecurityException e) {
            handleSecurityException(e);
        }
        catch (IllegalArgumentException e) {
            handleIllegalArgumentException(e);
        }
        catch (FileNotFoundException e) {
            handleFileNotFoundException(e);
        }
        catch (IOException e) {
            handleIOException(e);
        }
        catch (UnsupportedAudioFileException e) {
            handleUnsupportedAudioFileException(e);
        }
        catch (LineUnavailableException e) {
            handleLineUnavailableException(e);
        }
    }

    private void handleUnsupportedAudioFileException(UnsupportedAudioFileException e)
    {
        throw new IllegalArgumentException("Format of sound file not supported: " + filename, e);
    }

    private void handleFileNotFoundException(FileNotFoundException e)
    {
        throw new IllegalArgumentException("Could not find sound file: " + filename, e);
    }
    
    private void handleIOException(IOException e)
    {
        throw new IllegalArgumentException("Could not open sound file: " + filename, e);
    }

    private void handleLineUnavailableException(LineUnavailableException e)
    {
        // We only want to print this error message once.
        if(! lineUnavailableHandled) {
            System.err.println("Cannot get access to the sound card. "
                + "If you have a sound card installed, check your system settings, "
                + "and close down any other programs that might be using the sound card.");
            e.printStackTrace();
            lineUnavailableHandled = true;
        }
    }

    private void handleIllegalArgumentException(IllegalArgumentException e)
    {
        // We only want to print this error message once.
        if (!illegalArgumentHandled) {
            System.err.println("Could not play sound file: " + filename);
            System.err.println("If you have a sound card installed, check your system settings.");
            e.printStackTrace();
            illegalArgumentHandled = true;
        }
    }

    private void handleSecurityException(SecurityException e)
    {
        // We only want to print this error message once.
        if (!securityHandled) {
            System.err.println("Could not play sound file due to security restrictions: " + filename);
            System.err.println("If you have a sound card installed, check your system settings.");
            e.printStackTrace();
            securityHandled = true;
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
