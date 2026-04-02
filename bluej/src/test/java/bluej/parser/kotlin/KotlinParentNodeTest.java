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

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import bluej.parser.Token;
import bluej.parser.Token.TokenType;
import bluej.parser.nodes.ParsedCUNode;
import bluej.parser.nodes.ReparseableDocument;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for KotlinParentNode — verifies that tokenizeText() produces
 * correct Token linked lists using KotlinLexer + KotlinToken mapping.
 *
 * <p>Also serves as a regression test for the JavaParentNode.tokenizeText()
 * static→virtual refactor: if tokenizeText() were still static, calling
 * it on a KotlinParentNode would produce Java tokenization instead of Kotlin.</p>
 */
public class KotlinParentNodeTest
{
    // Helper: minimal ReparseableDocument for testing tokenizeText()

    /**
     * Minimal implementation of ReparseableDocument that wraps a String.
     * Only makeReader() is needed for tokenizeText() testing.
     */
    private static class StringDocument implements ReparseableDocument
    {
        private final String content;

        StringDocument(String content)
        {
            this.content = content;
        }

        @Override
        public Reader makeReader(int startPos, int endPos)
        {
            return new StringReader(content.substring(startPos, endPos));
        }

        @Override
        public Element getDefaultRootElement()
        {
            return null;
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
            // no-op for testing
        }

        @Override
        public void flushReparseQueue()
        {
            // no-op for testing
        }

        @Override
        public void markSectionParsed(int pos, int size)
        {
            // no-op for testing
        }
    }

    // Helper: extract non-END tokens from linked list

    private List<Token> collectTokens(Token head)
    {
        List<Token> tokens = new ArrayList<>();
        Token t = head;
        while (t != null && t.id != TokenType.END)
        {
            tokens.add(t);
            t = t.next;
        }
        return tokens;
    }

    /**
     * Tokenize a Kotlin source string via KotlinParentNode.tokenizeText().
     */
    private List<Token> tokenizeKotlin(String source)
    {
        KotlinParentNode node = new KotlinParentNode(null);
        StringDocument doc = new StringDocument(source);
        Token head = node.tokenizeText(doc, 0, source.length());
        return collectTokens(head);
    }

    // Tests: KEYWORD1 — Control flow + Modifiers (matches Java's role-based scheme)

    @Test
    public void testHardKeywordsAreKeyword1()
    {
        // Control flow hard keywords → KEYWORD1
        String[] controlFlowKeywords = {
            "if", "when", "for", "while", "return", "do", "throw", "try",
            "else", "is", "in", "break", "continue", "as"
        };

        for (String kw : controlFlowKeywords)
        {
            List<Token> tokens = tokenizeKotlin(kw);
            Token kwToken = tokens.stream()
                .filter(t -> t.id != TokenType.DEFAULT)
                .findFirst()
                .orElse(null);
            assertNotNull("Keyword '" + kw + "' should produce a non-DEFAULT token", kwToken);
            assertEquals("Keyword '" + kw + "' should be KEYWORD1",
                TokenType.KEYWORD1, kwToken.id);
        }
    }

    @Test
    public void testFunKeywordIsKeyword2()
    {
        // fun is a declaration keyword → KEYWORD2
        List<Token> tokens = tokenizeKotlin("fun");
        Token funToken = tokens.stream()
            .filter(t -> t.id != TokenType.DEFAULT)
            .findFirst()
            .orElse(null);
        assertNotNull(funToken);
        assertEquals(TokenType.KEYWORD2, funToken.id);
        assertEquals(3, funToken.length);
    }

    @Test
    public void testValVarAreKeyword2()
    {
        // val/var are declaration keywords → KEYWORD2
        for (String kw : new String[]{"val", "var"})
        {
            List<Token> tokens = tokenizeKotlin(kw);
            Token kwToken = tokens.stream()
                .filter(t -> t.id != TokenType.DEFAULT)
                .findFirst()
                .orElse(null);
            assertNotNull(kwToken);
            assertEquals("'" + kw + "' should be KEYWORD2", TokenType.KEYWORD2, kwToken.id);
        }
    }

    // Tests: Modifiers → KEYWORD1 (matches Java)

