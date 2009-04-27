package greenfoot.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Wraps a SourceDataLine to work around all the different bugs that happens on
 * different systems.
 * 
 * 
 * Basically, we can't use open and close repeatedly or drain.
 * 
 * @author Poul Henriksen
 * 
 */
public class AudioLine
{
    private static void printDebug(String s)
    {
        // Comment this line out if you don't want debug info.
        System.out.println(s);
    }

    private SourceDataLine line;
    private AudioFormat format;

    /** Total bytes written since playback started. */
    private long totalWritten;

    /** Whether the line is open. */
    private boolean open;

    /** Whether the line has been started. */
    private boolean started;

    /**
     * Whether data is currently being written to the line (or blocked on
     * write).
     */
    private boolean writing;

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
            open = true;
            line.open(format);
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
            line.stop();
            line.flush();
            totalWritten = 0;
            started = false;
            timeTracker.reset();
            notifyAll();
        }
        printDebug("reset() end");
    }

    /**
     * Will attempt to write the given bytes to the line. This method might
     * block if it can't write it all at once. The line has to be open before
     * this method is called.
     * 
     * @return The number of bytes written (different from len if the line was
     *         stopped while writing).
     */
    public int write(byte[] b, int off, int len)
    {
        synchronized (this) {
            if (!open) {
                throw new IllegalStateException("Line not open.");
            }

            writing = true;
            line.start();
            started = true;
            timeTracker.start();
        }
        int written = line.write(b, off, len);
        synchronized (this) {
            // drain() might be waiting, so we should wake it up.
            notifyAll();
            totalWritten += written;
            writing = false;
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
        while (timeLeft > 0 ) {
            printDebug(" timeLeft: " + timeLeft);
            if (started && timeLeft > 0) {
                try {
                    wait(timeLeft);
                }
                catch (InterruptedException e) {
                }
            }
            else if (!started || writing){
                try {
                    // Line is stopped, or we are currently writing to the line
                    // so we wait until waken up again.
                    wait();
                }
                catch (InterruptedException e) {
                }
            }
            timeLeft = getTimeLeft();
            if (!open) {
                break;
            }
        }

        printDebug("Draining end: " + timeLeft);
        if (timeLeft > 0) {
            return false;
        }
        else {
            return true;
        }
    }

    private synchronized long getTimeLeft()
    {
        return SoundUtils.getTimeToPlayBytes(totalWritten, format) - timeTracker.getTimeTracked();
    }

}
