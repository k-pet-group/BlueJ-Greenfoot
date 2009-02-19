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
package bluej.runtime;

import junit.framework.*;
import junit.runner.*;

/**
 * RemoteTestRunner expects the name of a TestCase class as argument.
 * If this class defines a static <code>suite</code> method it 
 * will be invoked and the returned test is run. Otherwise all 
 * the methods starting with "test" having no arguments are run.
 */
public class RemoteTestRunner extends BaseTestRunner
{
    /**
     * Always use the StandardTestSuiteLoader. Overridden from
     * BaseTestRunner.
     */
    public TestSuiteLoader getLoader()
    {
        return new StandardTestSuiteLoader();
    }

    public void testFailed(int status, Test test, Throwable t)
    {
    }
    
    public void testStarted(String testName)
    {
    }
    
    public void testEnded(String testName)
    {
    }

    /**
     * Creates the TestResult to be used for the test run.
     */
    protected TestResult createTestResult()
    {
        return new TestResult();
    }
    
    public TestResult doRun(Test suite)
    {
        TestResult result = createTestResult();
        result.addListener(this);
//        long startTime= System.currentTimeMillis();
        suite.run(result);
//        long endTime= System.currentTimeMillis();
//        long runTime= endTime-startTime;

        return result;
    }

    protected void runFailed(String message)
    {
        System.err.println(message);
    }
    
}