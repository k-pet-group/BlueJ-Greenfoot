package bluej.debugger.gentype;


/**
 * "boolean" primitive type
 * 
 * @author Davin McCall
 * @version $Id: GenTypeBool.java 3075 2004-11-09 00:10:18Z davmac $
 */
public class GenTypeBool extends GenTypePrimitive
{
    public GenTypeBool()
    {
        super();
    }
    
    public String toString()
    {
        return "boolean";
    }
    
    public boolean isAssignableFrom(GenType t)
    {
        if (t instanceof GenTypeBool)
            return true;
        else
            return false;
    }
}
