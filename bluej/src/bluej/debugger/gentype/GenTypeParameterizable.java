package bluej.debugger.gentype;

import java.util.Map;

/**
 * Interface for a parameterizable type, that is, a type which could have type
 * parameters. This includes classes, arrays, wild card types, and type
 * parameters themselves.
 * 
 * @author Davin McCall
 */
public abstract class GenTypeParameterizable
    implements GenType
{

    /**
     * Return an equivalent type where all the type parameters have been mapped
     * to the corresponding types using the given map.
     * 
     * @param tparams
     *            A map of (String name -> GenType type).
     * @return An equivalent type with parameters mapped.
     */
    abstract public GenType mapTparsToTypes(Map tparams);

    abstract public boolean equals(GenTypeParameterizable other);

    /**
     * Assuming that this is some type which encloses some type parameters by
     * name, and the given template is a similar type but with actual type
     * arguments, obtain a map which maps the name of the argument (in this
     * type) to the actual type (from the template type).<p>
     * 
     * The given map may already contain some mappings. In this case, the
     * existing mappings will be retained or made more specific.
     * 
     * @param map   A map (String -> GenTypeSolid) to which mappings should
     *              be added
     * @param template   The template to use
     */
    abstract protected void getParamsFromTemplate(Map map, GenTypeParameterizable template);

    /**
     * Find the most precise type that can be determined by taking into account
     * commonalities between this type and the given type. For instance if the
     * other class is a subtype of this type, return the subtype. Also, if
     * the types have wildcard type parameters, combine them to form a more
     * specific parameter.
     * 
     * @param other  The other type to precisify against
     * @return  The most precise determinable type, or null if this comparison
     *          is meaningless for the given type (incompatible types).
     */
    abstract protected GenTypeParameterizable precisify(GenTypeParameterizable other);

    abstract public String toString(NameTransform nt);
}