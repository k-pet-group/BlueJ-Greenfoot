package bluej.debugger.jdi;

import bluej.debugger.DebuggerObject;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.JavaNames;

import java.util.List;
import java.util.ArrayList;

import com.sun.jdi.*;

/**
 * Represents an object running on the user (remote) machine.
 *
 * @author  Michael Kolling
 * @version $Id: JdiObject.java 589 2000-06-28 04:31:40Z mik $
 */
public class JdiObject extends DebuggerObject
{
    ObjectReference obj;    // the remote object represented
    List fields;            // list of fields of the object

    /**
     * Factory method that returns instances of JdiObjects.
     *
     * @param obj   the remote object this encapsulates.
     * @return      a new JdiObject or a new JdiArray object if
     *              remote object is an array
     */
    public static JdiObject getDebuggerObject(ObjectReference obj)
    {
        if(obj instanceof ArrayReference)
            return new JdiArray((ArrayReference)obj);
        else
            return new JdiObject(obj);
    }


    // -- instance methods --

    protected JdiObject() {}

    /**
     * Constructor is private so that instances need to use getJdiObject
     * factory method.
     *
     * @param obj the remote debugger object (Jdi code) this encapsulates.
     */
    private JdiObject(ObjectReference obj)
    {
        this.obj = obj;
        getRemoteFields();
    }


    /**
     * Get the name of the class of this object.
     */
    public String getClassName()
    {
        return obj.referenceType().name();
    }

    /**
     * Return true if this object is an array. This is always false, since
     * arrays are wropped in the subclass "JdiArray".
     */
    public boolean isArray()
    {
        return false;
    }


    /**
     * Return the number of static fields (including inherited fields).
     */
    public int getStaticFieldCount()
    {
        return getFieldCount(true);
    }

    /**
     * Return the number of object fields.
     */
    public int getInstanceFieldCount()
    {
        return getFieldCount(false);
    }

    private int getFieldCount(boolean getStatic)
    {
        int count = 0;

        for (int i = 0; i < fields.size(); i++) {
            Field field = (Field)fields.get(i);
            if(field.isStatic() == getStatic)
                count++;
        }
        return count;
    }


    /**
     * Return the name of the static field at 'slot'.
     *
     * @param slot  The slot number to be checked
     */
    public String getStaticFieldName(int slot)
    {
        return getField(true, slot).name();
    }

    /**
     * Return the name of the object field at 'slot'.
     *
     * @param slot  The slot number to be checked
     */
    public String getInstanceFieldName(int slot)
    {
        return getField(false, slot).name();
    }


    /**
     * Return true if the static field 'slot' is public.
     *
     * @param slot The slot number to be checked
     */
    public boolean staticFieldIsPublic(int slot)
    {
        return getField(true, slot).isPublic();
    }

    /**
     * Return true if the object field 'slot' is public.
     *
     * @param slot The slot number to be checked
     */
    public boolean instanceFieldIsPublic(int slot)
    {
        return getField(false, slot).isPublic();
    }


    /**
     * Return true if the static field 'slot' is an object (and not
     * a simple type).
     *
     * @param slot The slot number to be checked
     */
    public boolean staticFieldIsObject(int slot)
    {
        return checkFieldForObject(true, slot);
    }

    /**
     * Return true if the object field 'slot' is an object (and not
     * a simple type).
     *
     * @param slot The slot number to be checked
     */
    public boolean instanceFieldIsObject(int slot)
    {
        return checkFieldForObject(false, slot);
    }

    private boolean checkFieldForObject(boolean getStatic, int slot)
    {
        Field field = getField(getStatic, slot);
        Value val = obj.getValue(field);
        return (val instanceof ObjectReference);
    }

    /**
     * Return true if the object field 'slot' is an object (and not
     * a simple type).
     *
     * @param slot The slot number to be checked
     */
    public boolean fieldIsObject(int slot)
    {
        Field field = (Field)fields.get(slot);
        Value val = obj.getValue(field);
        return (val instanceof ObjectReference);
    }


    /**
     * Return the object in static field 'slot'. Slot must exist and
     * must be of object type.
     *
     * @param slot  The slot number to be returned
     * @return the object at slot
     */
    public DebuggerObject getStaticFieldObject(int slot)
    {
        Field field = getField(true, slot);
        ObjectReference val = (ObjectReference)obj.getValue(field);
        return getDebuggerObject(val);
    }

    /**
     * Return the object in object field 'slot'. Slot must exist and
     * must be of object type.
     *
     * @param slot  The slot number to be returned
     */
    public DebuggerObject getInstanceFieldObject(int slot)
    {
        Field field = getField(false, slot);
        ObjectReference val = (ObjectReference)obj.getValue(field);
        return getDebuggerObject(val);
    }

