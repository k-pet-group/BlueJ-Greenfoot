package bluej.debugger.gentype;

/*
 * "int" primitive type.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeInt.java 2581 2004-06-10 01:09:01Z davmac $
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
}
