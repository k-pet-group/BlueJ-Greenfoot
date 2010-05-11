/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Plays sound from a URL. The sound is loaded into memory the first time it is
 * played.
 * 
 * @author Poul Henriksen
 * 
 */
public class SoundClip implements Sound
{
    private void printDebug(String s) 
    {
          //System.out.println(s);        
    }
    
    /** Name of the file holding the sound data. Used for debugging. */
    private final String name;
    
    /** URL of the sound data. */
    private final URL url;

    /**
     * The clip that this SoundClip represents. Can be null (when state is
     * CLOSED)
     */
    private Clip soundClip;

    /** The states a clip can be in. */
    private enum ClipState {
        STOPPED, PLAYING, PAUSED_LOOPING, PAUSED_PLAYING, CLOSED, LOOPING
    };

    private ClipState clipState = ClipState.CLOSED;
    
    private TimeTracker playedTimeTracker = new TimeTracker();
    
    private TimeTracker stoppedTimeTracker = new TimeTracker();
    
    /** Length of this clip in ms. */
    private long clipLength;
    
    /**
     * Thread that closes this sound clip after a timeout.
     */
    private Thread closeThread;
    
    /**
     * How long to wait until closing the clip after playback has finished. In
     * ms.
     */
    private static final int CLOSE_TIMEOUT = 20000;
    

    /**
     * Indicates that the clips should be closed as soon as playback has
     * finished. This will effectively ignore the CLOSE_TIMEOUT.
     */
    private boolean closeWhenFinished = false;

    /**
     * Extra delay in ms added to the sleep time before closing the clip. This
     * is just an extra buffer of time to make sure we don't close it too soon.
     * This helps avoid stopping the sound too soon which seems to happen on
     * some Linux systems.
     */
    private final static int EXTRA_SLEEP_DELAY = 50;

    /** Listener for state changes. */
    private SoundPlaybackListener playbackListener;

    private boolean resumedLoop;

    /**
     * Creates a new sound clip
     */
    public SoundClip(String name, URL url, SoundPlaybackListener listener)
    {
        this.name = name;
        this.url = url;
        playbackListener = listener;
    }

