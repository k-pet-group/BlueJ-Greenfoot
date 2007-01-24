package greenfoot.sound;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import bluej.utility.Debug;

/**
 * Plays sound from a URL. To avoid loading the entire sound clip into
 * memory, the sound is streamed.
 * 
 * @author Poul Henriksen
 * 
 */
public class SoundStream extends Sound
{
    private URL url;
    private boolean stop;
    private boolean pause;
    private SoundPlayer player;

    private SourceDataLine line;
    private AudioInputStream stream;
    private AudioFormat format;

    private boolean hasPlayed = false;

    public SoundStream(URL url, SoundPlayer player)
        throws UnsupportedAudioFileException, IOException, LineUnavailableException
    {
        this.url = url;
        stop = false;
        this.player = player;
        open();
    }

    private void open()
        throws UnsupportedAudioFileException, IOException, LineUnavailableException
    {
        stream = AudioSystem.getAudioInputStream(url);

        format = stream.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        // If the format is not supported we try to convert it.
        if (!AudioSystem.isLineSupported(info)) {
            // TODO TEST THIS!!!!
            System.out.println("Converting");
            format = getCompatibleFormat(format);
            // Create the converter
            stream = AudioSystem.getAudioInputStream(format, stream);
            info = new DataLine.Info(SourceDataLine.class, format);
        }

        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
    }

    public synchronized void stop()
    {
        stop = true;
        notifyAll();
    }

    public synchronized void pause()
    {
        pause = true;
    }

    public synchronized void resume()
    {
        pause = false;
        notifyAll();
    }

    public void play()
    {
        if(hasPlayed) {
            throw new IllegalStateException("This sound has already been played.");
        }
        hasPlayed = true;
        
        try {
            int frameSize = format.getFrameSize();
            byte[] buffer = new byte[4 * 1024 * frameSize]; // 4 * 1024 * frameSize
            int bytesInBuffer = 0;

            int bytesRead = stream.read(buffer, 0, buffer.length - bytesInBuffer);
            while (bytesRead != -1 && !stop) {
                line.start();
                bytesInBuffer += bytesRead;

                // Only write in multiples of frameSize
                int bytesToWrite = (bytesInBuffer / frameSize) * frameSize;

                // Play it
                line.write(buffer, 0, bytesToWrite);
                // Copy remaining bytes (if we did not have a multiple of
                // frameSize)
                int remaining = bytesInBuffer - bytesToWrite;
                if (remaining > 0)
                    System.arraycopy(buffer, bytesToWrite, buffer, 0, remaining);
                bytesInBuffer = remaining;

                bytesRead = stream.read(buffer, bytesInBuffer, buffer.length - bytesInBuffer);

                synchronized (this) {
                    while (pause) {
                        try {
                            wait();
                        }
                        catch (InterruptedException e) {}
                    }
                }
            }

            line.drain();
        }
        catch (IOException e1) {
            // this should not happen, since the error should have happened in the open() method
            Debug.reportError("Error when streaming sound.", e1);
        }
        finally {
            if (line != null) {
                line.close();
            }
            if (stream != null) {
                try {
                    stream.close();
                }
                catch (IOException e) {;
                }
            }
            player.soundFinished(this);
        }
    }

    public String toString()
    {
        return url + " " + super.toString();
    }
}
