/**
 ** A type-ahead input buffer for the BlueJ terminal. Implemented with
 ** a circular array.
 **
 ** @author Michael Kolling
 ** @version
 **/

package bluej.terminal;

import bluej.utility.Debug;

import java.awt.Toolkit;

public final class InputBuffer 
{
    private char[] buffer;
    private int bufferNextFull = 0;	// next free position
    private int bufferNextFree = 0;	// next full position
    private int bufferSize;

    InputBuffer(int size)
    {
        buffer = new char[size];
        bufferSize = size;
    }

    public boolean putChar(char ch)
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

    public boolean backSpace()
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

    public char getChar()
    {
        // block until input available

        while(isEmpty()) {
            try {
                synchronized(this) {
                    wait();		// sleep until there is some input
                }
            } catch(InterruptedException e) {
                // ignore it
            }
        }

        char ch = buffer[bufferNextFull];
        bufferNextFull = advance(bufferNextFull);
        return ch;
    }

    public int numberOfCharacters()
    {
        if(bufferNextFree >= bufferNextFull)
            return bufferNextFree - bufferNextFull;
        else
            return (bufferNextFree + bufferSize) - bufferNextFull;
    }

    public synchronized void notifyReaders()
    {
        notify();
    }

    public boolean isFull()
    {
        return advance(bufferNextFree) == bufferNextFull;
    }

    public boolean isEmpty()
    {
        return bufferNextFull == bufferNextFree;
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
