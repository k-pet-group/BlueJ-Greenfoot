package bluej.debugger.gentype;


/* An unbounded wildcard.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeUnbounded.java 3063 2004-10-25 02:37:00Z davmac $
 */
public class GenTypeUnbounded extends GenTypeWildcard
{
    public GenTypeUnbounded()
    {
        super(noBounds, noBounds);
    }
}
