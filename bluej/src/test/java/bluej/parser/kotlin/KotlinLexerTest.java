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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import bluej.parser.lexer.LocatableToken;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for KotlinLexer — PSI wrapper producing LocatableTokens.
 */
public class KotlinLexerTest
{
    // Helper: tokenize and return non-whitespace tokens

    private List<LocatableToken> tokenize(String source)
    {
        KotlinLexer lexer = new KotlinLexer(source);
        List<LocatableToken> tokens = new ArrayList<>();
        while (true)
        {
            LocatableToken token = lexer.nextToken();
            if (token.getType() == KotlinToken.EOF)
            {
                break;
            }
            if (token.getType() != KotlinToken.WHITE_SPACE
                && token.getType() != KotlinToken.DANGLING_NEWLINE)
            {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private List<LocatableToken> tokenizeAll(String source)
    {
        KotlinLexer lexer = new KotlinLexer(source);
        List<LocatableToken> tokens = new ArrayList<>();
        while (true)
        {
            LocatableToken token = lexer.nextToken();
            if (token.getType() == KotlinToken.EOF)
            {
                break;
            }
            tokens.add(token);
        }
        return tokens;
    }

    // Basic tokenization

    @Test
    public void testEmptyInput()
    {
        KotlinLexer lexer = new KotlinLexer("");
        LocatableToken token = lexer.nextToken();
        assertEquals(KotlinToken.EOF, token.getType());
    }

    @Test
    public void testSimpleVariableDeclaration()
    {
        List<LocatableToken> tokens = tokenize("val x: Int = 5");
        assertTrue(tokens.size() >= 6);
        assertEquals(KotlinToken.KW_VAL, tokens.get(0).getType());
        assertEquals("val", tokens.get(0).getText());
        assertEquals(KotlinToken.IDENTIFIER, tokens.get(1).getType());
        assertEquals("x", tokens.get(1).getText());
        assertEquals(KotlinToken.COLON, tokens.get(2).getType());
        assertEquals(KotlinToken.IDENTIFIER, tokens.get(3).getType());
        assertEquals("Int", tokens.get(3).getText());
        assertEquals(KotlinToken.EQ, tokens.get(4).getType());
        assertEquals(KotlinToken.INTEGER_LITERAL, tokens.get(5).getType());
        assertEquals("5", tokens.get(5).getText());
    }

    @Test
    public void testFunctionDeclaration()
    {
        List<LocatableToken> tokens = tokenize("fun greet(name: String): String");
        assertEquals(KotlinToken.KW_FUN, tokens.get(0).getType());
        assertEquals(KotlinToken.IDENTIFIER, tokens.get(1).getType());
        assertEquals("greet", tokens.get(1).getText());
        assertEquals(KotlinToken.LPAR, tokens.get(2).getType());
    }

    @Test
    public void testClassDeclaration()
    {
        List<LocatableToken> tokens = tokenize("open class Dog(val name: String) : Animal()");
        assertEquals(KotlinToken.KW_OPEN, tokens.get(0).getType());
        assertEquals(KotlinToken.KW_CLASS, tokens.get(1).getType());
        assertEquals(KotlinToken.IDENTIFIER, tokens.get(2).getType());
        assertEquals("Dog", tokens.get(2).getText());
    }

    // Comments

    @Test
    public void testLineComment()
    {
        List<LocatableToken> tokens = tokenizeAll("// this is a comment");
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == KotlinToken.EOL_COMMENT));
    }

    @Test
    public void testBlockComment()
    {
        List<LocatableToken> tokens = tokenizeAll("/* block comment */");
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == KotlinToken.BLOCK_COMMENT));
    }

    @Test
    public void testKdocComment()
    {
        List<LocatableToken> tokens = tokenizeAll("/** KDoc comment */");
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == KotlinToken.DOC_COMMENT));
    }

    // String literals

    @Test
    public void testSimpleString()
    {
        List<LocatableToken> tokens = tokenizeAll("\"hello\"");
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == KotlinToken.OPEN_QUOTE));
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == KotlinToken.REGULAR_STRING_PART));
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == KotlinToken.CLOSING_QUOTE));
    }

    @Test
    public void testStringTemplate()
    {
        List<LocatableToken> tokens = tokenizeAll("\"Hello, $name!\"");
        // Should contain: OPEN_QUOTE, REGULAR_STRING_PART("Hello, "),
        // SHORT_TEMPLATE_ENTRY_START($), IDENTIFIER(name),
        // REGULAR_STRING_PART("!"), CLOSING_QUOTE
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == KotlinToken.SHORT_TEMPLATE_ENTRY_START));
    }

    @Test
    public void testRawString()
    {
        List<LocatableToken> tokens = tokenizeAll("\"\"\"raw\nstring\"\"\"");
        // Raw strings still use OPEN_QUOTE/CLOSING_QUOTE/REGULAR_STRING_PART
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == KotlinToken.OPEN_QUOTE));
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == KotlinToken.CLOSING_QUOTE));
    }

    // Kotlin-specific operators

    @Test
    public void testKotlinOperators()
    {
        List<LocatableToken> tokens = tokenize("a?.b ?: c!!");
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == KotlinToken.SAFE_ACCESS));
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == KotlinToken.ELVIS));
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == KotlinToken.EXCLEXCL));
    }

    @Test
    public void testRangeOperator()
    {
        List<LocatableToken> tokens = tokenize("1..10");
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == KotlinToken.RANGE));
    }

    @Test
    public void testArrowOperator()
    {
        List<LocatableToken> tokens = tokenize("{ x -> x + 1 }");
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == KotlinToken.ARROW));
    }

    // Position tracking

    @Test
    public void testPositionTrackingSingleLine()
    {
        KotlinLexer lexer = new KotlinLexer("val x");
        LocatableToken val = lexer.nextToken();
        assertEquals(1, val.getLine());
        assertEquals(1, val.getColumn());
        assertEquals(0, val.getPosition());
        assertEquals(3, val.getLength()); // "val" is 3 chars
    }

    @Test
    public void testPositionTrackingMultiLine()
    {
        KotlinLexer lexer = new KotlinLexer("val x\nvar y");
        // Skip tokens until we get to 'var' on line 2
        List<LocatableToken> tokens = new ArrayList<>();
        while (true)
        {
            LocatableToken t = lexer.nextToken();
            if (t.getType() == KotlinToken.EOF) {
                break;
            }
            tokens.add(t);
        }

        // Find 'var' token
        LocatableToken varToken = tokens.stream()
            .filter(t -> t.getType() == KotlinToken.KW_VAR)
            .findFirst()
            .orElse(null);

        assertNotNull("Should find 'var' token", varToken);
        assertEquals("'var' should be on line 2", 2, varToken.getLine());
    }

    @Test
    public void testLineNumbersAre1Based()
    {
        KotlinLexer lexer = new KotlinLexer("val");
        LocatableToken token = lexer.nextToken();
        assertEquals("Line numbers should be 1-based", 1, token.getLine());
        assertEquals("Column numbers should be 1-based", 1, token.getColumn());
    }

    // Reader constructor

    @Test
    public void testReaderConstructor()
    {
        StringReader reader = new StringReader("fun main()");
        KotlinLexer lexer = new KotlinLexer(reader);
        LocatableToken token = lexer.nextToken();
        assertEquals(KotlinToken.KW_FUN, token.getType());
        assertEquals("fun", token.getText());
    }

    @Test
    public void testReaderWithPosition()
    {
        StringReader reader = new StringReader("val x");
        KotlinLexer lexer = new KotlinLexer(reader, 5, 3, 100);
        LocatableToken token = lexer.nextToken();
        assertEquals(5, token.getLine());
        assertEquals(3, token.getColumn());
        assertEquals(100, token.getPosition());
    }

    // Edge cases

    @Test
    public void testOnlyWhitespace()
    {
        List<LocatableToken> tokens = tokenize("   \n\n  ");
        assertTrue("Only whitespace should produce no non-whitespace tokens", tokens.isEmpty());
    }

    @Test
    public void testOnlyComments()
    {
        List<LocatableToken> tokens = tokenize("// comment\n/* block */");
        assertTrue(tokens.size() == 2);
        assertEquals(KotlinToken.EOL_COMMENT, tokens.get(0).getType());
        assertEquals(KotlinToken.BLOCK_COMMENT, tokens.get(1).getType());
    }

    @Test
    public void testAllKeywordsInContext()
    {
        // A realistic Kotlin snippet with multiple keywords
        String source = "abstract class Shape {\n"
            + "    open fun area(): Double = 0.0\n"
            + "    override fun toString(): String = \"Shape\"\n"
            + "}";
        List<LocatableToken> tokens = tokenize(source);
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == KotlinToken.KW_ABSTRACT));
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == KotlinToken.KW_CLASS));
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == KotlinToken.KW_OPEN));
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == KotlinToken.KW_FUN));
        assertTrue(tokens.stream().anyMatch(t -> t.getType() == KotlinToken.KW_OVERRIDE));
    }
}
