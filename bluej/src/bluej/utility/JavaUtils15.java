/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011,2014,2018,2020  Michael Kolling and John Rosenberg
 
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
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Java 1.5+ version of JavaUtils.
 * 
 * @author Davin McCall
 */
@OnThread(Tag.Any)
public class JavaUtils15 extends JavaUtils
{
    /*
     * Make signatures for methods, constructors
     */
    
    /*
     * Make descriptions of methods
     */
    
    /**
     * Get a short or long method description which maps type parameters to types using
     * the supplied map. 
     */
    @OnThread(Tag.FXPlatform)
    public String getDescription(Method method, String [] paramnames,
            Map<String,? extends GenTypeParameter> tparams, boolean longDesc)
        throws ClassNotFoundException
    {
        // If tparams is null, the parent object is raw.
        if(tparams == null) {
            String name = JavaUtils14.getTypeName(method.getReturnType()) + " " + method.getName();
            Class<?>[] params = method.getParameterTypes();
            String[] paramTypes = JavaUtils14.getParameterTypes(params);
            return makeDescription(name, paramTypes, paramnames, longDesc, method.isVarArgs());
        }
        
        // Don't want to modify the map which was passed in, so make a copy:
        Map<String,GenTypeParameter> newMap = new HashMap<String,GenTypeParameter>(tparams);

        // add any method type parameters into the map, replacing existing
        // map entries.
        List<GenTypeDeclTpar> myParams = getTypeParams(method);
        for(Iterator<GenTypeDeclTpar> i = myParams.iterator(); i.hasNext(); ) {
            GenTypeDeclTpar tpar = i.next();
            newMap.put(tpar.getTparName(), tpar);
        }
        
        // assemble the type parameters, return type, method name, parameters
        String name = getTypeParameters(method);
        JavaType rtype = getReturnType(method);
        name += rtype.mapTparsToTypes(newMap).toString(true) + " " + method.getName();
        JavaType[] paramTypes = getParamGenTypes(method, false);
        String[] paramTypeNames = new String[paramTypes.length];
        for(int i = 0; i < paramTypes.length; i++) {
            paramTypeNames[i] = paramTypes[i].mapTparsToTypes(newMap).toString(true);
        }
        
        return makeDescription(name, paramTypeNames, paramnames, longDesc, method.isVarArgs());
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public String getShortDesc(Method method, String [] paramnames, Map<String,GenTypeParameter> tparams)
        throws ClassNotFoundException
    {
        return getDescription(method, paramnames, tparams, false);
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public String getLongDesc(Method method, String [] paramnames, Map<String,GenTypeParameter> tparams)
        throws ClassNotFoundException
    {
        return getDescription(method, paramnames, tparams, true);
    }
    
    @Override
    public String getShortDesc(Method method, String [] paramnames)
        throws ClassNotFoundException
    {
        try {
            String name = getTypeParameters(method);
            name += getTypeName(method.getGenericReturnType()) + " " + method.getName();

            // Get the names without introducing ellipsis for varargs
            Type[] paramTypes = method.getGenericParameterTypes();       
            String[] paramTypeNames = getParameterTypes(paramTypes, false);

            return makeDescription(name, paramTypeNames, paramnames, false, method.isVarArgs());
        }
        catch (TypeNotPresentException tnpe) {
            throw new ClassNotFoundException(tnpe.typeName(), tnpe.getCause());
        }
    }

    @Override
    public String getLongDesc(Method method, String [] paramnames) throws ClassNotFoundException
    {
        try {
            String name = getTypeParameters(method);
            name += getTypeName(method.getGenericReturnType()) + " " + method.getName();

            // Get the names without introducing ellipsis for varargs
            Type[] paramTypes = method.getGenericParameterTypes();       
            String[] paramTypeNames = getParameterTypes(paramTypes, false);

            // String[] paramTypes = getParameterTypes(method);
            return makeDescription(name, paramTypeNames, paramnames, true, method.isVarArgs());
        }
        catch (TypeNotPresentException tnpe) {
            throw new ClassNotFoundException(tnpe.typeName(), tnpe.getCause());
        }
    }
    
    /*
     * Make descriptions of constructors
     */
    
    /**
     * Make a constructor description (short or long).
     */
    public String getDescription(Constructor<?> constructor, String [] paramnames, boolean longDesc)
        throws ClassNotFoundException
    {
        String name = getTypeParameters(constructor);
        name += constructor.getDeclaringClass().getSimpleName();
        name += typeParamsToString(constructor.getDeclaringClass().getTypeParameters(), false); 

        // Get the names without introducing ellipsis for varargs
        Type[] paramTypes = constructor.getGenericParameterTypes();       
        String[] paramTypeNames = getParameterTypes(paramTypes, false);

        //String[] paramTypes = getParameterTypes(constructor);
        return makeDescription(name, paramTypeNames, paramnames, longDesc, constructor.isVarArgs());
    }
    
    @Override
    public String getShortDesc(Constructor<?> constructor, String [] paramnames)
        throws ClassNotFoundException
    {
        return getDescription(constructor, paramnames, false);
    }

    @Override
    public String getLongDesc(Constructor<?> constructor, String [] paramnames)
        throws ClassNotFoundException
    {
        return getDescription(constructor, paramnames, true);
    }
    
    /*
     * Check various attributes of constructors / methods
     */
    
    @Override
    public boolean isVarArgs(Constructor<?> cons)
    {
        return cons.isVarArgs();
    }
    
    @Override
    public boolean isVarArgs(Method method)
    {
        return method.isVarArgs();
    }

    @Override
    public boolean isSynthetic(Method method)
    {
        return method.isSynthetic();
    }
    
    @Override
    public boolean isEnum(Class<?> cl)
    {
        return cl.isEnum();
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public JavaType getReturnType(Method method) throws ClassNotFoundException
    {
        try {
            Type rt = method.getGenericReturnType();
            return genTypeFromType(rt);
        }
        catch (TypeNotPresentException tnpe) {
            throw new ClassNotFoundException(tnpe.typeName(), tnpe.getCause());
        }
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public JavaType getRawReturnType(Method method)
    {
        Class<?> c = method.getReturnType();
        return JavaUtils.genTypeFromClass(c);
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public JavaType getFieldType(Field field) throws ClassNotFoundException
    {
        try {
            return genTypeFromType(field.getGenericType());
        }
        catch (TypeNotPresentException tnpe) {
            throw new ClassNotFoundException(tnpe.typeName(), tnpe.getCause());
        }
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public JavaType getRawFieldType(Field field)
    {
        Class<?> c = field.getType();
        return JavaUtils.genTypeFromClass(c);
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public List<GenTypeDeclTpar> getTypeParams(Method method)
    {
        return getTypeParams((GenericDeclaration) method);
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public List<GenTypeDeclTpar> getTypeParams(Constructor<?> cons)
    {
        return getTypeParams((GenericDeclaration) cons);
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public List<GenTypeDeclTpar> getTypeParams(Class<?> cl)
    {
        return getTypeParams((GenericDeclaration) cl);
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public GenTypeClass getSuperclass(Class<?> cl) throws ClassNotFoundException
    {
        try {
            Type sc = cl.getGenericSuperclass();
            if( sc == null ) {
                return null;
            }
            return (GenTypeClass)genTypeFromType(sc);
        }
        catch (TypeNotPresentException tnpe) {
            throw new ClassNotFoundException(tnpe.typeName(), tnpe.getCause());
        }
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public GenTypeClass [] getInterfaces(Class<?> cl) throws ClassNotFoundException
    {
        try {
            Type [] classes = cl.getGenericInterfaces();
            GenTypeClass [] gentypes = new GenTypeClass[classes.length];

            for( int i = 0; i < classes.length; i++ ) {
                gentypes[i] = (GenTypeClass)genTypeFromType(classes[i]);
            }

            return gentypes;
        }
        catch (TypeNotPresentException tnpe) {
            throw new ClassNotFoundException(tnpe.typeName(), tnpe.getCause());
        }
    }    
    
    @Override
    public String[] getParameterTypes(Method method) throws ClassNotFoundException
    {
        try {
            Type [] params = method.getGenericParameterTypes();
            boolean isVarArgs = isVarArgs(method);
            return getParameterTypes(params, isVarArgs);
        }
        catch (TypeNotPresentException tnpe) {
            throw new ClassNotFoundException(tnpe.typeName(), tnpe.getCause());
        }
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public JavaType[] getParamGenTypes(Method method, boolean raw) throws ClassNotFoundException
    {
        try {
            Type [] params;
            if (raw) {
                params = method.getParameterTypes();
            }
            else {
                params = method.getGenericParameterTypes();
            }
            JavaType [] gentypes = new JavaType[params.length];
            for(int i = 0; i < params.length; i++) {
                gentypes[i] = genTypeFromType(params[i]);
            }
            return gentypes;
        }
        catch (TypeNotPresentException tnpe) {
            throw new ClassNotFoundException(tnpe.typeName(), tnpe.getCause());
        }
    }

    @Override
    public String[] getParameterTypes(Constructor<?> constructor) throws ClassNotFoundException
    {
        try {
            Type [] params = constructor.getGenericParameterTypes();
            boolean isVarArgs = isVarArgs(constructor);
            return getParameterTypes(params, isVarArgs);
        }
        catch (TypeNotPresentException tnpe) {
            throw new ClassNotFoundException(tnpe.typeName(), tnpe.getCause());
        }

    }

    @Override
    @OnThread(Tag.FXPlatform)
    public JavaType[] getParamGenTypes(Constructor<?> constructor) throws ClassNotFoundException
    {
        try {
            Type [] params = constructor.getGenericParameterTypes();
            JavaType [] gentypes = new JavaType[params.length];
            for(int i = 0; i < params.length; i++) {
                gentypes[i] = genTypeFromType(params[i]);
            }
            return gentypes;
        }
        catch (TypeNotPresentException tnpe) {
            throw new ClassNotFoundException(tnpe.typeName(), tnpe.getCause());
        }
    }

    /* -------------- Internal methods ---------------- */
    
    /**
     * Get the type parameters for any GenericDeclaration implementor. This
     * includes Methods, Constructors and Classes.
     */
    @OnThread(Tag.FXPlatform)
    private List<GenTypeDeclTpar> getTypeParams(GenericDeclaration decl)
    {
        List<GenTypeDeclTpar> rlist = new ArrayList<GenTypeDeclTpar>();
        TypeVariable<?> [] tvars = decl.getTypeParameters();

        Map<String,GenTypeDeclTpar> tvarMap = new HashMap<String,GenTypeDeclTpar>();

        for (TypeVariable<?> tvar : tvars) {
            tvarMap.put(tvar.getName(), new GenTypeDeclTpar(tvar.getName()));
        }

        for( int i = 0; i < tvars.length; i++ ) {
            // find the bounds.
            Type [] bounds = tvars[i].getBounds();
            GenTypeSolid [] upperBounds = new GenTypeSolid[bounds.length];
            for (int j = 0; j < bounds.length; j++) {
                upperBounds[j] = (GenTypeSolid) genTypeFromType(bounds[j], tvarMap);
            }

            // add the type parameter to the list.
            GenTypeDeclTpar tpar = tvarMap.get(tvars[i].getName());
            tpar.setBounds(upperBounds);

            rlist.add(tpar);
        }
        return rlist;
    }
    
    /**
     * Gets nicely formatted strings describing the parameter types.
     */
    private String[] getParameterTypes(Type[] params, boolean isVarArgs) throws ClassNotFoundException
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

    /**
     * Express the given type as a string.
     */
    static private String getTypeName(Type type) throws ClassNotFoundException
    {
        try {
            StringBuffer sb = new StringBuffer();
            Type primtype = type;
            int dimensions = 0;
            while(primtype instanceof GenericArrayType) {
                dimensions++;
                primtype = ((GenericArrayType)primtype).getGenericComponentType();
            }

            if(primtype instanceof Class<?>) {
                sb.append(JavaUtils14.getTypeName((Class<?>)primtype));
            }
            else if(primtype instanceof ParameterizedType) {
                sb.append(getTypeName((ParameterizedType)primtype));
            }
            else if(primtype instanceof TypeVariable<?>) {
                sb.append(((TypeVariable<?>)primtype).getName());
            }
            else if(primtype instanceof WildcardType) {
                sb.append(getTypeName((WildcardType)primtype));
            }
            else {
                Debug.message("getTypeName(): Unknown type: " + primtype.getClass().getName());
            }

            while( dimensions > 0 ) {
                sb.append("[]");
                dimensions--;
            }
            return sb.toString();
        }
        catch (TypeNotPresentException tnpe) {
            throw new ClassNotFoundException(tnpe.typeName(), tnpe.getCause());
        }
    }

    static private String getTypeName(ParameterizedType type)
        throws ClassNotFoundException
    {
        StringBuffer sb = new StringBuffer();
        sb.append(getTypeName(type.getRawType()));
        sb.append('<');
        
        Type [] argTypes = type.getActualTypeArguments();
        for(int i = 0; i < argTypes.length; i++) {
            sb.append(getTypeName(argTypes[i]));
            if( i != argTypes.length - 1 ) {
                sb.append(',');
            }
        }
        
        sb.append('>');
        return sb.toString();
    }

    static private String getTypeName(WildcardType type) throws ClassNotFoundException
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
    static private String createVarArg(String typeName)
    {
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
        throws ClassNotFoundException
    {
        return typeParamsToString(method.getTypeParameters(), true);
    }
    
    static private String getTypeParameters(Constructor<?> cons)
        throws ClassNotFoundException
    {
        return typeParamsToString(cons.getTypeParameters(), true);
    }
    
    /**
     * Convert a TypeVariable array into a string representing a type parameter sequence,
     * surrounded by angle brackets, with an optional trailing space (omitted if there
     * are no type parameters).
     */
    static private String typeParamsToString(TypeVariable<?> [] tparams, boolean extraSpace)
        throws ClassNotFoundException
    {
        if( tparams.length != 0 ) {
            String name = "<";
            for( int i = 0; i < tparams.length; i++ ) {
                TypeVariable<?> type = tparams[i];
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
                               
                if( i != tparams.length - 1 ) {
                    name += ',';
                }
            }
            name += ">";
            if (extraSpace) {
                name += " ";
            }
            return name;
        }
        else {
            return "";
        }
    }

    /**
     * Build a GenType structure from a "Type" object.
     */
    @OnThread(Tag.FXPlatform)
    private static JavaType genTypeFromType(Type t)
    {
        return (JavaType) genTypeFromType(t, new HashMap<String,GenTypeParameter>());
    }
    
    /**
     * Build a GenType structure from a "Type" object, using the given backTrace
     * stack to avoid infinite recursion.
     */
    @OnThread(Tag.FXPlatform)
    private static GenTypeParameter genTypeFromType(Type t, Map<String,? extends GenTypeParameter> tvars)
    {
        if (t instanceof Class<?>) {
            return JavaUtils.genTypeFromClass((Class<?>)t);
        }
        
        if (t instanceof TypeVariable<?>) {
            TypeVariable<?> tv = (TypeVariable<?>) t;
            GenTypeParameter existingTpar = tvars.get(tv.getName());
            if (existingTpar != null) {
                return existingTpar;
            }
            
            return new GenTypeTpar(tv.getName());
        }
        if (t instanceof WildcardType) {
            WildcardType wtype = (WildcardType)t;
            Type[] upperBounds = wtype.getUpperBounds();
            Type[] lowerBounds = wtype.getLowerBounds();
            // The check for lowerBounds[0] == null is necessary. Appears to be
            // a bug in Java 1.5 beta2.
            if (lowerBounds.length == 0 || lowerBounds[0] == null) {
                if (upperBounds.length == 0 || upperBounds[0] == null) {
                    return new GenTypeUnbounded();
                }
                else {
                    GenTypeSolid gtp = (GenTypeSolid)genTypeFromType(upperBounds[0], tvars);
                    if( upperBounds.length != 1 ) {
                        Debug.message("GenTypeFromType: multiple upper bounds for wildcard type?");
                    }
                    return new GenTypeExtends(gtp);
                }
            } else {
                if (upperBounds.length != 0 && upperBounds[0] != null && upperBounds[0] != Object.class) {
                    Debug.message("getTypeName: upper and lower bound?");
                }
                if (lowerBounds.length != 1) {
                    Debug.message("getTypeName: multiple lower bounds for wildcard type?");
                }
                GenTypeParameter lbound = genTypeFromType(lowerBounds[0], tvars);
                return new GenTypeSuper((GenTypeSolid) lbound);
            }
        }
        if( t instanceof ParameterizedType ) {
            ParameterizedType pt = (ParameterizedType)t;
            Class<?> rawtype = (Class<?>)pt.getRawType();
            Type [] argtypes = pt.getActualTypeArguments();
            List<GenTypeParameter> arggentypes = new ArrayList<GenTypeParameter>();
            
            // Convert the Type [] into a List of GenType
            for( int i = 0; i < argtypes.length; i++ )
                arggentypes.add(genTypeFromType(argtypes[i], tvars));
            
            // Check for outer type
            GenTypeClass outer = null;
            if (pt.getOwnerType() != null) {
                outer = (GenTypeClass) genTypeFromType(pt.getOwnerType());
            }
            
            return new GenTypeClass(new JavaReflective(rawtype), arggentypes, outer);
        }
        
        // Assume we have an array
        GenericArrayType gat = (GenericArrayType)t;
        JavaType componentType = (JavaType) genTypeFromType(gat.getGenericComponentType(), tvars);
        
        return componentType.getArray();
    }
}
