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

import java.lang.reflect.*;
import java.util.*;

import bluej.debugger.gentype.*;

/**
 * Java 1.5 version of JavaUtils.
 * 
 * @author Davin McCall
 * @version $Id: JavaUtils15.java 6164 2009-02-19 18:11:32Z polle $
 */
public class JavaUtils15 extends JavaUtils {

    /*
     * Make signatures for methods, constructors
     */
    
    public String getSignature(Method method)
    {
        String name = getTypeParameters(method);
        name += getTypeName(method.getGenericReturnType()) + " " + method.getName();
        Type[] params = method.getGenericParameterTypes();
        return makeSignature(name, params, method.isVarArgs());
    }
    
    public String getSignature(Constructor cons)
    {
        String name = getTypeParameters(cons);
        name += JavaNames.getBase(cons.getName());
        Type[] params = cons.getGenericParameterTypes();
        
        return makeSignature(name, params, cons.isVarArgs());
    }
    
    /**
     * Build the signature string. Format: name(type,type,type)
     */
    static private String makeSignature(String name, Type[] params, boolean isVarArgs)
    {
        String [] typeStrings = typeArrayToStrings(params);
        return makeDescription(name, typeStrings, null, true, isVarArgs);
    }
    
    /**
     * Convert an array of types to an array of strings representing those types.
     */
    private static String [] typeArrayToStrings(Type [] types)
    {
        String [] rval = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            rval[i] = getTypeName(types[i]);
        }
                
