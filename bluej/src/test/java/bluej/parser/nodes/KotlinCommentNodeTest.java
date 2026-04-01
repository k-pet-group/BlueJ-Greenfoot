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
package bluej.parser.nodes;

import bluej.parser.Token.TokenType;
import bluej.parser.kotlin.KotlinCommentNode;
import bluej.parser.kotlin.KotlinParsedCUNode;
import bluej.parser.nodes.NodeTree.NodeAndPosition;

import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for KotlinCommentNode incremental reparse (Tier 1).
 *
 * <p>Verifies that edits inside comments are absorbed locally
 * ({@code textInserted}/{@code textRemoved} return {@code ALL_OK})
 * and that {@code reparseNode()} validates comment boundaries
 * using {@code KotlinLexer}.</p>
 *
 * <p>This test lives in {@code bluej.parser.nodes} (same package as
 * {@link ParsedNode}) so it can directly reference the protected
 * reparse result constants ({@code ALL_OK}, {@code NODE_SHRUNK},
 * {@code REMOVE_NODE}) without duplication.</p>
 */
public class KotlinCommentNodeTest
{
    // -----------------------------------------------------------------------
    // Test document implementation with line tracking
    // -----------------------------------------------------------------------

    /**
     * A mutable document implementation for testing comment reparse.
     * Supports line/column tracking via getDefaultRootElement().
     */
    private static class TestDocument implements ReparseableDocument
    {
        private String content;
        private int lastReparsePos = -1;
        private int lastReparseSize = -1;
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
            lastReparsePos = pos;
            lastReparseSize = size;
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

    /**
     * Line-based Element for getDefaultRootElement().
     * Splits content by newline to compute line-offset mappings.
     */
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
                    endOffset += 1; // account for '\n'
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

    // -----------------------------------------------------------------------
    // No-op listener
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // Helpers: create root node and comment node
    // -----------------------------------------------------------------------

    /**
     * Create a KotlinParsedCUNode as the root of the test tree.
     * ParsedCUNode overrides getSize()/resize()/setSize() with its own
     * {@code size} field, so it does not require NodeTree-based size
     * tracking — making it suitable as a test root without a real document.
     */
    private KotlinParsedCUNode createRoot(int size)
    {
        KotlinParsedCUNode root = new KotlinParsedCUNode();
        root.setSize(size);
        return root;
    }

    /**
     * Create a KotlinCommentNode and insert it into a parent
     * so that getSize()/resize()/setSize() work via NodeTree.
     */
    private KotlinCommentNode createCommentNode(KotlinParsedCUNode root,
            TokenType commentType, boolean singleLine, int offset, int size)
    {
        KotlinCommentNode node = new KotlinCommentNode(root, commentType, singleLine);
        node.setComplete(true);
        root.insertNode(node, offset, size, NO_OP);
        return node;
    }

    /**
     * Call {@code reparseNode()} on the given node. This helper exists
     * because {@code reparseNode} is {@code protected} in {@link ParsedNode};
     * accessing it through a {@code ParsedNode} reference (same package)
     * satisfies the Java access check, while a {@code KotlinCommentNode}
     * reference (different package) would not.
     */
    private int callReparseNode(ParsedNode node, ReparseableDocument doc,
            int nodePos, int offset, int maxParse, NodeStructureListener listener)
    {
        return node.reparseNode(doc, nodePos, offset, maxParse, listener);
    }

    // -----------------------------------------------------------------------
    // Tests: textInserted absorbs edits
    // -----------------------------------------------------------------------

    @Test
    public void testTextInsertedReturnsAllOk()
    {
        String content = "/* hello */";
        TestDocument doc = new TestDocument(content);
        KotlinParsedCUNode root = createRoot(content.length());

        KotlinCommentNode comment = createCommentNode(root,
                TokenType.COMMENT_NORMAL, false, 0, content.length());

        // Insert a character at position 3 ("/* hXello */")
        int result = comment.textInserted(doc, 0, 3, 1, NO_OP);

        assertEquals("textInserted should return ALL_OK",
                ParsedNode.ALL_OK, result);
    }

