package bluej.debugger.gentype;

import java.util.Map;

/* GenType, a tree strucutre describing a type (including generic types).
 * 
 * Most functionality is in subclasses.
 * 
 * @author Davin McCall
 * @version $Id: GenType.java 2655 2004-06-24 05:53:55Z davmac $
 */

public interface GenType {

    public String toString(boolean stripPrefix);

    public boolean isPrimitive();
    
    /**
     * Get an equivalent type where the type parameters have been mapped to
     * an actual type.
     * 
     * @param tparams A map (String->GenType) mapping the name of the type
     *                parameter to the corresponding type
     * @return A type with parameters mapped
     */
    public abstract GenType mapTparsToTypes(Map tparams);

}
