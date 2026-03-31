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
import bluej.parser.nodes.ReparseableDocument;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Parse tree node for multiline triple-quoted Kotlin strings
 * ({@code """..."""}).
 *
 * <p>Extends {@link KotlinParentNode} so the inherited
 * {@code getMarkTokensFor()} tree walk handles children (template
 * expressions) normally. Only {@link #tokenizeText} is overridden to
 * return {@link TokenType#STRING_LITERAL} for gap content — plain string
 * text between template expressions renders as green.</p>
 *
 * <p>Child {@link KotlinParentNode} nodes cover template expression bodies:
 * <ul>
 *   <li>{@code $name} → child spanning the identifier (normal Kotlin
 *       tokenization → black)</li>
 *   <li>{@code ${expr}} → child spanning the expression body (normal
 *       tokenization)</li>
 * </ul>
 * The {@code $} and {@code ${…}} delimiters are part of the gap
 * ({@code STRING_LITERAL} → green).</p>
 *
 * <p><b>Scope rendering:</b> Marked as inner ({@code setInner(true)}) so
 * that {@code JavaSyntaxView}'s scope walker treats it like other scope
 * nodes (if/for/when/while). It receives neutral C3/BK coloring, blending
 * with the enclosing scope background. Without this, its template
 * expression children would pollute the scope stack and break scope
 * background rendering on subsequent lines.</p>
 *
 * <p><b>Edit handling:</b> inherits {@code ParentParsedNode.textInserted()/
 * textRemoved()} (absorb edit, schedule deferred reparse) and
 * {@code ParentParsedNode.reparseNode()} (returns {@code REMOVE_NODE}).
 * This is the same strategy used by all Kotlin scope nodes — edits
 * cascade up to {@link KotlinParsedCUNode} for full PSI reparse.</p>
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

    // No textInserted/textRemoved/reparseNode overrides needed.
    // Inherited ParentParsedNode behavior: absorb edit → deferred reparse
    // → REMOVE_NODE → cascades up to KotlinParsedCUNode for full PSI reparse.
    // Consistent with all other Kotlin scope nodes.
}
