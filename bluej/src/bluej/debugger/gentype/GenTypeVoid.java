package bluej.debugger.gentype;


/**
 * Primitive type "void".
 * 
 * @author Davin McCall
 * @version $Id: GenTypeVoid.java 3075 2004-11-09 00:10:18Z davmac $
 */
public class GenTypeVoid extends GenTypePrimitive
{
    public GenTypeVoid()
    {
        super();
    }

    public String toString() {
        return "void";
    }

    public boolean isAssignableFrom(GenType t)
    {
        return false;
    }
}
