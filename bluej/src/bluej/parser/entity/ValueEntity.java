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
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;

public class ValueEntity extends JavaEntity
{
    private String name;
    private JavaType type;
    
    public ValueEntity(JavaType type)
    {
        this.type = type;
    }
    
    public ValueEntity(String name, JavaType type)
    {
        this.name = name;
        this.type = type;
    }
    
    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public JavaEntity getSubentity(String name, Reflective accessSource)
    {
        GenTypeClass ctype = type.asClass();
        if (ctype != null) {
            // ctype.getReflective().
            // TODO
        }
        return null;
    }

    @Override
    public JavaType getType()
    {
        return type;
    }

    @Override
    public JavaEntity resolveAsValue()
    {
        return this;
    }
    
    @Override
    public JavaEntity setTypeArgs(List<TypeArgumentEntity> tparams)
    {
        return null;
    }
}
