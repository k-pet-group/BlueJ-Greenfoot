/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2019  Michael Kolling and John Rosenberg
 
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
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Represents the result of running a single test method.
 */
@OnThread(Tag.Any)
public class JdiTestResultError extends JdiTestResult
{
    SourceLocation failPoint;

    JdiTestResultError(String className, String methodName, String displayName, String exceptionMsg, String traceMsg, SourceLocation failPoint, int runTimeMs)
    {
        super(className, methodName, displayName, runTimeMs);

        this.exceptionMsg = exceptionMsg;

        if (traceMsg != null) {
            this.traceMsg = getFilteredTrace(traceMsg);
        }
        else {
            this.traceMsg = null;
        }

        this.failPoint = failPoint;
    }
    
    @Override
    public String getExceptionMessage()
    {
        return exceptionMsg;
    }

    @Override
    public String getTrace()
    {
        return traceMsg;
    }
    
    @Override
    public SourceLocation getExceptionLocation()
    {
        return failPoint;
    }

    @Override
    public boolean isError()
    {
        return true;
    }

    @Override
    public boolean isFailure()
    {
        return false;
    }

    @Override
    public boolean isSuccess()
    {
        return false;
    }
}
