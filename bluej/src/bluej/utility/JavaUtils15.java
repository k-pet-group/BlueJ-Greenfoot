package bluej.utility;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

import bluej.debugger.gentype.*;

/*
 * Java 1.5 version of JavaUtils.
 * 
 * @author Davin McCall
 * @version $Id: JavaUtils15.java 2623 2004-06-18 07:15:53Z davmac $
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
        Type[] params = method.getGenericParameterTypes();
        return makeDescription(name, params, paramnames, method.isVarArgs(), false);
    }
    
    public String getLongDesc(Method method, String [] paramnames)
    {
        String name = getTypeParameters(method);
        name += getTypeName(method.getGenericReturnType()) + " " + method.getName();
        Type [] params = method.getGenericParameterTypes();
        return makeDescription(name, params, paramnames, method.isVarArgs(), true);
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

    /* -------------- Internal methods ---------------- */
    
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
            if( upperBounds.length == 0 || upperBounds[0] == null ) {
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
            if( upperBounds.length != 0 && upperBounds[0] != null )
                Debug.message("getTypeName: upper and lower bound?");
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

    static private String makeDescription(String name, Type[] params, String[] paramnames,
                            boolean isVarArgs, boolean includeTypeNames) {
        StringBuffer sb = new StringBuffer();
        sb.append(name);
        sb.append("(");
        for (int j = 0; j < params.length; j++) {
            if (includeTypeNames) {
                String typeName = getTypeName(params[j]);
                if(isVarArgs && j==(params.length-1)) {
                    typeName = createVarArg(typeName);
                }                
                sb.append(typeName);
                sb.append(" ");
            }
            String paramname = null;
            if (paramnames != null)
                paramname = paramnames[j];
            else if (!includeTypeNames) {
                //Debug.message("substitute type for name");
                String typeName = getTypeName(params[j]);
                if(isVarArgs && j==(params.length-1)) {
                    typeName = createVarArg(typeName);
                }             
                paramname = typeName;
            }
            if (paramname != null) {
                sb.append(paramname);
            }
            if (j < (params.length - 1))
                sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Get the type parameters for a generic method. For example, for the
     * method:   <code>&lt;T&gt; addAll(List&lt;T&gt;</code>
     * this would return "&lt;T&gt;".
     * Returns the empty string for a non-generic method.
     * @param method  The method to retrieve the parameters of
     * @return the parameters
     */
    static private String getTypeParameters(Method method)
    {
        TypeVariable[] tparams = method.getTypeParameters();
        if( tparams.length != 0 ) {
            String name = "<";
            for( int i = 0; i < tparams.length; i++ ) {
                name += getTypeName(tparams[i]);
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
                    if( upperBounds.length != 0 && upperBounds[0] != null )
                        Debug.message("getTypeName: upper and lower bound?");
                    if( lowerBounds.length != 1 )
                        Debug.message("getTypeName: multiple lower bounds for wildcard type?");
                    GenTypeSolid lbound = (GenTypeSolid)genTypeFromType(lowerBounds[0]);
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
