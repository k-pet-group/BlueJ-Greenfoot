package bluej.debugger.gentype;


/**
 * "char" primitive type.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeChar.java 3075 2004-11-09 00:10:18Z davmac $
 */
public class GenTypeChar extends GenTypePrimitive
{
    public GenTypeChar()
    {
        super();
    }
    
    public String toString()
    {
        return "char";
    }
    
    public boolean isAssignableFrom(GenType t)
    {
        if (t instanceof GenTypeChar)
            return true;
        else
            return false;
    }
}
