package bluej.debugger.gentype;

/*
 * "double" primitive type.
 *  
 * @author Davin McCall
 * @version $Id: GenTypeDouble.java 2615 2004-06-16 07:01:33Z davmac $
 */
public class GenTypeDouble implements GenType
{
    public GenTypeDouble()
    {
        super();
    }
    
    public String toString(boolean stripPrefix)
    {
        return "double";
    }
    
    public boolean isPrimitive()
    {
        return true;
    }
}
