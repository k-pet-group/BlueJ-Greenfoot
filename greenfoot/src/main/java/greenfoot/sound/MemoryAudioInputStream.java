/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2010  Poul Henriksen and Michael Kolling 
 
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
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * An implementation of GreenfootAudioInputStream that reads from a memory buffer
 * 
 * @author neil
 */
public class MemoryAudioInputStream implements GreenfootAudioInputStream
{
    private byte[] sound;
    private int startOffset;
    private int endOffset;
    private AudioFormat format;
    private int markIndex;
    private int curIndex;
    
    public MemoryAudioInputStream(byte[] sound, AudioFormat format)
    {
        curIndex = 0;
        markIndex = 0;
        startOffset = 0;
        endOffset = sound.length;
        this.sound = sound;
        this.format = format;
    }
    
    public MemoryAudioInputStream(byte[] sound, int offset, int length, AudioFormat format)
    {
        curIndex = offset;
        markIndex = curIndex;
        startOffset = offset;
        endOffset = offset + length;
        this.sound = sound;
        this.format = format;
    }
    
    private int getFrameSize()
    {
        return format.getFrameSize();
    }
    
    public int available() throws IOException
    {
        return endOffset - curIndex;
    }

    public void close() throws IOException
    {
    }

    public AudioFormat getFormat()
    {
        return format;
    }

    public String getSource()
    {
        return "Internal buffer";
    }

    public void mark(int readlimit)
    {
        markIndex = curIndex;

    }

    public boolean markSupported()
    {
        return true;
    }

    public void open() throws IOException, UnsupportedAudioFileException
    {
        curIndex = startOffset;
    }

    public int read() throws IOException
    {
        if (getFrameSize() != 1)
            throw new IOException("Attempted to read single byte but frame size is not 1");
        
        if (curIndex < endOffset)
            return sound[curIndex++];
        else
            return -1;
    }

    public int read(byte[] b) throws IOException
    {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException
    {
        if (curIndex >= endOffset)
            return -1;
        
        int maxRead = len - (len % getFrameSize());
        
        if (curIndex + maxRead > endOffset) {
            int left = endOffset - curIndex;
            maxRead = left - (left % getFrameSize());
        }
        
        System.arraycopy(sound, curIndex, b, off, maxRead);
        curIndex += maxRead;
        
        return maxRead;
    }

    public void reset() throws IOException
    {
        curIndex = markIndex;
    }

    public void restart() throws IOException, UnsupportedAudioFileException
    {
        curIndex = startOffset;
    }

    public long skip(long n) throws IOException
    {
        if (curIndex + n <= endOffset) {
            curIndex += n;
            return n;
        } else {
            int diff = endOffset - curIndex;
            curIndex = endOffset;
            return diff;
        }
    }
}
