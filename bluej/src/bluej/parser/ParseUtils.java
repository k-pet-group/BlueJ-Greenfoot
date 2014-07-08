/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2013,2014  Michael Kolling and John Rosenberg 
 
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import bluej.debugger.gentype.GenTypeArrayClass;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.JavaPrimitiveType;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.MethodReflective;
import bluej.debugger.gentype.Reflective;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.ImportedEntity;
import bluej.parser.entity.JavaEntity;
import bluej.parser.entity.ParsedArrayReflective;
import bluej.parser.entity.SolidTargEntity;
import bluej.parser.entity.TypeArgumentEntity;
import bluej.parser.entity.TypeEntity;
import bluej.parser.entity.UnboundedWildcardEntity;
import bluej.parser.entity.UnresolvedArray;
import bluej.parser.entity.UnresolvedEntity;
import bluej.parser.entity.WildcardExtendsEntity;
import bluej.parser.entity.WildcardSuperEntity;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.lexer.LocatableToken;
import bluej.pkgmgr.JavadocResolver;
import bluej.utility.JavaReflective;
import bluej.utility.JavaUtils;

/**
 * Utilities for parsers.
 * 
 * @author Davin McCall
 */
public class ParseUtils
{
    private static class DepthRef
    {
        int depth = 0;
    }
    
    /**
     * Get the possible code completions, based on the provided suggestions.
     * If there are can be no valid completions in the given context, returns null.
     */
    public static AssistContent[] getPossibleCompletions(CodeSuggestions suggests, 
            JavadocResolver javadocResolver)
    {
        GenTypeClass exprType = initGetPossibleCompletions(suggests);
        if (exprType != null){
            //process
            List<AssistContent> completions = processQueue(exprType, suggests, javadocResolver);
            
            return completions.toArray(new AssistContent[completions.size()]);
        }
        return null;
    }
    
    /**
     * Determine the target type for which members can be suggested (for code completion).
     * This utility method wraps primitives arrays as a suitable class type.
     * 
     * @param suggests  The code completion data
     * @param javadocResolver   A javadoc resolver (not used)
     * @return  A suitable GenTypeClass representing the target type for completion
     *           purposes, or null if there is no such suitable type.
     */
    public static GenTypeClass initGetPossibleCompletions(CodeSuggestions suggests)
    {
        if (suggests != null) {
            GenTypeClass exprType = suggests.getSuggestionType().asClass();
            if (exprType == null) {
                final JavaType arrayComponent = suggests.getSuggestionType().getArrayComponent();
                if (arrayComponent != null && arrayComponent.isPrimitive()) {
                    // Array of primitives:
                    // For code completion purposes, consider this as an array of object with a tweaked name:
                    exprType = new GenTypeArrayClass(new ParsedArrayReflective(new JavaReflective(Object.class),"Object")
                    {
                        @Override
                        public String getSimpleName()
                        {
                            return arrayComponent.toString() + "[]";
                        }
                        
                    }, arrayComponent);
                }
                else {
                    return null;
                }
            }
            return exprType;
        }

        return null; // no completions
    }
    
