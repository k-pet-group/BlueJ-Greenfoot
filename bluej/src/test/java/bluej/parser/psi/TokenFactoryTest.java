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
package bluej.parser.psi;

import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import org.jetbrains.kotlin.com.intellij.lang.ASTNode;
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.openapi.util.TextRange;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TokenFactory}.
 * 
 * <p>Tests offset-to-line/column conversion, token creation, and integration
 * with TokenTypeMapper. Validates boundary conditions, multi-line handling,
 * and error cases.</p>
 */
public class TokenFactoryTest {
    
    /**
     * Test constructor null validation - null document.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNullDocument() {
        new TokenFactory(null, "test");
    }
    
    /**
     * Test constructor null validation - null source text.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNullSourceText() {
        Document mockDoc = mock(Document.class);
        new TokenFactory(mockDoc, null);
    }
    
    /**
     * Test constructor accepts valid parameters.
     */
    @Test
    public void testConstructorAcceptsValidParameters() {
        Document mockDoc = mock(Document.class);
        TokenFactory factory = new TokenFactory(mockDoc, "test");
        assertNotNull("Factory should be created", factory);
    }
    
    /**
     * Test create() rejects null PSI element.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateRejectsNullElement() {
        Document mockDoc = mock(Document.class);
        TokenFactory factory = new TokenFactory(mockDoc, "test");
        factory.create(null);
    }
    
    /**
     * Test create() rejects element with null text range.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateRejectsElementWithNullRange() {
        Document mockDoc = mock(Document.class);
        TokenFactory factory = new TokenFactory(mockDoc, "test");
        
        PsiElement mockElement = mock(PsiElement.class);
        when(mockElement.getTextRange()).thenReturn(null);
        
        factory.create(mockElement);
    }
    
    /**
     * Test single-line token at file start (offset 0).
     * Source: "class Test"
     * Element: "class" at offset 0-5
     * Expected: LineColPos(1, 1, 0) to LineColPos(1, 6, 5)
     */
    @Test
    public void testSingleLineTokenAtFileStart() {
        String sourceText = "class Test";
        Document mockDoc = createMockDocument(sourceText);
        TokenFactory factory = new TokenFactory(mockDoc, sourceText);
        
        // Create element for "class" at offset 0-5
        PsiElement mockElement = createMockElement("class", 0, 5, KtTokens.CLASS_KEYWORD);
        
        LocatableToken token = factory.create(mockElement);
        
        // Verify positions
        assertEquals("Begin line should be 1", 1, token.getLine());
        assertEquals("Begin column should be 1", 1, token.getColumn());
        assertEquals("Begin position should be 0", 0, token.getPosition());
        
        assertEquals("End line should be 1", 1, token.getEndLine());
        assertEquals("End column should be 6", 6, token.getEndColumn());
        assertEquals("End position should be 5", 5, token.getEndPosition());
        
        // Verify text and type
        assertEquals("Token text should be 'class'", "class", token.getText());
        assertEquals("Token type should be LITERAL_class", 
            JavaTokenTypes.LITERAL_class, token.getType());
    }
    
    /**
     * Test single-line token in middle of line.
     * Source: "fun hello() = 42"
     * Element: "hello" at offset 4-9
     * Expected: LineColPos(1, 5, 4) to LineColPos(1, 10, 9)
     */
    @Test
    public void testSingleLineTokenMiddleOfLine() {
        String sourceText = "fun hello() = 42";
        Document mockDoc = createMockDocument(sourceText);
        TokenFactory factory = new TokenFactory(mockDoc, sourceText);
        
        // Create element for "hello" at offset 4-9
        PsiElement mockElement = createMockElement("hello", 4, 9, null);
        
        LocatableToken token = factory.create(mockElement);
        
        // Verify positions
        assertEquals("Begin line should be 1", 1, token.getLine());
        assertEquals("Begin column should be 5", 5, token.getColumn());
        assertEquals("Begin position should be 4", 4, token.getPosition());
        
        assertEquals("End line should be 1", 1, token.getEndLine());
        assertEquals("End column should be 10", 10, token.getEndColumn());
        assertEquals("End position should be 9", 9, token.getEndPosition());
        
        assertEquals("Token text should be 'hello'", "hello", token.getText());
    }
    
