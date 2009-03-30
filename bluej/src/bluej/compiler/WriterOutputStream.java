/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.compiler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * An output stream which decodes its input using a character set encoding,
 * and writes the resulting characters to a Writer.
 * 
 * @author Davin McCall
 */
public class WriterOutputStream extends OutputStream
{
    private Charset cs = Charset.forName(System.getProperty("file.encoding"));
    private CharsetDecoder decoder = cs.newDecoder();
    
    /*
     * We use two buffers, one is a byte buffer and the other is a character buffer.
     * Bytes written to the output stream are stored in the byte buffer. When the
     * buffer gets full, the decoder is called; it reads from the byte buffer and
     * writes to the character buffer. When the character buffer is full, the
     * characters are written to the underlying writer.
     */
    
    ByteBuffer inBuffer;
    CharBuffer outBuffer;
    
    private Writer writer;

    /**
     * Create a new WriterOutputStream which writes to the given writer.
     */
    public WriterOutputStream(Writer writer)
    {
        this.writer = writer;
        decoder.reset();
        decoder.onMalformedInput(CodingErrorAction.REPLACE);
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        inBuffer = ByteBuffer.allocate(4096);
        inBuffer.clear();
        outBuffer = CharBuffer.allocate(4096);
        outBuffer.clear();
    }
    
    public void write(int b)
        throws IOException
    {
        write(new byte[] {(byte) b}, 0, 1);
    }

    public void write(byte[] b, int off, int len)
        throws IOException
    {
        int remaining = inBuffer.remaining();
        while (len > 0) {
            int toWrite = remaining;
            
            if (toWrite > len) {
                toWrite = len;
            }
            
            inBuffer.put(b, off, toWrite);
            off += toWrite;
            len -= toWrite;
            
            remaining -= toWrite;
            if (remaining == 0) {
                flush();
                remaining = inBuffer.remaining();
            }
        }
    }
    
    /**
     * Flush the input buffer (byte buffer), as much as possible. This may
     * leave a few undecoded bytes in the buffer.
     * 
     * @param endOfInput  true if there is no more input available
     * @throws IOException
     */
    private void flushInBuffer(boolean endOfInput) throws IOException
    {
        // Prepare to read from the input buffer
        inBuffer.flip();
        
        CoderResult result = decoder.decode(inBuffer, outBuffer, endOfInput);
        while (result.isOverflow()) {
            flushOutBuffer();
            result = decoder.decode(inBuffer, outBuffer, endOfInput);
        }
        
        // Remove processed input from the input buffer, and position for writing
        inBuffer.compact();
    }
    
    /**
     * Flush the output buffer (character buffer). All characters which have
     * been decoded so far will be written to the underlying writer.
     * 
     * @throws IOException
     */
    private void flushOutBuffer() throws IOException
    {
        outBuffer.flip();
        writer.write(outBuffer.toString());
        outBuffer.clear();
    }
    
    /**
     * Decode as much input as possible, write all decoded input to the
     * underlying writer, and flush the writer.
     * 
     * @param endOfInput  true if there is no more input available
     * @throws IOException
     */
    private void flush(boolean endOfInput)
        throws IOException
    {
        flushInBuffer(endOfInput);
        flushOutBuffer();
        writer.flush();
    }
    
    public void flush() throws IOException
    {
        flush(false);
    }
    
    public void close() throws IOException
    {
        if (writer != null) {
            flush(true);
            writer = null;
        }
    }
}
