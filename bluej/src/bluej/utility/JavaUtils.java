package bluej.utility;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import bluej.Config;
import bluej.debugger.gentype.GenType;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugger.gentype.GenTypeDeclTpar;

/*
 * Utilities for dealing with reflection, which must behave differently for
 * Java 1.4 / 1.5. Use the factory method "getJavaUtils" to retrieve an object
 * to use. 
 *   
 * @author Davin McCall
 * @version $Id: JavaUtils.java 2655 2004-06-24 05:53:55Z davmac $
 */
public abstract class JavaUtils {

    private static JavaUtils jutils;
    
    /**
     * Factory method. Returns a JavaUtils object.
     * @return an object supporting the approriate feature set
     */
    public static JavaUtils getJavaUtils()
    {
        if( jutils != null )
            return jutils;
        if( Config.isJava15() ) {
            try {
                Class J15Class = Class.forName("bluej.utility.JavaUtils15");
                jutils = (JavaUtils)J15Class.newInstance();
            }
            catch(ClassNotFoundException cnfe) { }
            catch(IllegalAccessException iae) { }
            catch(InstantiationException ie) { }
        }
        else
            jutils = new JavaUtils14();
        return jutils;
    }
    
    /**
     * Get a "signature" description of a method.
     * Looks like:  void method(int, int, int)
     *   (ie. excludes parameter names)
     * @param method The method to get the signature for
     * @return the signature string
     */
    abstract public String getSignature(Method method);
    
    /**
     * Get a "signature" description of a constructor.
     * Looks like:  ClassName(int, int, int)
     *   (ie. excludes parameter names)
     * @param cons the Constructor to get the signature for
     * @return the signature string
     */
    abstract public String getSignature(Constructor cons);
 
    /**
     * Get a "short description" of a method. This is like the signature,
     * but substitutes the parameter names for their types.
     * 
     * @param method   The method to get the description of
     * @param paramnames  The parameter names of the method
     * @return The description.
     */
    abstract public String getShortDesc(Method method, String [] paramnames);
    
    /**
     * Get a long String describing the method. A long description is
     * similar to the short description, but it has type names and parameters
     * included.
     */
    abstract public String getLongDesc(Method method, String [] paramnames);
    
    /**
     * Get a "short description" of a constructor. This is like the signature,
     * but substitutes the parameter names for their types.
     * 
     * @param constructor   The constructor to get the description of
     * @return The description.
     */
    abstract public String getShortDesc(Constructor constructor, String [] paramnames);
    
    /**
     * Get a long String describing the constructor. A long description is
     * similar to the short description, but it has type names and parameters
     * included.
     */
    abstract public String getLongDesc(Constructor constructor, String [] paramnames);
    
    abstract public boolean isVarArgs(Constructor cons);
    
    abstract public boolean isVarArgs(Method method);    
   
    abstract public boolean isEnum(Class cl);
    
    abstract public GenType getReturnType(Method method);
    
    /**
     * Get a list of the type parameters for a generic method.
     * (return an empty list if the method is not generic).
     * 
     * @param method   The method fro which to find the type parameters
     * @return  A list of GenTypeDeclTpar
     */
    abstract public List getTypeParams(Method method);
    
    abstract public List getTypeParams(Class cl);
    
    abstract public GenTypeClass getSuperclass(Class cl);
    
    /**
     * Get a list of the interfaces directly implemented by the given class.
     * @param cl  The class for which to find the interfaces
     * @return    An array of interfaces
     */
    abstract public GenTypeClass [] getInterfaces(Class cl);
    
    /**
     * Gets an array of nicely formatted strings with the types of the parameters.
     * Include the ellipsis (...) for a varargs method.
     * 
     * @param method The method to get the parameters for.
     */
    abstract public String[] getParameterTypes(Method method);
    
    /**
     * Get an array containing the argument types of the method.
     * 
     * In the case of a varargs method, the last argument will be an array
     * type.
     * 
     * @param method  the method whose argument types to get
     * @return  the argument types
     */
    abstract public GenType[] getParamGenTypes(Method method);
    
    /**
     * Gets an array of nicely formatted strings with the types of the parameters.
     * Include the ellipsis (...) for a varargs constructor.
     * 
     * @param constructor The constructor to get the parameters for.
     */
    abstract public String[] getParameterTypes(Constructor constructor);
    
    /**
     * Get an array containing the argument types of the method.
     * 
     * In the case of a varargs method, the last argument will be an array
     * type.
     * 
     * @param method  the method whose argument types to get
     * @return  the argument types
     */
    abstract public GenType[] getParamGenTypes(Constructor constructor);

    /**
     * Change a list of type parameters (with bounds) into a map, which maps
     * the name of the parameter to its bounding type.
     * 
     * @param tparams   A list of GenTypeDeclTpar
     * @return          A map (String -> GenTypeSolid)
     */
    public static Map TParamsToMap(List tparams)
    {
        Map rmap = new HashMap();
        for( Iterator i = tparams.iterator(); i.hasNext(); ) {
            GenTypeDeclTpar n = (GenTypeDeclTpar)i.next();
            rmap.put(n.getTparName(), n.getBound().mapTparsToTypes(rmap));
        }
        return rmap;
    }

    protected static String makeDescription(String name, String[] paramTypes, String[] paramNames, boolean includeTypeNames, boolean isVarArgs)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(name);
        sb.append("(");
        for (int j = 0; j < paramTypes.length; j++) {
            if (isVarArgs && j == paramTypes.length - 1) {
                if (includeTypeNames || paramNames == null || paramNames[j] == null)
                    sb.append(paramTypes[j].substring(0, paramTypes[j].length() - 2));
                sb.append(" ... ");
            }
            else if (includeTypeNames || paramNames == null || paramNames[j] == null) {                              
                sb.append(paramTypes[j]);
                sb.append(" ");
            }
            
            if (paramNames != null && paramNames[j] != null) {
                sb.append(paramNames[j]);
            }
            if (j < (paramTypes.length - 1))
                sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
}
