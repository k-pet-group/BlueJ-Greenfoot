package bluej.utility;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import bluej.Config;

/*
 * Utilities for dealing with reflection, which must behave differently for
 * Java 1.4 / 1.5. Use the factory method "getJavaUtils" to retrieve an object
 * to use. 
 *   
 * @author Davin McCall
 * @version $Id: JavaUtils.java 2568 2004-06-02 05:38:07Z davmac $
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
        if( Config.isJava15() )
            jutils = new JavaUtils15();
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
     * @param method   The method to get the description of
     * @param paramnames  The parameter names of the method
     * @return The description.
     */
    abstract public String getShortDesc(Method method, String [] paramnames);
}
