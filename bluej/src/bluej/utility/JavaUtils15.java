package bluej.utility;

import java.lang.reflect.*;

/*
 * Java 1.5 version of JavaUtils.
 * 
 * @author Davin McCall
 * @version $Id: JavaUtils15.java 2582 2004-06-10 04:32:41Z davmac $
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

    /* Internal methods */
    
    private static String getTypeName(Type type)
    {
        StringBuffer sb = new StringBuffer();
        Type primtype = type;
        int dimensions = 0;
        while(primtype instanceof GenericArrayType) {
            dimensions++;
            primtype = ((GenericArrayType)primtype).getGenericComponentType();
        }
        
        if( type == null )
            Debug.message("type == null??");
            
        if(type instanceof Class)
            sb.append(JavaUtils14.getTypeName((Class)type));
        else if(type instanceof ParameterizedType)
            sb.append(getTypeName((ParameterizedType)type));
        else if(type instanceof TypeVariable)
            sb.append(((TypeVariable)type).getName());
        else if(type instanceof WildcardType)
            sb.append(getTypeName((WildcardType)type));
        else
            Debug.message("getTypeName: Unknown type: " + type.getClass().getName());

        while( dimensions > 0 ) {
            sb.append("[]");
            dimensions--;
        }
        return sb.toString();
    }

    private static String getTypeName(ParameterizedType type)
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

    private static String getTypeName(WildcardType type)
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
    private String makeSignature(String name, Type[] params, boolean isVarArgs)
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

    private String createVarArg(String typeName) {
        String lastArrayStripped = typeName.substring(0,typeName.length()-2);
        return lastArrayStripped + " ...";        
    }

    protected String makeDescription(String name, Type[] params, String[] paramnames,
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
    private String getTypeParameters(Method method)
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
}
