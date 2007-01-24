package greenfoot.sound;

import greenfoot.util.GreenfootUtil;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Plays sound from a file or URL. To avoid loading the entire sound clip into memory,
 * the sound is streamed.
 * 
 * @author Poul Henriksen
 * 
 */
public class SoundStream implements Sound
{
    private URL url;
    private boolean stop;
    private boolean pause;
    private SoundPlayer player;
    
    public SoundStream(String file, SoundPlayer player) {
        url = GreenfootUtil.getURL(file, "sounds");
        stop = false;
        this.player = player;
    }
    
    public synchronized void stop() {
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
        throws IOException, UnsupportedAudioFileException, LineUnavailableException
    {

        SourceDataLine line = null;
        AudioInputStream is = null;
        try {

            is = AudioSystem.getAudioInputStream(url);

            AudioFormat format = is.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            // If the format is not supported we try to convert it. 
            if (!AudioSystem.isLineSupported(info)) {
                //TODO TEST THIS!!!!
                System.out.println("Converting");
                // Target format
                AudioFormat supportedFormat = new AudioFormat(format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(), true, false);

                // Create the converter
                is = AudioSystem.getAudioInputStream(supportedFormat, is);
               
                format = is.getFormat();
                info = new DataLine.Info(SourceDataLine.class, format);
            }


            line = (SourceDataLine) AudioSystem.getLine(info);
            try {
                line.open(format);
            }
            catch(LineUnavailableException e) {
                System.err.println("Is another application using the sound card? Could not play sound: " + url);
                e.printStackTrace();
            }
            int frameSize = format.getFrameSize();
            byte[] buffer = new byte[ 2 * frameSize]; //4 * 1024 * frameSize
            int bytesInBuffer = 0;

            int bytesRead = is.read(buffer, 0, buffer.length - bytesInBuffer);
            boolean isFirst = true;
            while (bytesRead != -1 && !stop) {
                line.start();
                bytesInBuffer += bytesRead;

                // Only write in multiples of frameSize
                int bytesToWrite = (bytesInBuffer / frameSize) * frameSize;

                //Play it
                line.write(buffer, 0, bytesToWrite);
                // Copy remaining bytes (if we did not have a multiple of frameSize)
                int remaining = bytesInBuffer - bytesToWrite;
                if (remaining > 0)
                    System.arraycopy(buffer, bytesToWrite, buffer, 0, remaining);
                bytesInBuffer = remaining;
                

            /*    if(isFirst) {
                    isFirst = false;

                   // Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                    Thread.yield();
                }*/
                
                bytesRead = is.read(buffer, bytesInBuffer, buffer.length - bytesInBuffer);
                while (pause) {
                    synchronized (this) {
                        try {
                            wait();
                        }
                        catch (InterruptedException e) {}
                    }
                }
            }
            
            line.drain();
        }
        finally {
            if (line != null)
                line.close();
            if (is != null)
                is.close();
            player.soundStreamFinished(this);            
        }
    }
    
    public String toString() {
        return url + " " + super.toString();
    }

}
