package bluej.debugger.gentype;

import java.util.Map;


public class GenTypeTpar extends GenTypeSolid
{
    private String name;
    
    public GenTypeTpar(String parname)
    {
        name = parname;
    }
    
    public String getTparName()
    {
        return name;
    }
    
    public String toString(boolean stripPrefix)
    {
        return name;
    }
    
    public String toString(NameTransform nt)
    {
        return name;
    }
    
    public boolean equals(GenTypeParameterizable other)
    {
        if( ! (other instanceof GenTypeTpar) )
            return false;
        return name.equals(((GenTypeTpar)other).name);
    }
    
    public GenType mapTparsToTypes(Map tparams)
    {
        GenTypeParameterizable newType = (GenTypeParameterizable)tparams.get(name);
        if( newType == null )
            return this;
        else
            return newType;
    }
    
    protected void getParamsFromTemplate(Map map, GenTypeParameterizable template)
    {
        // If a mapping already exists, precisify it against the template.
        // Otherwise, create a new mapping to the template.
        
        GenTypeParameterizable x = (GenTypeSolid) map.get(name);
        if (x != null)
            x = x.precisify(template);
        else
            x = template;
        map.put(name, x);
    }
    
    protected GenTypeParameterizable precisify(GenTypeParameterizable other)
    {
        return other;
    }

    public boolean isPrimitive()
    {
        return false;
    }
}
