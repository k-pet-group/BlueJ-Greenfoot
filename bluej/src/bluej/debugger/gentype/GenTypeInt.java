package bluej.debugger.gentype;

/*
 * "int" primitive type.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeInt.java 2615 2004-06-16 07:01:33Z davmac $
 */
public class GenTypeInt implements GenType
{
    public GenTypeInt()
    {
        super();
    }
    
    public String toString(boolean stripPrefix)
    {
        return "int";
    }
    
    public boolean isPrimitive()
    {
        return true;
    }
}
