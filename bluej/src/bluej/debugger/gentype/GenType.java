package bluej.debugger.gentype;

/* GenType, a tree strucutre describing a type (including generic types).
 * 
 * Most functionality is in subclasses.
 * 
 * @author Davin McCall
 * @version $Id: GenType.java 2581 2004-06-10 01:09:01Z davmac $
 */

public interface GenType {

    public String toString(boolean stripPrefix);

}
