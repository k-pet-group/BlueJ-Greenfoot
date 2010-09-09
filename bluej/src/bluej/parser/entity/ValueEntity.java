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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import bluej.debugger.gentype.FieldReflective;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.GenTypeSolid;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;
import bluej.utility.JavaUtils;

/**
 * Represents a value entity - an unspecified value with a known type.
 * 
 * @author Davin McCall
 */
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
    public JavaEntity getSubentity(String name, Reflective accessor)
    {
        GenTypeSolid [] ubounds = type.getUpperBounds();
        if (ubounds == null || ubounds.length == 0) {
            return null;
        }
        
        GenTypeClass ctype = ubounds[0].asClass();
        
        if (ctype != null) {
            Reflective ctypeRef = ctype.getReflective();
            LinkedList<Reflective> stypes = new LinkedList<Reflective>();
            stypes.add(ctypeRef);
            
            FieldReflective field = null;
            
            while (! stypes.isEmpty()) {
                ctypeRef = stypes.poll();
                Map<String,FieldReflective> fields = ctypeRef.getDeclaredFields();
                field = fields.get(name);
                if (field != null) {
                    break;
                }
                stypes.addAll(ctypeRef.getSuperTypesR());
            }
            
            if (field != null) {
                ctype = ctype.mapToSuper(ctypeRef.getName());
                if (JavaUtils.checkMemberAccess(ctype.getReflective(), type.asSolid(), accessor,
                        field.getModifiers(), false)) {
                    JavaType fieldType = field.getType();
                    Map<String,GenTypeParameter> tparMap = ctype.getMap();
                    fieldType = fieldType.mapTparsToTypes(tparMap).getCapture();
                    return new ValueEntity(this.name + "." + name, fieldType);
                }
            }
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
