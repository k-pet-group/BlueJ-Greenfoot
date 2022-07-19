/*
 This file is part of the BlueJ program. 
 Copyright (C) 2009,2010,2011,2012,2014,2016,2022  Michael Kolling and John Rosenberg
 
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
package bluej.parser;

import bluej.parser.lexer.JavaTokenFilter;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LineColPos;
import bluej.parser.lexer.LocatableToken;
import com.google.common.collect.LinkedListMultimap;

import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 * Tests for the Java lexer.
 * 
 * @author Davin McCall
 */
public class LexerTest extends junit.framework.TestCase
{
    private TokenStream getLexerFor(String s)
    {
        TokenStream lexer = JavaParser.getLexer(new StringReader(s));
        return new JavaTokenFilter(lexer, null);
    }
    
    private TokenStream getNonfilteringLexerFor(String s)
    {
        return JavaParser.getLexer(new StringReader(s));
    }
    
    public void testKeywordParse()
    {
        // Modifiers
        TokenStream ts = getLexerFor("public private protected volatile transient abstract synchronized strictfp static");
        LocatableToken token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_public);
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.LITERAL_private, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_protected);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_volatile);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_transient);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.ABSTRACT);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_synchronized);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.STRICTFP);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_static);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.EOF);
        
        // Additional keywords
        ts = getLexerFor("return import package final yield");
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_return);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_import);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_package);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.FINAL);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_yield);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.EOF);
        
        // Type declaration words
        ts = getLexerFor("class interface enum extends implements record sealed permits non-sealed");
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_class);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_interface);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_enum);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_extends);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_implements);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_record);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_sealed);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_permits);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_non_sealed);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.EOF);
        
        // Primitive types
        ts = getLexerFor("short int long float double boolean char void");
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_short);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_int);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_long);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_float);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_double);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_boolean);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_char);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_void);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.EOF);

        // Literals
        ts = getLexerFor("null true false this super");
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_null);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_true);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_false);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_this);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_super);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.EOF);
        
        // Control flow
        ts = getLexerFor("try catch throw if while do else switch case break continue");
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_try);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_catch);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_throw);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_if);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_while);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_do);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_else);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_switch);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_case);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_break);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_continue);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.EOF);
        
        // "goto" is a reserved word
        ts = getLexerFor("goto");
    }
    
    public void testSymbols() throws Exception
    {
        TokenStream ts = getLexerFor("+ - = += -= / * /= *= : :: ! ~ @ % %=");
        LocatableToken token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.PLUS);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.MINUS);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.ASSIGN);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.PLUS_ASSIGN);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.MINUS_ASSIGN);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.DIV);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.STAR);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.DIV_ASSIGN);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.STAR_ASSIGN);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.COLON);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.METHOD_REFERENCE);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LNOT);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.BNOT);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.AT);
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.MOD,  token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.MOD_ASSIGN,  token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.EOF,  token.getType());

        ts = getLexerFor("& | && || &= |= ^ ^= . ...");
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.BAND);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.BOR);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LAND);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LOR);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.BAND_ASSIGN);
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.BOR_ASSIGN, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.BXOR, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.BXOR_ASSIGN, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.DOT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.TRIPLE_DOT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.EOF, token.getType());
        
        ts = getLexerFor("<< >> <<= >>= >>> >>>=");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.SL, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.SR, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.SL_ASSIGN, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.SR_ASSIGN, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.BSR, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.BSR_ASSIGN, token.getType());
        
        ts = getLexerFor("< > <= >= != ==");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.LT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.GT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.LE, token.getType());        
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.GE, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NOT_EQUAL, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.EQUAL, token.getType());
    }
    
    public void testSymbols2() throws Exception
    {
        // Test symbols in combination
        TokenStream ts = getLexerFor("{([})]");
        LocatableToken token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.LCURLY, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.LPAREN, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.LBRACK, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.RCURLY, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.RPAREN, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.RBRACK, token.getType());
        
        ts = getLexerFor("+++++-----!!!!~~~~(()){{}}");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.INC, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.INC, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.PLUS, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.DEC, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.DEC, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.MINUS, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.LNOT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.LNOT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.LNOT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.LNOT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.BNOT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.BNOT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.BNOT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.BNOT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.LPAREN, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.LPAREN, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.RPAREN, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.RPAREN, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.LCURLY, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.LCURLY, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.RCURLY, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.RCURLY, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.EOF, token.getType());
        
        ts = getLexerFor(" [][] ]); ]).");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.LBRACK, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.RBRACK, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.LBRACK, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.RBRACK, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.RBRACK, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.RPAREN, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.SEMI, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.RBRACK, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.RPAREN, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.DOT, token.getType());
        
        ts = getLexerFor(">>, >>>,");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.SR, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.COMMA, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.BSR, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.COMMA, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.EOF, token.getType());
    }
    
    public void testIdentifiers() throws Exception
    {
        TokenStream ts = getLexerFor("_abc abc123 def123kjl98 \\u0396XYZ");
        LocatableToken token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.IDENT, token.getType());
        assertEquals("_abc", token.getText());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.IDENT, token.getType());
        assertEquals("abc123", token.getText());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.IDENT, token.getType());
        assertEquals("def123kjl98", token.getText());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.IDENT, token.getType());
        assertEquals("\u0396XYZ", token.getText());
    }
    
    public void testOther() throws Exception
    {
        TokenStream ts = getLexerFor("\"a string\" an_identifier99 '\\n' 1234 1234l 0.34 .78 01.2 .56f 5.06d 0x1234");
        LocatableToken token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.STRING_LITERAL, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.IDENT, token.getType());
        assertEquals("an_identifier99", token.getText());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.CHAR_LITERAL, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_INT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_LONG, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_DOUBLE, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_DOUBLE, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_DOUBLE, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_FLOAT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_DOUBLE, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_INT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.EOF, token.getType());
        
        ts = getLexerFor("0x5678l 0x01AF02 0x01AF02l 44.3E6 44.3E6f");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_LONG, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_INT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_LONG, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_DOUBLE, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_FLOAT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.EOF, token.getType());
        
        // A case which we've seen fail
        ts = getLexerFor("/120.0f,");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.DIV, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_FLOAT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.COMMA, token.getType());
    }
    
    public void testOther2() throws Exception
    {
        // Comments
        TokenStream ts = getNonfilteringLexerFor("/* multiline */   // single line");
        LocatableToken token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.ML_COMMENT, token.getType());
        assertEquals("/* multiline */", token.getText());
        
        token = (LocatableToken) ts.nextToken();
        
        // Note this fails with the old lexer (whch filters SL_COMMENTs):
        assertEquals(JavaTokenTypes.SL_COMMENT, token.getType());
        assertEquals(19, token.getColumn());
        assertEquals(1, token.getLine());
        assertEquals(33, token.getEndColumn());
        assertEquals(1, token.getEndLine());
        assertEquals("// single line", token.getText());
        
        //more complicated comments
        ts = getNonfilteringLexerFor("/**test*this***/");
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.ML_COMMENT);
        assertEquals("/**test*this***/", token.getText());
        
        ts = getNonfilteringLexerFor("/*test/check*/");
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.ML_COMMENT);
        assertEquals("/*test/check*/", token.getText());
        
        ts = getNonfilteringLexerFor("/*test//check*/");
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.ML_COMMENT);
        assertEquals("/*test//check*/", token.getText());
        
        // Make sure single line comment terminates
        ts = getNonfilteringLexerFor("// single line comment\n  an_identifier");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.SL_COMMENT, token.getType());
        assertEquals("// single line comment", token.getText());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.IDENT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.EOF, token.getType());
        
        // Simple EOF
        ts = getNonfilteringLexerFor("");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.EOF, token.getType());
        assertEquals(1, token.getColumn());
        assertEquals(1, token.getLine());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.EOF, token.getType());
        assertEquals(1, token.getColumn());
        assertEquals(1, token.getLine());
    }
    
    public void testOther3() throws Exception
    {
        // String literal with embedded \"
        TokenStream ts = getLexerFor("\"a \\\"string\"identifier");
        LocatableToken token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.STRING_LITERAL, token.getType());
        assertEquals("\"a \\\"string\"", token.getText());
        assertEquals(1, token.getColumn());
        assertEquals(13, token.getEndColumn());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.IDENT, token.getType());
        assertEquals(13, token.getColumn());
        assertEquals("identifier", token.getText());

        // String literal with embedded \\
        ts = getLexerFor("\" \\\\nn \\\\\"identifier");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.STRING_LITERAL, token.getType());
        assertEquals("\" \\\\nn \\\\\"", token.getText());
        assertEquals(11, token.getEndColumn());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.IDENT, token.getType());
        assertEquals(11, token.getColumn());
        assertEquals("identifier", token.getText());
        
        // Character literal '\''
        ts = getLexerFor("ident1'\\''ident2");
        token = (LocatableToken) ts.nextToken();
        assertEquals(7, token.getEndColumn());
        assertEquals(JavaTokenTypes.IDENT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(7, token.getColumn());
        assertEquals(JavaTokenTypes.CHAR_LITERAL, token.getType());
        assertEquals(11, token.getEndColumn());
        assertEquals("'\\''", token.getText());
        token = (LocatableToken) ts.nextToken();
        assertEquals(11, token.getColumn());
        assertEquals(JavaTokenTypes.IDENT, token.getType());
        
        // Character literal '\\'
        ts = getLexerFor("ident1'\\\\'ident2");
        token = (LocatableToken) ts.nextToken();
        assertEquals(7, token.getEndColumn());
        assertEquals(JavaTokenTypes.IDENT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(7, token.getColumn());
        assertEquals(JavaTokenTypes.CHAR_LITERAL, token.getType());
        assertEquals(11, token.getEndColumn());
        assertEquals("'\\\\'", token.getText());
        token = (LocatableToken) ts.nextToken();
        assertEquals(11, token.getColumn());
        assertEquals(JavaTokenTypes.IDENT, token.getType());
    }

    public void testTextBlock()
    {
        TokenStream ts = getLexerFor("\"\"\"\none line text block\"\"\"identifier");
        LocatableToken token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.STRING_LITERAL_MULTILINE, token.getType());
        assertEquals("\"\"\"\none line text block\"\"\"", token.getText());
        assertEquals(1, token.getLine());
        assertEquals(1, token.getColumn());
        assertEquals(2, token.getEndLine());
        assertEquals(23, token.getEndColumn());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.IDENT, token.getType());
        assertEquals(23, token.getColumn());
        assertEquals("identifier", token.getText());


        ts = getLexerFor("\"\"\"\nfirst line\nsecond line\"\\\"\n  \"\"\"identifier");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.STRING_LITERAL_MULTILINE, token.getType());
        assertEquals("\"\"\"\nfirst line\nsecond line\"\\\"\n  \"\"\"", token.getText());
        assertEquals(1, token.getLine());
        assertEquals(1, token.getColumn());
        assertEquals(4, token.getEndLine());
        assertEquals(6, token.getEndColumn());

        ts = getLexerFor("\"\"\"\nbefore\"text in quotes\"\n\"\"\"identifier");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.STRING_LITERAL_MULTILINE, token.getType());
        assertEquals("\"\"\"\nbefore\"text in quotes\"\n\"\"\"", token.getText());
        assertEquals(1, token.getLine());
        assertEquals(1, token.getColumn());
        assertEquals(3, token.getEndLine());
        assertEquals(4, token.getEndColumn());
        ts = getLexerFor("\"\"\"\nbefore\"text in quotes\\\"\"\"\"identifier");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.STRING_LITERAL_MULTILINE, token.getType());
        assertEquals("\"\"\"\nbefore\"text in quotes\\\"\"\"\"", token.getText());
        assertEquals(1, token.getLine());
        assertEquals(1, token.getColumn());
        assertEquals(2, token.getEndLine());
        assertEquals(27, token.getEndColumn());
    }
    
    public void testJava7NumLiterals() throws Exception
    {
        TokenStream ts = getLexerFor("1_000_000 1_000_000_000L 1_2.3_4f 1_2.3_4 1_2e3_4 0x12_34 " +
                "0b111011001 0B1100110 0b11_00_11 0B11_11_00 0b11001L");
        LocatableToken token = ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_INT, token.getType());
        token = ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_LONG, token.getType());
        token = ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_FLOAT, token.getType());
        token = ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_DOUBLE, token.getType());
        token = ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_DOUBLE, token.getType());
        token = ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_INT, token.getType());
        
        token = ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_INT, token.getType());
        token = ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_INT, token.getType());
        token = ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_INT, token.getType());
        token = ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_INT, token.getType());
        token = ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_LONG, token.getType());
    }
    
    public void testPositionTracking() throws Exception
    {
        TokenStream ts = getLexerFor("one two three\nfour five six  \n  seven eight nine");
        LocatableToken token = (LocatableToken) ts.nextToken();
        assertEquals(1, token.getLine());
        assertEquals(1, token.getColumn());
        assertEquals(1, token.getEndLine());
        assertEquals(4, token.getEndColumn());
        
        token = (LocatableToken) ts.nextToken(); // two
        token = (LocatableToken) ts.nextToken(); // three
        assertEquals(1, token.getLine());
        assertEquals(9, token.getColumn());
        assertEquals(1, token.getEndLine());
        assertEquals(14, token.getEndColumn());

        token = (LocatableToken) ts.nextToken(); // four
        assertEquals(2, token.getLine());
        assertEquals(1, token.getColumn());
        assertEquals(2, token.getEndLine());
        assertEquals(5, token.getEndColumn());
        
        token = (LocatableToken) ts.nextToken(); // five
        token = (LocatableToken) ts.nextToken(); // six
        token = (LocatableToken) ts.nextToken(); // seven
        assertEquals(3, token.getLine());
        assertEquals(3, token.getColumn());
        
        // Multi-line comment - fails with old lexer:
        ts = getNonfilteringLexerFor("\n/* This is a multi-line\ncomment\n*/");
        token = (LocatableToken) ts.nextToken();
        assertEquals(2, token.getLine());
        assertEquals(1, token.getColumn());
        assertEquals(4, token.getEndLine());
        assertEquals(3, token.getEndColumn());        
    }
    
    public void testPositionTracking2() throws Exception
    {
        // Unicode escape sequences
        TokenStream ts = getLexerFor("\\u0041ident another");
        LocatableToken token = (LocatableToken) ts.nextToken();
        assertEquals(1, token.getColumn());
        assertEquals(12, token.getEndColumn());
        token = (LocatableToken) ts.nextToken();
        assertEquals(13, token.getColumn());
        assertEquals(20, token.getEndColumn());
        
        ts = getLexerFor("ident\\u0041 another");
        token = (LocatableToken) ts.nextToken();
        assertEquals(1, token.getColumn());
        assertEquals(12, token.getEndColumn());
        token = (LocatableToken) ts.nextToken();
        assertEquals(13, token.getColumn());
        assertEquals(20, token.getEndColumn());

        // Unicode escape sequences - fails with old lexer
        ts = getLexerFor("one\\u0020two");
        token = (LocatableToken) ts.nextToken();
        assertEquals(4, token.getEndColumn());
        token = (LocatableToken) ts.nextToken();
        assertEquals(10, token.getColumn());
        
        // fails with old lexer:
        ts = getLexerFor("one\\u000Atwo");
        token = (LocatableToken) ts.nextToken();
        token = (LocatableToken) ts.nextToken();
        assertEquals(1, token.getLine());
        assertEquals(1, token.getEndLine());
        assertEquals(10, token.getColumn());
        assertEquals(13, token.getEndColumn());
    }
    
    public void testPositionTracking3() throws Exception
    {
        // EOF position shouldn't change between reads
        TokenStream ts = getLexerFor("one =");
        LocatableToken token = (LocatableToken) ts.nextToken(); // one
        assertEquals(1, token.getColumn());
        token = (LocatableToken) ts.nextToken(); // =
        assertEquals(5, token.getColumn());
        token = (LocatableToken) ts.nextToken(); // EOF
        assertEquals(6, token.getColumn());
        token = (LocatableToken) ts.nextToken(); // EOF
        assertEquals(6, token.getColumn());
    }
    
    public void testPositionTracking4() throws Exception
    {
        TokenStream ts = getLexerFor("\"somestring\" +\n    \"another string\";");
        LocatableToken token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.STRING_LITERAL, token.getType());
        assertEquals(1, token.getColumn());
        assertEquals(1, token.getLine());
        assertEquals(13, token.getEndColumn());
        assertEquals(1, token.getEndLine());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.PLUS, token.getType());
        assertEquals(14, token.getColumn());
        assertEquals(1, token.getLine());
        assertEquals(15, token.getEndColumn());
        assertEquals(1, token.getEndLine());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.STRING_LITERAL, token.getType());
        assertEquals(5, token.getColumn());
        assertEquals(2, token.getLine());
        assertEquals(21, token.getEndColumn());
        assertEquals(2, token.getEndLine());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.SEMI, token.getType());
        assertEquals(21, token.getColumn());
        assertEquals(2, token.getLine());
        assertEquals(22, token.getEndColumn());
        assertEquals(2, token.getEndLine());
    }

    public void testPositionTracking5() throws Exception
    {
        // Two possible interpretations:
        TokenStream ts = getLexerFor("999abcdef");
        LocatableToken token = (LocatableToken) ts.nextToken(); // one
        if (token.getType() == JavaTokenTypes.INVALID) {
            assertEquals(token.getColumn(), 1);
            assertEquals(token.getEndColumn(), 10);
        }
        else {
            assertEquals(JavaTokenTypes.NUM_INT, token.getType());
            assertEquals(4, token.getEndColumn());
            token = (LocatableToken) ts.nextToken();
            assertEquals(JavaTokenTypes.IDENT, token.getType());
            assertEquals(4, token.getColumn());
            assertEquals(10, token.getEndColumn());
        }
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.EOF, token.getType());
        assertEquals(10, token.getColumn());

        ts = getLexerFor("999|=");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_INT, token.getType());
        assertEquals(4, token.getEndColumn());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.BOR_ASSIGN, token.getType());
        assertEquals(4, token.getColumn());
        
        ts = getLexerFor("  999\n  {");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_INT, token.getType());
        assertEquals(6, token.getEndColumn());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.LCURLY, token.getType());
        assertEquals(3, token.getColumn());
        assertEquals(2, token.getLine());
    }

    // Helper record for the expected start and end of a token
    private record StartEnd(LineColPos start, LineColPos end){}
    // Given start and end position in String (starting at zero), create a corresponding StartEnd item
    private StartEnd p(int startIndex, int endIndex)
    {
        return new StartEnd(new LineColPos(1, startIndex + 1, startIndex), new LineColPos(1, endIndex + 1, endIndex));
    }
    
    // Test dealing with hyphenated keywords using a tricky example
    public void testPositionTracking6() throws Exception
    {
        // This should be parsed as (adding spaces to break tokens):
        // non-sealed - non-sealed - non - sealed2 - not - sealed - non-sealed - non -- sealed +
        TokenStream ts = getLexerFor("non-sealed-non-sealed-non-sealed2-not-sealed-non-sealed-non--sealed+");
        
        // The linked list multimap means the following entries will be iterated through
        // in the order we insert them (i.e. the order they are written here):
        LinkedListMultimap<Integer, StartEnd> expected = LinkedListMultimap.create();
        expected.put(JavaTokenTypes.LITERAL_non_sealed, p(0, 10));
        expected.put(JavaTokenTypes.MINUS, p(10, 11));
        expected.put(JavaTokenTypes.LITERAL_non_sealed, p(11, 21));
        expected.put(JavaTokenTypes.MINUS, p(21, 22));
        expected.put(JavaTokenTypes.IDENT, p(22, 25));
        expected.put(JavaTokenTypes.MINUS, p(25, 26));
        expected.put(JavaTokenTypes.IDENT, p(26, 33));
        expected.put(JavaTokenTypes.MINUS, p(33, 34));
        expected.put(JavaTokenTypes.IDENT, p(34, 37));
        expected.put(JavaTokenTypes.MINUS, p(37, 38));
        expected.put(JavaTokenTypes.LITERAL_sealed, p(38, 44));
        expected.put(JavaTokenTypes.MINUS, p(44, 45));
        expected.put(JavaTokenTypes.LITERAL_non_sealed, p(45, 55));
        expected.put(JavaTokenTypes.MINUS, p(55, 56));
        expected.put(JavaTokenTypes.IDENT, p(56, 59));
        expected.put(JavaTokenTypes.DEC, p(59, 61));
        expected.put(JavaTokenTypes.LITERAL_sealed, p(61, 67));
        expected.put(JavaTokenTypes.PLUS, p(67, 68));

        for (Entry<Integer, StartEnd> entry : expected.entries())
        {
            StartEnd pos = entry.getValue();
            LocatableToken token = (LocatableToken) ts.nextToken();
            assertEquals(pos.toString(), entry.getKey().intValue(), token.getType());
            assertEquals(token.getPosition(), pos.start().position());
            assertEquals(token.getLine(), pos.start().line());
            assertEquals(token.getColumn(), pos.start().column());
            assertEquals(token.getEndPosition(), pos.end().position());
            assertEquals(token.getEndLine(), pos.end().line());
            assertEquals(token.getEndColumn(), pos.end().column());
        }
    }
    
    public void testBroken() throws Exception
    {
        // These should have a meaningful return from the lexer
        // These fail with the old lexer
        
        // "unexpected character"
        TokenStream ts = getLexerFor("\\");
        LocatableToken token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.INVALID, token.getType());
        
        ts = getLexerFor("]\\\n"); // right bracket, then an invalid, then new line
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.RBRACK, token.getType());
        assertEquals(1, token.getColumn());
        assertEquals(2, token.getEndColumn());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.INVALID, token.getType());
        assertEquals(2, token.getColumn());
        assertEquals(3, token.getEndColumn());
        
        ts = getLexerFor("/* Unterminated comment\n}");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.INVALID, token.getType());
        assertEquals(1, token.getColumn());
        assertEquals(1, token.getLine());
        assertEquals(2, token.getEndColumn());
        assertEquals(2, token.getEndLine());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.EOF, token.getType());
        assertEquals(2, token.getColumn());
        assertEquals(2, token.getEndLine());
        
        ts = getLexerFor("\\u95 incomplete unicode escape");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.INVALID, token.getType());
        assertEquals(1, token.getColumn());
        assertEquals(5, token.getEndColumn());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.IDENT, token.getType());
        assertEquals(6, token.getColumn());
        
        ts = getLexerFor(".. incomplete ellipsis");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.INVALID, token.getType());
        assertEquals(1, token.getColumn());
        assertEquals(3, token.getEndColumn());
        token = (LocatableToken) ts.nextToken();
        assertEquals(4, token.getColumn());
        
        // Unterminated character literal
        ts = getLexerFor("  'ab + -\n an_identifier");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.INVALID, token.getType());
        assertEquals(3, token.getColumn());
        assertEquals(10, token.getEndColumn());
        assertEquals(1, token.getLine());
        assertEquals(1, token.getEndLine());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.IDENT, token.getType());
        assertEquals(2, token.getColumn());
        assertEquals(2, token.getLine());

        // Unterminated string literal
        ts = getLexerFor("  \"ab + -\n an_identifier");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.INVALID, token.getType());
        assertEquals(3, token.getColumn());
        assertEquals(10, token.getEndColumn());
        assertEquals(1, token.getLine());
        assertEquals(1, token.getEndLine());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.IDENT, token.getType());
        assertEquals(2, token.getColumn());
        assertEquals(2, token.getLine());
    }
    
    public void testStressLexer() throws Exception
    {
        Map<Integer,String> tokenMap = new HashMap<Integer,String>();
        tokenMap.put(JavaTokenTypes.COMMA, ",");
        tokenMap.put(JavaTokenTypes.PLUS, "+");
        tokenMap.put(JavaTokenTypes.MINUS, "-");
        tokenMap.put(JavaTokenTypes.ASSIGN, "=");
        tokenMap.put(JavaTokenTypes.LT, "<");
        tokenMap.put(JavaTokenTypes.GT, ">");
        tokenMap.put(JavaTokenTypes.EQUAL, "==");
        tokenMap.put(JavaTokenTypes.SL, "<<");
        tokenMap.put(JavaTokenTypes.SR, ">>");
        tokenMap.put(JavaTokenTypes.BSR, ">>>");
        tokenMap.put(JavaTokenTypes.NOT_EQUAL, "!=");
        tokenMap.put(JavaTokenTypes.LE, "<=");
        tokenMap.put(JavaTokenTypes.GE, ">=");
        tokenMap.put(JavaTokenTypes.SL_ASSIGN, "<<=");
        tokenMap.put(JavaTokenTypes.SR_ASSIGN, ">>=");
        tokenMap.put(JavaTokenTypes.BSR_ASSIGN, ">>>=");
        tokenMap.put(JavaTokenTypes.PLUS_ASSIGN, "+=");
        tokenMap.put(JavaTokenTypes.MINUS_ASSIGN, "-=");
        tokenMap.put(JavaTokenTypes.STAR_ASSIGN, "*=");
        tokenMap.put(JavaTokenTypes.DIV_ASSIGN, "/=");
        tokenMap.put(JavaTokenTypes.MOD_ASSIGN, "%=");
        tokenMap.put(JavaTokenTypes.BOR_ASSIGN, "|=");
        tokenMap.put(JavaTokenTypes.BAND_ASSIGN, "&=");
        tokenMap.put(JavaTokenTypes.BXOR_ASSIGN, "^=");
        tokenMap.put(JavaTokenTypes.LPAREN, "(");
        tokenMap.put(JavaTokenTypes.RPAREN, ")");
        tokenMap.put(JavaTokenTypes.LBRACK, "[");
        tokenMap.put(JavaTokenTypes.RBRACK, "]");
        tokenMap.put(JavaTokenTypes.LCURLY, "{");
        tokenMap.put(JavaTokenTypes.RCURLY, "}");
        tokenMap.put(JavaTokenTypes.DOT, ".");
        tokenMap.put(JavaTokenTypes.IDENT, "abcdefg");
        tokenMap.put(JavaTokenTypes.STRING_LITERAL, "\"A string literal\"");
        tokenMap.put(JavaTokenTypes.CHAR_LITERAL, "'n'");
        tokenMap.put(JavaTokenTypes.LAMBDA, "->");
        tokenMap.put(JavaTokenTypes.LITERAL_sealed, "sealed");
        tokenMap.put(JavaTokenTypes.LITERAL_non_sealed, "non-sealed");

        Map<Integer,Set<Integer>> cantFollow = new HashMap<Integer,Set<Integer>>();
        // "+" can't precede: +, +=, ++, =, ==
        Set<Integer> nonSet = new HashSet<Integer>();
        nonSet.add(JavaTokenTypes.PLUS);
        nonSet.add(JavaTokenTypes.PLUS_ASSIGN);
        nonSet.add(JavaTokenTypes.INC);
        nonSet.add(JavaTokenTypes.ASSIGN);
        nonSet.add(JavaTokenTypes.EQUAL);
        cantFollow.put(JavaTokenTypes.PLUS, nonSet);
        // "-" can't precede -, -=, --, =, ==, >, >>, >>>, ->, >>=, >>>=, >=
        nonSet = new HashSet<Integer>();
        nonSet.add(JavaTokenTypes.MINUS);
        nonSet.add(JavaTokenTypes.MINUS_ASSIGN);
        nonSet.add(JavaTokenTypes.DEC);
        nonSet.add(JavaTokenTypes.ASSIGN);
        nonSet.add(JavaTokenTypes.EQUAL);
        nonSet.add(JavaTokenTypes.GT);
        nonSet.add(JavaTokenTypes.SR);
        nonSet.add(JavaTokenTypes.BSR);
        nonSet.add(JavaTokenTypes.SR_ASSIGN);
        nonSet.add(JavaTokenTypes.BSR_ASSIGN);
        nonSet.add(JavaTokenTypes.GE);
        nonSet.add(JavaTokenTypes.LAMBDA);
        cantFollow.put(JavaTokenTypes.MINUS, nonSet);
        
        // "=" can't precede =, ==
        nonSet = new HashSet<Integer>();
        nonSet.add(JavaTokenTypes.ASSIGN);
        nonSet.add(JavaTokenTypes.EQUAL);
        cantFollow.put(JavaTokenTypes.ASSIGN, nonSet);
        // "<" can't precede "<", "<=", "<<", "<<=", "=", "=="
        nonSet = new HashSet<Integer>();
        nonSet.add(JavaTokenTypes.LT);
        nonSet.add(JavaTokenTypes.LE);
        nonSet.add(JavaTokenTypes.SL);
        nonSet.add(JavaTokenTypes.SL_ASSIGN);
        nonSet.add(JavaTokenTypes.ASSIGN);
        nonSet.add(JavaTokenTypes.EQUAL);
        cantFollow.put(JavaTokenTypes.LT, nonSet);
        // ">" can't precede ">", ">=", ">>", ">>>", ">>=", ">>>=", "=", "=="
        // ">> can't precede the same.
        nonSet = new HashSet<Integer>();
        nonSet.add(JavaTokenTypes.GT);
        nonSet.add(JavaTokenTypes.GE);
        nonSet.add(JavaTokenTypes.SR);
        nonSet.add(JavaTokenTypes.BSR);
        nonSet.add(JavaTokenTypes.SR_ASSIGN);
        nonSet.add(JavaTokenTypes.BSR_ASSIGN);
        nonSet.add(JavaTokenTypes.ASSIGN);
        nonSet.add(JavaTokenTypes.EQUAL);
        cantFollow.put(JavaTokenTypes.GT, nonSet);
        cantFollow.put(JavaTokenTypes.SR, nonSet);
        // "<<" can't precede "=", "=="
        nonSet = new HashSet<Integer>();
        nonSet.add(JavaTokenTypes.ASSIGN);
        nonSet.add(JavaTokenTypes.EQUAL);
        cantFollow.put(JavaTokenTypes.SL, nonSet);
        // ">>>" can't precede "=", "=="
        cantFollow.put(JavaTokenTypes.BSR, nonSet);
        // "." can't follow itself (part of ellipsis)
        cantFollow.put(JavaTokenTypes.DOT, Set.of(JavaTokenTypes.DOT));
        // identifier can't follow identifier or the two keywords
        nonSet = Set.of(JavaTokenTypes.IDENT, JavaTokenTypes.LITERAL_sealed, JavaTokenTypes.LITERAL_non_sealed);
        cantFollow.put(JavaTokenTypes.IDENT, nonSet);
        cantFollow.put(JavaTokenTypes.LITERAL_sealed, nonSet);
        cantFollow.put(JavaTokenTypes.LITERAL_non_sealed, nonSet);
        
        Set<Integer> tokens = tokenMap.keySet();
        for (Iterator<Integer> i = tokens.iterator(); i.hasNext(); ) {
            int ival = i.next();
            for (Iterator<Integer> j = tokens.iterator(); j.hasNext(); ) {
                int jval = j.next();
                Set<Integer> notToFollow = cantFollow.get(ival);
                if (notToFollow != null && notToFollow.contains(jval)) {
                    continue;
                }
                String testMe = tokenMap.get(ival) + tokenMap.get(jval);
                //System.out.println("String = " + testMe);
                TokenStream ts = getLexerFor(testMe);
                LocatableToken token = (LocatableToken) ts.nextToken();
                assertEquals(testMe, ival, token.getType());
                token = (LocatableToken) ts.nextToken();
                assertEquals(testMe, jval, token.getType());
                
                // Check EOF and its position
                token = (LocatableToken) ts.nextToken();
                assertEquals(JavaTokenTypes.EOF, token.getType());
                assertEquals(testMe.length() + 1, token.getColumn());
                
                // Read EOF again; it should be the same
                token = (LocatableToken) ts.nextToken();
                assertEquals(JavaTokenTypes.EOF, token.getType());
                assertEquals(testMe.length() + 1, token.getColumn());
                
                testMe = tokenMap.get(ival) + "\n" + tokenMap.get(jval);
                ts = getLexerFor(testMe);
                token = (LocatableToken) ts.nextToken();
                assertEquals(1, token.getColumn());
                assertEquals(1 + tokenMap.get(ival).length(), token.getEndColumn());
                assertEquals(1, token.getLine());
                assertEquals(1, token.getEndLine());
                token = (LocatableToken) ts.nextToken();
                assertEquals(jval, token.getType());
                assertEquals(1, token.getColumn());
                assertEquals(2, token.getLine());
            }
        }
    }
}
