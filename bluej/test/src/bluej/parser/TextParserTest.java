package bluej.parser;

import junit.framework.TestCase;
import bluej.debugmgr.objectbench.ObjectBench;
import bluej.debugmgr.texteval.TextParser;

/**
 * Test that void results are handled correctly by the textpad parser.
 * 
 * @author Davin McCall
 * @version $Id: TextParserTest.java 3326 2005-02-28 21:50:06Z davmac $
 */
public class TextParserTest extends TestCase
{
    public void testVoidResult()
    {
        ObjectBench ob = new ObjectBench();
        TextParser tp = new TextParser(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("System.out.println(\"no comment\");");
        assertNull(r);
    }
    
    public void testArithmeticPromotion()
    {
        ObjectBench ob = new ObjectBench();
        TextParser tp = new TextParser(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("2+3");
        assertEquals("int", r);
        r = tp.parseCommand("2.0+3");
        assertEquals("double", r);
        r = tp.parseCommand("2.2+3.0f");
        assertEquals("double", r);
    }
    
    public void testCasting()
    {
        ObjectBench ob = new ObjectBench();
        TextParser tp = new TextParser(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("(String)s");
        assertEquals("java.lang.String", r);
    }

    public void testStaticMethodCall()
    {
        ObjectBench ob = new ObjectBench();
        TextParser tp = new TextParser(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("javax.swing.BorderFactory.createEmptyBorder()");
        assertEquals("javax.swing.border.Border", r);
    }

    public void testStaticVariable()
    {
        ObjectBench ob = new ObjectBench();
        TextParser tp = new TextParser(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("System.out");
        assertEquals("java.io.PrintStream", r);
        r = tp.parseCommand("java.lang.System.out");
        assertEquals("java.io.PrintStream", r);
    }
    
    public void testNewInnerClass()
    {
        ObjectBench ob = new ObjectBench();
        TextParser tp = new TextParser(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("javax.swing.Box.new Filler()");
        assertEquals("javax.swing.Box$Filler", r);
    }
    
    public void testCastToWildcard()
    {
        ObjectBench ob = new ObjectBench();
        TextParser tp = new TextParser(getClass().getClassLoader(), "", ob);
        String r = tp.parseCommand("(java.util.LinkedList<?>) new java.util.LinkedList<Thread>()");
        assertEquals("java.util.LinkedList<?>", r);
    }
}
