package bluej.parser;

import bluej.debugger.gentype.JavaType;
import bluej.parser.lexer.LocatableToken;

/**
 * Wrapper for information about code suggestions.
 * 
 * @author Davin McCall
 */
public class CodeSuggestions
{
    private JavaType suggestionType;
    private LocatableToken suggestionToken;
    
    public CodeSuggestions(JavaType suggestionType, LocatableToken suggestionToken)
    {
        this.suggestionToken = suggestionToken;
        this.suggestionType = suggestionType;
    }
    
    public LocatableToken getSuggestionToken()
    {
        return suggestionToken;
    }
    
    public JavaType getSuggestionType()
    {
        return suggestionType;
    }
}
