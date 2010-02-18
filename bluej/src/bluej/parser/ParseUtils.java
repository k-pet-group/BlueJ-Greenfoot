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

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import bluej.debugger.gentype.JavaPrimitiveType;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;
import bluej.parser.entity.EntityResolver;
import bluej.parser.entity.JavaEntity;
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
     * @param depthRef  The argument depth
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
                    taList.add(new UnboundedWildcardEntity());
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
            
            if (! i.hasNext()) {
                return null;
            }
            token = i.next();
            int ttype = token.getType();
            while (ttype == JavaTokenTypes.GT || ttype == JavaTokenTypes.SR || ttype == JavaTokenTypes.BSR) {
                switch (ttype) {
                case JavaTokenTypes.BSR:
                    depthRef.depth--;
                case JavaTokenTypes.SR:
                    depthRef.depth--;
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
