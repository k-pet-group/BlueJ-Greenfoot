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
package bluej.utility;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bluej.debugger.gentype.FieldReflective;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.JavaPrimitiveType;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.MethodReflective;
import bluej.debugger.gentype.Reflective;

/**
 * A reflective for GenTypeClass which uses the standard java reflection API.  
 * 
 * @author Davin McCall
 */
public class JavaReflective extends Reflective
{
    private Class<?> c;
    
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
    
    public JavaReflective(Class<?> c)
    {
        this.c = c;
    }
    
    public String getName()
    {
        return c.getName();
    }
    
    public String getSimpleName()
    {
        if (c.isArray()) {
            return c.getComponentType().getName().replace('$', '.') + "[]";
        }
        else {
            return c.getName().replace('$', '.');
        }
    }

    @Override
    public boolean isInterface()
    {
        return c.isInterface();
    }
    
    @Override
    public boolean isStatic()
    {
        return Modifier.isStatic(c.getModifiers());
    }
    
    @Override
    public boolean isPublic()
    {
        return Modifier.isPublic(c.getModifiers());
    }
    
    public List<GenTypeDeclTpar> getTypeParams()
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
            Class<?> arrClass = Class.forName(rname, false, cloader);
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
            Class<?> cr = cloader.loadClass(name);
            return new JavaReflective(cr);
        }
        catch (ClassNotFoundException cnfe) {
            return null;
        }
    }

    public List<Reflective> getSuperTypesR()
    {
        List<Reflective> l = new ArrayList<Reflective>();
        
        // Arrays must be specially handled
        if (c.isArray()) {
            Class<?> ct = c.getComponentType();  // could be primitive, but won't matter
            JavaReflective ctR = new JavaReflective(ct);
            List<Reflective> componentSuperTypes = ctR.getSuperTypesR();
            Iterator<Reflective> i = componentSuperTypes.iterator();
            while (i.hasNext()) {
                JavaReflective componentSuperType = (JavaReflective) i.next();
                l.add(componentSuperType.getArrayOf());
            }
        }
        
        Class<?> superclass = c.getSuperclass();
        if( superclass != null )
            l.add(new JavaReflective(superclass));

        Class<?> [] interfaces = c.getInterfaces();
        for( int i = 0; i < interfaces.length; i++ ) {
            l.add(new JavaReflective(interfaces[i]));
        }
        
        // Interfaces with no direct superinterfaces have a supertype of Object
        if (superclass == null && interfaces.length == 0 && c.isInterface())
            l.add(new JavaReflective(Object.class));
        
        return l;
    }

    public List<GenTypeClass> getSuperTypes()
    {
        List<GenTypeClass> l = new ArrayList<GenTypeClass>();

        // Arrays must be specially handled
        if (c.isArray()) {
            Class<?> ct = c.getComponentType();   // could be primitive (is ok)
            JavaReflective ctR = new JavaReflective(ct);
            List<GenTypeClass> componentSuperTypes = ctR.getSuperTypes(); // generic types
            Iterator<GenTypeClass> i = componentSuperTypes.iterator();
            while (i.hasNext()) {
                GenTypeClass componentSuperType = i.next();
                l.add(componentSuperType.getArray());
            }
        }

        GenTypeClass superclass = JavaUtils.getJavaUtils().getSuperclass(c);
        if( superclass != null ) {
            l.add(superclass);
        }
        
        GenTypeClass[] interfaces = JavaUtils.getJavaUtils().getInterfaces(c);
        for( int i = 0; i < interfaces.length; i++ ) {
            l.add(interfaces[i]);
        }

        // Interfaces with no direct superinterfaces have a supertype of Object
        if (superclass == null && interfaces.length == 0 && c.isInterface()) {
            l.add(new GenTypeClass(new JavaReflective(Object.class)));
        }
        
        return l;
    }
    
    /**
     * Get the underlying class (as a java.lang.Class object) that this
     * reflective represents.
     */
    public Class<?> getUnderlyingClass()
    {
        return c;
    }

    public boolean isAssignableFrom(Reflective r)
    {
        if (r instanceof JavaReflective) {
            return c.isAssignableFrom(((JavaReflective)r).getUnderlyingClass());
        }
        else {
            return false;
        }
    }
    
    @Override
    public Map<String,FieldReflective> getDeclaredFields()
    {
        try {
            Field [] fields = c.getDeclaredFields();
            Map<String,FieldReflective> rmap = new HashMap<String,FieldReflective>();
            for (int i = 0; i < fields.length; i++) {
                JavaType fieldType = JavaUtils.getJavaUtils().getFieldType(fields[i]);
                FieldReflective fref = new FieldReflective(fields[i].getName(), fieldType,
                        fields[i].getModifiers());
                rmap.put(fields[i].getName(), fref);
            }

            // See JLS section 10.7: arrays have a "public final int length" field
            if (c.isArray()) {
                rmap.put("length", new FieldReflective("length", JavaPrimitiveType.getInt(), Modifier.PUBLIC | Modifier.FINAL));
            }

            return rmap;
        }
        catch (LinkageError le) {
            // getDeclaredFields() can throw a LinkageError
            return Collections.emptyMap();
        }
    }
    
    @Override
    public Map<String,Set<MethodReflective>> getDeclaredMethods()
    {
        try {
            Method [] methods = c.getDeclaredMethods();
            Map<String,Set<MethodReflective>> rmap = new HashMap<String,Set<MethodReflective>>();
            for (Method method : methods) {
                if (method.isSynthetic()) {
                    continue;
                }

                JavaType rtype = JavaUtils.getJavaUtils().getReturnType(method);
                List<GenTypeDeclTpar> tpars = JavaUtils.getJavaUtils().getTypeParams(method);

                // We need to create a map from each type parameter name to its type
                // as a GenTypeDeclTpar
                Map<String,GenTypeDeclTpar> tparMap = new HashMap<String,GenTypeDeclTpar>();
                storeTparMappings(tpars, tparMap);
                if (! Modifier.isStatic(method.getModifiers())) {
                    getTparMapping(method.getDeclaringClass(), tparMap);
                }

                JavaType [] paramTypes = JavaUtils.getJavaUtils().getParamGenTypes(method, false);
                List<JavaType> paramTypesList = new ArrayList<JavaType>(paramTypes.length);
                for (JavaType paramType : paramTypes) {
                    paramTypesList.add(paramType.mapTparsToTypes(tparMap).getUpperBound());
                }

                rtype = rtype.mapTparsToTypes(tparMap).getUpperBound();

                String name = method.getName();
                MethodReflective mr = new MethodReflective(name, rtype, tpars, paramTypesList,
                        this,
                        JavaUtils.getJavaUtils().isVarArgs(method),
                        method.getModifiers());
                Set<MethodReflective> rset = rmap.get(method.getName());
                if (rset == null) {
                    rset = new HashSet<MethodReflective>();
                    rmap.put(method.getName(), rset);
                }
                rset.add(mr);
            }

            // See JLS section 10.7: arrays have a "public Object clone()" method
            if (c.isArray()) {
                rmap.put("clone", Collections.singleton(new MethodReflective("clone", new GenTypeClass(new JavaReflective(Object.class)), new ArrayList<GenTypeDeclTpar>(), new ArrayList<JavaType>(), this, false, Modifier.PUBLIC)));
            }

            return rmap;
        }
        catch (LinkageError le) {
            // getDeclaredMethods() can cause a LinkageError
            return Collections.emptyMap();
        }
    }
    
    private void getTparMapping(Class<?> c, Map<String,GenTypeDeclTpar> tparMap)
    {
        JavaUtils ju = JavaUtils.getJavaUtils();
        List<GenTypeDeclTpar> tpars = ju.getTypeParams(c);
        storeTparMappings(tpars, tparMap);
        
        Method m = c.getEnclosingMethod();
        Constructor<?> cc = c.getEnclosingConstructor();
        c = c.getEnclosingClass();
        
        while (c != null || m != null || cc != null) {
            if (c != null) {
                tpars = ju.getTypeParams(c);
                storeTparMappings(tpars, tparMap);
                c = c.getEnclosingClass();
                m = c.getEnclosingMethod();
                cc = c.getEnclosingConstructor();
            }
            else if (m != null) {
                tpars = ju.getTypeParams(m);
                storeTparMappings(tpars, tparMap);
                if (! Modifier.isStatic(m.getModifiers())) {
                    c = m.getDeclaringClass();
                }
                m = null;
            }
            else if (cc != null) {
                tpars = ju.getTypeParams(cc);
                storeTparMappings(tpars, tparMap);
                c = cc.getDeclaringClass();
                cc = null;
            }
        }
    }
    
    private void storeTparMappings(List<GenTypeDeclTpar> tpars, Map<String, ? super GenTypeDeclTpar> map)
    {
        for (GenTypeDeclTpar tpar : tpars) {
            if (! map.containsKey(tpar.getTparName())) {
                map.put(tpar.getTparName(), tpar);
            }
        }
    }
    
    @Override
    public List<Reflective> getInners()
    {
        Class<?>[] inners = c.getDeclaredClasses();
        List<Reflective> innersR = new ArrayList<Reflective>(inners.length);
        for (Class<?> inner : inners) {
            innersR.add(new JavaReflective(inner));
        }
        return innersR;
    }
    
    @Override
    public Reflective getOuterClass()
    {
        Class<?> declaring = c.getDeclaringClass();
        if (declaring != null) {
            return new JavaReflective(declaring);
        }
        return null;
    }
}
