/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011,2012,2013  Poul Henriksen and Michael Kolling 
 
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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Plays sound from a URL. The sound is loaded into memory the first time it is
 * played.
 * 
 * <p>Much of the complexity of this code comes from the need to work around bugs in
 * the audio support of various Java runtimes.
 * 
 * @author Poul Henriksen
 */
public class SoundClip implements Sound, LineListener
{
    private static ClipCache clipCache = new ClipCache();
    private static ClipProcessThread processThread = new ClipProcessThread();
    private static ClipCloserThread closerThread = new ClipCloserThread();

    /** URL of the sound data. */
    private final URL url;
    
    /** Data for the clip (used for caching) */
    private ClipData clipData;
    
    /**
     * The clip that this SoundClip represents. Can be null (when state is
     * CLOSED)
     */
    private Clip soundClip;

    /** The states a clip can be in. */
    private enum ClipState
    {
        STOPPED, PLAYING, PAUSED_LOOPING, PAUSED_PLAYING, CLOSED, LOOPING, STOPPING
    };
    
    /** requested clip state (never STOPPING) */
    private ClipState clipState = ClipState.CLOSED;
    
    /** actual state playing vs looping vs stopping vs stopped */
    private ClipState currentState = ClipState.STOPPED;
    
    /**
     * The master volume of the sound clip.
     */
    private int masterVolume = 100;
    
    /** Listener for state changes. */
    private SoundPlaybackListener playbackListener;
    
    private boolean resumedLoop;
    
    /** Sound has been stopped; if played again, should be played from start rather than current position */
    private boolean resetToStart;

    /**
     * Creates a new sound clip
     */
    public SoundClip(String name, URL url, SoundPlaybackListener listener)
    {
        this.url = url;
        playbackListener = listener;
    }

    /**
     * Load the sound file supplied by the parameter into this sound engine.
     */
    private boolean open()
    {
        try {
            load();
            soundClip.addLineListener(this);
            return true;
        }
        catch (SecurityException e) {
            SoundExceptionHandler.handleSecurityException(e, url.toString());
        }
        catch (IllegalArgumentException e) {
            SoundExceptionHandler.handleIllegalArgumentException(e, url.toString());
        }
        catch (FileNotFoundException e) {
            SoundExceptionHandler.handleFileNotFoundException(e, url.toString());
        }
        catch (IOException e) {
            SoundExceptionHandler.handleIOException(e, url.toString());
        }
        catch (UnsupportedAudioFileException e) {
            SoundExceptionHandler.handleUnsupportedAudioFileException(e,
                    url.toString());
        }
        catch (LineUnavailableException e) {
            SoundExceptionHandler.handleLineUnavailableException(e);
        }
        return false;
    }

    private void load() throws UnsupportedAudioFileException, IOException,
            LineUnavailableException
    {
        clipData = clipCache.getCachedClip(url);
        InputStream is = new ByteArrayInputStream(clipData.getBuffer());
        AudioFormat format = clipData.getFormat();
        AudioInputStream stream = new AudioInputStream(is, format, clipData.getLength());
        DataLine.Info info = new DataLine.Info(Clip.class, format);

        // getLine throws illegal argument exception if it can't find a line.
        soundClip = (Clip) AudioSystem.getLine(info);
        soundClip.open(stream);
        
        // Note we don't use soundClip.getMicrosecondLength() due to an IcedTea bug:
        // http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=902
        setVolume(masterVolume);
    }
    
    /**
     * Preloads the clip, by opening it.
     */
    public synchronized void preLoad()
    {
        //Ignore all exceptions when pre-loading
        try
        {
            clipData = clipCache.getCachedClip(url);
            clipCache.releaseClipData(clipData);
        }
        catch (IOException e) {
            
        }
        catch (UnsupportedAudioFileException e) {
            
        }
    }

    /*
     * @see greenfoot.sound.Sound#play()
     */
    @Override
    public synchronized void play()
    {
        if (clipState == ClipState.PLAYING) {
            return;
        }
        resumedLoop = false;
        if (soundClip == null) {
            // Open the clip in another thread
            processThread.addToQueue(this);
            currentState = ClipState.STOPPED;
        }
        else if (currentState == ClipState.STOPPED) {
            if (resetToStart) {
                resetToStart = false;
                soundClip.setFramePosition(0);
            }
            soundClip.loop(0);
            soundClip.start();
        }
        else if (currentState == ClipState.STOPPING) {
            // This state only occurs while processState is executing
            // (and stopping or pausing a clip).
            setState(ClipState.PLAYING);
            // The rest is handled in processState.
            return;
        }
        else if (currentState == ClipState.LOOPING) {
            soundClip.loop(0);
        }
        setState(ClipState.PLAYING);
        if (soundClip != null) {
            currentState = ClipState.PLAYING;
        }
    }

