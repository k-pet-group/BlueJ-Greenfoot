package bluej.debugger.gentype;

/*
 * "byte" primitive type
 * 
 * @author Davin McCall
 * @version $Id: GenTypeByte.java 2581 2004-06-10 01:09:01Z davmac $
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
}
