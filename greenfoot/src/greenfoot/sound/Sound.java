/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011,2012  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.sound;

/**
 * Interface for different types of sounds supported by Greenfoot.
 * 
 * @see SoundStream
 * @see MidiFileSound
 * @see SoundClip
 * @author Poul Henriksen 
 *
 */
public interface Sound
{

    /**
     * Closes this sound. Will immediately release any resources for the sound.
     */
    public abstract void close();

    /**
     * Stop this sound. 
     *
     * After this method has been called: isStopped=true, isPlaying=false, isPaused=false.
     */
    public abstract void stop();

    /**
     * Pause the song. Paused sounds can be resumed.
     *
     * After this method has been called: isStopped=false, isPlaying=false, isPaused=true.
     */
    public abstract void pause();

    /**
     * Play this sound. 
     * 
     * After this method has been called and no exception occurs: isStopped=false, isPlaying=true, isPaused=false.
     * If a problem occurs it should be: isStopped=true, isPlaying=false, isPaused=false.
     * 
     */
    public abstract void play();

    /**
     * Plays this sound in a loop. 
     * 
     * After this method has been called and no exception occurs: isStopped=false, isPlaying=true, isPaused=false.
     * If a problem occurs it should be: isStopped=true, isPlaying=false, isPaused=false.
     * 
     */
    public abstract void loop();

    /**
     * True if the sound is currently playing.
     * 
     */
    public abstract boolean isPlaying();

    /**
     * True if the sound is currently paused.
     * 
     */
    public abstract boolean isPaused();

    /**
     * True if the sound is currently paused.
     * 
     */
    public abstract boolean isStopped();

    /**
     * Set the sound volume.
     * @param level the level between 0-100 of the sound.
     */
    public abstract void setVolume(int level);

    /**
     * Get the current volume of the sound.
     * @return the sound volume, between 0-100.
     */
    public abstract int getVolume();
}
