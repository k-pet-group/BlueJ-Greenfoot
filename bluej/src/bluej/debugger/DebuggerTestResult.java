package bluej.debugger;

import java.util.List;

/**
 *  A class representing an object in the debugged VM.
 *
 * @author  Andrew Patterson
 * @version $Id: DebuggerTestResult.java 1626 2003-02-11 01:46:35Z ajp $
 */
public abstract class DebuggerTestResult
{
    abstract public int runCount();
    
    abstract public int errorCount();
    
    abstract public int failureCount();
    
    abstract public String getError(int i);
    
    abstract public String getFailure(int i);
}
