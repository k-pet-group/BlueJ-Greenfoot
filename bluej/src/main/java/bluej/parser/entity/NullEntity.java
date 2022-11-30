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

import bluej.debugger.gentype.JavaPrimitiveType;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;

/**
 * The "null" entity. This represents the "null" value (literal).
 * 
 * @author Davin McCall
 */
public class NullEntity extends ValueEntity
{
    public NullEntity()
    {
        super(JavaPrimitiveType.getNull());
    }
    
    @Override
    public String getName()
    {
        return null;
    }

    @Override
    public JavaEntity getSubentity(String name, Reflective accessSource)
    {
        return null;
    }

    @Override
    public JavaType getType()
    {
        return JavaPrimitiveType.getNull();
    }
    
    @Override
    public boolean isNullEntity()
    {
        return true;
    }
    
    @Override
    public JavaEntity setTypeArgs(List<TypeArgumentEntity> tparams)
    {
        return null;
    }

    @Override
    public ValueEntity resolveAsValue()
    {
        return this;
    }
}
