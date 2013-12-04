/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2013  Poul Henriksen and Michael Kolling 
 
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
import javax.sound.sampled.UnsupportedAudioFileException;

import bluej.utility.Debug;

/**
 * Wrapper classer for a AudioInputStream. It just delegates all methods
 * to the wrapped class except for restart().
 * 
 * @author Poul Henriksen
 */
public class JavaAudioInputStream implements GreenfootAudioInputStream
{
    private AudioInputStream stream;
    private URL url;
    private boolean readingHasStarted = false;
    private boolean open;

    public JavaAudioInputStream(URL url) throws UnsupportedAudioFileException,
    IOException
    {
        this.url = url;
        open();
    }

    public void open() throws UnsupportedAudioFileException, IOException
    {
        if (!open) {
            readingHasStarted = false;

            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // This exception is probably not fatal, so just log it and continue.
                    Debug.reportError("Exception while closing java audio input stream.", e);
                }
            }
            stream = AudioSystem.getAudioInputStream(url);
            open = true;
        }
    }

    public void restart() throws UnsupportedAudioFileException, IOException
    {
        if(!open || readingHasStarted() || stream == null) {
            open = false;
            open();
        }
    }

    /**
     * Whether reading from this stream has begun.
     * 
     * @return True if it has been restarted and no reading has been done since.
     *         False otherwise.
     */
    private boolean readingHasStarted()
    {
        return readingHasStarted;
    }

    public String getSource()
    {
        return url.toString();
    }

    public int available() throws IOException
    {
        return stream.available();
    }

    public void close() throws IOException
    {
        open = false;
        stream.close();
    }

    public boolean equals(Object obj)
    {
        if (obj == null) {
            return false;
        }
        if (! (obj instanceof JavaAudioInputStream) ) {
            return false;
        }
        return stream.equals(((JavaAudioInputStream)obj).stream);
    }

    public AudioFormat getFormat()
    {
        return stream.getFormat();
    }

    public long getFrameLength()
    {
        return stream.getFrameLength();
    }

    public int hashCode()
    {
        return stream.hashCode();
    }

    public void mark(int readlimit)
    {
        stream.mark(readlimit);
    }

    public boolean markSupported()
    {
        return stream.markSupported();
    }

    public int read() throws IOException
    {
        readingHasStarted = true;
        return stream.read();
    }

    public int read(byte[] b, int off, int len) throws IOException
    {
        readingHasStarted = true;
        return stream.read(b, off, len);
    }

    public int read(byte[] b) throws IOException
    {
        readingHasStarted = true;
        return stream.read(b);
    }

    public void reset() throws IOException
    {
        stream.reset();
    }

    public long skip(long n) throws IOException
    {
        readingHasStarted = true;
        return stream.skip(n);
    }

    public String toString()
    {
        return stream.toString();
    }
}