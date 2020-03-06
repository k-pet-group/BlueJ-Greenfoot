/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2011,2013,2014,2015,2016,2018,2019,2020  Michael Kolling and John Rosenberg
 
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

import bluej.debugger.gentype.ConstructorReflective;
import bluej.debugger.gentype.FieldReflective;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.JavaPrimitiveType;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.MethodReflective;
import bluej.debugger.gentype.Reflective;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A reflective for GenTypeClass which uses the standard java reflection API.  
 * 
 * @author Davin McCall
 */
public class JavaReflective extends Reflective
{
    private Class<?> c;
    
    @Override
    public int hashCode()
    {
        return c.hashCode();
    }
    
    @Override
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
        if (c == null)
            throw new NullPointerException();
        this.c = c;
    }
    
    @Override
    public String getName()
    {
        return c.getName();
    }
    
    @Override
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
    
    @Override
    public boolean isFinal()
    {
        return Modifier.isFinal(c.getModifiers());
    }
    
    @Override
    public List<GenTypeDeclTpar> getTypeParams()
    {
        return JavaUtils.getJavaUtils().getTypeParams(c);
    }
    
    @Override
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
    
    @Override
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
        catch (LinkageError le) {
            return null;
        }
    }

    @Override
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

    @Override
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

        GenTypeClass superclass = null;
        try {
            superclass = JavaUtils.getJavaUtils().getSuperclass(c);
            if( superclass != null ) {
                l.add(superclass);
            }
        }
        catch (ClassNotFoundException cnfe) {}
        
        GenTypeClass[] interfaces;
        try {
            interfaces = JavaUtils.getJavaUtils().getInterfaces(c);
            for( int i = 0; i < interfaces.length; i++ ) {
                l.add(interfaces[i]);
            }
        }
        catch (ClassNotFoundException cnfe) {
            interfaces = new GenTypeClass[0];
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

    @Override
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
                try {
                    JavaType fieldType = JavaUtils.getJavaUtils().getFieldType(fields[i]);
                    FieldReflective fref = new FieldReflective(fields[i].getName(), fieldType,
                            fields[i].getModifiers(), this);
                    rmap.put(fields[i].getName(), fref);
                }
                catch (ClassNotFoundException cnfe) {
                    // Can happen if a type parameter cannot be found
                }
            }

            // See JLS section 10.7: arrays have a "public final int length" field
            if (c.isArray()) {
                rmap.put("length", new FieldReflective("length", JavaPrimitiveType.getInt(),
                        Modifier.PUBLIC | Modifier.FINAL, this));
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

                JavaType rtype;
                try {
                    rtype = JavaUtils.getJavaUtils().getReturnType(method);
                }
                catch (ClassNotFoundException cnfe) {
                    // Type parameter missing
                    rtype = JavaUtils.getJavaUtils().getRawReturnType(method);
                }
                List<GenTypeDeclTpar> tpars = JavaUtils.getJavaUtils().getTypeParams(method);

                // We need to create a map from each type parameter name to its type
                // as a GenTypeDeclTpar
                Map<String,GenTypeDeclTpar> tparMap = new HashMap<String,GenTypeDeclTpar>();
                storeTparMappings(tpars, tparMap);
                if (! Modifier.isStatic(method.getModifiers())) {
                    getTparMapping(method.getDeclaringClass(), tparMap);
                }

                try {
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
                catch (ClassNotFoundException cnfe) {
                    continue;
                }
            }

            // See JLS section 10.7: arrays have a "public Object clone()" method
            if (c.isArray()) {
                rmap.put("clone", Collections.singleton(new MethodReflective("clone", 
                        new GenTypeClass(new JavaReflective(Object.class)),
                        new ArrayList<GenTypeDeclTpar>(), new ArrayList<JavaType>(), this, false,
                        Modifier.PUBLIC)));
            }

            return rmap;
        }
        catch (LinkageError le) {
            // getDeclaredMethods() can cause a LinkageError
            return Collections.emptyMap();
        }
    }

    @Override
    public List<ConstructorReflective> getDeclaredConstructors()
    {
        List<ConstructorReflective> r = new ArrayList<>();
        
        try {
            for (Constructor<?> con : c.getDeclaredConstructors())
            {
                List<GenTypeDeclTpar> tpars = JavaUtils.getJavaUtils().getTypeParams(con);
    
                // We need to create a map from each type parameter name to its type
                // as a GenTypeDeclTpar
                Map<String, GenTypeDeclTpar> tparMap = new HashMap<String, GenTypeDeclTpar>();
                storeTparMappings(tpars, tparMap);
                
                try
                {
                    JavaType [] paramTypes = JavaUtils.getJavaUtils().getParamGenTypes(con);
                    List<JavaType> paramTypesList = new ArrayList<JavaType>(paramTypes.length);
                    for (JavaType paramType : paramTypes)
                    {
                        paramTypesList.add(paramType.mapTparsToTypes(tparMap).getUpperBound());
                    }
    
                    r.add(new ConstructorReflective(tpars, paramTypesList, this,
                            JavaUtils.getJavaUtils().isVarArgs(con), con.getModifiers()));
                }
                catch (ClassNotFoundException exc)
                {
                    // Ignore this one
                }
            }
        }
        catch (LinkageError | SecurityException exc) {
            // getDelaredConstructors can throw LinkageErrors if classes are missing.
        }
        
        return r;
    }

    /**
     * Store, into the specified map, a mapping from the enclosing method/class/constructor
     * type parameter names to the corresponding type parameters. Existing entries in the
     * map are not overwritten.
     * 
     * @param c        The type, whose enclosing entities type parameters are required
     * @param tparMap  The map, into which the mappings from name to type parameter are to be stored
     */
    @OnThread(Tag.FXPlatform)
    private void getTparMapping(Class<?> c, Map<String,GenTypeDeclTpar> tparMap)
    {
        JavaUtils ju = JavaUtils.getJavaUtils();
        List<GenTypeDeclTpar> tpars = ju.getTypeParams(c);
        storeTparMappings(tpars, tparMap);
        
        Method m = c.getEnclosingMethod();
        Constructor<?> cc = c.getEnclosingConstructor();
        c = c.getEnclosingClass();
        
        // Simple experimentation agrees with the documentation: the enclosing method/constructor
        // cannot be non-null if the enclosing class is null (because the method/constructor must
        // be enclosed by the enclosing class).
        
        while (c != null) {
            if (m != null) {
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
            
            if (c != null) {
                tpars = ju.getTypeParams(c);
                storeTparMappings(tpars, tparMap);
                c = c.getEnclosingClass();
                if (c != null) {
                    m = c.getEnclosingMethod();
                    cc = c.getEnclosingConstructor();
                }
            }
        }
    }
    
    /**
     * Store a set of mappings from type parameter names to the type parameter (GenTypeDeclTpar).
     * Existing mappings are not overwritten.
     * 
     * @param tpars  The set of type parameters to create mappings for
     * @param map    The map of name to type parameter
     */
    private void storeTparMappings(List<GenTypeDeclTpar> tpars, Map<String, ? super GenTypeDeclTpar> map)
    {
        for (GenTypeDeclTpar tpar : tpars) {
            if (! map.containsKey(tpar.getTparName())) {
                map.put(tpar.getTparName(), tpar);
            }
        }
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
    
    @Override
    public Reflective getInnerClass(String name)
    {
        try {
            Class<?> [] declared = c.getDeclaredClasses();
            for (Class<?> inner : declared) {
                String innerName = inner.getName();
                int lastDollar = innerName.lastIndexOf('$');
                if (lastDollar != -1) {
                    String baseName = innerName.substring(lastDollar + 1);
                    if (baseName.equals(name)) {
                        return new JavaReflective(inner);
                    }
                }
            }
        }
        catch (LinkageError le) {}
        return null;
    }

    @Override
    public String getModuleName()
    {
        return c.getModule() != null ? c.getModule().getName() : null;
    }
}
