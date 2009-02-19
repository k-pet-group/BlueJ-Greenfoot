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
package bluej.utility;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.GenTypeArray;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.Reflective;

/**
 * A reflective for GenTypeClass which uses the standard java reflection API.  
 * 
 * @author Davin McCall
 * @version $Id: JavaReflective.java 6164 2009-02-19 18:11:32Z polle $
 */
public class JavaReflective extends Reflective {

    private Class c;
    
    public int hashCode()
    {
        return c.hashCode();
    }
    
    public boolean equals(Object other)
    {
        if (other instanceof JavaReflective) {
            JavaReflective jrOther = (JavaReflective) other;
            return jrOther.c == c;
        }
        return false;
    }
    
    public JavaReflective(Class c)
    {
        this.c = c;
    }
    
    public String getName()
    {
        return c.getName();
    }

    public boolean isInterface()
    {
        return c.isInterface();
    }
    
    public boolean isStatic()
    {
        return (c.getModifiers() & Modifier.STATIC) != 0;
    }
    
    public List getTypeParams()
    {
        return JavaUtils.getJavaUtils().getTypeParams(c);
    }
    
    public Reflective getArrayOf()
    {
        String rname;
        if (c.isArray())
            rname = "[" + c.getName();
        else
            rname = "[L" + c.getName() + ";";
        
        try {
            ClassLoader cloader = c.getClassLoader();
            if (cloader == null)
                cloader = ClassLoader.getSystemClassLoader();
            Class arrClass = cloader.loadClass(rname);
            return new JavaReflective(arrClass);
        }
        catch (ClassNotFoundException cnfe) {}
        
        return null;
    }
    
    public Reflective getRelativeClass(String name)
    {
        try {
            ClassLoader cloader = c.getClassLoader();
            if (cloader == null)
                cloader = ClassLoader.getSystemClassLoader();
            Class cr = cloader.loadClass(name);
            return new JavaReflective(cr);
        }
        catch (ClassNotFoundException cnfe) {
            return null;
        }
    }

    public List getSuperTypesR() {
        List l = new ArrayList();
        
        // Arrays must be specially handled
        if (c.isArray()) {
            Class ct = c.getComponentType();  // could be primitive, but won't matter
            JavaReflective ctR = new JavaReflective(ct);
            List componentSuperTypes = ctR.getSuperTypesR();
            Iterator i = componentSuperTypes.iterator();
            while (i.hasNext()) {
                JavaReflective componentSuperType = (JavaReflective) i.next();
                l.add(componentSuperType.getArrayOf());
            }
        }
        
        Class superclass = c.getSuperclass();
        if( superclass != null )
            l.add(new JavaReflective(superclass));

        Class [] interfaces = c.getInterfaces();
        for( int i = 0; i < interfaces.length; i++ ) {
            l.add(new JavaReflective(interfaces[i]));
        }
        
        // Interfaces with no direct superinterfaces have a supertype of Object
        if (superclass == null && interfaces.length == 0 && c.isInterface())
            l.add(new JavaReflective(Object.class));
        
        return l;
    }

    public List getSuperTypes() {
        List l = new ArrayList();

        // Arrays must be specially handled
        if (c.isArray()) {
            Class ct = c.getComponentType();   // could be primitive (is ok)
            JavaReflective ctR = new JavaReflective(ct);
            List componentSuperTypes = ctR.getSuperTypes(); // generic types
            Iterator i = componentSuperTypes.iterator();
            while (i.hasNext()) {
                GenTypeClass componentSuperType = (GenTypeClass) i.next();
                l.add(new GenTypeArray(componentSuperType, componentSuperType.getReflective().getArrayOf()));
            }
        }

        JavaType superclass = JavaUtils.getJavaUtils().getSuperclass(c);
        if( superclass != null )
            l.add(superclass);
        
        JavaType [] interfaces = JavaUtils.getJavaUtils().getInterfaces(c);
        for( int i = 0; i < interfaces.length; i++ ) {
            l.add(interfaces[i]);
        }

        // Interfaces with no direct superinterfaces have a supertype of Object
        if (superclass == null && interfaces.length == 0 && c.isInterface())
            l.add(new GenTypeClass(new JavaReflective(Object.class)));
        
        return l;
    }
    
    /**
     * Get the underlying class (as a java.lang.Class object) that this
     * reflective represents.
     */
    public Class getUnderlyingClass()
    {
        return c;
    }

    public boolean isAssignableFrom(Reflective r)
    {
        if (r instanceof JavaReflective) {
            return c.isAssignableFrom(((JavaReflective)r).getUnderlyingClass());
        }
        else
            return false;
    }
}
