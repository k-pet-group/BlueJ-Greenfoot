package bluej.debugger.gentype;

import java.util.Map;

/*
 * "double" primitive type.
 *  
 * @author Davin McCall
 * @version $Id: GenTypeDouble.java 2656 2004-06-25 01:44:18Z davmac $
 */
public class GenTypeDouble implements GenType
{
    public GenTypeDouble()
    {
        super();
    }
    
    public String toString()
    {
        return toString(false);
    }
    
    public String toString(boolean stripPrefix)
    {
        return "double";
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
