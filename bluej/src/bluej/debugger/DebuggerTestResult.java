package bluej.debugger;

/**
 * A class representing the running of a single test
 * method.
 *
 * @author  Andrew Patterson
 * @version $Id: DebuggerTestResult.java 1727 2003-03-26 04:23:18Z ajp $
 */
public abstract class DebuggerTestResult
{
	/**
	 * Return the name of the test method.
	 * 
	 * @return  the name of the test method in the
	 *          form ClassName.methodName
	 */
    abstract public String getName();
    
    /**
     * Return whether this test method was a success.
     * 
     * @return  true if this test was a success
     */
    abstract public boolean isSuccess();
    
    /**
     * Return whether this test failed.
     * 
     * @return  true if this test resulted in a failure
     */
    abstract public boolean isFailure();
    
    abstract public boolean isError();
    
    /**
     * Return a stack trace for the test failure.
     * 
     * This method can be called only when the test
     * resulted in a failure or an error.
     * 
     * @return  a String of the stack trace of the error
     */
    abstract public String getTrace();
    
	/**
	 * Return an exception message for the test failure.
	 * 
	 * This method can be called only when the test
	 * resulted in a failure or an error.
	 * 
	 * @return  a String of the details of the exception
	 */
    abstract public String getExceptionMessage();
}
