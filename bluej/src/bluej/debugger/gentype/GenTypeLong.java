package bluej.debugger.gentype;

/*
 * "long" primitive type
 * 
 * @author Davin McCall
 * @version $Id: GenTypeLong.java 2615 2004-06-16 07:01:33Z davmac $
 */
public class GenTypeLong implements GenType
{
    public GenTypeLong()
    {
        super();
    }
    
    public String toString(boolean stripPrefix)
    {
        return "long";
    }
    
    public boolean isPrimitive()
    {
        return true;
    }
}