        return rval;
    }
    
    /*
     * Make descriptions of methods
     */
    
    /**
     * Get a short or long method description which maps type parameters to types using
     * the supplied map. 
     */
    public String getDescription(Method method, String [] paramnames, Map tparams, boolean longDesc)
    {
        // If tparams is null, the parent object is raw.
        if(tparams == null) {
            String name = JavaUtils14.getTypeName(method.getReturnType()) + " " + method.getName();
            Class[] params = method.getParameterTypes();
            String[] paramTypes = JavaUtils14.getParameterTypes(params);
            return makeDescription(name, paramTypes, paramnames, longDesc, false);
        }
        
        // Don't want to modify the map which was passed in, so make a copy:
        Map newMap = new HashMap(tparams);

        // add any method type parameters into the map, replacing existing
        // map entries.
        List myParams = getTypeParams(method);
        for(Iterator i = myParams.iterator(); i.hasNext(); ) {
            GenTypeDeclTpar tpar = (GenTypeDeclTpar)i.next();
            newMap.put(tpar.getTparName(), tpar);
        }
        
        // assemble the type parameters, return type, method name, parameters
        String name = getTypeParameters(method);
        JavaType rtype = getReturnType(method);
        name += rtype.mapTparsToTypes(newMap).toString(true) + " " + method.getName();
        JavaType[] paramTypes = getParamGenTypes(method, false);
        String[] paramTypeNames = new String[paramTypes.length];
        for(int i = 0; i < paramTypes.length; i++)
            paramTypeNames[i] = paramTypes[i].mapTparsToTypes(newMap).toString(true);
        
        return makeDescription(name, paramTypeNames, paramnames, longDesc, method.isVarArgs());
    }

    public String getShortDesc(Method method, String [] paramnames, Map tparams)
    {
        return getDescription(method, paramnames, tparams, false);
    }

    public String getLongDesc(Method method, String [] paramnames, Map tparams)
    {
        return getDescription(method, paramnames, tparams, true);
    }
    
    public String getShortDesc(Method method, String [] paramnames)
    {
        String name = getTypeParameters(method);
        name += getTypeName(method.getGenericReturnType()) + " " + method.getName();

        // Get the names without introducing ellipsis for varargs
        Type[] paramTypes = method.getGenericParameterTypes();       
        String[] paramTypeNames = getParameterTypes(paramTypes, false);
        
        return makeDescription(name, paramTypeNames, paramnames, false, method.isVarArgs());
    }

    public String getLongDesc(Method method, String [] paramnames)
    {
        String name = getTypeParameters(method);
        name += getTypeName(method.getGenericReturnType()) + " " + method.getName();
        
        // Get the names without introducing ellipsis for varargs
        Type[] paramTypes = method.getGenericParameterTypes();       
        String[] paramTypeNames = getParameterTypes(paramTypes, false);

        // String[] paramTypes = getParameterTypes(method);
        return makeDescription(name, paramTypeNames, paramnames, true, method.isVarArgs());
    }
    
    /*
     * Make descriptions of constructors
     */
    
    /**
     * Make a constructor description (short or long).
     */
    public String getDescription(Constructor constructor, String [] paramnames, boolean longDesc)
    {
        String name = getTypeParameters(constructor);
        name += constructor.getName();        
        name += typeParamsToString(constructor.getDeclaringClass().getTypeParameters(), false); 

        // Get the names without introducing ellipsis for varargs
        Type[] paramTypes = constructor.getGenericParameterTypes();       
        String[] paramTypeNames = getParameterTypes(paramTypes, false);

        //String[] paramTypes = getParameterTypes(constructor);
        return makeDescription(name, paramTypeNames, paramnames, longDesc, constructor.isVarArgs());
    }
    
    public String getShortDesc(Constructor constructor, String [] paramnames)
    {
        return getDescription(constructor, paramnames, false);
    }

    public String getLongDesc(Constructor constructor, String [] paramnames)
    {
        return getDescription(constructor, paramnames, true);
    }
    
    /*
     * Check various attributes of constructors / methods
     */
    
    public boolean isVarArgs(Constructor cons)
    {
        return cons.isVarArgs();
    }
    
    public boolean isVarArgs(Method method)
    {
        return method.isVarArgs();
    }

    public boolean isSynthetic(Method method)
    {
        return method.isSynthetic();
    }
    
    public boolean isEnum(Class cl) {
        return cl.isEnum();
    }
    
    public JavaType getReturnType(Method method)
    {
        Type rt = method.getGenericReturnType();
        return genTypeFromType(rt);
    }
    
    public JavaType getRawReturnType(Method method)
    {
        Class c = method.getReturnType();
        return JavaUtils14.genTypeFromClass14(c);
    }
    
    public JavaType getFieldType(Field field)
    {
        return genTypeFromType(field.getGenericType());
    }
    
    public JavaType getRawFieldType(Field field)
    {
        Class c = field.getType();
        return JavaUtils14.genTypeFromClass14(c);
    }
    
    public List getTypeParams(Method method)
    {
        return getTypeParams((GenericDeclaration) method);
    }
    
    public List getTypeParams(Constructor cons)
    {
        return getTypeParams((GenericDeclaration) cons);
    }

    public List getTypeParams(Class cl)
    {
        return getTypeParams((GenericDeclaration) cl);
    }
    
    public GenTypeClass getSuperclass(Class cl)
    {
        Type sc = cl.getGenericSuperclass();
        if( sc == null )
            return null;
        return (GenTypeClass)genTypeFromType(sc);
    }
    
    public GenTypeClass [] getInterfaces(Class cl)
    {
        Type [] classes = cl.getGenericInterfaces();
        GenTypeClass [] gentypes = new GenTypeClass[classes.length];
        
        for( int i = 0; i < classes.length; i++ )
            gentypes[i] = (GenTypeClass)genTypeFromType(classes[i]);
        
        return gentypes;
    }    
    
    public String[] getParameterTypes(Method method) 
    {
        Type [] params = method.getGenericParameterTypes();
        boolean isVarArgs = isVarArgs(method);
        return getParameterTypes(params, isVarArgs);
    }
    
    public JavaType[] getParamGenTypes(Method method, boolean raw)
    {
        Type [] params;
        if (raw)
            params = method.getParameterTypes();
        else
            params = method.getGenericParameterTypes();
        JavaType [] gentypes = new JavaType[params.length];
        for(int i = 0; i < params.length; i++) {
            gentypes[i] = genTypeFromType(params[i]);
        }
        return gentypes;
    }

    public String[] getParameterTypes(Constructor constructor) 
    {
        Type [] params = constructor.getGenericParameterTypes();
        boolean isVarArgs = isVarArgs(constructor);
        return getParameterTypes(params, isVarArgs);
    }

    public JavaType[] getParamGenTypes(Constructor constructor)
    {
        Type [] params = constructor.getGenericParameterTypes();
        JavaType [] gentypes = new JavaType[params.length];
        for(int i = 0; i < params.length; i++) {
            gentypes[i] = genTypeFromType(params[i]);
        }
        return gentypes;
    }
        
    /**
     * Build a GenType structure from a "Type" object.
     */
    public JavaType genTypeFromClass(Class t)
    {
        return genTypeFromType(t);
    }
    
    /* -------------- Internal methods ---------------- */
    
    /**
     * Get the type parameters for any GenericDeclaration implementor. This
     * includes Methods, Constructors and Classes.
     */
    private List getTypeParams(GenericDeclaration decl)
    {
        List rlist = new ArrayList();
        TypeVariable [] tvars = decl.getTypeParameters();
        for( int i = 0; i < tvars.length; i++ ) {
            // find the bounds.
            Type [] bounds = tvars[i].getBounds();
            GenTypeSolid [] upperBounds = new GenTypeSolid[bounds.length];
            for (int j = 0; j < bounds.length; j++)
                upperBounds[j] = (GenTypeSolid) genTypeFromType(bounds[j]);
            
            // add the type parameter to the list.
            rlist.add(new GenTypeDeclTpar(tvars[i].getName(), upperBounds));
        }
        return rlist;
    }
    
    /**
     * Gets nicely formatted strings describing the parameter types.
     */
    private String[] getParameterTypes(Type[] params, boolean isVarArgs)
    {
        String[] parameterTypes = new String[params.length];
        for (int j = 0; j < params.length; j++) {
            String typeName = getTypeName(params[j]);
            if (isVarArgs && j == (params.length - 1)) {
                typeName = createVarArg(typeName);
            }
            parameterTypes[j] = typeName;
        }
        return parameterTypes;
    }

    static private String getTypeName(Type type)
    {
        StringBuffer sb = new StringBuffer();
        Type primtype = type;
        int dimensions = 0;
        while(primtype instanceof GenericArrayType) {
            dimensions++;
            primtype = ((GenericArrayType)primtype).getGenericComponentType();
        }
        
        if( primtype == null )
            Debug.message("type == null??");
            
        if(primtype instanceof Class)
            sb.append(JavaUtils14.getTypeName((Class)primtype));
        else if(primtype instanceof ParameterizedType)
            sb.append(getTypeName((ParameterizedType)primtype));
        else if(primtype instanceof TypeVariable)
            sb.append(((TypeVariable)primtype).getName());
        else if(primtype instanceof WildcardType)
            sb.append(getTypeName((WildcardType)primtype));
        else
            Debug.message("getTypeName: Unknown type: " + primtype.getClass().getName());
        
        while( dimensions > 0 ) {
            sb.append("[]");
            dimensions--;
        }
        return sb.toString();
    }

    static private String getTypeName(ParameterizedType type)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(getTypeName(type.getRawType()));
        sb.append('<');
        
        Type [] argTypes = type.getActualTypeArguments();
        for(int i = 0; i < argTypes.length; i++) {
            sb.append(getTypeName(argTypes[i]));
            if( i != argTypes.length - 1 )
                sb.append(',');
        }
        
        sb.append('>');
        return sb.toString();
    }

    static private String getTypeName(WildcardType type)
    {
        StringBuffer sb = new StringBuffer();
        Type[] upperBounds = type.getUpperBounds();
        Type[] lowerBounds = type.getLowerBounds();
        // The check for lowerBounds[0] == null is necessary. Appears to be
        // a bug in Java 1.5 beta2.
        if( lowerBounds.length == 0 || lowerBounds[0] == null ) {
            
            // An unbounded wildcard by reflection appears as
            // "? extends java.lang.Object". We check for that case and
            // reduce it back to unbounded. This is not necessarily correct
            // seeing as it could have been declared with "extends", but that
            // would be superfluous and will hopefully prove to be uncommon.
            if( upperBounds.length == 0 || upperBounds[0] == null || upperBounds[0].equals(Object.class)) {
                sb.append("?"); // unbounded wildcard
            }
            else {
                sb.append("? extends ");
                sb.append(getTypeName(upperBounds[0]));
                if( upperBounds.length != 1 )
                    Debug.message("getTypeName: multiple upper bounds for wildcard type?");
            }
        } else {
            sb.append("? super ");
            if( lowerBounds[0] == null ) {
                Debug.message("lower bound[0] is null??");
                sb.append("[null type]");
            }
            else
                sb.append(getTypeName(lowerBounds[0]));
            if( upperBounds.length != 0 && upperBounds[0] != null && upperBounds[0] != Object.class) {
                Debug.message("getTypeName: upper and lower bound?");
                Debug.message("upper bound is: " + upperBounds[0]);
            }
            if( lowerBounds.length != 1 )
                Debug.message("getTypeName: multiple lower bounds for wildcard type?");
        }
        return sb.toString();
    }

    /**
     * Convert a type name into its vararg form. For instance,
     * "int []" becomes "int ...".
     */
    static private String createVarArg(String typeName) {
        String lastArrayStripped = typeName.substring(0,typeName.length()-2);
        return lastArrayStripped + " ...";        
    }

    /**
     * Get the type parameters for a generic method. For example, for the
     * method:   <code>&lt;T&gt; addAll(List&lt;T&gt;)</code>
     * this would return "&lt;T&gt; " (including the trailing space).
     * Returns the empty string for a non-generic method.
     * 
     * @param method  The method to retrieve the parameters of
     * @return the parameters (or an empty string)
     */
    static private String getTypeParameters(Method method)
    {
        return typeParamsToString(method.getTypeParameters(), true);
    }
    
    static private String getTypeParameters(Constructor cons)
    {
        return typeParamsToString(cons.getTypeParameters(), true);
    }
    
    /**
     * Convert a TypeVariable array into a string representing a type parameter sequence,
     * surrounded by angle brackets, with an optional trailing space (omitted if there
     * are no type parameters).
     */
    static private String typeParamsToString(TypeVariable [] tparams, boolean extraSpace)
    {
        if( tparams.length != 0 ) {
            String name = "<";
            for( int i = 0; i < tparams.length; i++ ) {
                TypeVariable type = tparams[i];
                name += type.getName();        
                Type[] upperBounds = type.getBounds();

                // An unbounded type by reflection appears as
                // " extends java.lang.Object". We check for that case and
                // reduce it back to unbounded. This is not necessarily correct
                // seeing as it could have been declared with "extends", but that
                // would be superfluous and will hopefully prove to be uncommon.
                if (upperBounds.length == 0 || upperBounds[0] == null
                        || upperBounds[0].equals(Object.class)) {
                    //add nothing
                } else {
                    name += " extends " + getTypeName(upperBounds[0]);
                    for (int j = 1; j < upperBounds.length; j++) {
                        name += " & " + getTypeName(upperBounds[j]);
                    }
                }
                
                               
                if( i != tparams.length - 1 )
                    name += ',';
            }
            name += ">";
            if (extraSpace)
                name += " ";
            return name;
        }
        else
            return "";
    }

    /**
     * Build a GenType structure from a "Type" object.
     */
    private static JavaType genTypeFromType(Type t)
    {
        return genTypeFromType(t, new LinkedList());
    }
    
    /**
     * Build a GenType structure from a "Type" object, using the given backTrace
     * stack to avoid infinite recursion.
     */
    private static JavaType genTypeFromType(Type t, List backTrace)
    {
        if( t instanceof Class )
            return JavaUtils14.genTypeFromClass14((Class)t);
        if (t instanceof TypeVariable) {
            TypeVariable tv = (TypeVariable) t;
            if (backTrace.contains(t))
                return new GenTypeUnbounded();
            
            // get the bounds, convert to GenType
            Type[] bounds = tv.getBounds();
            GenTypeSolid[] gtBounds = new GenTypeSolid[bounds.length];
            backTrace.add(t);
            for (int i = 0; i < bounds.length; i++) {
                gtBounds[i] = (GenTypeSolid) genTypeFromType(bounds[i], backTrace);
            }

            return new GenTypeDeclTpar(tv.getName(), gtBounds);
        }
        if( t instanceof WildcardType ) {
            if (backTrace.contains(t))
                return new GenTypeUnbounded();
            
            WildcardType wtype = (WildcardType)t;
            Type[] upperBounds = wtype.getUpperBounds();
            Type[] lowerBounds = wtype.getLowerBounds();
            backTrace.add(t);
            // The check for lowerBounds[0] == null is necessary. Appears to be
            // a bug in Java 1.5 beta2.
            if( lowerBounds.length == 0 || lowerBounds[0] == null ) {
                if( upperBounds.length == 0 || upperBounds[0] == null ) {
                    return new GenTypeUnbounded();
                }
                else {
                    GenTypeSolid gtp = (GenTypeSolid)genTypeFromType(upperBounds[0], backTrace);
                    if( upperBounds.length != 1 )
                        Debug.message("GenTypeFromType: multiple upper bounds for wildcard type?");
                    return new GenTypeExtends(gtp);
                }
            } else {
                if( lowerBounds[0] == null ) {
                    Debug.message("lower bound[0] is null??");
                    return new GenTypeSuper(null);
                }
                else {
                    if (upperBounds.length != 0 && upperBounds[0] != null && upperBounds[0] != Object.class)
                        Debug.message("getTypeName: upper and lower bound?");
                    if (lowerBounds.length != 1)
                        Debug.message("getTypeName: multiple lower bounds for wildcard type?");
                    GenTypeSolid lbound = (GenTypeSolid) genTypeFromType(lowerBounds[0], backTrace);
                    return new GenTypeSuper(lbound);
                }
            }
        }
        if( t instanceof ParameterizedType ) {
            ParameterizedType pt = (ParameterizedType)t;
            Class rawtype = (Class)pt.getRawType();
            Type [] argtypes = pt.getActualTypeArguments();
            List arggentypes = new ArrayList();
            
            // Convert the Type [] into a List of GenType
            for( int i = 0; i < argtypes.length; i++ )
                arggentypes.add(genTypeFromType(argtypes[i], backTrace));
            
            // Check for outer type
            GenTypeClass outer = null;
            if (pt.getOwnerType() != null)
                outer = (GenTypeClass) genTypeFromType(pt.getOwnerType());
            
            return new GenTypeClass(new JavaReflective(rawtype), arggentypes, outer);
        }
        
        // Assume we have an array
        GenericArrayType gat = (GenericArrayType)t;
        JavaType componentType = genTypeFromType(gat.getGenericComponentType(), backTrace);
        
        Reflective reflective = new JavaReflective(getRclass(gat));
        return new GenTypeArray(componentType, reflective);
    }
    
    /**
     * Get the raw name of some type, such as would be returned by
     * Class.getName()
     */
    static private Class getRclass(Type t)
    {
        int arrnum = 0;
        while (! (t instanceof Class)) {
            if (t instanceof ParameterizedType)
                t = ((ParameterizedType) t).getRawType();
        
            if (t instanceof TypeVariable)
                t = ((TypeVariable) t).getBounds()[0];
            
            if (t instanceof WildcardType)
                t = ((WildcardType) t).getUpperBounds()[0];
            
            if (t instanceof GenericArrayType) {
                arrnum++;
                t = ((GenericArrayType) t).getGenericComponentType();
            }
        }
        
        String rName;
        Class rClass = (Class) t;
        ClassLoader classLoader = rClass.getClassLoader();
        
        if (arrnum == 0) {
            rName = rClass.getName();
        }
        else {
            // arrnum > 0 !
            rName = genTypeFromType(t).arrayComponentName();
            while (arrnum > 0) {
                rName = "[" + rName;
                arrnum--;
            }
        }
            
        try {
            if (classLoader == null)
                classLoader = ClassLoader.getSystemClassLoader();
            return classLoader.loadClass(rName);
        }
        catch (ClassNotFoundException cnfe) { return null; }
    }
}
