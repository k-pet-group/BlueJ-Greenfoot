package bluej.runtime;

import java.io.IOException;
import java.io.InputStream;

import bluej.terminal.InputBuffer;

/**
 * BlueJ input stream. An input stream filter to process "End of file"
 * signals (CTRL-Z or CTRL-D) from a terminal
 * 
 * @author Davin McCall
 * @version $Id: BJInputStream.java 3315 2005-02-17 00:21:15Z davmac $
 */
public class BJInputStream extends InputStream
{
    private InputStream source;
    
    /**
     * Construct a BJ
     * @param source  The source input stream, generally System.in
     */
    BJInputStream(InputStream source)
    {
        this.source = source;
    }
    
    /* (non-Javadoc)
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException {
        int n = source.read();
        // CTRL-Z or CTRL-D
        if (n == InputBuffer.EOF_CHAR)
            return -1;
        return n;
    }
}
