package bluej.debugger.gentype;

import java.util.Map;

/*
 * "float" primitive type.
 *  
 * @author Davin McCall
 * @version $Id: GenTypeFloat.java 2655 2004-06-24 05:53:55Z davmac $
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
    
    public boolean isPrimitive()
    {
        return true;
    }
    
    public GenType mapTparsToTypes(Map tparams)
    {
        return this;
    }
}
