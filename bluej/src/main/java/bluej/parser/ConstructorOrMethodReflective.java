/*
 This file is part of the BlueJ program.
 Copyright (C) 2015 Michael Kölling and John Rosenberg

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
package bluej.parser;

import java.util.Collections;
import java.util.List;

import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;

/**
 * Created by neil on 08/09/15.
 */
public class ConstructorOrMethodReflective
{
    protected Reflective declaringType;
    protected int modifiers;
    protected List<GenTypeDeclTpar> tparTypes;
    protected List<String> paramNames;
    protected List<JavaType> paramTypes;
    protected boolean isVarArgs;
    protected String javaDoc;


    public Reflective getDeclaringType()
    {
        return declaringType;
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
}
