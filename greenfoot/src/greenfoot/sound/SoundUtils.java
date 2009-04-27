package greenfoot.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

public class SoundUtils
{

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
	public static long getTimeToPlayFrames(long frames, AudioFormat format) {
	    if (format.getFrameRate() != AudioSystem.NOT_SPECIFIED) {
			return (long) (1000 * frames / format.getFrameRate());
		} else {
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
