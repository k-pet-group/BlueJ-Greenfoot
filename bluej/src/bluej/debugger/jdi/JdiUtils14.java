package bluej.debugger.jdi;

import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ReferenceType;

/*
 * Jdi utilities, java 1.4 version.
 *  
 * @author Davin McCall
 * @version $Id: JdiUtils14.java 2830 2004-08-03 09:26:06Z polle $
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
    
    public String genericSignature(LocalVariable lv)
    {
        return null;
    }
    
    public boolean isEnum(ClassType ct)
    {
        return false;
    }
}
