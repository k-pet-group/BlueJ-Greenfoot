package bluej.debugger.gentype;

/*
 * "float" primitive type.
 *  
 * @author Davin McCall
 * @version $Id: GenTypeFloat.java 2581 2004-06-10 01:09:01Z davmac $
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
}
