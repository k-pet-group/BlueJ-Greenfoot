/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2019  Michael Kolling and John Rosenberg
 
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
package bluej.terminal;

import threadchecker.OnThread;
import threadchecker.Tag;

import java.awt.*;

/**
 * A type-ahead input buffer for the BlueJ terminal. Implemented with
 * a circular array.
 *
 * @author  Michael Kolling
 * @version $Id: InputBuffer.java 12537 2014-10-10 13:05:36Z nccb $
 */
@OnThread(Tag.Any)
public final class InputBuffer 
{
    private char[] buffer;
    private int bufferNextFull = 0;    // next free position
    private int bufferNextFree = 0;    // next full position
    private int bufferSize;
    private boolean eofMark = false;
    
    public static char EOF_CHAR = '\u0004'; // internal code for EOF

    public InputBuffer(int size)
    {
        buffer = new char[size];
        bufferSize = size;
    }

    public synchronized boolean putChar(char ch)
    {
        if(isFull()) {
            Toolkit.getDefaultToolkit().beep();
            return false;
        }
        else {
            buffer[bufferNextFree] = ch;
            bufferNextFree = advance(bufferNextFree);
            return true;
        }
    }

    public synchronized boolean putString(String input)
    {
        boolean putAny = false;
        for (char c : input.toCharArray())
        {
            // Ordering crucial here; we always want to try the method
            // call regardless of the value of putAny:
            putAny = putChar(c) || putAny;
        }
        return putAny;
    }

    public synchronized boolean backSpace()
    {
        if(!isEmpty()) {
            bufferNextFree = backwards(bufferNextFree);
            return true;
        }
        else {
            Toolkit.getDefaultToolkit().beep();
            return false;
        }
    }

    public synchronized char getChar()
    {
        // block until input available

        while(isEmpty()) {
            try {
                wait();        // sleep until there is some input
            } catch(InterruptedException e) {
                // our main process is telling us
                // we want to exit the character
                // reading loop
                // we'll return a return character
                return '\n';
            }
        }

        char ch = buffer[bufferNextFull];
        bufferNextFull = advance(bufferNextFull);
        // If an "end-of-file" has been signalled, send it now 
        if (eofMark) {
            eofMark = false;
            putChar(EOF_CHAR); // "Ctrl-D" character. This is really just a
            // code used internally; it does not need to match the system
            // EOF character.
            notifyReaders();
        }
        return ch;
    }
    
    /**
     * Signal that an EOF condition should be emulated by the terminal.
     */
    public synchronized void signalEOF()
    {
        // EOF is indicated by sending EOF_CHAR to the debug VM, which is then
        // interpreted by the custom input stream installed therein.
        if (! isFull())
            putChar(EOF_CHAR);
        else
            eofMark = true;
        notifyReaders();
    }

    public synchronized void notifyReaders()
    {
        notify();
    }

    public synchronized boolean isEmpty()
    {
        return bufferNextFull == bufferNextFree;
    }

    // This method should be synchronized if it is made public
    private boolean isFull()
    {
        return advance(bufferNextFree) == bufferNextFull;
    }
    
    private int advance(int pos)
    {
        return (++pos) % bufferSize;
    }

    private int backwards(int pos)
    {
        pos--;
        return (pos < 0 ? bufferSize - 1 : pos);
    }
}
