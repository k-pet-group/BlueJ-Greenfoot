package bluej.debugger.jdi;

import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import bluej.debugger.DebuggerTerminal;

/*
 * Stub terminal implementation for testing.
 * 
 * @author Davin McCall
 * @version $Id: TestTerminal.java 3077 2004-11-09 04:33:53Z davmac $
 */
public class TestTerminal
    implements DebuggerTerminal
{

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerTerminal#getErrorWriter()
     */
    public Writer getErrorWriter()
    {
        // return new FakeWriter();
        return new OutputStreamWriter(System.err);
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerTerminal#getWriter()
     */
    public Writer getWriter()
    {
        return new FakeWriter();
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerTerminal#getReader()
     */
    public Reader getReader()
    {
        return new FakeReader();
    }

}

/**
 * A Writer whose output goes into a black hole.
 * 
 * @author Davin McCall
 */
class FakeWriter extends Writer
{
    public void flush()
    {
        
    }
    
    public void write(char [] data, int off, int len)
    {

    }
    
    public void close()
    {
        
    }
}

class FakeReader extends Reader
{
    public int read(char [] data, int off, int len)
    {
        return -1;
    }
    
    public void close()
    {
    }
}
