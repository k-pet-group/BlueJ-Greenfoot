package bluej.debugger.gentype;

/*
 * "boolean" primitive type
 * 
 * @author Davin McCall
 * @version $Id: GenTypeBool.java 2581 2004-06-10 01:09:01Z davmac $
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
}
