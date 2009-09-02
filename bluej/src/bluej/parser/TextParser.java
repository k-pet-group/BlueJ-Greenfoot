package bluej.parser;

import java.io.Reader;
import java.io.StringReader;

import antlr.TokenStreamException;
import bluej.parser.ast.gen.JavaTokenTypes;

public class TextParser extends NewParser
{
    public TextParser(Reader r)
    {
        super(r);
    }
    
    public TextParser(String s)
    {
        this(new StringReader(s));
    }
    
    public boolean atEnd()
    {
        try {
            return tokenStream.LA(1).getType() == JavaTokenTypes.EOF;
        } catch (TokenStreamException e) {
            return true;
        }
    }
}
