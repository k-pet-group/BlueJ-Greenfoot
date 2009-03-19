/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kšlling 
 
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
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
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
public class SoundStream extends Sound implements Runnable
{
    private URL url;
    private boolean stop;
    private boolean pause;
    private SoundCollection player;

    private volatile boolean playing = false;
    
    private Thread playThread ;

    public SoundStream(URL url, SoundCollection player)
        throws UnsupportedAudioFileException, IOException, LineUnavailableException
    {
        this.url = url;
        stop = false;
        this.player = player;
        playThread = new Thread(this, url.toString());
        //open();
        //TODO: we probably shouldn't open it already since it will allocate resources.
    }

   /* private void open()
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
    }*/

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
    
    public boolean isPlaying() 
    {
        return playing;
    }

    public void play()
    {
        // TODO should be able to play again later. Right now it can't
       synchronized (this) {
           playing = true;
           stop=false;

           //playThread = new Thread(this, url.toString());
           
           if(!playThread.isAlive())
               playThread.start();
           notifyAll();
        
    }
       //Block
       while(isPlaying()) {
           try {
            Thread.sleep(200);
        }
        catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
       }
        
    }

    public String toString()
    {
        return url + " " + super.toString();
    }

    public void run()
    {

        while (true) {
            AudioInputStream stream = null;
            SourceDataLine line = null;
            try {
                
                stream = AudioSystem.getAudioInputStream(url);

                AudioFormat format = stream.getFormat();
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
                line.addLineListener(new LineListener(){

                    public void update(LineEvent event)
                    {
                       System.out.println("Got event: " + event);
                    }});
                int frameSize = format.getFrameSize();
                byte[] buffer = new byte[4 * 1024 * frameSize]; // 4 * 1024 *
                // frameSize
                synchronized (this) {
                    while (stop) {
                        System.out.println("Stopped");
                        this.wait();
                        System.out.println("Done waiting");
                        
                    }
                }
                playing = true;
                System.out.println("Playing");
                int bytesInBuffer = 0;
                int bytesRead = stream.read(buffer, 0, buffer.length - bytesInBuffer);
                while (bytesRead != -1) {
                    synchronized (this) {
                        if (stop)
                            break;
                    }
                    line.start();
                    bytesInBuffer += bytesRead;

                    // Only write in multiples of frameSize
                    int bytesToWrite = (bytesInBuffer / frameSize) * frameSize;
                    
          //          System.out.println("bytesToWrite: " + bytesToWrite);
                    
                    // Play it
                    int written = line.write(buffer, 0, bytesToWrite);
                    if(written != bytesToWrite) {
         //               System.out.println("*********************   written: " + written);
                    }
                    // Copy remaining bytes (if we did not have a multiple of
                    // frameSize)
                    int remaining = bytesInBuffer - bytesToWrite;
        //            System.out.println("remaining: " + remaining);
                    if (remaining > 0)
                        System.arraycopy(buffer, bytesToWrite, buffer, 0, remaining);
                    bytesInBuffer = remaining;

                    bytesRead = stream.read(buffer, bytesInBuffer, buffer.length - bytesInBuffer);

                    synchronized (this) {
                        while (pause) {
                            
                            try {
                                System.out.println("pause");
                                wait();
                            }
                            catch (InterruptedException e) {}
                        }
                    }
                }
                System.out.println("Finished loop");
                synchronized (this) {
                    // We played to the end. Stop the sound
                    stop = true;
                }
                System.out.println("sync done:" + line + "  framePos: " + line.getFramePosition() + "  avail:" + line.available() + "  active:" + line.isActive() + "  open:" + line.isOpen() + "  runnig:" + line.isRunning() );
              //  com.sun.media.sound.MixerSourceLine sunLine = (com.sun.media.sound.MixerSourceLine)   line;
                line.drain(); // It sometimes hangs here
                System.out.println("line drained");

            }

            catch (IOException e1) {
                // this should not happen, since the error should have happened
                // in the open() method                
                Debug.reportError("Error when streaming sound.", e1);
                //TODO Remove below
                System.err.println("remove here");
                e1.printStackTrace();
            }
            catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (UnsupportedAudioFileException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (LineUnavailableException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            finally {
                if (line != null) {
                    line.close();
                }
                if (stream != null) {
                    try {
                        stream.close();
                    }
                    catch (IOException e) {
                    }
                }
                playing = false;
                player.playbackStopped(this);
            }
        }
    }
}
