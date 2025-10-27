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

import bluej.parser.lexer.LocatableToken;
import org.jetbrains.kotlin.com.intellij.openapi.editor.Document;
import org.jetbrains.kotlin.com.intellij.psi.PsiComment;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.kotlin.psi.*;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Fixture tests for {@link TokenFactory} with exact position validation.
 * 
 * <p>Validates TokenFactory's position calculation accuracy by testing known
 * leaf tokens at documented positions in test corpus files. Uses exact
 * position assertions (not lenient {@code >=1} checks).</p>
 * 
 * <p><strong>Approach:</strong> Each test finds specific leaf PSI tokens
 * (keywords, identifiers, operators) and validates their exact line/column
 * positions match expected values from the test corpus.</p>
 * 
 * <p>Test corpus files: {@code test/resources/bluej/parser/psi/tokens/}</p>
 * 
 * @see TokenFactory
 * @see PsiEnvironment
 */
public class TokenFactoryIntegrationTest {
    
    private PsiEnvironment env;
    
    /**
     * Initialize PSI environment before each test.
     */
    @Before
    public void setUp() {
        env = PsiEnvironment.getInstance();
        assertTrue("PSI environment should be initialized", env.isInitialized());
    }
    
    /**
     * Test exact token positions in SimplePositions.kt.
     *
     * <p>Validates TokenFactory produces exact positions for known leaf tokens:
     * keywords (class, val, fun), identifiers, and operators.</p>
     * 
     * <p>Positions are validated against documented locations in SimplePositions.kt.</p>
     */
    @Test
    public void testExactTokenPositions() throws Exception {
        String content = loadTestResource("psi/tokens/SimplePositions.kt");
        KtFile ktFile = env.parseFile("SimplePositions.kt", content);
        Document doc = createMockDocument(content);
        TokenFactory factory = new TokenFactory(doc, content);
        
        // Test "class" keyword - Line 2, Col 1
        PsiElement classKeyword = findFirstLeafToken(ktFile, "class");
        assertNotNull("Should find 'class' keyword", classKeyword);
        LocatableToken token = factory.create(classKeyword);
        assertEquals("class keyword text", "class", token.getText());
        assertEquals("class keyword line", 2, token.getLine());
        assertEquals("class keyword column", 1, token.getColumn());
        
        // Test identifier "SimpleTest" - Line 2, Col 7
        PsiElement identifier = findFirstLeafToken(ktFile, "SimpleTest");
        assertNotNull("Should find 'SimpleTest' identifier", identifier);
        token = factory.create(identifier);
        assertEquals("SimpleTest text", "SimpleTest", token.getText());
        assertEquals("SimpleTest line", 2, token.getLine());
        assertEquals("SimpleTest column", 7, token.getColumn());
        
        // Test "val" keyword - Line 4, Col 5
        PsiElement valKeyword = findFirstLeafToken(ktFile, "val");
        assertNotNull("Should find 'val' keyword", valKeyword);
        token = factory.create(valKeyword);
        assertEquals("val keyword text", "val", token.getText());
        assertEquals("val keyword line", 4, token.getLine());
        assertEquals("val keyword column", 5, token.getColumn());
        
        // Test identifier "x" - Line 4, Col 9
        PsiElement xIdentifier = findFirstLeafToken(ktFile, "x");
        assertNotNull("Should find 'x' identifier", xIdentifier);
        token = factory.create(xIdentifier);
        assertEquals("x identifier text", "x", token.getText());
        assertEquals("x identifier line", 4, token.getLine());
        assertEquals("x identifier column", 9, token.getColumn());
        
        // Test "=" operator - Line 4, Col 11
        PsiElement assignOp = findFirstLeafToken(ktFile, "=");
        assertNotNull("Should find '=' operator", assignOp);
        token = factory.create(assignOp);
        assertEquals("= operator text", "=", token.getText());
        assertEquals("= operator line", 4, token.getLine());
        assertEquals("= operator column", 11, token.getColumn());
        
        // Test "fun" keyword inside class - Line 6, Col 5
        PsiElement funKeyword = findFirstLeafToken(ktFile, "fun");
        assertNotNull("Should find 'fun' keyword", funKeyword);
        token = factory.create(funKeyword);
        assertEquals("fun keyword text", "fun", token.getText());
        assertEquals("fun keyword line", 6, token.getLine());
        assertEquals("fun keyword column", 5, token.getColumn());
        
        // Test "simpleMethod" identifier - Line 6, Col 9
        PsiElement methodName = findFirstLeafToken(ktFile, "simpleMethod");
        assertNotNull("Should find 'simpleMethod' identifier", methodName);
        token = factory.create(methodName);
        assertEquals("simpleMethod text", "simpleMethod", token.getText());
        assertEquals("simpleMethod line", 6, token.getLine());
        assertEquals("simpleMethod column", 9, token.getColumn());
        
        // Test number literal "42" - Line 4, Col 13
        PsiElement numberLiteral = findFirstLeafToken(ktFile, "42");
        assertNotNull("Should find '42' literal", numberLiteral);
        token = factory.create(numberLiteral);
        assertEquals("42 literal text", "42", token.getText());
        assertEquals("42 literal line", 4, token.getLine());
        assertEquals("42 literal column", 13, token.getColumn());
    }
    
