package bluej.parser;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;

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
        List references = new ArrayList();
        references.add("Insets");
        references.add("Color");
        references.add("Rectangle");
        references.add("Graphics");
        references.add("Graphics2D");
        references.add("Runnable");
        references.add("Exception");
        references.add("Dummy1");
        references.add("Dummy2");
        
        File file = getFile("AffinedTransformer.dat");
        ClassInfo info = ClassParser.parse(file, references);

        assertEquals("AffinedTransformer",info.getName());
        assertEquals("JFrame",info.getSuperclass());
        assertEquals("bluej.parser.ast.data",info.getPackage());

        assertEquals(7, info.getUsed().size());
        
        // Check package selections
        Selection testSel = info.getPackageNameSelection();
        assertEquals(1, testSel.getLine());
        assertEquals(9, testSel.getColumn());
        assertEquals(1, testSel.getEndLine());
        assertEquals(30, testSel.getEndColumn());
        
        testSel = info.getPackageSemiSelection();
        assertEquals(1, testSel.getLine());
        assertEquals(30, testSel.getColumn());
        assertEquals(1, testSel.getEndLine());
        assertEquals(31, testSel.getEndColumn());
        
        testSel = info.getPackageStatementSelection();
        assertEquals(1, testSel.getLine());
        assertEquals(1, testSel.getColumn());
        assertEquals(1, testSel.getEndLine());
        assertEquals(8, testSel.getEndColumn());
        
        // AffinedTransformer already extends JFrame
        Selection extendsInsert = info.getExtendsInsertSelection();
        assertNull(extendsInsert);
        
        // No type parameters
        List l = info.getTypeParameterTexts();
        if (l != null)
            assertEquals(0, l.size());
        testSel = info.getTypeParametersSelection();
        assertNull(testSel);
        
        // Implements insert
        Selection implementsInsert = info.getImplementsInsertSelection();
        assertEquals(47, implementsInsert.getEndColumn());
        assertEquals(47, implementsInsert.getColumn());
        assertEquals(6, implementsInsert.getEndLine());
        assertEquals(6, implementsInsert.getLine());

        Selection superReplace = info.getSuperReplaceSelection();
        assertEquals(6, superReplace.getLine());
        assertEquals(41, superReplace.getColumn());
        assertEquals(6, superReplace.getEndLine());
        assertEquals(47, superReplace.getEndColumn());
        
        /*
         * Second file - no superclass, multiple interfaces 
         */
        
        file = getFile("multi_interface.dat");
        info = ClassParser.parse(file);
        
        extendsInsert = info.getExtendsInsertSelection();
        assertEquals(10, extendsInsert.getEndColumn());
        assertEquals(10, extendsInsert.getColumn());
        assertEquals(1, extendsInsert.getEndLine());
        assertEquals(1, extendsInsert.getLine());
        
        // the implements insert selection should be just beyond the
        // end of the last implemented interface
        implementsInsert = info.getImplementsInsertSelection();
        assertEquals(32, implementsInsert.getEndColumn());
        assertEquals(32, implementsInsert.getColumn());
        assertEquals(1, implementsInsert.getEndLine());
        assertEquals(1, implementsInsert.getLine());
        
        // the interface selections: "implements" "AA" "," "BB" "," "CC"
        List interfaceSels = info.getInterfaceSelections();
        assertEquals(6, interfaceSels.size());
        Iterator i = interfaceSels.iterator();
        
        // "implements"
        Selection interfaceSel = (Selection) i.next();
        assertEquals(1, interfaceSel.getLine());
        assertEquals(11, interfaceSel.getColumn());
        assertEquals(1, interfaceSel.getEndLine());
        assertEquals(21, interfaceSel.getEndColumn());
        
        // "AA"
        interfaceSel = (Selection) i.next();
        assertEquals(1, interfaceSel.getLine());
        assertEquals(22, interfaceSel.getColumn());
        assertEquals(1, interfaceSel.getEndLine());
        assertEquals(24, interfaceSel.getEndColumn());

        // ", "
        interfaceSel = (Selection) i.next();
        assertEquals(1, interfaceSel.getLine());
        assertEquals(24, interfaceSel.getColumn());
        assertEquals(1, interfaceSel.getEndLine());
        assertEquals(26, interfaceSel.getEndColumn());

        // "BB"
        interfaceSel = (Selection) i.next();
        assertEquals(1, interfaceSel.getLine());
        assertEquals(26, interfaceSel.getColumn());
        assertEquals(1, interfaceSel.getEndLine());
        assertEquals(28, interfaceSel.getEndColumn());

        // ", "
        interfaceSel = (Selection) i.next();
        assertEquals(1, interfaceSel.getLine());
        assertEquals(28, interfaceSel.getColumn());
        assertEquals(1, interfaceSel.getEndLine());
        assertEquals(30, interfaceSel.getEndColumn());

        // "CC"
        interfaceSel = (Selection) i.next();
        assertEquals(1, interfaceSel.getLine());
        assertEquals(30, interfaceSel.getColumn());
        assertEquals(1, interfaceSel.getEndLine());
        assertEquals(32, interfaceSel.getEndColumn());
        
    }
}
