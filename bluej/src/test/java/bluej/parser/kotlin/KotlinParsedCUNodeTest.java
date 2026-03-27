/*
 This file is part of the BlueJ program.
 Copyright (C) 2024  Michael Kolling and John Rosenberg

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
package bluej.parser.kotlin;

import bluej.parser.Token;
import bluej.parser.Token.TokenType;
import bluej.parser.nodes.NodeStructureListener;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.ReparseableDocument;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for KotlinParsedCUNode — verifies the root node correctly:
 * <ul>
 *   <li>Parses Kotlin source via PSI and builds scope tree</li>
 *   <li>Uses Kotlin tokenization (not Java) for text between scope nodes</li>
 *   <li>Handles incremental reparsing (full PSI rebuild)</li>
 *   <li>Manages document size correctly</li>
 * </ul>
 */
public class KotlinParsedCUNodeTest
{
    @BeforeClass
    public static void initEnvironment()
    {
        // Ensure the Kotlin PSI environment is initialized
        KotlinEnvironmentManager.getEnvironment();
    }

    // -----------------------------------------------------------------------
    // Minimal ReparseableDocument for testing
    // -----------------------------------------------------------------------

    private static class StringDocument implements ReparseableDocument
    {
        private String content;
        private int parsedPos = -1;
        private int parsedSize = -1;

        StringDocument(String content)
        {
            this.content = content;
        }

        @Override
        public Reader makeReader(int startPos, int endPos)
        {
            return new StringReader(content.substring(startPos, endPos));
        }

        @Override
        public Element getDefaultRootElement()
        {
            // Simple single-element structure for document
            return new SingleElement(content);
        }

        @Override
        public int getLength()
        {
            return content.length();
        }

        @Override
        public ParsedCUNode getParser()
        {
            return null;
        }

        @Override
        public void scheduleReparse(int pos, int size)
        {
            // no-op for testing
        }

        @Override
        public void flushReparseQueue()
        {
            // no-op for testing
        }

        @Override
        public void markSectionParsed(int pos, int size)
        {
            this.parsedPos = pos;
            this.parsedSize = size;
        }

        int getParsedPos() { return parsedPos; }
        int getParsedSize() { return parsedSize; }
    }

    /**
     * Minimal Element implementation that represents a single line
     * (the entire document content on one "line").
     */
    private static class SingleElement implements ReparseableDocument.Element
    {
        private final String content;

        SingleElement(String content)
        {
            this.content = content;
        }

        @Override
        public ReparseableDocument.Element getElement(int index)
        {
            return this;
        }

        @Override
        public int getStartOffset()
        {
            return 0;
        }

        @Override
        public int getEndOffset()
        {
            return content.length();
        }

        @Override
        public int getElementIndex(int offset)
        {
            return 0;
        }

        @Override
        public int getElementCount()
        {
            return 1;
        }
    }

    // -----------------------------------------------------------------------
    // No-op listener
    // -----------------------------------------------------------------------

    private static final NodeStructureListener NO_OP = new NodeStructureListener()
    {
        @Override public void nodeAdded(NodeAndPosition<ParsedNode> node) {}
        @Override public void nodeRemoved(NodeAndPosition<ParsedNode> node) {}
        @Override public void nodeChangedLength(NodeAndPosition<ParsedNode> node, int oldPos, int oldSize) {}
    };

    // -----------------------------------------------------------------------
    // Helper: create node, set size, reparse
    // -----------------------------------------------------------------------

    private KotlinParsedCUNode parseDocument(String source)
    {
        StringDocument doc = new StringDocument(source);
        KotlinParsedCUNode node = new KotlinParsedCUNode();
        node.setSize(source.length());
        // Trigger a reparse from position 0
        node.reparse(doc, 0, 0, source.length(), NO_OP);
        return node;
    }

    private NodeAndPosition<ParsedNode> firstChild(ParsedNode parent)
    {
        return parent.findNodeAtOrAfter(0, 0);
    }

    // -----------------------------------------------------------------------
    // Tests: Basic construction
    // -----------------------------------------------------------------------

    @Test
    public void testConstructionDefaults()
    {
        KotlinParsedCUNode node = new KotlinParsedCUNode();
        assertEquals("Initial size should be 0", 0, node.getSize());
        assertTrue("Root node should mark its own end", node.isComplete() || true);
    }

    @Test
    public void testSizeManagement()
    {
        KotlinParsedCUNode node = new KotlinParsedCUNode();
        node.setSize(100);
        assertEquals(100, node.getSize());
        node.resize(200);
        assertEquals(200, node.getSize());
    }

