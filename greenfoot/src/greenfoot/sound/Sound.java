package greenfoot.sound;

import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public interface Sound
{    
    /**
     * Stop this sound.
     *
     */
    public void stop() ;
    
    /**
     * Pause the song. Paused sounds can be resumed.
     *
     */
    public void pause();

    /**
     * Resume a paused sound
     *
     */
    public void resume();
    
    /**
     * Play this sound.
     * @throws IOException
     * @throws UnsupportedAudioFileException
     * @throws LineUnavailableException
     */
    public void play() throws IOException, UnsupportedAudioFileException, LineUnavailableException;
    
}
