/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.parser.entity;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;
import bluej.utility.JavaReflective;

/**
 * An entity which essentially wraps a JavaType.
 * 
 * @author Davin McCall
 */
public class TypeEntity extends PackageOrClass
{
    private JavaType thisType;
    
    public TypeEntity(JavaType type)
    {
        thisType = type;
    }
    
    public TypeEntity(Reflective ref)
    {
        //thisRef = ref;
        thisType = new GenTypeClass(ref);
    }
    
    public TypeEntity(Class<?> c)
    {
        Reflective thisRef = new JavaReflective(c);
        thisType = new GenTypeClass(thisRef);
    }
    
    TypeEntity(Reflective r, GenTypeClass outer)
    {
        // thisRef = r;
        thisType = new GenTypeClass(r, Collections.<GenTypeParameter>emptyList(), outer);
    }

    public JavaType getType()
    {
        return thisType;
    }
    
    public GenTypeClass getClassType()
    {
        return thisType.getCapture().asClass();
    }
    
    public JavaEntity getSubentity(String name, Reflective reflective)
    {
        GenTypeClass thisClass = thisType.getCapture().asClass();
        if (thisClass == null) {
            return null;
        }
        else if (thisClass.getArrayComponent() != null) {
            // Hmm, should we handle array.length here?
            return null;
        }
        
        // subentity of a class could be a member type or field
        // Is it a field?
        
        Map<String,JavaType> m = thisClass.getReflective().getDeclaredFields();
        JavaType type = m.get(name);
        if (type != null) {
            return new ValueEntity(name, type);
        }

        // Is it a member type?
        return getPackageOrClassMember(name);
    }
    
    public TypeEntity getPackageOrClassMember(String name)
    {
        GenTypeClass thisClass = thisType.getCapture().asClass();
        if (thisClass == null) {
            return null;
        }

        Reflective thisRef = thisClass.getReflective();
        if (thisRef != null) {
            Reflective member = thisRef.getRelativeClass(thisRef.getName() + '$' + name);
            if (member != null) {
                GenTypeClass inner = new GenTypeClass(member,
                    Collections.<GenTypeParameter>emptyList(), thisClass);
                return new TypeEntity(inner);
            }
        }
        
        return null;
    }
    
    public String getName()
    {
        return getType().toString();
    }
    
    /* (non-Javadoc)
     * @see bluej.parser.entity.ClassEntity#setTypeParams(java.util.List)
     */
    public TypeEntity setTypeArgs(List<TypeArgumentEntity> tparams)
    {
        GenTypeClass classType = thisType.getCapture().asClass();
        if (classType == null) {
            return null;
        }
        GenTypeClass outer = classType.getOuterType();
        List<GenTypeParameter> ttparams = new LinkedList<GenTypeParameter>();
        for (TypeArgumentEntity eparam : tparams) {
            GenTypeParameter tparamType = eparam.getType();
            if (tparamType == null) {
                return null;
            }
            ttparams.add(tparamType);
        }
        
        return new TypeEntity(new GenTypeClass(classType.getReflective(), ttparams, outer));
    }
    
    @Override
    public TypeEntity resolveAsType()
    {
        return this;
    }
}
