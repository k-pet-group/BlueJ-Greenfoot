package bluej.parser.psi;

import bluej.parser.lexer.JavaTokenFilter;
import bluej.parser.lexer.LocatableToken;

import java.util.List;

public interface JavaParserCallbacksAdapter extends JavaParserCallbacks {
    JavaTokenFilter getTokenStream();

    LocatableToken getLastToken();

    void setLastToken(LocatableToken lastToken);

    /**
     * Sets the emit range start position (line and column, both are included).
     * Callbacks will only be triggered if their position is >= this start position.
     *
     * @param line   Starting line (1-based)
     * @param column Starting column (1-based)
     */
    void setEmitRangeStart(int line, int column);

    /**
     * Sets the emit range start from a token (token position is included).
     *
     * @param token Token marking the start of the emit range
     */
    default void setEmitRangeStart(LocatableToken token)  {
        if (token != null) {
            setEmitRangeStart(token.getLine(), token.getColumn());
        }
    }

    /**
     * Sets the emit range start position with control over inclusion.
     *
     * @param line     Starting line (1-based)
     * @param column   Starting column (1-based)
     * @param included If false, position is AFTER the specified location
     */
    default void setEmitRangeStart(int line, int column, boolean included) {
        if (included) {
            setEmitRangeStart(line, column);
        } else {
            // Set to position after the specified location
            // For simplicity, increment column (line wrap handling would be complex)
            setEmitRangeStart(line, column + 1);
        }
    }

    /**
     * Sets the emit range start from a token with control over inclusion.
     *
     * @param token    Token marking the start boundary
     * @param included If false, range starts AFTER this token
     */
    default void setEmitRangeStart(LocatableToken token, boolean included)  {
        if (token != null) {
            if (included) {
                setEmitRangeStart(token.getLine(), token.getColumn());
            } else {
                setEmitRangeStart(token.getEndLine(), token.getEndColumn() + 1);
            }
        }
    }

    /**
     * Clears the emit range filter, allowing all callbacks to be triggered.
     */
    void clearEmitRangeStart();

    /**
     * Checks if a position is within the emit range.
     *
     * @param line   Line number to check
     * @param column Column number to check
     * @return true if position >= emit range start (or no range set)
     */
    boolean isInEmitRange(int line, int column);

    /**
     * Checks if a token is within the emit range.
     */
    default boolean isInEmitRange(LocatableToken token) {
        if (token == null) {
            return isInEmitRange(getTokenStream().LA(1));
        }
        return isInEmitRange(token.getLine(), token.getColumn());
    }

    /**
     * Checks if the last token in a list is within the emit range.
     */
    default boolean isInEmitRange(List<LocatableToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return isInEmitRange(getTokenStream().LA(1));
        }
        return isInEmitRange(tokens.getLast());
    }

    /**
     * Advances the token stream to the position of the target token and sets it as lastToken.
     * This ensures the token stream is properly positioned before recording the last token.
     *
     * @param targetToken The token to advance to and set as lastToken
     * @param included    If false, advances to one token before the target (target not included in construct)
     */
    default void skipToToken(LocatableToken targetToken, boolean included) {
        if (targetToken == null) {
            return;
        }

        JavaTokenFilter tokenStream = getTokenStream();
        LocatableToken currentToken;
        LocatableToken previousToken = tokenStream.getMostRecent();

        if (previousToken != null && previousToken.getPosition() >= targetToken.getPosition()) {
            return;
        }

        // Keep consuming tokens until we reach or pass the target token's position
        while ((currentToken = tokenStream.nextToken()) != null) {
            // Check if we've reached the target token by comparing positions
            if (currentToken.getPosition() >= targetToken.getPosition()) {
                if (!included) {
                    tokenStream.pushBack(currentToken);
                    currentToken = previousToken;
                }

                break;
            }
            // Consume this token and continue
            previousToken = currentToken;
        }

        // Set lastToken: if included use target, otherwise use token before it
        if (currentToken == null) {
            setLastToken(currentToken);
        }
    }

    /**
     * Advances the token stream to the target token and sets it as lastToken (token IS included).
     */
    default void skipToToken(LocatableToken targetToken) {
        skipToToken(targetToken, true);
    }

    /**
     * Advances the token stream to the last token in a list and sets it as lastToken.
     *
     * @param tokens List of tokens to process
     */
    default void skipToLastToken(List<LocatableToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        skipToToken(tokens.getLast(), true);
    }
}
