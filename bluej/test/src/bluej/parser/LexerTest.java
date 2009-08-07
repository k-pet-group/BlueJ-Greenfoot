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
        TokenStream ts = getLexerFor("public private protected volatile transient abstract");
        LocatableToken token = (LocatableToken) ts.nextToken();
        assert(token.getType() == JavaTokenTypes.LITERAL_public);
        token = (LocatableToken) ts.nextToken();
        assert(token.getType() == JavaTokenTypes.LITERAL_private);
        token = (LocatableToken) ts.nextToken();
        assert(token.getType() == JavaTokenTypes.LITERAL_protected);
        token = (LocatableToken) ts.nextToken();
        assert(token.getType() == JavaTokenTypes.LITERAL_volatile);
        token = (LocatableToken) ts.nextToken();
        assert(token.getType() == JavaTokenTypes.LITERAL_transient);
        token = (LocatableToken) ts.nextToken();
        assert(token.getType() == JavaTokenTypes.ABSTRACT);
        token = (LocatableToken) ts.nextToken();
        assert(token.getType() == JavaTokenTypes.EOF);
        
        ts = getLexerFor("class interface enum");
        token = (LocatableToken) ts.nextToken();
        assert(token.getType() == JavaTokenTypes.LITERAL_class);
        token = (LocatableToken) ts.nextToken();
        assert(token.getType() == JavaTokenTypes.LITERAL_interface);
        token = (LocatableToken) ts.nextToken();
        assert(token.getType() == JavaTokenTypes.LITERAL_enum);
        token = (LocatableToken) ts.nextToken();
        assert(token.getType() == JavaTokenTypes.EOF);
    }
}
