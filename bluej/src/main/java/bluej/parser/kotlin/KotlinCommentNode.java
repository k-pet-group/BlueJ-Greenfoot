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
import bluej.parser.lexer.LocatableToken;
import bluej.parser.nodes.JavaParentNode;
import bluej.parser.nodes.NodeStructureListener;
import bluej.parser.nodes.ReparseableDocument;
import bluej.parser.nodes.ReparseableDocument.Element;

import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.Reader;

/**
 * A leaf parse node representing a Kotlin comment (KDoc, block comment, or
 * end-of-line comment) in the scope tree.
 *
 * <p>This node prevents keyword highlighting inside comments by returning a
 * single comment-typed token from {@link #getMarkTokensFor}. When
 * {@code JavaSyntaxView} encounters this node, it uses the returned token
 * directly instead of calling {@code tokenizeText()} &mdash; so keywords like
 * {@code class}, {@code if}, {@code is} inside comment text are never
 * re-lexed as keywords.</p>
 *
 * <p>This mirrors Java's {@link bluej.parser.nodes.CommentNode} approach
 * for incremental reparse: edits inside comments are absorbed locally
 * ({@code textInserted}/{@code textRemoved} return {@code ALL_OK}), and
 * the deferred {@code reparseNode()} validates the comment boundaries using
 * {@link KotlinLexer}. This avoids triggering a full-file PSI reparse
 * (~50&ndash;100ms) for every keystroke inside a comment, reducing the
 * cost to ~0.1ms.</p>
 *
 * <p>Extends {@link JavaParentNode} (rather than {@code ParsedNode} directly)
 * because {@code ParsedNode}'s constructor is package-private to
 * {@code bluej.parser.nodes}. {@link JavaParentNode} provides a public
 * constructor accessible from {@code bluej.parser.kotlin}.</p>
 *
 * @author BlueJ Team
 */
@OnThread(Tag.FXPlatform)
public class KotlinCommentNode extends JavaParentNode
{
    private TokenType commentType;
    private boolean singleLine;

    /**
     * Create a comment node with explicit single-line flag.
     *
     * @param parent      the parent node in the parse tree
     * @param commentType either {@link TokenType#COMMENT_JAVADOC} for KDoc
     *                    or {@link TokenType#COMMENT_NORMAL} for block/line comments
     * @param singleLine  {@code true} for {@code //} end-of-line comments,
     *                    {@code false} for {@code /* *}{@code /} and {@code /** *}{@code /}
     */
    public KotlinCommentNode(JavaParentNode parent, TokenType commentType,
            boolean singleLine)
    {
        super(parent);
        this.commentType = commentType;
        this.singleLine = singleLine;
    }

    /**
     * Create a comment node (defaults to multi-line).
     *
     * @param parent      the parent node in the parse tree
     * @param commentType either {@link TokenType#COMMENT_JAVADOC} for KDoc
     *                    or {@link TokenType#COMMENT_NORMAL} for block/line comments
     */
    public KotlinCommentNode(JavaParentNode parent, TokenType commentType)
    {
        this(parent, commentType, false);
    }

    @Override
    public int getNodeType()
    {
        return NODETYPE_COMMENT;
    }

    @Override
    protected boolean marksOwnEnd()
    {
        return true;
    }

    /**
     * Return a single token covering the entire comment region.
     * This prevents {@code tokenizeText()} from being called on comment
     * content, which would re-lex keywords inside comments.
     */
    @Override
    public TokenAndScope getMarkTokensFor(int pos, int length, int nodePos,
            ReparseableDocument document)
    {
        Token tok = new Token(length, commentType);
        tok.next = new Token(0, TokenType.END);
        return new TokenAndScope(tok, pos);
    }

    // -----------------------------------------------------------------------
    // Edit absorption — absorb locally, schedule deferred reparse
    // -----------------------------------------------------------------------

