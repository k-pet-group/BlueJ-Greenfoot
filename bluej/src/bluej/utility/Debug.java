package bluej.utility;

/**
 ** @version $Id: Debug.java 63 1999-05-04 00:03:10Z mik $
 ** @author Michael Cahill
 ** Class to handle debugging message
 **/

public class Debug
{
    public static final void message(String msg)
    {
	System.out.println(msg);
	System.out.flush();
    }
	
    public static void reportError(String error)
    {
	System.err.println("Internal BlueJ error: " + error);
    }

    public static final void assert(boolean condition)
    {
	if(!condition)
	    throw new AssertionViolationError();
    }
}

