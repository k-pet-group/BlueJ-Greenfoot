package bluej.debugger.gentype;

/*
 * "boolean" primitive type
 * 
 * @author Davin McCall
 * @version $Id: GenTypeBool.java 2615 2004-06-16 07:01:33Z davmac $
 */
public class GenTypeBool implements GenType
{
    public GenTypeBool()
    {
        super();
    }
    public String toString(boolean stripPrefix)
    {
        return "boolean";
    }
    public boolean isPrimitive()
    {
        return true;
    }
}
