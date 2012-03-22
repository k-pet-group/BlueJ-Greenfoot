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

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.DataLine.Info;

import bluej.utility.Debug;

/**
 * Plays sound from a URL. To avoid loading the entire sound clip into memory,
 * the sound is streamed. The sound can either be a standard sound supported by
 * the core Java libraries or an MP3.
 * 
 * @see Mp3AudioInputStream
 * @see JavaAudioInputStream
 * 
 * @author Poul Henriksen
 * 
 */
public class SoundStream implements Sound, Runnable
{
    private static void printDebug(String s)
    {
        // Comment this line out if you don't want debug info.
        // System.out.println(s);
    }
    /**
     * How long to wait until closing the line and stopping the playback thread
     * after playback has finished. In ms.
     */
    private static final int CLOSE_TIMEOUT = 1000;
    /**
     * Signals that the sound should loop.
     */
    private boolean loop = false;
    /**
     * Signals that the playback should stop. 
     */
    private boolean stop = true;
    /**
     * Signals that the playback should pause.
     */
    private boolean pause = false;
    /** Signals that playback should start over from the beginning. */
    private boolean restart = false;
    /**
     * Flag that indicates whether the sound is currently stopped (not playing
     * or paused). Almost the same as the stop signal, except that this flag
     * will be set to false when the end of the input has been reached.
     */
    private boolean stopped = true;
    /**
     * Stream where data is read from. This stream should only be accessed from
     * the playThread.
     */
    private final GreenfootAudioInputStream inputStream;
    /** Listener for state changes. */
    private final SoundPlaybackListener playbackListener;
    /** The line that we play the sound through */
    private volatile AudioLine line;
    private AudioFormat format;
    private Info info;
    /** Thread that handles the actual playback of the sound. */
    private Thread playThread;

    public SoundStream(GreenfootAudioInputStream inputStream, SoundPlaybackListener playbackListener)
    {
        this.playbackListener = playbackListener;
        this.inputStream = inputStream;
        try {
            // Preparing the line here, speeds up the first playback of the
            // sound.
            format = inputStream.getFormat();
            info = new DataLine.Info(SourceDataLine.class, format);
            line = initialiseLine(info, format);
        }
        catch (IllegalArgumentException e) {
            // Thrown by getLine()
            SoundExceptionHandler.handleIllegalArgumentException(e, inputStream.getSource());
        }
        catch (LineUnavailableException e) {
            SoundExceptionHandler.handleLineUnavailableException(e);
        }
    }

    @Override
    public synchronized void play()
    {
        if (isPlaying()) {
            // Make sure we no longer loop.
            loop = false;
        }
        else {
            // We are not playing, so we should start playing.
            startPlayback();
        }
    }

    @Override
    public synchronized void loop()
    {
        // Make sure we loop.
        loop = true;
        if (!isPlaying()) {
            // We are not playing, so we should start playing.
            startPlayback();
        }
    }

    /**
     * Starts playback by creating the thread if necessary, clearing the
     * stop, stopped, and pause flags and notifying listeners.
     */
    private void startPlayback()
    {
        if (!pause) {
            restart = true;
            if (playThread == null) {
                printDebug("Starting new playthread");
                playThread = new Thread(this, "SoundStream:" + inputStream.getSource());
                playThread.start();
            }
            if (line != null) {
                line.reset();
            }
        }
        stopped = false;
        pause = false;
        stop = false;
        if (line != null) {
            line.start();
        }
        notifyAll();
        playbackListener.playbackStarted(this);
    }

    @Override
    public synchronized void close()
    {
        if (line != null) {
            reset();
            line.close();
            notifyAll();
            playbackListener.playbackStopped(this);
        }
    }

    @Override
    public synchronized void stop()
    {
        if (!stop) {
            stop = true;
            stopped = true;
            pause = false;
            line.reset();
            notifyAll();
            playbackListener.playbackStopped(this);
        }
    }

    @Override
    public synchronized void pause()
    {
        if (!stopped && !pause) {
            line.stop();
            pause = true;
            notifyAll();
            playbackListener.playbackPaused(this);
        }
    }

    @Override
    public synchronized boolean isPlaying()
    {
        return !stopped && !pause;
    }

    @Override
    public synchronized boolean isStopped()
    {
        return stopped && !pause;
    }

    @Override
    public synchronized boolean isPaused()
    {
        return pause;
    }

    @Override
    public String toString()
    {
        return inputStream.getSource() + " " + super.toString();
    }

