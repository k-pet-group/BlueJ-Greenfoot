/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.sound.Sound;
import greenfoot.sound.SoundFactory;

/**
 * Represents audio that can be played in Greenfoot. A GreenfootSound loads the audio from a file.
 * The sound cannot be played several times simultaneously, but can be played several times sequentially. 
 * 
 * <p>Most files of the following formats are supported: AIFF, AU, WAV, MP3 and MIDI.
 * 
 * @author Poul Henriksen
 * @version 2.4
 */
public class GreenfootSound
{

    private Sound sound;
    /**
     * The name of the file where the sound is loaded from.
     */
    private String filename;

    /**
     * Creates a new sound from the given file. 
     * 
     * @param filename Typically the name of a file in the sounds directory in
     *            the project directory. 
     */
    public GreenfootSound(String filename)
    {
        this.filename = filename;
        sound = SoundFactory.getInstance().createSound(filename, false);
    }

    /**
     * Start playing this sound. If it is playing already, it will do
     * nothing. If the sound is currently looping, it will finish the current
     * loop and stop. If the sound is currently paused, it will resume playback
     * from the point where it was paused. The sound will be played once.
     */
    public void play()
    {
        sound.play();
    }

    /**
     * Play this sound repeatedly in a loop. If called on an already looping
     * sound, it will do nothing. If the sound is already playing once, it will
     * start looping instead. If the sound is currently paused, it will resume
     * playing from the point where it was paused.
     */
    public void playLoop()
    {
        sound.loop();
    }

    /**
     * Stop playing this sound if it is currently playing. If the sound is
     * played again later, it will start playing from the beginning. If the
     * sound is currently paused it will now be stopped instead.
     */
    public void stop()
    {
        sound.stop();
    }

    /**
     * Pauses the current sound if it is currently playing. If the sound is
     * played again later, it will resume from the point where it was paused.
     * <p>
     * Make sure that this is really the method you want. If possible, you
     * should always use {@link #stop()}, because the resources can be released
     * after calling {@link #stop()}. The resources for the sound will not be
     * released while it is paused.
     * @see #stop()
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
     * Get the current volume of the sound, between 0 (off) and 100 (loudest.)
     */
    public int getVolume()
    {
        return sound.getVolume();
    }

    /**
     * Set the current volume of the sound between 0 (off) and 100 (loudest.)
     * @param level the level to set the sound volume to.
     */
    public void setVolume(int level)
    {
        sound.setVolume(level);
    }

    /**
     * Returns a string representation of this sound containing the name of the
     * file and whether it is currently playing or not.
     */
    public String toString()
    {
        String s = super.toString() + " file: " + filename + " ";
        if (sound != null) {
            s += ". Is playing: " + isPlaying();
        }
        else {
            s += ". Not found.";
        }
        return s;
    }
}
