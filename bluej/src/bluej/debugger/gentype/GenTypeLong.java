package bluej.debugger.gentype;

import java.util.Map;

/*
 * "long" primitive type
 * 
 * @author Davin McCall
 * @version $Id: GenTypeLong.java 2655 2004-06-24 05:53:55Z davmac $
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
    
    public boolean isPrimitive()
    {
        return true;
    }
    
    public GenType mapTparsToTypes(Map tparams)
    {
        return this;
    }
}
