package bluej.runtime;

// NOTE: CURRENTLY UNUSED. PENDING FOR REMOVAL

/**
 * The ExitChecker checks whether a System.exit() call has occurred.
 * ATTENTION: The class needs jdk 1.4 to be compiled!
 *
 * @author  Michael Kolling
 * @version $Id: ExitChecker.java 2370 2003-11-19 00:50:01Z ajp $
 */
public class ExitChecker
{

    /**
     * We need to recognise System.exit calls. We do this by installing a security
     * manager that traps the checkExit() call. checkExit() gets called from
     * within Runtime.exit(). The problem is that there could be (and are) other
     * places calling checkExit. So: if we detect a checkExit call, we start 
     * checking the stack here to see whether we are coming from Runtime.exit().
     * If so, we have a real exit call and return true, if not it's something else
     * and we return false.
     */
    public static boolean isSystemExit()
    {
        // in order to get a stack trace, we throw and catch an exception
        try {
            throwException();
        }
        catch(IllegalStateException exc) {
            // StackTraceElement[] stack = exc.getStackTrace(); // needs jdk 1.4!
            
            // in a real Runtime.exit(), the stack top looks like this:
            // [0] bluej.runtime.ExitChecker:throwException
            // [1] bluej.runtime.ExitChecker:isSystemExit
            // [2] bluej.runtime.RemoteSecurityManager:checkExit
            // [3] java.lang.Runtime:exit

            //if((stack[3].getClassName().equals("java.lang.Runtime")) &&
              // (stack[3].getMethodName().equals("exit")))
                //return true;
        }
        return false;
    }
    
    private static void throwException()
        throws IllegalStateException
    {
        throw new IllegalStateException();
    }
}
