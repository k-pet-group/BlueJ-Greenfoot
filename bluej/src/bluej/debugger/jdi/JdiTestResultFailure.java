package bluej.debugger.jdi;

/**
 * Represents the result of running a single test method.
 */
public class JdiTestResultFailure extends JdiTestResult
{
    JdiTestResultFailure(String className, String methodName, String exceptionMsg, String traceMsg)
    {
		super(className, methodName);

		if (exceptionMsg == null)
			throw new NullPointerException("exceptionMsg cannot be null");
			
		this.exceptionMsg = exceptionMsg;

		if (traceMsg != null)
			this.traceMsg = getFilteredTrace(traceMsg);
	    else
	    	this.traceMsg = null;
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
     * @see bluej.debugger.DebuggerTestResult#isError()
     */
    public boolean isError()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerTestResult#isFailure()
     */
    public boolean isFailure()
    {
        return true;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerTestResult#isSuccess()
     */
    public boolean isSuccess()
    {
        return false;
    }
}
