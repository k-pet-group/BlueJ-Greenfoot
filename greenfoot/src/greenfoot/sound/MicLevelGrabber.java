package greenfoot.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

/**
 * Gets the overall level of the default microphone plugged into the system.
 * @author Michael
 */
public class MicLevelGrabber {

    private static final MicLevelGrabber INSTANCE = new MicLevelGrabber();
    private final AudioFormat format;
    private int level;
    private final Runnable updator;
    private volatile boolean running;

    /**
     * Create a new mic level grabber and initialise the updator.
     */
    private MicLevelGrabber() {
        format = new AudioFormat(22050, 8, 1, true, true);
        updator = new Runnable() {

            public void run() {
                try {
                    TargetDataLine line = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, format));
                    line.open();
                    line.start();
//                  int bufferSize = (int) (format.getSampleRate() / 2) * format.getFrameSize();
                    int bufferSize = 100;
                    byte buffer[] = new byte[bufferSize];
                    int bytesRead = line.read(buffer, 0, bufferSize);
                    line.stop();
                    level = (int) ((getRMS(buffer, bytesRead) / 127) * 100);
                }
                catch (LineUnavailableException ex) {
                    throw new RuntimeException("Couldn't get mic level", ex);
                }
                finally {
                    running = false;
                }
            }
        };
    }

    /**
     * Get the singleton instance of this class.
     * @return the instance.
     */
    public static MicLevelGrabber getInstance() {
        return INSTANCE;
    }

    /**
     * Update the microphone level (spawns off a new thread) and return the
     * level. It will most likely be the previous result that's returned,
     * however for things like meters where the level is being constantly
     * monitored this shouldn't be a problem.
     */
    public int getLevel() {
        updateLevel();
        return level;
    }

    /**
     * Spawn off the thread to update the mic level value, if it isn't already
     * running.
     */
    private synchronized void updateLevel() {
        if (!running) {
            running = true;
            new Thread(updator).start();
        }
    }

    /**
     * Get the root mean square average of an array of bytes.
     * @param arr the array of bytes.
     * @param lim the index to read up to in the array.
     * @return the root mean square of the values.
     */
    private static double getRMS(byte[] arr, int lim) {
        double average = 0;
        for (int i = 0; i < arr.length && i < lim; i++) {
            average += arr[i] * arr[i];
        }
        average /= arr.length;
        return Math.sqrt(average);
    }
}
