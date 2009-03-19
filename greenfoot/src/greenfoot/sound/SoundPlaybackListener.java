package greenfoot.sound;

/**
 * Interface for listeners that wants to get notified when playback of a sound has started or stopped.
 * 
 * @author Poul Henriksen
 */
public interface SoundPlaybackListener
{
    public void playbackStarted(Sound sound);
    public void playbackPaused(Sound sound);
    public void playbackStopped(Sound sound);
}
