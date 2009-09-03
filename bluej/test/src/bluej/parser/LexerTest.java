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
    
    private TokenStream getNonfilteringLexerFor(String s)
    {
        EscapedUnicodeReader euReader = new EscapedUnicodeReader(new StringReader(s));
        JavaLexer lexer = new JavaLexer(euReader);
        lexer.setTokenObjectClass("bluej.parser.ast.LocatableToken");
        lexer.setTabSize(1);
        euReader.setAttachedScanner(lexer);
        return lexer;
    }
    
    public void testKeywordParse() throws TokenStreamException
    {
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
        
        ts = getLexerFor("class interface enum extends implements");
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
        TokenStream ts = getLexerFor("+ - = += -= / * /= *= : ! ~ @ % %=");
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

        ts = getLexerFor("& | && || &= |= ^ ^=");
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

        ts = getLexerFor("{([})]");
        token = (LocatableToken) ts.nextToken();
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
        TokenStream ts = getLexerFor("\"a string\" an_identifier99 '\\n' 1234 1234l 0.34 .56f 5.06d 0x1234 0x5678l");
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
        assertEquals(JavaTokenTypes.NUM_FLOAT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_DOUBLE, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_INT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.NUM_LONG, token.getType());
        
        // Comments
        ts = getNonfilteringLexerFor("/* multiline */   // single line");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.ML_COMMENT, token.getType());
        assertEquals("/* multiline */", token.getText());
        
        ts = getNonfilteringLexerFor("// single line comment\n  an_identifier");
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.SL_COMMENT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.IDENT, token.getType());
        token = (LocatableToken) ts.nextToken();
        assertEquals(JavaTokenTypes.EOF, token.getType());

        token = (LocatableToken) ts.nextToken();
        if (token.getType() == JavaTokenTypes.WS) {
            token = (LocatableToken) ts.nextToken();
        }
        
        // Note this fails with the old lexer (whch filters SL_COMMENTs):
        assertEquals(JavaTokenTypes.SL_COMMENT, token.getType());
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

        // Unicode escape sequences
        ts = getLexerFor("\\u0041ident another");
        token = (LocatableToken) ts.nextToken();
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
        ts = getLexerFor("one\u0020two");
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
        
        // Multi-line comment - fails with old lexer:
        ts = getNonfilteringLexerFor("\n/* This is a multi-line\ncomment\n*/");
        token = (LocatableToken) ts.nextToken();
        while (token.getType() == JavaTokenTypes.WS) {
            token = (LocatableToken) ts.nextToken();
        }
        assertEquals(2, token.getLine());
        assertEquals(1, token.getColumn());
        assertEquals(4, token.getEndLine());
        assertEquals(3, token.getEndColumn());        
    }
    
    public void testBroken() throws Exception
    {
        // These should have a meaningful return from the lexer
        // These fail with the old lexer
        
        // "unexpected character"
        TokenStream ts = getLexerFor("\\");
        LocatableToken token = (LocatableToken) ts.nextToken();
        //assertEquals(JavaTokenTypes.???, token.getType());
        
        ts = getLexerFor("/* Unterminated comment");
        token = (LocatableToken) ts.nextToken();
        //assertEquals(JavaTokenTypes.NUM_LONG, token.getType());
        
        ts = getLexerFor("\\u95 incomplete unicode escape");
        token = (LocatableToken) ts.nextToken();
    }
}