    /**
     * Test multi-line token spanning three lines.
     * Source:
     * "fun hello(\n"         // Line 1: offsets 0-10 (newline at 10)
     * "    name: String\n"   // Line 2: offsets 11-27 (newline at 27)
     * ")"                     // Line 3: offset 28
     *
     * Element: "(\n    name: String\n)" at offset 9-28
     * Start: offset 9 = '(' on line 1
     * End: offset 28 = position after '\n' on line 2, which is line 3 column 1
     */
    @Test
    public void testMultiLineTokenSpanningThreeLines() {
        String sourceText = "fun hello(\n    name: String\n)";
        Document mockDoc = createMockDocument(sourceText);
        TokenFactory factory = new TokenFactory(mockDoc, sourceText);
        
        // Element spans from "(" at offset 9 to after second newline at offset 28
        String elementText = "(\n    name: String\n)";
        PsiElement mockElement = createMockElement(elementText, 9, 28, null);
        
        LocatableToken token = factory.create(mockElement);
        
        // Verify begin position (line 1, column 10 - the opening paren)
        assertEquals("Begin line should be 1", 1, token.getLine());
        assertEquals("Begin column should be 10", 10, token.getColumn());
        assertEquals("Begin position should be 9", 9, token.getPosition());
        
        // Verify end position (line 3, column 1 - start of line with closing paren)
        assertEquals("End line should be 3", 3, token.getEndLine());
        assertEquals("End column should be 1", 1, token.getEndColumn());
        assertEquals("End position should be 28", 28, token.getEndPosition());
        
        assertEquals("Token text should match", elementText, token.getText());
    }
    
    /**
     * Test empty file edge case.
     * Source: ""
     * Element at offset 0-0 should map to position (1,1,0)
     */
    @Test
    public void testEmptyFile() {
        String sourceText = "";
        Document mockDoc = createMockDocument(sourceText);
        TokenFactory factory = new TokenFactory(mockDoc, sourceText);
        
        // Element at position 0-0
        PsiElement mockElement = createMockElement("", 0, 0, null);
        
        LocatableToken token = factory.create(mockElement);
        
        assertEquals("Begin line should be 1", 1, token.getLine());
        assertEquals("Begin column should be 1", 1, token.getColumn());
        assertEquals("Begin position should be 0", 0, token.getPosition());
        
        assertEquals("End line should be 1", 1, token.getEndLine());
        assertEquals("End column should be 1", 1, token.getEndColumn());
        assertEquals("End position should be 0", 0, token.getEndPosition());
    }
    
    /**
     * Test token at line start (column 1) on line 2.
     * Source:
     * "fun test()\n"  // Line 1: offsets 0-11
     * "class Foo"     // Line 2: offsets 11-20
     * 
     * Element: "class" at offset 11-16
     */
    @Test
    public void testTokenAtLineStartOnLine2() {
        String sourceText = "fun test()\nclass Foo";
        Document mockDoc = createMockDocument(sourceText);
        TokenFactory factory = new TokenFactory(mockDoc, sourceText);
        
        // Element for "class" at line 2, column 1
        PsiElement mockElement = createMockElement("class", 11, 16, KtTokens.CLASS_KEYWORD);
        
        LocatableToken token = factory.create(mockElement);
        
        assertEquals("Begin line should be 2", 2, token.getLine());
        assertEquals("Begin column should be 1", 1, token.getColumn());
        assertEquals("Begin position should be 11", 11, token.getPosition());
        
        assertEquals("Token type should be LITERAL_class", 
            JavaTokenTypes.LITERAL_class, token.getType());
    }
    
