package bluej.utility;

/**
 ** @version $Id: Debug.java 36 1999-04-27 04:04:54Z mik $
 ** @author Michael Cahill
 ** Class to handle debugging message
 **/

public class Debug
{
    static final boolean debug = true;
	
    public static final void message(String msg)
    {
	if(debug) {
	    System.out.println(msg);
	    System.out.flush();
	}
    }
	
    public static final void assert(boolean condition)
    {
	if(debug && !condition)
	    throw new AssertionViolationError();
    }
}

