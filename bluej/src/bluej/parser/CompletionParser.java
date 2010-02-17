/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeSolid;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.MethodReflective;
import bluej.debugger.gentype.Reflective;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;

/**
 * A parser which determines what code completions are available.
 * 
 * @author Davin McCall
 */
public class CompletionParser extends TextParser
{
    private Map<String,JavaType> fieldSuggestions = Collections.emptyMap();
    private Map<String,Set<MethodReflective>> methodSuggestions = Collections.emptyMap();
    private JavaEntity suggestionEntity;
    private LocatableToken suggestionToken;
    
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
    public CompletionParser(EntityResolver resolver, Reader reader, JavaEntity enclosingType)
    {
        super(resolver, reader);
        suggestionEntity = enclosingType;
    }

    public CompletionParser(EntityResolver resolver, Reader reader,
            JavaEntity enclosingType, int line, int col)
    {
        super(resolver, reader, line, col);
        suggestionEntity = enclosingType;
    }
    
    @Override
    protected void error(String msg)
    {
        return;
    }
    
    public Map<String,JavaType> getFieldSuggestions()
    {
        return fieldSuggestions;
    }
    
    public Map<String,Set<MethodReflective>> getMethodSuggestions()
    {
        return methodSuggestions;
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
                return valEnt.getType().getCapture().asSolid();
            }
            valEnt = suggestionEntity.resolveAsType();
            if (valEnt != null) {
                return valEnt.getType().getCapture().asSolid();
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
    }
    
    @Override
    protected void completeCompoundValueEOF(LocatableToken token)
    {
        suggestionToken = token;
        suggestionEntity = popValueStack();
    }
    
    @Override
    protected void endExpression(LocatableToken token)
    {
        super.endExpression(token);
        if (token.getType() == JavaTokenTypes.EOF) {
            suggestFor(getSuggestionType());
        }
    }
    
    private void suggestFor(GenTypeSolid type)
    {
        if (type != null) {
            //JavaType type = valueEnt.getType().getCapture();
            GenTypeClass ctype = type.asClass();
            if (ctype != null) {
                Reflective r = ctype.getReflective();
                fieldSuggestions = r.getDeclaredFields();
                methodSuggestions = r.getDeclaredMethods();
            }
        }
    }
    
    @Override
    protected void gotLiteral(LocatableToken token)
    {
        if (token.getType() == JavaTokenTypes.LITERAL_this) {
            valueStack.push(suggestionEntity);
        }
        else {
            super.gotLiteral(token);
        }
    }
}
