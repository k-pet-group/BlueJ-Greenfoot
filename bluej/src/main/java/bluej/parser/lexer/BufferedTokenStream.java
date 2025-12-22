package bluej.parser.lexer;

import bluej.parser.TokenStream;

public interface BufferedTokenStream extends TokenStream {
    TokenStream getSourceStream();

    LocatableToken nextToken();

    /**
     * Push a token on to the stream. The token will be returned by the next call
     * to nextToken().
     */
    void pushBack(LocatableToken token);

    /**
     * Gets the most recent token returned by nextToken which has not been
     * pushed back using pushBack.
     */
    LocatableToken getMostRecent();

    /**
     * Look ahead a certain number of tokens (without actually consuming them).
     *
     * @param distance The distance to look ahead (1 or greater).
     */
    LocatableToken LA(int distance);
}
