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
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.DataLine.Info;


import bluej.utility.Debug;

/**
 * Plays sound from a URL. To avoid loading the entire sound clip into memory,
 * the sound is streamed.
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
 * <li>Seems to work without any problems. It gets the stop event correctly, it
 * goes from active to inactive when reaching the end.</li>
 * <li>Haven't tested whether line.drain() works though.</li>
 * 
 * </ul>
 * <p>
 * Mac (OS 10.5.6, JDK 1.5.0_16
 * <ul>
 * <li>Closing and opening a line repeatedly crashes the JVM with this error.
 * Can be reproduced in the piano scenario if you quickly press the same button
 * about 10-20 times in row. (JDK 1.5 prints the error below, 1.6 jsut crashes
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
 * <p>
 * So, on Linux we can only use the frame position as indicator of when the
 * playback has finished, which will only work correctly if we use line.close()
 * and line.open() to reset the frame position to 0 every time playback is
 * started or restarted. To avoid using close()/open() I tried marking the frame
 * position at which playback was restarted to offset the frame position, but
 * this is not reliable on mac at least, so I don't trust it for other systems
 * either.
 * <p>
 * On Mac we cannot use line.close()/line.open() at all because it crashes the
 * JVM badly. This also means that we cannot use line.drain() because the only
 * way of interrupting that is to call close() on the line. We do get the
 * correct START events if we don't close the line prematurely though, so this,
 * in conjunction with the frame position can be used to determine end of media.
 * <p>
 * On windows, we could use drain() and close() to make it work. We have to make
 * sure that the line is not stopped before invoking drain though, which could
 * be difficult. Probably need a flag to indicate whether a stop request has
 * been send. Better to make it more similar to Linux though to avoid too many
 * different implementations.
 * 
 * <p>
 * 
 * For windows and Linux I can use the same implementation, by using close/open
 * and frame position. For Windows I have to be aware that it might reset the
 * frame position though, and watch for a decrease in frame position to detect
 * end of the stream in case it misses the end frame.
 * 
 * On mac I have to avoid using open/close.
 * 
 * TODO:
 * 
 *  Conversions of incompatible formats.
 *  
 *  
 *  
 * @author Poul Henriksen
 * 
 */
public class SoundStream extends Sound implements Runnable
{
    private static void printDebug(String s) 
    {
    	// Comment this line out if you don't want debug info.  
    	 System.out.println(s);        
    }
    
    /**
     * How long to wait until closing the line and stopping the playback thread
     * after playback has finished. In ms.
     */
    private static final int CLOSE_TIMEOUT = 1000;
    
    /**
     * URL of the stream of sound data.
     */
    private URL url;

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

   
    /** Listener for state changes. */
    private SoundPlaybackListener playbackListener;
    
    /** The line that we play the sound through */
    private AudioLine line;
    
    /** Thread that handles the actual playback of the sound. */
    private Thread playThread ;
    
	public SoundStream(URL url, SoundPlaybackListener playbackListener)
    {
        this.url = url;
        this.playbackListener = playbackListener;        
    }

    public synchronized void play() 
    {
		loop = false;
		startPlayback();
	}    
    
    /**
	 * Resumes playback from where it last played. If the sound is not currently
	 * paused this call does nothing.
	 */
    public synchronized void resume() 
    {   
		if (pause) {
			startPlayback();
		}
    }
    
    public synchronized void loop()
    {
		loop = true;		
		startPlayback();		
    }

    /**
	 * Starts playback by creating the thread if necessary, clearing the
	 * stop, stopped, and pause flags and notifying listeners.
	 */
	private void startPlayback() {
		if (!pause) {
			restart = true;
			if (playThread == null) {
				printDebug("Starting new playthread");
				playThread = new Thread(this, "SoundStream:" + url.toString());
				playThread.start();
			}
			if(line != null) {
				line.reset();
			}
		}
		stopped = false;
		pause = false;
		stop = false;
		if(line != null) {
			line.start();
		}
		notifyAll();
		playbackListener.playbackStarted(this);
	}

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
    
    public synchronized void pause()
    {
        if (!stopped && !pause) {
        	line.stop();
        	pause = true;
            notifyAll();
            playbackListener.playbackPaused(this);
        }
    }
    
    public synchronized boolean isPlaying() 
    {
        return !stopped && !pause;
    }    

    public synchronized boolean isStopped() 
    {
        return stopped && !pause;
    }
    

    public synchronized boolean isPaused() 
    {
        return pause;
    }
    
    public String toString()
    {
        return url + " " + super.toString();
    }    

    public void run()
    {
        // Whether the thread should stay alive or die.
        boolean stayAlive = true;

        AudioInputStream inputStream = null;
        AudioFormat format = null;
        Info info;
        try {
            while (stayAlive) {
                if (inputStream != null) {
                    inputStream.close();
                }
                inputStream = AudioSystem.getAudioInputStream(url);
                format = inputStream.getFormat();
                info = new DataLine.Info(SourceDataLine.class, format);
                
                int frameSize = format.getFrameSize();
                int bufferSize = SoundUtils.getBufferSizeToHold(format, 0.5);
                if (bufferSize == -1) {
                    bufferSize = 64 * 1024;
                }

                byte[] buffer = new byte[bufferSize];

                synchronized (this) {
                    if (line == null) {
                        line = initialiseLine(info, format);
                    }
                    line.open();
                    restart = false;
                }

                printDebug("Stream available (in bytes): " + inputStream.available() + " in frames: "
                        + inputStream.available() / frameSize);

                int bytesRead = inputStream.read(buffer, 0, format.getFrameSize() * 100);
                int bytesInBuffer = bytesRead;
                printDebug(" read: " + bytesRead);
                while (bytesInBuffer > 0) {
                    // Only write in multiples of frameSize
                    int bytesToWrite = (bytesInBuffer / frameSize) * frameSize;

                    synchronized (this) {
                        // Handle stop
                        if (stop)
                            break;

                        // Handle pause
                        if (pause) {
                            doPause();
                        }

                        // Handle restart
                        if (restart) {
                            printDebug("restart in thread");
                            line.reset();
                            try {
                                inputStream.close();
                            }
                            catch (IOException e) {
                                Debug.reportError("Exception while closing sound input stream.", e);
                            }
                            inputStream = AudioSystem.getAudioInputStream(url);
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
                        // Have a short pause before we get rid of the
                        // thread, in case the sound is played again soon
                        // after.
                        try {
                            printDebug("WAIT");
                            wait(CLOSE_TIMEOUT);
                        }
                        catch (InterruptedException e) {}
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
            SoundExceptionHandler.handleIllegalArgumentException(e, url.toString());
        }
        catch (UnsupportedAudioFileException e) {
            SoundExceptionHandler.handleUnsupportedAudioFileException(e, url.toString());
        }
        catch (LineUnavailableException e) {
            SoundExceptionHandler.handleLineUnavailableException(e);
        }
        catch (IOException e) {
            SoundExceptionHandler.handleIOException(e, url.toString());
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
            playbackListener.playbackStopped(this);
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
				} catch (InterruptedException e) {
					Debug.reportError(
							"Interrupted while pausing sound: " + url, e);
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
}
