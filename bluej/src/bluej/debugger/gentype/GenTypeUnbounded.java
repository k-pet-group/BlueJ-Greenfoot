package bluej.debugger.gentype;

import java.util.Map;

/* An unbounded wildcard.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeUnbounded.java 2581 2004-06-10 01:09:01Z davmac $
 */
public class GenTypeUnbounded extends GenTypeWildcard
{
    private String unboundedString = "?";
    
    public GenTypeUnbounded()
    {
        super(null, null);
    }
    
    public String toString(boolean stripPrefix)
    {
        return unboundedString;
    }
    
    protected GenTypeParameterizable precisify(GenTypeParameterizable other)
    {
        // Anything is more precise than this!
        return other;
    }
        
    protected GenTypeParameterizable mapTparsToTypes(Map tparams)
    {
        return this;
    }
    
    public boolean equals(GenTypeParameterizable other)
    {
        // All instances of GenTypeUnbounded are equal.
        return (other instanceof GenTypeUnbounded);
    }

}
