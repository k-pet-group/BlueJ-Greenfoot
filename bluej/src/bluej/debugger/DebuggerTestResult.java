package bluej.debugger;

/**
 * A class representing the running of a single test
 * method.
 *
 * @author  Andrew Patterson
 * @version $Id: DebuggerTestResult.java 2926 2004-08-23 02:48:40Z davmac $
 */
public abstract class DebuggerTestResult
{
	/**
	 * Return the fully qualified name of the test method.
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
     * If !isSuccess then this returns true if the
     * test result was an expected 'failure'.
     * 
     * @return  true if this test resulted in a failure
     */
    abstract public boolean isFailure();
    
	/**
	 * If !isSuccess then this returns true if the
	 * test result was an unexpected 'error'.
	 * 
	 * @return  true if this test resulted in an error
	 */
    abstract public boolean isError();
    
    /**
     * Return a stack trace for the test failure/error.
     * 
     * This method can be called only when the test
     * resulted in a failure or an error.
     * 
     * @return  a String of the stack trace of the failure/error
     */
    abstract public String getTrace();
    
	/**
	 * Return an exception message for the test failure/error.
	 * 
	 * This method can be called only when the test
	 * resulted in a failure or an error.
	 * 
	 * @return  a String of the details of the exception thrown
	 */
    abstract public String getExceptionMessage();
    
    /**
     * Return the location of the failure/error point (ie. the point where
     * the exception was thrown)
     * 
     * @return  a SourceLocation with the details of the failure point
     */
    abstract public SourceLocation getExceptionLocation();
}
