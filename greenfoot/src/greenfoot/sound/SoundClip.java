package greenfoot.sound;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Plays sound from a URL. The sound is loaded into memory before it is played.
 * 
 * @author Poul Henriksen
 * 
 */
public class SoundClip extends Sound
    implements LineListener
{
    private Clip soundClip;

    private SoundPlayer player;

    private URL url;

    private boolean hasPlayed;

    public SoundClip(URL url, SoundPlayer player)
        throws LineUnavailableException, IOException, UnsupportedAudioFileException
    {
        this.player = player;
        this.url = url;
        open();
    }

    /**
     * Load the sound file supplied by the parameter into this sound engine.
     * 
     * @throws LineUnavailableException
     * @throws IOException
     * @throws UnsupportedAudioFileException
     */
    private void open()
        throws LineUnavailableException, IOException, UnsupportedAudioFileException
    {
        AudioInputStream stream = AudioSystem.getAudioInputStream(url);
        AudioFormat format = stream.getFormat();
        DataLine.Info info = new DataLine.Info(Clip.class, format);

        // Convert sound formats that are not supported
        if (!AudioSystem.isLineSupported(info)) {
            format = getCompatibleFormat(format);
            stream = AudioSystem.getAudioInputStream(format, stream);
            info = new DataLine.Info(Clip.class, stream.getFormat(), ((int) stream.getFrameLength() * format
                    .getFrameSize()));
        }
        soundClip = (Clip) AudioSystem.getLine(info);
        soundClip.open(stream);
        soundClip.addLineListener(this);
    }

    /**
     * Stop this sound.
     * 
     */
    public void stop()
    {
        soundClip.stop();
        soundClip.setMicrosecondPosition(0);
    }

    /**
     * Pause the song. Paused sounds can be resumed.
     * 
     */
    public void pause()
    {
        soundClip.stop();
    }

    /**
     * Resume a paused sound.
     * 
     */
    public void resume()
    {
        soundClip.start();
    }

    /**
     * Play this sound.
     */
    public void play()
    {
        if(hasPlayed) {
            throw new IllegalStateException("This sound has already been played.");
        }
        hasPlayed = true;
        soundClip.setMicrosecondPosition(0);
        soundClip.start();
    }

    public void update(LineEvent event)
    {
        if (event.getType() == LineEvent.Type.STOP) {
            player.soundFinished(this);
        }
    }

    public String toString()
    {
        return url + " " + super.toString();
    }
}