    /**
     * Load the sound file supplied by the parameter into this sound engine.
     * 
     */
    private boolean open()
    {
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(url);
            AudioFormat format = stream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            // getLine throws illegal argument exception if it can't find a line.
            soundClip = (Clip) AudioSystem.getLine(info); 
            soundClip.open(stream);
            clipLength = soundClip.getMicrosecondLength() / 1000;
            setState(ClipState.STOPPED);
            return true;
        } catch (SecurityException e) {
            SoundExceptionHandler.handleSecurityException(e, url.toString());
        } catch (IllegalArgumentException e) {
            SoundExceptionHandler.handleIllegalArgumentException(e, url.toString());
        } catch (FileNotFoundException e) {
            SoundExceptionHandler.handleFileNotFoundException(e, url.toString());
        } catch (IOException e) {
            SoundExceptionHandler.handleIOException(e, url.toString());
        } catch (UnsupportedAudioFileException e) {
            SoundExceptionHandler.handleUnsupportedAudioFileException(e,
                    url.toString());
        } catch (LineUnavailableException e) {
            SoundExceptionHandler.handleLineUnavailableException(e);
        }
        return false;
    }

    /**
     * Play this sound from the beginning of the sound.    
     */
    public synchronized void play()
    {

        printDebug("00");

        if(clipState == ClipState.PLAYING) {
            return;
        }

        if (soundClip == null || clipState == ClipState.CLOSED) {
            if(! open()) {
                return;
            }
        }

        printDebug("1");

        if(clipState == ClipState.LOOPING) {
            // Play the current loop till the end, then stop.
            playedTimeTracker.setTimeTracked(SoundUtils.getTimeToPlayFrames(soundClip.getLongFramePosition() % soundClip.getFrameLength(), soundClip.getFormat()));
            soundClip.loop(0);
        }
        else if (clipState == ClipState.STOPPED){
            printDebug("a2.5");	
            playedTimeTracker.reset();
            soundClip.setFramePosition(0);
            printDebug("a3");
            soundClip.start();
        }
        else {
            printDebug("b2.5");	
            soundClip.start();
            printDebug("b3");
        }
        setState(ClipState.PLAYING);
        playedTimeTracker.start();
        stoppedTimeTracker.reset();
        printDebug("play: " + this);
        startCloseThread();

    }

    /**
     * Play this sound from the beginning of the sound and loop around when the
     * end have been reached.
     * 
     */
    public synchronized void loop()
    {
        if(clipState == ClipState.LOOPING) {
            return;
        }
        if (soundClip == null || clipState == ClipState.CLOSED) {
            if(! open()) {
                return;
            }
        } else if(soundClip != null) {
            soundClip.stop();
        }          

        if(clipState == ClipState.PLAYING || isPaused()) {
            // Clip.loop will only loop from current frame to endframe,
            // NOT from beginning frame as it should. To fix this, we have to
            // use play() once instead, then detect when that has finished, and
            // then start looping again. We restart looping in the closeThread.
            playedTimeTracker.setTimeTracked(SoundUtils.getTimeToPlayFrames(soundClip.getLongFramePosition() % soundClip.getFrameLength(), soundClip.getFormat()));
            playedTimeTracker.start();
            soundClip.start();
            resumedLoop = true;
            startCloseThread();            
        }
        else {
            resumedLoop = false;  	
            notifyAll(); // Make sure the kill thread is stopped.
            soundClip.setMicrosecondPosition(0);
            soundClip.setLoopPoints(0, -1);
            stoppedTimeTracker.reset();
            playedTimeTracker.reset();
            soundClip.loop(Clip.LOOP_CONTINUOUSLY);
        }

        setState(ClipState.LOOPING);
    }

    /**
     * Stop this sound.
     * 
     */
    public synchronized void stop()
    {
        if (soundClip == null || isStopped()) {
            return;
        }
        setState(ClipState.STOPPED);
        playedTimeTracker.reset();
        stoppedTimeTracker.reset();
        stoppedTimeTracker.start();
        soundClip.stop();
        soundClip.setMicrosecondPosition(0);
        printDebug("Stop: " + this);
    }

    /**
     * Closes this sound. It will release all the resources for this sound
     * immediately.
     * 
     */
    public synchronized void close()
    {
        if (soundClip == null) {
            return;
        }
        setState(ClipState.CLOSED);
        playedTimeTracker.reset();
        stoppedTimeTracker.reset();
        soundClip.close();
        soundClip = null;
        closeThread = null;
        printDebug("Closed: " + this);
    }

    public synchronized void setCloseWhenFinished(boolean b) 
    {
        closeWhenFinished = b;
        notifyAll();
    }

    /**
     * Pause the clip. Paused sounds can be resumed.
     * 
     */
    public synchronized void pause()
    {
        resumedLoop = false;
        playedTimeTracker.pause();
        if (soundClip == null || isPaused()) {
            return;
        }
        if(clipState == ClipState.PLAYING) {
            setState(ClipState.PAUSED_PLAYING);
        }
        if(clipState == ClipState.LOOPING) {
            setState(ClipState.PAUSED_LOOPING);
        }
        soundClip.stop();
        printDebug("Pause: " + this);
    }

    /**
     * Resume a paused clip. If the clip is not currently paused, this call will
     * do nothing
     * 
     */
    public synchronized void resume()
    {
        if (soundClip == null || !isPaused()) {
            return;
        }
        playedTimeTracker.start();

        if(clipState == ClipState.PAUSED_PLAYING) {
            setState(ClipState.PLAYING);
            soundClip.start();
        }
        if(clipState == ClipState.PAUSED_LOOPING) {
            setState(ClipState.LOOPING);
            // Clip.loop will only loop from current frame to endframe,
            // NOT from beginning frame as it should. To fix this, we have to
            // use play() once instead, then detect when that has finished, and
            // then start looping again. We restart looping in the closeThread.
            playedTimeTracker.setTimeTracked(SoundUtils.getTimeToPlayFrames(soundClip.getLongFramePosition() % soundClip.getFrameLength(), soundClip.getFormat()));
            soundClip.start();
            resumedLoop = true;
            startCloseThread();
        }
        printDebug("Resume: " + this);
    }

    private void setState(ClipState newState)
    {
        if (clipState != newState) {
            printDebug("Setting state to: " + newState);
            clipState = newState;
            switch(clipState) {
            case PLAYING :
                playbackListener.playbackStarted(this);
                break;
            case STOPPED :
                playbackListener.playbackStopped(this);
                break;
            case PAUSED_LOOPING :
                playbackListener.playbackPaused(this);
                break;
            case PAUSED_PLAYING :
                playbackListener.playbackPaused(this);
                break;
            case LOOPING :
                playbackListener.playbackStarted(this);
                break;
            case CLOSED :
                playbackListener.soundClosed(this);
            }
        }

        // The close thread might be waiting, so we wake it up.
        this.notifyAll();
    }

    /**
     * Get a name for this sound. The name should uniquely identify the sound
     * clip.
     */
    public String getName()
    {
        return name;
    }

    /**
     * True if the sound is currently playing.
     */
    public synchronized boolean isPlaying()
    {
        return clipState == ClipState.PLAYING || clipState == ClipState.LOOPING;
    }

    /**
     * True if the sound is currently paused.
     */
    public synchronized boolean isPaused()
    {
        return clipState == ClipState.PAUSED_PLAYING || clipState == ClipState.PAUSED_LOOPING;
    }

    /**
     * True if the sound is currently stopped.
     */
    public synchronized boolean isStopped()
    {
        return clipState == ClipState.STOPPED || clipState == ClipState.CLOSED;
    }

    /**
     * Close the clip when it should have finished playing. This will be done
     * asynchronously.
     * 
     * The reason we are using this is instead of listening for LineEvent.STOP
     * is that on some linux systems the LineEvent for stop is send before
     * playback has actually stopped.
     * 
     * @param sleepTime Minimum time to wait before closing the stream.
     */
    private synchronized void startCloseThread()
    {
        if (closeThread == null) {
            closeThread = new Thread("SoundClipCloseThread") {
                public void run()
                {
                    boolean stayAlive = true;
                    printDebug("closeThread.run()");
                    SoundClip thisClip = SoundClip.this;
                    while (stayAlive && thisClip.soundClip != null) {
                        synchronized (thisClip) {
                            long playTime = playedTimeTracker.getTimeTracked();                           
                            long timeLeftOfPlayback = clipLength - playTime + EXTRA_SLEEP_DELAY;
                            long timeLeftToClose = CLOSE_TIMEOUT - stoppedTimeTracker.getTimeTracked();

                            switch (clipState) {
                            case LOOPING:
                                printDebug("looping");
                                if (resumedLoop && timeLeftOfPlayback <= 0) {
                                    printDebug("Resuming loop in closethread.");
                                    soundClip.stop();
                                    soundClip.setFramePosition(0);
                                    soundClip.setLoopPoints(0, -1);
                                    soundClip.loop(Clip.LOOP_CONTINUOUSLY);
                                    resumedLoop = false;		
                                    thisClip.closeThread = null;
                                    stayAlive = false;						
                                } else if (!resumedLoop){
                                    printDebug("Cancelling close thread because of loop started.");
                                    thisClip.closeThread = null;
                                    stayAlive = false;
                                } else {
                                    printDebug("Waiting for loop to finish: " + timeLeftOfPlayback);
                                    try {
                                        thisClip.wait(timeLeftOfPlayback);
                                    } catch (InterruptedException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                }
                                break;
                            case PLAYING:
                                if (timeLeftOfPlayback > 0) {
                                    printDebug("Waiting to stop playback: "
                                            + timeLeftOfPlayback);
                                    try {
                                        thisClip.wait(timeLeftOfPlayback);
                                    } catch (InterruptedException e) {
                                        // TODO Handle this!
                                        e.printStackTrace();
                                    }
                                    printDebug("Wait done playback");
                                } else {
                                    thisClip.stop();
                                }
                                break;

                            case PAUSED_PLAYING:
                            case PAUSED_LOOPING:
                                printDebug(" waiting in pause: " + clipState);
                                try {
                                    thisClip.wait();
                                } catch (InterruptedException e) {
                                    // TODO Handle this!
                                    e.printStackTrace();
                                }
                                break;

                            case STOPPED:
                                if (timeLeftToClose > 0 && !closeWhenFinished) {
                                    printDebug("Waiting to close: "
                                            + timeLeftToClose);
                                    try {
                                        thisClip.wait(timeLeftToClose);
                                    } catch (InterruptedException e) {
                                        // TODO Handle this!
                                        e.printStackTrace();
                                    }
                                    printDebug("Wait done close");
                                } else {	
                                    printDebug("Autoclosing clip: " + thisClip.name);
                                    thisClip.close();
                                }
                                break;
                            case CLOSED:
                                printDebug("Closing clip: " + thisClip.name);
                                stayAlive = false;
                                break;
                            }                            
                        }
                    }
                }
            };
            closeThread.start();
        }
    }

    public String toString()
    {
        return url + " " + super.toString();
    }
}