package bluej.debugger.gentype;

/*
 * "long" primitive type
 * 
 * @author Davin McCall
 * @version $Id: GenTypeLong.java 2581 2004-06-10 01:09:01Z davmac $
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
}