    protected static List<AssistContent> processQueue(GenTypeClass exprType, CodeSuggestions suggests,
            JavadocResolver javadocResolver)
    {
        GenTypeClass accessType = suggests.getAccessType();
        Reflective accessReflective = (accessType != null) ? accessType.getReflective() : null;

        // Use two sets, one to keep track of which types we have already processed,
        // another for individual methods.
        Set<String> contentSigs = new HashSet<String>();
        Set<String> typesDone = new HashSet<String>();
        List<AssistContent> completions = new ArrayList<AssistContent>();

        LinkedList<GenTypeClass> typeQueue = new LinkedList<GenTypeClass>();
        typeQueue.add(exprType);
        GenTypeClass origExprType = exprType;

        while (!typeQueue.isEmpty()) {
            exprType = typeQueue.removeFirst();

            if (!typesDone.add(exprType.getReflective().getName())) {
                // we've already done this type...
                continue;
            }
            Map<String, Set<MethodReflective>> methods = exprType.getReflective().getDeclaredMethods();
            Map<String, GenTypeParameter> typeArgs = exprType.getMap();

            for (String name : methods.keySet()) {
                Set<MethodReflective> mset = methods.get(name);
                for (MethodReflective method : mset) {
                    if (accessReflective != null
                            && !JavaUtils.checkMemberAccess(method.getDeclaringType(),
                                    origExprType,
                                    suggests.getAccessType().getReflective(),
                                    method.getModifiers(), suggests.isStatic())) {
                        continue;
                    }
                    discoverElement(javadocResolver, contentSigs, completions, typeArgs, method);

                    for (GenTypeClass stype : exprType.getReflective().getSuperTypes()) {
                        if (typeArgs != null) {
                            typeQueue.add(stype.mapTparsToTypes(typeArgs));
                        } else {
                            typeQueue.add(stype.getErasedType());
                        }
                    }

                    Reflective outer = exprType.getReflective().getOuterClass();
                    if (outer != null) {
                        typeQueue.add(new GenTypeClass(outer));
                    }
                }
            }
        }
        return completions;
    }
    
    
    /**
     * This method discovers and returns one completion (last one if any) and updates the typeQueue for further 
     * processing by processQueue.
     */
    public static AssistContent discoverElement(JavadocResolver javadocResolver, Set<String> contentSigs, List<AssistContent> completions, 
            Map<String, GenTypeParameter> typeArgs, MethodReflective method)
    {
        AssistContent result = null;
        MethodCompletion completion = null;
        completion = new MethodCompletion(method,
                typeArgs, javadocResolver);
        String sig = completion.getDisplayName();
        if (contentSigs.add(sig)) {
            completions.add(completion);
            result = completion;
            // Sort the completions by name
            //    Collections.sort(completions, new CompletionComparator());
        }
        
        return result;
    }
    
    /**
     * Get an entity for an imported type specifier. This is different from a non-imported type
     * because in that it must be qualified.
     * 
     * @param resolver  Entity resolver which will (eventually) resolve the entity
     * @param tokens  The tokens making up the specification
     */
    public static JavaEntity getImportEntity(EntityResolver resolver,
            Reflective querySource, List<LocatableToken> tokens)
    {
        if (tokens.isEmpty()) {
            return null;
        }
        
        Iterator<LocatableToken> i = tokens.iterator();
        LocatableToken tok = i.next();
        if (tok.getType() != JavaTokenTypes.IDENT) {
            return null;
        }
        
        List<String> names = new LinkedList<String>();
        names.add(tok.getText());
        
        while (i.hasNext()) {
            tok = i.next();
            if (tok.getType() != JavaTokenTypes.DOT || ! i.hasNext()) {
                return null;
            }
            tok = i.next();
            if (tok.getType() != JavaTokenTypes.IDENT) {
                return null;
            }
            names.add(tok.getText());
        }
        
        return new ImportedEntity(resolver, names, querySource);
    }
    
    /**
     * Get an entity for a type specification (specified by a list of tokens). The
     * returned entity might be unresolved.
     * 
     * @param resolver   Entity resolver which will (eventually) resolve the entity
     * @param querySource  The source of the query - a fully qualified class name
     * @param tokens  The tokens specifying the type
     */
    public static JavaEntity getTypeEntity(EntityResolver resolver,
            Reflective querySource, List<LocatableToken> tokens)
    {
        DepthRef dr = new DepthRef();
        return getTypeEntity(resolver, querySource, tokens.listIterator(), dr);
    }
    
