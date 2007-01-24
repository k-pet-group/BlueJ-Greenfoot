package greenfoot.sound;

import greenfoot.util.GreenfootUtil;

import java.applet.Applet;
import java.applet.AudioClip;
import java.io.File;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;

/**
 * A sound that is loaded into memory before it is played.
 * @author Poul Henriksen
 *
 */
public class SoundClip implements Sound
{
    public static SoundEngine soundEngine = new SoundEngine();

    private URL url;
    private boolean stop;
    private boolean pause;
    private SoundPlayer player;
//    private AudioClip clip;

    private Clip soundClip;
    
    public SoundClip(String file, SoundPlayer player) {
        url = GreenfootUtil.getURL(file, "sounds");
       // stop = false;
        this.player = player;
      //  clip = Applet.newAudioClip(url);
        loadSound(url);
    }
    

    /**
     * Load the sound file supplied by the parameter into this sound engine.
     * 
     * @return  True if successful, false if the file could not be decoded.
     */
    private boolean loadSound(URL url) 
    {
       // currentSoundDuration = 0;

        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(url);
            AudioFormat format = stream.getFormat();

            // we cannot play ALAW/ULAW, so we convert them to PCM
            //
            if ((format.getEncoding() == AudioFormat.Encoding.ULAW) ||
                (format.getEncoding() == AudioFormat.Encoding.ALAW)) 
            {
                AudioFormat tmp = new AudioFormat(
                                          AudioFormat.Encoding.PCM_SIGNED, 
                                          format.getSampleRate(),
                                          format.getSampleSizeInBits() * 2,
                                          format.getChannels(),
                                          format.getFrameSize() * 2,
                                          format.getFrameRate(),
                                          true);
                stream = AudioSystem.getAudioInputStream(tmp, stream);
                format = tmp;
            }
            DataLine.Info info = new DataLine.Info(Clip.class, 
                                           stream.getFormat(),
                                           ((int) stream.getFrameLength() *
                                           format.getFrameSize()));

            soundClip = (Clip) AudioSystem.getLine(info);
            soundClip.open(stream);
          /*  currentSoundFrameLength = (int) stream.getFrameLength();
            currentSoundDuration = (int) (soundClip.getBufferSize() / 
                              (soundClip.getFormat().getFrameSize() * 
                              soundClip.getFormat().getFrameRate()));*/
            return true;
        } catch (LineUnavailableException e) {
           // e.printStackTrace();
            soundClip = null;
            return false;
        }
        catch (Exception ex) {
           // ex.printStackTrace();
            soundClip = null;
            return false;
        }
    }
    
    /**
     * Stop this sound.
     *
     */
    public void stop() {
        soundClip.stop();
    }
    
    /**
     * Pause the song. Paused sounds can be resumed.
     * <p>
     * Not implemented for clips.
     *
     */
    public void pause() {
        
    }

    /**
     * Resume a paused sound.
     * <p>
     * Not implemented for clips.
     *
     */
    public void resume()
    {
        
    }
    
    /**
     * Play this sound.
     */
    public void play() 
    {
            soundClip.start();
    }
    
    
    
}
