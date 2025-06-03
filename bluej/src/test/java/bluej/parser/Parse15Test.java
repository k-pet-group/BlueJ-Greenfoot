/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016  Michael Kolling and John Rosenberg 
 
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

import java.io.File;
import java.io.StringReader;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;

import static bluej.utility.ResourceFileReader.getResourceFile;

/**
 * Run sample source file(s) containing Java 1.5 specific features
 * eg. generics, enums, static imports, foreach, varargs etc.
 *
 * @author  Bruce Quig
 */
public class Parse15Test extends junit.framework.TestCase
{

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
     * in our parser. Needs to be run under jdk1.5 or later.
     * 
     * @throws Exception
     */
    public void testNoParseExceptions()
        throws Exception
    {
        InfoParser.parse(getResourceFile(getClass(), "/bluej/parser/15_generic.dat"));
    }
    
    public void testSelections()
        throws Exception
    {
        ClassInfo info = InfoParser.parse(getResourceFile(getClass(), "/bluej/parser/generic_selections.dat"));

//        Selection testSel = info.getTypeParametersSelection();
//        assertEquals(3, testSel.getLine());
//        assertEquals(19, testSel.getColumn());
//        assertEquals(3, testSel.getEndLine());
//        assertEquals(40, testSel.getEndColumn());
        
        Selection testSel = info.getSuperReplaceSelection();
        assertEquals(3, testSel.getLine());
        assertEquals(49, testSel.getColumn());
        assertEquals(4, testSel.getEndLine());
        assertEquals(31, testSel.getEndColumn());
        
        List<Selection> l = info.getInterfaceSelections();
        assertEquals(4, l.size());
        // "implements"  "List<Thread>"  ","  "GenInt<U>"
        Iterator<Selection> i = l.iterator();
        i.next();
        
        testSel = (Selection) i.next();
        assertEquals(5, testSel.getLine());
        assertEquals(16, testSel.getColumn());
        assertEquals(5, testSel.getEndLine());
        assertEquals(28, testSel.getEndColumn());
        
        i.next();
        testSel = (Selection) i.next();
        assertEquals(5, testSel.getLine());
        assertEquals(30, testSel.getColumn());
        assertEquals(5, testSel.getEndLine());
        assertEquals(39, testSel.getEndColumn());
    }
    
    public void testStaticImport()
    {
        boolean success = true;
        try {
            InfoParser.parse(new StringReader(
                    "import static java.awt.Color.BLACK;\n" +
                    "class A { }"),
                    null, null);
        }
        catch (Exception e) {
            success = false;
        }
        assertTrue(success);
    }
}
