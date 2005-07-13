package bluej.debugger.jdi;

import bluej.debugger.gentype.*;

import com.sun.jdi.ClassLoaderReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;

/**
 * A proxy-type reflective for arrays.
 * 
 * @author Davin McCall
 * @version $Id: JdiArrayReflective.java 3463 2005-07-13 01:55:27Z davmac $
 */
public class JdiArrayReflective extends JdiReflective {

    private JavaType componentType;
    
    public JdiArrayReflective(JavaType t, ReferenceType srctype)
    {
        super(null, srctype);
        componentType = t;
    }
    
    public JdiArrayReflective(JavaType t, ClassLoaderReference classLoader, VirtualMachine vm)
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
        if (componentType.typeIs(JavaType.JT_BOOLEAN))
            return "Z";
        if (componentType.typeIs(JavaType.JT_BYTE))
            return "B";
        if (componentType.typeIs(JavaType.JT_CHAR))
            return "C";
        if (componentType.typeIs(JavaType.JT_DOUBLE))
            return "D";
        if (componentType.typeIs(JavaType.JT_FLOAT))
            return "F";
        if (componentType.typeIs(JavaType.JT_INT))
            return "I";
        if (componentType.typeIs(JavaType.JT_LONG))
            return "J";
        if (componentType.typeIs(JavaType.JT_SHORT))
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
