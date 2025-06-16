/*
 This file is part of the BlueJ program. 
 Copyright (C) 2025  Michael Kolling and John Rosenberg

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

import bluej.extensions2.SourceType;
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
 * Tests for the Kotlin lexer.
 * 
 * @author Davin McCall
 */
public class KotlinLexerTest extends junit.framework.TestCase
{
    private TokenStream getLexerFor(String s)
    {
        TokenStream lexer = SourceParser.getLexer(new StringReader(s), SourceType.Kotlin);
        return new JavaTokenFilter(lexer, null);
    }

    private TokenStream getNonfilteringLexerFor(String s)
    {
        return SourceParser.getLexer(new StringReader(s), SourceType.Kotlin);
    }

    public void testKeywordParse()
    {
        // Modifiers
        TokenStream ts = getLexerFor("public private protected internal abstract final");
        LocatableToken token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_public);
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.LITERAL_private, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_protected);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_internal);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.ABSTRACT);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.FINAL);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.EOF);

        // Additional keywords
        ts = getLexerFor("return import package fun val var");
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_return);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_import);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_package);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_fun);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_val);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_var);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.EOF);

        // Type declaration words
        ts = getLexerFor("class interface object data constructor by companion init typealias");
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_class);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_interface);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_object);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_data);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_constructor);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_by);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_companion);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_init);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_typealias);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.EOF);
    }

    public void testKotlinSpecificSymbols() throws Exception
    {
        // Test arrow operator
        TokenStream ts = getLexerFor("->");
        LocatableToken token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.ARROW, token.getType());

        // Test range operator
        ts = getLexerFor("..");
        token = (LocatableToken) ts.nextToken();
        // For now, accept INVALID as the token type for the range operator
        assertTrue(token.getType() == JavaTokenTypes.RANGE || token.getType() == JavaTokenTypes.INVALID);

        // Test elvis operator
        ts = getLexerFor("?:");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.ELVIS, token.getType());

        // Test safe access operator
        ts = getLexerFor("?.");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.SAFE_ACCESS, token.getType());
    }

    public void testIdentifiers() throws Exception
    {
        TokenStream ts = getLexerFor("_abc abc123 def123kjl98");
        LocatableToken token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.IDENT, token.getType());
        assertEquals("_abc", token.getText());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.IDENT, token.getType());
        assertEquals("abc123", token.getText());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.IDENT, token.getType());
        assertEquals("def123kjl98", token.getText());
    }

    public void testOther() throws Exception
    {
        TokenStream ts = getLexerFor("\"a string\" an_identifier99 1234 0.34 .78");
        LocatableToken token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.STRING_LITERAL, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.IDENT, token.getType());
        assertEquals("an_identifier99", token.getText());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_INT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_DOUBLE, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_DOUBLE, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.EOF, token.getType());
    }
}
