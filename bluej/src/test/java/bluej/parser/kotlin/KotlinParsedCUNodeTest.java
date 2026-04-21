/*
 This file is part of the BlueJ program.
 Copyright (C) 2026  Michael Kolling and John Rosenberg

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

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import bluej.parser.Token;
import bluej.parser.Token.TokenType;
import bluej.parser.nodes.NodeStructureListener;
import bluej.parser.nodes.NodeTree.NodeAndPosition;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ParsedNode;
import bluej.parser.nodes.ReparseableDocument;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for KotlinParsedCUNode -- verifies PSI parsing, Kotlin
 * tokenization, and incremental reparsing of the root scope node.
 */
public class KotlinParsedCUNodeTest
{
    @BeforeClass
    public static void initEnvironment()
    {
        // Ensure the Kotlin PSI environment is initialized
        KotlinEnvironmentManager.getEnvironment();
    }

    private static class StringDocument implements ReparseableDocument
    {
        private String content;
        private final List<int[]> parsedSections = new ArrayList<>();

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
            parsedSections.add(new int[]{pos, size});
        }

        List<int[]> getParsedSections() { return parsedSections; }
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

    private static final NodeStructureListener NO_OP = new NodeStructureListener()
    {
        @Override public void nodeAdded(NodeAndPosition<ParsedNode> node) {}
        @Override public void nodeRemoved(NodeAndPosition<ParsedNode> node) {}
        @Override public void nodeChangedLength(NodeAndPosition<ParsedNode> node, int oldPos, int oldSize) {}
    };

    private KotlinParsedCUNode parseDocument(String source)
    {
        StringDocument doc = new StringDocument(source);
        KotlinParsedCUNode node = new KotlinParsedCUNode();
        node.setSize(source.length());
        node.reparse(doc, 0, 0, source.length(), NO_OP);
        return node;
    }

    private NodeAndPosition<ParsedNode> firstChild(ParsedNode parent)
    {
        return parent.findNodeAtOrAfter(0, 0);
    }

    @Test
    public void testConstructionDefaults()
    {
        KotlinParsedCUNode node = new KotlinParsedCUNode();
        assertEquals("Initial size should be 0", 0, node.getSize());
        // A freshly constructed node is not yet marked complete
        assertFalse("Freshly constructed node should not be complete", node.isComplete());
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

        // Class contains an inner NONE node (container+inner pattern from
        // KotlinPsiScopeBuilder), then the method is nested inside that.
        NodeAndPosition<ParsedNode> innerNp = classNp.getNode()
            .findNodeAtOrAfter(classNp.getPosition(), classNp.getPosition());
        assertNotNull("Class should contain an inner node", innerNp);
        assertEquals("First child of class should be inner NONE node",
            ParsedNode.NODETYPE_NONE, innerNp.getNode().getNodeType());

        // Method is inside the inner node
        NodeAndPosition<ParsedNode> methodNp = innerNp.getNode()
            .findNodeAtOrAfter(innerNp.getPosition(), innerNp.getPosition());
        assertNotNull("Inner node should contain a method", methodNp);
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

        // Method contains an inner NONE node (container+inner pattern from
        // KotlinPsiScopeBuilder), then control flow nodes are nested inside.
        NodeAndPosition<ParsedNode> innerNp = methodNp.getNode()
            .findNodeAtOrAfter(methodNp.getPosition(), methodNp.getPosition());
        assertNotNull("Method should contain an inner node", innerNp);
        assertEquals("First child of method should be inner NONE node",
            ParsedNode.NODETYPE_NONE, innerNp.getNode().getNodeType());

        // Control flow nodes are inside the inner node
        NodeAndPosition<ParsedNode> first = innerNp.getNode()
            .findNodeAtOrAfter(innerNp.getPosition(), innerNp.getPosition());
        assertNotNull("Inner node should contain control flow children", first);
        int nt = first.getNode().getNodeType();
        assertTrue("First child should be SELECTION or ITERATION",
            nt == ParsedNode.NODETYPE_SELECTION || nt == ParsedNode.NODETYPE_ITERATION);
    }

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

        // Navigate through inner NONE node to reach the method
        NodeAndPosition<ParsedNode> innerNp = classNp.getNode()
            .findNodeAtOrAfter(classNp.getPosition(), classNp.getPosition());
        assertNotNull(innerNp);
        assertTrue("Inner node should be KotlinParentNode",
            innerNp.getNode() instanceof KotlinParentNode);

        NodeAndPosition<ParsedNode> methodNp = innerNp.getNode()
            .findNodeAtOrAfter(innerNp.getPosition(), innerNp.getPosition());
        assertNotNull(methodNp);
        assertTrue("Method node should be KotlinParentNode",
            methodNp.getNode() instanceof KotlinParentNode);
    }

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

    @Test
    public void testMarkSectionParsedCalled()
    {
        String source = "class Foo { }";
        StringDocument doc = new StringDocument(source);
        KotlinParsedCUNode node = new KotlinParsedCUNode();
        node.setSize(source.length());
        node.reparse(doc, 0, 0, source.length(), NO_OP);

        List<int[]> sections = doc.getParsedSections();
        assertFalse("markSectionParsed should have been called", sections.isEmpty());
        assertEquals("First markSectionParsed should be at offset 0",
            0, sections.get(0)[0]);
        assertTrue("First markSectionParsed size should be > 0",
            sections.get(0)[1] > 0);
    }

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

        // Class contains an inner NONE node (container+inner pattern from
        // KotlinPsiScopeBuilder), then methods are nested inside that.
        NodeAndPosition<ParsedNode> innerNp = classNp.getNode()
            .findNodeAtOrAfter(classNp.getPosition(), classNp.getPosition());
        assertNotNull("Class should contain an inner node", innerNp);
        assertEquals("First child of class should be inner NONE node",
            ParsedNode.NODETYPE_NONE, innerNp.getNode().getNodeType());

        // Methods are inside the inner node
        NodeAndPosition<ParsedNode> methodNp = innerNp.getNode()
            .findNodeAtOrAfter(innerNp.getPosition(), innerNp.getPosition());
        assertNotNull("Inner node should contain methods", methodNp);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, methodNp.getNode().getNodeType());
    }
}
