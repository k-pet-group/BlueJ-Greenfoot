package bluej.debugger.jdi;

import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ReferenceType;

/*
 * Jdi Utilities, java 1.5 version.
 * 
 * @author Davin McCall
 * @version $Id: JdiUtils15.java 2830 2004-08-03 09:26:06Z polle $
 */
public class JdiUtils15 extends JdiUtils
{
    public boolean hasGenericSig(Field f)
    {
        return f.genericSignature() != null;
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
