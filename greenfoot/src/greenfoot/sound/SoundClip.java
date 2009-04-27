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
public class SoundClip extends Sound
{
    private void printDebug(String s) 
    {
       //   System.out.println(s);        
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

    /** The four states a clip can be in. */
    private enum ClipState {
        STOPPED, PLAYING, PAUSED, CLOSED
    };

    private ClipState clipState = ClipState.CLOSED;
    
    private TimeTracker timeTracker = new TimeTracker();
    
    /** Length of this clip in ms. */
    private long clipLength;
    
    /**
     * Thread that closes this sound clip after a timeout.
     */
    private Thread closeThread;
    
    /**
     * How long to wait until closing the line after playback has finished. In
     * ms.
     */
    private static final int CLOSE_TIMEOUT = 2000;

	/**
	 * Extra delay in ms added to the sleep time before closing the clip. This
	 * is just an extra buffer of time to make sure we don't close it too soon.
	 * Only really needed if CLOSE_TIMEOUT is very low, and only on some
	 * systems.
	 */
    private final static int EXTEA_SLEEP_DELAY = 300;

    /** Listener for state changes. */
    private SoundPlaybackListener playbackListener;

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
     * @throws LineUnavailableException if a matching line is not available due
     *             to resource restrictions
     * @throws IOException if an I/O exception occurs
     * @throws SecurityException if a matching line is not available due to
     *             security restrictions
     * @throws UnsupportedAudioFileException if the URL does not point to valid
     *             audio file data
     * @throws IllegalArgumentException if the system does not support at least
     *             one line matching the specified Line.Info object through any
     *             installed mixer
     */
    private void open()
        throws LineUnavailableException, IOException, UnsupportedAudioFileException, IllegalArgumentException,
        SecurityException
    {
        AudioInputStream stream = AudioSystem.getAudioInputStream(url);
        AudioFormat format = stream.getFormat();
        DataLine.Info info = new DataLine.Info(Clip.class, format);

        // Convert sound formats that are not supported
        // TODO: Check that this works
        if (!AudioSystem.isLineSupported(info)) {
            format = getCompatibleFormat(format);
            stream = AudioSystem.getAudioInputStream(format, stream);
            info = new DataLine.Info(Clip.class, stream.getFormat(), ((int) stream.getFrameLength() * format
                    .getFrameSize()));
        }
        soundClip = (Clip) AudioSystem.getLine(info); // getLine throws illegal argument exception if it can't find a line.
        soundClip.open(stream);
        clipLength = soundClip.getMicrosecondLength() / 1000;
        setState(ClipState.CLOSED);
    }

    /**
     * Play this sound from the beginning of the sound.
     * 
     * @throws LineUnavailableException if a matching line is not available due
     *             to resource restrictions
     * @throws IOException if an I/O exception occurs
     * @throws SecurityException if a matching line is not available due to
     *             security restrictions
     * @throws UnsupportedAudioFileException if the URL does not point to valid
     *             audio file data
     * @throws IllegalArgumentException if the system does not support at least
     *             one line matching the specified Line.Info object through any
     *             installed mixer
     */
    public synchronized void play()
        throws LineUnavailableException, IOException, UnsupportedAudioFileException, IllegalArgumentException,
        SecurityException
    {

        printDebug("00");
        timeTracker.reset();

        if (soundClip == null) {
            open();
        }
        printDebug("1");
        setState(ClipState.PLAYING);
        printDebug("1.5");
        if(soundClip.isRunning()) {
            soundClip.stop(); // sometimes it gets stuck here on my ubuntu at home. Maybe it is already stopped? or about to be stopped? or something?
        }
        printDebug("2");
        soundClip.setMicrosecondPosition(0);
        printDebug("3");
        soundClip.start();
        timeTracker.start();
        printDebug("play: " + this);
        startCloseThread();
    }
    
    /**
	 * Play this sound from the beginning of the sound and loop around when the
	 * end have been reached. NOT IMPLEMENTED YET.
	 * 
	 * @throws LineUnavailableException
	 *             if a matching line is not available due to resource
	 *             restrictions
	 * @throws IOException
	 *             if an I/O exception occurs
	 * @throws SecurityException
	 *             if a matching line is not available due to security
	 *             restrictions
	 * @throws UnsupportedAudioFileException
	 *             if the URL does not point to valid audio file data
	 * @throws IllegalArgumentException
	 *             if the system does not support at least one line matching the
	 *             specified Line.Info object through any installed mixer
	 */
    public synchronized void loop()
        throws LineUnavailableException, IOException, UnsupportedAudioFileException, IllegalArgumentException,
        SecurityException
    {
    	
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
        soundClip.stop();
        soundClip.setMicrosecondPosition(0);
        printDebug("Stop: " + this);
    }

    /**
     * Pause the clip. Paused sounds can be resumed.
     * 
     */
    public synchronized void pause()
    {
        timeTracker.pause();
        if (soundClip == null || isPaused()) {
            return;
        }
        setState(ClipState.PAUSED);
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
        timeTracker.start();
        soundClip.start();
        setState(ClipState.PLAYING);
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
                case PAUSED :
                    playbackListener.playbackPaused(this);
                    break;
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
        return clipState == ClipState.PLAYING;
    }

    /**
     * True if the sound is currently paused.
     */
    public synchronized boolean isPaused()
    {
        return clipState == ClipState.PAUSED;
    }

    /**
     * True if the sound is currently playing.
     */
    public synchronized boolean isStopped()
    {
        return clipState == ClipState.STOPPED;
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
                    SoundClip thisClip = SoundClip.this;
                    while (thisClip.soundClip != null) {
                        synchronized (thisClip) {
                            long playTime = timeTracker.getTimeTracked();
                            long timeLeftOfPlayback = clipLength - playTime + EXTEA_SLEEP_DELAY;
                            long timeLeftToClose = timeLeftOfPlayback + CLOSE_TIMEOUT;
                            if (isPaused()) {
                                try {
                                    thisClip.wait();
                                }
                                catch (InterruptedException e) {
                                    // TODO Handle this!
                                    e.printStackTrace();
                                }
                            }
                            else if (timeLeftToClose > 0) {
                                printDebug("Waiting to close: " + timeLeftToClose);
                                try {
                                    thisClip.wait(timeLeftToClose);
                                }
                                catch (InterruptedException e) {
                                    // TODO Handle this!
                                    e.printStackTrace();
                                }
                                printDebug("Wait done");
                            }
                            else {
                                printDebug("Closing clip: " + thisClip.name);
                                thisClip.soundClip.close();
                                thisClip.soundClip = null;
                                thisClip.closeThread = null;
                                setState(ClipState.CLOSED);
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