package bluej.debugger.gentype;

import java.util.Map;

/* Interface for a parameterizable type, that is, a type which could have
 * type parameters. This includes classes, arrays, wild card types, and
 * type parameters themselves.
 * 
 * @author Davin McCall
 */
public abstract class GenTypeParameterizable implements GenType
{
    
    /**
     * Return an equivalent type where all the type parameters have been
     * mapped to the corresponding types using the given map. 
     * @param tparams  A map of (String name -> GenType type).
     * @return  An equivalent type with parameters mapped.
     */
    protected abstract GenTypeParameterizable mapTparsToTypes(Map tparams);
    
    public abstract boolean equals(GenTypeParameterizable other);
    
    protected abstract void getParamsFromTemplate(Map map, GenTypeParameterizable template);
    
    protected abstract GenTypeParameterizable precisify(GenTypeParameterizable other);
}