    @Test
    public void testSoftKeywordsAreKeyword1()
    {
        // Modifiers → KEYWORD1 (aligned with Java's role-based scheme)
        String[] modifierKeywords = {
            "open", "override", "abstract", "data", "sealed",
            "inline", "operator", "infix", "const", "lateinit"
        };

        for (String kw : modifierKeywords)
        {
            List<Token> tokens = tokenizeKotlin(kw);
            Token kwToken = tokens.stream()
                .filter(t -> t.id != TokenType.DEFAULT)
                .findFirst()
                .orElse(null);
            assertNotNull("Modifier keyword '" + kw + "' should produce a non-DEFAULT token", kwToken);
            assertEquals("Modifier keyword '" + kw + "' should be KEYWORD1",
                TokenType.KEYWORD1, kwToken.id);
        }
    }

    // Tests: Visibility modifiers → KEYWORD1 (matches Java)

    @Test
    public void testVisibilityModifiersAreKeyword1()
    {
        // Visibility modifiers → KEYWORD1 (aligned with Java where public/private are modifiers)
        String[] visKeywords = {"private", "public", "internal", "protected"};

        for (String kw : visKeywords)
        {
            List<Token> tokens = tokenizeKotlin(kw);
            Token kwToken = tokens.stream()
                .filter(t -> t.id != TokenType.DEFAULT)
                .findFirst()
                .orElse(null);
            assertNotNull("Visibility keyword '" + kw + "' should produce a non-DEFAULT token", kwToken);
            assertEquals("Visibility keyword '" + kw + "' should be KEYWORD1",
                TokenType.KEYWORD1, kwToken.id);
        }
    }

    // Tests: Literals → this/super/null/true/false

    @Test
    public void testThisSuperNullTrueFalseAreKeyword()
    {
        // These are hard keywords that map to KEYWORD1 or KEYWORD3
        // (this/super/null/true/false → KEYWORD1 per KotlinToken)
        for (String kw : new String[]{"this", "super", "null", "true", "false"})
        {
            List<Token> tokens = tokenizeKotlin(kw);
            Token kwToken = tokens.stream()
                .filter(t -> t.id != TokenType.DEFAULT)
                .findFirst()
                .orElse(null);
            assertNotNull(kwToken);
            assertTrue("'" + kw + "' should be a keyword type, got " + kwToken.id,
                kwToken.id == TokenType.KEYWORD1 || kwToken.id == TokenType.KEYWORD3);
        }
    }

    // Tests: String literals → STRING_LITERAL

    @Test
    public void testStringLiteralTokenType()
    {
        List<Token> tokens = tokenizeKotlin("\"hello world\"");
        boolean hasStringLiteral = tokens.stream()
            .anyMatch(t -> t.id == TokenType.STRING_LITERAL);
        assertTrue("String literal should produce STRING_LITERAL token", hasStringLiteral);
    }

    @Test
    public void testMultilineStringLiteral()
    {
        // Only test the opening quote since multiline strings span lines
        List<Token> tokens = tokenizeKotlin("\"\"\"hello\"\"\"");
        boolean hasStringLiteral = tokens.stream()
            .anyMatch(t -> t.id == TokenType.STRING_LITERAL);
        assertTrue("Triple-quoted string should produce STRING_LITERAL token", hasStringLiteral);
    }

    // Tests: Comments → COMMENT_NORMAL / COMMENT_JAVADOC

    @Test
    public void testLineComment()
    {
        List<Token> tokens = tokenizeKotlin("// a comment");
        boolean hasComment = tokens.stream()
            .anyMatch(t -> t.id == TokenType.COMMENT_NORMAL);
        assertTrue("Line comment should produce COMMENT_NORMAL token", hasComment);
    }

    @Test
    public void testBlockComment()
    {
        List<Token> tokens = tokenizeKotlin("/* block */");
        boolean hasComment = tokens.stream()
            .anyMatch(t -> t.id == TokenType.COMMENT_NORMAL);
        assertTrue("Block comment should produce COMMENT_NORMAL token", hasComment);
    }

    @Test
    public void testKDocComment()
    {
        List<Token> tokens = tokenizeKotlin("/** KDoc */");
        boolean hasKDoc = tokens.stream()
            .anyMatch(t -> t.id == TokenType.COMMENT_JAVADOC);
        assertTrue("KDoc comment should produce COMMENT_JAVADOC token", hasKDoc);
    }

    // Tests: Numeric literals

    @Test
    public void testNumericLiteral()
    {
        List<Token> tokens = tokenizeKotlin("42");
        Token numToken = tokens.stream()
            .filter(t -> t.id != TokenType.DEFAULT)
            .findFirst()
            .orElse(null);
        assertNotNull(numToken);
        assertEquals(TokenType.CHAR_LITERAL, numToken.id); // numerics map to CHAR_LITERAL
    }