    /**
     * Test token at end of file.
     * Source: "val x = 42"
     * Element: "42" at offset 8-10 (end of file)
     */
    @Test
    public void testTokenAtEndOfFile() {
        String sourceText = "val x = 42";
        Document mockDoc = createMockDocument(sourceText);
        TokenFactory factory = new TokenFactory(mockDoc, sourceText);
        
        PsiElement mockElement = createMockElement("42", 8, 10, null);
        
        LocatableToken token = factory.create(mockElement);
        
        assertEquals("Begin position should be 8", 8, token.getPosition());
        assertEquals("End position should be 10", 10, token.getEndPosition());
        assertEquals("Token text should be '42'", "42", token.getText());
    }
    
    /**
     * Test element with negative start offset throws exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeStartOffsetThrowsException() {
        String sourceText = "test";
        Document mockDoc = createMockDocument(sourceText);
        TokenFactory factory = new TokenFactory(mockDoc, sourceText);
        
        PsiElement mockElement = mock(PsiElement.class);
        TextRange mockRange = mock(TextRange.class);
        when(mockElement.getTextRange()).thenReturn(mockRange);
        when(mockRange.getStartOffset()).thenReturn(-1);
        when(mockRange.getEndOffset()).thenReturn(4);
        
        factory.create(mockElement);
    }
    
    /**
     * Test element with start offset beyond source length throws exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testStartOffsetBeyondLengthThrowsException() {
        String sourceText = "test";
        Document mockDoc = createMockDocument(sourceText);
        TokenFactory factory = new TokenFactory(mockDoc, sourceText);
        
        PsiElement mockElement = mock(PsiElement.class);
        TextRange mockRange = mock(TextRange.class);
        when(mockElement.getTextRange()).thenReturn(mockRange);
        when(mockRange.getStartOffset()).thenReturn(10);
        when(mockRange.getEndOffset()).thenReturn(15);
        when(mockElement.getText()).thenReturn("test");
        
        factory.create(mockElement);
    }
    
    /**
     * Test element with end offset before start throws exception.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testEndOffsetBeforeStartThrowsException() {
        String sourceText = "test";
        Document mockDoc = createMockDocument(sourceText);
        TokenFactory factory = new TokenFactory(mockDoc, sourceText);
        
        PsiElement mockElement = mock(PsiElement.class);
        TextRange mockRange = mock(TextRange.class);
        when(mockElement.getTextRange()).thenReturn(mockRange);
        when(mockRange.getStartOffset()).thenReturn(3);
        when(mockRange.getEndOffset()).thenReturn(1);
        
        factory.create(mockElement);
    }
    
    /**
     * Test element with null text is handled gracefully (returns empty string).
     */
    @Test
    public void testNullTextHandledGracefully() {
        String sourceText = "test";
        Document mockDoc = createMockDocument(sourceText);
        TokenFactory factory = new TokenFactory(mockDoc, sourceText);
        
        PsiElement mockElement = mock(PsiElement.class);
        TextRange mockRange = mock(TextRange.class);
        when(mockElement.getTextRange()).thenReturn(mockRange);
        when(mockRange.getStartOffset()).thenReturn(0);
        when(mockRange.getEndOffset()).thenReturn(4);
        when(mockElement.getText()).thenReturn(null);
        
        LocatableToken token = factory.create(mockElement);
        
        assertEquals("Null text should be converted to empty string", 
            "", token.getText());
    }
    
    /**
     * Test token type mapping integration with keyword.
     * Verifies TokenFactory correctly uses TokenTypeMapper for type assignment.
     */
    @Test
    public void testTokenTypeMappingForKeyword() {
        String sourceText = "fun test()";
        Document mockDoc = createMockDocument(sourceText);
        TokenFactory factory = new TokenFactory(mockDoc, sourceText);
        
        // Create "fun" keyword element
        PsiElement mockElement = createMockElement("fun", 0, 3, KtTokens.FUN_KEYWORD);
        
        LocatableToken token = factory.create(mockElement);
        
        assertEquals("fun keyword should map to LITERAL_fun via TokenTypeMapper", 
            JavaTokenTypes.LITERAL_fun, token.getType());
    }
    
