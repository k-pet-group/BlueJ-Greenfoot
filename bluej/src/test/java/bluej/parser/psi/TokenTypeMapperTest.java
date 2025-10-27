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
import org.jetbrains.kotlin.com.intellij.lang.ASTNode;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TokenTypeMapper}.
 * 
 * <p>Tests comprehensive mapping from Kotlin PSI element types to JavaTokenTypes constants.
 * Verifies correct token type assignment for keywords, modifiers, delimiters, operators,
 * and declaration types.</p>
 */
public class TokenTypeMapperTest {
    
    /**
     * Test null safety - null elements should return IDENT.
     */
    @Test
    public void testNullElement() {
        int result = TokenTypeMapper.mapPsiElementType(null);
        assertEquals("Null element should map to IDENT", JavaTokenTypes.IDENT, result);
    }
    
    /**
     * Test class declaration mapping.
     */
    @Test
    public void testClassDeclaration() {
        KtClass mockClass = mock(KtClass.class);
        when(mockClass.isInterface()).thenReturn(false);
        when(mockClass.isEnum()).thenReturn(false);
        
        int result = TokenTypeMapper.mapPsiElementType(mockClass);
        assertEquals("Regular class should map to LITERAL_class", 
            JavaTokenTypes.LITERAL_class, result);
    }
    
    /**
     * Test interface declaration mapping.
     */
    @Test
    public void testInterfaceDeclaration() {
        KtClass mockInterface = mock(KtClass.class);
        when(mockInterface.isInterface()).thenReturn(true);
        
        int result = TokenTypeMapper.mapPsiElementType(mockInterface);
        assertEquals("Interface should map to LITERAL_interface", 
            JavaTokenTypes.LITERAL_interface, result);
    }
    
    /**
     * Test enum declaration mapping.
     */
    @Test
    public void testEnumDeclaration() {
        KtClass mockEnum = mock(KtClass.class);
        when(mockEnum.isInterface()).thenReturn(false);
        when(mockEnum.isEnum()).thenReturn(true);
        
        int result = TokenTypeMapper.mapPsiElementType(mockEnum);
        assertEquals("Enum should map to LITERAL_enum", 
            JavaTokenTypes.LITERAL_enum, result);
    }
    
    /**
     * Test object declaration mapping.
     */
    @Test
    public void testObjectDeclaration() {
        KtObjectDeclaration mockObject = mock(KtObjectDeclaration.class);
        
        int result = TokenTypeMapper.mapPsiElementType(mockObject);
        assertEquals("Object declaration should map to LITERAL_object", 
            JavaTokenTypes.LITERAL_object, result);
    }
    
    /**
     * Test named function mapping (identifier context).
     */
    @Test
    public void testNamedFunctionIdentifier() {
        KtNamedFunction mockFunction = mock(KtNamedFunction.class);
        
        int result = TokenTypeMapper.mapPsiElementType(mockFunction);
        assertEquals("Function name should map to IDENT", 
            JavaTokenTypes.IDENT, result);
    }
    
    /**
     * Test property mapping (identifier context).
     */
    @Test
    public void testPropertyIdentifier() {
        KtProperty mockProperty = mock(KtProperty.class);
        
        int result = TokenTypeMapper.mapPsiElementType(mockProperty);
        assertEquals("Property name should map to IDENT", 
            JavaTokenTypes.IDENT, result);
    }
    
    /**
     * Test type alias mapping.
     */
    @Test
    public void testTypeAlias() {
        KtTypeAlias mockTypeAlias = mock(KtTypeAlias.class);
        
        int result = TokenTypeMapper.mapPsiElementType(mockTypeAlias);
        assertEquals("Type alias should map to LITERAL_typealias", 
            JavaTokenTypes.LITERAL_typealias, result);
    }
    
    /**
     * Test name reference expression mapping.
     */
    @Test
    public void testNameReferenceExpression() {
        KtNameReferenceExpression mockNameRef = mock(KtNameReferenceExpression.class);
        
        int result = TokenTypeMapper.mapPsiElementType(mockNameRef);
        assertEquals("Name reference should map to IDENT", 
            JavaTokenTypes.IDENT, result);
    }
    
    /**
     * Test keyword token mapping - class keyword.
     */
    @Test
    public void testClassKeyword() {
        PsiElement mockElement = createMockLeafElement(KtTokens.CLASS_KEYWORD);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("class keyword should map to LITERAL_class", 
            JavaTokenTypes.LITERAL_class, result);
    }
    
    /**
     * Test keyword token mapping - fun keyword.
     */
    @Test
    public void testFunKeyword() {
        PsiElement mockElement = createMockLeafElement(KtTokens.FUN_KEYWORD);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("fun keyword should map to LITERAL_fun", 
            JavaTokenTypes.LITERAL_fun, result);
    }
    
