package bluej.parser.ast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;

import antlr.RecognitionException;
import antlr.TokenStreamException;

/**
 * Run sample source file(s) containing Java 1.5 specific features
 * eg. generics, enums, static imports, foreach, varargs etc.
 *
 * @author  Bruce Quig
 */
public class Parse15Test extends junit.framework.TestCase
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
     * @throws RecognitionException
     * @throws TokenStreamException
     * @throws FileNotFoundException
     */
    public void testNoParseExceptions()
        throws RecognitionException, TokenStreamException, FileNotFoundException
    {
        // this file came from some guys web page.. it just includes lots of
        // Java constructs
        new JavaAnalyzer(new BufferedReader(new FileReader(getFile("15_generic.dat"))));
    } 

//    public void testValidClassInfo()
//        throws Exception
//    {
//        ClassInfo info;
//        File file = getFile("AffinedTransformer.dat");
//        info = bluej.parser.ClassParser.parse(file);
//
//        assertEquals("AffinedTransformer",info.getName());
//        assertEquals("JFrame",info.getSuperclass());
//        assertEquals("bluej.parser.ast.data",info.getPackage());
//
//        assertEquals(6, info.getUsed().size());
//    }
}
