package bluej.debugger.gentype;

/*
 * "float" primitive type.
 *  
 * @author Davin McCall
 * @version $Id: GenTypeFloat.java 2615 2004-06-16 07:01:33Z davmac $
 */
public class GenTypeFloat implements GenType
{
    public GenTypeFloat()
    {
        super();
    }
    
    public String toString(boolean stripPrefix)
    {
        return "float";
    }
    
    public boolean isPrimitive()
    {
        return true;
    }
}
