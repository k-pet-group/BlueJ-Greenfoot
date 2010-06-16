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
package bluej.views;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.GenTypeParameter;
import bluej.debugger.gentype.JavaType;
import bluej.utility.JavaUtils;

/**
 * A representation of a Java method in BlueJ
 * 
 * @author Michael Cahill
 * @author Michael Kolling
 */
public class MethodView extends CallableView implements Comparable<MethodView>
{
    protected Method method;
    protected View returnType;

    /**
     * Constructor.
     */
    public MethodView(View view, Method method)
    {
        super(view);
        this.method = method;
    }

    public Method getMethod()
    {
        return method;
    }
    
    /**
     * Returns a string describing this Method.
     */
    public String toString()
    {
        return method.toString();
    }

    public int getModifiers()
    {
        return method.getModifiers();
    }

    public boolean hasParameters()
    {
        return (method.getParameterTypes().length > 0);
    }
    
    public boolean isConstructor()
    {
        return false;
    }

    /**
     * Returns a signature string in the format
     * "type name(type,type,type)".
     */
    public String getSignature()
    {
        return JavaUtils.getSignature(method);
    }
    
    /**
     * Get the "call signature", ie. the signature without the return type.
     * This should not be made user visible, it is for internal purposes only.
     * It is useful for locating methods which override a method in a super
     * class, without having to worry about covariant returns and generic
     * methods etc.
     */
    public String getCallSignature()
    {
        StringBuffer name = new StringBuffer();
        name.append(method.getName());
        name.append('(');
        Class<?>[] params = method.getParameterTypes();
        for(int i = 0; i < params.length; i++) {
            name.append(params[i].getName());
            if (i != params.length - 1)
                name.append(',');
        }
        name.append(')');
        return name.toString();
    }
    
    /**
     * Get a short String describing this member. A description is similar
     * to the signature, but it has parameter names in it instead of types.
     */
    public String getShortDesc()
    {
        return JavaUtils.getJavaUtils().getShortDesc(method, getParamNames());
    }

    /**
     * Get a short description string, mapping type parameters from the
     * class to the corresponding instantiation type. Type parameters not
     * contained in the map are mapped to their erasure type; type parameters
     * from a generic method are left unmapped.
     *  
     * @param genericParams  The map of name to type
     * @return  the signature string with type parameters mapped
     */
    public String getShortDesc(Map<String,GenTypeParameter> genericParams)
    {
        return JavaUtils.getJavaUtils().getShortDesc(method, getParamNames(), genericParams);
    }
    
    /**
     * Get a long String describing this member. A long description is
     * similar to the short description, but it has type names and parameters
     * included.
     */
    public String getLongDesc()
    {
        return JavaUtils.getJavaUtils().getLongDesc(method, getParamNames());
    }
    
    /**
     * Get a long String describing this member, with type parameters from the
     * class mapped to the corresponding instantiation type. Type parameters
     * not contained in the map are mapped to their erasure type; type
     * parameters from a generic method are left unmapped.
     * 
     * @param genericParams  The map of String -> GenType
     * @return  the signature string with type parameters mapped
     */
    public String getLongDesc(Map<String,GenTypeParameter> genericParams)
    {
        if (genericParams == null && isStatic()) {
            return JavaUtils.getJavaUtils().getLongDesc(method, getParamNames());
        }
        else {
            return JavaUtils.getJavaUtils().getLongDesc(method, getParamNames(), genericParams);
        }
    }

    /**
     * Get an array of Class objects representing method's parameters
     * @returns array of Class objects
     */
    public Class<?>[] getParameters()
    {
        return method.getParameterTypes();
    }
    
    public JavaType[] getParamTypes(boolean raw)
    {
        JavaUtils jutils = JavaUtils.getJavaUtils();
        JavaType [] ptypes = jutils.getParamGenTypes(method, raw);
        return ptypes;
    }
    
    public GenTypeDeclTpar[] getTypeParams()
    {
        JavaUtils jutils = JavaUtils.getJavaUtils();
        List<GenTypeDeclTpar> tparams = jutils.getTypeParams(method);
        return tparams.toArray(new GenTypeDeclTpar[0]);
    }
    
    public String[] getParamTypeStrings() 
    {
        return JavaUtils.getJavaUtils().getParameterTypes(method);
    }

    /**
     * Returns the name of this method as a String
     */
    public String getName() {
        return method.getName();
    }

    /**
     * @returns a boolean indicating whether this method has no return value
     */
    public boolean isVoid() {
        String resultName = getReturnType().getQualifiedName();
        return "void".equals(resultName);
    }

    /**
     * @returns if this method is the main method (a static void returning
     * function called main with a string array as an argument)
     */
    public boolean isMain() {
        if (!isVoid())
            return false;
        if ("main".equals(getName())) {
            Class<?>[] c = getParameters();
            if (c.length != 1)
                return false;
            if (c[0].isArray() && String.class.equals(c[0].getComponentType())) {
                if (Modifier.isStatic(getModifiers()) && Modifier.isPublic(getModifiers()))
                    return true;
            }
        }
        return false;
    }
    
    /**
     * Whether this method has a var arg.
     */
    public boolean isVarArgs()
    {
        return JavaUtils.getJavaUtils().isVarArgs(method);
    }
    
    /**
     * Test whether the method is generic.
     */
    public boolean isGeneric()
    {
        return !JavaUtils.getJavaUtils().getTypeParams(method).isEmpty();
    }

    /**
     * Returns a Class object that represents the formal return type
     * of the method represented by this Method object.
     */
    public View getReturnType()
    {
        if (returnType == null)
            returnType = View.getView(method.getReturnType());
        return returnType;
    }
    
    public JavaType getGenericReturnType() {
        return JavaUtils.getJavaUtils().getReturnType(method);
    }
    
    public void print(FormattedPrintWriter out, Map<String,GenTypeParameter> typeParams, int indents)
    {
        Comment comment = getComment();
        if(comment != null)
            comment.print(out, indents);

        out.setItalic(false);
        out.setBold(true);
        for(int i=0; i<indents; i++)
            out.indentLine();
        out.println(getLongDesc(typeParams));
    }

    // ==== Comparable interface ====
    
    /**
     * Compare operation to provide alphabetical sorting by method name.
     */
    public int compareTo(MethodView other)
    {
        return method.getName().compareTo(other.method.getName());
    }
}
