package bluej.debugger.gentype;

import java.util.Map;

/*
 * "byte" primitive type
 * 
 * @author Davin McCall
 * @version $Id: GenTypeByte.java 2656 2004-06-25 01:44:18Z davmac $
 */
public class GenTypeByte implements GenType
{
    public GenTypeByte()
    {
        super();
    }
    
    public String toString()
    {
        return toString(false);
    }
    
    public String toString(boolean stripPrefix)
    {
        return "byte";
    }
    
    public boolean isPrimitive()
    {
        return true;
    }
    
    public GenType mapTparsToTypes(Map tparams)
    {
        return this;
    }
}
