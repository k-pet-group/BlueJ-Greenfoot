package bluej.debugger.jdi;

import bluej.Config;
import bluej.debugger.DebuggerObject;

import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;

/*
 * Utility methods for Jdi. Used to abstract away differences between java
 * 1.4 and 1.5
 * 
 * @author Davin McCall
 * @version $Id: JdiUtils.java 2961 2004-08-30 12:54:12Z polle $
 */
public abstract class JdiUtils {

    private static JdiUtils jutils = null;
    private static final String nullLabel = Config.getString("debugger.null");
    
    /**
     * Factory method. Returns a JdiUtils object.
     * @return an object supporting the approriate feature set
     */
    public static JdiUtils getJdiUtils()
    {
        if( jutils != null )
            return jutils;
        if( Config.isJava15() ) {
            try {
                Class J15Class = Class.forName("bluej.debugger.jdi.JdiUtils15");
                jutils = (JdiUtils)J15Class.newInstance();
            }
            catch(ClassNotFoundException cnfe) { }
            catch(IllegalAccessException iae) { }
            catch(InstantiationException ie) { }
        }
        else
            jutils = new JdiUtils14();
        return jutils;
    }

    abstract public boolean hasGenericSig(Field f);
    
    abstract public String genericSignature(Field f);
    
    abstract public String genericSignature(ReferenceType rt);
    
    abstract public String genericSignature(LocalVariable lv);
    
    abstract public boolean isEnum(ClassType ct);
    
    /**
     *  Return the value of a field as as string.
     *
     *@param  val  Description of Parameter
     *@return      The ValueString value
     */
    public String getValueString(Value val)
    {
        if (val == null) {
            return nullLabel;
        }
        else if (val instanceof StringReference) {
            return "\"" + ((StringReference) val).value() + "\"";
            // toString should be okay for this as well once the bug is out...
        }
        else if (val.type() instanceof ClassType && isEnum((ClassType) val.type())) {
            ClassType type = (ClassType) val.type();
            Field nameField = type.fieldByName("name");
            String name = ((StringReference) ((ObjectReference) val).getValue(nameField)).value();
            return name;
        }
        else if (val instanceof ObjectReference) {
            return DebuggerObject.OBJECT_REFERENCE;
        }
        return val.toString();
    }
}