    @Test
    public void testTextInsertedGrowsNodeSize()
    {
        String content = "/* hello */";
        TestDocument doc = new TestDocument(content);
        KotlinParsedCUNode root = createRoot(content.length());

        KotlinCommentNode comment = createCommentNode(root,
                TokenType.COMMENT_NORMAL, false, 0, content.length());
        int originalSize = comment.getSize();

        comment.textInserted(doc, 0, 3, 5, NO_OP);

        assertEquals("Node should grow by inserted length",
                originalSize + 5, comment.getSize());
    }

    @Test
    public void testTextInsertedSchedulesReparse()
    {
        String content = "/* hello */";
        TestDocument doc = new TestDocument(content);
        KotlinParsedCUNode root = createRoot(content.length());

        KotlinCommentNode comment = createCommentNode(root,
                TokenType.COMMENT_NORMAL, false, 0, content.length());

        comment.textInserted(doc, 0, 3, 1, NO_OP);

        assertEquals("Reparse should be scheduled at insertion point",
                3, doc.lastReparsePos);
    }

    // -----------------------------------------------------------------------
    // Tests: textRemoved absorbs edits
    // -----------------------------------------------------------------------

    @Test
    public void testTextRemovedReturnsAllOk()
    {
        String content = "/* hello */";
        TestDocument doc = new TestDocument(content);
        KotlinParsedCUNode root = createRoot(content.length());

        KotlinCommentNode comment = createCommentNode(root,
                TokenType.COMMENT_NORMAL, false, 0, content.length());

        int result = comment.textRemoved(doc, 0, 3, 1, NO_OP);

        assertEquals("textRemoved should return ALL_OK",
                ParsedNode.ALL_OK, result);
    }

    @Test
    public void testTextRemovedShrinksNodeSize()
    {
        String content = "/* hello */";
        TestDocument doc = new TestDocument(content);
        KotlinParsedCUNode root = createRoot(content.length());

        KotlinCommentNode comment = createCommentNode(root,
                TokenType.COMMENT_NORMAL, false, 0, content.length());
        int originalSize = comment.getSize();

        comment.textRemoved(doc, 0, 3, 2, NO_OP);

        assertEquals("Node should shrink by removed length",
                originalSize - 2, comment.getSize());
    }

    // -----------------------------------------------------------------------
    // Tests: reparseNode validates block comment
    // -----------------------------------------------------------------------

    @Test
    public void testReparseValidBlockComment()
    {
        String content = "/* hello world */";
        TestDocument doc = new TestDocument(content);
        KotlinParsedCUNode root = createRoot(content.length());

        KotlinCommentNode comment = createCommentNode(root,
                TokenType.COMMENT_NORMAL, false, 0, content.length());

        int result = callReparseNode(comment, doc, 0, 0, content.length(), NO_OP);

        assertEquals("Valid block comment should return ALL_OK",
                ParsedNode.ALL_OK, result);
    }

    // -----------------------------------------------------------------------
    // Tests: reparseNode validates KDoc comment
    // -----------------------------------------------------------------------

    @Test
    public void testReparseValidKDocComment()
    {
        String content = "/** KDoc comment */";
        TestDocument doc = new TestDocument(content);
        KotlinParsedCUNode root = createRoot(content.length());

        KotlinCommentNode comment = createCommentNode(root,
                TokenType.COMMENT_JAVADOC, false, 0, content.length());

        int result = callReparseNode(comment, doc, 0, 0, content.length(), NO_OP);

        assertEquals("Valid KDoc comment should return ALL_OK",
                ParsedNode.ALL_OK, result);
    }

    // -----------------------------------------------------------------------
    // Tests: reparseNode validates EOL comment
    // -----------------------------------------------------------------------

