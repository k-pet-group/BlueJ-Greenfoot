package bluej.runtime;

import java.util.Hashtable;

/**
 ** @version $Id: ObjectResultWrapper.java 47 1999-04-28 01:24:27Z ajp $
 ** @author Michael Cahill
 ** @author Michael Kolling
 ** Interface implemented by all "shell" classes - used for method invocation
 ** and object creation.
 **/

public class ObjectResultWrapper
{
    public Object result;
    ObjectResultWrapper(Object result) { this.result = result; }
}
