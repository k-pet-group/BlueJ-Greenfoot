package bluej.debugger.gentype;


/**
 * "? super ..." type.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeSuper.java 3508 2005-08-08 04:18:26Z davmac $
 */
public class GenTypeSuper extends GenTypeWildcard
{
    public GenTypeSuper(GenTypeSolid baseType) {
        super(null, baseType);
    }
    
    public String toString(boolean stripPrefix)
    {
        return "? super " + lowerBound.toString(stripPrefix);
    }
    
    public String toString(NameTransform nt)
    {
        return "? super " + lowerBound.toString(nt);
    }
}