    /**
     * Return the object in field 'slot'. Slot must exist and
     * must be of object type.
     *
     * @param slot  The slot number to be returned
     */
    public DebuggerObject getFieldObject(int slot)
    {
        Field field = (Field)fields.get(slot);
        ObjectReference val = (ObjectReference)obj.getValue(field);
        return getDebuggerObject(val);
    }


    /**
     * Return an array of strings with the description of each static field
     * in the format "<modifier> <type> <name> = <value>".
     */
    public List getStaticFields(boolean includeModifiers)
    {
        return getFields(false, true, includeModifiers);
    }

    /**
     * Return a list of strings with the description of each instance field
     * in the format "<modifier> <type> <name> = <value>".
     */
    public List getInstanceFields(boolean includeModifiers)
    {
        return getFields(false, false, includeModifiers);
    }


    /**
     * Return a list of strings with the description of each field
     * in the format "<modifier> <type> <name> = <value>".
     */
    public List getAllFields(boolean includeModifiers)
    {
        return getFields(true, true, includeModifiers);
    }


    /**
     * Return a list of strings with the description of each field
     * in the format "<modifier> <type> <name> = <value>".
     * If 'getAll' is true, both static and instance fields are returned
     * ('getStatic' is ignored). If 'getAll' is false, then 'getStatic'
     * determines whether static fields or instance fields are returned.
     */
    private List getFields(boolean getAll, boolean getStatic,
                             boolean includeModifiers)
    {
        List fieldStrings = new ArrayList(fields.size());

        ReferenceType cls = obj.referenceType();
        List visible = cls.visibleFields();

        for (int i = 0; i < fields.size(); i++) {
            Field field = (Field)fields.get(i);

            if(getAll || (field.isStatic() == getStatic)) {
                Value val = obj.getValue(field);

                String valString = getValueString(val);
                String fieldString = "";

                if(includeModifiers) {
                    if(field.isPrivate())
                        fieldString  = "private ";
                    if(field.isProtected())
                        fieldString = "protected ";
                    if(field.isPublic())
                        fieldString = "public ";
                }

                fieldString += JavaNames.stripPrefix(field.typeName())
                    + " " + field.name()
                    + " = " + valString;

        		if (!visible.contains(field)) {
                    fieldString += " (hidden)";
        		}
                // the following code adds the word "inherited" to inherited
                // fields - currently unused
                //else if (!field.declaringType().equals(cls)) {
                //    fieldString += " (inherited)";
                //}
                fieldStrings.add(fieldString);
            }
        }
        return fieldStrings;
    }

    private Field getField(boolean getStatic, int slot)
    {
        for (int i = 0; i < fields.size(); i++) {
            Field field = (Field)fields.get(i);
            if(field.isStatic() == getStatic) {
                if(slot == 0)
                    return field;
                else
                    slot--;
            }
        }
        Debug.reportError("invalid slot in remote object");
        return null;
    }

    /**
     * Get the list of fields for this object.
     */
    private void getRemoteFields()
    {
        ReferenceType cls = obj.referenceType();
        if (cls != null)
            fields = cls.allFields();
        else {
            Debug.reportError("cannot get class for remote object");
            fields = new ArrayList();
        }
    }

    /**
     *  Return the value of a field as as string.
     */
    public static String getValueString(Value val)
    {
        if (val == null)
            return "<null>";
        else if (val instanceof StringReference)
            {
                return "\"" + ((StringReference)val).value() + "\"";
                // toString should be okay for this as well once the bug is out...
            }
        else if (val instanceof ObjectReference)
            {
                return "<object reference>";
            }

        // the following should not be necessary but it seems like
        // the 1.3 beta jpda has a bug in the toString() method.
        // revisit this code when 1.3 is released
        else if (val instanceof BooleanValue)
            {
                return String.valueOf(((BooleanValue)val).value());
            }
        else if (val instanceof ByteValue)
            {
                return String.valueOf(((ByteValue)val).value());
            }
        else if (val instanceof CharValue)
            {
                return String.valueOf(((CharValue)val).value());
            }
        else if (val instanceof DoubleValue)
            {
                return String.valueOf(((DoubleValue)val).value());
            }
        else if (val instanceof FloatValue)
            {
                return String.valueOf(((FloatValue)val).value());
            }
        else if (val instanceof IntegerValue)
            {
                return String.valueOf(((IntegerValue)val).value());
            }
        else if (val instanceof LongValue)
            {
                return String.valueOf(((LongValue)val).value());
            }
        else if (val instanceof ShortValue)
            {
                return String.valueOf(((ShortValue)val).value());
            }
        else
            return val.toString();
    }
}
