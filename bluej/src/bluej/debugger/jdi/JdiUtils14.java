package bluej.debugger.jdi;

import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;

/*
 * Jdi utilities, java 1.4 version.
 *  
 * @author Davin McCall
 * @version $Id: JdiUtils14.java 2973 2004-09-01 10:44:52Z polle $
 */
public class JdiUtils14 extends JdiUtils {

    public boolean hasGenericSig(ObjectReference obj)
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
