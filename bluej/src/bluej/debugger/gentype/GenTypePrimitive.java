package bluej.debugger.gentype;

import java.util.Map;

/**
 * Base class for primitive types.
 * 
 * @author Davin McCall
 */
abstract public class GenTypePrimitive
    implements GenType
{
    /* (non-Javadoc)
     * 
     * primitive types cannot have type prefixes.
     *  
     * @see bluej.debugger.gentype.GenType#toString(boolean)
     */
    public String toString(boolean stripPrefix)
    {
        return toString();
    }
    
    /* (non-Javadoc)
     * @see bluej.debugger.gentype.GenType#isPrimitive()
     */
    public boolean isPrimitive()
    {
        return true;
    }

    /* (non-Javadoc)
     * 
     * For primitive types, "isAssignableFromRaw" is equivalent to
     * "isAssignableFrom".
     * 
     * @see bluej.debugger.gentype.GenType#isAssignableFromRaw(bluej.debugger.gentype.GenType)
     */
    public boolean isAssignableFromRaw(GenType t)
    {
        return isAssignableFrom(t);
    }

    /* (non-Javadoc)
     * @see bluej.debugger.gentype.GenType#mapTparsToTypes(java.util.Map)
     */
    public GenType mapTparsToTypes(Map tparams)
    {
        return this;
    }

    public GenType getArrayComponent()
    {
        return null;
    }

}
