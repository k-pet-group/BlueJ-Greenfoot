/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2015,2020  Michael Kolling and John Rosenberg 
 
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
package bluej.views;

import java.lang.reflect.Constructor;
import java.util.List;

import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.utility.JavaUtils;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A representation of a Java constructor in BlueJ
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 */
public final class ConstructorView extends CallableView
{
    @OnThread(Tag.Any)
    private final Constructor<?> cons;

    /**
     * Constructor.
     */
    public ConstructorView(View view, Constructor<?> cons)
    {
        super(view);
        this.cons = cons;
    }

    /**
     * Returns a string describing this Constructor.
     */
    public String toString()
    {
        return cons.toString();
    }

    @OnThread(Tag.Any)
    public int getModifiers()
    {
        return cons.getModifiers();
    }

    /**
     * Returns a boolean indicating whether this method has parameters
     */
    public boolean hasParameters()
    {
        return (cons.getParameterTypes().length > 0);
    }

    public boolean isGeneric()
    {
        return !JavaUtils.getJavaUtils().getTypeParams(cons).isEmpty();
    }
    
    public boolean isConstructor()
    {
        return true;
    }

    @Override
    public boolean isVoid()
    {
        // Constructor can't return void, by definition:
        return false;
    }

    /**
     * Returns a signature string in the format
     *  name(type,type,type)
     */
    public String getSignature()
    {
        return JavaUtils.getSignature(cons);
    }

    /**
     * Get a short String describing this member. A description is similar
     * to the signature, but it has parameter names in it instead of types.
     */
    public String getShortDesc() 
    {
        try {
            return JavaUtils.getJavaUtils().getShortDesc(cons, getParamNames());
        }
        catch (ClassNotFoundException cnfe) {
            return ""; // TODO handle better
        }
    }

    /**
     * Get a long String describing this member. A long description is
     * similar to the short description, but it has type names and parameters
     * included.
     */
    public String getLongDesc() 
    {
        try {
            return JavaUtils.getJavaUtils().getLongDesc(cons, getParamNames());
        }
        catch (ClassNotFoundException cnfe) {
            return ""; // TODO handle better
        }
    }
    
    /**
     * Get an array of Class objects representing constructor's parameters
     * @returns array of Class objects
     */
    public Class<?>[] getParameters()
    {
        return cons.getParameterTypes();
    }
    
    @Override
    public String[] getParamTypeStrings() 
    {
        try {
            return JavaUtils.getJavaUtils().getParameterTypes(cons);
        }
        catch (ClassNotFoundException cnfe) {
            return new String[0]; // TODO handle better
        }
    }
    
    @Override
    public JavaType[] getParamTypes(boolean raw)
    {
        try {
            return JavaUtils.getJavaUtils().getParamGenTypes(cons);
        }
        catch (ClassNotFoundException cnfe) {
            return new JavaType[0]; // TODO handle better
        }
    }
    
    @Override
    public GenTypeDeclTpar[] getTypeParams()
    {
        JavaUtils jutils = JavaUtils.getJavaUtils();
        List<GenTypeDeclTpar> tparams = jutils.getTypeParams(cons);
        return tparams.toArray(new GenTypeDeclTpar[tparams.size()]);
    }

    /**
     * Whether this method has a var arg.
     */
    public boolean isVarArgs()
    {
        return JavaUtils.getJavaUtils().isVarArgs(cons);
    }
}
