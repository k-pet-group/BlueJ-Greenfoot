package bluej.debugger.gentype;

import java.util.Map;

/*
 * "int" primitive type.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeInt.java 2656 2004-06-25 01:44:18Z davmac $
 */
public class GenTypeInt implements GenType
{
    public GenTypeInt()
    {
        super();
    }
    
    public String toString()
    {
        return toString(false);
    }
    
    public String toString(boolean stripPrefix)
    {
        return "int";
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
