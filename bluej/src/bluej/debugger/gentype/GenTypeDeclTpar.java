package bluej.debugger.gentype;

/**
 * This represents a type parameter in a declaration list. It is the same
 * as a type parameter anywhere else, except that it can be bounded.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeDeclTpar.java 3075 2004-11-09 00:10:18Z davmac $
 */
public class GenTypeDeclTpar extends GenTypeTpar {

    GenTypeSolid [] upperBounds;
    
    public GenTypeDeclTpar(String parname, GenTypeSolid bound) {
        super(parname);
        upperBounds = new GenTypeSolid [] { bound };
    }
    
    /**
     * Constructor for a type parameter with bounds. The array passed to this
     * constructor should not be modified afterwards.
     * 
     * @param parname  The name of this type parameter
     * @param bounds   The declared upper bounds for this type parameter
     */
    public GenTypeDeclTpar(String parname, GenTypeSolid [] bounds) {
        super(parname);
        upperBounds = bounds;
    }
    
    // TODO: replace all calls to this with "getBounds".
    public GenTypeSolid getBound() {
        return upperBounds[0];
    }
    
    /**
     * Get the bounds of this type parameter, as an array of GenTypeSolid.
     */
    public GenTypeSolid [] upperBounds() {
        GenTypeSolid [] r = new GenTypeSolid [upperBounds.length];
        System.arraycopy(upperBounds, 0, r, 0, upperBounds.length);
        return r;
    }
    
    /**
     * Returns a string describing this type parameter. This includes name and bound as written in Java. <br>
     * Example: T extends Integer
     */
    public String toString(boolean stripPrefix) {
        //need prefix to match java.lang.Object
        String bound = getBound().toString(false); 
        if (bound.equals("java.lang.Object")) {
            return getTparName();
        } else {
            //now we strip the prefix if needed
            return getTparName() + " extends " + getBound().toString(stripPrefix);
        }
    }

}
