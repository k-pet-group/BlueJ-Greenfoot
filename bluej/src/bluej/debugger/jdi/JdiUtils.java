package bluej.debugger.jdi;

import bluej.Config;

import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ReferenceType;

/*
 * Utility methods for Jdi. Used to abstract away differences between java
 * 1.4 and 1.5
 * 
 * @author Davin McCall
 * @version $Id: JdiUtils.java 2830 2004-08-03 09:26:06Z polle $
 */
public abstract class JdiUtils {

    private static JdiUtils jutils = null;
    
    /**
     * Factory method. Returns a JdiUtils object.
     * @return an object supporting the approriate feature set
     */
    public static JdiUtils getJdiUtils()
    {
        if( jutils != null )
            return jutils;
        if( Config.isJava15() ) {
            try {
                Class J15Class = Class.forName("bluej.debugger.jdi.JdiUtils15");
                jutils = (JdiUtils)J15Class.newInstance();
            }
            catch(ClassNotFoundException cnfe) { }
            catch(IllegalAccessException iae) { }
            catch(InstantiationException ie) { }
        }
        else
            jutils = new JdiUtils14();
        return jutils;
    }

    abstract public boolean hasGenericSig(Field f);
    
    abstract public String genericSignature(Field f);
    
    abstract public String genericSignature(ReferenceType rt);
    
    abstract public String genericSignature(LocalVariable lv);
    
    abstract public boolean isEnum(ClassType ct);
}
