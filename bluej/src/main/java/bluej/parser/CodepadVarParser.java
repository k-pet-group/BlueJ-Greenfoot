/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2016,2020  Michael Kolling and John Rosenberg
 
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
import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import bluej.debugger.gentype.JavaType;
import bluej.debugmgr.codepad.DeclaredVar;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.ValueEntity;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Parse variable declarations/initializations (for the codepad).
 * 
 * @author Davin McCall
 */
@OnThread(Tag.FXPlatform)
public class CodepadVarParser extends TextParser
{
    private EntityResolver resolver;
    
    private int arrayCount = 0;
    private int modifiers = 0;
    private boolean gotFirstVar = false;
    private JavaType baseType;
    private boolean varKeyword;
    private List<DeclaredVar> variables = new ArrayList<DeclaredVar>();
    
    
    public CodepadVarParser(EntityResolver resolver, Reader reader, JavaEntity accessType)
    {
        super(resolver, reader, accessType, true);
        this.resolver = resolver;
    }

    public CodepadVarParser(EntityResolver resolver, String text, JavaEntity accessType)
    {
        this(resolver, new StringReader(text), accessType);
    }
    
    /**
     * Get the variables found to be declared by the parsed text. This should be called after
     * first calling "parseVariableDeclarations()".
     */
    public List<DeclaredVar> getVariables()
    {
        return variables;
    }
    
    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    protected void gotTypeSpec(List<LocatableToken> tokens)
    {
        super.gotTypeSpec(tokens);
        if (! gotFirstVar) {
            JavaEntity bent = ParseUtils.getTypeEntity(resolver, null, tokens);
            if (bent == null)
            {
                return;
            }
            if (bent.getName() != null && bent.getName().equals("var"))
            {
                varKeyword = true;
            }
            else
            {
                bent = bent.resolveAsType();
                if (bent != null)
                {
                    baseType = bent.getType();
                }
            }
        }
    }
        
    @Override
    protected void gotArrayDeclarator()
    {
        arrayCount++;
    }
    
    @Override
    protected void gotVariableDecl(LocatableToken first, LocatableToken idToken, boolean inited)
    {
        gotFirstVar = true;
        if (baseType != null || varKeyword) {
            JavaType vtype = baseType;
            while (!varKeyword && arrayCount > 0) {
                vtype = vtype.getArray();
                arrayCount--;
            }
            variables.add(new DeclaredVar(inited, Modifier.isFinal(modifiers),
                    vtype, idToken.getText()));
        }
    }
    
    @Override
    protected void gotSubsequentVar(LocatableToken first, LocatableToken idToken, boolean inited)
    {
        if (baseType != null || varKeyword) {
            JavaType vtype = baseType;
            while (!varKeyword && arrayCount > 0) {
                vtype = vtype.getArray();
                arrayCount--;
            }
            variables.add(new DeclaredVar(inited, Modifier.isFinal(modifiers),
                    vtype, idToken.getText()));
        }
    }
    
    @Override
    protected void gotModifier(LocatableToken token)
    {
        if (! gotFirstVar) {
            if (token.getType() == JavaTokenTypes.FINAL) {
                modifiers |= Modifier.FINAL;
            }
        }
    }

    @Override
    protected void endVariableDecls(LocatableToken token, boolean included)
    {
        super.endVariableDecls(token, included);
        // If they used var, we must try to infer the type from the expression they gave:
        if (varKeyword)
        {
            for (DeclaredVar variable : variables)
            {
                JavaEntity expressionType = getExpressionType();
                if (expressionType != null)
                {
                    ValueEntity entity = expressionType.resolveAsValue();
                    if (entity != null)
                    {
                        JavaType type = entity.getType();
                        // Don't give things a null type if they were initialised with null; leave as unknown type
                        // which will rightly give an error to the user that type couldn't be inferred:
                        if (type != null && (!type.isPrimitive() || !type.typeIs(JavaType.JT_NULL)))
                        {
                            variable.setDeclType(type);
                        }
                    }
                }
            }
        }
    }
}
