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