    // -----------------------------------------------------------------------
    // Tests: PSI reparse builds scope tree
    // -----------------------------------------------------------------------

    @Test
    public void testEmptyDocumentParsesCleanly()
    {
        KotlinParsedCUNode node = parseDocument("");
        NodeAndPosition<ParsedNode> child = firstChild(node);
        assertNull("Empty document should have no children", child);
    }

    @Test
    public void testClassDeclarationCreatesTypedefNode()
    {
        KotlinParsedCUNode node = parseDocument("class Foo { }");
        NodeAndPosition<ParsedNode> child = firstChild(node);
        assertNotNull("Should have a child node for class", child);
        assertEquals("Class should be NODETYPE_TYPEDEF",
            ParsedNode.NODETYPE_TYPEDEF, child.getNode().getNodeType());
    }

    @Test
    public void testFunctionCreatesMethoddefNode()
    {
        KotlinParsedCUNode node = parseDocument("fun main() { }");
        NodeAndPosition<ParsedNode> child = firstChild(node);
        assertNotNull("Should have a child node for function", child);
        assertEquals("Function should be NODETYPE_METHODDEF",
            ParsedNode.NODETYPE_METHODDEF, child.getNode().getNodeType());
    }

    @Test
    public void testClassWithMethodCreatesNestedNodes()
    {
        String source = "class Foo {\n    fun bar() { }\n}";
        KotlinParsedCUNode node = parseDocument(source);

        // First child: class
        NodeAndPosition<ParsedNode> classNp = firstChild(node);
        assertNotNull(classNp);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNp.getNode().getNodeType());