    // Tests: Token chain structure

    @Test
    public void testTokenChainEndsWithEND()
    {
        KotlinParentNode node = new KotlinParentNode(null);
        String source = "val x = 1";
        StringDocument doc = new StringDocument(source);
        Token head = node.tokenizeText(doc, 0, source.length());

        // Walk to the end
        Token t = head;
        while (t.next != null && t.id != TokenType.END)
        {
            t = t.next;
        }
        assertEquals("Token chain should end with END", TokenType.END, t.id);
    }

    @Test
    public void testTokenLengthsSumToSourceLength()
    {
        String source = "fun main() { println(42) }";
        List<Token> tokens = tokenizeKotlin(source);
        int totalLength = tokens.stream().mapToInt(t -> t.length).sum();
        assertEquals("Token lengths should sum to source length", source.length(), totalLength);
    }

    // Tests: Mixed Kotlin code

    @Test
    public void testMixedKotlinLine()
    {
        // "val x = 42" should produce: KEYWORD2("val") DEFAULT(" ") DEFAULT("x") DEFAULT(" = ") CHAR_LITERAL("42")
        String source = "val x = 42";
        List<Token> tokens = tokenizeKotlin(source);

        // First non-whitespace token should be KEYWORD2 for "val" (declaration keyword)
        Token first = tokens.get(0);
        assertEquals(TokenType.KEYWORD2, first.id);
        assertEquals(3, first.length); // "val" is 3 chars

        // Total length should match
        int totalLength = tokens.stream().mapToInt(t -> t.length).sum();
        assertEquals(source.length(), totalLength);
    }

    @Test
    public void testFunctionDeclaration()
    {
        String source = "fun greet(name: String)";
        List<Token> tokens = tokenizeKotlin(source);

        // First token should be KEYWORD2 for "fun" (declaration keyword)
        Token first = tokens.get(0);
        assertEquals(TokenType.KEYWORD2, first.id);
        assertEquals(3, first.length);

        // Total length should match
        int totalLength = tokens.stream().mapToInt(t -> t.length).sum();
        assertEquals(source.length(), totalLength);
    }

    // Tests: Empty / edge cases

    @Test
    public void testEmptySource()
    {
        KotlinParentNode node = new KotlinParentNode(null);
        StringDocument doc = new StringDocument("");
        Token head = node.tokenizeText(doc, 0, 0);
        // Should return a chain ending in END with no content
        assertNotNull(head);
        assertEquals(TokenType.END, head.id);
    }

    @Test
    public void testIdentifierOnly()
    {
        List<Token> tokens = tokenizeKotlin("myVariable");
        assertEquals(1, tokens.size());
        assertEquals(TokenType.DEFAULT, tokens.get(0).id);
        assertEquals(10, tokens.get(0).length);
    }

    // Tests: Virtual dispatch (regression test for static→virtual refactor)

    @Test
    public void testVirtualDispatchUsesKotlinLexer()
    {
        // "fun" is a Kotlin keyword (KEYWORD2 — declaration) but NOT a Java keyword.
        // If tokenizeText() were still static, it would be Java's tokenization
        // which would return DEFAULT for "fun".
        KotlinParentNode node = new KotlinParentNode(null);
        StringDocument doc = new StringDocument("fun");
        Token head = node.tokenizeText(doc, 0, 3);
        List<Token> tokens = collectTokens(head);

        Token funToken = tokens.stream()
            .filter(t -> t.id != TokenType.DEFAULT)
            .findFirst()
            .orElse(null);
        assertNotNull("'fun' should be tokenized as a keyword by KotlinParentNode", funToken);
        assertEquals(TokenType.KEYWORD2, funToken.id);
    }

    @Test
    public void testVirtualDispatchValIsKeyword()
    {
        // "val" is a Kotlin keyword but Java would tokenize it as DEFAULT
        KotlinParentNode node = new KotlinParentNode(null);
        StringDocument doc = new StringDocument("val");
        Token head = node.tokenizeText(doc, 0, 3);
        List<Token> tokens = collectTokens(head);

        Token valToken = tokens.stream()
            .filter(t -> t.id != TokenType.DEFAULT)
            .findFirst()
            .orElse(null);
        assertNotNull("'val' should be tokenized as a keyword by KotlinParentNode", valToken);
        assertEquals(TokenType.KEYWORD2, valToken.id);
    }
}
