/*
 This file is part of the BlueJ program.
 Copyright (C) 2025,2026  Michael Kolling and John Rosenberg

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
package bluej.parser.nodes;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import bluej.parser.kotlin.KotlinEnvironmentManager;
import bluej.parser.kotlin.KotlinParentNode;
import bluej.parser.kotlin.KotlinParsedCUNode;
import bluej.parser.kotlin.KotlinPsiScopeBuilder;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPsiFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for KotlinParentNode block-level PSI reparse (Tier 3).
 */
public class KotlinParentNodeReparseTest
{
    private static KtPsiFactory psiFactory;

    @BeforeClass
    public static void setupPsiFactory()
    {
        psiFactory = KotlinEnvironmentManager.getPsiFactory();
    }

    private static class TestDocument implements ReparseableDocument
    {
        private String content;
        private int lastMarkParsedPos = -1;
        private int lastMarkParsedSize = -1;

        TestDocument(String content)
        {
            this.content = content;
        }

        void setContent(String content)
        {
            this.content = content;
        }

        @Override
        public Reader makeReader(int startPos, int endPos)
        {
            return new StringReader(content.substring(startPos,
                    Math.min(endPos, content.length())));
        }

        @Override
        public Element getDefaultRootElement()
        {
            return new RootElement(content);
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
        }

        @Override
        public void flushReparseQueue()
        {
        }

        @Override
        public void markSectionParsed(int pos, int size)
        {
            lastMarkParsedPos = pos;
            lastMarkParsedSize = size;
        }
    }

    private static class RootElement implements ReparseableDocument.Element
    {
        private final List<LineElement> lines;

        RootElement(String content)
        {
            lines = new ArrayList<>();
            int offset = 0;
            String[] parts = content.split("\n", -1);
            for (int i = 0; i < parts.length; i++)
            {
                int endOffset = offset + parts[i].length();
                if (i < parts.length - 1)
                {
                    endOffset += 1;
                }
                lines.add(new LineElement(offset, endOffset));
                offset = endOffset;
            }
        }

        @Override
        public ReparseableDocument.Element getElement(int index)
        {
            if (index < 0 || index >= lines.size())
            {
                return lines.get(lines.size() - 1);
            }
            return lines.get(index);
        }

        @Override
        public int getStartOffset()
        {
            return 0;
        }

        @Override
        public int getEndOffset()
        {
            return lines.isEmpty() ? 0 : lines.get(lines.size() - 1).getEndOffset();
        }

        @Override
        public int getElementIndex(int offset)
        {
            for (int i = 0; i < lines.size(); i++)
            {
                if (offset < lines.get(i).getEndOffset())
                {
                    return i;
                }
            }
            return lines.size() - 1;
        }

        @Override
        public int getElementCount()
        {
            return lines.size();
        }
    }

    private static class LineElement implements ReparseableDocument.Element
    {
        private final int startOffset;
        private final int endOffset;

        LineElement(int startOffset, int endOffset)
        {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }

        @Override
        public ReparseableDocument.Element getElement(int index)
        {
            return null;
        }

        @Override
        public int getStartOffset()
        {
            return startOffset;
        }

        @Override
        public int getEndOffset()
        {
            return endOffset;
        }

        @Override
        public int getElementIndex(int offset)
        {
            return 0;
        }

        @Override
        public int getElementCount()
        {
            return 0;
        }
    }

    private static final NodeStructureListener NO_OP = new NodeStructureListener()
    {
        @Override
        public void nodeAdded(NodeAndPosition<ParsedNode> node) {}

        @Override
        public void nodeRemoved(NodeAndPosition<ParsedNode> node) {}

        @Override
        public void nodeChangedLength(NodeAndPosition<ParsedNode> node,
                int oldPos, int oldSize) {}
    };

    /**
     * Build a scope tree from source text using the PSI factory.
     */
    private KotlinParsedCUNode buildScopeTree(String source)
    {
        KtFile ktFile = psiFactory.createFile(source);
        KotlinParsedCUNode root = new KotlinParsedCUNode();
        root.setSize(source.length());
        KotlinPsiScopeBuilder.buildScopesFromFile(ktFile, root, 0);
        return root;
    }

