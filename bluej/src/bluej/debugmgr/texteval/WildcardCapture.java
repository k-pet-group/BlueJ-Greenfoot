package bluej.debugmgr.texteval;

import bluej.debugger.gentype.GenTypeDeclTpar;
import bluej.debugger.gentype.GenTypeSolid;
import bluej.debugger.gentype.NameTransform;

/**
 * Represent the capture of a wildcard. This is really an unnamed type parameter.
 * 
 * @author Davin McCall
 * @version $Id$
 */
public class WildcardCapture extends GenTypeDeclTpar
{
    public WildcardCapture(GenTypeSolid [] ubounds)
    {
        super(null, ubounds);
    }
    
    public WildcardCapture(GenTypeSolid [] ubounds, GenTypeSolid lbound)
    {
        super(null, ubounds, lbound);
    }
    
    //public JavaType mapTparsToTypes(Map tparams)
    //{
    //    // We behave differently than a regular tpar - no mapping occurs.
    //    return this;
    //}

    public String toString(boolean stripPrefix)
    {
        // Need to be a little careful, as one of the bounds could be a capture itself.
        if (lBound != null) {
            return "capture of ? super " + lBound.getReferenceSupertypes()[0].toString(stripPrefix);
        }
        if (upperBounds.length != 0) {
            return "capture of ? extends " + upperBounds[0].getReferenceSupertypes()[0].toString(stripPrefix);
        }
        return "capture of ?";
    }
    
    public String toString(NameTransform nt)
    {
        // Need to be a little careful, as one of the bounds could be a capture itself.
        if (lBound != null) {
            return "? super " + lBound.getReferenceSupertypes()[0].toString(nt);
        }
        if (upperBounds.length != 0) {
            return "? extends " + upperBounds[0].getReferenceSupertypes()[0].toString(nt);
        }
        return "?";
    }

}
