package bluej.debugger.gentype;

/*
 * "char" primitive type.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeChar.java 2581 2004-06-10 01:09:01Z davmac $
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
}
