package bluej.runtime;

import java.util.Hashtable;

/**
 ** @version $Id: DoubleResultWrapper.java 44 1999-04-28 00:22:34Z ajp $
 ** @author Michael Cahill
 ** @author Michael Kolling
 ** Interface implemented by all "shell" classes - used for method invocation
 ** and object creation.
 **/

public class DoubleResultWrapper
{
    public double result;
    DoubleResultWrapper(double result) { this.result = result; }
}
