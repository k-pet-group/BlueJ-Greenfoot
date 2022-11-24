/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011  Michael Kolling and John Rosenberg 
 
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

import java.util.List;

import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A general abstraction for handling entities which may have fields or
 * members - including packages, classes, and values.
 * 
 * <p>Entities can be unresolved, meaning that they represent an expression such
 * as "xyz.abc" where "xyz" could be a package, class or local member.
 * 
 * @author Davin McCall
 */
public abstract class JavaEntity
{
    /**
     * If this entity is unresolved, resolve it now as a value.
     */
    @OnThread(Tag.FXPlatform)
    public ValueEntity resolveAsValue()
    {
        return null;
    }
    
    /**
     * If this entity is unresolved, resolve it now as a type.
     */
    @OnThread(Tag.FXPlatform)
    public TypeEntity resolveAsType()
    {
        return null;
    }
    
    /**
     * If this entity is unresolved, resolve it now as either a package or a
     * qualified class.
     */
    @OnThread(Tag.FXPlatform)
    public PackageOrClass resolveAsPackageOrClass()
    {
        return resolveAsType();
    }
    
    /**
     * Get the type of the entity. For a class entity, this is the class itself
     * (may be generic). For a value this is the value type.
     * 
     * <p>You should always resolve the entity to either a value or type before
     * calling this method.
     * 
     * <p>Returns null if no type is available or undetermined (i.e. if the entity
     * has not properly been resolved).
     */
    @OnThread(Tag.FXPlatform)
    public abstract JavaType getType();
    
    /**
     * Check whether this entity represents "null". "null" has no type.
     */
    public boolean isNullEntity()
    {
        return false;
    }
    
    /**
     * Get a sub-entity (member, field, whatever) by name.
     * @param name  The name of the subentity
     * @param accessSource  The source of the access (for access control purposes)
     * @return  The subentity  (or null if one does not exist)
     */
    @OnThread(Tag.FXPlatform)
    public abstract JavaEntity getSubentity(String name, Reflective accessSource);
    
    /**
     * Get the name of the entity. This returns the fully-qualified name of the
     * entity.
     */
    public abstract String getName();
    
    /**
     * Check whether this identity represents a single identifier. If it does,
     * this method returns true and getName() returns the identifier.
     */
    public boolean isIdentifier()
    {
        return false;
    }
    
    /**
     * Set the type parameters of this entity. The return is a duplicate of this entity with
     * the type parameters set as specified. If type parameters cannot be applied to this
     * entity the return is null.
     * 
     * @param tparams   A list of type parameters
     */
    public abstract JavaEntity setTypeArgs(List<TypeArgumentEntity> tparams);
}
