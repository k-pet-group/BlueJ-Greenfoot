package bluej.debugger.gentype;

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
}
