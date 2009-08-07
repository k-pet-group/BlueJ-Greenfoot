package bluej.parser;

import bluej.parser.ast.Token;

public interface TokenStream {
    public Token nextToken() throws TokenStreamException;

}
