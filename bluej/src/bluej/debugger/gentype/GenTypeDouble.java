package bluej.debugger.gentype;

/*
 * "double" primitive type.
 *  
 * @author Davin McCall
 * @version $Id: GenTypeDouble.java 2581 2004-06-10 01:09:01Z davmac $
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
}
