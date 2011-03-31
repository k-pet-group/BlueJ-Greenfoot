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
package greenfoot.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

public class SoundUtils
{

    /**
     * Convert an integer value between 0-100 between the specified floating
     * point values.
     * @param val the value to convert.
     * @param min the minimum floating point value.
     * @param max the maximum floating point value.
     * @return a float between the two values assuming val is between 0-100.
     */
    public static float convertMinMax(int val, float min, float max)
    {
        float range = max - min;
        float newVal = val / (100 / range);
        return newVal + min;
    }

    /**
     * Convert a value on a logarithmic scale between 0-100 to a linear scale
     * in the same range.
     * @param level the logarithmic level.
     * @return the linear level.
     */
    public static int logToLin(int level) {
        return (int) ((Math.log(level) / Math.log(100)) * 100);
    }

    /**
     * Calculate how long it will take to play the given number of bytes.
     * 
     * @param bytes Number of bytes.
     * @param format The format used to play the bytes.
     * @return time in ms or -1 if it could not be calculated.
     */
    public static long getTimeToPlayBytes(long bytes, AudioFormat format)
    {
        return getTimeToPlayFrames(bytes / format.getFrameSize(), format);
    }

    /**
     * Calculate how long it will take to play the given number of frames.
     * 
     * @param bytes Number of bytes.
     * @param format The format used to play the bytes.
     * @return time in ms or -1 if it could not be calculated.
     */
    public static long getTimeToPlayFrames(long frames, AudioFormat format)
    {
        if (format.getFrameRate() != AudioSystem.NOT_SPECIFIED) {
            return (long) (1000 * frames / format.getFrameRate());
        }
        else {
            return -1;
        }
    }

    /**
     * Will attempt to calculate a buffer size that can hold the given time of
     * audio data. If unsuccessful it will return -1.
     * 
     * @return size in bytes.
     */
    public static int getBufferSizeToHold(AudioFormat format, double seconds)
    {
        int bufferSize;
        if (format.getFrameRate() != AudioSystem.NOT_SPECIFIED) {
            bufferSize = (int) Math.ceil(format.getFrameSize() * format.getFrameRate() * seconds);
        }
        else if (format.getSampleRate() != AudioSystem.NOT_SPECIFIED) {
            bufferSize = (int) Math.ceil((format.getSampleSizeInBits() / 8) * format.getChannels()
                    * format.getSampleRate() * seconds);
        }
        else {
            bufferSize = -1;
        }
        return bufferSize;
    }
}
