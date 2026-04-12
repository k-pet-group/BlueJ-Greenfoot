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
package bluej.parser.kotlin;

import bluej.parser.Token;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for KotlinToken — verifies PSI IElementType to BlueJ Token.TokenType mapping.
 */
public class KotlinTokenTest
{
    @Test
    public void testControlFlowKeywordsMappedToKeyword1()
    {
        assertDisplayType(KotlinToken.KW_IF, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_ELSE, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_WHEN, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_FOR, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_WHILE, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_DO, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_RETURN, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_BREAK, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_CONTINUE, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_THROW, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_TRY, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_IS, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_AS, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_IN, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_NOT_IS, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_NOT_IN, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_TYPEOF, Token.TokenType.KEYWORD1);
    }

    @Test
    public void testModifierKeywordsMappedToKeyword1()
    {
        // Visibility modifiers → KEYWORD1 (matches Java)
        assertDisplayType(KotlinToken.KW_PRIVATE, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_PUBLIC, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_INTERNAL, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_PROTECTED, Token.TokenType.KEYWORD1);
        // Other modifiers → KEYWORD1
        assertDisplayType(KotlinToken.KW_ABSTRACT, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_OPEN, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_FINAL, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_SEALED, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_OVERRIDE, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_INNER, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_LATEINIT, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_DATA, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_VALUE, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_INLINE, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_CONST, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_SUSPEND, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_VARARG, Token.TokenType.KEYWORD1);
        assertDisplayType(KotlinToken.KW_OUT, Token.TokenType.KEYWORD1);
    }

    @Test
    public void testDeclarationKeywordsMappedToKeyword2()
    {
        // Type declarations
        assertDisplayType(KotlinToken.KW_CLASS, Token.TokenType.KEYWORD2);
        assertDisplayType(KotlinToken.KW_INTERFACE, Token.TokenType.KEYWORD2);
        assertDisplayType(KotlinToken.KW_ENUM, Token.TokenType.KEYWORD2);
        assertDisplayType(KotlinToken.KW_OBJECT, Token.TokenType.KEYWORD2);
        assertDisplayType(KotlinToken.KW_TYPE_ALIAS, Token.TokenType.KEYWORD2);
        assertDisplayType(KotlinToken.KW_PACKAGE, Token.TokenType.KEYWORD2);
        assertDisplayType(KotlinToken.KW_IMPORT, Token.TokenType.KEYWORD2);
        // Member declarations
        assertDisplayType(KotlinToken.KW_FUN, Token.TokenType.KEYWORD2);
        assertDisplayType(KotlinToken.KW_VAL, Token.TokenType.KEYWORD2);
        assertDisplayType(KotlinToken.KW_VAR, Token.TokenType.KEYWORD2);
        // Structural
        assertDisplayType(KotlinToken.KW_CONSTRUCTOR, Token.TokenType.KEYWORD2);
        assertDisplayType(KotlinToken.KW_INIT, Token.TokenType.KEYWORD2);
        assertDisplayType(KotlinToken.KW_COMPANION, Token.TokenType.KEYWORD2);
        assertDisplayType(KotlinToken.KW_WHERE, Token.TokenType.KEYWORD2);
        assertDisplayType(KotlinToken.KW_BY, Token.TokenType.KEYWORD2);
        assertDisplayType(KotlinToken.KW_GET, Token.TokenType.KEYWORD2);
        assertDisplayType(KotlinToken.KW_SET, Token.TokenType.KEYWORD2);
    }

    @Test
    public void testReferenceKeywordsMappedToKeyword3()
    {
        assertDisplayType(KotlinToken.KW_THIS, Token.TokenType.KEYWORD3);
        assertDisplayType(KotlinToken.KW_SUPER, Token.TokenType.KEYWORD3);
        assertDisplayType(KotlinToken.KW_NULL, Token.TokenType.KEYWORD3);
        assertDisplayType(KotlinToken.KW_TRUE, Token.TokenType.KEYWORD3);
        assertDisplayType(KotlinToken.KW_FALSE, Token.TokenType.KEYWORD3);
    }

    @Test
    public void testNumericLiteralsMappedToCharLiteral()
    {
        assertDisplayType(KotlinToken.INTEGER_LITERAL, Token.TokenType.CHAR_LITERAL);
        assertDisplayType(KotlinToken.FLOAT_LITERAL, Token.TokenType.CHAR_LITERAL);
        assertDisplayType(KotlinToken.CHARACTER_LITERAL, Token.TokenType.CHAR_LITERAL);
    }

