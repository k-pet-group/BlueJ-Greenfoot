package bluej.debugger.gentype;

import java.util.Map;

/* A "solid" type is a non-primitive, non-wildcard type. This includes arrays,
 * classes, and type parameters. Basically, a "solid" is anything that can be
 * a component type for a wildcard clause.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeSolid.java 2581 2004-06-10 01:09:01Z davmac $
 */
public abstract class GenTypeSolid extends GenTypeParameterizable {

    /**
     * @see bluej.debugger.gentype.GenTypeParameterizable#mapTparsToTypes(java.util.Map)
     */
    protected abstract GenTypeParameterizable mapTparsToTypes(Map tparams);

    /**
     * @see bluej.debugger.gentype.GenTypeParameterizable#equals(bluej.debugger.gentype.GenTypeParameterizable)
     */
    public abstract boolean equals(GenTypeParameterizable other);

    /**
     * @see bluej.debugger.gentype.GenTypeParameterizable#getParamsFromTemplate(java.util.Map, bluej.debugger.gentype.GenTypeParameterizable)
     */
    protected abstract void getParamsFromTemplate(Map map,
            GenTypeParameterizable template);

    /**
     * @see bluej.debugger.gentype.GenType#toString(boolean)
     */
    public abstract String toString(boolean stripPrefix);

}
