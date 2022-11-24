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

import java.io.Closeable;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Interface for AudioInputStreams as used in Greenfoot. It is basically just an
 * interface for InputStream but with the getFormat method from AudioInputStream
 * added and three other useful methods: open, restart and getSource.
 * 
 * @author Poul Henriksen
 * 
 */
public interface GreenfootAudioInputStream extends Closeable
{
    /**
     * Opens this stream.
     * 
     * @throws UnsupportedAudioFileException
     *             if the stream does not point to valid audio file data
     *             recognised by the system
     * @throws IOException
     *             if an I/O exception occurs
     */
    public void open() throws IOException, UnsupportedAudioFileException;

    /**
     * Restarts this stream by repositioning to the beginning of the stream.
     * 
     * @throws UnsupportedAudioFileException
     *             if the stream does not point to valid audio file data
     *             recognised by the system
     * @throws IOException
     *             if an I/O exception occurs
     */
    public void restart() throws IOException, UnsupportedAudioFileException;

    /**
     * Gets the source where this stream comes from. Typically a filename on a
     * URL.
     */
    public String getSource();

    // ================================================================
    // Interface for AudioInputStream
    // ================================================================

    /**
     * Obtains the audio format of the sound data in this audio input stream.
     * 
     * @return an audio format object describing this stream's format
     */
    public AudioFormat getFormat();

    // ================================================================
    // Interface for InputStream
    // ================================================================

    /**
     * Reads the next byte of data from the audio input stream.  The audio input
     * stream's frame size must be one byte, or an <code>IOException</code>
     * will be thrown.
     *
     * @return the next byte of data, or -1 if the end of the stream is reached
     * @throws IOException if an input or output error occurs
     * @see #read(byte[], int, int)
     * @see #read(byte[])
     * @see #available
     * <p>
     * @see javax.sound.sampled.AudioInputStream#read()
     */
    public int read() throws IOException;


    /**
     * Reads some number of bytes from the audio input stream and stores them into
     * the buffer array <code>b</code>. The number of bytes actually read is
     * returned as an integer. This method blocks until input data is
     * available, the end of the stream is detected, or an exception is thrown.
     * <p>This method will always read an integral number of frames.
     * If the length of the array is not an integral number
     * of frames, a maximum of <code>b.length - (b.length % frameSize)
     * </code> bytes will be read.
     *
     * @param b the buffer into which the data is read
     * @return the total number of bytes read into the buffer, or -1 if there
     * is no more data because the end of the stream has been reached
     * @throws IOException if an input or output error occurs
     * @see #read(byte[], int, int)
     * @see #read()
     * @see #available
     * @see javax.sound.sampled.AudioInputStream#read(byte[]))
     */
    public int read(byte b[]) throws IOException;

    /**
     * Reads up to a specified maximum number of bytes of data from the audio
     * stream, putting them into the given byte array.
     * <p>This method will always read an integral number of frames.
     * If <code>len</code> does not specify an integral number
     * of frames, a maximum of <code>len - (len % frameSize)
     * </code> bytes will be read.
     *
     * @param b the buffer into which the data is read
     * @param off the offset, from the beginning of array <code>b</code>, at which
     * the data will be written
     * @param len the maximum number of bytes to read
     * @return the total number of bytes read into the buffer, or -1 if there
     * is no more data because the end of the stream has been reached
     * @throws IOException if an input or output error occurs
     * @see #read(byte[])
     * @see #read()
     * @see #skip
     * @see #available
     * @see javax.sound.sampled.AudioInputStream#read(byte[], int, int)
     */
    public int read(byte b[], int off, int len) throws IOException;


    /**
     * Skips over and discards a specified number of bytes from this
     * audio input stream.
     * @param n the requested number of bytes to be skipped
     * @return the actual number of bytes skipped
     * @throws IOException if an input or output error occurs
     * @see #read
     * @see #available
     * @see javax.sound.sampled.AudioInputStream#skip(long)
     */
    public long skip(long n) throws IOException;

    /**
     * Returns the maximum number of bytes that can be read (or skipped over) from this
     * audio input stream without blocking.  This limit applies only to the next invocation of
     * a <code>read</code> or <code>skip</code> method for this audio input stream; the limit
     * can vary each time these methods are invoked.
     * Depending on the underlying stream,an IOException may be thrown if this
     * stream is closed.
     * @return the number of bytes that can be read from this audio input stream without blocking
     * @throws IOException if an input or output error occurs
     * @see #read(byte[], int, int)
     * @see #read(byte[])
     * @see #read()
     * @see #skip
     * @see javax.sound.sampled.AudioInputStream#available()
     */
    public int available() throws IOException;


    /**
     * Closes this audio input stream and releases any system resources associated
     * with the stream.
     * @throws IOException if an input or output error occurs
     * @see javax.sound.sampled.AudioInputStream#close
     */
    public void close() throws IOException;

    /**
     * Marks the current position in this audio input stream.
     * @param readlimit the maximum number of bytes that can be read before
     * the mark position becomes invalid.
     * @see #reset
     * @see #markSupported
     * @see javax.sound.sampled.AudioInputStream#mark(int)
     */
    public void mark(int readlimit);

    /**
     * Repositions this audio input stream to the position it had at the time its
     * <code>mark</code> method was last invoked.
     * @throws IOException if an input or output error occurs.
     * @see #mark
     * @see #markSupported
     * @see javax.sound.sampled.AudioInputStream#reset()
     */
    public void reset() throws IOException;

    /**
     * Tests whether this audio input stream supports the <code>mark</code> and
     * <code>reset</code> methods.
     * @return <code>true</code> if this stream supports the <code>mark</code>
     * and <code>reset</code> methods; <code>false</code> otherwise
     * @see #mark
     * @see #reset
     * @see javax.sound.sampled.AudioInputStream#markSupported()
     */
    public boolean markSupported();

}
