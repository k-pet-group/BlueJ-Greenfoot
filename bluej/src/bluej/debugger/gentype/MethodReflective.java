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

import java.util.List;

/**
 * Represents a method from a reflective.
 * 
 * @author Davin McCall
 */
public class MethodReflective
{
    private JavaType returnType;
    private List<GenTypeDeclTpar> tparTypes;
    private List<JavaType> paramTypes;
    private boolean isVarArgs;
    private boolean isStatic;
    
    /**
     * Construct a MethodReflective object.
     * @param returnType  The return type of the method
     * @param tparTypes   The type parameter definitions (for a generic method); may be null
     * @param paramTypes  The types of the method parameters
     * @param isVarArgs   Whether the method is a "varargs" method. If true, the last paramType is
     *                    the component type, not the array type.
     * @param isStatic    Whether the method is a static method
     */
    public MethodReflective(JavaType returnType, List<GenTypeDeclTpar> tparTypes, List<JavaType> paramTypes, boolean isVarArgs,
            boolean isStatic)
    {
        this.returnType = returnType;
        this.tparTypes = tparTypes;
        this.paramTypes = paramTypes;
        this.isVarArgs = isVarArgs;
        this.isStatic = isStatic;
    }
    
    public boolean isStatic()
    {
        return isStatic;
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
    
    public List<GenTypeDeclTpar> getTparTypes()
    {
        return tparTypes;
    }
    
    public JavaType getReturnType()
    {
        return returnType;
    }
}
