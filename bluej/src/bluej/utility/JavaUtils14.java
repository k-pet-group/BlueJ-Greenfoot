package bluej.utility;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import bluej.debugger.gentype.*;

/**
 * Java 1.4 version of JavaUtils
 * 
 * @author Davin McCall
 * 
 * @version $Id: JavaUtils14.java 3751 2006-01-31 00:11:20Z davmac $
 */
public class JavaUtils14 extends JavaUtils
{

    public String getSignature(Method method)
    {
        String name = getTypeName(method.getReturnType()) + " " + method.getName();
        Class[] params = method.getParameterTypes();
        return makeSignature(name, params);
    }

    public String getShortDesc(Method method, String[] paramnames)
    {
        String name = getTypeName(method.getReturnType()) + " " + method.getName();
        String[] paramTypes = getParameterTypes(method);
        return makeDescription(name, paramTypes, paramnames, false, false);
    }

    public String getShortDesc(Method method, String [] paramnames, Map tparams)
    {
        return getShortDesc(method, paramnames);
    }
    
    public String getLongDesc(Method method, String[] paramnames)
    {
        String name = getTypeName(method.getReturnType()) + " " + method.getName();
        String[] paramTypes = getParameterTypes(method);
        return makeDescription(name, paramTypes, paramnames, true, false);
    }

    public String getLongDesc(Method method, String[] paramnames, Map tparams)
    {
        return getLongDesc(method, paramnames);
    }

    public String getLongDesc(Constructor constructor, String[] paramnames)
    {
        String name = constructor.getName();
        String[] paramTypes = getParameterTypes(constructor);
        return makeDescription(name, paramTypes, paramnames, true, false);
    }

    public String getShortDesc(Constructor constructor, String[] paramnames)
    {
        String name = constructor.getName();
        String[] paramTypes = getParameterTypes(constructor);
        return makeDescription(name, paramTypes, paramnames, false, false);
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
    
    public boolean isSynthetic(Method method)
    {
        return false;
    }

    public boolean isEnum(Class cl)
    {
        return false;
    }

    public JavaType getReturnType(Method method)
    {
        Class retType = method.getReturnType();
        return genTypeFromClass(retType);
    }
    
    public JavaType getRawReturnType(Method method)
    {
        return getReturnType(method);
    }
    
    public JavaType getFieldType(Field field)
    {
        return genTypeFromClass(field.getType());
    }
    
    public JavaType getRawFieldType(Field field)
    {
        return genTypeFromClass(field.getType());
    }

    public List getTypeParams(Method method)
    {
        return Collections.EMPTY_LIST;
    }
    
    public List getTypeParams(Constructor cons)
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
        if (sc == null)
            return null;
        return (GenTypeClass) genTypeFromClass(sc);
    }

    public GenTypeClass[] getInterfaces(Class cl)
    {
        Class[] classes = cl.getInterfaces();
        GenTypeClass[] gentypes = new GenTypeClass[classes.length];

        for (int i = 0; i < classes.length; i++)
            gentypes[i] = (GenTypeClass) genTypeFromClass(classes[i]);

        return gentypes;
    }

    public String[] getParameterTypes(Method method)
    {
        Class[] params = method.getParameterTypes();
        return getParameterTypes(params);
    }

    public JavaType[] getParamGenTypes(Method method, boolean raw)
    {
        Class[] params = method.getParameterTypes();
        JavaType[] gentypes = new JavaType[params.length];
        for (int i = 0; i < params.length; i++) {
            gentypes[i] = genTypeFromClass(params[i]);
        }
        return gentypes;
    }

    public String[] getParameterTypes(Constructor constructor)
    {
        Class[] params = constructor.getParameterTypes();
        return getParameterTypes(params);
    }

    public JavaType[] getParamGenTypes(Constructor constructor)
    {
        Class[] params = constructor.getParameterTypes();
        JavaType[] gentypes = new JavaType[params.length];
        for (int i = 0; i < params.length; i++) {
            gentypes[i] = genTypeFromClass(params[i]);
        }
        return gentypes;
    }

    /* ------------- Internal methods --------------- */

    /**
     * Gets nicely formatted strings describing the parameter types.
     */
    static String[] getParameterTypes(Class[] params)
    {
        String[] parameterTypes = new String[params.length];
        for (int j = 0; j < params.length; j++) {
            String typeName = getTypeName(params[j]);
            parameterTypes[j] = typeName;
        }
        return parameterTypes;
    }

    static public String getTypeName(Class type)
    {
        if (type.isArray()) {
            try {
                Class primtype = type;
                int dimensions = 0;
                while (primtype.isArray()) {
                    dimensions++;
                    primtype = primtype.getComponentType();
                }
                StringBuffer sb = new StringBuffer();
                sb.append(JavaNames.stripPrefix(primtype.getName()));
                for (int i = 0; i < dimensions; i++)
                    sb.append("[]");
                return sb.toString();
            }
            catch (Throwable e) {
                // ignore it
            }
        }
        return JavaNames.stripPrefix(type.getName());
    }

    /**
     * Build the signature string. Format: name(type,type,type)
     */
    private String makeSignature(String name, Class[] params)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(name);
        sb.append("(");
        for (int j = 0; j < params.length; j++) {
            String typeName = getTypeName(params[j]);
            sb.append(typeName);
            if (j < (params.length - 1))
                sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }

    static public JavaType genTypeFromClass(Class c)
    {
        if (c.isPrimitive()) {
            if (c == boolean.class)
                return JavaPrimitiveType.getBoolean();
            if (c == char.class)
                return JavaPrimitiveType.getChar();
            if (c == byte.class)
                return JavaPrimitiveType.getByte();
            if (c == short.class)
                return JavaPrimitiveType.getShort();
            if (c == int.class)
                return JavaPrimitiveType.getInt();
            if (c == long.class)
                return JavaPrimitiveType.getLong();
            if (c == float.class)
                return JavaPrimitiveType.getFloat();
            if (c == double.class)
                return JavaPrimitiveType.getDouble();
            if (c == void.class)
                return JavaPrimitiveType.getVoid();
            Debug.message("getReturnType: Unknown primitive type");
        }
        if (c.isArray()) {
            JavaType componentT = genTypeFromClass(c.getComponentType());
            return new GenTypeArray(componentT, new JavaReflective(c));
        }
        return new GenTypeClass(new JavaReflective(c));
    }

}