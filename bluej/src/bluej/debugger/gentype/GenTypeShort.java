package bluej.debugger.gentype;

/*
 * "short" primitive type.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeShort.java 2581 2004-06-10 01:09:01Z davmac $
 */
public class GenTypeShort implements GenType
{
    public GenTypeShort()
    {
        super();
    }
    
    public String toString(boolean stripPrefix)
    {
        return "short";
    }
}