    /**
     * Test keyword token mapping - val keyword.
     */
    @Test
    public void testValKeyword() {
        PsiElement mockElement = createMockLeafElement(KtTokens.VAL_KEYWORD);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("val keyword should map to LITERAL_val", 
            JavaTokenTypes.LITERAL_val, result);
    }
    
    /**
     * Test keyword token mapping - var keyword.
     */
    @Test
    public void testVarKeyword() {
        PsiElement mockElement = createMockLeafElement(KtTokens.VAR_KEYWORD);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("var keyword should map to LITERAL_var", 
            JavaTokenTypes.LITERAL_var, result);
    }
    
    /**
     * Test delimiter mapping - left brace.
     */
    @Test
    public void testLeftBrace() {
        PsiElement mockElement = createMockLeafElement(KtTokens.LBRACE);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("{ should map to LCURLY", 
            JavaTokenTypes.LCURLY, result);
    }
    
    /**
     * Test delimiter mapping - right brace.
     */
    @Test
    public void testRightBrace() {
        PsiElement mockElement = createMockLeafElement(KtTokens.RBRACE);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("} should map to RCURLY", 
            JavaTokenTypes.RCURLY, result);
    }
    
    /**
     * Test delimiter mapping - left parenthesis.
     */
    @Test
    public void testLeftParen() {
        PsiElement mockElement = createMockLeafElement(KtTokens.LPAR);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("( should map to LPAREN", 
            JavaTokenTypes.LPAREN, result);
    }
    
    /**
     * Test delimiter mapping - right parenthesis.
     */
    @Test
    public void testRightParen() {
        PsiElement mockElement = createMockLeafElement(KtTokens.RPAR);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals(") should map to RPAREN", 
            JavaTokenTypes.RPAREN, result);
    }
    
    /**
     * Test control flow keyword mapping - if.
     */
    @Test
    public void testIfKeyword() {
        PsiElement mockElement = createMockLeafElement(KtTokens.IF_KEYWORD);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("if keyword should map to LITERAL_if", 
            JavaTokenTypes.LITERAL_if, result);
    }
    
    /**
     * Test control flow keyword mapping - when.
     */
    @Test
    public void testWhenKeyword() {
        PsiElement mockElement = createMockLeafElement(KtTokens.WHEN_KEYWORD);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("when keyword should map to LITERAL_when", 
            JavaTokenTypes.LITERAL_when, result);
    }
    
    /**
     * Test literal mapping - true.
     */
    @Test
    public void testTrueLiteral() {
        PsiElement mockElement = createMockLeafElement(KtTokens.TRUE_KEYWORD);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("true should map to LITERAL_true", 
            JavaTokenTypes.LITERAL_true, result);
    }
    
    /**
     * Test literal mapping - false.
     */
    @Test
    public void testFalseLiteral() {
        PsiElement mockElement = createMockLeafElement(KtTokens.FALSE_KEYWORD);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("false should map to LITERAL_false", 
            JavaTokenTypes.LITERAL_false, result);
    }
    
    /**
     * Test literal mapping - null.
     */
    @Test
    public void testNullLiteral() {
        PsiElement mockElement = createMockLeafElement(KtTokens.NULL_KEYWORD);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("null should map to LITERAL_null", 
            JavaTokenTypes.LITERAL_null, result);
    }
    
    /**
     * Test operator mapping - plus.
     */
    @Test
    public void testPlusOperator() {
        PsiElement mockElement = createMockLeafElement(KtTokens.PLUS);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("+ should map to PLUS", 
            JavaTokenTypes.PLUS, result);
    }
    
    /**
     * Test operator mapping - assignment.
     */
    @Test
    public void testAssignmentOperator() {
        PsiElement mockElement = createMockLeafElement(KtTokens.EQ);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("= should map to ASSIGN", 
            JavaTokenTypes.ASSIGN, result);
    }
    
    /**
     * Test operator mapping - equality.
     */
    @Test
    public void testEqualityOperator() {
        PsiElement mockElement = createMockLeafElement(KtTokens.EQEQ);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("== should map to EQUAL", 
            JavaTokenTypes.EQUAL, result);
    }
    
    /**
     * Test Kotlin-specific operator - arrow.
     */
    @Test
    public void testArrowOperator() {
        PsiElement mockElement = createMockLeafElement(KtTokens.ARROW);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("-> should map to ARROW", 
            JavaTokenTypes.ARROW, result);
    }
    
    /**
     * Test Kotlin-specific operator - elvis.
     */
    @Test
    public void testElvisOperator() {
        PsiElement mockElement = createMockLeafElement(KtTokens.ELVIS);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("?: should map to ELVIS", 
            JavaTokenTypes.ELVIS, result);
    }
    