    /**
     * Play this sound from the beginning of the sound and loop around when the
     * end have been reached.
     */
    @Override
    public synchronized void loop()
    {
        if (clipState == ClipState.LOOPING) {
            return;
        }
        if (soundClip == null) {
            // Open the clip in another thread
            processThread.addToQueue(this);
            currentState = ClipState.STOPPED;
        }
        else if (currentState == ClipState.STOPPED) {
            if (resetToStart) {
                resetToStart = false;
                soundClip.setFramePosition(0);
            }
            soundClip.setLoopPoints(0, -1);
            soundClip.loop(Clip.LOOP_CONTINUOUSLY);
        }
        else if (currentState == ClipState.STOPPING) {
            // This state only occurs while processState is executing
            // (and stopping or pausing a clip).
            setState(ClipState.LOOPING);
            // The rest is handled in processState.
            return;
        }
        else if (currentState == ClipState.PLAYING) {
            soundClip.setLoopPoints(0, -1);
            soundClip.loop(Clip.LOOP_CONTINUOUSLY);
            resumedLoop = true;
        }
        setState(ClipState.LOOPING);
        if (soundClip != null) {
            currentState = ClipState.LOOPING;
        }
    }
    
    public void processState()
    {
        ClipState toState;
        Clip soundClip;

        // This message essentially manages a transition from one state
        // (currentState) to another (the desired state, as per clipState).
        //
        // currentState can be:
        //   PLAYING, LOOPING, STOPPED (including paused), STOPPING
        //      STOPPING only occurs while executing this method.
        //
        // There are certain combinations we won't see here:
        //  current = LOOPING, desired = PLAYING
        //  current = PLAYING, desired = LOOPING
        
        synchronized (this) {
            toState = clipState; // desired state
            soundClip = this.soundClip;
            
            if (clipState == ClipState.PLAYING) {
                if (currentState != ClipState.PLAYING) {
                    if (soundClip == null && !open()) {
                        return;
                    }
                    soundClip = this.soundClip;
                    soundClip.start();
                    currentState = ClipState.PLAYING;
                }
                return;
            }
            else if (clipState == ClipState.LOOPING) {
                if (currentState != ClipState.LOOPING) {
                    if (soundClip == null && !open()) {
                        return;
                    }
                    soundClip = this.soundClip;
                    soundClip.setFramePosition(0);
                    soundClip.setLoopPoints(0, -1);
                    soundClip.loop(Clip.LOOP_CONTINUOUSLY);
                    resumedLoop = false;
                    currentState = ClipState.LOOPING;
                }
                return;
            }
            else if (clipState == ClipState.CLOSED) {
                return;
            }
            else if (isPaused() || clipState == ClipState.STOPPED) {
                if (currentState == ClipState.PLAYING || currentState == ClipState.LOOPING) {
                    currentState = ClipState.STOPPING;
                    resumedLoop = false;
                }
                else {
                    return; // prevent code below from executing
                }
            }
        }
        
        if (toState == ClipState.STOPPED
                || toState == ClipState.PAUSED_LOOPING
                || toState == ClipState.PAUSED_PLAYING) {
            // We have to do this outside of 'synchronized' - OpenJDK seems
            // to callback the listener on a different thread, but nevertheless
            // waits for it to return before returning from soundClip.stop() below,
            // which means we'll get a deadlock if we're sync'd now.
            // Also, stop() can take quite a while to execute on OpenJDK.
            soundClip.stop();
            
            synchronized (this) {
                if (resetToStart) {
                    resetToStart = false;
                    soundClip.setFramePosition(0);
                }
                
                currentState = ClipState.STOPPED;
                
                // Possibly the clip state changed while we were stopping/pausing.
                // We need to deal with this now.
                if (clipState == ClipState.PLAYING) {
                    soundClip.loop(0);
                    soundClip.start();
                    currentState = ClipState.PLAYING;
                }
                else if (clipState == ClipState.LOOPING) {
                    soundClip.setLoopPoints(0, -1);
                    soundClip.loop(Clip.LOOP_CONTINUOUSLY);
                    currentState = ClipState.LOOPING;
                }
            }
        }
    }
    
