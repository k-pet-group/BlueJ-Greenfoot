package bluej.debugger.gentype;

import java.util.Map;

public class GenTypeArray extends GenTypeClass
{
    JavaType baseType;
    
    public GenTypeArray(JavaType baseType, Reflective r)
    {
        super(r);
        this.baseType = baseType;
    }

    public String toString(boolean stripPrefix)
    {
        return baseType.toString(stripPrefix) + "[]";
    }
    
    public String toString(NameTransform nt)
    {
        if(baseType instanceof GenTypeParameterizable)
            return ((GenTypeParameterizable)baseType).toString(nt) + "[]";
        else
            return baseType.toString() + "[]";
    }
    
    public JavaType getArrayComponent()
    {
        return baseType;
    }
    
    public JavaType mapTparsToTypes(Map tparams)
    {
        JavaType newBase = baseType.mapTparsToTypes(tparams);
        if( newBase == baseType )
            return this;
        else
            return new GenTypeArray(newBase, reflective);
    }

}