    /**
     * Test Kotlin-specific operator - safe access.
     */
    @Test
    public void testSafeAccessOperator() {
        PsiElement mockElement = createMockLeafElement(KtTokens.SAFE_ACCESS);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("?. should map to SAFE_ACCESS", 
            JavaTokenTypes.SAFE_ACCESS, result);
    }
    
    /**
     * Test punctuation mapping - dot.
     */
    @Test
    public void testDotPunctuation() {
        PsiElement mockElement = createMockLeafElement(KtTokens.DOT);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals(". should map to DOT", 
            JavaTokenTypes.DOT, result);
    }
    
    /**
     * Test punctuation mapping - comma.
     */
    @Test
    public void testCommaPunctuation() {
        PsiElement mockElement = createMockLeafElement(KtTokens.COMMA);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals(", should map to COMMA", 
            JavaTokenTypes.COMMA, result);
    }
    
    /**
     * Test default behavior - unmapped element type should return IDENT.
     */
    @Test
    public void testUnmappedElementReturnsIdent() {
        // Create a mock element that doesn't match any known patterns
        PsiElement mockElement = mock(PsiElement.class);
        ASTNode mockNode = mock(ASTNode.class);
        when(mockElement.getNode()).thenReturn(mockNode);
        when(mockNode.getElementType()).thenReturn(null);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("Unmapped element should default to IDENT", 
            JavaTokenTypes.IDENT, result);
    }
    
    /**
     * Test element with no AST node returns IDENT.
     */
    @Test
    public void testElementWithNoNodeReturnsIdent() {
        PsiElement mockElement = mock(PsiElement.class);
        when(mockElement.getNode()).thenReturn(null);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("Element with no node should default to IDENT", 
            JavaTokenTypes.IDENT, result);
    }
    
    /**
     * Test package keyword mapping.
     */
    @Test
    public void testPackageKeyword() {
        PsiElement mockElement = createMockLeafElement(KtTokens.PACKAGE_KEYWORD);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("package keyword should map to LITERAL_package", 
            JavaTokenTypes.LITERAL_package, result);
    }
    
    /**
     * Test import keyword mapping.
     */
    @Test
    public void testImportKeyword() {
        PsiElement mockElement = createMockLeafElement(KtTokens.IMPORT_KEYWORD);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("import keyword should map to LITERAL_import", 
            JavaTokenTypes.LITERAL_import, result);
    }
    
    /**
     * Test return keyword mapping.
     */
    @Test
    public void testReturnKeyword() {
        PsiElement mockElement = createMockLeafElement(KtTokens.RETURN_KEYWORD);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("return keyword should map to LITERAL_return", 
            JavaTokenTypes.LITERAL_return, result);
    }
    
    /**
     * Test try keyword mapping.
     */
    @Test
    public void testTryKeyword() {
        PsiElement mockElement = createMockLeafElement(KtTokens.TRY_KEYWORD);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("try keyword should map to LITERAL_try", 
            JavaTokenTypes.LITERAL_try, result);
    }
    
    /**
     * Test catch keyword mapping.
     */
    @Test
    public void testCatchKeyword() {
        PsiElement mockElement = createMockLeafElement(KtTokens.CATCH_KEYWORD);
        
        int result = TokenTypeMapper.mapPsiElementType(mockElement);
        assertEquals("catch keyword should map to LITERAL_catch", 
            JavaTokenTypes.LITERAL_catch, result);
    }
    
