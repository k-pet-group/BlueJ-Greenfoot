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
     * Runs a suite extracted from a TestCase subclass.
     */
/*    static public TestResult run(Class testClass)
    {
        return run(new TestSuite(testClass));
    } */

    /**
     * Runs a single test and collects its results.
     */
    static public TestResult run(Test test)
    {
        RemoteTestRunner runner= new RemoteTestRunner();

        return runner.doRun(test);
    }

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
//        ResultPrinter rp = new ResultPrinter(System.out);

        TestResult result = createTestResult();
        result.addListener(this);
//		result.addListener(rp);
//        long startTime= System.currentTimeMillis();
        suite.run(result);
//        long endTime= System.currentTimeMillis();
//        long runTime= endTime-startTime;
//        rp.print(result, runTime);

        return result;
    }

/*    public static void main(String args[])
    {
        TestRunner aTestRunner= new TestRunner();
        try {
            TestResult r= aTestRunner.start(args);
            if (!r.wasSuccessful()) 
                System.exit(FAILURE_EXIT);
            System.exit(SUCCESS_EXIT);
        } catch(Exception e) {
            System.err.println(e.getMessage());
            System.exit(EXCEPTION_EXIT);
        }
    }


		//junit.runner.BaseTestRunner.getFilteredTrace(failure.trace()));
*/     
    protected void runFailed(String message)
    {
        System.err.println(message);
    }
    
}