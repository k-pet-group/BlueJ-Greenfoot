package bluej.debugger.gentype;

import java.util.ArrayList;
import java.util.Map;

/**
 * A "solid" type is a non-primitive, non-wildcard type. This includes arrays,
 * classes, and type parameters. Basically, a "solid" is anything that can be
 * a component type for a wildcard clause.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeSolid.java 3075 2004-11-09 00:10:18Z davmac $
 */
public abstract class GenTypeSolid extends GenTypeParameterizable {

    /*
     * @see bluej.debugger.gentype.GenTypeParameterizable#equals(bluej.debugger.gentype.GenTypeParameterizable)
     */
    public abstract boolean equals(GenTypeParameterizable other);

    /*
     * @see bluej.debugger.gentype.GenTypeParameterizable#getParamsFromTemplate(java.util.Map, bluej.debugger.gentype.GenTypeParameterizable)
     */
    public abstract void getParamsFromTemplate(Map map,
            GenTypeParameterizable template);

    /*
     * @see bluej.debugger.gentype.GenType#toString(boolean)
     */
    public abstract String toString(boolean stripPrefix);
    
    public String toString()
    {
        return toString(false);
    }
    
    /**
     * Get the upper bounds for this type, as an array of reference types.
     */
    public abstract GenTypeClass [] getUpperBoundsC();
    
    public GenTypeSolid [] getUpperBounds()
    {
        return getUpperBoundsC();
    }
    
    public static GenTypeClass gcd(GenTypeSolid a, GenTypeSolid b)
    {
        return commonBases(a,b)[0];
    }
    
    /**
     * Find the common super type(s).<p>
     * 
     * For two classes which extend a common base class, this should return
     * the base class. If the two classes implement the same interface, the
     * returned array will also contain the interface (unless the interface
     * is indirectly implemented via the common base class).
     */
    public static GenTypeClass [] commonBases(GenTypeSolid a, GenTypeSolid b)
    {
        GenTypeClass [] aUpper = a.getUpperBoundsC();
        GenTypeClass [] bUpper = b.getUpperBoundsC();
        ArrayList r = new ArrayList();
        // Return any bases which are common to any two upper bounds
        
        for (int i = 0; i < aUpper.length; i++) {
            for (int j = 0; j < bUpper.length; j++) {
                GenTypeClass.getCommonBases(aUpper[i], bUpper[j], r);
            }
        }
        
        // TODO duplicates can be combined via precisification
        return (GenTypeClass []) r.toArray(new GenTypeClass[0]);
    }
}
