package bluej.debugger.gentype;


/**
 * "int" primitive type.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeInt.java 3075 2004-11-09 00:10:18Z davmac $
 */
public class GenTypeInt extends GenTypePrimitive
{
    public GenTypeInt()
    {
        super();
    }
    
    public String toString()
    {
        return "int";
    }
    
    public boolean isAssignableFrom(GenType t)
    {
        if (t instanceof GenTypeChar)
            return true;
        else if (t instanceof GenTypeByte)
            return true;
        else if (t instanceof GenTypeShort)
            return true;
        else if (t instanceof GenTypeInt)
            return true;
        return false;
    }
}
