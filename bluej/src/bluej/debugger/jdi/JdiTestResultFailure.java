/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
package bluej.debugger.jdi;

import bluej.debugger.SourceLocation;

/**
 * Represents the result of running a single test method.
 */
public class JdiTestResultFailure extends JdiTestResult
{
    SourceLocation failPoint;
    
    JdiTestResultFailure(String className, String methodName, String exceptionMsg, String traceMsg, SourceLocation failPoint)
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
