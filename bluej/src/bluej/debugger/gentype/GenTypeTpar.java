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
    
    public String arrayComponentName()
    {
        // We don't know the erased type.
        throw new UnsupportedOperationException();
    }
    
    public boolean isInterface()
    {
        return false;
    }
    
    public boolean equals(GenTypeParameterizable other)
    {
        if( ! (other instanceof GenTypeTpar) )
            return false;
        return name.equals(((GenTypeTpar)other).name);
    }
    
    public JavaType mapTparsToTypes(Map tparams)
    {
        if (tparams == null)
            return this;
        
        GenTypeParameterizable newType = (GenTypeParameterizable)tparams.get(name);
        if( newType == null )
            return this;
        else
            return newType;
    }
    
    public void getParamsFromTemplate(Map map, GenTypeParameterizable template)
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
    
    public GenTypeParameterizable precisify(GenTypeParameterizable other)
    {
        return other;
    }
    
    public boolean isPrimitive()
    {
        return false;
    }
    
    public boolean isAssignableFrom(JavaType t)
    {
        if (t instanceof GenTypeTpar)
            if (((GenTypeTpar)t).name.equals(name))
                return true;

        return false;
    }
    
    public boolean isAssignableFromRaw(JavaType t)
    {
        // We don't know the erased type.
        throw new UnsupportedOperationException();
    }
    
    public GenTypeClass [] getUpperBoundsC()
    {
        // We don't know the erased type.
        throw new UnsupportedOperationException();
    }
    
    public GenTypeSolid [] getLowerBounds()
    {
        // We don't know the erased type.
        throw new UnsupportedOperationException();
    }

    public JavaType getErasedType()
    {
        // We don't know the erased type.
        throw new UnsupportedOperationException();
    }
}
