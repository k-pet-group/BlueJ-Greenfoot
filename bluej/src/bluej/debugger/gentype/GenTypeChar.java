package bluej.debugger.gentype;

/*
 * "char" primitive type.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeChar.java 2615 2004-06-16 07:01:33Z davmac $
 */
public class GenTypeChar implements GenType
{
    public GenTypeChar()
    {
        super();
    }
    
    public String toString(boolean stripPrefix)
    {
        return "char";
    }
    
    public boolean isPrimitive()
    {
        return true;
    }
}
