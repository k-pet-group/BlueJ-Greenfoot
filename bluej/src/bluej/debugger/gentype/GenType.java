package bluej.debugger.gentype;

/* GenType, a tree strucutre describing a type (including generic types).
 * 
 * Most functionality is in subclasses.
 * 
 * @author Davin McCall
 * @version $Id: GenType.java 2615 2004-06-16 07:01:33Z davmac $
 */

public interface GenType {

    public String toString(boolean stripPrefix);

    public boolean isPrimitive();
}
