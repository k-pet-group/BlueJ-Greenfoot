package bluej.utility;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import bluej.debugger.gentype.*;


/*
 * Java 1.4 version of JavaUtils
 * 
 * @author Davin McCall
 * @version $Id: JavaUtils14.java 2635 2004-06-19 16:27:32Z polle $
 */
public class JavaUtils14 extends JavaUtils {

    public String getSignature(Method method) {
        String name = getTypeName(method.getReturnType()) + " " + method.getName();
        Class[] params = method.getParameterTypes();
        return makeSignature(name, params);
    }
    
    public String getShortDesc(Method method, String [] paramnames)
    {
        String name = getTypeName(method.getReturnType()) + " " + method.getName();
        Class[] params = method.getParameterTypes();
        return makeDescription(name, params, paramnames, false);
    }
    
    public String getLongDesc(Method method, String [] paramnames)
    {
        String name = getTypeName(method.getReturnType()) + " " + method.getName();
        Class [] params = method.getParameterTypes();
        return makeDescription(name, params, paramnames, true);
    }

    public String getLongDesc(Constructor constructor, String [] paramnames)
    {
        String name = constructor.getName();
        Class [] params = constructor.getParameterTypes();
        return makeDescription(name, params, paramnames, true);
    }
    
    public String getShortDesc(Constructor constructor, String [] paramnames)
    {
        String name = constructor.getName();
        Class[] params = constructor.getParameterTypes();
        return makeDescription(name, params, paramnames, false);
    }    
    
    public String getSignature(Constructor cons)
    {
        String name = JavaNames.getBase(cons.getName());
        Class[] params = cons.getParameterTypes();
        return makeSignature(name, params);
    }

    public boolean isVarArgs(Constructor cons)
    {
        return false;
    }
    
    public boolean isVarArgs(Method method)
    {
        return false;
    }

    public boolean isEnum(Class cl) {
        return false;
    }

    public GenType getReturnType(Method method)
    {
        Class retType = method.getReturnType();
        return genTypeFromClass(retType);
    }
    
    public List getTypeParams(Method method)
    {
        return Collections.EMPTY_LIST;
    }
    
    public List getTypeParams(Class cl)
    {
        return Collections.EMPTY_LIST;
    }

    public GenTypeClass getSuperclass(Class cl)
    {
        Class sc = cl.getSuperclass();
        if( sc == null )
            return null;
        return (GenTypeClass)genTypeFromClass(sc);
    }

    public GenTypeClass [] getInterfaces(Class cl)
    {
        Class [] classes = cl.getInterfaces();
        GenTypeClass [] gentypes = new GenTypeClass[classes.length];
        
        for( int i = 0; i < classes.length; i++ )
            gentypes[i] = (GenTypeClass)genTypeFromClass(classes[i]);
        
        return gentypes;
    }

    /* ------------- Internal methods --------------- */

    static public String getTypeName(Class type)
    {
        if(type.isArray())
        {
            try {
                Class primtype = type;
                int dimensions = 0;
                while(primtype.isArray())
                    {
                        dimensions++;
                        primtype = primtype.getComponentType();
                    }
                StringBuffer sb = new StringBuffer();
                sb.append(JavaNames.stripPrefix(primtype.getName()));
                for (int i = 0; i < dimensions; i++)
                    sb.append("[]");
                return sb.toString();
            } catch (Throwable e) {
                // ignore it
            }
        }
        return JavaNames.stripPrefix(type.getName());
    }
    
    /**
     * Build the signature string. Format: name(type,type,type)
     */
    private String makeSignature(String name, Class[] params) {
        StringBuffer sb = new StringBuffer();
        sb.append(name);
        sb.append("(");
        for (int j = 0; j < params.length; j++) {
            String typeName = getTypeName(params[j]);
            sb.append(typeName);
            if (j < (params.length - 1))
                sb.append(",");
        }
        sb.append(")");
        return sb.toString();
    }

    protected String makeDescription(String name, Class[] params,
                                    String[] paramnames, boolean includeTypeNames) {
        StringBuffer sb = new StringBuffer();
        sb.append(name);
        sb.append("(");
        for (int j = 0; j < params.length; j++) {
            if (includeTypeNames) {
                String typeName = getTypeName(params[j]);
                sb.append(typeName);
                sb.append(" ");
            }
            String paramname = null;
            if (paramnames != null)
                paramname = paramnames[j];
            else if (!includeTypeNames) {
                //Debug.message("substitute type for name");
                String typeName = getTypeName(params[j]);
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

    static public GenType genTypeFromClass(Class c)
    {
        if( c.isPrimitive()) {
            if( c == boolean.class )
                return new GenTypeBool();
            if( c == char.class )
                return new GenTypeChar();
            if( c == byte.class )
                return new GenTypeByte();
            if( c == short.class )
                return new GenTypeShort();
            if( c == int.class )
                return new GenTypeInt();
            if( c == long.class )
                return new GenTypeLong();
            if( c == float.class )
                return new GenTypeFloat();
            if( c == void.class )
                return new GenTypeVoid();
            Debug.message("getReturnType: Unknown primitive type");
        }
        if( c.isArray() ) {
            GenType componentT = genTypeFromClass(c.getComponentType());
            return new GenTypeArray(componentT, new JavaReflective(c));
        }
        return new GenTypeClass(new JavaReflective(c));
    }

}
