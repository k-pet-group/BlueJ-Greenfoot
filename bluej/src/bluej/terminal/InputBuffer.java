package bluej.terminal;

import java.awt.Toolkit;

/**
 * A type-ahead input buffer for the BlueJ terminal. Implemented with
 * a circular array.
 *
 * @author  Michael Kolling
 * @version $Id: InputBuffer.java 3315 2005-02-17 00:21:15Z davmac $
 */
public final class InputBuffer 
{
    private char[] buffer;
    private int bufferNextFull = 0;	// next free position
    private int bufferNextFree = 0;	// next full position
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
                synchronized(this) {
                    wait();		// sleep until there is some input
                }
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
