package bluej.debugger.jdi;

import bluej.debugger.gentype.*;

import com.sun.jdi.ReferenceType;

/**
 * A proxy-type reflective for arrays.
 * 
 * @author Davin McCall
 * @version $Id: JdiArrayReflective.java 2581 2004-06-10 01:09:01Z davmac $
 */
public class JdiArrayReflective extends JdiReflective {

    private GenType componentType;
    
    public JdiArrayReflective(GenType t, ReferenceType srctype)
    {
        super(null, srctype);
        componentType = t;
    }
    
    protected void checkLoaded()
    {
        name = "[" + componentName();
        super.checkLoaded();
    }
    
    private String componentName()
    {
        if( componentType instanceof GenTypeBool )
            return "Z";
        if( componentType instanceof GenTypeByte )
            return "B";
        if( componentType instanceof GenTypeChar )
            return "C";
        if( componentType instanceof GenTypeDouble )
            return "D";
        if( componentType instanceof GenTypeFloat )
            return "F";
        if( componentType instanceof GenTypeInt )
            return "I";
        if( componentType instanceof GenTypeLong )
            return "J";
        if( componentType instanceof GenTypeShort )
            return "S";
        
        if( componentType instanceof GenTypeArray ) {
            Reflective r = ((GenTypeArray)componentType).getReflective();
            return r.getName();
        }
        
        // If we get to here, assume it's a class/interface type.
        GenTypeClass gtc = (GenTypeClass)componentType;
        return "L" + gtc.rawName() + ";";
    }
}
