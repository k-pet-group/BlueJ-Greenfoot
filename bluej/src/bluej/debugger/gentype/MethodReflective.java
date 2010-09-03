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
package bluej.debugger.gentype;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;

/**
 * Represents a method from a reflective.
 * 
 * @author Davin McCall
 */
public class MethodReflective
{
    private String name;
    private JavaType returnType;
    private List<GenTypeDeclTpar> tparTypes;
    private List<JavaType> paramTypes;
    private boolean isVarArgs;
    private Reflective declaringType;
    private String javaDoc;
    private List<String> paramNames;
    private int modifiers;
    
    /**
     * Construct a MethodReflective object.
     * @param name        The name of the method
     * @param returnType  The return type of the method
     * @param tparTypes   The type parameter definitions (for a generic method); may be null
     * @param paramTypes  The types of the method parameters
     * @param isVarArgs   Whether the method is a "varargs" method. If true, the last paramType is
     *                    the component type, not the array type.
     * @param isStatic    Whether the method is a static method
     */
    public MethodReflective(String name, JavaType returnType, List<GenTypeDeclTpar> tparTypes,
            List<JavaType> paramTypes, Reflective declaringType, boolean isVarArgs, int modifiers)
    {
        this.name = name;
        this.returnType = returnType;
        this.tparTypes = tparTypes;
        this.paramTypes = paramTypes;
        this.declaringType = declaringType;
        this.isVarArgs = isVarArgs;
        this.modifiers = modifiers;
    }
    
    /**
     * Set the javadoc for this method.
     */
    public void setJavaDoc(String javaDoc)
    {
        this.javaDoc = javaDoc;
    }
    
    /**
     * Get the javadoc for this method. Returns null if not available
     * (if it has not been set).
     */
    public String getJavaDoc()
    {
        return javaDoc;
    }
    
    /**
     * Set the parameter names for this method.
     * @param paramNames  A list of parameter names. The MethodReflective takes ownership
     *                    of the given list (it should not be later modified).
     */
    public void setParamNames(List<String> paramNames)
    {
        this.paramNames = paramNames;
    }
    
    /**
     * Get the parameter names for this method, if known.
     * @return A list of the parameter names in order, or null if the parameter names are
     *         not known.
     */
    public List<String> getParamNames()
    {
        return paramNames;
    }
    
    /**
     * Get the method name.
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * Check whether the method is a static method.
     */
    public boolean isStatic()
    {
        return Modifier.isStatic(modifiers);
    }
    
    /**
     * Get the method modifiers as a bitmask.
     * 
     * @see java.lang.reflect.Modifier
     */
    public int getModifiers()
    {
        return modifiers;
    }
    
    public boolean isVarArgs()
    {
        return isVarArgs;
    }
    
    public boolean isAbstract()
    {
        return false; // not yet implemented
    }
    
    /**
     * Get the method parameter types. For a varargs method, the last parameter type returned is
     * the element type, not the array type.
     */
    public List<JavaType> getParamTypes()
    {
        return paramTypes;
    }
    
    /**
     * Get the method type parameters. If the method has no type parameters,
     * returns an empty list.
     */
    public List<GenTypeDeclTpar> getTparTypes()
    {
        return tparTypes == null ? Collections.<GenTypeDeclTpar>emptyList() : tparTypes;
    }
    
    public JavaType getReturnType()
    {
        return returnType;
    }
    
    public Reflective getDeclaringType()
    {
        return declaringType;
    }
}
