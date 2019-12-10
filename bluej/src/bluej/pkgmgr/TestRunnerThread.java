/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2012,2014,2016,2018,2019  Michael Kolling and John Rosenberg
 
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

import bluej.Config;
import bluej.collect.DataCollector;
import bluej.debugger.DebuggerTestResult;
import bluej.debugger.jdi.TestResultsWithRunTime;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.role.UnitTestClassRole;
import bluej.testmgr.TestDisplayFrame;
import bluej.utility.Debug;
import javafx.application.Platform;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
    @OnThread(Tag.Worker)
    private final Iterator<ClassTarget> testIterator;
    private final PkgMgrFrame pmf;

    private final String methodName; // Name of the test method; null to run all tests.
    
    private int state;
    private final Project project;

    /**
     * Construct a test runner thread for running multiple tests.
     */
    @OnThread(Tag.FXPlatform)
    public TestRunnerThread(PkgMgrFrame pmf, Iterator<ClassTarget> i)
    {
        this.pmf = pmf;
        this.project = pmf.getProject();
        this.methodName = null;
        testIterator = i;
        state = 0;
    }
    
    /**
     * Construct a test runner thread for running a single test.
     */
    @OnThread(Tag.FXPlatform)
    public TestRunnerThread(PkgMgrFrame pmf, ClassTarget ct, String methodName)
    {
        this.pmf = pmf;
        this.project = pmf.getProject();
        List<ClassTarget> l = new ArrayList<ClassTarget>(1);
        l.add(ct);
        testIterator = l.iterator();
        this.methodName = methodName;
        state = 0;
    }

    @OnThread(value = Tag.Worker, ignoreParent = true)
    public void run()
    {
        while (testIterator.hasNext()) 
        {

            ClassTarget ct = testIterator.next();

            List<String> allMethods;
            if (methodName == null)
            {
                // Run all tests for a target, so find out what they are:
                CompletableFuture<List<String>> methodsFuture = new CompletableFuture<>();
                Platform.runLater(() -> startTestFindMethods(ct, methodsFuture));
                try
                {
                    allMethods = methodsFuture.get();
                }
                catch (InterruptedException | ExecutionException e)
                {
                    Debug.reportError(e);
                    allMethods = Collections.emptyList();
                }
            }
            else 
            {
                // Run only a single test.
                allMethods = Arrays.asList(methodName);
            }

            // State 1 has given us the tests we need to run. Now run them:
            // With JUnit 5, a method does not always match to 1 test (parameterized),
            // so we should not rely on this to assume there are no more than 1 result for 1 single method of test.
            if (allMethods.size() > 0)
            {
                TestResultsWithRunTime lastResults = project.getDebugger().runTestMethod(ct.getQualifiedName(), (allMethods.size() == 1) ? methodName : null);
                // Add all test results to the test display frame:
                for (DebuggerTestResult result : lastResults.getResults())
                {
                    Platform.runLater(() -> showNextResult(result));
                }
                Platform.runLater(() -> TestDisplayFrame.getTestDisplay()
                        .updateTotalTimeMs(lastResults.getTotalRunTime()));
            }
        }

        // Finally, tell the PkgMgrFrame that we're done:
        Platform.runLater(() -> {
            if (methodName == null)
                pmf.endTestRun();
        });
    }

    @OnThread(Tag.FXPlatform)
    private void showNextResult(DebuggerTestResult lastResult)
    {
        // Here we add a test result to the test display frame.
        boolean quiet = methodName != null && lastResult.isSuccess();
        TestDisplayFrame.getTestDisplay().addResult(lastResult, quiet);

        if (quiet)
            pmf.setStatus(methodName + " " + Config.getString("pkgmgr.test.succeeded"));

        DataCollector.testResult(pmf.getPackage(), lastResult);
    }

    @OnThread(Tag.FXPlatform)
    private void startTestFindMethods(ClassTarget ct, CompletableFuture<List<String>> methodsFuture)
    {
        // State 1 is where we confirm that we really do have an executable unit
        // test class, and we delegate to the unit test role to gives us some
        // test methods to executed.
        if (ct.isCompiled() && ct.isUnitTest() && ! ct.isAbstract()) {
            UnitTestClassRole utcr = (UnitTestClassRole) ct.getRole();

            List<String> allMethods = utcr.startRunTest(pmf, ct, TestRunnerThread.this);
            if (allMethods == null)
                methodsFuture.complete(Collections.emptyList());
            else
                methodsFuture.complete(allMethods);
        }
        else {
            methodsFuture.complete(Collections.emptyList());
        }
    }
}
