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
import bluej.parser.nodes.JavaParentNode;
import bluej.parser.nodes.NodeStructureListener;
import bluej.parser.nodes.ReparseableDocument;

import threadchecker.OnThread;
import threadchecker.Tag;

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
 * <p><b>Edit handling:</b> inherits {@code ParentParsedNode.textInserted()/
 * textRemoved()} (absorb edit, schedule deferred reparse). Overrides
 * {@link #reparseNode} to always return {@code REMOVE_NODE}, which
 * cascades the reparse up to the parent node (inner body or root) for
 * a PSI-based rebuild. This is the same simple strategy used by
 * {@link KotlinStringNode} &mdash; correct and maintainable at the
 * cost of ~5&ndash;100ms per comment edit (vs ~0.1ms with a smart
 * KotlinLexer-based Tier 1 reparse). Educational files are small
 * enough that this is acceptable.</p>
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
    private final TokenType commentType;

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

    /**
     * Always return {@code REMOVE_NODE} so the parent node reparses.
     *
     * <p>The parent (inner body node via {@code createBlock()} or root via
     * {@code createFile()}) rebuilds the tree from PSI, which will create
     * a fresh {@code KotlinCommentNode} with the correct boundaries.</p>
     */
    @Override
    protected int reparseNode(ReparseableDocument document, int nodePos,
            int offset, int maxParse, NodeStructureListener listener)
    {
        return REMOVE_NODE;
    }

    // textInserted/textRemoved: inherited from ParentParsedNode
    // (absorb edit, resize, schedule deferred reparse → reparseNode() → REMOVE_NODE)
}
