package bluej.parser.ast;

/**
 * Write a description of the test class $CLASSNAME here.
 *
 * @author  (your name)
 * @version (a version number or a date)
 */
public class ExpressionTest extends junit.framework.TestCase
{
    /**
     * Insert fixtures here
     */

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
    public void testArithmeticPromotion()
    {
//        assertEquals(int.class, JavaParser.parseExpression("2+3;") );
//        assertEquals(double.class, JavaParser.parseExpression("2.0+3;") );
//        assertEquals(double.class, JavaParser.parseExpression("2.2+3.0f;") );
    }

    public void testCasting()
    {
//        assertEquals(String.class, JavaParser.parseExpression("(String)s;") );
//        assertEquals(null, JavaParser.parseExpression("System.out.flush();") );
    }

    public void testStaticMethodCall()
    {
//        assertEquals(javax.swing.border.Border.class, JavaParser.parseExpression("javax.swing.BorderFactory.createEmptyBorder();") );
    }

    public void testStaticVariable()
    {
//        assertEquals(java.io.PrintStream.class, JavaParser.parseExpression("System.out;") );   
//        assertEquals(java.io.PrintStream.class, JavaParser.parseExpression("java.lang.System.out;") );   
    }
    
    public void testNewInnerClass()
    {
//        assertEquals(javax.swing.Box.Filler.class, JavaParser.parseExpression("javax.swing.Box.new Filler();") );
    } 
}