    private NodeAndPosition<ParsedNode> firstChild(ParsedNode parent)
    {
        return parent.findNodeAtOrAfter(0, 0);
    }

    /**
     * Find the inner node of a container, skipping any comment nodes.
     */
    private NodeAndPosition<ParsedNode> findInner(NodeAndPosition<ParsedNode> containerNp)
    {
        NodeAndPosition<ParsedNode> np = containerNp.getNode().findNodeAtOrAfter(
            containerNp.getPosition(), containerNp.getPosition());
        while (np != null && np.getNode().getNodeType() == ParsedNode.NODETYPE_COMMENT)
        {
            int afterNode = np.getPosition() + np.getSize() + 1;
            np = containerNp.getNode().findNodeAtOrAfter(afterNode,
                    containerNp.getPosition());
        }
        assertNotNull("Container should have inner node (after skipping comments)", np);
        assertTrue("Child should be inner (after skipping comments)",
            np.getNode().isInner());
        return np;
    }

    /**
     * Call {@code reparseNode()} via a {@code ParsedNode} reference so
     * that same-package protected access works.
     */
    private int callReparseNode(ParsedNode node, ReparseableDocument doc,
            int nodePos, int offset, int maxParse, NodeStructureListener listener)
    {
        return node.reparseNode(doc, nodePos, offset, maxParse, listener);
    }

    /**
     * Check whether a node has any children of the given type.
     */
    private boolean hasChildOfType(ParsedNode parent, int parentPos, int nodeType)
    {
        NodeAndPosition<ParsedNode> child = parent.findNodeAtOrAfter(parentPos, parentPos);
        while (child != null)
        {
            if (child.getNode().getNodeType() == nodeType)
            {
                return true;
            }
            int afterNode = child.getPosition() + child.getSize() + 1;
            child = parent.findNodeAtOrAfter(afterNode, parentPos);
        }
        return false;
    }

    @Test
    public void testInnerNodeBlockReparseReturnsAllOk()
    {
        String source = "fun main() { val x = 1 }";
        KotlinParsedCUNode root = buildScopeTree(source);
        TestDocument doc = new TestDocument(source);

        NodeAndPosition<ParsedNode> methodNp = firstChild(root);
        assertNotNull(methodNp);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, methodNp.getNode().getNodeType());

        NodeAndPosition<ParsedNode> innerNp = findInner(methodNp);

        int result = callReparseNode(innerNp.getNode(), doc,
                innerNp.getPosition(), innerNp.getPosition(),
                innerNp.getSize(), NO_OP);

