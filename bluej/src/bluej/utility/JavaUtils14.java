package bluej.utility;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;


/*
 * Java 1.4 version of JavaUtils
 * 
 * @author Davin McCall
 * @version $Id: JavaUtils14.java 2568 2004-06-02 05:38:07Z davmac $
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
    
    public String getSignature(Constructor cons)
    {
        String name = JavaNames.getBase(cons.getName());
        Class[] params = cons.getParameterTypes();
        return makeSignature(name, params);
    }


    /* Internal methods */
    
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

}
