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

/**
 * The BlueJ proxy Field object. This represents a field of a class or object.
 *
 * @author Clive Miller
 * @version $Id: BField.java 1459 2002-10-23 12:13:12Z jckm $
 * @see bluej.extensions.BObject#getField(java.lang.String)
 * @see bluej.extensions.BObject#getFields(boolean)
 * @see bluej.extensions.BClass#getStaticField(java.lang.String)
 * @see bluej.extensions.BClass#getStaticFields()
 */
public class BField
{
    private final BPackage pkg;
    private final ObjectReference ref;
    private final Field field;
    private final BObject obj;
    private final boolean array;
    private final int element;
   
    BField (BPackage pkg, ObjectReference ref, Field field)
    {
        this.pkg = pkg;
        this.ref = ref;
        this.field = field;
        this.array = false;
        this.element = 0;
        this.obj = null;
    }
    
    /**
     * Constructor for an array
     */
    BField (BPackage pkg, ArrayReference ref, BObject obj, int element)
    {
        this.pkg = pkg;
        this.ref = ref;
        this.field = null;
        this.obj = obj;
        this.array = true;
        this.element = element;
    }
    
    /**
     * Gets the name of the type of this field object.
     * @return the name of the type of the value held in this field. For example,
     * <bl>
     *    <li>int
     *    <li>float
     * </bl>
     */
    public String getTypeName()
    {
        String type = field == null ? obj.getType().getArrayType().getName()
                                    : field.typeName();
        if (type.equals("java.lang.String")) type = "String"; // don't ask me why!
        return type;
    }

    /**
     * Gets the name of the field
     * @return a String containing the fully-qualified type name
     */
    public String getName()
    {
        return array ? obj.getName()+"["+element+"]" 
                     : field.name();
    }
    
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
            return new BObject (pkg, new ObjectWrapper (PkgMgrFrame.findFrame (pkg.getRealPackage()), JdiObject.getDebuggerObject((ObjectReference)val), getName()), getName());
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
    
    /**
     * Determines the modifiers for this method.
     * Use <code>java.lang.reflect.Modifiers</code> to
     * decode the meaning of this integer
     * @return The modifiers of this method, encoded in
     * a standard Java language integer. If this field
     * is an array element, this value will probably
     * be meaningless.
     */
    public int getModifiers()
    {
        return array ? obj.getType().getModifiers()
                     : field.modifiers();
    }

    /**
     * Gets a description of this field
     * @return the type, name and value of the field
     */
    public String toString()
    {
        String mod = Modifier.toString (getModifiers());
        if (mod.length() > 0) mod += " ";
        return mod + getTypeName()+": "+getName()+"="+getValue();
    }
}