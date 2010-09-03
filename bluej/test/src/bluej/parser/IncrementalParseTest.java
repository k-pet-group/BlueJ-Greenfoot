/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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

import java.util.List;

import javax.swing.text.BadLocationException;

import junit.framework.TestCase;
import bluej.editor.moe.MoeSyntaxDocument;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageResolver;
import bluej.parser.entity.TypeEntity;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.ParsedTypeNode;
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

    public void test3() throws Exception
    {
        String aSrc = "class A {\n" +   // 0 - 10
            "}\n";                      // 10 - 12
        
        MoeSyntaxDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);
        
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(11, nap.getSize());
        
        aDoc.insertString(10, "\n", null);
        aDoc.flushReparseQueue();
        aDoc.remove(10, 1);
        aDoc.flushReparseQueue();
        
        // Check that the structure is correct
        nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(11, nap.getSize());
    }
    
    public void test4() throws Exception
    {
        String aSrc = "class A {\n" +         // 0 - 10
            "  public void someFunc() {\n" +  // 10 - 37
            "    if (true) {\n" +             // 37 - 53 
            "    }\n" +                       // 53 - 59
            "  \n" +                          // 59 - 62  
            "  }\n" +                         // 62 - 66
            "}\n";                            // 66 - 68

        MoeSyntaxDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);

        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(67, nap.getSize());

        // Insert "else" clause, length 13
        aDoc.insertString(59, "    else { }\n", null);
        aDoc.flushReparseQueue();

        // Check that the structure is correct
        nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(80, nap.getSize());
        
        // Class inner
        nap = nap.getNode().findNodeAt(9, nap.getPosition());
        assertNotNull(nap);
        assertEquals(9, nap.getPosition());
        assertEquals(57+13, nap.getSize());

        // Method
        nap = nap.getNode().findNodeAt(12, nap.getPosition());
        assertNotNull(nap);
        assertEquals(12, nap.getPosition());
        assertEquals(53+13, nap.getSize());
        
        // Method inner
        nap = nap.getNode().findNodeAt(36, nap.getPosition());
        assertNotNull(nap);
        assertEquals(36, nap.getPosition());
        assertEquals(28+13, nap.getSize());
        
        // If/else
        nap = nap.getNode().findNodeAt(41, nap.getPosition());
        assertNotNull(nap);
        assertEquals(41, nap.getPosition());
        assertEquals(17+13, nap.getSize());
    }

    public void test5() throws Exception
    {
        // A class with an extra closing '}':
        String aSrc = "class A {\n" +   // 0 - 10
            "}\n";                      // 10 - 12
        
        MoeSyntaxDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);
        
        // Class should extend from 0 - 11
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(11, nap.getSize());
        
        // Now insert a new '}' which should terminate the class:
        aDoc.insertString(10, "}\n", null);
        aDoc.flushReparseQueue();
        
        nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(11, nap.getSize());
        
        // Now remove the first '}'
        aDoc.remove(10, 1);
        aDoc.flushReparseQueue();
        
        // Check that the structure is correct
        nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(12, nap.getSize());
    }
    
    public void test6() throws Exception
    {
        // A class with an extra closing '}':
        String aSrc = "class A {\n" +   // 0 - 10
            "\n" +                      // 10 - 11
            "}\n";                      // 11 - 13
        
        MoeSyntaxDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);

        // Class should extend from 0 - 12
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(12, nap.getSize());

        // inner
        nap = nap.getNode().findNodeAt(9, nap.getPosition());
        assertNotNull(nap);
        assertEquals(9, nap.getPosition());
        assertEquals(11, nap.getEnd());
     
        // Now insert a new '}' and then remove it again
        aDoc.insertString(10, "}", null);
        aDoc.flushReparseQueue();
        aDoc.remove(10, 1);
        
        aNode = aDoc.getParser();
        nap = aNode.findNodeAt(0, 0);

        // Class should extend from 0 - 12
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(12, nap.getSize());

        // inner
        nap = nap.getNode().findNodeAt(9, nap.getPosition());
        assertNotNull(nap);
        assertEquals(9, nap.getPosition());
        assertEquals(11, nap.getEnd());
    }
    
    public void test7() throws Exception
    {
        String aSrc = "class A {\n" +         // 0 - 10
            "  class B {\n" +                 // 10 - 22 
            "  \n" +                          // 22 - 25 
            "  }\n" +                         // 25 - 29
            "}\n";                            // 29 - 31

        MoeSyntaxDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();

        // A Outer
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(30, nap.getSize());
        
        // A Inner
        nap = nap.getNode().findNodeAt(9, nap.getPosition());
        assertNotNull(nap);
        assertEquals(9, nap.getPosition());
        assertEquals(29, nap.getEnd());
        
        // B Outer
        nap = nap.getNode().findNodeAt(12, nap.getPosition());
        assertNotNull(nap);
        assertEquals(12, nap.getPosition());
        assertEquals(28, nap.getEnd());
        
        // B Inner
        nap = nap.getNode().findNodeAt(21, nap.getPosition());
        assertNotNull(nap);
        assertEquals(21, nap.getPosition());
        assertEquals(27, nap.getEnd());
        
        // Delete the '} from B
        aDoc.remove(27, 1);
        
        aNode = aDoc.getParser();

        // Class B should now soak up A's '}'. As a result A outer and inner
        // should extend to end-of-file.
        
        // A Outer
        nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(30, nap.getSize());

        // A Inner
        nap = nap.getNode().findNodeAt(9, nap.getPosition());
        assertNotNull(nap);
        assertEquals(9, nap.getPosition());
        assertEquals(30, nap.getEnd());
        
        // Re-insert '}'
        aDoc.insertString(27, "}", null);
        
        aNode = aDoc.getParser();
        nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(30, nap.getSize());
        
        // A Inner
        nap = nap.getNode().findNodeAt(9, nap.getPosition());
        assertNotNull(nap);
        assertEquals(9, nap.getPosition());
        assertEquals(29, nap.getEnd());
        
        // B Outer
        nap = nap.getNode().findNodeAt(12, nap.getPosition());
        assertNotNull(nap);
        assertEquals(12, nap.getPosition());
        assertEquals(28, nap.getEnd());
        
        // B Inner
        nap = nap.getNode().findNodeAt(21, nap.getPosition());
        assertNotNull(nap);
        assertEquals(21, nap.getPosition());
        assertEquals(27, nap.getEnd());
    }
    
    public void test8() throws Exception
    {
        String aSrc = "class A {\n" +         // 0 - 10
            "  public void someFunc() {\n" +  // 10 - 37
            "    if (true) {\n" +             // 37 - 53 
            "    }\n" +                       // 53 - 59
            "  \n" +                          // 59 - 62  
            "  }\n" +                         // 62 - 66
            "}\n";                            // 66 - 68

        MoeSyntaxDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);

        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(67, nap.getSize());

        // First remove "if (true) {"
        aDoc.remove(41, 11);
        aDoc.flushReparseQueue();
        
        // Now remove the "}" from the old if statement
        aDoc.remove(57 - 11, 1);
        aDoc.flushReparseQueue();
        
        aNode = aDoc.getParser();
        nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(67 - 12, nap.getSize());
    }
    
    public void test9() throws Exception
    {
        String aSrc = "class A {\n" +         // 0 - 10
            "  public void someFunc() {\n" +  // 10 - 37
            "  \n" +                          // 37 - 40  
            "  }\n" +                         // 40 - 44
            "}\n";                            // 44 - 46

        MoeSyntaxDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);

        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(45, nap.getSize());
        
        // Insert "if() {"
        aDoc.insertString(39, "if(true) {", null);
        
        aNode = aDoc.getParser();
        
        // Typedef node
        nap = aNode.findNodeAt(39, 0);
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(56, nap.getSize());
        
        // Typedef inner
        nap = nap.getNode().findNodeAt(39, nap.getPosition());
        assertNotNull(nap);
        assertEquals(9, nap.getPosition());
        assertEquals(56, nap.getEnd());
        
        // Method outer
        nap = nap.getNode().findNodeAt(39, nap.getPosition());
        assertNotNull(nap);
        assertEquals(12, nap.getPosition());
        assertEquals(55, nap.getEnd());

        // Method inner
        nap = nap.getNode().findNodeAt(39, nap.getPosition());
        assertNotNull(nap);
        assertEquals(36, nap.getPosition());
        assertEquals(54, nap.getEnd());
        
        // Remove "if() {" etc, re-insert, re-check
        aDoc.remove(39, 10);
        aDoc.flushReparseQueue();
        aDoc.insertString(39, "if(true) {", null);
        
        aNode = aDoc.getParser();
        
        // Typedef node
        nap = aNode.findNodeAt(39, 0);
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(56, nap.getSize());
        
        // Typedef inner
        nap = nap.getNode().findNodeAt(39, nap.getPosition());
        assertNotNull(nap);
        assertEquals(9, nap.getPosition());
        assertEquals(56, nap.getEnd());
        
        // Method outer
        nap = nap.getNode().findNodeAt(39, nap.getPosition());
        assertNotNull(nap);
        assertEquals(12, nap.getPosition());
        assertEquals(55, nap.getEnd());

        // Method inner
        nap = nap.getNode().findNodeAt(39, nap.getPosition());
        assertNotNull(nap);
        assertEquals(36, nap.getPosition());
        assertEquals(54, nap.getEnd());
    }

    public void test10() throws Exception
    {
        String aSrc = 
            "/** A comment */\n" +  // 0 - 17
            "class A {\n" +         // 17 - 27
            "}\n";                  // 27 - 29

        MoeSyntaxDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);

        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(28, nap.getSize());
        
        // Change the multi-line comment to a single line
        aDoc.insertString(0, "/", null);
        
        aNode = aDoc.getParser();
        nap = aNode.findNodeAt(18, 0);
        assertNotNull(nap);
        assertEquals(18, nap.getPosition());
        assertEquals(11, nap.getSize());
        
        // Now back to a single line
        aDoc.remove(0, 1);

        aNode = aDoc.getParser();
        nap = aNode.findNodeAt(18, 0);
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(28, nap.getSize());
    }
    
    public void test11() throws Exception
    {
        // Regression test for bug #276
        String aSrc = 
            "public class ArrayWrapper\n" +  // 0 - 26 
            "{\n" +                          // 26 - 28 
            "  private int x;\n" +           // 28 - 45 
            "  /**\n" +                      // 45 - 51 
            "   * Constructor for objects of class ArrayWrapper\n" +  // 51 - 102 
            "   */\n" +                      // 102 - 108 
            "  public ArrayWrapper()\n" +    // 108 - 132
            "  {\n" +                        // 132 - 136 
            "    x = 0;\n" +                 // 136 - 147
            "  }\n" +                        // 147 - 151
            "}\n";                           // 151 - 153

        MoeSyntaxDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);

        // nap is class node, ends just after '}' at 152
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(152, nap.getSize());
        
        // class inner node
        nap = nap.getNode().findNodeAt(27, nap.getPosition());
        assertNotNull(nap);
        assertEquals(27, nap.getPosition());
        assertEquals(151, nap.getEnd());
        
        // First in the inner - field node
        nap = nap.getNode().findNodeAtOrAfter(27, 27);
        assertNotNull(nap);
        assertEquals(30, nap.getPosition());
        assertEquals(44, nap.getEnd());
        
        // next - method node (comment is inside it)
        nap = nap.nextSibling();
        assertNotNull(nap);
        assertEquals(47, nap.getPosition());
        assertEquals(150, nap.getEnd());
        
        // Look inside the method node - see comment
        nap = nap.getNode().findNodeAtOrAfter(47, 47);
        assertNotNull(nap);
        assertEquals(47, nap.getPosition());
        assertEquals(107, nap.getEnd());
        
        nap = nap.nextSibling(); // method inner
        assertNotNull(nap);
        assertEquals(135, nap.getPosition());
        assertEquals(149, nap.getEnd());

        int removeSize = 107 - 42;
        aDoc.remove(42, removeSize); // remove selection:
        // from just before "x" in "private int x;" to the end of
        // the comment.
        
        //aNode = aDoc.getParser();
        nap = aNode.findNodeAt(0, 0);

        // nap is class node
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(152 - removeSize, nap.getSize());

        // class inner node
        nap = nap.getNode().findNodeAt(27, nap.getPosition());
        assertNotNull(nap);
        assertEquals(27, nap.getPosition());
        assertEquals(151 - removeSize, nap.getEnd());

        // field
        nap = nap.getNode().findNodeAtOrAfter(27, 27);
        assertNotNull(nap);
        assertEquals(30, nap.getPosition());
        assertEquals(42, nap.getEnd());
        NodeAndPosition<ParsedNode> nnap = nap.nextSibling();
        
        // The field following was removed, or is at a suitable place
        assertTrue(nnap == null || nnap.getPosition() >= nap.getEnd());
    }
    
    public void testChangeSuper() throws Exception
    {
        String aSrc = "class A {\n" +         // 0 - 10
            "  public void someFunc() {\n" +  // 10 - 37
            "  \n" +                          // 37 - 40  
            "  }\n" +                         // 40 - 44
            "}\n";                            // 44 - 46

        MoeSyntaxDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);

        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(45, nap.getSize());
        
        // Typedef node
        nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        
        ParsedNode pn = nap.getNode();
        List<JavaEntity> etypes = ((ParsedTypeNode) pn).getExtendedTypes();
        assertTrue(etypes.isEmpty());
        
        // Now make it extend String
        aDoc.insertString(7, " extends String", null);
        aNode = aDoc.getParser();
        
        nap = aNode.findNodeAt(0,0);
        assertNotNull(nap);
        
        pn = nap.getNode();
        etypes = ((ParsedTypeNode) pn).getExtendedTypes();
        assertEquals(1, etypes.size());
        
        TypeEntity tent = etypes.get(0).resolveAsType();
        assertNotNull(tent);
        assertEquals("java.lang.String", tent.getType().toString());
        
        // Now make it extend Object
        aDoc.remove(16, 6);  // "String"
        aDoc.insertString(16, "Object", null);
        aNode = aDoc.getParser();
        
        nap = aNode.findNodeAt(0,0);
        assertNotNull(nap);
        
        pn = nap.getNode();
        etypes = ((ParsedTypeNode) pn).getExtendedTypes();
        assertEquals(1, etypes.size());
        
        tent = etypes.get(0).resolveAsType();
        assertNotNull(tent);
        assertEquals("java.lang.Object", tent.getType().toString());
    }

}
