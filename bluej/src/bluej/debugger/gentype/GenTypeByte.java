package bluej.debugger.gentype;

import java.util.Map;

/*
 * "byte" primitive type
 * 
 * @author Davin McCall
 * @version $Id: GenTypeByte.java 2655 2004-06-24 05:53:55Z davmac $
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
    
    public boolean isPrimitive()
    {
        return true;
    }
    
    public GenType mapTparsToTypes(Map tparams)
    {
        return this;
    }
}
