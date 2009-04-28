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
import greenfoot.sound.SoundExceptionHandler;
import greenfoot.sound.SoundFactory;

/**
 * Represents audio that can be played in Greenfoot. GreenfootSound represent
 * one sound that can be played, stopped, looped etc.
 * 
 * TODO:
 * 
 * Midi (playback of a file to start with, maybe creation later)
 * 
 * What happens if playback is started from a static method? Should it depend on
 * whether simulation is running? Should we allow playback if the simulation is
 * not running? Probably not
 * 
 * Add support for MP3, OGG etc
 * 
 * Currently play and loop are inconsistent in behaviour because they behave
 * differently depending on whether the sound is paused or not. Maybe we need a
 * resume() method to resume playback from a paused sound?
 * 
 * Do we need a isLooping() method?
 * 
 * Do we need a close() to release resources? Do we then need an open as well?
 * 
 * @author Poul Henriksen
 * @version 2.0
 */
public class GreenfootSound
{
    private Sound sound;
    private String filename;
    
    /**
     * Creates a new sound from the given file. The following formats are
     * supported: AIFF, AU and WAV.
     * 
     * @param filename Typically the name of a file in the sounds directory in
     *            the project directory. 
     */
    public GreenfootSound(String filename)
    {
        this.filename = filename;
        try {
            sound = SoundFactory.getInstance().createSound(filename);
        }
        catch (SecurityException e) {
            SoundExceptionHandler.handleSecurityException(e, filename);
        }
        catch (IllegalArgumentException e) {
        	SoundExceptionHandler.handleIllegalArgumentException(e, filename);
        }
        catch (FileNotFoundException e) {
        	SoundExceptionHandler.handleFileNotFoundException(e, filename);
        }
        catch (IOException e) {
        	SoundExceptionHandler.handleIOException(e, filename);
        }
        catch (UnsupportedAudioFileException e) {
        	SoundExceptionHandler.handleUnsupportedAudioFileException(e, filename);
        }
        catch (LineUnavailableException e) {
        	SoundExceptionHandler.handleLineUnavailableException(e);
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
	 * Start playback of this sound. If it is playing already, it will restart
	 * playback from the beginning. If the sound is currently paused, it will
	 * resume playback from the point where it was paused. The sound will be
	 * played once.
	 */
	public void play()
    {
        // TODO: check if simulation is running
        try {
            sound.play();
        }
        catch (SecurityException e) {
            SoundExceptionHandler.handleSecurityException(e, filename);
        }
        catch (IllegalArgumentException e) {
        	SoundExceptionHandler.handleIllegalArgumentException(e, filename);
        }
        catch (FileNotFoundException e) {
        	SoundExceptionHandler.handleFileNotFoundException(e, filename);
        }
        catch (IOException e) {
        	SoundExceptionHandler.handleIOException(e, filename);
        }
        catch (UnsupportedAudioFileException e) {
        	SoundExceptionHandler.handleUnsupportedAudioFileException(e, filename);
        }
        catch (LineUnavailableException e) {
        	SoundExceptionHandler.handleLineUnavailableException(e);
        }
    }

	/**
	 * Play this sound in a loop until it is explicitly stopped, or the current
	 * execution is stopped. If called on an already looping sound, it will
	 * restart the loop from the beginning of the sound. If the sound is
	 * currently paused, it will resume playback from the point where it was
	 * paused.
	 */
	public void loop() 
	{
    	try {
			sound.loop();
		} catch (SecurityException e) {
			SoundExceptionHandler.handleSecurityException(e, filename);
		} catch (IllegalArgumentException e) {
			SoundExceptionHandler.handleIllegalArgumentException(e, filename);
		} catch (FileNotFoundException e) {
			SoundExceptionHandler.handleFileNotFoundException(e, filename);
		} catch (IOException e) {
			SoundExceptionHandler.handleIOException(e, filename);
		} catch (UnsupportedAudioFileException e) {
			SoundExceptionHandler.handleUnsupportedAudioFileException(e, filename);
		} catch (LineUnavailableException e) {
			SoundExceptionHandler.handleLineUnavailableException(e);
		}
	}

	/**
	 * Pauses the current playback of this sound. If the sound playback is
	 * started again later, it will resume from the point where it was paused.
	 * The resources for the sound will not be released until it is unpaused.
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
        return sound.isPaused();
    }
    
    /**
	 * True if the sound is currently stopped. This means that either stop() has
	 * been called or the sound has played to the end.
	 */
    public boolean isStopped()
    {
    	return sound.isStopped();
    }
}
