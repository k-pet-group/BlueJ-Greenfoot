package bluej.debugger.gentype;

/* Primitive type "void".
 * 
 * @author Davin McCall
 */
public class GenTypeVoid implements GenType {

    public String toString(boolean stripPrefix) {
        return "void";
    }

    public boolean isPrimitive()
    {
        return true;
    }
}
