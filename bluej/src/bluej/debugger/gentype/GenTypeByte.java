package bluej.debugger.gentype;


/**
 * "byte" primitive type
 * 
 * @author Davin McCall
 * @version $Id: GenTypeByte.java 3075 2004-11-09 00:10:18Z davmac $
 */
public class GenTypeByte extends GenTypePrimitive
{
    public GenTypeByte()
    {
        super();
    }
    
    public String toString()
    {
        return "byte";
    }
    
    public boolean isAssignableFrom(GenType t)
    {
        if (t instanceof GenTypeByte)
            return true;
        else
            return false;
    }
}
