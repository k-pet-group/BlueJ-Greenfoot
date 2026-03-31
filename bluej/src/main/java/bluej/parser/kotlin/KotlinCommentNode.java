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

/**
 * A leaf parse node representing a Kotlin comment (KDoc, block comment, or
 * end-of-line comment) in the scope tree.
 *
 * <p>This node prevents keyword highlighting inside comments by returning a
 * single comment-typed token from {@link #getMarkTokensFor}. When
 * {@code JavaSyntaxView} encounters this node, it uses the returned token
 * directly instead of calling {@code tokenizeText()} — so keywords like
 * {@code class}, {@code if}, {@code is} inside comment text are never
 * re-lexed as keywords.</p>
 *
 * <p>This mirrors Java's {@link bluej.parser.nodes.CommentNode} approach,
 * but is simpler: all edit operations return {@link #REMOVE_NODE} to trigger
 * a full-file PSI reparse via {@link KotlinParsedCUNode}, consistent with
 * Kotlin's always-full-reparse strategy.</p>
 *
 * <p>Extends {@link JavaParentNode} (rather than {@code ParsedNode} directly)
 * because {@code ParsedNode}'s constructor is package-private to
 * {@code bluej.parser.nodes}. {@link JavaParentNode} provides a public
 * constructor accessible from {@code bluej.parser.kotlin}.</p>
 *
 * @author BlueJ Team
 */
public class KotlinCommentNode extends JavaParentNode
{
    private final TokenType commentType;

    /**
     * Create a comment node.
     *
     * @param parent      the parent node in the parse tree
     * @param commentType either {@link TokenType#COMMENT_JAVADOC} for KDoc
     *                    or {@link TokenType#COMMENT_NORMAL} for block/line comments
     */
    public KotlinCommentNode(JavaParentNode parent, TokenType commentType)
    {
        super(parent);
        this.commentType = commentType;
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

    @Override
    public int textInserted(ReparseableDocument document, int nodePos,
            int insPos, int length, NodeStructureListener listener)
    {
        // Trigger full PSI reparse by requesting node removal.
        // This bubbles up to KotlinParsedCUNode which does a full-file reparse.
        return REMOVE_NODE;
    }

    @Override
    public int textRemoved(ReparseableDocument document, int nodePos,
            int delPos, int length, NodeStructureListener listener)
    {
        return REMOVE_NODE;
    }

    @Override
    protected int reparseNode(ReparseableDocument document, int nodePos,
            int offset, int maxParse, NodeStructureListener listener)
    {
        // Always defer to full PSI reparse — Kotlin doesn't do incremental
        // comment re-validation like Java's CommentNode does.
        return REMOVE_NODE;
    }
}
