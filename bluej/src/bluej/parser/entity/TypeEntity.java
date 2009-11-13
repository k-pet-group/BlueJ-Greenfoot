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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;
import bluej.parser.SemanticException;
import bluej.utility.JavaReflective;

/**
 * An entity which essentially wraps a Reflective.
 * 
 * TODO clean up.
 * 
 * @author Davin McCall
 */
public class TypeEntity extends ClassEntity
{
    //Class thisClass;
    List tparams;
    GenTypeClass outer;
    Reflective thisRef;
    GenTypeClass thisClass;
    
    
    public TypeEntity(Reflective ref)
    {
        thisRef = ref;
        tparams = Collections.EMPTY_LIST;
    }
    
    public TypeEntity(Class c)
    {
        //thisClass = c;
        thisRef = new JavaReflective(c);
        tparams = Collections.EMPTY_LIST;
    }
    
    TypeEntity(Class c, List tparams)
    {
        //thisClass = c;
        thisRef = new JavaReflective(c);
        this.tparams = tparams;
    }
    
    TypeEntity(Class c, GenTypeClass outer)
    {
        //thisClass = c;
        thisRef = new JavaReflective(c);
        this.outer = outer;
        tparams = Collections.EMPTY_LIST;
    }
    
    TypeEntity(Reflective r, GenTypeClass outer)
    {
        thisRef = r;
        this.outer = outer;
        tparams = Collections.EMPTY_LIST;
    }

    TypeEntity(Class c, GenTypeClass outer, List tparams)
    {
        //thisClass = c;
        thisRef = new JavaReflective(c);
        this.outer = outer;
        this.tparams = tparams;
    }
            
    TypeEntity(Reflective r, GenTypeClass outer, List tparams)
    {
        thisRef = r;
        this.outer = outer;
        this.tparams = tparams;
    }

    public ClassEntity setTypeParams(List tparams) throws SemanticException
    {
        // this.tparams = tparams;
        return new TypeEntity(thisRef, outer, tparams);
    }

    public JavaType getType()
    {
        return getClassType();
    }
    
    public GenTypeClass getClassType()
    {
        return new GenTypeClass(thisRef, tparams, outer);
    }
    
    public JavaEntity getSubentity(String name)
    {
        // subentity of a class could be a member type or field
        // Is it a field?
        Map<String,JavaType> m = thisRef.getDeclaredFields();
        JavaType type = m.get(name);
        if (type != null) {
            return new ValueEntity(name, type);
        }

        // Is it a member type?
        return getPackageOrClassMember(name);
    }
    
    public PackageOrClass getPackageOrClassMember(String name)
    {
        // A class cannot have a package member...
        return new TypeEntity(getMemberClass(name), getClassType());
    }
    
//    Class getMemberClass(String name)
//    {
//        // Is it a member type?
//        Class c;
//        try {
//            c = classLoader.loadClass(thisClass.getName() + '$' + name);
//            return c;
//            //return new TypeEntity(c, (GenTypeClass) getType());
//        }
//        catch (ClassNotFoundException cnfe) {
//            // No more options - it must be an error
//            return null;
//        }
//    }
    
    Reflective getMemberClass(String name)
    {
        // Is it a member type?
        return thisRef.getRelativeClass(thisRef.getName() + '$' + name);
    }

//    public ClassEntity getStaticMemberClass(String name) throws SemanticException
//    {
//        Class c = getMemberClass(name);
//        if (Modifier.isStatic(c.getModifiers()))
//            return new TypeEntity(c, (GenTypeClass) getType());
//        
//        // Not a static member - we fail
//        throw new SemanticException();
//    }
    
    public JavaEntity getStaticField(String name)
    {
        Map<String,JavaType> m = thisRef.getDeclaredFields();
        JavaType type = m.get(name);
        if (type != null) {
            // TODO
        }
        return null;
    }
    
//    public List getStaticMethods(String name)
//    {
//        return getAccessibleStaticMethods(thisClass, name, packageScope);
//    }
    
    public String getName()
    {
        return getType().toString();
    }
    
    public boolean isClass()
    {
        return true;
    }
}