    /**
     * Set the volume level for this sound.
     * @param level the volume level.
     */
    @Override
    public synchronized void setVolume(int level)
    {
        this.masterVolume = level;
        if (soundClip != null) {
            if (soundClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl volume = (FloatControl) soundClip.getControl(FloatControl.Type.MASTER_GAIN);
                volume.setValue(SoundUtils.convertMinMax(level, volume.getMinimum(), volume.getMaximum()));
            }
        }
    }

    /**
     * Get the volume level.
     * @return the volume level.
     */
    @Override
    public synchronized int getVolume()
    {
        return masterVolume;
    }

    /**
     * Stop this sound.
     */
    @Override
    public synchronized void stop()
    {
        if (isStopped()) {
            return;
        }
        setState(ClipState.STOPPED);
        if (soundClip != null) {
            if (currentState == ClipState.STOPPED) {
                // Release the clip.
                closerThread.addClip(soundClip);
                soundClip = null;
            }
            else {
                resetToStart = true;
                processThread.addToQueue(this);
            }
        }
    }

    /**
     * Closes this sound. It will release all the resources for this sound
     * immediately.
     */
    @Override
    public synchronized void close()
    {
        if (clipState != ClipState.CLOSED) {
            if (soundClip != null) {
                setVolume(0);
                clipCache.releaseClipData(clipData);
                closerThread.addClip(soundClip);
                soundClip = null;
            }
            setState(ClipState.CLOSED);
        }
    }

    /**
     * Pause the clip. Paused sounds can be resumed.
     */
    @Override
    public synchronized void pause()
    {
        resumedLoop = false;
        if (soundClip == null) {
            return;
        }
        if (clipState == ClipState.PLAYING) {
            setState(ClipState.PAUSED_PLAYING);
            processThread.addToQueue(this);
        }
        if (clipState == ClipState.LOOPING) {
            setState(ClipState.PAUSED_LOOPING);
            processThread.addToQueue(this);
        }
    }

    private void setState(ClipState newState)
    {
        if (clipState != newState) {
            clipState = newState;
            switch (clipState) {
                case PLAYING:
                    playbackListener.playbackStarted(this);
                    break;
                case STOPPED:
                    playbackListener.playbackStopped(this);
                    break;
                case PAUSED_LOOPING:
                    playbackListener.playbackPaused(this);
                    break;
                case PAUSED_PLAYING:
                    playbackListener.playbackPaused(this);
                    break;
                case LOOPING:
                    playbackListener.playbackStarted(this);
                    break;
                case CLOSED:
                    playbackListener.soundClosed(this);
            }
        }
    }

    /**
     * True if the sound is currently playing.
     */
    @Override
    public synchronized boolean isPlaying()
    {
        return clipState == ClipState.PLAYING || clipState == ClipState.LOOPING;
    }

    /**
     * True if the sound is currently paused.
     */
    @Override
    public synchronized boolean isPaused()
    {
        return clipState == ClipState.PAUSED_PLAYING || clipState == ClipState.PAUSED_LOOPING;
    }

    /**
     * True if the sound is currently stopped.
     */
    @Override
    public synchronized boolean isStopped()
    {
        return clipState == ClipState.STOPPED || clipState == ClipState.CLOSED;
    }

    @Override
    public void update(LineEvent event)
    {
        if (event.getType() == LineEvent.Type.STOP) {
            synchronized (this) {
                if (currentState == ClipState.STOPPING) {
                    // setState(ClipState.STOPPED);
                }
                else {
                    currentState = ClipState.STOPPED;
                    if (resumedLoop && clipState == ClipState.LOOPING) {
                        // If the sound is supposed to be looping, we shouldn't get a stop event for it.
                        // However, Java bugs on various JDKs mean that sometimes the sound stops instead
                        // of looping; furthermore it can be impossible to resume the sound. This happens
                        // when loop() is called while the sound is playing regularly (in non-looping mode).
                        //
                        // We work around that here by ditching the clip and creating a new one which we
                        // loop on (in processState method).
                        //
                        // In non-broken Java runtimes, this code shouldn't trigger, as a looping sound
                        // shouldn't fire a stop event.
                        closerThread.addClip(soundClip);
                        soundClip = null;
                        // Avoid restarting on this thread to avoid OpenJDK pulseaudio deadlock:
                        processThread.addToQueue(this);
                    }
                    else if (! isPaused()) {
                        setState(ClipState.STOPPED);
                        // May have been restarted by a listener, so this check
                        // of clipState is not as redundant as it looks:
                        if (clipState == ClipState.STOPPED && soundClip != null) {
                            // If not, we'll close.
                            closerThread.addClip(soundClip);
                            soundClip = null;
                        }
                    }
                }
            }
        }
    }

    @Override
    public String toString()
    {
        return url + " " + super.toString();
    }
}
