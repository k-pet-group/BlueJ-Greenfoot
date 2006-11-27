package bluej.runtime;

import java.io.IOException;
import java.io.InputStream;

import bluej.terminal.InputBuffer;

/**
 * BlueJ input stream. An input stream filter to process "End of file"
 * signals (CTRL-Z or CTRL-D) from a terminal
 * 
 * @author Davin McCall
 * @version $Id: BJInputStream.java 4708 2006-11-27 00:47:57Z bquig $
 */
public class BJInputStream extends InputStream
{
    private InputStream source;
    private byte [] buffer = null;
    int buffoffset = 0;
    
    boolean endOfLine = false;
    boolean exOnEOL = false;
    
    /**
     * Construct a BJ
     * @param source  The source input stream, generally System.in
     */
    BJInputStream(InputStream source)
    {
        this.source = source;
    }
    
    public int read() throws IOException {
        
        if (exOnEOL && endOfLine)
            throw new IOException();
        
        int n = source.read();
        
        // Check for EOF signal
        if (n == InputBuffer.EOF_CHAR)
            return -1;
        
        // Are we line-buffering?
        if (exOnEOL && n == '\n')
            endOfLine = true;
        
        return n;
    }
    
    public int read(byte [] b, int off, int len) throws IOException
    {
        // reads are line buffered. If a complete line is read, we don't
        // want to read any more. So we flag this and it is handled in the
        // read() method.
        exOnEOL = true;
        int n = super.read(b, off, len);
        exOnEOL = false;
        endOfLine = false;
        return n;
    }
    
    public int available() throws IOException
    {
        return source.available();
    }
}
