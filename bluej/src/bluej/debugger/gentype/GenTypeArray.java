/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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
package bluej.debugger.gentype;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import bluej.utility.JavaReflective;

/**
 * A specialization of GenTypeClass for arrays.
 * 
 * @author Davin McCall
 */
public class GenTypeArray extends GenTypeSolid
{
    JavaType baseType;
    
    /**
     * Construct a new GenTypeArray, with the given component type.
     */
    public GenTypeArray(JavaType baseType)
    {
        super();
        this.baseType = baseType;
    }

    public String toString(boolean stripPrefix)
    {
        return baseType.toString(stripPrefix) + "[]";
    }
    
    public String toString(NameTransform nt)
    {
        return baseType.toString(nt) + "[]";
    }

    @Override
    public String toTypeArgString(NameTransform nt)
    {
        return toString(nt);
    }

    public String arrayComponentName()
    {
        return "[" + baseType.getUpperBound().arrayComponentName();
    }
    
    @Override
    public JavaType getCapture()
    {
        JavaType baseCap = baseType.getCapture();
        if (baseCap == baseType) {
            return this;
        }
        return baseCap.getArray();
    }
        
    public JavaType getArrayComponent()
    {
        return baseType;
    }
    
    public GenTypeSolid getLowerBound()
    {
        return this;
    }

    @Override
    public boolean equals(JavaType other)
    {
        return baseType.equals(other.getArrayComponent());
    }
    
    @Override
    public void erasedSuperTypes(Set<Reflective> s)
    {
        GenTypeSolid baseSolid = baseType.getUpperBound().asSolid();
        if (baseSolid != null) {
            Set<Reflective> bSupers = new HashSet<Reflective>();
            baseSolid.erasedSuperTypes(bSupers);
            for (Reflective r : bSupers) {
                s.add(r.getArrayOf());
            }
        }
        else {
            // Must be primitive
            Class<?> aClass = null;
            JavaType baseType = this.baseType.getUpperBound();
            if (baseType.typeIs(JT_VOID)) {
                aClass = void.class;
            }
            else if (baseType.typeIs(JT_BOOLEAN)) {
                aClass = boolean.class;
            }
            else if (baseType.typeIs(JT_BYTE)) {
                aClass = byte.class;
            }
            else if (baseType.typeIs(JT_CHAR)) {
                aClass = char.class;
            }
            else if (baseType.typeIs(JT_DOUBLE)) {
                aClass = double.class;
            }
            else if (baseType.typeIs(JT_FLOAT)) {
                aClass = float.class;
            }
            else if (baseType.typeIs(JT_INT)) {
                aClass = int.class;
            }
            else if (baseType.typeIs(JT_LONG)) {
                aClass = long.class;
            }
            s.add(new JavaReflective(aClass).getArrayOf());
        }
    }
    
    @Override
    public GenTypeArray getArray()
    {
        return new GenTypeArray(this);
    }
    
    @Override
    public JavaType getErasedType()
    {
        JavaType baseErased = baseType.getErasedType();
        if (baseErased == baseType) {
            return this;
        }
        else {
            return baseErased.getArray();
        }
    }
    
    @Override
    public void getParamsFromTemplate(Map<String, GenTypeParameter> map,
            GenTypeParameter template)
    {
        GenTypeParameter ntemplate = template.getArrayComponent();
        if (ntemplate != null) {
            baseType.getParamsFromTemplate(map, ntemplate);
        }
    }
        
    @Override
    public GenTypeClass[] getReferenceSupertypes()
    {
        // There's not really much we can do here
        return new GenTypeClass[0];
    }
    
    @Override
    public GenTypeParameter mapTparsToTypes(Map<String, ? extends GenTypeParameter> tparams)
    {
        GenTypeParameter mappedBase = baseType.mapTparsToTypes(tparams);
        if (mappedBase != baseType) {
            if (mappedBase.isWildcard()) {
                // An array of wildcard should be represented instead as a wildcard
                // with array bounds.
                GenTypeSolid ubound = mappedBase.getUpperBound().asSolid();
                ubound = (ubound == null) ? ubound : ubound.getArray();
                GenTypeSolid lbound = mappedBase.getLowerBound();
                lbound = (lbound == null) ? lbound : lbound.getArray();
                new GenTypeWildcard(ubound, lbound);
            }
            return mappedBase.getUpperBound().getArray();
        }
        return this;
    }
    
    @Override
    public boolean isAssignableFrom(JavaType t)
    {
        JavaType componentType = t.getArrayComponent();
        if (componentType != null) {
            return baseType.getCapture().isAssignableFrom(componentType);
        }
        return false;
    }
    
    @Override
    public boolean isAssignableFromRaw(JavaType t)
    {
        JavaType componentType = t.getArrayComponent();
        if (componentType != null) {
            return baseType.getCapture().isAssignableFromRaw(componentType);
        }
        return false;
    }
    
    @Override
    public boolean isInterface()
    {
        return false;
    }
    
}
