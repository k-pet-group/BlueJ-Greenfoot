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

import javax.swing.text.BadLocationException;

import junit.framework.TestCase;
import bluej.editor.moe.MoeSyntaxDocument;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.PackageResolver;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

public class IncrementalParseTest extends TestCase
{
    {
        InitConfig.init();
    }
    
    private TestEntityResolver resolver;
    
    @Override
    protected void setUp() throws Exception
    {
        resolver = new TestEntityResolver(new ClassLoaderResolver(this.getClass().getClassLoader()));
    }
    
    @Override
    protected void tearDown() throws Exception
    {
    }
    
    /**
     * Generate a compilation unit node based on some source code.
     */
    private MoeSyntaxDocument docForSource(String sourceCode, String pkg)
    {
        EntityResolver resolver = new PackageResolver(this.resolver, pkg);
        MoeSyntaxDocument document = new MoeSyntaxDocument(resolver);
        document.enableParser(true);
        try {
            document.insertString(0, sourceCode, null);
        }
        catch (BadLocationException ble) {}
        return document;
    }
    
    public void test1() throws Exception
    {
        String aSrc = "class A {\n" +   // 0 - 10
            "  void method() {\n" +     // 10 - 28 
            "  }\n" +                   // 28 - 32 
            "}\n";                      // 32 - 34
        
        MoeSyntaxDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);
        
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(33, nap.getSize());
        
        aDoc.remove(8, 1);  // remove the opening '{' of the class
        aDoc.flushReparseQueue();
        aDoc.insertString(8, "{", null);  // re-insert it
        aDoc.flushReparseQueue();
        
        // Now check that the structure is the same
        nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(33, nap.getSize());
    }

    public void test2() throws Exception
    {
        String aSrc = "class A {\n" +   // 0 - 10
            "  void method() {\n" +     // 10 - 28 
            "  }\n" +                   // 28 - 32 
            "}\n";                      // 32 - 34
        
        MoeSyntaxDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);
        
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(33, nap.getSize());
        
        aDoc.insertString(8, "impl", null);
        aDoc.flushReparseQueue();
        aDoc.insertString(12, "ements ", null);
        aDoc.flushReparseQueue();
        aDoc.insertString(19, "Runnable ", null);
        aDoc.flushReparseQueue();
        
        // Check that the structure is correct
        nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(53, nap.getSize());
    }

    
}
