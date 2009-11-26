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

import java.util.List;

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeParameterizable;
import bluej.debugger.gentype.JavaType;
import bluej.parser.SemanticException;


/**
 * An entity representing a class or generic type.
 * 
 * TODO cleanup.
 * 
 * @author Davin McCall
 */
public abstract class ClassEntity extends PackageOrClass
{
    public JavaType getType()
    {
        return getClassType();
    }
    
    @Override
    public final ClassEntity resolveAsType()
    {
        return this;
    }
    
    /**
     * Get the type represented by this entity as a GenTypeClass.
     */
    public abstract GenTypeClass getClassType();
    
    /**
     * Set the type parameters of this entity. The return is a duplicate of this entity with
     * the type parameters set as specified.
     * 
     * @param tparams   A list of GenTypeParameterizable type parameters
     * @throws SemanticException
     */
    public abstract ClassEntity setTypeParams(List<GenTypeParameterizable> tparams);
}
