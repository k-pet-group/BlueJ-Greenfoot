package bluej.parser.ast;

import java.io.Reader;

import antlr.*;
import antlr.collections.AST;
import bluej.parser.ast.gen.*;

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
        
        // create a scanner that reads from the input stream passed to us
        JavaLexer lexer = new JavaLexer(r);
        lexer.setTokenObjectClass("bluej.parser.ast.LocatableToken");

        // with a tab size of one, the rows and column numbers that
        // locatable token returns are model coordinates in the editor
        // (not view coordinates)
        // ie a keyword may appear to start at column 14 because of tabs
        // but in the actual document model its really at column 4
        // so we set our tabsize to 1 so that it maps directly to the
        // document model
        lexer.setTabSize(1);

        // create a filter to handle our comments
        TokenStreamHiddenTokenFilter filter;
        filter = new TokenStreamHiddenTokenFilter(lexer);
        filter.hide(JavaRecognizer.SL_COMMENT);
        filter.hide(JavaRecognizer.ML_COMMENT);

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