    @Override
    public void run()
    {
        // Whether the thread should stay alive or die.
        boolean stayAlive = true;

        try {
            while (stayAlive) {
                inputStream.restart();
                synchronized (this) {
                    if (line == null || !format.matches(inputStream.getFormat())) {
                        // If we don't have a line or the format has changed we
                        // need a new line.
                        format = inputStream.getFormat();
                        info = new DataLine.Info(SourceDataLine.class, format);
                        line = initialiseLine(info, format);
                    }
                    line.open();
                    restart = false;
                }

                int frameSize = format.getFrameSize();
                int bufferSize = SoundUtils.getBufferSizeToHold(format, 0.5);
                if (bufferSize == -1) {
                    bufferSize = 64 * 1024;
                }

                byte[] buffer = new byte[bufferSize];

                printDebug("Stream available (in bytes): " + inputStream.available() + " in frames: "
                        + inputStream.available() / frameSize);

                int bytesRead = inputStream.read(buffer, 0, bufferSize);
                int bytesInBuffer = bytesRead;
                printDebug(" read: " + bytesRead);
                while (bytesInBuffer > 0) {
                    // Only write in multiples of frameSize
                    int bytesToWrite = (bytesInBuffer / frameSize) * frameSize;

                    synchronized (this) {
                        // Handle stop
                        if (stop) {
                            break;
                        }

                        // Handle pause
                        if (pause) {
                            doPause();
                        }

                        // Handle restart
                        if (restart) {
                            printDebug("restart in thread");
                            line.reset();
                            inputStream.restart();
                            restart = false;
                            bytesInBuffer = 0;
                            bytesRead = 0;
                            bytesToWrite = 0;
                            printDebug("inputStream available after restart in thread: " + inputStream.available());
                        }
                    }
                    // Play it
                    int written = line.write(buffer, 0, bytesToWrite);

                    printDebug(" wrote: " + written);

                    // Copy remaining bytes (if we wrote less than what is in
                    // the buffer)
                    int remaining = bytesInBuffer - written;
                    if (remaining > 0) {
                        printDebug("remaining: " + remaining + "  written: " + written + "   bytesInBuffer: "
                                + bytesInBuffer + "   bytesToWrite: " + bytesToWrite);
                        System.arraycopy(buffer, written, buffer, 0, remaining);
                    }
                    bytesInBuffer = remaining;

                    printDebug("remaining: " + remaining + "  written: " + written + "   bytesInBuffer: "
                            + bytesInBuffer + "   bytesToWrite: " + bytesToWrite);
                    bytesRead = inputStream.read(buffer, bytesInBuffer, buffer.length - bytesInBuffer);
                    if (bytesRead != -1) {
                        bytesInBuffer += bytesRead;
                    }
                    printDebug(" read: " + bytesRead);
                }

                line.drain();

                synchronized (this) {

                    // NOTE: If the size of the stream is a multiple of 64k (=
                    // 16k frames) then it plays the last 64k twice if I don't
                    // stop it here.
                    // It still has a strange clicking sound at the end, which
                    // is probably because it starts playing a bit of the extra,
                    // but is stopped before it finishes.
                    // To make this more explicit, add a delay before
                    // line.reset.
                    // For example 4d.wav from piano scenario. Happens on my
                    // macbook and Ubuntu in the office. Poul.

                    if (!loop || stop) {
                        line.reset();
                    }

                    if ((!restart && !loop) || stop) {
                        stopped = true;
                        playbackListener.playbackStopped(this);
                        // Have a short pause before we get rid of the
                        // thread, in case the sound is played again soon
                        // after.
                        try {
                            if (line.isOpen()) {
                                printDebug("WAIT");
                                wait(CLOSE_TIMEOUT);
                            }
                        }
                        catch (InterruptedException e) {
                        }
                        // Kill thread if we have not received a signal to
                        // continue playback.
                        if ((!restart && !loop) || stop) {
                            line.close();
                            stayAlive = false;
                            reset();
                            printDebug("KILL THREAD");
                        }
                    }

                    printDebug(" 2 restart =  " + restart + "  stop = " + stop);

                    // If a restart was signalled, remove the signal and
                    // just continue.
                    if (restart) {
                        restart = false;
                    }
                }

            }
        }
        catch (IllegalArgumentException e) {
            // Thrown by getLine()
            SoundExceptionHandler.handleIllegalArgumentException(e, inputStream.getSource());
        }
        catch (UnsupportedAudioFileException e) {
            SoundExceptionHandler.handleUnsupportedAudioFileException(e, inputStream.getSource());
        }
        catch (LineUnavailableException e) {
            SoundExceptionHandler.handleLineUnavailableException(e);
        }
        catch (IOException e) {
            SoundExceptionHandler.handleIOException(e, inputStream.getSource());
        }
        finally {
            if (stayAlive == true) {
                // Abnormal termination, lets reset:
                reset();
            }
            if (line != null) {
                line.close();
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            playbackListener.soundClosed(this);
        }
    }

    /**
     * Pauses as long as the pause signal is true.
     */
    private synchronized void doPause()
    {
        if (pause) {
            while (pause) {
                try {
                    printDebug("In pause loop");
                    line.stop();
                    printDebug("In pause loop 2");
                    wait();
                }
                catch (InterruptedException e) {
                    Debug.reportError(
                            "Interrupted while pausing sound: " + inputStream.getSource(), e);
                }
            }
            line.start();
        }
    }

    /**
     * Initialise the line by creating it and setting up listeners.
     * 
     * @param info
     * @throws LineUnavailableException
     *             if a matching line is not available due to resource
     *             restrictions
     * @throws SecurityException
     *             if a matching line is not available due to security
     *             restrictions
     * @throws IllegalArgumentException
     *             if the system does not support at least one line matching the
     *             specified
     */
    private AudioLine initialiseLine(DataLine.Info info, AudioFormat format)
            throws LineUnavailableException, IllegalArgumentException
    {
        //Throws IllegalArgumentException if it can't find a line
        SourceDataLine l = (SourceDataLine) AudioSystem.getLine(info);
        printDebug("buffer size: " + l.getBufferSize());
        return new AudioLine(l, format);
    }

    /**
     * Stops the thread and reset all flags and signals to initial values.
     */
    private synchronized void reset()
    {
        stopped = true;
        pause = false;
        loop = false;
        stop = true;
        playThread = null;
    }

    public long getLongFramePosition()
    {
        return line.getLongFramePosition();
    }

    @Override
    public void setVolume(int level)
    {
        line.setVolume(SoundUtils.logToLin(level));
    }

    @Override
    public int getVolume()
    {
        return line.getVolume();
    }
}