        assertEquals("Block-level reparse of function body should return ALL_OK",
                ParsedNode.ALL_OK, result);
    }

    @Test
    public void testInnerNodeBlockReparseMarksSectionParsed()
    {
        String source = "fun main() { val x = 1 }";
        KotlinParsedCUNode root = buildScopeTree(source);
        TestDocument doc = new TestDocument(source);

        NodeAndPosition<ParsedNode> methodNp = firstChild(root);
        NodeAndPosition<ParsedNode> innerNp = findInner(methodNp);

        callReparseNode(innerNp.getNode(), doc,
                innerNp.getPosition(), innerNp.getPosition(),
                innerNp.getSize(), NO_OP);

        assertEquals("markSectionParsed should be called with inner node position",
                innerNp.getPosition(), doc.lastMarkParsedPos);
        assertEquals("markSectionParsed should be called with inner node size",
                innerNp.getSize(), doc.lastMarkParsedSize);
    }

    @Test
    public void testBlockReparsePreservesIfChild()
    {
        String source = "fun test() { if (true) { println() } }";
        KotlinParsedCUNode root = buildScopeTree(source);
        TestDocument doc = new TestDocument(source);

        NodeAndPosition<ParsedNode> methodNp = firstChild(root);
        NodeAndPosition<ParsedNode> innerNp = findInner(methodNp);

        // Verify if-child exists before reparse
        assertTrue("Should have SELECTION child before reparse",
                hasChildOfType(innerNp.getNode(), innerNp.getPosition(),
                        ParsedNode.NODETYPE_SELECTION));

        // Reparse
        int result = callReparseNode(innerNp.getNode(), doc,
                innerNp.getPosition(), innerNp.getPosition(),
                innerNp.getSize(), NO_OP);
        assertEquals(ParsedNode.ALL_OK, result);

        // Verify if-child still present after reparse
        assertTrue("Should have SELECTION child after block-level reparse",
                hasChildOfType(innerNp.getNode(), innerNp.getPosition(),
                        ParsedNode.NODETYPE_SELECTION));
    }

    @Test
    public void testBlockReparsePreservesForChild()
    {
        String source = "fun test() { for (i in 1..10) { println(i) } }";
        KotlinParsedCUNode root = buildScopeTree(source);
        TestDocument doc = new TestDocument(source);

        NodeAndPosition<ParsedNode> methodNp = firstChild(root);
        NodeAndPosition<ParsedNode> innerNp = findInner(methodNp);

        assertTrue("Should have ITERATION child before reparse",
                hasChildOfType(innerNp.getNode(), innerNp.getPosition(),
                        ParsedNode.NODETYPE_ITERATION));

        int result = callReparseNode(innerNp.getNode(), doc,
                innerNp.getPosition(), innerNp.getPosition(),
                innerNp.getSize(), NO_OP);
        assertEquals(ParsedNode.ALL_OK, result);

        assertTrue("Should have ITERATION child after block-level reparse",
                hasChildOfType(innerNp.getNode(), innerNp.getPosition(),
                        ParsedNode.NODETYPE_ITERATION));
    }

    @Test
    public void testBlockReparseAddsNewScope()
    {
        // Initial source: no control flow in function body
        String originalSource = "fun test() { val x = 1 }";
        KotlinParsedCUNode root = buildScopeTree(originalSource);

        NodeAndPosition<ParsedNode> methodNp = firstChild(root);
        NodeAndPosition<ParsedNode> innerNp = findInner(methodNp);
        int innerPos = innerNp.getPosition();

        // Verify no SELECTION children initially
        assertFalse("Should have no SELECTION children initially",
                hasChildOfType(innerNp.getNode(), innerPos,
                        ParsedNode.NODETYPE_SELECTION));

        // Simulate edit: add an if block to the function body.
        // Keep the same function structure so the inner node position stays valid.
        String newSource = "fun test() { if (true) { println() } }";
        TestDocument doc = new TestDocument(newSource);

        // Compute new inner size: content between the function's braces
        int openBrace = newSource.indexOf('{');
        int closeBrace = newSource.lastIndexOf('}');
        int newInnerSize = closeBrace - openBrace - 1;

        // Resize nodes to match new document
        innerNp.getNode().resize(newInnerSize);
        root.setSize(newSource.length());
        methodNp.getNode().resize(newSource.length());

        // Reparse
        int result = callReparseNode(innerNp.getNode(), doc,
                innerPos, innerPos, newInnerSize, NO_OP);
        assertEquals("Block-level reparse should return ALL_OK",
                ParsedNode.ALL_OK, result);

        // Verify SELECTION child was created
        assertTrue("Should have SELECTION child after adding if block",
                hasChildOfType(innerNp.getNode(), innerPos,
                        ParsedNode.NODETYPE_SELECTION));
    }

    @Test
    public void testBlockReparseRemovesScope()
    {
        // Initial: function with if block
        String originalSource = "fun test() { if (true) { println() } }";
        KotlinParsedCUNode root = buildScopeTree(originalSource);

        NodeAndPosition<ParsedNode> methodNp = firstChild(root);
        NodeAndPosition<ParsedNode> innerNp = findInner(methodNp);
        int innerPos = innerNp.getPosition();

        // Verify SELECTION child exists
        assertTrue("Should have SELECTION child initially",
                hasChildOfType(innerNp.getNode(), innerPos,
                        ParsedNode.NODETYPE_SELECTION));

        // Simulate edit: remove if, replace with simple statement
        String newSource = "fun test() { val x = 1 }";
        TestDocument doc = new TestDocument(newSource);

        int openBrace = newSource.indexOf('{');
        int closeBrace = newSource.lastIndexOf('}');
        int newInnerSize = closeBrace - openBrace - 1;

        innerNp.getNode().resize(newInnerSize);
        root.setSize(newSource.length());
        methodNp.getNode().resize(newSource.length());

        // Reparse
        int result = callReparseNode(innerNp.getNode(), doc,
                innerPos, innerPos, newInnerSize, NO_OP);
        assertEquals(ParsedNode.ALL_OK, result);

        // Verify no SELECTION children remain
        assertFalse("Should have no SELECTION children after removing if block",
                hasChildOfType(innerNp.getNode(), innerPos,
                        ParsedNode.NODETYPE_SELECTION));
    }

    @Test
    public void testBlockEndBeyondDocumentReturnsRemoveNode()
    {
        // Build a tree from valid source
        String source = "fun test() { val x = 1 }";
        KotlinParsedCUNode root = buildScopeTree(source);

        NodeAndPosition<ParsedNode> methodNp = firstChild(root);
        NodeAndPosition<ParsedNode> innerNp = findInner(methodNp);

        // Simulate a document that has been significantly truncated.
        // The inner node's size extends well past the document end,
        // so nodePos + getSize() > document.getLength().
        String truncatedSource = "fun test() { v";
        TestDocument doc = new TestDocument(truncatedSource);

        // Don't resize the inner node — its size still references the
        // original document. nodePos + getSize() now exceeds the
        // truncated document length.
        int result = callReparseNode(innerNp.getNode(), doc,
                innerNp.getPosition(), innerNp.getPosition(),
                innerNp.getSize(), NO_OP);

        assertEquals("Inner node extending past document should return REMOVE_NODE",
                ParsedNode.REMOVE_NODE, result);
    }

    @Test
    public void testInnerNodeAtPositionZeroSucceeds()
    {
        // An inner node at position 0 should succeed — createBlock()
        // adds its own wrapping internally, so no ±1 boundary issue.
        String content = "val x = 1";
        TestDocument doc = new TestDocument(content);
        KotlinParsedCUNode root = new KotlinParsedCUNode();
        root.setSize(content.length());

        KotlinParentNode innerNode = new KotlinParentNode(root);
        innerNode.setInner(true);
        innerNode.setComplete(true);
        root.insertNode(innerNode, 0, content.length(), NO_OP);

        // nodePos=0 is valid — createBlock() handles wrapping internally
        int result = callReparseNode(innerNode, doc, 0, 0, content.length(), NO_OP);

        assertEquals("Inner node at position 0 should succeed",
                ParsedNode.ALL_OK, result);
    }

    @Test
    public void testMethodContainerReturnsRemoveNode()
    {
        String source = "fun main() { val x = 1 }";
        KotlinParsedCUNode root = buildScopeTree(source);
        TestDocument doc = new TestDocument(source);

        NodeAndPosition<ParsedNode> methodNp = firstChild(root);
        assertNotNull(methodNp);
        assertEquals(ParsedNode.NODETYPE_METHODDEF, methodNp.getNode().getNodeType());
        assertTrue("Method should be a container", methodNp.getNode().isContainer());

        int result = callReparseNode(methodNp.getNode(), doc,
                methodNp.getPosition(), methodNp.getPosition(),
                methodNp.getSize(), NO_OP);

        assertEquals("Container (METHODDEF) should return REMOVE_NODE",
                ParsedNode.REMOVE_NODE, result);
    }

    @Test
    public void testClassContainerReturnsRemoveNode()
    {
        String source = "class Foo { }";
        KotlinParsedCUNode root = buildScopeTree(source);
        TestDocument doc = new TestDocument(source);

        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull(classNp);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNp.getNode().getNodeType());
        assertTrue("Class should be a container", classNp.getNode().isContainer());

        int result = callReparseNode(classNp.getNode(), doc,
                classNp.getPosition(), classNp.getPosition(),
                classNp.getSize(), NO_OP);

        assertEquals("Container (TYPEDEF) should return REMOVE_NODE",
                ParsedNode.REMOVE_NODE, result);
    }

    @Test
    public void testSelectionContainerReturnsRemoveNode()
    {
        String source = "fun test() { if (true) { println() } }";
        KotlinParsedCUNode root = buildScopeTree(source);
        TestDocument doc = new TestDocument(source);

        NodeAndPosition<ParsedNode> methodNp = firstChild(root);
        NodeAndPosition<ParsedNode> methodInner = findInner(methodNp);

        NodeAndPosition<ParsedNode> ifNp = methodInner.getNode().findNodeAtOrAfter(
                methodInner.getPosition(), methodInner.getPosition());
        assertNotNull(ifNp);
        assertEquals(ParsedNode.NODETYPE_SELECTION, ifNp.getNode().getNodeType());
        assertTrue("if should be a container", ifNp.getNode().isContainer());

        int result = callReparseNode(ifNp.getNode(), doc,
                ifNp.getPosition(), ifNp.getPosition(),
                ifNp.getSize(), NO_OP);

        assertEquals("Container (SELECTION) should return REMOVE_NODE",
                ParsedNode.REMOVE_NODE, result);
    }

    @Test
    public void testClassInnerNodeReturnsRemoveNode()
    {
        // Class body inner nodes cannot use block-level reparse because
        // KtPsiFactory.createBlock() creates a KtBlockExpression, which
        // only processes block statements — not class member declarations
        // (functions, nested classes). These cascade to full-file reparse.
        String source = "class Foo {\n    fun bar() { }\n}";
        KotlinParsedCUNode root = buildScopeTree(source);
        TestDocument doc = new TestDocument(source);

        NodeAndPosition<ParsedNode> classNp = firstChild(root);
        assertNotNull(classNp);
        assertEquals(ParsedNode.NODETYPE_TYPEDEF, classNp.getNode().getNodeType());

        NodeAndPosition<ParsedNode> classInner = findInner(classNp);

        int result = callReparseNode(classInner.getNode(), doc,
                classInner.getPosition(), classInner.getPosition(),
                classInner.getSize(), NO_OP);
        assertEquals("Class body inner node should return REMOVE_NODE",
                ParsedNode.REMOVE_NODE, result);
    }

    @Test
    public void testBlockReparseChildPositionsAreCorrect()
    {
        // Verify that after block-level reparse, child nodes have correct
        // document positions. This catches the bug where passing nodePos
        // as parentAbsPos (instead of 1) would produce wrong relPos values.
        String source = "fun test() { if (true) { println() } }";
        //              0123456789012345678901234567890123456789
        //              0         1         2         3
        KotlinParsedCUNode root = buildScopeTree(source);
        TestDocument doc = new TestDocument(source);

        NodeAndPosition<ParsedNode> methodNp = firstChild(root);
        NodeAndPosition<ParsedNode> innerNp = findInner(methodNp);
        int innerPos = innerNp.getPosition();

        // Record the if-node position BEFORE reparse (from initial PSI parse)
        NodeAndPosition<ParsedNode> ifBefore = innerNp.getNode().findNodeAtOrAfter(
                innerPos, innerPos);
        // Skip non-SELECTION children (comments etc.)
        while (ifBefore != null && ifBefore.getNode().getNodeType() != ParsedNode.NODETYPE_SELECTION)
        {
            int after = ifBefore.getPosition() + ifBefore.getSize() + 1;
            ifBefore = innerNp.getNode().findNodeAtOrAfter(after, innerPos);
        }
        assertNotNull("Should find if-node before reparse", ifBefore);
        int expectedIfPos = ifBefore.getPosition();
        int expectedIfSize = ifBefore.getSize();

        // Block-level reparse
        int result = callReparseNode(innerNp.getNode(), doc,
                innerPos, innerPos, innerNp.getSize(), NO_OP);
        assertEquals(ParsedNode.ALL_OK, result);

        // Find the if-node AFTER reparse
        NodeAndPosition<ParsedNode> ifAfter = innerNp.getNode().findNodeAtOrAfter(
                innerPos, innerPos);
        while (ifAfter != null && ifAfter.getNode().getNodeType() != ParsedNode.NODETYPE_SELECTION)
        {
            int after = ifAfter.getPosition() + ifAfter.getSize() + 1;
            ifAfter = innerNp.getNode().findNodeAtOrAfter(after, innerPos);
        }
        assertNotNull("Should find if-node after reparse", ifAfter);

        // Position and size must match the original PSI-based parse
        assertEquals("if-node position should be preserved after block reparse",
                expectedIfPos, ifAfter.getPosition());
        assertEquals("if-node size should be preserved after block reparse",
                expectedIfSize, ifAfter.getSize());
    }

    @Test
    public void testBlockReparseMultipleChildPositions()
    {
        // Verify position accuracy with multiple children at different offsets
        String source = "fun test() {\n    if (a) { }\n    for (i in 1..3) { }\n}";
        KotlinParsedCUNode root = buildScopeTree(source);
        TestDocument doc = new TestDocument(source);

        NodeAndPosition<ParsedNode> methodNp = firstChild(root);
        NodeAndPosition<ParsedNode> innerNp = findInner(methodNp);
        int innerPos = innerNp.getPosition();

        // Collect original child positions
        List<int[]> originalChildren = new ArrayList<>();
        NodeAndPosition<ParsedNode> child = innerNp.getNode().findNodeAtOrAfter(
                innerPos, innerPos);
        while (child != null)
        {
            originalChildren.add(new int[]{
                child.getNode().getNodeType(),
                child.getPosition(),
                child.getSize()
            });
            int after = child.getPosition() + child.getSize() + 1;
            child = innerNp.getNode().findNodeAtOrAfter(after, innerPos);
        }
        assertFalse("Should have children before reparse", originalChildren.isEmpty());

        // Block-level reparse
        int result = callReparseNode(innerNp.getNode(), doc,
                innerPos, innerPos, innerNp.getSize(), NO_OP);
        assertEquals(ParsedNode.ALL_OK, result);

        // Collect reparsed child positions
        List<int[]> reparsedChildren = new ArrayList<>();
        child = innerNp.getNode().findNodeAtOrAfter(innerPos, innerPos);
        while (child != null)
        {
            reparsedChildren.add(new int[]{
                child.getNode().getNodeType(),
                child.getPosition(),
                child.getSize()
            });
            int after = child.getPosition() + child.getSize() + 1;
            child = innerNp.getNode().findNodeAtOrAfter(after, innerPos);
        }

        // Same number of children
        assertEquals("Should have same number of children after reparse",
                originalChildren.size(), reparsedChildren.size());

        // Each child should match
        for (int i = 0; i < originalChildren.size(); i++)
        {
            int[] orig = originalChildren.get(i);
            int[] rep = reparsedChildren.get(i);
            assertEquals("Child " + i + " type should match", orig[0], rep[0]);
            assertEquals("Child " + i + " position should match", orig[1], rep[1]);
            assertEquals("Child " + i + " size should match", orig[2], rep[2]);
        }
    }

    @Test
    public void testNestedControlFlowRebuiltCorrectly()
    {
        String source = "fun test() {\n    if (true) {\n        for (i in 1..3) {\n            println(i)\n        }\n    }\n}";
        KotlinParsedCUNode root = buildScopeTree(source);
        TestDocument doc = new TestDocument(source);

        NodeAndPosition<ParsedNode> methodNp = firstChild(root);
        NodeAndPosition<ParsedNode> methodInner = findInner(methodNp);

        // Reparse the method body
        int result = callReparseNode(methodInner.getNode(), doc,
                methodInner.getPosition(), methodInner.getPosition(),
                methodInner.getSize(), NO_OP);
        assertEquals(ParsedNode.ALL_OK, result);

        // Verify the if child is present
        assertTrue("Should have SELECTION child (if) after reparse",
                hasChildOfType(methodInner.getNode(), methodInner.getPosition(),
                        ParsedNode.NODETYPE_SELECTION));

        // Verify the for is nested inside the if
        NodeAndPosition<ParsedNode> ifNp = methodInner.getNode().findNodeAtOrAfter(
                methodInner.getPosition(), methodInner.getPosition());
        while (ifNp != null && ifNp.getNode().getNodeType() != ParsedNode.NODETYPE_SELECTION)
        {
            int after = ifNp.getPosition() + ifNp.getSize() + 1;
            ifNp = methodInner.getNode().findNodeAtOrAfter(after, methodInner.getPosition());
        }
        assertNotNull("Should find if node", ifNp);

        NodeAndPosition<ParsedNode> ifInner = findInner(ifNp);
        assertTrue("if body should contain ITERATION child (for)",
                hasChildOfType(ifInner.getNode(), ifInner.getPosition(),
                        ParsedNode.NODETYPE_ITERATION));
    }
}
