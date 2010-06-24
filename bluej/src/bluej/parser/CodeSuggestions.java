/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010  Michael Kolling and John Rosenberg 

 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 

 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 

 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 

 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
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
    private boolean staticRestricted=false;   //restrict suggestions to only static methods e.g for a class
    
    /**
     * Construct a new set of CodeSuggestions.
     * @param suggestionType  The type to suggest members from
     * @param accessType      The type which is doing the access (for access control purposes).
     *                        May be null.
     * @param suggestionToken The token representing the suggestion prefix, i.e. the portion of the
     *                        member name already typed by the user
     * @param staticOnly    Indicates if true that non-static members should not be included
     *                      in the returned results                 
     */
    public CodeSuggestions(JavaType suggestionType, GenTypeClass accessType, LocatableToken suggestionToken, boolean staticOnly)
    {
        this.suggestionToken = suggestionToken;
        this.suggestionType = suggestionType;
        this.accessType = accessType;
        this.staticRestricted = staticOnly;
    }
    
    public LocatableToken getSuggestionToken()
    {
        return suggestionToken;
    }
    
    public JavaType getSuggestionType()
    {
        return suggestionType;
    }
    
    /**
     * Get the type in which the expression occurs (the "access type").
     * This might return null.
     */
    public GenTypeClass getAccessType()
    {
        return accessType;
    }

    public void setStatic(boolean restricted)
    {
        this.staticRestricted = restricted;
    }

    public boolean isStatic()
    {
        return staticRestricted;
    }
}
