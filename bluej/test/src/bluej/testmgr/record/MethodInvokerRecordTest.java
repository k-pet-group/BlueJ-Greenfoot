package bluej.testmgr.record;

/**
 * Write a description of the test class $CLASSNAME here.
 *
 * @author  (your name)
 * @version (a version number or a date)
 */
public class MethodInvokerRecordTest extends junit.framework.TestCase
{
    MethodInvokerRecord lotsOfAssertions;
    
    /**
     * Sets up the test fixture.
     *
     * Called before every test case method.
     */
    protected void setUp()
    {
        lotsOfAssertions = new MethodInvokerRecord(java.lang.Math.class,
                                                    "Math.pow(4,3)");
        lotsOfAssertions.addAssertion(
            InvokerRecord.makeAssertionStatement("assertNotNull"));
        lotsOfAssertions.addAssertion(
            InvokerRecord.makeAssertionStatement("assertEquals","8"));
        lotsOfAssertions.addAssertion(
            InvokerRecord.makeAssertionStatement("assertEquals","8","0.1"));
    }

    /**
     * Tears down the test fixture.
     *
     * Called after every test case method.
     */
    protected void tearDown()
    {
    }

    public void testMethodInvokerNoBench()
    {
        String testMethodResultNoBench =
            "\t\t{\n" +
            "\t\t\tjava.lang.Math result = Math.pow(4,3);\n" +
            "\t\t\tassertNotNull(result);\n" +
            "\t\t\tassertEquals(8, result);\n" +
            "\t\t\tassertEquals(8, result, 0.1);\n" +
            "\t\t}\n";
        assertEquals(testMethodResultNoBench,
                        lotsOfAssertions.toTestMethod());
        assertNull(lotsOfAssertions.toFixtureDeclaration());
        assertEquals("\t\tMath.pow(4,3);\n",
                        lotsOfAssertions.toFixtureSetup());
    }

    /**
     * Test the case where we have a method invoker record
     * which we assign a name to (i.e. the user gets a
     * method result and then does a "Get" on the object to
     * put it on the object bench).
     */
    public void testMethodInvokerWithBench()
    {
        String testMethodResultWithBench =
            "\t\tjava.lang.Math on_bench = Math.pow(4,3);\n" +
            "\t\tassertNotNull(on_bench);\n" +
            "\t\tassertEquals(8, on_bench);\n" +
            "\t\tassertEquals(8, on_bench, 0.1);\n";

        // put it on the bench with name "on_bench" and with a
        // type that is its actual type
        lotsOfAssertions.setBenchName("on_bench", "java.lang.Math");

        assertEquals(testMethodResultWithBench,
                        lotsOfAssertions.toTestMethod());
        assertEquals("\tprivate java.lang.Math on_bench;\n",
                        lotsOfAssertions.toFixtureDeclaration());
        assertEquals("\t\ton_bench = Math.pow(4,3);\n",
                        lotsOfAssertions.toFixtureSetup());
    }

    /**
     * Test the case where we have a method invoker record
     * which we assign a name to (i.e. the user gets a
     * method result and then does a "Get" on the object to
     * put it on the object bench) but which needs a type cast
     * for the assignment statement.
     */
    public void testMethodInvokerWithBenchTypecast()
    {
        String testMethodResultWithBenchTypecast =
            "\t\tjava.lang.Math on_bench = Math.pow(4,3);\n" +
            "\t\tassertNotNull(on_bench);\n" +
            "\t\tassertEquals(8, on_bench);\n" +
            "\t\tassertEquals(8, on_bench, 0.1);\n";

        // put it on the bench with name "on_bench" and with a
        // type that is its actual type
        lotsOfAssertions.setBenchName("on_bench", "java.lang.Math");

        assertEquals(testMethodResultWithBenchTypecast,
                        lotsOfAssertions.toTestMethod());
        assertEquals("\tprivate java.lang.Math on_bench;\n",
                        lotsOfAssertions.toFixtureDeclaration());
        assertEquals("\t\ton_bench = Math.pow(4,3);\n",
                        lotsOfAssertions.toFixtureSetup());
    }

}
