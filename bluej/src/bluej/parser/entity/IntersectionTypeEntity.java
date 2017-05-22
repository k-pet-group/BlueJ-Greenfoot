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
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.parser.entity;

import java.util.Iterator;
import java.util.List;

import bluej.debugger.gentype.GenTypeSolid;
import bluej.debugger.gentype.IntersectionType;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An entity representing intersection types (such as the bounds for type parameters).
 * 
 * @author Davin McCall
 */
public class IntersectionTypeEntity extends JavaEntity
{
    private List<JavaEntity> types;
    
    /**
     * Get an entity representing an intersection of the given types. If there are no types,
     * this yields a "java.lang.Object" entity. If there is only one type, this returns that
     * type.
     */
    @OnThread(Tag.FXPlatform)
    public static JavaEntity getIntersectionEntity(List<JavaEntity> types, EntityResolver resolver)
    {
        if (types.size() == 0) {
            return resolver.resolveQualifiedClass("java.lang.Object");
        }
        if (types.size() == 1) {
            return types.get(0);
        }
        return new IntersectionTypeEntity(types);
    }
    
    private IntersectionTypeEntity(List<JavaEntity> types)
    {
        this.types = types;
    }
    
    @Override
    public String getName()
    {
        Iterator<JavaEntity> i = types.iterator();
        String name = i.next().getName();
        for ( ; i.hasNext(); ) {
            name += "&" + i.next().getName();
        }
        return name;
    }
    
    @Override
    public JavaEntity getSubentity(String name, Reflective accessSource)
    {
        return null;
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public JavaType getType()
    {
        GenTypeSolid [] components = new GenTypeSolid[types.size()];
        int index = 0;
        for (JavaEntity type : types) {
            TypeEntity tent = type.resolveAsType();
            if (tent == null) {
                return null;
            }
            components[index] = tent.getType().asSolid();
            if (components[index++] == null) {
                // Not a "solid" type.
                return null;
            }
        }
        
        return IntersectionType.getIntersection(components);
    }
    
    @Override
    public JavaEntity setTypeArgs(List<TypeArgumentEntity> tparams)
    {
        return null;
    }
    
    @Override
    public TypeEntity resolveAsType()
    {
        JavaType type = getType();
        if (type == null) {
            return null;
        }
        return new TypeEntity(type);
    }
    
}
