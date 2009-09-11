package bluej.parser;

import bluej.parser.ast.LocatableToken;

/**
 * A token stream - a stream of tokens. This replaces the TokenStream from Antlr.
 * 
 * @author Davin McCall
 */
public interface TokenStream
{
    public LocatableToken nextToken();
}