    /**
     * Get an entity for a type specification. The returned entity may be unresolved.
     * Returns null if the type specification appears to be invalid.
     */
    private static JavaEntity getTypeEntity(EntityResolver resolver, Reflective querySource,
            ListIterator<LocatableToken> i, DepthRef depthRef)
    {
        LocatableToken token = i.next();
        if (JavaParser.isPrimitiveType(token)) {
            JavaType type = null;
            switch (token.getType()) {
            case JavaTokenTypes.LITERAL_int:
                type = JavaPrimitiveType.getInt();
                break;
            case JavaTokenTypes.LITERAL_short:
                type = JavaPrimitiveType.getShort();
                break;
            case JavaTokenTypes.LITERAL_long:
                type = JavaPrimitiveType.getLong();
                break;
            case JavaTokenTypes.LITERAL_char:
                type = JavaPrimitiveType.getChar();
                break;
            case JavaTokenTypes.LITERAL_byte:
                type = JavaPrimitiveType.getByte();
                break;
            case JavaTokenTypes.LITERAL_boolean:
                type = JavaPrimitiveType.getBoolean();
                break;
            case JavaTokenTypes.LITERAL_double:
                type = JavaPrimitiveType.getDouble();
                break;
            case JavaTokenTypes.LITERAL_float:
                type = JavaPrimitiveType.getFloat();
                break;
            case JavaTokenTypes.LITERAL_void:
                type = JavaPrimitiveType.getVoid();
            }
            
            while (i.hasNext()) {
                token = i.next();
                if (token.getType() == JavaTokenTypes.LBRACK) {
                    type = type.getArray();
                    i.next();  // RBRACK
                }
                else {
                    return null;
                }
            }
            
            return new TypeEntity(type);
        }
        
        String text = token.getText();
        
        JavaEntity poc = UnresolvedEntity.getEntity(resolver, text, querySource);
        while (poc != null && i.hasNext()) {
            token = i.next();
            if (token.getType() == JavaTokenTypes.LT) {
                // Type arguments
                poc = processTypeArgs(resolver, querySource, poc, i, depthRef);
                if (poc == null) {
                    return null;
                }
                if (! i.hasNext()) {
                    return poc;
                }
                token = i.next();
            }
            if (token.getType() != JavaTokenTypes.DOT) {
                while (token.getType() == JavaTokenTypes.LBRACK) {
                    poc = new UnresolvedArray(poc);
                    if (i.hasNext()) {
                        token = i.next(); // RBRACK
                    }
                    if (! i.hasNext()) {
                        return poc;
                    }
                    token = i.next();
                }
                
                i.previous(); // allow token to be re-read by caller
                return poc;
            }
            token = i.next();            
            if (token.getType() != JavaTokenTypes.IDENT) {
                break;
            }
            poc = poc.getSubentity(token.getText(), querySource);
        }
        
        return poc;
    }

    /**
     * Process tokens as type arguments
     * @param base  The base type, i.e. the type to which the arguments are applied
     * @param i     A ListIterator to iterate through the tokens
     * @param depthRef  The current argument depth; will be adjusted on return
     * @return   A JavaEntity representing the type with type arguments applied (or null)
     */
    private static JavaEntity processTypeArgs(EntityResolver resolver, Reflective querySource, 
            JavaEntity base, ListIterator<LocatableToken> i, DepthRef depthRef)
    {
        int startDepth = depthRef.depth;
        List<TypeArgumentEntity> taList = new LinkedList<TypeArgumentEntity>();
        depthRef.depth++;
        
        mainLoop:
        while (i.hasNext() && depthRef.depth > startDepth) {
            LocatableToken token = i.next();
            if (token.getType() == JavaTokenTypes.QUESTION) {
                if (! i.hasNext()) {
                    return null;
                }
                token = i.next();
                if (token.getType() == JavaTokenTypes.LITERAL_super) {
                    JavaEntity taEnt = getTypeEntity(resolver, querySource, i, depthRef);
                    if (taEnt == null) {
                        return null;
                    }
                    taList.add(new WildcardSuperEntity(taEnt));
                }
                else if (token.getType() == JavaTokenTypes.LITERAL_extends) {
                    JavaEntity taEnt = getTypeEntity(resolver, querySource, i, depthRef);
                    if (taEnt == null) {
                        return null;
                    }
                    taList.add(new WildcardExtendsEntity(taEnt));
                }
                else {
                    taList.add(new UnboundedWildcardEntity(resolver));
                    i.previous();
                }
            }
            else {
                i.previous();
                JavaEntity taEnt = getTypeEntity(resolver, querySource, i, depthRef);
                if (taEnt == null) {
                    return null;
                }
                taList.add(new SolidTargEntity(taEnt));
            }
            
            if (depthRef.depth <= startDepth) {
                // We've hit the closing '>' already
                break;
            }
            
            if (! i.hasNext()) {
                return null;
            }
            token = i.next();
            int ttype = token.getType();
            while (ttype == JavaTokenTypes.GT || ttype == JavaTokenTypes.SR || ttype == JavaTokenTypes.BSR) {
                switch (ttype) {
                case JavaTokenTypes.BSR:
                    depthRef.depth--;
                    // fall through to next case:
                case JavaTokenTypes.SR:
                    depthRef.depth--;
                    // fall through to default case:
                default:
                    depthRef.depth--;
                }
                if (! i.hasNext()) {
                    break mainLoop;
                }
                token = i.next();
                ttype = token.getType();
            }
            
            if (ttype != JavaTokenTypes.COMMA) {
                i.previous();
                break;
            }
        }
        return base.setTypeArgs(taList);
    }
}
