package bluej.debugger.jdi;

import com.sun.jdi.Field;
import com.sun.jdi.ReferenceType;

/*
 * Jdi Utilities, java 1.5 version.
 * 
 * @author Davin McCall
 * @version $Id: JdiUtils15.java 2582 2004-06-10 04:32:41Z davmac $
 */
public class JdiUtils15 extends JdiUtils {

    public boolean hasGenericSig(Field f) {
        return f.genericSignature() != null;
    }

    public String genericSignature(Field f) {
        return f.genericSignature();
    }

    public String genericSignature(ReferenceType rt) {
        return rt.genericSignature();
    }

}
