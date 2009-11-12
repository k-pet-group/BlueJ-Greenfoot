/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.parser;

/**
 * Test the unit testing parse code.
 *
 * @author  Andrew Patterson
 */
public class UnitTestAnalyzerTest extends junit.framework.TestCase
{
	private String testSrc =
"class IgnoreMe extends junit.framework.TestCase {"             + "\n" + // 1
"    protected void testYYY() { }"                              + "\n" + // 2
"}"                                                             + "\n" + // 3
"public class TestSrc extends junit.framework.TestCase {"       + "\n" + // 4
"    private int x = 55;"                                       + "\n" + // 5
"    java.util.HashMap h = new HashMap(),"                      + "\n" + // 6
"                      i,"                                      + "\n" + // 7
"                      j = null;"                               + "\n" + // 8
""                                                              + "\n" + // 9
"    /**"                                                       + "\n" + // 10
"     * Should be ignored because of the parameter"             + "\n" + // 11
"     */"                                                       + "\n" + // 12 
"    protected void setUp(int a)"                               + "\n" + // 13
"    {"                                                         + "\n" + // 14
"        for (int i=0; i<10; i++) { ; }"                        + "\n" + // 15
"    }"                                                         + "\n" + // 16
""                                                              + "\n" + // 17
"    protected void setUp()"                                    + "\n" + // 18
"    {"                                                         + "\n" + // 19
"        for (int i=0; i<10; i++) { ; }"                        + "\n" + // 20
"    }"                                                         + "\n" + // 21
""                                                              + "\n" + // 22
"    // variables and method names are in a different scope"    + "\n" + // 23
"    public String testXXX;"                                    + "\n" + // 24
""                                                              + "\n" + // 25
"    /**"                                                       + "\n" + // 26
"     * Here is an attached comment"                            + "\n" + // 27
"     */"                                                       + "\n" + // 28
"    protected void testXXX()"                                  + "\n" + // 29
"    {"                                                         + "\n" + // 30
"        System.out.println(\"Hello\");"                        + "\n" + // 31
"     }"                                                        + "\n" + // 32
"}"                                                             + "\n";  // 33

    private UnitTestAnalyzer uta;
    
    /**
     * Sets up the test fixture.
     *
     * Called before every test case method.
     */
    protected void setUp() throws Exception
    {
        uta = new UnitTestAnalyzer(new java.io.StringReader(testSrc));
    }

    /**
     * Tears down the test fixture.
     *
     * Called after every test case method.
     */
    protected void tearDown()
    {
    }

    /**
     * A sample test case method
     */
    public void testFindingVariables()
    {
       java.util.List<SourceSpan> variables = uta.getFieldSpans();

       SourceSpan xSpan = (SourceSpan) variables.get(0);
       assertEquals(5, xSpan.getStartLine());
       assertEquals(5, xSpan.getStartColumn());
       assertEquals(5, xSpan.getEndLine());
       assertEquals(24, xSpan.getEndColumn());

       SourceSpan hashmapSpan = (SourceSpan) variables.get(1);
       assertEquals(6, hashmapSpan.getStartLine());
       assertEquals(5, hashmapSpan.getStartColumn());
       assertEquals(8, hashmapSpan.getEndLine());
       assertEquals(32, hashmapSpan.getEndColumn());

       SourceSpan testXXXSpan = (SourceSpan) variables.get(2);
       assertEquals(24, testXXXSpan.getStartLine());
       assertEquals(5, testXXXSpan.getStartColumn());
       assertEquals(24, testXXXSpan.getEndLine());
       assertEquals(27, testXXXSpan.getEndColumn());
    } 

    public void testFindingMethods()
    {
        SourceSpan setUpSpan = uta.getMethodBlockSpan("setUp");

        assertEquals(19, setUpSpan.getStartLine());
        assertEquals(5, setUpSpan.getStartColumn());
        assertEquals(21, setUpSpan.getEndLine());
        assertEquals(6, setUpSpan.getEndColumn());

        SourceSpan testXXXSpan = uta.getMethodBlockSpan("testXXX");

        assertEquals(30, testXXXSpan.getStartLine());
        assertEquals(5, testXXXSpan.getStartColumn());
        assertEquals(32, testXXXSpan.getEndLine());
        assertEquals(7, testXXXSpan.getEndColumn());
    }
    
    public void testMethodInsertion()
    {
        SourceLocation insertLocation = uta.getNewMethodInsertLocation();
        
        assertEquals(33, insertLocation.getLine());
        assertEquals(1, insertLocation.getColumn());
    }
    
    public void testFixtureInsertion()
    {
        SourceLocation insertLocation = uta.getFixtureInsertLocation();
        
        assertEquals(4, insertLocation.getLine());
        assertEquals(55, insertLocation.getColumn());
    }
}
