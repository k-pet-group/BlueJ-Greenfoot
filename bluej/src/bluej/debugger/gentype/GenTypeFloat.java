package bluej.debugger.gentype;


/**
 * "float" primitive type.
 *  
 * @author Davin McCall
 * @version $Id: GenTypeFloat.java 3075 2004-11-09 00:10:18Z davmac $
 */
public class GenTypeFloat extends GenTypePrimitive
{
    public GenTypeFloat()
    {
        super();
    }
    
    public String toString()
    {
        return "float";
    }
    
    public boolean isAssignableFrom(GenType t)
    {
        if (t instanceof GenTypeByte)
            return true;
        else if (t instanceof GenTypeChar)
            return true;
        else if (t instanceof GenTypeShort)
            return true;
        else if (t instanceof GenTypeInt)
            return true;
        else if (t instanceof GenTypeLong)
            return true;
        else if (t instanceof GenTypeFloat)
            return true;
        else
            return false;
    }
}
