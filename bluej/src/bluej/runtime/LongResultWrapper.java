package bluej.runtime;

import java.util.Hashtable;

/**
 ** @version $Id: LongResultWrapper.java 47 1999-04-28 01:24:27Z ajp $
 ** @author Michael Cahill
 ** @author Michael Kolling
 ** Interface implemented by all "shell" classes - used for method invocation
 ** and object creation.
 **/

public class LongResultWrapper
{
    public long result;
    LongResultWrapper(long result) { this.result = result; }
}
