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
    private List<JavaType> paramTypes;
    private boolean isVarArgs;
    private boolean isStatic;
    
    public MethodReflective(JavaType returnType, List<JavaType> paramTypes, boolean isVarArgs,
            boolean isStatic)
    {
        this.returnType = returnType;
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
    
    public List<JavaType> getParamTypes()
    {
        return paramTypes;
    }
    
    public JavaType getReturnType()
    {
        return returnType;
    }
}
