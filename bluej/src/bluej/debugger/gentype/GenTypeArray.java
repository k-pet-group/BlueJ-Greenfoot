/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class GenTypeArray extends GenTypeClass
{
    JavaType baseType;
    
    public GenTypeArray(JavaType baseType, Reflective r)
    {
        super(r);
        this.baseType = baseType;
    }

    public String toString(boolean stripPrefix)
    {
        return baseType.toString(stripPrefix) + "[]";
    }
    
    public String toString(NameTransform nt)
    {
        if(baseType instanceof GenTypeParameterizable)
            return ((GenTypeParameterizable)baseType).toString(nt) + "[]";
        else
            return baseType.toString() + "[]";
    }
    
    public String arrayComponentName()
    {
        return "[" + baseType.arrayComponentName();
    }
    
    public JavaType getArrayComponent()
    {
        return baseType;
    }
    
    public JavaType mapTparsToTypes(Map tparams)
    {
        JavaType newBase = baseType.mapTparsToTypes(tparams);
        if( newBase == baseType )
            return this;
        else
            return new GenTypeArray(newBase, reflective);
    }

    public GenTypeSolid getLowerBound()
    {
        if (baseType.isPrimitive())
            return this;
        else {
            GenTypeSolid Lbounds = ((GenTypeParameterizable) baseType).getLowerBound();
            Reflective newR = Lbounds.getErasedType().asClass().reflective.getArrayOf();
            return new GenTypeArray(Lbounds, newR);
        }
    }
    
    public JavaType getErasedType()
    {
        if (baseType instanceof GenTypeParameterizable) {
            GenTypeParameterizable pbtype = (GenTypeParameterizable) baseType;
            GenTypeClass pbErased = (GenTypeClass) pbtype.getErasedType();
            return new GenTypeArray(pbErased, pbErased.reflective.getArrayOf());
        }
        else
            return this;
    }

    public void erasedSuperTypes(Set s)
    {
        Stack refs = new Stack();
        if (baseType instanceof GenTypeSolid) {
            GenTypeSolid sbaseType = (GenTypeSolid) baseType;
            Set baseEST = new HashSet();
            sbaseType.erasedSuperTypes(baseEST);
            Iterator i = baseEST.iterator();
            while (i.hasNext()) {
                refs.push(((Reflective) i.next()).getArrayOf());
            }
        }
        else
            refs.push(reflective);
        
        while(! refs.empty()) {
            Reflective r = (Reflective) refs.pop();
            if (! s.contains(r)) {
                // The reflective is not already in the set, so
                // add it and queue its supertypes
                s.add(r);
                refs.addAll(r.getSuperTypesR());
            }
        }
    }

}