    /**
     * Test comparison operators.
     */
    @Test
    public void testComparisonOperators() {
        assertEquals("< should map to LT", 
            JavaTokenTypes.LT, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.LT)));
        
        assertEquals("> should map to GT", 
            JavaTokenTypes.GT, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.GT)));
        
        assertEquals("<= should map to LE", 
            JavaTokenTypes.LE, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.LTEQ)));
        
        assertEquals(">= should map to GE", 
            JavaTokenTypes.GE, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.GTEQ)));
    }
    
    /**
     * Test logical operators.
     */
    @Test
    public void testLogicalOperators() {
        assertEquals("&& should map to LAND", 
            JavaTokenTypes.LAND, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.ANDAND)));
        
        assertEquals("|| should map to LOR", 
            JavaTokenTypes.LOR, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.OROR)));
        
        assertEquals("! should map to LNOT", 
            JavaTokenTypes.LNOT, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.EXCL)));
    }
    
    /**
     * Test increment/decrement operators.
     */
    @Test
    public void testIncrementDecrementOperators() {
        assertEquals("++ should map to INC", 
            JavaTokenTypes.INC, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.PLUSPLUS)));
        
        assertEquals("-- should map to DEC", 
            JavaTokenTypes.DEC, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.MINUSMINUS)));
    }
    
    /**
     * Test arithmetic operators.
     */
    @Test
    public void testArithmeticOperators() {
        assertEquals("* should map to STAR", 
            JavaTokenTypes.STAR, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.MUL)));
        
        assertEquals("/ should map to DIV", 
            JavaTokenTypes.DIV, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.DIV)));
        
        assertEquals("% should map to MOD", 
            JavaTokenTypes.MOD, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.PERC)));
    }
    
    /**
     * Test type keywords.
     */
    @Test
    public void testTypeKeywords() {
        assertEquals("is keyword should map to LITERAL_is", 
            JavaTokenTypes.LITERAL_is, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.IS_KEYWORD)));
        
        assertEquals("in keyword should map to LITERAL_in", 
            JavaTokenTypes.LITERAL_in, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.IN_KEYWORD)));
        
        assertEquals("as keyword should map to LITERAL_as", 
            JavaTokenTypes.LITERAL_as, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.AS_KEYWORD)));
    }
    
    /**
     * Test Kotlin-specific punctuation.
     */
    @Test
    public void testKotlinSpecificPunctuation() {
        assertEquals(":: should map to DOUBLE_COLON", 
            JavaTokenTypes.DOUBLE_COLON, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.COLONCOLON)));
        
        assertEquals(".. should map to RANGE", 
            JavaTokenTypes.RANGE, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.RANGE)));
    }
    
    /**
     * Test all bracket types.
     */
    @Test
    public void testBrackets() {
        assertEquals("[ should map to LBRACK", 
            JavaTokenTypes.LBRACK, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.LBRACKET)));
        
        assertEquals("] should map to RBRACK", 
            JavaTokenTypes.RBRACK, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.RBRACKET)));
    }
    
    /**
     * Test common punctuation.
     */
    @Test
    public void testCommonPunctuation() {
        assertEquals(": should map to COLON", 
            JavaTokenTypes.COLON, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.COLON)));
        
        assertEquals("; should map to SEMI", 
            JavaTokenTypes.SEMI, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.SEMICOLON)));
        
        assertEquals("? should map to QUESTION", 
            JavaTokenTypes.QUESTION, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.QUEST)));
        
        assertEquals("@ should map to AT", 
            JavaTokenTypes.AT, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.AT)));
    }
    
    /**
     * Test exception handling keywords.
     */
    @Test
    public void testExceptionKeywords() {
        assertEquals("throw should map to LITERAL_throw", 
            JavaTokenTypes.LITERAL_throw, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.THROW_KEYWORD)));
        
        assertEquals("finally should map to LITERAL_finally", 
            JavaTokenTypes.LITERAL_finally, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.FINALLY_KEYWORD)));
    }
    
    /**
     * Test loop keywords.
     */
    @Test
    public void testLoopKeywords() {
        assertEquals("for should map to LITERAL_for", 
            JavaTokenTypes.LITERAL_for, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.FOR_KEYWORD)));
        
        assertEquals("while should map to LITERAL_while", 
            JavaTokenTypes.LITERAL_while, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.WHILE_KEYWORD)));
        
        assertEquals("do should map to LITERAL_do", 
            JavaTokenTypes.LITERAL_do, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.DO_KEYWORD)));
        
        assertEquals("break should map to LITERAL_break", 
            JavaTokenTypes.LITERAL_break, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.BREAK_KEYWORD)));
        
        assertEquals("continue should map to LITERAL_continue", 
            JavaTokenTypes.LITERAL_continue, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.CONTINUE_KEYWORD)));
    }
    
    /**
     * Test special Kotlin keywords.
     */
    @Test
    public void testSpecialKotlinKeywords() {
        assertEquals("constructor should map to LITERAL_constructor", 
            JavaTokenTypes.LITERAL_constructor, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.CONSTRUCTOR_KEYWORD)));
        
        assertEquals("init should map to LITERAL_init", 
            JavaTokenTypes.LITERAL_init, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.INIT_KEYWORD)));
        
        assertEquals("this should map to LITERAL_this", 
            JavaTokenTypes.LITERAL_this, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.THIS_KEYWORD)));
        
        assertEquals("super should map to LITERAL_super", 
            JavaTokenTypes.LITERAL_super, 
            TokenTypeMapper.mapPsiElementType(createMockLeafElement(KtTokens.SUPER_KEYWORD)));
    }
    
    /**
     * Helper method to create a mock PSI element with a specific token type.
     * 
     * @param tokenType The Kotlin token type to assign
     * @return A mocked PsiElement with the specified token type
     */
    private PsiElement createMockLeafElement(org.jetbrains.kotlin.com.intellij.psi.tree.IElementType tokenType) {
        PsiElement mockElement = mock(PsiElement.class);
        ASTNode mockNode = mock(ASTNode.class);
        when(mockElement.getNode()).thenReturn(mockNode);
        when(mockNode.getElementType()).thenReturn(tokenType);
        return mockElement;
    }
}