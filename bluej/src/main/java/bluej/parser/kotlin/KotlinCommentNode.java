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

import bluej.parser.Token;
import bluej.parser.Token.TokenType;
import bluej.parser.nodes.JavaParentNode;
import bluej.parser.nodes.NodeStructureListener;
import bluej.parser.nodes.ReparseableDocument;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A parse node representing a Kotlin comment (KDoc, block, or end-of-line)
 * in the scope tree. Returns a single comment-typed token to prevent keyword
 * highlighting inside comments.
 *
 * @author BlueJ Team
 */
@OnThread(Tag.FXPlatform)
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

}
