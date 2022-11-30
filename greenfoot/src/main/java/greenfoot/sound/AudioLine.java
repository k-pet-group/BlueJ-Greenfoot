/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011  Poul Henriksen and Michael Kolling 
 
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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Wraps a SourceDataLine to work around all the different bugs that happens on
 * different systems.
 * <p>
 * To work around all the different problems listed below, this class minimises
 * the use of open and close and never uses drain. Instead we use the fact that
 * we know how long it takes to play a certain number of bytes and use this to
 * implement our own drain method. By using this timing we also don't have to
 * open and close on the line in order to reset the framecount of the line when
 * restarting, since it is not used anyway.
 * 
 * <p>
 * <p>
 * There are several inconsistencies between different platforms that means that
 * this class is more complicated than it really should be if everything worked
 * as it should. Below is listed the different problems observed on various
 * platforms:
 * <p>
 * Windows XP on Poul's home PC (SP3, Sun JDK 1.6.11, SB Live Sound Card) and
 * Windows Vista on Poul's office PC (dell build in soundcard) and Windows XP on
 * Poul's office PC (SP2, dell onboard soundcard)
 * <ul>
 * <li>Line does not receive a stop signal when end of media has been reached.</li>
 * <li>Line is reported as active even when end of media has been reached. If
 * invoking stop, then start again, it seems to remain inactive though (this
 * does not generate a START event, only a stop)</li>
 * <li>The frame position reported by line.getLongFramePosition() is incorrect.
 * After reaching the last frame, it will, after a while, start over at frame
 * position 0 and count up to the last frame again. It will repeat this forever.
 * </li>
 * </ul>
 * <p>
 * Linux on Poul's home PC (Ubuntu 8.10, Sun JDK 1.6.10, SB Live Sound Card):
 * <ul>
 * <li>Line does not receive a stop signal when end of media has been reached.</li>
 * <li>Line is reported as active even when end of media has been reached.</li>
 * <li>Hangs if line.drain() is used (need to confirm this, saw it a long time
 * ago, and it might have been because of timing issues resulting in drain()
 * being invoked on a stopped line)</li>
 * <li>The frame position reported by line.getLongFramePosition() is correct and
 * seems to be the only way of detecting when the end of the media has been
 * reached.</li>
 * </ul>
 * <p>
 * <p>
 * Linux on Poul's office PC (Ubuntu 8.10, Sun JDK 1.6.10 / 1.5.16, SB Live
 * Sound Card):
 * <ul>
 * <li>Repeatedly calling close and open makes it hang in close.</li>
 * <li>Haven't tested whether line.drain() works.</li>
 * 
 * </ul>
 * <p>
 * Mac (OS 10.5.6, JDK 1.5.0_16
 * <ul>
 * <li>Closing and opening a line repeatedly crashes the JVM with this error.
 * Can be reproduced in the piano scenario if you quickly press the same button
 * about 10-20 times in row. (JDK 1.5 prints the error below, 1.6 just crashes
 * silently): <br>
 * java(3382,0xb1b4e000) malloc: *** mmap(size=1073745920) failed (error
 * code=12)<br>
 * error: can't allocate region<br>
 * set a breakpoint in malloc_error_break to debug</li>
 * <li>It skips START events if the line is closed before we have received the
 * START event.</li>
 * </ul>
 * 
 * 
 * 
 * @author Poul Henriksen
 * 
 */
public class AudioLine
{

    private static void printDebug(String s)
    {
        // Comment this line out if you don't want debug info.
        // System.out.println(s);
    }
    /**
     * Extra delay in ms added to the sleep time before stopping the sound. This
     * is just an extra buffer of time to make sure we don't close it too soon.
     * This helps avoid stopping the sound too soon which seems to happen on
     * some Linux systems.
     */
    private final static int EXTRA_SLEEP_DELAY = 50;
    /**
     * The actual line that we wrap. I assume this object is thread-safe,
     * because it has methods that only makes sense in a multi-threaded
     * environment (drain()).
     */
    private volatile SourceDataLine line;
    private AudioFormat format;
    /** Total bytes written since playback started. */
    private long totalWritten;
    /** Whether the line is open. */
    private boolean open;
    /** Whether the line has been started. */
    private boolean started;
    private int masterVolume;
    /**
     * Whether data is currently being written to the line (or blocked on
     * write).
     */
    private boolean writing;
    /**
     * Whether the line is reset. As soon as data has been written, the line is
     * no longer reset.
     */
    private boolean reset;
    /** Keeps track of how much time have been spend on active playback. */
    private TimeTracker timeTracker;

    public AudioLine(SourceDataLine line, AudioFormat format)
    {
        this.line = line;
        this.format = format;
        timeTracker = new TimeTracker();
    }

    /**
     * Opens the line. If already open, this method does nothing.
     * 
     * @throws LineUnavailableException if the line cannot be opened due to
     *             resource restrictions
     * @throws IllegalArgumentException if <code>format</code> is not fully
     *             specified or invalid
     * @throws SecurityException if the line cannot be opened due to security
     *             restrictions
     */
    public synchronized void open()
            throws LineUnavailableException, IllegalArgumentException, IllegalStateException, SecurityException
    {
        if (!open) {
            line.open(format);
            open = true;
            reset = true;
        }
    }

    /**
     * Closes the line. If the line is not open, this method does nothing.
     */
    public synchronized void close()
    {
        if (open) {
            open = false;
            reset();
            line.close();
        }
    }

    /**
     * Starts the line. Can be used to resume playback that have been stopped.
     * Unlike SourceDataLine this method does NOT have to be called before
     * writing to the line.
     */
    public synchronized void start()
    {
        if (!started && open) {
            line.start();
            started = true;
            if (getTimeLeft() > 0) {
                // If we haven't finished playback of bytes already written, we
                // should start the tracker again.
                timeTracker.start();
            }
        }
    }

    /**
     * Stops the line. Playback will stop. It can later be resumed by calling
     * start.
     */
    public synchronized void stop()
    {
        if (open) {
            //stop = true;
            notifyAll();
            started = false;
            line.stop();
            timeTracker.pause();
        }
    }

    /**
     * Resets this line by stopping playback and clearing all the data in the
     * buffer. The line will remain open. If the line is not open, this method
     * does nothing.
     */
    public synchronized void reset()
    {
        printDebug("reset() start");

        if (open) {
            if (started) {
                line.stop();
            }
            if (!reset) {
                line.flush();
            }
            totalWritten = 0;
            // TODO: totalWritten might be updated after this in write method.
            started = false;
            timeTracker.reset();
            notifyAll();
        }
        reset = true;
        printDebug("reset() end");
    }

    /**
     * Will attempt to write the given bytes to the line. This method might
     * block if it can't write it all at once. If the line is not open then this
     * method will return 0 immediately.
     * 
     * @return The number of bytes written (different from len if the line was
     *         stopped while writing).
     */
    public int write(byte[] b, int off, int len)
    {
        synchronized (this) {
            if (!open) {
                return 0;
            }

            writing = true;
            started = true;
            reset = false;
            timeTracker.start();
        }
        line.start();
        int written = line.write(b, off, len);
        synchronized (this) {
            // drain() might be waiting, so we should wake it up.
            notifyAll();
            writing = false;
            if (!reset) {
                totalWritten += written;
            }
            else if (reset && open) {
                // Flush what we just wrote to keep it reset.
                line.flush();
            }
            return written;
        }
    }

    /**
     * Wait for the line to finish playback. If this line is closed or reset it
     * will return immediately. If the line is stopped, this method will not
     * return until the line is started again.
     * 
     * @return True if we successfully drained all the data, false if the line
     *         was closed before playback finished.
     */
    public synchronized boolean drain()
    {
        printDebug("Draining start");
        printDebug(" totalWritten: " + totalWritten);
        long timeLeft = getTimeLeft();
        while (timeLeft > 0 && open) {
            printDebug(" timeLeft: " + timeLeft);
            if (started && timeLeft > 0) {
                try {
                    wait(timeLeft);
                }
                catch (InterruptedException e) {
                }
            }
            else if (!started || writing) {
                try {
                    // Line is stopped, or we are currently writing to the line
                    // so we wait until waken up again.
                    wait();
                }
                catch (InterruptedException e) {
                }
            }
            timeLeft = getTimeLeft();
        }

        printDebug("Draining end: " + timeLeft);
        if (timeLeft > 0) {
            return false;
        }
        else {
            return true;
        }
    }

    public synchronized boolean isOpen()
    {
        return open;
    }

    private synchronized long getTimeLeft()
    {
        return SoundUtils.getTimeToPlayBytes(totalWritten, format) - timeTracker.getTimeTracked() + EXTRA_SLEEP_DELAY;
    }

    public long getLongFramePosition()
    {
        return line.getLongFramePosition();
    }

    public synchronized void setVolume(int masterVolume)
    {
        this.masterVolume = masterVolume;
        try {
            open();
            if (line != null) {
                if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl volume = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                    float val = SoundUtils.convertMinMax(masterVolume, volume.getMinimum(), volume.getMaximum());
                    volume.setValue(val);
                }
            }
        }
        catch (LineUnavailableException ex) {
            SoundExceptionHandler.handleLineUnavailableException(ex);
        }
    }

    public synchronized int getVolume()
    {
        return masterVolume;
    }
}
