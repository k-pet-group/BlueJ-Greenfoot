/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011,2012  Poul Henriksen and Michael Kolling 
 
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
        STOPPED, PLAYING, PAUSED_LOOPING, PAUSED_PLAYING, CLOSED, LOOPING
    };
    
    /** requested clip state */
    private ClipState clipState = ClipState.CLOSED;
    
    /** actual state playing vs looping */
    private ClipState currentState = ClipState.CLOSED;
    
    /**
     * The master volume of the sound clip.
     */
    private int masterVolume = 100;
    
    /** Listener for state changes. */
    private SoundPlaybackListener playbackListener;
    private boolean resumedLoop;

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
        else if (soundClip == null) {
            // Open the clip in another thread
            processThread.addToQueue(this);
            currentState = ClipState.STOPPED;
        }
        else if (isPaused()) {
            soundClip.start();
        }
        else if (clipState == ClipState.LOOPING) {
            soundClip.loop(0);
        }
        else {
            // play from the start
            soundClip.setFramePosition(0);
            soundClip.loop(0);
            soundClip.start();
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
        else if (soundClip == null) {
            // Open the clip in another thread
            processThread.addToQueue(this);
            currentState = ClipState.STOPPED;
        }
        else if (isPaused()) {
            soundClip.start();
            resumedLoop = true; // loop explicitly from beginning
        }
        else if (clipState == ClipState.PLAYING) {
            resumedLoop = true;
        }
        else {
            // loop from the start
            soundClip.setFramePosition(0);
            soundClip.setLoopPoints(0, -1);
            soundClip.loop(Clip.LOOP_CONTINUOUSLY);
            soundClip.start();
        }
        setState(ClipState.LOOPING);
        if (soundClip != null) {
            currentState = ClipState.LOOPING;
        }
    }
    
    public void processState()
    {
        ClipState toState;

        synchronized (this) {
            toState = clipState;
            if (clipState == ClipState.PLAYING) {
                if (currentState != ClipState.PLAYING) {
                    if (!open()) {
                        return;
                    }
                    soundClip.start();
                    currentState = ClipState.PLAYING;
                }
            }
            else if (clipState == ClipState.LOOPING) {
                if (currentState != ClipState.LOOPING) {
                    if (!open()) {
                        return;
                    }
                    soundClip.setFramePosition(0);
                    soundClip.setLoopPoints(0, -1);
                    soundClip.loop(Clip.LOOP_CONTINUOUSLY);
                    resumedLoop = false;
                    currentState = ClipState.LOOPING;
                }
            }
            else if (isPaused() || clipState == ClipState.STOPPED) {
                if (currentState == ClipState.PLAYING || currentState == ClipState.LOOPING) {
                    currentState = clipState;
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
            if (toState == ClipState.STOPPED) {
                soundClip.setMicrosecondPosition(0);
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
            processThread.addToQueue(this);
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
                if (resumedLoop && clipState == ClipState.LOOPING) {
                    // Avoid restarting on this thread to avoid OpenJDK pulseaudio deadlock:
                    processThread.addToQueue(this);
                }
                else if (! isPaused()) {
                    setState(ClipState.STOPPED);
                    // May have been restarted by a listener:
                    if (clipState == ClipState.STOPPED && soundClip != null) {
                        // If not, we'll close.
                        closerThread.addClip(soundClip);
                        soundClip = null;
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
