/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011,2012,2014  Michael Kolling and John Rosenberg 
 
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

import java.io.Reader;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import bluej.debugger.gentype.FieldReflective;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeSolid;
import bluej.debugger.gentype.MethodReflective;
import bluej.debugger.gentype.Reflective;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.lexer.LocatableToken;

/**
 * A parser which determines what code completions are available.
 * 
 * @author Davin McCall
 */
public class CompletionParser extends TextParser
{
    private Map<String,Set<MethodReflective>> methodSuggestions = Collections.emptyMap();
    private Map<String,FieldReflective> fieldSuggestions = Collections.emptyMap();
    private JavaEntity suggestionEntity;
    private LocatableToken suggestionToken;
    private boolean staticRestricted=false;
    private boolean plain = true; // Assume plain until disproven

    /**
     * Construct an expression parser, used for suggesting completions.
     * 
     * <p>Generally, after construction, call "parseExpression" and then
     * "getFieldSuggestions".
     * 
     * @param resolver   The resolver used to resolve identifiers
     * @param reader     The reader for the java source. This must return end-of-file
     *                    at the point where suggestions are to be made.
     * @param enclosingType  An entity representing the enclosing type of the cursor location
     */
    public CompletionParser(EntityResolver resolver, Reader reader, JavaEntity defaultEnt)
    {
        super(resolver, reader, defaultEnt, false);
        suggestionEntity = defaultEnt;
    }

    /**
     * Construct an expression parser, used for suggesting code completions,
     * specifying the position within the document at which the expression
     * begins.
     * 
     * <p>Generally, after construction, call "parseExpression" and then
     * "getFieldSuggestions".
     * 
     * @param resolver   The resolver used to resolve identifiers
     * @param reader     The reader for the java source. This must return end-of-file
     *                    at the point where suggestions are to be made. The first
     *                    character read should be the character at the specified line
     *                    and column of the document.
     * @param defaultEnt  An entity representing the enclosing type or value of the
     *                    cursor location.
     * @param  line    The source line where the expression begins
     * @param  col     The source column where the expression begins
     */
    public CompletionParser(EntityResolver resolver, Reader reader,
            JavaEntity defaultEnt, int line, int col, int pos)
    {
        super(resolver, reader, defaultEnt, false, line, col, pos);
        suggestionEntity = defaultEnt;
    }
    
    @Override
    protected void error(String msg, int beginLine, int beginCol, int endLine, int endCol)
    {
        return;
    }
    
    public Map<String,Set<MethodReflective>> getMethodSuggestions()
    {
        if (methodSuggestions == null) {
            suggestFor(getSuggestionType());
        }
        return methodSuggestions;
    }
    
    public Map<String, FieldReflective> getFieldSuggestions()
    {
        if (fieldSuggestions == null) {
            suggestFor(getSuggestionType());
        }
        return fieldSuggestions;
    }
    
    /**
     * Get the type for which to make suggestions. The suggestions presented to the user
     * should be members of the returned type.
     */
    public GenTypeSolid getSuggestionType()
    {
        if (suggestionEntity != null) {
            JavaEntity valEnt = suggestionEntity.resolveAsValue();
            if (valEnt != null) {
                return valEnt.getType().asSolid();
            }
            valEnt = suggestionEntity.resolveAsType();
            if (valEnt != null) {
                setStatic(true);
                return valEnt.getType().asSolid();
            }
        }
        return null;
    }
    
    /**
     * Get the token, if any, which represents the partial identifier just before the
     * completion point.
     */
    public LocatableToken getSuggestionToken()
    {
        return suggestionToken;
    }
    
    @Override
    protected void gotDotEOF(LocatableToken token)
    {
        suggestionEntity = popValueStack();
        plain = false;
    }
    
    @Override
    protected void gotIdentifierEOF(LocatableToken token)
    {
        suggestionToken = token;
    }
    
    @Override
    protected void gotMemberAccessEOF(LocatableToken token)
    {
        suggestionToken = token;
        suggestionEntity = popValueStack();
        plain = false;
    }
    
    @Override
    protected void completeCompoundValueEOF(LocatableToken token)
    {
        suggestionToken = token;
        suggestionEntity = popValueStack();
        plain = false;
    }
    
    private void suggestFor(GenTypeSolid type)
    {
        if (type != null) {
            //JavaType type = valueEnt.getType().getCapture();
            GenTypeClass ctype = type.asClass();
            if (ctype != null) {
                Reflective r = ctype.getReflective();
                methodSuggestions = r.getDeclaredMethods();
                fieldSuggestions = r.getDeclaredFields();
            }
        }
    }
    
    public boolean isSuggestionStatic()
    {
        return staticRestricted;
    }

    protected void setStatic(boolean restricted)
    {
        this.staticRestricted = restricted;
    }
    
    public boolean isPlain()
    {
        return plain;
    }
    
}
