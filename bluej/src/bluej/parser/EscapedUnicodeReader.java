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
package bluej.parser;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
public final class EscapedUnicodeReader extends Reader
{
    Reader sourceReader;

    // A buffer of characters read (while looking for a unicode escape sequence)
    // that should be used before reading from the source reader
    // List may seem a bit overkill, but it should be used very rarely
    private LinkedList<Character> buffer = new LinkedList<Character>();
    
    private int position; // position within source stream
    private int line = 1;
    private int column = 1;


    public EscapedUnicodeReader(Reader source)
    {
        sourceReader = source;
    }
    
    public void setLineCol(int line, int column)
    {
        this.line = line;
        this.column = column;
    }

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
                else { 
                    break;
                }
            }
        }

        // if we failed to read anything, it's due to end-of-stream
        if (numRead == 0 && len != 0)
            numRead = -1;

        return numRead;
    }

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
        if (!buffer.isEmpty()) {
            char bufferedChar = buffer.pollFirst();
            processChar(bufferedChar);
            return bufferedChar;
        }
        else {
            rchar = readSourceChar();
        }

        if (rchar == '\\') {
            // This could be the beginning of an escaped unicode sequence,
            // \\uXXXX (with only a single backslash)
            return readEscapedUnicodeSequence();
        }

        return rchar;
    }

    private int readEscapedUnicodeSequence() throws IOException
    {
        // The Java Language Spec specifies that any number of 'u' characters may appear in sequence
        // as part of a unicode escape.
        int uc = sourceReader.read();
        if (uc != 'u') {
            if (uc != -1) buffer.addLast((char)uc);
            return '\\';
        }
        
        // Used to keep a record of all the characters we've consumed,
        // in case it's not a valid escape and we have to go through them all again:
        List<Character> seenSoFar = new ArrayList<Character>();
        seenSoFar.add((char)uc);
        
        while (uc == 'u') {
            uc = sourceReader.read();
            if (uc != -1) seenSoFar.add((char)uc);
        }
                      
        int val = Character.digit((char) uc, 16);
        if (val == -1) {
            buffer.addAll(seenSoFar);
            return '\\';
        }
        
        for (int i = 0; i < 3; i++) {
            val *= 0x10;
            uc = sourceReader.read();
            if (uc != -1) seenSoFar.add((char)uc);
            int digitVal = Character.digit((char) uc, 16);
            if (digitVal == -1) {
                buffer.addAll(seenSoFar);
                return '\\';
            }
            val += digitVal;
        }
        
        // Only update position based on the chars once we know
        // we've found a valid escape:
        for (char c : seenSoFar) {
            processChar(c);
        }
        
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
}