    @Test
    public void testReparseValidEolComment()
    {
        String content = "// end of line comment";
        TestDocument doc = new TestDocument(content);
        KotlinParsedCUNode root = createRoot(content.length());

        KotlinCommentNode comment = createCommentNode(root,
                TokenType.COMMENT_NORMAL, true, 0, content.length());

        int result = callReparseNode(comment, doc, 0, 0, content.length(), NO_OP);

        assertEquals("Valid EOL comment should return ALL_OK",
                ParsedNode.ALL_OK, result);
    }

    // -----------------------------------------------------------------------
    // Tests: reparseNode detects broken structure
    // -----------------------------------------------------------------------

    @Test
    public void testReparseBrokenBlockComment()
    {
        // The document contains text that is not a comment at all.
        String content = "not a comment";
        TestDocument doc = new TestDocument(content);
        KotlinParsedCUNode root = createRoot(content.length());

        KotlinCommentNode comment = createCommentNode(root,
                TokenType.COMMENT_NORMAL, false, 0, content.length());

        int result = callReparseNode(comment, doc, 0, 0, content.length(), NO_OP);

        assertEquals("Non-comment content should return REMOVE_NODE",
                ParsedNode.REMOVE_NODE, result);
    }

    // -----------------------------------------------------------------------
    // Tests: reparseNode detects type change (single-line <-> multi-line)
    // -----------------------------------------------------------------------

    @Test
    public void testReparseDetectsSingleToMultiLineChange()
    {
        // Node was created as single-line, but content is now a block comment
        String content = "/* now a block */";
        TestDocument doc = new TestDocument(content);
        KotlinParsedCUNode root = createRoot(content.length());

        // Create as single-line (was originally //)
        KotlinCommentNode comment = createCommentNode(root,
                TokenType.COMMENT_NORMAL, true, 0, content.length());

        int result = callReparseNode(comment, doc, 0, 0, content.length(), NO_OP);

        assertEquals("Single-line -> multi-line change should return REMOVE_NODE",
                ParsedNode.REMOVE_NODE, result);
    }

    @Test
    public void testReparseDetectsMultiToSingleLineChange()
    {
        // Node was created as multi-line, but content is now a line comment
        String content = "// now a line comment";
        TestDocument doc = new TestDocument(content);
        KotlinParsedCUNode root = createRoot(content.length());

        // Create as multi-line (was originally /* */)
        KotlinCommentNode comment = createCommentNode(root,
                TokenType.COMMENT_NORMAL, false, 0, content.length());

        int result = callReparseNode(comment, doc, 0, 0, content.length(), NO_OP);

        assertEquals("Multi-line -> single-line change should return REMOVE_NODE",
                ParsedNode.REMOVE_NODE, result);
    }

    // -----------------------------------------------------------------------
    // Tests: reparseNode adjusts size (NODE_SHRUNK)
    // -----------------------------------------------------------------------

    @Test
    public void testReparseSizeAdjustment()
    {
        // Content is a valid block comment that is smaller than the node's
        // registered size — reparseNode should adjust via NODE_SHRUNK
        String content = "/* hi */  extra";
        TestDocument doc = new TestDocument(content);
        KotlinParsedCUNode root = createRoot(content.length());

        // Create node with the full document length (including "  extra")
        KotlinCommentNode comment = createCommentNode(root,
                TokenType.COMMENT_NORMAL, false, 0, content.length());

        int result = callReparseNode(comment, doc, 0, 0, content.length(), NO_OP);

        // The lexer should see "/* hi */" as the comment token (8 chars),
        // and the node was 15 chars. So it should shrink.
        assertEquals("Size mismatch should return NODE_SHRUNK",
                ParsedNode.NODE_SHRUNK, result);
        assertEquals("Node should be resized to actual comment length",
                8, comment.getSize());
    }
}
