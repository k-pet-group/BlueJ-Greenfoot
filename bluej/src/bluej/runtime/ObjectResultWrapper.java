package bluej.runtime;

/**
 ** @version $Id: ObjectResultWrapper.java 1819 2003-04-10 13:47:50Z fisker $
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
