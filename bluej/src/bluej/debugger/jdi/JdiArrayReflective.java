package bluej.debugger.jdi;

import bluej.debugger.gentype.*;

import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;

/**
 * A proxy-type reflective for arrays.
 * 
 * @author Davin McCall
 * @version $Id: JdiArrayReflective.java 3102 2004-11-18 01:39:18Z davmac $
 */
public class JdiArrayReflective extends JdiReflective {

    private GenType componentType;
    
    public JdiArrayReflective(GenType t, ReferenceType srctype)
    {
        super(null, srctype);
        componentType = t;
    }
    
    public JdiArrayReflective(GenType t, ClassLoaderReference classLoader, VirtualMachine vm)
    {
        super(null, classLoader, vm);
    }
    
    public String getName()
    {
        checkLoaded();
        return super.getName();
    }
    
    protected void checkLoaded()
    {
        name = "[" + componentName();
        super.checkLoaded();
    }
    
    /**
     * Get the component name, as it appears in the class name given to a
     * classloader.
     */
    private String componentName()
    {
        if (componentType.typeIs(GenType.GT_BOOLEAN))
            return "Z";
        if (componentType.typeIs(GenType.GT_BYTE))
            return "B";
        if (componentType.typeIs(GenType.GT_CHAR))
            return "C";
        if (componentType.typeIs(GenType.GT_DOUBLE))
            return "D";
        if (componentType.typeIs(GenType.GT_FLOAT))
            return "F";
        if (componentType.typeIs(GenType.GT_INT))
            return "I";
        if (componentType.typeIs(GenType.GT_LONG))
            return "J";
        if (componentType.typeIs(GenType.GT_SHORT))
            return "S";

        if (componentType instanceof GenTypeArray) {
            Reflective r = ((GenTypeArray) componentType).getReflective();
            return r.getName();
        }

        // If we get to here, assume it's a class/interface type.
        GenTypeClass gtc = (GenTypeClass) componentType;
        return "L" + gtc.rawName() + ";";
    }
}
