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

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;
import bluej.parser.ast.LocatableToken;
import bluej.parser.ast.gen.JavaTokenTypes;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;

/**
 * A parser which determines what code completions are available.
 * 
 * @author Davin McCall
 */
public class CompletionParser extends TextParser
{
    private Map<String,JavaType> fieldSuggestions = Collections.emptyMap();
    //private Map<String,String> methodSuggestions;
    
    /**
     * Construct an expression parser, used for suggesting completions.
     * 
     * <p>Generally, after construction, call "parseExpression" and then
     * "getFieldSuggestions".
     * 
     * @param resolver   The resolver used to resolve identifiers
     * @param reader     The reader for the java source. This must return end-of-file
     *                    at the point where suggestions are to be made.
     */
    public CompletionParser(EntityResolver resolver, Reader reader)
    {
        super(resolver, reader);
    }
    
    public Map<String,JavaType> getFieldSuggestions()
    {
        return fieldSuggestions;
    }
    
    @Override
    protected void endExpression(LocatableToken token)
    {
        if (! operatorStack.isEmpty()) {
            if (operatorStack.peek().getType() == JavaTokenTypes.DOT) {
                JavaEntity entity = popValueStack();
                suggestFor(entity);
            }
        }
        super.endExpression(token);
    }
    
    private void suggestFor(JavaEntity entity)
    {
        JavaEntity valueEnt = entity.resolveAsValue();
        if (valueEnt != null) {
            JavaType type = valueEnt.getType().getCapture();
            GenTypeClass ctype = type.asClass();
            if (ctype != null) {
                Reflective r = ctype.getReflective();
                fieldSuggestions = r.getDeclaredFields();
                // r.getDeclaredMethods();
            }
        }
    }
}
