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
import java.io.FileNotFoundException;
import java.net.URL;

import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.symtab.ClassInfo;
import bluej.parser.symtab.Selection;

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
        throws FileNotFoundException
    {
        assertNotNull(InfoParser.parse(getFile("escaped_unicode_string.dat")));
        
        ClassInfo info = InfoParser.parse(getFile("escaped_unicode_method.dat"),
                new ClassLoaderResolver(this.getClass().getClassLoader()));
        
        // Superclass name is Error (encoded)
        assertEquals("java.lang.Error", info.getSuperclass());
        
        // The selection should be 12 characters long (2 * 6)
        Selection testSel = info.getSuperReplaceSelection();
        assertEquals(48, testSel.getColumn());
        assertEquals(58, testSel.getEndColumn());
    } 
}
