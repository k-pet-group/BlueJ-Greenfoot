package bluej.debugger.gentype;

import java.util.Map;

/*
 * "char" primitive type.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeChar.java 2656 2004-06-25 01:44:18Z davmac $
 */
public class GenTypeChar implements GenType
{
    public GenTypeChar()
    {
        super();
    }
    
    public String toString()
    {
        return toString(false);
    }
    
    public String toString(boolean stripPrefix)
    {
        return "char";
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
