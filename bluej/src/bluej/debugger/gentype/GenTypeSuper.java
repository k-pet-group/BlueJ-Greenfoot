package bluej.debugger.gentype;


/**
 * "? super ..." type.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeSuper.java 3063 2004-10-25 02:37:00Z davmac $
 */
public class GenTypeSuper extends GenTypeWildcard
{
    public GenTypeSuper(GenTypeSolid baseType) {
        super(null, baseType);
    }
    
    public String toString(boolean stripPrefix)
    {
        return "? super " + lowerBounds[0].toString(stripPrefix);
    }
    
    public String toString(NameTransform nt)
    {
        return "? super " + lowerBounds[0].toString(nt);
    }
}
