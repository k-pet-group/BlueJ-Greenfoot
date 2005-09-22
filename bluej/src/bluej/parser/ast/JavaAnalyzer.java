package bluej.parser.ast;

import java.io.Reader;

import antlr.RecognitionException;
import antlr.TokenStream;
import antlr.TokenStreamException;
import antlr.collections.AST;
import bluej.parser.EscapedUnicodeReader;
import bluej.parser.JavaTokenFilter;
import bluej.parser.ast.gen.JavaLexer;
import bluej.parser.ast.gen.JavaRecognizer;

/**
 * Analyse Java source and create an AST tree of the source.
 *
 * @author Andrew
 */
public class JavaAnalyzer
{
    private boolean parsedOk;
    private AST rootAST;
    
    /**
     *
     */
    public JavaAnalyzer(Reader r) throws RecognitionException, TokenStreamException
    {
        parsedOk = false;
        
        // We use a lexer pipeline:
        // First, deal with escaped unicode characters:
        EscapedUnicodeReader eur = new EscapedUnicodeReader(r);

        // Next create the initial lexer stage
        JavaLexer lexer = new JavaLexer(eur);
        lexer.setTokenObjectClass("bluej.parser.ast.LocatableToken");
        lexer.setTabSize(1);
        eur.setAttachedScanner(lexer);
        
        // Finally filter out comments and whitespace
        TokenStream filter = new JavaTokenFilter(lexer);

        // create a parser that reads from the scanner
        JavaRecognizer parser = new JavaRecognizer(filter);
        parser.setASTNodeClass("bluej.parser.ast.LocatableAST");

        // start parsing at the compilationUnit rule
        parser.compilationUnit();
      
        rootAST = parser.getAST();

        parsedOk = true;            
    }
    
    public AST getAST()
    {
        return rootAST;
    }
}
