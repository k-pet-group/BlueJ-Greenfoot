package bluej.parser.ast;

import antlr.BaseAST;
import bluej.parser.*;
import bluej.parser.ast.gen.*;

class AnotherTest extends junit.framework.TestCase
{
    protected void setUp()
    {
        
    }
    
    protected void tearDown()
    {
    }

    public void testYYY()
    {
        
    }
}

/**
 * Test the unit testing parse code.
 *
 * @author  Andrew Patterson
 */
public class UnitTestAnalyzerTest extends junit.framework.TestCase
{
	private String testSrc =
"class IgnoreMe extends junit.framework.TestCase {"             + "\n" +
"    protected void testYYY() { }"                              + "\n" +
"}"                                                             + "\n" +
"public class TestSrc extends junit.framework.TestCase {"       + "\n" +
"    private int x = 55;"                                       + "\n" +
"    java.util.HashMap h = new HashMap(),"                      + "\n" +
"                      i,"                                      + "\n" +
"                      j = null;"                               + "\n" +
""                                                              + "\n" +
"    /**"                                                       + "\n" +
"     * Should be ignored because of the parameter"             + "\n" +
"     */"                                                       + "\n" +
"    protected void setUp(int a)"                               + "\n" +
"    {"                                                         + "\n" +
"        for (int i=0; i<10; i++) { ; }"                        + "\n" +
"    }"                                                         + "\n" +
""                                                              + "\n" +
"    protected void setUp()"                                    + "\n" +
"    {"                                                         + "\n" +
"        for (int i=0; i<10; i++) { ; }"                        + "\n" +
"    }"                                                         + "\n" +
""                                                              + "\n" +
"    // variables and method names are in a different scope"    + "\n" +
"    public String testXXX;"                                    + "\n" +
""                                                              + "\n" +
"    /**"                                                       + "\n" +
"     * Here is an attached comment"                            + "\n" +
"     */"                                                       + "\n" +
"    protected void testXXX()"                                  + "\n" +
"    {"                                                         + "\n" +
"        System.out.println(\"Hello\");"                        + "\n" +
"     }"                                                        + "\n" +
"}"                                                             + "\n";

    private BaseAST parsedAST;
    private LocatableAST firstClassAST;
    private LocatableAST openingBracketAST;
    private LocatableAST firstAfterOpeningBracketAST;
    private UnitTestAnalyzer uta;
    
    /**
     * Sets up the test fixture.
     *
     * Called before every test case method.
     */
    protected void setUp() throws Exception
    {
        parsedAST = (BaseAST) JavaParser.parseFile(new java.io.StringReader(testSrc));

        // operate on the first class defined in the source file.
        // this could be a mistaken assumption but for unit tests its
        // probably correct
        firstClassAST = (LocatableAST) parsedAST.getFirstChild();

        openingBracketAST = (LocatableAST) firstClassAST.getFirstChild();
        firstAfterOpeningBracketAST = (LocatableAST) firstClassAST.getFirstChild().getNextSibling();

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
        uta.getUnitTestClass();
        
        java.util.List variables = null;

       BaseAST childAST = (BaseAST) firstAfterOpeningBracketAST.getNextSibling();

       while(childAST != null) {
           if(childAST.getType() == UnitTestParserTokenTypes.OBJBLOCK) {
               variables = UnitTestAnalyzer.getVariableSourceSpans(childAST);
               break;
           }                         
           childAST = (BaseAST) childAST.getNextSibling();            
       }

       SourceSpan xSpan = (SourceSpan) variables.get(0);
       assertEquals(5, xSpan.getStartLine());
       assertEquals(5, xSpan.getStartColumn());
       assertEquals(5, xSpan.getEndLine());
       assertEquals(23, xSpan.getEndColumn());

       SourceSpan hashmapSpan = (SourceSpan) variables.get(1);
       assertEquals(6, hashmapSpan.getStartLine());
       assertEquals(5, hashmapSpan.getStartColumn());
       assertEquals(6, hashmapSpan.getEndLine());
       assertEquals(31, hashmapSpan.getEndColumn());

       SourceSpan testXXXSpan = (SourceSpan) variables.get(2);
       assertEquals(24, testXXXSpan.getStartLine());
       assertEquals(5, testXXXSpan.getStartColumn());
       assertEquals(24, testXXXSpan.getEndLine());
       assertEquals(26, testXXXSpan.getEndColumn());
    } 

	public void testFindingMethods()
	{
	   BaseAST childAST = (BaseAST) firstAfterOpeningBracketAST.getNextSibling();

	   while(childAST != null) {
	       if(childAST.getType() == UnitTestParserTokenTypes.OBJBLOCK) {
	           SourceSpan setUpSpan = UnitTestAnalyzer.getMethodBlockSourceSpan(childAST, "setUp");

	           assertEquals(19, setUpSpan.getStartLine());
	           assertEquals(5, setUpSpan.getStartColumn());
	           assertEquals(21, setUpSpan.getEndLine());
	           assertEquals(5, setUpSpan.getEndColumn());

	           SourceSpan testXXXSpan = UnitTestAnalyzer.getMethodBlockSourceSpan(childAST, "testXXX");
           
               assertEquals(30, testXXXSpan.getStartLine());
               assertEquals(5, testXXXSpan.getStartColumn());
               assertEquals(32, testXXXSpan.getEndLine());
               assertEquals(6, testXXXSpan.getEndColumn());
           
               break;
		   }                         
		   childAST = (BaseAST) childAST.getNextSibling();            
	   }
	} 
}
