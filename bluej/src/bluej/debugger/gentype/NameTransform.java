package bluej.debugger.gentype;

/*
 * Interface for representing some sort of transform on type names, such as
 * stripping of package prefixes.
 * 
 * @author Davin McCall
 * 
 * @version $Id: NameTransform.java 2818 2004-07-26 03:42:35Z davmac $
 */
public interface NameTransform
{
    /**
     * Translate the given (fully qualified) type name.
     * 
     * @param typeName
     *            The type name to translate
     * @return The translated type name
     */
    public String transform(String typeName);
}