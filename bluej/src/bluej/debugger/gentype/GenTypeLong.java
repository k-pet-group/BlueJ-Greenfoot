package bluej.debugger.gentype;


/**
 * "long" primitive type
 * 
 * @author Davin McCall
 * @version $Id: GenTypeLong.java 3075 2004-11-09 00:10:18Z davmac $
 */
public class GenTypeLong extends GenTypePrimitive
{
    public GenTypeLong()
    {
        super();
    }
    
    public String toString()
    {
        return "long";
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
        else if (t instanceof GenTypeLong)
            return true;
        else
            return false;
    }
}
