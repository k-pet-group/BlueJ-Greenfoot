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
package bluej.parser.entity;

import bluej.debugger.gentype.JavaType;

/**
 * A general abstraction for handling entities which may have fields or
 * members - including packages, classes, and values.
 * 
 * <p>Entities can be unresolved, meaning that they represent an expression such
 * as "xyz.abc" where "xyz" could be a package, class or local member.
 * 
 * @author Davin McCall
 * @version $Id$
 */
public abstract class JavaEntity
{
    /**
     * If this entity is unresolved, resolve it now as a value.
     */
    public JavaEntity resolveAsValue()
    {
        return null;
    }
    
    /**
     * If this entity is unresolved, resolve it now as a value or a type.
     */
//    public JavaEntity resolveAsValOrType() throws SemanticException
//    {
//        throw new SemanticException();
//    }
    
    /**
     * If this entity is unresolved, resolve it now as a type.
     */
    public JavaEntity resolveAsType()
    {
        return null;
    }
    
    /**
     * Get the type of the entity. For a class entity, this is the class itself
     * (may be generic). For a value this is the value type.
     * 
     * Returns null if no type is available.
     */ 
    public abstract JavaType getType();
    
    /**
     * Get a sub-entity (member, field, whatever) by name.
     * @param name  The name of the subentity
     * @return  The subentity  (or null if one does not exist)
     */
    public abstract JavaEntity getSubentity(String name);
    
    public abstract String getName();
}
