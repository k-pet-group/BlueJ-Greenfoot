/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2012,2019  Michael Kolling and John Rosenberg
 
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
 * A class representing the running of a single test
 * method.
 *
 * @author  Andrew Patterson
 */
@OnThread(Tag.Any)
public abstract class DebuggerTestResult
{
    /**
     * Return the fully qualified name of the test method.
     * 
     * @return  the name of the test method in the
     *          form ClassName.methodName
     */
    public String getQualifiedMethodName()
    {
        String methodName = getMethodName();
        // We remove the method parameters' types that may have been included in the method name for JUnit 5
        String rawMethodName = (methodName.contains("(")) ? methodName.substring(0, methodName.indexOf("(")) : methodName;
        return getQualifiedClassName() + "." + rawMethodName;
    }

    /**
     * Get the name of the test method.
     */
    public abstract String getMethodName();

    /**
     * Get the fully qualified class name which the test method belongs to
     */
    public abstract String getQualifiedClassName();

    /**
     * Get the display name of the test method.
     */
    public abstract String getDisplayName();

    /**
     * Return the run time of the test in milliseconds
     * 
     * @return  the total running time of the test in
     *          milliseconds
     */
    abstract public int getRunTimeMs();
    
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
     * the exception was thrown). May return null if the information is not
     * available.
     * 
     * @return  a SourceLocation with the details of the failure point
     */
    abstract public SourceLocation getExceptionLocation();
}