    @Test
    public void testStringPartsMappedToStringLiteral()
    {
        assertDisplayType(KotlinToken.OPEN_QUOTE, Token.TokenType.STRING_LITERAL);
        assertDisplayType(KotlinToken.CLOSING_QUOTE, Token.TokenType.STRING_LITERAL);
        assertDisplayType(KotlinToken.REGULAR_STRING_PART, Token.TokenType.STRING_LITERAL);
        assertDisplayType(KotlinToken.ESCAPE_SEQUENCE, Token.TokenType.STRING_LITERAL);
        assertDisplayType(KotlinToken.SHORT_TEMPLATE_ENTRY_START, Token.TokenType.STRING_LITERAL);
        assertDisplayType(KotlinToken.LONG_TEMPLATE_ENTRY_START, Token.TokenType.STRING_LITERAL);
        assertDisplayType(KotlinToken.LONG_TEMPLATE_ENTRY_END, Token.TokenType.STRING_LITERAL);
    }

    @Test
    public void testCommentsMappedCorrectly()
    {
        assertDisplayType(KotlinToken.EOL_COMMENT, Token.TokenType.COMMENT_NORMAL);
        assertDisplayType(KotlinToken.BLOCK_COMMENT, Token.TokenType.COMMENT_NORMAL);
        assertDisplayType(KotlinToken.SHEBANG_COMMENT, Token.TokenType.COMMENT_NORMAL);
        assertDisplayType(KotlinToken.DOC_COMMENT, Token.TokenType.COMMENT_JAVADOC);
    }

    @Test
    public void testAnnotationAtMappedToLabel()
    {
        assertDisplayType(KotlinToken.AT, Token.TokenType.LABEL);
    }

    @Test
    public void testOperatorsAndIdentifiersMappedToDefault()
    {
        assertDisplayType(KotlinToken.IDENTIFIER, Token.TokenType.DEFAULT);
        assertDisplayType(KotlinToken.PLUS, Token.TokenType.DEFAULT);
        assertDisplayType(KotlinToken.MINUS, Token.TokenType.DEFAULT);
        assertDisplayType(KotlinToken.LPAR, Token.TokenType.DEFAULT);
        assertDisplayType(KotlinToken.RBRACE, Token.TokenType.DEFAULT);
        assertDisplayType(KotlinToken.DOT, Token.TokenType.DEFAULT);
        assertDisplayType(KotlinToken.ARROW, Token.TokenType.DEFAULT);
        assertDisplayType(KotlinToken.SAFE_ACCESS, Token.TokenType.DEFAULT);
        assertDisplayType(KotlinToken.ELVIS, Token.TokenType.DEFAULT);
    }

    @Test
    public void testPsiHardKeywordMapping()
    {
        assertEquals(KotlinToken.KW_VAL, KotlinToken.mapTokenType(KtTokens.VAL_KEYWORD));
        assertEquals(KotlinToken.KW_VAR, KotlinToken.mapTokenType(KtTokens.VAR_KEYWORD));
        assertEquals(KotlinToken.KW_FUN, KotlinToken.mapTokenType(KtTokens.FUN_KEYWORD));
        assertEquals(KotlinToken.KW_CLASS, KotlinToken.mapTokenType(KtTokens.CLASS_KEYWORD));
        assertEquals(KotlinToken.KW_IF, KotlinToken.mapTokenType(KtTokens.IF_KEYWORD));
        assertEquals(KotlinToken.KW_WHEN, KotlinToken.mapTokenType(KtTokens.WHEN_KEYWORD));
        assertEquals(KotlinToken.KW_RETURN, KotlinToken.mapTokenType(KtTokens.RETURN_KEYWORD));
    }

    @Test
    public void testPsiModifierKeywordMapping()
    {
        assertEquals(KotlinToken.KW_ABSTRACT, KotlinToken.mapTokenType(KtTokens.ABSTRACT_KEYWORD));
        assertEquals(KotlinToken.KW_OPEN, KotlinToken.mapTokenType(KtTokens.OPEN_KEYWORD));
        assertEquals(KotlinToken.KW_SEALED, KotlinToken.mapTokenType(KtTokens.SEALED_KEYWORD));
        assertEquals(KotlinToken.KW_DATA, KotlinToken.mapTokenType(KtTokens.DATA_KEYWORD));
        assertEquals(KotlinToken.KW_OVERRIDE, KotlinToken.mapTokenType(KtTokens.OVERRIDE_KEYWORD));
    }

    @Test
    public void testPsiVisibilityMapping()
    {
        assertEquals(KotlinToken.KW_PRIVATE, KotlinToken.mapTokenType(KtTokens.PRIVATE_KEYWORD));
        assertEquals(KotlinToken.KW_PUBLIC, KotlinToken.mapTokenType(KtTokens.PUBLIC_KEYWORD));
        assertEquals(KotlinToken.KW_INTERNAL, KotlinToken.mapTokenType(KtTokens.INTERNAL_KEYWORD));
        assertEquals(KotlinToken.KW_PROTECTED, KotlinToken.mapTokenType(KtTokens.PROTECTED_KEYWORD));
    }