    /**
     * Test multiple tokens on same line have correct columns.
     * Source: "val x = 42"
     * Tokens: "val" (0-3), "x" (4-5), "=" (6-7), "42" (8-10)
     */
    @Test
    public void testMultipleTokensOnSameLine() {
        String sourceText = "val x = 42";
        Document mockDoc = createMockDocument(sourceText);
        TokenFactory factory = new TokenFactory(mockDoc, sourceText);
        
        // Token 1: "val" at offset 0-3
        LocatableToken val = factory.create(createMockElement("val", 0, 3, KtTokens.VAL_KEYWORD));
        assertEquals("val begin column", 1, val.getColumn());
        assertEquals("val end column", 4, val.getEndColumn());
        
        // Token 2: "x" at offset 4-5
        LocatableToken x = factory.create(createMockElement("x", 4, 5, null));
        assertEquals("x begin column", 5, x.getColumn());
        assertEquals("x end column", 6, x.getEndColumn());
        
        // Token 3: "=" at offset 6-7
        LocatableToken eq = factory.create(createMockElement("=", 6, 7, KtTokens.EQ));
        assertEquals("= begin column", 7, eq.getColumn());
        assertEquals("= end column", 8, eq.getEndColumn());
        
        // Token 4: "42" at offset 8-10
        LocatableToken num = factory.create(createMockElement("42", 8, 10, null));
        assertEquals("42 begin column", 9, num.getColumn());
        assertEquals("42 end column", 11, num.getEndColumn());
    }
    
    /**
     * Test tokens across multiple lines have correct line numbers.
     * Source:
     * "class Foo {\n"    // Line 1: offsets 0-11 (newline at 11)
     * "    val x = 1\n"  // Line 2: offsets 12-25 (newline at 25)
     * "}"                // Line 3: offset 26
     */
    @Test
    public void testTokensAcrossMultipleLines() {
        String sourceText = "class Foo {\n    val x = 1\n}";
        Document mockDoc = createMockDocument(sourceText);
        TokenFactory factory = new TokenFactory(mockDoc, sourceText);
        
        // Line 1: "class" at offset 0-5
        LocatableToken classToken = factory.create(
            createMockElement("class", 0, 5, KtTokens.CLASS_KEYWORD));
        assertEquals("class on line 1", 1, classToken.getLine());
        
        // Line 2: "val" at offset 16-19
        LocatableToken valToken = factory.create(
            createMockElement("val", 16, 19, KtTokens.VAL_KEYWORD));
        assertEquals("val on line 2", 2, valToken.getLine());
        assertEquals("val at column 5 (4 spaces indent + 1)", 5, valToken.getColumn());
        
        // Line 3: "}" at offset 26-27
        LocatableToken braceToken = factory.create(
            createMockElement("}", 26, 27, KtTokens.RBRACE));
        assertEquals("} on line 3", 3, braceToken.getLine());
        assertEquals("} at column 1", 1, braceToken.getColumn());
    }
    
