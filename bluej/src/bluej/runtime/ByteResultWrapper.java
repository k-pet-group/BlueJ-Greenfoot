package bluej.runtime;

import java.util.Hashtable;

/**
 ** @version $Id: ByteResultWrapper.java 47 1999-04-28 01:24:27Z ajp $
 ** @author Michael Cahill
 ** @author Michael Kolling
 ** Interface implemented by all "shell" classes - used for method invocation
 ** and object creation.
 **/

public class ByteResultWrapper
{
    public byte result;
    ByteResultWrapper(byte result) { this.result = result; }
}
