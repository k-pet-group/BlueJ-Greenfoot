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

import bluej.debugger.gentype.JavaType;

/**
 * A general abstraction for handling entities which may have fields or
 * members - including packages, classes, and values.
 * 
 * @author Davin McCall
 * @version $Id$
 */
abstract class JavaEntity
{
    /**
     * Get the type of the entity. For a class entity, this is the class itself
     * (may be generic). For a value this is the value type.
     * 
     * Throws SemanticException if the entity doesn't have an
     * assosciated type (for instance, it represents a package)
     */ 
    abstract JavaType getType() throws SemanticException;
    
    /**
     * Get a sub-entity (member, field, whatever) by name.
     * @param name  The name of the subentity
     * @return  The subentity
     * @throws SemanticException  if the given subentity doesn't exist
     */
    abstract JavaEntity getSubentity(String name) throws SemanticException;
    
    abstract boolean isClass();
    
    abstract String getName();
}
