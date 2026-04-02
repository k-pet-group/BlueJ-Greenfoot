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
 * Parse tree node for multiline triple-quoted Kotlin strings
 * ({@code """..."""}). Tokenizes gap content as string literals and
 * delegates template expression bodies to child nodes for normal
 * Kotlin tokenization.
 *
 * @author BlueJ Team
 */
@OnThread(Tag.FXPlatform)
public class KotlinStringNode extends KotlinParentNode
{
    /**
     * Create a string node for a multiline triple-quoted string.
     *
     * @param parent the parent node in the parse tree
     */
    public KotlinStringNode(JavaParentNode parent)
    {
        super(parent, NODETYPE_NONE);
        setInner(true);
    }

    @Override
    public boolean isContainer()
    {
        return false;
    }

    @Override
    protected boolean marksOwnEnd()
    {
        return true;
    }

    /**
     * Return {@link TokenType#STRING_LITERAL} tokens for all gap content.
     * This ensures plain string text between template expressions renders
     * as green, while child nodes handle template bodies via normal
     * Kotlin tokenization.
     */
    @Override
    protected Token tokenizeText(ReparseableDocument document, int pos, int length)
    {
        Token tok = new Token(length, TokenType.STRING_LITERAL);
        tok.next = new Token(0, TokenType.END);
        return tok;
    }

    /**
     * Always return {@code REMOVE_NODE} so the parent node reparses.
     *
     * <p>This override is critical: without it, the inherited
     * {@link KotlinParentNode#reparseNode} would attempt block-level PSI
     * reparse via {@code createBlock()} on the string's text content,
     * because this node passes all its guards ({@code isInner()=true},
     * {@code isContainer()=false}, parent is not TYPEDEF). That would
     * treat string content as Kotlin code, rebuild children as code
     * scopes inside the string node, and then {@link #tokenizeText}
     * would render everything (including real code after the string)
     * as {@code STRING_LITERAL}.</p>
     */
    @Override
    protected int reparseNode(ReparseableDocument document, int nodePos,
            int offset, int maxParse, NodeStructureListener listener)
    {
        return REMOVE_NODE;
    }
}
