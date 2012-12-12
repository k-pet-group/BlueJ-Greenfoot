/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2012  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bluej.Config;
import bluej.collect.DataCollector;
import bluej.debugger.DebuggerTestResult;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.role.UnitTestClassRole;
import bluej.testmgr.TestDisplayFrame;

/**
 * Provide a thread class for running unit tests.
 * 
 * Unit tests are user code so they must be executed on a seperate thread.
 * This class provides the means to do this.
 * 
 * There are two primary modes of operation: run a single test (methodname != null),
 * and run all tests for a series of ClassTargets.
 * 
 * @author Davin McCall
 */
public class TestRunnerThread extends Thread
{
    private Iterator<ClassTarget> testIterator;
    private DebuggerTestResult lastResult = null;
    private PkgMgrFrame pmf;

    private ClassTarget ct;
    private String[] allMethods;
    private String methodName; // Name of the test method; null to run all tests.
    
    private int state;
    
    /**
     * Construct a test runner thread for running multiple tests.
     */
    public TestRunnerThread(PkgMgrFrame pmf, Iterator<ClassTarget> i)
    {
        this.pmf = pmf;
        testIterator = i;
        state = 0;
    }
    
    /**
     * Construct a test runner thread for running a single test.
     */
    public TestRunnerThread(PkgMgrFrame pmf, ClassTarget ct, String methodName)
    {
        this.pmf = pmf;
        List<ClassTarget> l = new ArrayList<ClassTarget>(1);
        l.add(ct);
        testIterator = l.iterator();
        this.methodName = methodName;
        state = 0;
    }
    
    /**
     * Set the methods to be tested. The UnitTestClassRole calls this after determining
     * which methods should be run.
     * 
     * @param methods  An array of method names
     */
    public void setMethods(String [] methods)
    {
        allMethods = methods;
    }
    
    public void run()
    {
        // This implements a state machine. State 0 is the first state, and consists of
        // the primary loop from which the other states are executed
        // (via EventQueue.invokeAndWait).
        
        switch (state) {
            case 0:
                try {
                    while (testIterator.hasNext()) {
                        
                        ct = (ClassTarget) testIterator.next();
                        
                        if (methodName == null) {
                            // Run all tests for a target.
                            state = 1;
                            EventQueue.invokeAndWait(this);
                        }
                        else {
                            // Run only a single test.
                            allMethods = new String [] { methodName };
                        }
                        
                        // State 1 has given us the tests we need to run. Now run them:
                        for (int i = 0; i < allMethods.length; i++) {
                            lastResult = pmf.getProject().getDebugger().runTestMethod(ct.getQualifiedName(), allMethods[i]);
                            
                            // Add the test result to the test display frame in state 2:
                            state = 2;
                            EventQueue.invokeAndWait(this);
                        }
                    }
                    
                    // Finally, tell the PkgMgrFrame that we're done:
                    state = 3;
                    EventQueue.invokeAndWait(this);
                }
                catch (InvocationTargetException ite) { ite.printStackTrace(); }
                catch (InterruptedException ie) {}
                break;
                
            // State 1 is where we confirm that we really do have an executable unit
            // test class, and we delegate to the unit test role to gives us some
            // test methods to executed.
            case 1:
                if (ct.isCompiled() && ct.isUnitTest() && ! ct.isAbstract()) {
                    UnitTestClassRole utcr = (UnitTestClassRole) ct.getRole();

                    utcr.doRunTest(pmf, ct, TestRunnerThread.this);
                }
                else {
                    allMethods = new String[0];
                }
                break;
                
            // Here we add a test result to the test display frame.
            case 2:
                boolean quiet = methodName != null && lastResult.isSuccess();
                TestDisplayFrame.getTestDisplay().addResult(lastResult, quiet);
                
                if (quiet)
                    pmf.setStatus(methodName + " " + Config.getString("pkgmgr.test.succeeded"));

                DataCollector.testResult(pmf.getPackage(), lastResult);
                
                break;

            // Now we are finished.
            case 3:
                if (methodName == null)
                    pmf.endTestRun();
                
                break;
        }
    }
}
