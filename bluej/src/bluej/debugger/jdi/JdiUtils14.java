package bluej.debugger.jdi;

import com.sun.jdi.Field;
import com.sun.jdi.ReferenceType;

/*
 * Jdi utilities, java 1.4 version.
 *  
 * @author Davin McCall
 * @version $Id: JdiUtils14.java 2582 2004-06-10 04:32:41Z davmac $
 */
public class JdiUtils14 extends JdiUtils {

    public boolean hasGenericSig(Field f)
    {
        return false;
    }
    
    public String genericSignature(Field f)
    {
        return null;
    }
    
    public String genericSignature(ReferenceType rt)
    {
        return null;
    }
}
