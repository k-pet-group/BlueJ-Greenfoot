package bluej.debugger.jdi;

import bluej.debugger.SourceLocation;

/**
 * Represents the result of running a single test method.
 */
public class JdiTestResultError extends JdiTestResult
{
    SourceLocation failPoint;

    JdiTestResultError(String className, String methodName, String exceptionMsg, String traceMsg, SourceLocation failPoint)
    {
		super(className, methodName);

		if (exceptionMsg == null)
			throw new NullPointerException("exceptionMsg cannot be null");
			
		this.exceptionMsg = exceptionMsg;

		if (traceMsg != null)
			this.traceMsg = getFilteredTrace(traceMsg);
	    else
	    	this.traceMsg = null;
        
        this.failPoint = failPoint;
    }
    
    /**
     * @see bluej.debugger.DebuggerTestResult#getExceptionMessage()
     */
    public String getExceptionMessage()
    {
        return exceptionMsg;
    }

    /**
     * 
     * @see bluej.debugger.DebuggerTestResult#getTrace()
     */
    public String getTrace()
    {
        return traceMsg;
    }
    
    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerTestResult#getExceptionLocation()
     */
    public SourceLocation getExceptionLocation()
    {
        return failPoint;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerTestResult#isError()
     */
    public boolean isError()
    {
        return true;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerTestResult#isFailure()
     */
    public boolean isFailure()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerTestResult#isSuccess()
     */
    public boolean isSuccess()
    {
        return false;
    }
}
