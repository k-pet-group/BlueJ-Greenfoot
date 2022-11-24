/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.debugger;

import threadchecker.OnThread;
import threadchecker.Tag;

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
@OnThread(Tag.Any)
public class DebuggerResult
{
    private final int exitStatus; // one of Debugger.NORMAL_EXIT, EXCEPTION, TERMINATED
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
    
    /**
     * Get the status of the invocation. Returns one of:
     * <ul>
     * <li>Debugger.NORMAL_EXIT - the invocation succeeded
     * <li>Debugger.EXCEPTION   - an exception occurred in the invoked code
     * <li>Debugger.TERMINATED  - the debug VM exited (possibly due to a
     *                            System.exit() call)
     * </ul>
     * @return
     */
    public int getExitStatus()
    {
        return exitStatus;
    }
    
    /**
     * Returns the result of the invocation as a DebuggerObject.
     * This is only valid for a NORMAL_EXIT; any other invocation
     * result will cause this method to return null.
     */
    public DebuggerObject getResultObject()
    {
        return resultObject;
    }
    
    public ExceptionDescription getException()
    {
        return exception;
    }
}
