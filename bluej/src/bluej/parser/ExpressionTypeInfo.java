/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010,2014,2019  Michael Kolling and John Rosenberg 

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
 * Wrapper for information about the type of expression appearing at a certain point in a source
 * document, useful for (among other things) offering code completion.
 * <p>
 * This can handle document locations that are part-way through an expression. The "suggestion
 * type" is the type of the expression appearing before the most recent dot, if any (or the type
 * from which unqualified members are resolved).
 * 
 * @author Davin McCall
 */
public class ExpressionTypeInfo
{
    private JavaType suggestionType;
    private GenTypeClass accessType;
    private LocatableToken suggestionToken;
    private boolean staticRestricted;   //restrict suggestions to only static methods e.g for a class
    private boolean plain;
    
    /**
     * Construct a new set of CodeSuggestions.
     * @param suggestionType  The type to suggest members from
     * @param accessType      The type which is doing the access (for access control purposes).
     *                        May be null.
     * @param suggestionToken The token representing the suggestion prefix, i.e. the portion of the
     *                        member name already typed by the user
     * @param staticOnly    Indicates if true that non-static members should not be included
     *                      in the returned results                
     * @param plain         If true, means that there is no compound expression on which we are
     *                      completing.  Essentially, if plain is true then the user could be referring
     *                      to a local variable. 
     */
    public ExpressionTypeInfo(JavaType suggestionType, GenTypeClass accessType, LocatableToken suggestionToken, boolean staticOnly, boolean plain)
    {
        this.suggestionToken = suggestionToken;
        this.suggestionType = suggestionType;
        this.accessType = accessType;
        this.staticRestricted = staticOnly;
        this.plain = plain;
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

    public boolean isStatic()
    {
        return staticRestricted;
    }
    
    public boolean isPlain()
    {
        return plain;
    }
}
