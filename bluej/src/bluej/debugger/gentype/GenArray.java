package bluej.debugger.gentype;

import java.util.Map;

/**
 * A general array type. The component type can be anything, including a
 * wildcard or type parameter. This is distinct from GenTypeArray, where the
 * component type must be some solid type.
 * 
 * @author Davin McCall
 * @version $Id: GenArray.java 3075 2004-11-09 00:10:18Z davmac $
 */
public class GenArray extends GenTypeParameterizable
{
    private GenType componentType;
    
    public GenArray(GenType componentType)
    {
        this.componentType = componentType;
    }
    
    public GenType getArrayComponent()
    {
        return componentType;
    }
    
    /* (non-Javadoc)
     * @see bluej.debugger.gentype.GenType#mapTparsToTypes(java.util.Map)
     */
    public GenType mapTparsToTypes(Map tparams)
    {
        GenType mappedComponent = componentType.mapTparsToTypes(tparams);
        if (mappedComponent == componentType)
            return this;
        else
            return new GenArray(mappedComponent);
    }

    /* (non-Javadoc)
     * @see bluej.debugger.gentype.GenTypeParameterizable#equals(bluej.debugger.gentype.GenTypeParameterizable)
     */
    public boolean equals(GenTypeParameterizable other)
    {
        if (this == other)
            return true;
        
        GenType otherComponent = other.getArrayComponent();
        if (otherComponent != null)
            return componentType.equals(otherComponent);
        else
            return false;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.gentype.GenTypeParameterizable#getParamsFromTemplate(java.util.Map, bluej.debugger.gentype.GenTypeParameterizable)
     */
    protected void getParamsFromTemplate(Map map, GenTypeParameterizable template)
    {
        GenType otherComponent = template.getArrayComponent();
        if (componentType instanceof GenTypeParameterizable) {
            GenTypeParameterizable pcomponentType = (GenTypeParameterizable) componentType;
            if (otherComponent instanceof GenTypeParameterizable) {
                pcomponentType.getParamsFromTemplate(map, (GenTypeParameterizable) otherComponent);
            }
        }
    }

    /* (non-Javadoc)
     * @see bluej.debugger.gentype.GenTypeParameterizable#precisify(bluej.debugger.gentype.GenTypeParameterizable)
     */
    public GenTypeParameterizable precisify(GenTypeParameterizable other)
    {
        // TODO Auto-generated method stub
        return this;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.gentype.GenTypeParameterizable#getUpperBounds()
     */
    public GenTypeSolid[] getUpperBounds()
    {
        // TODO Auto-generated method stub
        return new GenTypeSolid[0];
    }

    /* (non-Javadoc)
     * @see bluej.debugger.gentype.GenTypeParameterizable#getLowerBounds()
     */
    public GenTypeSolid[] getLowerBounds()
    {
        // TODO Auto-generated method stub
        return new GenTypeSolid[0];
    }

    /* (non-Javadoc)
     * @see bluej.debugger.gentype.GenTypeParameterizable#toString(bluej.debugger.gentype.NameTransform)
     */
    public String toString(NameTransform nt)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.gentype.GenType#toString(boolean)
     */
    public String toString(boolean stripPrefix)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.gentype.GenType#isPrimitive()
     */
    public boolean isPrimitive()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.gentype.GenType#isAssignableFrom(bluej.debugger.gentype.GenType)
     */
    public boolean isAssignableFrom(GenType t)
    {
        if (t instanceof GenTypeArray)
            return componentType.isAssignableFrom(((GenArray)t).componentType);
        else
            return false;
    }

    /* (non-Javadoc)
     * @see bluej.debugger.gentype.GenType#isAssignableFromRaw(bluej.debugger.gentype.GenType)
     */
    public boolean isAssignableFromRaw(GenType t)
    {
        if (t instanceof GenTypeArray)
            return componentType.isAssignableFromRaw(((GenArray)t).componentType);
        else
            return false;
    }

}
