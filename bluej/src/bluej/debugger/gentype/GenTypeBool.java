package bluej.debugger.gentype;

import java.util.Map;

/*
 * "boolean" primitive type
 * 
 * @author Davin McCall
 * @version $Id: GenTypeBool.java 2656 2004-06-25 01:44:18Z davmac $
 */
public class GenTypeBool implements GenType
{
    public GenTypeBool()
    {
        super();
    }
    
    public String toString()
    {
        return toString(false);
    }
    
    public String toString(boolean stripPrefix)
    {
        return "boolean";
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
