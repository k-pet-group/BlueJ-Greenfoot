package bluej.debugger.gentype;

import java.util.Map;

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
    
    public GenType mapTparsToTypes(Map tparams)
    {
        return this;
    }
}
