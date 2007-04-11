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
     * @throws IOException
     * @throws UnsupportedAudioFileException
     * @throws LineUnavailableException
     */
    public abstract void play();

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
