package bluej.debugger.jdi;

import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;

/*
 * Jdi Utilities, java 1.5 version.
 * 
 * @author Davin McCall
 * @version $Id: JdiUtils15.java 2973 2004-09-01 10:44:52Z polle $
 */
public class JdiUtils15 extends JdiUtils
{
    public boolean hasGenericSig(ObjectReference obj)
    {
        return obj.referenceType().genericSignature() != null;
    }

    public String genericSignature(Field f)
    {
        return f.genericSignature();
    }

    public String genericSignature(ReferenceType rt)
    {
        return rt.genericSignature();
    }

    public String genericSignature(LocalVariable lv)
    {
        return lv.genericSignature();
    }
    
    public boolean isEnum(ClassType ct)
    {
        return ct.isEnum();
    }
}
