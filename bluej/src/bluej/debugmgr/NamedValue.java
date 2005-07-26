package bluej.debugmgr;

import bluej.debugger.gentype.JavaType;

/**
 * A named value, such as an object on the object bench or a local variable
 * in the code page.
 * 
 * @author Davin McCall
 * @version $Id: NamedValue.java 3478 2005-07-26 02:46:05Z davmac $
 */
public interface NamedValue
{
    /**
     * Get the name of the named value.
     */
    public String getName();

    /**
     * Check whether the value has been initialized. This is used to
     * distinguish established values from values which are expected to be
     * initialized by the user. If it returns false, the value is not yet
     * available.
     */
    public boolean isInitialized();
    
    /**
     * Check whether the value of this named value can be modified.
     */
    public boolean isFinal();
    
    /**
     * Get the nominated type of this value.
     */
    public JavaType getGenType();
}
