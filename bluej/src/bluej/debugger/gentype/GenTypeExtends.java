package bluej.debugger.gentype;

/**
 * "? extends ..." type.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeExtends.java 3063 2004-10-25 02:37:00Z davmac $
 */
public class GenTypeExtends extends GenTypeWildcard
{
    public GenTypeExtends(GenTypeSolid baseType)
    {
        super(baseType, null);
    }
}