    /**
     * Absorb text insertion locally. The comment node grows to accommodate
     * the inserted text and schedules a deferred reparse. This avoids the
     * immediate {@code REMOVE_NODE} cascade that would trigger a full-file
     * PSI reparse.
     *
     * <p>The deferred reparse will call {@link #reparseNode} which validates
     * the comment boundaries using {@link KotlinLexer}.</p>
     */
    @Override
    public int textInserted(ReparseableDocument document, int nodePos,
            int insPos, int length, NodeStructureListener listener)
    {
        int newSize = getSize() + length;
        resize(newSize);
        document.scheduleReparse(insPos, length);
        return ALL_OK;
    }

    /**
     * Absorb text removal locally. The comment node shrinks and schedules
     * a deferred reparse for boundary validation.
     */
    @Override
    public int textRemoved(ReparseableDocument document, int nodePos,
            int delPos, int length, NodeStructureListener listener)
    {
        int newSize = getSize() - length;
        resize(newSize);
        document.scheduleReparse(delPos, 0);
        return ALL_OK;
    }

    // -----------------------------------------------------------------------
    // Reparse — validate comment boundaries with KotlinLexer
    // -----------------------------------------------------------------------

    /**
     * Validate the comment boundaries using {@link KotlinLexer}.
     *
     * <p>Creates a lexer over the node's document region and reads the first
     * token. If the token is still a comment of the same type (single-line
     * vs multi-line), the node adjusts its size and returns {@code ALL_OK}
     * or {@code NODE_SHRUNK}. If the comment structure is broken (e.g.,
     * the closing {@code * /} was deleted), returns {@code REMOVE_NODE}
     * to cascade to the parent for block-level or full-file reparse.</p>
     */
    @Override
    protected int reparseNode(ReparseableDocument document, int nodePos,
            int offset, int maxParse, NodeStructureListener listener)
    {
        // Compute line/column for the KotlinLexer position tracking
        Element map = document.getDefaultRootElement();
        int pline = map.getElementIndex(nodePos) + 1;
        int pcol = nodePos - map.getElement(pline - 1).getStartOffset() + 1;

        // Create a KotlinLexer over this node's text region
        Reader r = document.makeReader(nodePos, nodePos + getSize());
        KotlinLexer lexer = new KotlinLexer(r, pline, pcol, nodePos);

        LocatableToken commentToken = lexer.nextToken();

        // Check if the first token is still a comment
        if (!KotlinToken.isComment(commentToken.getType()))
        {
            return REMOVE_NODE;
        }

        // Determine if the re-lexed comment is single-line or multi-line
        boolean newSingleLine = (commentToken.getType() == KotlinToken.EOL_COMMENT);

        // Detect single-line <-> multi-line type change
        if (singleLine && !newSingleLine)
        {
            // Changed from single-line to multi-line — structural change
            return REMOVE_NODE;
        }
        else if (!singleLine && newSingleLine)
        {
            // Changed from multi-line to single-line — structural change
            return REMOVE_NODE;
        }

        // Update comment type (e.g., block -> KDoc or vice versa)
        TokenType newCommentType;
        if (commentToken.getType() == KotlinToken.DOC_COMMENT)
        {
            newCommentType = TokenType.COMMENT_JAVADOC;
        }
        else
        {
            newCommentType = TokenType.COMMENT_NORMAL;
        }
        commentType = newCommentType;
        singleLine = newSingleLine;

        // Compute new size from re-lexed token end position
        int newEnd = lineColToPos(document, commentToken.getEndLine(),
                commentToken.getEndColumn());
        int newSize = newEnd - nodePos;
        document.markSectionParsed(nodePos, newSize);

        if (getSize() != newSize)
        {
            setSize(newSize);
            return NODE_SHRUNK;
        }

        return ALL_OK;
    }

    /**
     * Convert a line/column position to an absolute document offset.
     * Matches the pattern used by Java's {@code CommentNode}.
     */
    private static int lineColToPos(ReparseableDocument document,
            int line, int col)
    {
        Element map = document.getDefaultRootElement();
        Element lineEl = map.getElement(line - 1);
        return lineEl.getStartOffset() + col - 1;
    }
}