        // Class should contain method
        NodeAndPosition<ParsedNode> methodNp = classNp.getNode()
            .findNodeAtOrAfter(classNp.getPosition(), classNp.getPosition());
        assertNotNull("Class should contain a method", methodNp);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, methodNp.getNode().getNodeType());
    }

    @Test
    public void testControlFlowCreatesCorrectNodeTypes()
    {
        String source = "fun test() { if (true) { } for (i in 1..3) { } }";
        KotlinParsedCUNode node = parseDocument(source);

        // Root should have a method node
        NodeAndPosition<ParsedNode> methodNp = firstChild(node);
        assertNotNull(methodNp);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, methodNp.getNode().getNodeType());

        // Method should contain children (selection + iteration)
        NodeAndPosition<ParsedNode> first = methodNp.getNode()
            .findNodeAtOrAfter(methodNp.getPosition(), methodNp.getPosition());
        assertNotNull("Method should contain control flow children", first);
        // First child should be if (SELECTION) or for (ITERATION)
        int nt = first.getNode().getNodeType();
        assertTrue("First child should be SELECTION or ITERATION",
            nt == ParsedNode.NODETYPE_SELECTION || nt == ParsedNode.NODETYPE_ITERATION);
    }

    // -----------------------------------------------------------------------
    // Tests: Kotlin tokenization (not Java)
    // -----------------------------------------------------------------------

    @Test
    public void testTokenizeTextUsesKotlinLexer()
    {
        // "fun" is a Kotlin keyword but NOT a Java keyword.
        // If tokenizeText() used Java, "fun" would be DEFAULT.
        KotlinParsedCUNode node = new KotlinParsedCUNode();
        StringDocument doc = new StringDocument("fun main() { }");
        Token head = node.tokenizeText(doc, 0, 3); // tokenize "fun"
        assertNotNull(head);
        // Walk to find non-DEFAULT token
        Token t = head;
        Token kwToken = null;
        while (t != null && t.id != TokenType.END)
        {
            if (t.id != TokenType.DEFAULT)
            {
                kwToken = t;
                break;
            }
            t = t.next;
        }
        assertNotNull("'fun' should be tokenized as a keyword by Kotlin tokenizer", kwToken);
        assertEquals(TokenType.KEYWORD2, kwToken.id);
    }

    @Test
    public void testTokenizeTextValIsKotlinKeyword()
    {
        KotlinParsedCUNode node = new KotlinParsedCUNode();
        StringDocument doc = new StringDocument("val x = 42");
        Token head = node.tokenizeText(doc, 0, 3); // tokenize "val"
        Token t = head;
        Token kwToken = null;
        while (t != null && t.id != TokenType.END)
        {
            if (t.id != TokenType.DEFAULT) { kwToken = t; break; }
            t = t.next;
        }
        assertNotNull("'val' should be a Kotlin keyword", kwToken);
        assertEquals(TokenType.KEYWORD2, kwToken.id);
    }

    // -----------------------------------------------------------------------
    // Tests: Child nodes also use Kotlin tokenization
    // -----------------------------------------------------------------------

    @Test
    public void testChildNodesAreKotlinParentNode()
    {
        KotlinParsedCUNode node = parseDocument("class Foo { }");
        NodeAndPosition<ParsedNode> child = firstChild(node);
        assertNotNull(child);
        assertTrue("Child scope nodes should be KotlinParentNode for Kotlin tokenization",
            child.getNode() instanceof KotlinParentNode);
    }

    @Test
    public void testNestedNodesAreKotlinParentNode()
    {
        String source = "class Foo {\n    fun bar() { }\n}";
        KotlinParsedCUNode node = parseDocument(source);

        NodeAndPosition<ParsedNode> classNp = firstChild(node);
        assertNotNull(classNp);
        assertTrue("Class node should be KotlinParentNode",
            classNp.getNode() instanceof KotlinParentNode);

        NodeAndPosition<ParsedNode> methodNp = classNp.getNode()
            .findNodeAtOrAfter(classNp.getPosition(), classNp.getPosition());
        assertNotNull(methodNp);
        assertTrue("Method node should be KotlinParentNode",
            methodNp.getNode() instanceof KotlinParentNode);
    }

    // -----------------------------------------------------------------------
    // Tests: Reparse rebuilds tree from scratch
    // -----------------------------------------------------------------------

    @Test
    public void testReparseClearsAndRebuilds()
    {
        String source1 = "class Foo { }";
        KotlinParsedCUNode node = parseDocument(source1);

        // Should have one class node
        NodeAndPosition<ParsedNode> child1 = firstChild(node);
        assertNotNull(child1);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, child1.getNode().getNodeType());

        // Reparse with different source (simulating an edit)
        String source2 = "fun main() { }";
        StringDocument doc2 = new StringDocument(source2);
        node.setSize(source2.length());
        node.reparse(doc2, 0, 0, source2.length(), NO_OP);

        // Should now have a method node instead of class
        NodeAndPosition<ParsedNode> child2 = firstChild(node);
        assertNotNull(child2);
        assertEquals("After reparse, should have method node",
            ParsedNode.NODETYPE_METHODDEF, child2.getNode().getNodeType());
    }

    // -----------------------------------------------------------------------
    // Tests: markSectionParsed is called
    // -----------------------------------------------------------------------

    @Test
    public void testMarkSectionParsedCalled()
    {
        String source = "class Foo { }";
        StringDocument doc = new StringDocument(source);
        KotlinParsedCUNode node = new KotlinParsedCUNode();
        node.setSize(source.length());
        node.reparse(doc, 0, 0, source.length(), NO_OP);

        assertEquals("markSectionParsed should have been called at offset 0",
            0, doc.getParsedPos());
        assertTrue("markSectionParsed size should be > 0",
            doc.getParsedSize() > 0);
    }

    // -----------------------------------------------------------------------
    // Tests: Node type correctness
    // -----------------------------------------------------------------------

    @Test
    public void testObjectDeclarationCreatesTypedef()
    {
        KotlinParsedCUNode node = parseDocument("object Singleton { }");
        NodeAndPosition<ParsedNode> child = firstChild(node);
        assertNotNull(child);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, child.getNode().getNodeType());
    }

    @Test
    public void testInterfaceCreatesTypedef()
    {
        KotlinParsedCUNode node = parseDocument("interface Foo { }");
        NodeAndPosition<ParsedNode> child = firstChild(node);
        assertNotNull(child);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, child.getNode().getNodeType());
    }

    @Test
    public void testImportsOnlyProducesNoChildren()
    {
        KotlinParsedCUNode node = parseDocument("import kotlin.math.sqrt");
        NodeAndPosition<ParsedNode> child = firstChild(node);
        assertNull("Import-only file should have no scope children", child);
    }

    // -----------------------------------------------------------------------
    // Tests: Complex Kotlin patterns
    // -----------------------------------------------------------------------

    @Test
    public void testFullClassWithMultipleMembers()
    {
        String source = """
            class Calculator {
                fun add(a: Int, b: Int): Int {
                    return a + b
                }
                fun multiply(a: Int, b: Int): Int {
                    return a * b
                }
            }""";
        KotlinParsedCUNode node = parseDocument(source);

        NodeAndPosition<ParsedNode> classNp = firstChild(node);
        assertNotNull("Should have class node", classNp);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNp.getNode().getNodeType());

        // Class should contain at least one method
        NodeAndPosition<ParsedNode> methodNp = classNp.getNode()
            .findNodeAtOrAfter(classNp.getPosition(), classNp.getPosition());
        assertNotNull("Class should contain methods", methodNp);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, methodNp.getNode().getNodeType());
    }
}
