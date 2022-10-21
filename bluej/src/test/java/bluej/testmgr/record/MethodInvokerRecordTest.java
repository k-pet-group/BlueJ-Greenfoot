/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2013  Michael Kolling and John Rosenberg 
 
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
package bluej.testmgr.record;

import bluej.debugger.gentype.JavaType;
import bluej.utility.JavaUtils;

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
    @Override
    protected void setUp()
    {
        JavaType mathType = JavaUtils.genTypeFromClass(java.lang.Math.class);
        lotsOfAssertions = new MethodInvokerRecord(mathType,
                "Math.pow(4,3)", new String [] {"3", "4"});
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
    @Override
    protected void tearDown()
    {
    }

    /**
     * Disabling this test - I doubt that it is testing a real situation.
     */
    /*
    public void testMethodInvokerNoBench()
    {
        String testMethodResultNoBench =
            "\t\tassertNotNull(Math.pow(4,3));\n";
        assertEquals(testMethodResultNoBench,
                        lotsOfAssertions.toTestMethod());
        assertNull(lotsOfAssertions.toFixtureDeclaration());
        assertEquals("\t\tMath.pow(4,3);\n",
                        lotsOfAssertions.toFixtureSetup());
    }
    */

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
                        lotsOfAssertions.toTestMethod(null, "\t\t"));
        assertEquals("\tprivate java.lang.Math on_bench;\n",
                        lotsOfAssertions.toFixtureDeclaration("\t"));
        assertEquals("\t\ton_bench = Math.pow(4,3);\n",
                        lotsOfAssertions.toFixtureSetup("\t\t"));
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
                        lotsOfAssertions.toTestMethod(null, "\t\t"));
        assertEquals("\tprivate java.lang.Math on_bench;\n",
                        lotsOfAssertions.toFixtureDeclaration("\t"));
        assertEquals("\t\ton_bench = Math.pow(4,3);\n",
                        lotsOfAssertions.toFixtureSetup("\t\t"));
    }

}
