package bluej.utility;

import java.lang.reflect.*;
import java.util.*;

import bluej.debugger.gentype.*;

/*
 * Java 1.5 version of JavaUtils.
 * 
 * @author Davin McCall
 * @version $Id: JavaUtils15.java 2951 2004-08-27 01:47:46Z davmac $
 */
public class JavaUtils15 extends JavaUtils {

    public String getSignature(Method method) {
        String name = getTypeParameters(method);
        name += getTypeName(method.getGenericReturnType()) + " " + method.getName();
        Type[] params = method.getGenericParameterTypes();
        return makeSignature(name, params, method.isVarArgs());
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
    
    public String getShortDesc(Method method, String [] paramnames, Map tparams)
    {
        // Don't want to modify the map which was passed in, so make a copy
        // of it:
        Map newMap = new HashMap();
        if (tparams != null)
            newMap.putAll(tparams);
        
        // add any method type parameters into the map, replacing existing
        // map entries.
        List myParams = getTypeParams(method);
        for(Iterator i = myParams.iterator(); i.hasNext(); ) {
            GenTypeDeclTpar tpar = (GenTypeDeclTpar)i.next();
            newMap.put(tpar.getTparName(), tpar);
        }
        
        String name = getTypeParameters(method);
        GenType rtype = getReturnType(method);
        name += rtype.mapTparsToTypes(newMap).toString(true) + " " + method.getName();
        GenType[] paramTypes = getParamGenTypes(method);
        String[] paramTypeNames = new String[paramTypes.length];
        for(int i = 0; i < paramTypes.length; i++)
            paramTypeNames[i] = paramTypes[i].mapTparsToTypes(newMap).toString(true);
        
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
    
    public String getShortDesc(Constructor constructor, String [] paramnames)
    {
        String name = constructor.getName();        
        name += getTypeParams(constructor);        

        // Get the names without introducing ellipsis for varargs
        Type[] paramTypes = constructor.getGenericParameterTypes();       
        String[] paramTypeNames = getParameterTypes(paramTypes, false);

        //String[] paramTypes = getParameterTypes(constructor);
        return makeDescription(name, paramTypeNames, paramnames, false, constructor.isVarArgs());
    }

    public String getLongDesc(Constructor constructor, String [] paramnames)
    {
        String name = constructor.getName();        
        name += getTypeParams(constructor); 

        // Get the names without introducing ellipsis for varargs
        Type[] paramTypes = constructor.getGenericParameterTypes();       
        String[] paramTypeNames = getParameterTypes(paramTypes, false);

        // String[] paramTypes = getParameterTypes(constructor);
        return makeDescription(name, paramTypeNames, paramnames, true, constructor.isVarArgs());
    }
    
    private String getTypeParams(Constructor constructor)
    {
        String typeString = "";
        List typeParams = getTypeParams(constructor.getDeclaringClass());
        if(typeParams.size()>0) {
            typeString += "<";
	        for (Iterator iter = typeParams.iterator(); iter.hasNext();) {
	            GenTypeDeclTpar element = (GenTypeDeclTpar) iter.next();
	            typeString += element.toString(true);
	            if(iter.hasNext()) {
	                typeString += ",";
	            }
	        }
	        typeString += ">";
        }
        return typeString;
    }
    
    public String getSignature(Constructor cons)
    {
        String name = JavaNames.getBase(cons.getName());
        Type[] params = cons.getGenericParameterTypes();
        return makeSignature(name, params, cons.isVarArgs());
    }

    public boolean isVarArgs(Constructor cons)
    {
        return cons.isVarArgs();
    }
    
    public boolean isVarArgs(Method method)
    {
        return method.isVarArgs();
    }

    public boolean isBridge(Method method)
    {
        return method.isBridge();
    }
    
    public boolean isEnum(Class cl) {
        return cl.isEnum();
    }
    
    public GenType getReturnType(Method method)
    {
        Type rt = method.getGenericReturnType();
        return genTypeFromType(rt);
    }
    
    public List getTypeParams(Method method)
    {
        List rlist = new ArrayList();
        TypeVariable [] tvars = method.getTypeParameters();
        for( int i = 0; i < tvars.length; i++ ) {
            // TODO multiple bounds.
            GenTypeSolid upperBound = (GenTypeSolid)genTypeFromType(tvars[i].getBounds()[0]);
            rlist.add(new GenTypeDeclTpar(tvars[i].getName(), upperBound));
        }
        return rlist;
    }

    public List getTypeParams(Class cl)
    {
        List rlist = new ArrayList();
        TypeVariable [] tvars = cl.getTypeParameters();
        for( int i = 0; i < tvars.length; i++ ) {
            // TODO multiple bounds.
            GenTypeSolid upperBound = (GenTypeSolid)genTypeFromType(tvars[i].getBounds()[0]);
            rlist.add(new GenTypeDeclTpar(tvars[i].getName(), upperBound));
        }
        return rlist;
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
    
    public GenType[] getParamGenTypes(Method method)
    {
        Type [] params = method.getGenericParameterTypes();
        GenType [] gentypes = new GenType[params.length];
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

    public GenType[] getParamGenTypes(Constructor constructor)
    {
        Type [] params = constructor.getGenericParameterTypes();
        GenType [] gentypes = new GenType[params.length];
        for(int i = 0; i < params.length; i++) {
            gentypes[i] = genTypeFromType(params[i]);
        }
        return gentypes;
    }
    
    /* -------------- Internal methods ---------------- */
    
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
     * Build the signature string. Format: name(type,type,type)
     */
    static private String makeSignature(String name, Type[] params, boolean isVarArgs)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(name);
        sb.append("(");
        for (int j = 0; j < params.length; j++) {
            String typeName = getTypeName(params[j]);
            if(isVarArgs && j==(params.length-1)) {
                typeName = createVarArg(typeName);
            }                
            sb.append(typeName);
            if (j < (params.length - 1))
                sb.append(",");
        }
        sb.append(")");
        return sb.toString();
    }

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
        TypeVariable[] tparams = method.getTypeParameters();
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
                    if (upperBounds.length != 1)
                        Debug.message("getTypeName: multiple upper bounds for typevariable type?");
                }
                
                               
                if( i != tparams.length - 1 )
                    name += ',';
            }
            return name + "> ";
        }
        else
            return "";
    }
    
    /**
     * Build a GenType structure from a "Type" oject.
     */
    static private GenType genTypeFromType(Type t)
    {
        if( t instanceof Class )
            return JavaUtils14.genTypeFromClass((Class)t);
        if( t instanceof TypeVariable )
            return new GenTypeTpar(((TypeVariable)t).getName());
        if( t instanceof WildcardType ) {
            WildcardType wtype = (WildcardType)t;
            Type[] upperBounds = wtype.getUpperBounds();
            Type[] lowerBounds = wtype.getLowerBounds();
            // The check for lowerBounds[0] == null is necessary. Appears to be
            // a bug in Java 1.5 beta2.
            if( lowerBounds.length == 0 || lowerBounds[0] == null ) {
                if( upperBounds.length == 0 || upperBounds[0] == null ) {
                    return new GenTypeUnbounded();
                }
                else {
                    GenTypeSolid gtp = (GenTypeSolid)genTypeFromType(upperBounds[0]);
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
                    GenTypeSolid lbound = (GenTypeSolid) genTypeFromType(lowerBounds[0]);
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
                arggentypes.add(genTypeFromType(argtypes[i]));
            
            return new GenTypeClass(new JavaReflective(rawtype), arggentypes);
        }
        
        // Assume we have an array
        GenericArrayType gat = (GenericArrayType)t;
        GenType componentType = genTypeFromType(gat.getGenericComponentType());
        return new GenTypeArray(componentType, new JavaArrayReflective(gat));
    }
    
}
