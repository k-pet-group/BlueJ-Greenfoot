package bluej.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import bluej.parser.ClassParser;

/**
 * Run a whole directory of sample source files through our parser.
 *
 * @author  Andrew Patterson
 * @version (a version number or a date)
 */
public class InternationalParseTest extends junit.framework.TestCase
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
        //i18n1data = ;
        //i18n1result = getFile("i18n3.res");
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
    public void testInternationalization()
        throws RecognitionException, TokenStreamException, FileNotFoundException
    {
        ClassParser.parse(getFile("escaped_unicode_string.dat"), null);
        ClassParser.parse(getFile("escaped_unicode_method.dat"), null);
    } 
}
