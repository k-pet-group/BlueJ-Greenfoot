/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2014,2019  Michael Kolling and John Rosenberg 
 
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

import bluej.JavaFXThreadingRule;
import bluej.parser.entity.ClassLoaderResolver;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.PackageResolver;
import bluej.parser.entity.TypeEntity;
import bluej.parser.entity.ValueEntity;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.ParsedTypeNode;
import bluej.parser.nodes.TypeInnerNode;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IncrementalParseTest
{
    @Rule
    public JavaFXThreadingRule javafxRule = new JavaFXThreadingRule();

    @BeforeClass
    public static void initConfig()
    {
        InitConfig.init();
    }
    
    private TestEntityResolver resolver;
    
    @Before
    public void setUp() throws Exception
    {
        resolver = new TestEntityResolver(new ClassLoaderResolver(this.getClass().getClassLoader()));
    }
    
    /**
     * Generate a compilation unit node based on some source code.
     */
    private TestableDocument docForSource(String sourceCode, String pkg)
    {
        EntityResolver resolver = new PackageResolver(this.resolver, pkg);
        TestableDocument document = new TestableDocument(resolver);
        document.enableParser(true);
        document.insertString(0, sourceCode);
        return document;
    }
    
    @Test
    public void test1() throws Exception
    {
        String aSrc = "class A {\n" +   // 0 - 10
            "  void method() {\n" +     // 10 - 28 
            "  }\n" +                   // 28 - 32 
            "}\n";                      // 32 - 34
        
        TestableDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);
        
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(33, nap.getSize());
        
        aDoc.remove(8, 1);  // remove the opening '{' of the class
        aDoc.insertString(8, "{");  // re-insert it
        
        // Now check that the structure is the same
        nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(33, nap.getSize());
    }

    @Test
    public void test2() throws Exception
    {
        String aSrc = "class A {\n" +   // 0 - 10
            "  void method() {\n" +     // 10 - 28 
            "  }\n" +                   // 28 - 32 
            "}\n";                      // 32 - 34
        
        TestableDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);
        
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(33, nap.getSize());
        
        aDoc.insertString(8, "impl");
        aDoc.insertString(12, "ements ");
        aDoc.insertString(19, "Runnable ");
        
        // Check that the structure is correct
        nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(53, nap.getSize());
    }

    @Test
    public void test3() throws Exception
    {
        String aSrc = "class A {\n" +   // 0 - 10
            "}\n";                      // 10 - 12
        
        TestableDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);
        
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(11, nap.getSize());
        
        aDoc.insertString(10, "\n");
        aDoc.remove(10, 1);
        
        // Check that the structure is correct
        nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(11, nap.getSize());
    }

    @Test
    public void test4() throws Exception
    {
        String aSrc = "class A {\n" +         // 0 - 10
            "  public void someFunc() {\n" +  // 10 - 37
            "    if (true) {\n" +             // 37 - 53 
            "    }\n" +                       // 53 - 59
            "  \n" +                          // 59 - 62  
            "  }\n" +                         // 62 - 66
            "}\n";                            // 66 - 68

        TestableDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);

        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(67, nap.getSize());

        // Insert "else" clause, length 13
        aDoc.insertString(59, "    else { }\n");

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

    @Test
    public void test5() throws Exception
    {
        // A class with an extra closing '}':
        String aSrc = "class A {\n" +   // 0 - 10
            "}\n";                      // 10 - 12
        
        TestableDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);
        
        // Class should extend from 0 - 11
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(11, nap.getSize());
        
        // Now insert a new '}' which should terminate the class:
        aDoc.insertString(10, "}\n");
        
        nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(11, nap.getSize());
        
        // Now remove the first '}'
        aDoc.remove(10, 1);
        
        // Check that the structure is correct
        nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(12, nap.getSize());
    }

    @Test
    public void test6() throws Exception
    {
        // A class with an extra closing '}':
        String aSrc = "class A {\n" +   // 0 - 10
            "\n" +                      // 10 - 11
            "}\n";                      // 11 - 13
        
        TestableDocument aDoc = docForSource(aSrc, "");
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
        aDoc.insertString(10, "}");
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

    @Test
    public void test7() throws Exception
    {
        String aSrc = "class A {\n" +         // 0 - 10
            "  class B {\n" +                 // 10 - 22 
            "  \n" +                          // 22 - 25 
            "  }\n" +                         // 25 - 29
            "}\n";                            // 29 - 31

        TestableDocument aDoc = docForSource(aSrc, "");
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
        aDoc.insertString(27, "}");
        
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

    @Test
    public void test8() throws Exception
    {
        String aSrc = "class A {\n" +         // 0 - 10
            "  public void someFunc() {\n" +  // 10 - 37
            "    if (true) {\n" +             // 37 - 53 
            "    }\n" +                       // 53 - 59
            "  \n" +                          // 59 - 62  
            "  }\n" +                         // 62 - 66
            "}\n";                            // 66 - 68

        TestableDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);

        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(67, nap.getSize());

        // First remove "if (true) {"
        aDoc.remove(41, 11);
        
        // Now remove the "}" from the old if statement
        aDoc.remove(57 - 11, 1);
        
        aNode = aDoc.getParser();
        nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(67 - 12, nap.getSize());
    }

    @Test
    public void test9() throws Exception
    {
        String aSrc = "class A {\n" +         // 0 - 10
            "  public void someFunc() {\n" +  // 10 - 37
            "  \n" +                          // 37 - 40  
            "  }\n" +                         // 40 - 44
            "}\n";                            // 44 - 46

        TestableDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);

        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(45, nap.getSize());
        
        // Insert "if() {"
        aDoc.insertString(39, "if(true) {");
        
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
        aDoc.insertString(39, "if(true) {");
        
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

    @Test
    public void test10() throws Exception
    {
        String aSrc = 
            "/** A comment */\n" +  // 0 - 17
            "class A {\n" +         // 17 - 27
            "}\n";                  // 27 - 29

        TestableDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);

        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(28, nap.getSize());
        
        // Change the multi-line comment to a single line
        aDoc.insertString(0, "/");
        
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

    @Test
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

        TestableDocument aDoc = docForSource(aSrc, "");
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
        aDoc.parsingSuspended = true;
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

    @Test
    public void test12() throws Exception
    {
        String aSrc = "class A {\n" +         // 0 - 10
            "    public int x() { }\n" +      // 10 - 33 
            "}\n";                            // 33 - 35
        
        TestableDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);

        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(34, nap.getSize());
        
        nap = nap.getNode().findNodeAt(9, nap.getPosition()); // class inner
        assertNotNull(nap);
        assertEquals(9, nap.getPosition());
        assertEquals(24, nap.getSize());
        
        nap = nap.getNode().findNodeAt(14, nap.getPosition()); // method
        assertNotNull(nap);
        assertEquals(14, nap.getPosition());
        assertEquals(18, nap.getSize());
        
        // Now remove the newline before the class' closing '}'
        aDoc.remove(32, 1);
        
        aNode = aDoc.getParser();
        nap = aNode.findNodeAt(0, 0);
        
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(33, nap.getSize());
        
        nap = nap.getNode().findNodeAt(9, nap.getPosition()); // class inner
        assertNotNull(nap);
        assertEquals(9, nap.getPosition());
        assertEquals(23, nap.getSize());
        
        nap = nap.getNode().findNodeAt(14, nap.getPosition()); // method
        assertNotNull(nap);
        assertEquals(14, nap.getPosition());
        assertEquals(18, nap.getSize());
        
        // Now re-insert the newline
        aDoc.insertString(32, "\n");

        aNode = aDoc.getParser();
        nap = aNode.findNodeAt(0, 0);
        
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(34, nap.getSize());
        
        nap = nap.getNode().findNodeAt(9, nap.getPosition()); // class inner
        assertNotNull(nap);
        assertEquals(9, nap.getPosition());
        assertEquals(24, nap.getSize());
        
        nap = nap.getNode().findNodeAt(14, nap.getPosition()); // method
        assertNotNull(nap);
        assertEquals(14, nap.getPosition());
        assertEquals(18, nap.getSize());
        
        // Now remove the newline before the class' closing '}'
        aDoc.remove(32, 1);
        
        aNode = aDoc.getParser();
        nap = aNode.findNodeAt(0, 0);
        
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(33, nap.getSize());
        
        nap = nap.getNode().findNodeAt(9, nap.getPosition()); // class inner
        assertNotNull(nap);
        assertEquals(9, nap.getPosition());
        assertEquals(23, nap.getSize());
        
        nap = nap.getNode().findNodeAt(14, nap.getPosition()); // method
        assertNotNull(nap);
        assertEquals(14, nap.getPosition());
        assertEquals(18, nap.getSize());
    }

    @Test
    public void test13() throws Exception
    {
        String aSrc = "class A {\n" +         // 0 - 10
            "    \n" +                        // 10 - 15 
            "    void xyz(int n) { }\n" +      // 15 - 39 
            "}\n";                            // 39 - 41
        
        TestableDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);

        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(40, nap.getSize());
        
        nap = nap.getNode().findNodeAt(9, nap.getPosition()); // class inner
        assertNotNull(nap);
        assertEquals(9, nap.getPosition());
        assertEquals(30, nap.getSize());
        
        nap = nap.getNode().findNodeAt(19, nap.getPosition()); // method
        assertNotNull(nap);
        assertEquals(19, nap.getPosition());
        assertEquals(19, nap.getSize());
        
        // Insert beginning of new method declaration:
        aDoc.insertString(14, "<T> fff(List<");
        
        aNode = aDoc.getParser();
        nap = aNode.findNodeAt(0, 0);
        
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(53, nap.getSize());
        
        nap = nap.getNode().findNodeAt(9, nap.getPosition()); // class inner
        assertNotNull(nap);
        assertEquals(9, nap.getPosition());
        assertEquals(43, nap.getSize());
    }

    @Test
    public void testChangeSuper() throws Exception
    {
        String aSrc = "class A {\n" +         // 0 - 10
            "  public void someFunc() {\n" +  // 10 - 37
            "  \n" +                          // 37 - 40  
            "  }\n" +                         // 40 - 44
            "}\n";                            // 44 - 46

        TestableDocument aDoc = docForSource(aSrc, "");
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
        aDoc.insertString(7, " extends String");
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
        aDoc.insertString(16, "Object");
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
    
    /**
     * Test for bug #317 regression.
     */
    @Test
    public void testRegression317() throws Exception
    {
        TestableDocument aDoc = docForSource("", "");
        
        aDoc.insertString(0, "class ");
        aDoc.getParser(); // empty reparse queue
        aDoc.insertString(6, "A\n");
        aDoc.getParser(); // empty reparse queue
        aDoc.insertString(8, "{\n\n}\n");
        
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);

        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(12, nap.getSize());
        
        // Insert a method
        aDoc.insertString(10, "    public void x() { }");
        
        aNode = aDoc.getParser();
        nap = aNode.findNodeAt(0, 0);
        
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(35, nap.getSize());
        
        nap = nap.getNode().findNodeAt(9, 0); // class inner
        assertNotNull(nap);
        assertEquals(9, nap.getPosition());
        assertEquals(25, nap.getSize());
        
        nap = nap.getNode().findNodeAt(14, nap.getPosition()); // method
        assertNotNull(nap);
        assertEquals(14, nap.getPosition());
        assertEquals(19, nap.getSize());
        
        
        // Delete space before class '{'
        aDoc.remove(7, 1);
        
        aNode = aDoc.getParser();
        nap = aNode.findNodeAt(0, 0);

        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(34, nap.getSize());
        
        nap = nap.getNode().findNodeAt(8, 0); // class inner
        assertNotNull(nap);
        assertEquals(8, nap.getPosition());
        assertEquals(25, nap.getSize());
        
        nap = nap.getNode().findNodeAt(13, nap.getPosition()); // method
        assertNotNull(nap);
        assertEquals(13, nap.getPosition());
        assertEquals(19, nap.getSize());
    }

    @Test
    public void testImportStmt() throws Exception
    {
        TestableDocument aDoc = docForSource("import somepkg.*; ;", "");
        
        // The import node should be size 16
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        assertEquals(17, nap.getSize());
        
        // Remove the first ';'
        aDoc.remove(16, 1);
        
        // Node should now extend to the next ';'
        aNode = aDoc.getParser();
        nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        assertEquals(18, nap.getSize());        
    }

    @Test
    public void testImportStmt2() throws Exception
    {
        TestableDocument aDoc = docForSource("import somepkg.* abc/* a comment */;", "");

        ParsedCUNode aNode = aDoc.getParser();
        
        // Remove the 'abc'
        aDoc.remove(17, 3);
        
        // Import node should exist now and extend to the ';'
        aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        assertEquals(33, nap.getSize());        
    }

    @Test
    public void testRegression331() throws Exception
    {
        String aSrc = "class A {\n" +         // 0 - 10
            "  public void someFunc() {\n" +  // 10 - 37
            "  \n" +                          // 37 - 40  
            "  }\n" +                         // 40 - 44
            "}\n";                            // 44 - 46

        TestableDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);

        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(45, nap.getSize());
        
        // Now insert " extends javax.swing.JFrame"
        aDoc.insertString(7, " extends javax");
        aNode = aDoc.getParser();
        
        aDoc.insertString(21, ".");
        aNode = aDoc.getParser();
        
        aDoc.insertString(22, "swing.JFrame");
        
        aNode = aDoc.getParser();
        nap = aNode.findNodeAt(0, 0);
        
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(72, nap.getSize());
    }

    @Test
    public void testRegression331p2() throws Exception
    {
        // Note the comment is significant in this.
        String aSrc = "/* comment */ class A extends javax.swing.JFrame {\n" +
            "  public void someFunc() {\n" +
            "  \n" +  
            "  }\n" +
            "}\n";

        TestableDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);

        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(86, nap.getSize());
        
        aDoc.insertString(48, ".");  // insert "." after "JFrame" - cause error
        aNode = aDoc.getParser();
        
        aDoc.remove(48, 1);  // remove it again
        aNode = aDoc.getParser();
        
        nap = aNode.findNodeAt(0, 0);
        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(86, nap.getSize());
    }

    @Test
    public void testNewStmt() throws Exception
    {
        String aSrc = "class A {\n" +         // 0 - 10 
            "  public void someFunc() {\n" +  // 10 - 37 
            "    A a = new \\A();\n" +        // 37 - 57
            "  }\n" +                         // 57 - 61 
            "}\n";                            // 61 - 63 

        TestableDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        
        aDoc.remove(51, 1); // remove the '\' before 'A()'
        
        aNode = aDoc.getParser();
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);

        assertNotNull(nap);
        assertEquals(0, nap.getPosition());
        assertEquals(61, nap.getSize());
        
        // type inner node
        nap = nap.getNode().findNodeAt(12, nap.getPosition());
        assertNotNull(nap);
        assertEquals(9, nap.getPosition());
        assertEquals(51, nap.getSize());
        
        // method node
        nap = nap.getNode().findNodeAt(12, nap.getPosition());
        assertNotNull(nap);
        assertEquals(12, nap.getPosition());
        assertEquals(47, nap.getSize());
        
        // method inner
        nap = nap.getNode().findNodeAt(36, nap.getPosition());
        assertNotNull(nap);
        assertEquals(36, nap.getPosition());
        assertEquals(22, nap.getSize());
        
        // field declaration node:
        nap = nap.getNode().findNodeAt(41, nap.getPosition());
        assertNotNull(nap);
        assertEquals(41, nap.getPosition());
        assertEquals(14, nap.getSize());
        
        // expression node:
        nap = nap.getNode().findNodeAt(47, nap.getPosition());
        assertNotNull(nap);
        assertEquals(47, nap.getPosition());
        assertEquals(7, nap.getSize());
    }

    // Should not throw an exception...
    @Test
    public void testRegression393() throws Exception
    {
        // Note the comment is significant in this.
        String aSrc = "@RunWith(Suite.class)\n" +  // 0 - 22
            "public class AllTests  {\n" +         // 22 - 47
            "//     public static void main(String[] args) {\n" +   // 47 -
            "}\n";

        TestableDocument aDoc = docForSource(aSrc, "");
        aDoc.getParser();
        
        aDoc.remove(47, 1); // remove '/' at start of comment
        aDoc.getParser();
    }

    @Test
    public void testRegression440() throws Exception
    {
        // Note the comment is significant in this.
        String aSrc = "public class XXXXX {\n" +   //  21
                "    public XXXXX() {\n" +         //  21
                "        //\n" +                   //  11
                "        for (int i = 0; i < 10; i++) {\n" +  // 39 
                "        \n" +                     //   9
                "        }\n" +                    //  10
                "    }\n" +                        //   6  
                "}\n";                             //   2

        TestableDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        
        NodeAndPosition<ParsedNode> nap = aNode.findNodeAt(0, 0);  // class
        assertEquals(0, nap.getPosition());
        assertEquals(118, nap.getSize());

        nap = nap.getNode().findNodeAt(20, nap.getPosition()); // class inner
        assertEquals(20, nap.getPosition());
        assertEquals(97, nap.getSize());
        
        nap = nap.getNode().findNodeAt(25, nap.getPosition()); // constructor
        assertEquals(25, nap.getPosition());
        assertEquals(91, nap.getSize());
        
        nap = nap.getNode().findNodeAt(41, nap.getPosition()); // constructor inner
        assertEquals(41, nap.getPosition());
        assertEquals(74, nap.getSize());

        nap = nap.getNode().findNodeAt(61, nap.getPosition()); // for loop
        assertEquals(61, nap.getPosition());
        assertEquals(49, nap.getSize());
        
        nap = nap.getNode().findNodeAt(91, nap.getPosition()); // for loop inner
        assertEquals(91, nap.getPosition());
        assertEquals(18, nap.getSize());
        
        // Insert '//' before 'for' and the next two lines
        aDoc.insertString(61, "//");
        aDoc.getParser();
        aDoc.insertString(61 + 39 + 2, "//");
        aDoc.getParser();
        aDoc.insertString(61 + 39 + 9 + 4, "//");
        
        aNode = aDoc.getParser();
        
        nap = aNode.findNodeAt(0, 0);  // class
        assertEquals(0, nap.getPosition());
        assertEquals(118 + 6, nap.getSize());

        nap = nap.getNode().findNodeAt(20, nap.getPosition()); // class inner
        assertEquals(20, nap.getPosition());
        assertEquals(97 + 6, nap.getSize());
        
        nap = nap.getNode().findNodeAt(25, nap.getPosition()); // constructor
        assertEquals(25, nap.getPosition());
        assertEquals(91 + 6, nap.getSize());
        
        nap = nap.getNode().findNodeAt(41, nap.getPosition()); // constructor inner
        assertEquals(41, nap.getPosition());
        assertEquals(74 + 6, nap.getSize());
    }

    @Test
    public void testMultipleDeclaration() throws Exception
    {
        String aSrc = "class A {\n" +  // 0 - 10
                "  public Runnable r, b = new Runnable() {\n" +  // 10 - 52 
                "    public void run() {\n" +             // 52 - 76
                "      c.run();\n"          +             // 76 - 81     
                "    }\n" +                               // 81 - 97
                "  };\n" +                                // 97 - 102
                "}\n";                                    // 102 - 104
        
        TestableDocument aDoc = docForSource(aSrc, "");
        ParsedCUNode aNode = aDoc.getParser();
        
        NodeAndPosition<ParsedNode> classNap = aNode.findNodeAt(0, 0);  // class
        assertEquals(0, classNap.getPosition());
        assertEquals(103, classNap.getSize());
        
        ParsedTypeNode classNode = (ParsedTypeNode) classNap.getNode();
        TypeInnerNode classInner = classNode.getInner();

        // There should be two field nodes:
        NodeAndPosition<ParsedNode> nap = classInner.findNodeAt(12, 9);
        assertEquals(12, nap.getPosition());
        assertEquals(29, nap.getEnd()); // *doesn't* include comma
        
        nap = classInner.findNodeAt(30, 9);
        assertEquals(30, nap.getPosition());
        assertEquals(101, nap.getEnd()); // includes semicolon
        
        JavaEntity rvar = classInner.getValueEntity("r", null);
        assertNotNull(rvar);
        ValueEntity rval = rvar.resolveAsValue();
        assertNotNull(rval);
        assertEquals("java.lang.Runnable", rval.getType().toString());
        
        JavaEntity bvar = classInner.getValueEntity("b", null);
        assertNotNull(bvar);
        ValueEntity bval = bvar.resolveAsValue();
        assertNotNull(bval);
        assertEquals(bval.getType().toString(), "java.lang.Runnable");

        
        
        aDoc.insertString(100, ", c");
        aNode = aDoc.getParser();
        nap = aNode.findNodeAt(0, 0);        
        classNode = (ParsedTypeNode) nap.getNode();
        classInner = classNode.getInner();

        // There should now be three field nodes:
        nap = classInner.findNodeAt(12, 9);
        assertEquals(12, nap.getPosition());
        assertEquals(29, nap.getEnd()); // *doesn't* include comma
        
        nap = classInner.findNodeAt(30, 9);
        assertEquals(30, nap.getPosition());
        assertEquals(100, nap.getEnd()); // *doesn't* include comma
        
        nap = classInner.findNodeAt(101, 9);
        assertEquals(101, nap.getPosition());
        assertEquals(104, nap.getEnd()); // includes semicolon
        
        
        
        bvar = classInner.getValueEntity("b", null);
        assertNotNull(bvar);
        bval = bvar.resolveAsValue();
        assertNotNull(bval);
        assertNotNull(bval.getType());
        assertEquals(bval.getType().toString(), "java.lang.Runnable");

        JavaEntity cvar = classInner.getValueEntity("c", null);
        assertNotNull(cvar);
        ValueEntity cval = cvar.resolveAsValue();
        assertNotNull(cval);
        assertNotNull(cval.getType());
        assertEquals(cval.getType().toString(), "java.lang.Runnable");
        
        
        ExpressionTypeInfo suggests = aNode.getExpressionType(84, aDoc);
        assertNotNull(suggests);
        assertEquals(suggests.getSuggestionType().toString(), "java.lang.Runnable");
    }
}
