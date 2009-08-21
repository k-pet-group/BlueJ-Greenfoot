package bluej.parser;

import java.io.StringReader;

import antlr.TokenStream;
import antlr.TokenStreamException;
import bluej.parser.ast.LocatableToken;
import bluej.parser.ast.gen.JavaLexer;
import bluej.parser.ast.gen.JavaTokenTypes;

/**
 * Tests for the Java lexer.
 * 
 * @author Davin McCall
 */
public class LexerTest extends junit.framework.TestCase
{
    private TokenStream getLexerFor(String s)
    {
        EscapedUnicodeReader euReader = new EscapedUnicodeReader(new StringReader(s));
        JavaLexer lexer = new JavaLexer(euReader);
        lexer.setTokenObjectClass("bluej.parser.ast.LocatableToken");
        lexer.setTabSize(1);
        euReader.setAttachedScanner(lexer);
        return new JavaTokenFilter(lexer, null);
    }
    
    public void testKeywordParse() throws TokenStreamException
    {
        TokenStream ts = getLexerFor("public private protected volatile transient abstract synchronized strictfp");
        LocatableToken token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_public);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_private);
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
        assertTrue(token.getType() == JavaTokenTypes.EOF);
        
        ts = getLexerFor("class interface enum");
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_class);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_interface);
        token = (LocatableToken) ts.nextToken();
        assertTrue(token.getType() == JavaTokenTypes.LITERAL_enum);
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
}
