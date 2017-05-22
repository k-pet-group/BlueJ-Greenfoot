/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2013,2015  Michael Kolling and John Rosenberg 

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
package bluej.parser;

import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.IOException;
import java.io.Reader;

/**
 * This is a Reader processes the stream from another reader, replacing unicode escape
 * sequences (backslash-'u'-XXXX where XXXX is a four digit hexadecimal number) with the
 * characters they represent.
 * 
 * This is suitable for a java pre-processor, before the lexer stage. It allows the lexer
 * to correctly recognize keywords, identifiers etc. which have embedded unicode escape
 * sequences.
 * 
 * @author Davin McCall
 */
@OnThread(Tag.Any)
public final class EscapedUnicodeReader extends Reader
{
    Reader sourceReader;

    private boolean charIsBuffered;
    private int bufferedChar;
    
    private int position; // position within source stream
    private int line = 1;
    private int column = 1;


    public EscapedUnicodeReader(Reader source)
    {
        sourceReader = source;
    }
    
    public void setLineColPos(int line, int column, int position)
    {
        this.line = line;
        this.column = column;
        this.position = position;
    }

    @Override
    public int read(char [] buffer, int off, int len) throws IOException
    {
        int numRead = 0;
        while (len > 0) {
            try {
                int r = getChar();
                if (r == -1)
                    break;
                buffer[off++] = (char) r;
                len--;
                numRead++;
            }
            catch (IOException ioe) {
                // If we got an exception, but successfully read some characters,
                // we should return those characters.
                if (numRead == 0) {
                    throw ioe;
                }
                break;
            }
        }

        // if we failed to read anything, it's due to end-of-stream
        if (numRead == 0 && len != 0)
            numRead = -1;

        return numRead;
    }

    @Override
    public void close() throws IOException
    {
        sourceReader.close();
    }

    /**
     * Get a single character, which may be an escaped unicode character (\\uXXXX, with a
     * single leading backslash)
     */
    private int getChar() throws IOException
    {
        int rchar;
        if (charIsBuffered) {
            charIsBuffered = false;
            if (bufferedChar != -1) {
                processChar((char) bufferedChar);
            }
            return bufferedChar;
        }
        rchar = readSourceChar();

        if (rchar == '\\') {
            // This could be the beginning of an escaped unicode sequence,
            // \\uXXXX (with only a single backslash)
            int nchar = sourceReader.read();

            if (nchar == 'u') {
                column++; position++;
                return readEscapedUnicodeSequence();
            }
            putBuffer(nchar);             
            return '\\';
        }

        return rchar;
    }

    private void putBuffer(int nchar)
    {
        bufferedChar = nchar;
        charIsBuffered = true;
    }

    private int readEscapedUnicodeSequence() throws IOException
    {
        // The Java Language Spec specifies that any number of 'u' characters may appear in sequence
        // as part of a unicode escape.
        int uc = sourceReader.read();
        while (uc == 'u') {
            processChar((char)uc);
            uc = sourceReader.read();
        }
        
        int val = Character.digit((char) uc, 16);
        if (val == -1) {
            putBuffer(uc);
            return 0xFFFF;
        }
        processChar((char)uc);
        
        int i = 0;
        do {
            val *= 0x10;
            uc = sourceReader.read();
            int digitVal = Character.digit((char) uc, 16);
            if (digitVal == -1) {
                putBuffer(uc);
                return 0xFFFF;
            }
            processChar((char)uc);
            val += digitVal;
            i++;
        } while (i < 3);
        
        return val;
    }

    private int readSourceChar() throws IOException
    {
        int rchar = sourceReader.read();
        if (rchar != -1) {
            processChar((char) rchar);
        }
        return rchar;
    }
    
    private void processChar(char ch)
    {
        position++;
        if (ch == '\n') {
            line++;
            column = 1;
        }
        else {
            column++;
        }
    }
    
    /**
     * Get the position within the source stream (i.e. number of characters read).
     */
    public int getPosition()
    {
        return position;
    }

    public int getLine()
    {
        return line;
    }

    public int getColumn()
    {
        return column;
    }

    @Override
    public int read() throws IOException
    {
        return getChar();
    }
}
