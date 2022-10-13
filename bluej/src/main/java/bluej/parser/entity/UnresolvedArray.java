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

/**
 * An array entity, where the element type hasn't yet been resolved.
 * 
 * @author Davin McCall
 */
public class UnresolvedArray extends JavaEntity
{
    private JavaEntity baseType;
    
    public UnresolvedArray(JavaEntity baseType)
    {
        this.baseType = baseType;
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
        return null;
    }

    @Override
    public JavaEntity setTypeArgs(List<TypeArgumentEntity> tparams)
    {
        return null;
    }
    
    @Override
    public TypeEntity resolveAsType()
    {
        TypeEntity bt = baseType.resolveAsType();
        if (bt != null) {
            return new TypeEntity(bt.getType().getArray());
        }
        return null;
    }

}
