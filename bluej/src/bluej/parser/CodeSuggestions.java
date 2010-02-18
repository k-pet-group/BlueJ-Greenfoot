package bluej.parser;

import bluej.debugger.gentype.GenTypeClass;
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
    private GenTypeClass accessType;
    private LocatableToken suggestionToken;
    
    /**
     * Construct a new set of CodeSuggestions.
     * @param suggestionType  The type to suggest members from
     * @param accessType      The type which is doing the access (for access control purposes)
     * @param suggestionToken The token representing the suggestion prefix, i.e. the portion of the
     *                        member name already typed by the user
     */
    public CodeSuggestions(JavaType suggestionType, GenTypeClass accessType, LocatableToken suggestionToken)
    {
        this.suggestionToken = suggestionToken;
        this.suggestionType = suggestionType;
        this.accessType = accessType;
    }
    
    public LocatableToken getSuggestionToken()
    {
        return suggestionToken;
    }
    
    public JavaType getSuggestionType()
    {
        return suggestionType;
    }
    
    public GenTypeClass getAccessType()
    {
        return accessType;
    }
}
