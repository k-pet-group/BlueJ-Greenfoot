package bluej.extensions;

import bluej.debugger.ObjectWrapper;
import bluej.debugger.jdi.JdiObject;
import bluej.pkgmgr.PkgMgrFrame;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.Field;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StringReference;
import com.sun.jdi.Value;
import com.sun.jdi.ShortValue;
import com.sun.jdi.LongValue;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.FloatValue;
import com.sun.jdi.DoubleValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.ByteValue;
import com.sun.jdi.BooleanValue;

import java.lang.reflect.Modifier;
import bluej.pkgmgr.Package;
import bluej.views.*;

/**
 * The BlueJ proxy Field object. This represents a field of a class or object.
 *
 * @author Clive Miller
 * @version $Id: BField.java 1651 2003-03-05 17:03:15Z damiano $
 * @see bluej.extensions.BObject#getField(java.lang.String)
 * @see bluej.extensions.BObject#getFields(boolean)
 * @see bluej.extensions.BClass#getStaticField(java.lang.String)
 * @see bluej.extensions.BClass#getStaticFields()
 */
public class BField
{
    private final Package bluej_pkg;
    private final ObjectReference ref;
    private final Field field;
    private final BObject obj;
    private final boolean array;
    private final int element;
    private FieldView bluej_view;

    BField (Package i_bluej_pkg, FieldView i_bluej_view )
    {
        bluej_pkg = i_bluej_pkg;
        bluej_view = i_bluej_view;
        element = 0;
        array = false;
        obj = null;
        field = null;
        ref = null;
    }        


  /*
    BField (Package i_bluej_pkg, ObjectReference ref, Field field)
    {
        bluej_pkg = i_bluej_pkg;
        this.ref = ref;
        this.field = field;
        this.array = false;
        this.element = 0;
        this.obj = null;
    }
    
    /**
     * Constructor for an array

    BField (Package i_bluej_pkg, ArrayReference ref, BObject obj, int element)
    {
        bluej_pkg = i_bluej_pkg;
        this.ref = ref;
        this.field = null;
        this.obj = obj;
        this.array = true;
        this.element = element;
    }


    /**
     * The name of the Field, as from reflection.
     */
    public String getName()
        {
        return bluej_view.getName();
        }

    /**
     * The type of the field, as from reflection
     */
    public Class getType()
        {
        return bluej_view.getDeclaringView().getClass();
        }

    /**
     * Gets this Filed Value on the given BObject
     */
    public Object get ( BObject onThisObject )
        {
        return null;  
        }
        
    /**
     * Gets the name of the type of this field object.
     * @return the name of the type of the value held in this field. For example,
     * <bl>
     *    <li>int
     *    <li>float
     * </bl>
     
    public String getTypeName()
    {
        String type = field == null ? obj.getType().getArrayType().getName()
                                    : field.typeName();
        if (type.equals("java.lang.String")) type = "String"; // don't ask me why!
        return type;
    }
*/
    /**
     * Gets the name of the field
     * @return a String containing the fully-qualified type name
    public String getName()
    {
        return array ? obj.getInstanceName()+"["+element+"]" 
                     : field.name();
    }
     */
    
    /**
     * Gets the value of this field object. 
     * @return an appropriate object. This could be one of:<bl>
     * <li><b>BObject</b> - if the item is an object
     * <li><b>Number</b> - if the item is a Byte, Double, Float, Integer, Long or Short
     * <li><b>Boolean</b>
     * <li><b>Character</b>
     * <li><b>String</b>
     * <li><b>BField[]</b> - if this is an array
     * <code><b>null</b></code> - if the field is null, or if the field does not exist
     * </bl>
     */
    public Object getValue()
    {
        Value val = array ? ((ArrayReference)ref).getValue (element)
                          : ref.getValue (field);
        if (val == null)
        {
            return null;
        }
        else if (val instanceof StringReference)
        {
            return ((StringReference) val).value();
        }
        else if (val instanceof ObjectReference)
        {
            PkgMgrFrame pmf = PkgMgrFrame.findFrame (bluej_pkg);
            return new BObject ( new ObjectWrapper (pmf, pmf.getObjectBench(), JdiObject.getDebuggerObject((ObjectReference)val), getName()));
        }
        else if (val instanceof BooleanValue)
        {
            return new Boolean (((BooleanValue) val).value());
        }
        else if (val instanceof ByteValue)
        {
            return new Byte (((ByteValue) val).value());
        }
        else if (val instanceof CharValue)
        {
            return new Character (((CharValue) val).value());
        }
        else if (val instanceof DoubleValue)
        {
            return new Double (((DoubleValue) val).value());
        }
        else if (val instanceof FloatValue)
        {
            return new Float (((FloatValue) val).value());
        }
        else if (val instanceof IntegerValue)
        {
            return new Integer (((IntegerValue) val).value());
        }
        else if (val instanceof LongValue)
        {
            return new Long (((LongValue) val).value());
        }
        else if (val instanceof ShortValue)
        {
            return new Short (((ShortValue) val).value());
        }
        else
        {
            return val.toString();
        }
    }
    
}