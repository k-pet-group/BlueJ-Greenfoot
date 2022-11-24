/*
 This file is part of the BlueJ program. 
 Copyright (C) 2010,2015  Michael Kolling and John Rosenberg
 
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

import bluej.parser.ConstructorOrMethodReflective;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Represents a method from a reflective.
 * 
 * @author Davin McCall
 */
public class MethodReflective extends ConstructorOrMethodReflective
{
    @OnThread(Tag.Any)
    private final String name;
    private final JavaType returnType;

    
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
     * Get the method name.
     */
    @OnThread(Tag.Any)
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

    public boolean isAbstract()
    {
        return false; // not yet implemented
    }

    
    public JavaType getReturnType()
    {
        return returnType;
    }

}