    /**
     * Test multiline token positions in MultilineElements.kt.
     *
     * <p>Validates position tracking for elements spanning multiple lines,
     * including multiline function declarations and class hierarchies.</p>
     */
    @Test
    public void testMultilineTokenPositions() throws Exception {
        String content = loadTestResource("psi/tokens/MultilineElements.kt");
        KtFile ktFile = env.parseFile("MultilineElements.kt", content);
        Document doc = createMockDocument(content);
        TokenFactory factory = new TokenFactory(doc, content);
        
        // Test "fun" keyword for multiline function - Line 2, Col 1
        PsiElement funKeyword = findFirstLeafToken(ktFile, "fun");
        assertNotNull("Should find 'fun' keyword", funKeyword);
        LocatableToken token = factory.create(funKeyword);
        assertEquals("fun keyword text", "fun", token.getText());
        assertEquals("fun keyword line", 2, token.getLine());
        assertEquals("fun keyword column", 1, token.getColumn());
        
        // Test "multilineFunction" identifier - Line 2, Col 5
        PsiElement funcName = findFirstLeafToken(ktFile, "multilineFunction");
        assertNotNull("Should find 'multilineFunction' identifier", funcName);
        token = factory.create(funcName);
        assertEquals("multilineFunction text", "multilineFunction", token.getText());
        assertEquals("multilineFunction line", 2, token.getLine());
        assertEquals("multilineFunction column", 5, token.getColumn());
        
        // Test "param1" identifier - Line 3, Col 5
        PsiElement param1 = findFirstLeafToken(ktFile, "param1");
        assertNotNull("Should find 'param1' identifier", param1);
        token = factory.create(param1);
        assertEquals("param1 text", "param1", token.getText());
        assertEquals("param1 line", 3, token.getLine());
        assertEquals("param1 column", 5, token.getColumn());
        
        // Test "class" keyword for MultilineClass - Line 15, Col 1
        PsiElement classKeyword = findFirstLeafToken(ktFile, "class");
        assertNotNull("Should find 'class' keyword", classKeyword);
        token = factory.create(classKeyword);
        assertEquals("class keyword text", "class", token.getText());
        assertEquals("class keyword line", 15, token.getLine());
        assertEquals("class keyword column", 1, token.getColumn());
        
        // Test "MultilineClass" identifier - Line 15, Col 7
        PsiElement className = findFirstLeafToken(ktFile, "MultilineClass");
        assertNotNull("Should find 'MultilineClass' identifier", className);
        token = factory.create(className);
        assertEquals("MultilineClass text", "MultilineClass", token.getText());
        assertEquals("MultilineClass line", 15, token.getLine());
        assertEquals("MultilineClass column", 7, token.getColumn());
    }
    
