package bluej.debugger.gentype;

import java.util.Map;

public class GenTypeArray extends GenTypeClass
{
    GenType baseType;
    
    public GenTypeArray(GenType baseType, Reflective r)
    {
        super(r);
        this.baseType = baseType;
    }

    public String toString(boolean stripPrefix)
    {
        return baseType.toString(stripPrefix) + "[]";
    }
    
    public GenType getBaseType()
    {
        return baseType;
    }
    
    public GenType mapTparsToTypes(Map tparams)
    {
        GenType newBase = baseType.mapTparsToTypes(tparams);
        if( newBase == baseType )
            return this;
        else
            return new GenTypeArray(newBase, reflective);
    }

}
