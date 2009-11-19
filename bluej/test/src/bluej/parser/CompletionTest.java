/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
import java.util.Properties;

import javax.swing.text.BadLocationException;

import junit.framework.TestCase;
import bluej.Boot;
import bluej.Config;
import bluej.editor.moe.MoeSyntaxDocument;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.nodes.ParsedCUNode;

public class CompletionTest extends TestCase
{
    {
        File bluejLibDir = Boot.getBluejLibDir();
        Config.initialise(bluejLibDir, new Properties(), false);
    }
    
    @Override
    protected void setUp() throws Exception
    {
    }
    
    @Override
    protected void tearDown() throws Exception
    {
    }
    
    public void test1()
    {
        String aClassSrc = "class A {" +
        "  public int f = 0;" +
        "}";
        
        MoeSyntaxDocument document = new MoeSyntaxDocument();
        try {
            document.insertString(0, aClassSrc, null);
        }
        catch (BadLocationException ble) {}
        ParsedCUNode aNode = new ParsedCUNode(document);
        
        TestEntityResolver resolver = new TestEntityResolver(new ClassLoaderResolver(this.getClass().getClassLoader()));
        resolver.addCompilationUnit("", aNode);
        
        JavaEntity entity = resolver.resolveValueEntity("A", "B");
        entity = entity.getSubentity("f");
        assertNotNull(entity);
        entity = entity.resolveAsValue();
        assertNotNull(entity);
        assertEquals("int", entity.getType().toString());
    }
    
    // Test that multiple fields defined in a single statement are handled correctly,
    // particularly if one in the middle is assigned a complex expression involving an
    // anonymous inner class
    
    // Test that forward references behave the same way as in Java
    // - field definitions may not forward reference other fields in the same class
    // - variables cannot be forward referenced
}
