package bluej.debugger.jdi;

import java.util.*;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.request.*;

import bluej.debugger.DebuggerTestResult;

/**
 * Represents the result of running
 */
public class JdiTestResult extends DebuggerTestResult
{
    private int runCount, errorCount, failureCount;
    private String errors[];
    private String failures[];
    
    JdiTestResult(ArrayReference arrayRef)
    {
        if (arrayRef == null)
            throw new NullPointerException("constructing JdiTestResult");

        try {
            runCount = Integer.parseInt(((StringReference) arrayRef.getValue(0)).value());
            errorCount = Integer.parseInt(((StringReference) arrayRef.getValue(1)).value());
            failureCount = Integer.parseInt(((StringReference) arrayRef.getValue(2)).value());
        }
        catch (NumberFormatException nfe) { }

        errors = new String[errorCount];
        failures = new String[failureCount];
                    
        for(int i=0; i<errorCount; i++)
            errors[i] = ((StringReference) arrayRef.getValue(3+i)).value();

        for(int i=0; i<failureCount; i++)
            failures[i] = ((StringReference) arrayRef.getValue(3+errorCount+i)).value();
    }
    
    public int runCount() { return runCount; }
    
    public int errorCount()  { return errorCount; }
    
    public int failureCount()  { return failureCount; }
    
    public String getError(int i) { return errors[i]; }
    
    public String getFailure(int i) { return failures[i]; }

}