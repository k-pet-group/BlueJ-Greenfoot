package bluej.debugger.gentype;


/**
 * "short" primitive type.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeShort.java 3075 2004-11-09 00:10:18Z davmac $
 */
public class GenTypeShort extends GenTypePrimitive
{
    public GenTypeShort()
    {
        super();
    }
    
    public String toString()
    {
        return "short";
    }
    
    public boolean isAssignableFrom(GenType t)
    {
        if (t instanceof GenTypeByte)
            return true;
        else if (t instanceof GenTypeShort)
            return true;
        else
            return false;
    }
}