    /**
     * Test Unicode character positions in UnicodeContent.kt.
     *
     * <p>Validates TokenFactory correctly handles multi-byte UTF-8 characters
     * in identifiers and strings, including emojis and international characters.</p>
     */
    @Test
    public void testUnicodeTokenPositions() throws Exception {
        String content = loadTestResource("psi/tokens/UnicodeContent.kt");
        KtFile ktFile = env.parseFile("UnicodeContent.kt", content);
        Document doc = createMockDocument(content);
        TokenFactory factory = new TokenFactory(doc, content);
        
        // Test "class" keyword - Line 2, Col 1
        PsiElement classKeyword = findFirstLeafToken(ktFile, "class");
        assertNotNull("Should find 'class' keyword", classKeyword);
        LocatableToken token = factory.create(classKeyword);
        assertEquals("class keyword text", "class", token.getText());
        assertEquals("class keyword line", 2, token.getLine());
        assertEquals("class keyword column", 1, token.getColumn());
        
        // Test Unicode class name "ClassWithÜnicode" - Line 2, Col 7
        PsiElement unicodeClass = findFirstLeafToken(ktFile, "ClassWithÜnicode");
        assertNotNull("Should find 'ClassWithÜnicode' identifier", unicodeClass);
        token = factory.create(unicodeClass);
        assertEquals("ClassWithÜnicode text", "ClassWithÜnicode", token.getText());
        assertEquals("ClassWithÜnicode line", 2, token.getLine());
        assertEquals("ClassWithÜnicode column", 7, token.getColumn());
        
        // Test "val" keyword - Line 3, Col 5
        PsiElement valKeyword = findFirstLeafToken(ktFile, "val");
        assertNotNull("Should find 'val' keyword", valKeyword);
        token = factory.create(valKeyword);
        assertEquals("val keyword text", "val", token.getText());
        assertEquals("val keyword line", 3, token.getLine());
        assertEquals("val keyword column", 5, token.getColumn());
        
        // Test Unicode identifier "émoji" - Line 3, Col 9
        PsiElement emojiId = findFirstLeafToken(ktFile, "émoji");
        assertNotNull("Should find 'émoji' identifier", emojiId);
        token = factory.create(emojiId);
        assertEquals("émoji text", "émoji", token.getText());
        assertEquals("émoji line", 3, token.getLine());
        assertEquals("émoji column", 9, token.getColumn());
        
        // Test "fun" keyword - Line 9, Col 1
        PsiElement funKeyword = findFirstLeafToken(ktFile, "fun");
        assertNotNull("Should find 'fun' keyword", funKeyword);
        token = factory.create(funKeyword);
        assertEquals("fun keyword text", "fun", token.getText());
        assertEquals("fun keyword line", 9, token.getLine());
        assertEquals("fun keyword column", 1, token.getColumn());
        
        // Test "testUnicode" identifier - Line 9, Col 5
        PsiElement testFunc = findFirstLeafToken(ktFile, "testUnicode");
        assertNotNull("Should find 'testUnicode' identifier", testFunc);
        token = factory.create(testFunc);
        assertEquals("testUnicode text", "testUnicode", token.getText());
        assertEquals("testUnicode line", 9, token.getLine());
        assertEquals("testUnicode column", 5, token.getColumn());
    }
    
    /**
     * Smoke test: validate TokenFactory integration across all test corpus files.
     *
     * <p>Quick validation that parsing and token creation works for all test files
     * without exceptions. Checks basic validity (positions > 0) but not exact values.</p>
     */
    @Test
    public void testTokenFactorySmoke() throws Exception {
        String[] testFiles = {
            "psi/tokens/SimplePositions.kt",
            "psi/tokens/MultilineElements.kt",
            "psi/tokens/NestedStructures.kt",
            "psi/tokens/UnicodeContent.kt"
        };
        
        for (String testFile : testFiles) {
            String content = loadTestResource(testFile);
            String fileName = testFile.substring(testFile.lastIndexOf('/') + 1);
            KtFile ktFile = env.parseFile(fileName, content);
            assertNotNull("Should parse " + testFile, ktFile);
            
            Document doc = createMockDocument(content);
            TokenFactory factory = new TokenFactory(doc, content);
            
            // Find first "class" or "fun" keyword
            PsiElement keyword = findFirstLeafToken(ktFile, "class");
            if (keyword == null) {
                keyword = findFirstLeafToken(ktFile, "fun");
            }
            
            if (keyword != null) {
                LocatableToken token = factory.create(keyword);
                assertTrue("Token should have valid line for " + testFile,
                    token.getLine() > 0);
                assertTrue("Token should have valid column for " + testFile,
                    token.getColumn() > 0);
                assertNotNull("Token should have text for " + testFile,
                    token.getText());
            }
        }
    }
    
    /**
     * Find first leaf PSI element with given text.
     * 
     * <p>Searches recursively for a leaf element (no children) matching the
     * specified text. Skips whitespace and comments.</p>
     *
     * @param root Root element to search from
     * @param text Text to match
     * @return First matching leaf element, or null if not found
     */
    private PsiElement findFirstLeafToken(PsiElement root, String text) {
        if (root == null) {
            return null;
        }
        
        // Skip whitespace and comments
        if (root instanceof PsiWhiteSpace || root instanceof PsiComment) {
            return null;
        }
        
        // Check if this is a leaf with matching text
        if (root.getFirstChild() == null && text.equals(root.getText())) {
            return root;
        }
        
        // Recursively search children
        for (PsiElement child = root.getFirstChild(); child != null; child = child.getNextSibling()) {
            PsiElement found = findFirstLeafToken(child, text);
            if (found != null) {
                return found;
            }
        }
        
        return null;
    }
    
    /**
     * Load test resource from classpath.
     *
     * @param resourcePath Path relative to test resources root
     * @return Content of the resource file
     * @throws IOException if resource cannot be loaded
     */
    private String loadTestResource(String resourcePath) throws IOException {
        InputStream is = getClass().getClassLoader()
            .getResourceAsStream("bluej/parser/" + resourcePath);
        
        if (is == null) {
            throw new IOException("Test resource not found: " + resourcePath);
        }
        
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
    
    /**
     * Create a mock Document with line tracking.
     *
     * <p>Mocks IntelliJ Document with realistic line number behavior for
     * testing TokenFactory's position calculations.</p>
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
}