    @Test
    public void testPsiLiteralMapping()
    {
        assertEquals(KotlinToken.INTEGER_LITERAL, KotlinToken.mapTokenType(KtTokens.INTEGER_LITERAL));
        assertEquals(KotlinToken.FLOAT_LITERAL, KotlinToken.mapTokenType(KtTokens.FLOAT_LITERAL));
        assertEquals(KotlinToken.CHARACTER_LITERAL, KotlinToken.mapTokenType(KtTokens.CHARACTER_LITERAL));
    }

    @Test
    public void testPsiStringMapping()
    {
        assertEquals(KotlinToken.OPEN_QUOTE, KotlinToken.mapTokenType(KtTokens.OPEN_QUOTE));
        assertEquals(KotlinToken.CLOSING_QUOTE, KotlinToken.mapTokenType(KtTokens.CLOSING_QUOTE));
        assertEquals(KotlinToken.REGULAR_STRING_PART, KotlinToken.mapTokenType(KtTokens.REGULAR_STRING_PART));
        assertEquals(KotlinToken.ESCAPE_SEQUENCE, KotlinToken.mapTokenType(KtTokens.ESCAPE_SEQUENCE));
    }

    @Test
    public void testPsiCommentMapping()
    {
        assertEquals(KotlinToken.EOL_COMMENT, KotlinToken.mapTokenType(KtTokens.EOL_COMMENT));
        assertEquals(KotlinToken.BLOCK_COMMENT, KotlinToken.mapTokenType(KtTokens.BLOCK_COMMENT));
        assertEquals(KotlinToken.DOC_COMMENT, KotlinToken.mapTokenType(KtTokens.DOC_COMMENT));
    }

    @Test
    public void testPsiOperatorMapping()
    {
        assertEquals(KotlinToken.PLUS, KotlinToken.mapTokenType(KtTokens.PLUS));
        assertEquals(KotlinToken.ARROW, KotlinToken.mapTokenType(KtTokens.ARROW));
        assertEquals(KotlinToken.SAFE_ACCESS, KotlinToken.mapTokenType(KtTokens.SAFE_ACCESS));
        assertEquals(KotlinToken.ELVIS, KotlinToken.mapTokenType(KtTokens.ELVIS));
        assertEquals(KotlinToken.EXCLEXCL, KotlinToken.mapTokenType(KtTokens.EXCLEXCL));
        assertEquals(KotlinToken.RANGE, KotlinToken.mapTokenType(KtTokens.RANGE));
    }

    @Test
    public void testNullPsiTypeMapsToEof()
    {
        assertEquals(KotlinToken.EOF, KotlinToken.mapTokenType(null));
    }

    @Test
    public void testUnknownPsiTypeFallsBackToIdentifier()
    {
        // Create a synthetic IElementType that isn't in KtTokens
        IElementType unknownType = new IElementType("UNKNOWN_TEST", null);
        assertEquals(KotlinToken.IDENTIFIER, KotlinToken.mapTokenType(unknownType));
    }

    @Test
    public void testIsKeyword()
    {
        assertTrue(KotlinToken.isKeyword(KotlinToken.KW_VAL));
        assertTrue(KotlinToken.isKeyword(KotlinToken.KW_ABSTRACT));
        assertTrue(KotlinToken.isKeyword(KotlinToken.KW_PRIVATE));
        assertFalse(KotlinToken.isKeyword(KotlinToken.IDENTIFIER));
        assertFalse(KotlinToken.isKeyword(KotlinToken.PLUS));
    }

    @Test
    public void testIsComment()
    {
        assertTrue(KotlinToken.isComment(KotlinToken.EOL_COMMENT));
        assertTrue(KotlinToken.isComment(KotlinToken.BLOCK_COMMENT));
        assertTrue(KotlinToken.isComment(KotlinToken.DOC_COMMENT));
        assertFalse(KotlinToken.isComment(KotlinToken.IDENTIFIER));
    }

    @Test
    public void testIsStringPart()
    {
        assertTrue(KotlinToken.isStringPart(KotlinToken.OPEN_QUOTE));
        assertTrue(KotlinToken.isStringPart(KotlinToken.REGULAR_STRING_PART));
        assertTrue(KotlinToken.isStringPart(KotlinToken.ESCAPE_SEQUENCE));
        assertFalse(KotlinToken.isStringPart(KotlinToken.IDENTIFIER));
    }

    private void assertDisplayType(int tokenType, Token.TokenType expected)
    {
        assertEquals("Token type " + tokenType + " should map to " + expected,
            expected, KotlinToken.toDisplayType(tokenType));
    }
}
