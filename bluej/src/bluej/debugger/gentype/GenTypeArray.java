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
package bluej.debugger.gentype;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * A specialization of GenTypeClass for arrays.
 * 
 * @author Davin McCall
 */
public class GenTypeArray extends GenTypeClass
{
    JavaType baseType;
    
    /**
     * Construct a new GenTypeArray, with the given component type and reflective.
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
    
    public String arrayComponentName()
    {
        return "[" + baseType.arrayComponentName();
    }
    
    @Override
    public String classloaderName()
    {
        return arrayComponentName();
    }
    
    public JavaType getArrayComponent()
    {
        return baseType;
    }
    
    public GenTypeClass mapTparsToTypes(Map<String, ? extends GenTypeParameter> tparams)
    {
        JavaType newBase = baseType.mapTparsToTypes(tparams);
        if( newBase == baseType )
            return this;
        else
            return new GenTypeArray(newBase);
    }

    public GenTypeSolid getLowerBound()
    {
        if (baseType.isPrimitive())
            return this;
        else {
            GenTypeSolid Lbounds = ((GenTypeParameter) baseType).getLowerBound();
            return new GenTypeArray(Lbounds);
        }
    }
    
    public GenTypeClass getErasedType()
    {
        if (baseType instanceof GenTypeParameter) {
            GenTypeParameter pbtype = (GenTypeParameter) baseType;
            JavaType pbErased = pbtype.getErasedType();
            return new GenTypeArray(pbErased);
        }
        else
            return this;
    }

    public void erasedSuperTypes(Set<Reflective> s)
    {
        Stack<Reflective> refs = new Stack<Reflective>();
        if (baseType instanceof GenTypeSolid) {
            GenTypeSolid sbaseType = (GenTypeSolid) baseType;
            Set<Reflective> baseEST = new HashSet<Reflective>();
            sbaseType.erasedSuperTypes(baseEST);
            Iterator<Reflective> i = baseEST.iterator();
            while (i.hasNext()) {
                refs.push(((Reflective) i.next()).getArrayOf());
            }
        }
        else
            // DAV how can this work if reflective == null?!
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
