package bluej.debugger.gentype;


/**
 * "double" primitive type.
 *  
 * @author Davin McCall
 * @version $Id: GenTypeDouble.java 3075 2004-11-09 00:10:18Z davmac $
 */
public class GenTypeDouble extends GenTypePrimitive
{
    public GenTypeDouble()
    {
        super();
    }
    
    public String toString()
    {
        return "double";
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
        else if (t instanceof GenTypeDouble)
            return true;
        else
            return false;
    }
}
