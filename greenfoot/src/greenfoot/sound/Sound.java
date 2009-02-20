/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kšlling 
 
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

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public abstract class Sound
{    
    /**
     * Stop this sound.
     *
     */
    public abstract void stop() ;
    
    /**
     * Pause the song. Paused sounds can be resumed.
     *
     */
    public abstract void pause();

    /**
     * Resume a paused sound
     *
     */
    public abstract void resume();
    
    /**
     * Play this sound. Should only be called once.
     * @throws UnsupportedAudioFileException 
     * @throws IOException 
     * @throws LineUnavailableException 
     * @throws SecurityException 
     * @throws IllegalArgumentException 
     * @throws IOException
     * @throws UnsupportedAudioFileException
     * @throws LineUnavailableException
     */
    public abstract void play() throws IllegalArgumentException, SecurityException, LineUnavailableException, IOException, UnsupportedAudioFileException;

    /**
     * Converts format to a compatible format.
     * <p>
     * TODO: needs testing! haven't tried with a non-compatible sound yet. 
     * 
     * @param format Original format
     * @return New compatible format.
     */

    protected AudioFormat getCompatibleFormat(AudioFormat format) {
      /*		AudioFormat tmp = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                format.getSampleRate(), format.getSampleSizeInBits() * 2,
                                format.getChannels(), format.getFrameSize() * 2, format
                                                .getFrameRate(), true);*/
        //    AudioFormat supportedFormat = new AudioFormat(format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(), true, false);
        
        return new AudioFormat(format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(), true, false);
    }
    
}
