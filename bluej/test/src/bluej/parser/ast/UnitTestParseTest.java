package bluej.parser.ast;

import java.util.Iterator;

import antlr.BaseAST;

/**
 * Write a description of the test class $CLASSNAME here.
 *
 * @author  (your name)
 * @version (a version number or a date)
 */
public class UnitTestParseTest extends junit.framework.TestCase
{
    private String sampleSrc =
        "class ATest {\n" +
        "  private int x = 55;\n" +
        "  java.util.HashMap h,i,j = null;\n" +
        "  public String s;\n" +
        "}";
    
    /**
     * Sets up the test fixture.
     *
     * Called before every test case method.
     */
    protected void setUp()
    {

        
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
    public void testNewInnerClass()
    {
        try {
           BaseAST ast = (BaseAST) JavaParser.parseFile(new java.io.StringReader(sampleSrc));

           // operate on the first class defined in the source file.
           // this could be a mistaken assumption but for unit tests its
           // probably correct
           BaseAST firstClass = (BaseAST) ast.getFirstChild();

           java.util.List variables = null;
           java.util.List setup = null;
           LocatableAST openingBracket = null;
           LocatableAST methodInsert = null;

           openingBracket = (LocatableAST) firstClass.getFirstChild();
           methodInsert = (LocatableAST) firstClass.getFirstChild().getNextSibling();
            
           BaseAST childAST = (BaseAST) methodInsert.getNextSibling();

           while(childAST != null) {
               if(childAST.getType() == UnitTestParserTokenTypes.OBJBLOCK) {
                    
                   variables = UnitTestParser.getVariableSelections(childAST);
                   setup = UnitTestParser.getSetupMethodSelections(childAST);
                   break;
               }                         
               childAST = (BaseAST) childAST.getNextSibling();            
           }

           Iterator it = variables.iterator();
           while(it.hasNext()) {
               System.out.println(it.next());
           }

        }
       catch (Exception e) { e.printStackTrace(); }

    } 
}
