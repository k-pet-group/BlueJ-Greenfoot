package bluej.debugger.gentype;


/**
 * A "solid" type is a non-primitive, non-wildcard type. This includes arrays,
 * classes, and type parameters. Basically, a "solid" is anything that can be
 * a component type for a wildcard clause.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeSolid.java 3240 2004-12-16 00:04:59Z davmac $
 */
public abstract class GenTypeSolid extends GenTypeParameterizable {

    // force toString(boolean) to be reimplemented
    public abstract String toString(boolean stripPrefix);
    
    // force toString(NameTransform) to be reimplemented
    public abstract String toString(NameTransform nt);
    
    // provide a default implementation for toString().
    public String toString()
    {
        return toString(false);
    }
    
    public boolean isPrimitive()
    {
        return false;
    }
    
    public abstract boolean isInterface();
    
    /**
     * Get the upper bounds for this type, as an array of reference types.
     */
    public abstract GenTypeClass [] getUpperBoundsC();
    
    public GenTypeSolid [] getUpperBounds()
    {
        return getUpperBoundsC();
    }
}