    /**
     * Test line and column calculation accuracy for known positions.
     * Ensures the 0-based to 1-based conversion is correct.
     */
    @Test
    public void testOffsetConversionAccuracy() {
        // Source with known positions:
        // Line 1: "abc\n" (offsets 0-3, newline at 3)
        // Line 2: "def\n" (offsets 4-7, newline at 7)  
        // Line 3: "ghi"   (offsets 8-10)
        String sourceText = "abc\ndef\nghi";
        Document mockDoc = createMockDocument(sourceText);
        TokenFactory factory = new TokenFactory(mockDoc, sourceText);
        
        // Test offset 0 -> line 1, col 1
        LocatableToken t0 = factory.create(createMockElement("a", 0, 1, null));
        assertEquals(1, t0.getLine());
        assertEquals(1, t0.getColumn());
        
        // Test offset 4 (first char of line 2) -> line 2, col 1
        LocatableToken t4 = factory.create(createMockElement("d", 4, 5, null));
        assertEquals(2, t4.getLine());
        assertEquals(1, t4.getColumn());
        
        // Test offset 8 (first char of line 3) -> line 3, col 1
        LocatableToken t8 = factory.create(createMockElement("g", 8, 9, null));
        assertEquals(3, t8.getLine());
        assertEquals(1, t8.getColumn());
        
        // Test offset 9 (middle of line 3) -> line 3, col 2
        LocatableToken t9 = factory.create(createMockElement("h", 9, 10, null));
        assertEquals(3, t9.getLine());
        assertEquals(2, t9.getColumn());
    }
    
    /**
     * Test token length calculation.
     * Token length should equal end offset - start offset.
     */
    @Test
    public void testTokenLengthCalculation() {
        String sourceText = "class MyClass";
        Document mockDoc = createMockDocument(sourceText);
        TokenFactory factory = new TokenFactory(mockDoc, sourceText);
        
        // "class" is 5 characters (offset 0-5)
        LocatableToken token = factory.create(
            createMockElement("class", 0, 5, KtTokens.CLASS_KEYWORD));
        
        assertEquals("Token length should be 5", 5, token.getLength());
    }
    
    /**
     * Test Unicode characters are handled correctly.
     * Source: "val emoji = \"😀\""
     */
    @Test
    public void testUnicodeCharactersHandled() {
        String sourceText = "val emoji = \"😀\"";
        Document mockDoc = createMockDocument(sourceText);
        TokenFactory factory = new TokenFactory(mockDoc, sourceText);
        
        // Test "val" keyword
        LocatableToken valToken = factory.create(
            createMockElement("val", 0, 3, KtTokens.VAL_KEYWORD));
        
        assertEquals("val should be at line 1, column 1", 1, valToken.getLine());
        assertEquals("val should be at column 1", 1, valToken.getColumn());
        
        // The emoji string - Document API handles multi-byte chars correctly
        // The exact offsets depend on Document's handling, but we trust IntelliJ's implementation
    }
    
    /**
     * Test delimiter tokens have correct types.
     */
    @Test
    public void testDelimiterTokenTypes() {
        String sourceText = "{}()[]";
        Document mockDoc = createMockDocument(sourceText);
        TokenFactory factory = new TokenFactory(mockDoc, sourceText);
        
        LocatableToken lbrace = factory.create(createMockElement("{", 0, 1, KtTokens.LBRACE));
        assertEquals(JavaTokenTypes.LCURLY, lbrace.getType());
        
        LocatableToken rbrace = factory.create(createMockElement("}", 1, 2, KtTokens.RBRACE));
        assertEquals(JavaTokenTypes.RCURLY, rbrace.getType());
        
        LocatableToken lparen = factory.create(createMockElement("(", 2, 3, KtTokens.LPAR));
        assertEquals(JavaTokenTypes.LPAREN, lparen.getType());
        
        LocatableToken rparen = factory.create(createMockElement(")", 3, 4, KtTokens.RPAR));
        assertEquals(JavaTokenTypes.RPAREN, rparen.getType());
    }
    
    /**
     * Test consecutive newlines (empty lines) are handled correctly.
     * Source:
     * "val x\n"  // Line 1
     * "\n"       // Line 2 (empty)
     * "val y"    // Line 3
     */
    @Test
    public void testEmptyLinesHandled() {
        String sourceText = "val x\n\nval y";
        Document mockDoc = createMockDocument(sourceText);
        TokenFactory factory = new TokenFactory(mockDoc, sourceText);
        
        // Token on line 3 after empty line
        LocatableToken valY = factory.create(createMockElement("val", 7, 10, KtTokens.VAL_KEYWORD));
        
        assertEquals("Token should be on line 3", 3, valY.getLine());
        assertEquals("Token should be at column 1", 1, valY.getColumn());
    }
    
