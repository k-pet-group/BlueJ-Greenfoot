package bluej.debugger.gentype;

/*
 * "short" primitive type.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeShort.java 2615 2004-06-16 07:01:33Z davmac $
 */
public class GenTypeShort implements GenType
{
    public GenTypeShort()
    {
        super();
    }
    
    public String toString(boolean stripPrefix)
    {
        return "short";
    }
    
    public boolean isPrimitive()
    {
        return true;
    }
}
