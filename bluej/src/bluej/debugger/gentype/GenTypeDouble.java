package bluej.debugger.gentype;

import java.util.Map;

/*
 * "double" primitive type.
 *  
 * @author Davin McCall
 * @version $Id: GenTypeDouble.java 2655 2004-06-24 05:53:55Z davmac $
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
    
    public boolean isPrimitive()
    {
        return true;
    }
    
    public GenType mapTparsToTypes(Map tparams)
    {
        return this;
    }
}