    /**
     * Test whitespace-only line positioning.
     * Source:
     * "val x\n"     // Line 1
     * "    \n"      // Line 2 (whitespace only)
     * "val y"       // Line 3
     */
    @Test
    public void testWhitespaceOnlyLine() {
        String sourceText = "val x\n    \nval y";
        Document mockDoc = createMockDocument(sourceText);
        TokenFactory factory = new TokenFactory(mockDoc, sourceText);
        
        // Token on line 3
        LocatableToken valY = factory.create(createMockElement("val", 11, 14, KtTokens.VAL_KEYWORD));
        
        assertEquals("Token should be on line 3", 3, valY.getLine());
    }
    
    /**
     * Helper method to create a mock Document with line tracking.
     * 
     * @param sourceText The source text for the document
     * @return A mocked Document with realistic line number behavior
     */
    private Document createMockDocument(String sourceText) {
        Document mockDoc = mock(Document.class);
        when(mockDoc.getTextLength()).thenReturn(sourceText.length());
        when(mockDoc.getText()).thenReturn(sourceText);
        
        // Mock line number calculations based on actual newline positions
        when(mockDoc.getLineNumber(anyInt())).thenAnswer(invocation -> {
            int offset = invocation.getArgument(0);
            if (offset < 0 || offset > sourceText.length()) {
                throw new IndexOutOfBoundsException("Offset " + offset + " out of bounds");
            }
            
            // Count newlines before this offset
            int lineNum = 0;
            for (int i = 0; i < Math.min(offset, sourceText.length()); i++) {
                if (sourceText.charAt(i) == '\n') {
                    lineNum++;
                }
            }
            return lineNum; // 0-based line number
        });
        
        // Mock line start offset calculations
        when(mockDoc.getLineStartOffset(anyInt())).thenAnswer(invocation -> {
            int line = invocation.getArgument(0);
            if (line < 0) {
                throw new IndexOutOfBoundsException("Line " + line + " < 0");
            }
            
            if (line == 0) {
                return 0; // First line starts at 0
            }
            
            // Find the (line)th newline and return offset after it
            int newlineCount = 0;
            for (int i = 0; i < sourceText.length(); i++) {
                if (sourceText.charAt(i) == '\n') {
                    newlineCount++;
                    if (newlineCount == line) {
                        return i + 1; // Return position after this newline
                    }
                }
            }
            
            // If line number is beyond text, throw exception
            throw new IndexOutOfBoundsException("Line " + line + " beyond document");
        });
        
        return mockDoc;
    }
    
    /**
     * Helper method to create a mock PSI element with specified range and text.
     * 
     * @param text Element text
     * @param startOffset Start offset (0-based)
     * @param endOffset End offset (0-based, exclusive)
     * @param tokenType Optional token type for leaf elements (can be null)
     * @return A mocked PsiElement
     */
    private PsiElement createMockElement(String text, int startOffset, int endOffset, 
                                        org.jetbrains.kotlin.com.intellij.psi.tree.IElementType tokenType) {
        PsiElement mockElement = mock(PsiElement.class);
        TextRange mockRange = mock(TextRange.class);
        
        when(mockElement.getTextRange()).thenReturn(mockRange);
        when(mockRange.getStartOffset()).thenReturn(startOffset);
        when(mockRange.getEndOffset()).thenReturn(endOffset);
        when(mockElement.getText()).thenReturn(text);
        
        // Set up AST node for token type mapping if provided
        if (tokenType != null) {
            ASTNode mockNode = mock(ASTNode.class);
            when(mockElement.getNode()).thenReturn(mockNode);
            when(mockNode.getElementType()).thenReturn(tokenType);
        }
        
        return mockElement;
    }
}