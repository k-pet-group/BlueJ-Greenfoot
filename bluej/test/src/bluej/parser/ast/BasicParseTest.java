package bluej.parser.ast;

import java.io.*;
import java.net.URL;

import antlr.*;

/**
 * Run a whole directory of sample source files through our parser.
 *
 * @author  Andrew Patterson
 * @version (a version number or a date)
 */
public class BasicParseTest extends junit.framework.TestCase
{
    /**
     * Get a data or result file from our hidden stash..
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
    public void testNoParseExceptionsOnStandardCode()
        throws RecognitionException, TokenStreamException, FileNotFoundException
    {
        // this file came from some guys web page.. it just includes lots of
        // Java constructs
        new JavaAnalyzer(new BufferedReader(new FileReader(getFile("java_basic.dat"))));

        // these files came from the test suite accompanying antlr
        new JavaAnalyzer(new BufferedReader(new FileReader(getFile("A.dat"))));
        new JavaAnalyzer(new BufferedReader(new FileReader(getFile("B.dat"))));
        new JavaAnalyzer(new BufferedReader(new FileReader(getFile("C.dat"))));
        new JavaAnalyzer(new BufferedReader(new FileReader(getFile("D.dat"))));
        new JavaAnalyzer(new BufferedReader(new FileReader(getFile("E.dat"))));
    } 
}
