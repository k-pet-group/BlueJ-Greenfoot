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
import bluej.parser.nodes.ReparseableDocument;

import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.Reader;

/**
 * Parent node for Kotlin parse tree elements. Overrides
 * {@link JavaParentNode#tokenizeText} to use {@link KotlinLexer} and
 * {@link KotlinToken} mapping instead of the Java lexer, providing
 * per-line Kotlin syntax highlighting when called via
 * {@link #getMarkTokensFor}.
 *
 * <p>This class also supports configurable node types so that a single
 * class can represent Kotlin classes (TYPEDEF), functions (METHODDEF),
 * and control-flow scopes (SELECTION, ITERATION) — all with correct
 * Kotlin tokenization via virtual dispatch.</p>
 *
 * <p>This class does NOT implement {@link bluej.parser.entity.EntityResolver}
 * beyond what {@code JavaParentNode} provides — entity resolution
 * (code completion, type-aware features) is not supported for Kotlin MVP.</p>
 *
 * @author BlueJ Team
 */
@OnThread(Tag.FXPlatform)
public class KotlinParentNode extends JavaParentNode
{
    private final int nodeType;
    private final boolean isContainerNode;
    private String name;

    /**
     * Construct a Kotlin parent node with the given parent and default
     * NODETYPE_NONE.
     *
     * @param parent the parent node (may be null for root nodes)
     */
    public KotlinParentNode(JavaParentNode parent)
    {
        this(parent, NODETYPE_NONE);
    }

    /**
     * Construct a Kotlin parent node with the given parent and explicit
     * node type. The node is marked as a container if the type is
     * TYPEDEF, METHODDEF, SELECTION, or ITERATION.
     *
     * @param parent   the parent node (may be null for root nodes)
     * @param nodeType one of the NODETYPE_* constants from {@link bluej.parser.nodes.ParsedNode}
     */
    public KotlinParentNode(JavaParentNode parent, int nodeType)
    {
        super(parent);
        this.nodeType = nodeType;
        this.isContainerNode = (nodeType == NODETYPE_TYPEDEF
            || nodeType == NODETYPE_METHODDEF
            || nodeType == NODETYPE_SELECTION
            || nodeType == NODETYPE_ITERATION);
    }

    @Override
    public int getNodeType()
    {
        return nodeType;
    }

    @Override
    public String getName()
    {
        return name;
    }

    /**
     * Set the display name for this node (class name, function name, etc.).
     */
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public boolean isContainer()
    {
        return isContainerNode;
    }

    @Override
    protected boolean marksOwnEnd()
    {
        // Scope nodes in Kotlin contain their own closing brace
        return isContainerNode;
    }

    /**
     * Tokenize a text region using KotlinLexer and KotlinToken mapping.
     *
     * <p>Overrides the Java-specific tokenization in {@link JavaParentNode}.
     * Maps Kotlin tokens to {@link TokenType} for CSS class assignment:
     * <ul>
     *   <li>{@code val/var/fun/class/if/when/for/while/return/this/super/null/true/false} → KEYWORD1</li>
     *   <li>{@code open/override/abstract/data/sealed/companion} → KEYWORD2</li>
     *   <li>{@code private/public/internal/protected} → KEYWORD3</li>
     *   <li>String literals → STRING_LITERAL</li>
     *   <li>Comments → COMMENT_NORMAL / COMMENT_JAVADOC</li>
     * </ul>
     *
     * @param document the document being tokenized
     * @param pos      start position in document
     * @param length   length of region to tokenize
     * @return linked list of Token objects with TokenType assignments
     */
    @Override
    protected Token tokenizeText(ReparseableDocument document, int pos, int length)
    {
        return doKotlinTokenization(document, pos, length);
    }

    /**
     * Static tokenization helper shared by {@link KotlinParentNode} and
     * {@link KotlinParsedCUNode} (which extends {@code IncrementalParsingNode}
     * and cannot inherit this class).
     */
    static Token doKotlinTokenization(ReparseableDocument document, int pos, int length)
    {
        Reader dr = document.makeReader(pos, pos + length);
        KotlinLexer lexer = new KotlinLexer(dr);

        Token dummyTok = new Token(0, TokenType.END);
        Token token = dummyTok;

        int curcol = 1;
        int remaining = length;
        while (remaining > 0)
        {
            LocatableToken lt = lexer.nextToken();

            // EOF — done tokenizing
            if (lt.getType() == KotlinToken.EOF)
            {
                if (remaining > 0)
                {
                    token.next = new Token(remaining, TokenType.DEFAULT);
                    token = token.next;
                }
                break;
            }

            // Whitespace tokens — emit as DEFAULT
            if (lt.getType() == KotlinToken.WHITE_SPACE || lt.getType() == KotlinToken.DANGLING_NEWLINE)
            {
                // If whitespace crosses a line boundary, only count up to remaining
                int wsLen = lt.getLength();
                if (lt.getEndLine() > 1 || wsLen >= remaining)
                {
                    token.next = new Token(remaining, TokenType.DEFAULT);
                    token = token.next;
                    break;
                }
                token.next = new Token(wsLen, TokenType.DEFAULT);
                token = token.next;
                remaining -= wsLen;
                curcol += wsLen;
                continue;
            }

            // If the token starts beyond our current position (gap), fill with DEFAULT
            if (lt.getColumn() > curcol)
            {
                int gap = lt.getColumn() - curcol;
                token.next = new Token(gap, TokenType.DEFAULT);
                token = token.next;
                remaining -= gap;
                curcol += gap;
            }

            // Map Kotlin token type to display type
            TokenType tokType = KotlinToken.toDisplayType(lt.getType());

            int tokLen = lt.getLength();
            if (lt.getEndLine() > 1)
            {
                tokLen = remaining;
            }

            token.next = new Token(tokLen, tokType);
            token = token.next;
            remaining -= tokLen;
            curcol += tokLen;
        }

        token.next = new Token(0, TokenType.END);
        return dummyTok.next;
    }
}
