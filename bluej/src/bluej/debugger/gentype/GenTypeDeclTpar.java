package bluej.debugger.gentype;

/*
 * This represents a type parameter in a declaration list. It is the same
 * as a type parameter anywhere else, except that it can be bounded.
 * 
 * @author Davin McCall
 */
public class GenTypeDeclTpar extends GenTypeTpar {

    GenTypeSolid upperBound;
    
    public GenTypeDeclTpar(String parname, GenTypeSolid bound) {
        super(parname);
        upperBound = bound;
    }
    
    public GenTypeSolid getBound() {
        return upperBound;
    }
    
   
    /**
     * Returns a string describing this type parameter. This includes name and bound as written in Java. <br>
     * Example: T extends Integer
     */
    public String toString(boolean stripPrefix) {
        String bound = getBound().toString(false);
        if (bound.equals("java.lang.Object")) {
            return getTparName();
        } else {
            return getTparName() + " extends " + getBound();
        }
    }

}
