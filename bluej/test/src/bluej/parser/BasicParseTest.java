package bluej.parser;

import java.io.File;
import java.net.URL;

import bluej.parser.symtab.ClassInfo;

/**
 * Run a whole directory of sample source files through our parser.
 *
 * @author  Andrew Patterson
 */
public class BasicParseTest extends junit.framework.TestCase
{
    /**
     * Get a data or result file from our hidden stash..
     * NOTE: the stash of data files is in the ast/data directory.
     * This is because eventually, we want all parsing in bluej to
     * be done by the AST routines, and we can get rid of this
     * parser. So we share the data file until then.
     * 
     * @param name
     * @return
     */
    private File getFile(String name)
    {
        URL url = getClass().getResource("/bluej/parser/ast/data/" + name);
        
        if (url == null || url.getFile().equals(""))
            return null;
        else
            return new File(url.getFile());
    }
    
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
     * Lots of sample files, none of which should cause exceptions
     * in our parser.
     * 
     * @throws Exception
     */
    public void testNoParseExceptionsOnStandardCode()
        throws Exception
    {
        // this file came from some guys web page.. it just includes lots of
        // Java constructs
        ClassParser.parse(getFile("java_basic.dat"), null);

        // these files came from the test suite accompanying antlr
        ClassParser.parse(getFile("A.dat"),null);
        ClassParser.parse(getFile("B.dat"),null);
        ClassParser.parse(getFile("C.dat"),null);
        ClassParser.parse(getFile("D.dat"),null);
        ClassParser.parse(getFile("E.dat"),null);
    } 

    public void testValidClassInfo()
        throws Exception
    {
        ClassInfo info;
        
        info = bluej.parser.ClassParser.parse(getFile("AffinedTransformer.dat"));

        assertEquals("AffinedTransformer",info.getName());
        assertEquals("JFrame",info.getSuperclass());
        assertEquals("bluej.parser.ast.data",info.getPackage());

        assertEquals(6, info.getUsed().size());
    }
}
