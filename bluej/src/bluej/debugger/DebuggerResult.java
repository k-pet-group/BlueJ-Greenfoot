package bluej.debugger;

/**
 * This class represents the result of a debugger invocation (execution of user code).
 * The three result types are:
 *  NORMAL_EXIT - execution completed normally, result object may be available
 *  EXCEPTION - execution terminated via an exception, ExceptionDescription available
 *  TERMINATED - remote VM terminated before execution completed (possibly result of
 *                System.exit() call).
 * 
 * @author Davin McCall
 */
public class DebuggerResult
{
    private int exitStatus; // one of Debugger.NORMAL_EXIT, EXCEPTION, TERMINATED
    private DebuggerObject resultObject;
    private ExceptionDescription exception;
    
    /**
     * Construct a DebuggerResult for a normal completion.
     * @param resultObject  The result of the execution.
     */
    public DebuggerResult(DebuggerObject resultObject)
    {
        exitStatus = Debugger.NORMAL_EXIT;
        this.resultObject = resultObject;
    }
    
    /**
     * Construct a DebuggerResult for an execution which resulted in an exception.
     */
    public DebuggerResult(ExceptionDescription exception)
    {
        exitStatus = Debugger.EXCEPTION;
        this.exception = exception;
    }
    
    public DebuggerResult(int status)
    {
        exitStatus = status;
    }
    
    public int getExitStatus()
    {
        return exitStatus;
    }
    
    public DebuggerObject getResultObject()
    {
        return resultObject;
    }
    
    public ExceptionDescription getException()
    {
        return exception;
    }
}
