package bluej.debugger.gentype;

/*
 * "byte" primitive type
 * 
 * @author Davin McCall
 * @version $Id: GenTypeByte.java 2615 2004-06-16 07:01:33Z davmac $
 */
public class GenTypeByte implements GenType
{
    public GenTypeByte()
    {
        super();
    }
    public String toString(boolean stripPrefix)
    {
        return "byte";
    }
    public boolean isPrimitive()
    {
        return true;
    }
}
