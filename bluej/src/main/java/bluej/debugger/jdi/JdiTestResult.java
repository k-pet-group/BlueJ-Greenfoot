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
package bluej.debugger.jdi;

import java.io.*;
import java.util.Arrays;
import java.util.stream.Collectors;

import bluej.debugger.DebuggerTestResult;
import bluej.debugger.SourceLocation;
import bluej.utility.Utility;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Represents the result of running a single test method.
 */
@OnThread(Tag.Any)
public class JdiTestResult extends DebuggerTestResult
{
    protected String className;
    protected String methodName;
    protected String displayName;
    protected String exceptionMsg, traceMsg;  // null if no failure
    protected int runTimeMs;

    JdiTestResult(String className, String methodName, String displayName, int runTimeMs)
    {
        if (className == null || methodName == null)
            throw new NullPointerException("constructing JdiTestResult");

        this.className = className;
        this.methodName = methodName;
        this.displayName = displayName;
        this.runTimeMs = runTimeMs;

        this.exceptionMsg = null;
        this.traceMsg = null;
    }

    public String getQualifiedClassName()
    {
        return className;
    }

    public String getMethodName()
    {
        return methodName;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * @see bluej.debugger.DebuggerTestResult#getExceptionMessage()
     */
    public String getExceptionMessage()
    {
        throw new IllegalStateException("getting Exception message from successful test");
    }

    /**
     * @see bluej.debugger.DebuggerTestResult#getRunTimeMs()
     */
    public int getRunTimeMs()
    {
        return runTimeMs;
    }

    /**
     * @see bluej.debugger.DebuggerTestResult#getTrace()
     */
    public String getTrace()
    {
        throw new IllegalStateException("getting stack trace from successful test");
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerTestResult#getExceptionLocation()
     */
    public SourceLocation getExceptionLocation()
    {
        throw new IllegalStateException("getting stack trace from successful test");
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
        return false;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.DebuggerTestResult#isSuccess()
     */
    public boolean isSuccess()
    {
        return true;
    }

    /**
     * Filters stack frames from internal JUnit classes
     */
    public static String getFilteredTrace(String stack)
    {
        String[] lines = Utility.splitLines(stack);

        int lastLine = lines.length - 1;
        while (lastLine >= 0)
        {
            if (filterLine(lines[lastLine]))
            {
                lastLine -= 1;
            }
            else
            {
                break;
            }
        }

        return Arrays.stream(lines, 0, lastLine + 1).collect(Collectors.joining("\n"));
    }

    static boolean filterLine(String line)
    {
        String[] patterns = new String[]{
                "junit.framework.TestCase",
                "junit.framework.TestResult",
                "junit.framework.TestSuite",
                "junit.framework.Assert.", // don't filter AssertionFailure
                "junit.swingui.TestRunner",
                "junit.awtui.TestRunner",
                "junit.textui.TestRunner",
                "org.junit.runner",
                "org.junit.internal",
                "sun.reflect.",
                "jdk.internal.reflect.",
                "bluej.",
                "java.lang.reflect.Method.invoke("
        };
        for (int i = 0; i < patterns.length; i++)
        {
            if (line.indexOf(patterns[i]) > 0)
                return true;
        }
        return false;
    }
}
