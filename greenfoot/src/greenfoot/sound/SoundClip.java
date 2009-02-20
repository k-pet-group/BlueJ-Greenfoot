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
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Plays sound from a URL. The sound is loaded into memory before it is played.
 * 
 * @author Poul Henriksen
 * 
 */
public class SoundClip extends Sound
    implements LineListener
{
    private Clip soundClip;
    private SoundPlayer player;
    private String name;
    private URL url;
    private boolean isPlaying;
    
    /** The time at which sound playback was started. */
    private long startTime;
    
    /**
     * Extra delay in ms added to the sleep time before closing down the clip.
     * This helps avoid closing down the clip too soon.
     */
    private final static int EXTEA_SLEEP_DELAY = 300;

    /**
     * Creates a new sound clip
     * 
     * @throws LineUnavailableException if a matching line is not available due to resource restrictions
     * @throws IOException if an I/O exception occurs
     * @throws SecurityException if a matching line is not available due to security restrictions
     * @throws UnsupportedAudioFileException if the URL does not point to valid audio file data
     * @throws IllegalArgumentException if the system does not support at least one line matching the specified Line.Info object through any installed mixer
     */
    public SoundClip(String name, URL url, SoundPlayer player)
        throws LineUnavailableException, IOException, UnsupportedAudioFileException, SecurityException, IllegalArgumentException
    {
        this.name = name;
        this.player = player;
        this.url = url;
        isPlaying = false;
    }

    /**
     * Load the sound file supplied by the parameter into this sound engine.
     * 
     * @throws LineUnavailableException if a matching line is not available due to resource restrictions
     * @throws IOException if an I/O exception occurs
     * @throws SecurityException if a matching line is not available due to security restrictions
     * @throws UnsupportedAudioFileException if the URL does not point to valid audio file data
     * @throws IllegalArgumentException if the system does not support at least one line matching the specified Line.Info object through any installed mixer
     */    
    private void open()
        throws LineUnavailableException, IOException, UnsupportedAudioFileException, IllegalArgumentException, SecurityException
    {
        AudioInputStream stream = AudioSystem.getAudioInputStream(url);
        AudioFormat format = stream.getFormat();
        DataLine.Info info = new DataLine.Info(Clip.class, format);

        // Convert sound formats that are not supported
        if (!AudioSystem.isLineSupported(info)) {
            format = getCompatibleFormat(format);
            stream = AudioSystem.getAudioInputStream(format, stream);
            info = new DataLine.Info(Clip.class, stream.getFormat(), ((int) stream.getFrameLength() * format
                    .getFrameSize()));
        }
        soundClip = (Clip) AudioSystem.getLine(info);
        soundClip.open(stream);
        soundClip.addLineListener(this);
    }

    /**
     * Stop this sound.
     * 
     */
    public void stop()
    {
        soundClip.stop();
        soundClip.setMicrosecondPosition(0);
        isPlaying = false;
        soundClip.close();
    }

    /**
     * Pause the clip. Paused sounds can be resumed.
     * 
     */
    public void pause()
    {
        soundClip.stop();
    }

    /**
     * Resume a paused clip.
     * 
     */
    public void resume()
    {
        soundClip.start();
    }

    /**
     * Play this sound.
     * @throws LineUnavailableException if a matching line is not available due to resource restrictions
     * @throws IOException if an I/O exception occurs
     * @throws SecurityException if a matching line is not available due to security restrictions
     * @throws UnsupportedAudioFileException if the URL does not point to valid audio file data
     * @throws IllegalArgumentException if the system does not support at least one line matching the specified Line.Info object through any installed mixer
     
     */
    public void play() throws IllegalArgumentException, SecurityException, LineUnavailableException, IOException, UnsupportedAudioFileException
    {
        open();
        isPlaying = true;
        soundClip.setMicrosecondPosition(0);
        soundClip.start();
    }

    /**
     * Get a name for this sound. The name should uniquely identify 
     * the sound clip.
     */
    public String getName()
    {
        return name;
    }

    /**
     * True if the sound is currently playing.
     */
    public boolean isPlaying()
    {
        return isPlaying;
    }

    /**
     * Listener method to pick up end of play.
     */
    public void update(LineEvent event)
    {
        if (event.getType() == LineEvent.Type.STOP) {
            // This is a hack to make it work on Linux, where the STOP event is
            // send BEFORE the sound has actually finished playing (probably
            // send when some buffer is empty instead)
            long sleepTime = soundClip.getMicrosecondLength() / 1000 - (System.currentTimeMillis() - startTime);
            delayedClose(sleepTime);
        }
        else if (event.getType() == LineEvent.Type.START) {
            startTime = System.currentTimeMillis();
        }
        
    }

    /**
     * Close the clip after the given amount of time. This will be done
     * asynchronously.
     * 
     * @param sleepTime
     *            Minimum time to wait before closing the stream.
     */
    private void delayedClose(final long sleepTime)
    {
        new Thread() {
            public void run()
            {
                try {
                    // We add the extra sleep time, just to make sure
                    // that we don't close it before playback has
                    // finished. This fixes some issues when using applets where
                    // it seems like the sounds are stopped too early
                    // (Linux/Firefox).
                    Thread.sleep(sleepTime + EXTEA_SLEEP_DELAY);
                }
                catch (Throwable e) {}
                isPlaying = false;
                player.soundFinished(SoundClip.this);
                SoundClip.this.stop();
            }
        }.start();
    }
    
    public String toString()
    {
        return url + " " + super.toString();
    }
}
