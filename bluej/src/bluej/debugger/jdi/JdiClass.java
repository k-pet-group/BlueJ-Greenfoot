package bluej.debugger.jdi;

import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerObject;
import bluej.utility.Debug;
import bluej.utility.JavaNames;

import java.util.List;
import java.util.ArrayList;

import com.sun.jdi.*;

/**
 *  Represents an class running on the user (remote) machine.
 *
 *@author     Michael Kolling
 *@created    December 26, 2000
 *@version    $Id: JdiClass.java 1059 2001-12-20 13:49:55Z mik $
 */
public class JdiClass extends DebuggerClass
{
    ReferenceType remoteClass;  // the remote class represented
    List staticFields;

    // -- instance methods --

    /**
     *  Create a remote class object.
     *
     *@param  obj  the remote debugger object (Jdi code) this encapsulates.
     */
    public JdiClass(ReferenceType remoteClass)
    {
        this.remoteClass = remoteClass;
        getRemoteFields();
    }


    /**
     *  Return the number of static fields (including inherited fields).
     *
     *@return    The StaticFieldCount value
     */
    public int getStaticFieldCount()
    {
        return staticFields.size();
    }


    /**
     *  Return the name of the static field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The StaticFieldName value
     */
    public String getStaticFieldName(int slot)
    {
        return ((Field)staticFields.get(slot)).name();
    }


    /**
     *  Return the object in static field 'slot'. Slot must exist and
     *  must be of object type.
     *
     *@param  slot  The slot number to be returned
     *@return       the object at slot
     */
    public DebuggerObject getStaticFieldObject(int slot)
    {
        Field field = (Field)staticFields.get(slot);
        ObjectReference val = (ObjectReference) remoteClass.getValue(field);
        return JdiObject.getDebuggerObject(val);
    }

    /**
     *  Return an array of strings with the description of each static field
     *  in the format "<modifier> <type> <name> = <value>".
     *
     *@param  includeModifiers  Description of Parameter
     *@return                   The StaticFields value
     */
    public List getStaticFields(boolean includeModifiers)
    {
        return getFields(includeModifiers);
    }


    /**
     *  Return true if the static field 'slot' is public.
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public boolean staticFieldIsPublic(int slot)
    {
        return ((Field)staticFields.get(slot)).isPublic();
    }


    /**
     *  Return true if the static field 'slot' is an object (and not
     *  a simple type).
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public boolean staticFieldIsObject(int slot)
    {
        Field field = (Field) staticFields.get(slot);
        Value val = remoteClass.getValue(field);
        return (val instanceof ObjectReference);
    }


    /**
     *  Return a list of strings with the description of each field
     *  in the format "<modifier> <type> <name> = <value>".
     */
    private List getFields(boolean includeModifiers)
    {
        List fieldStrings = new ArrayList(staticFields.size());
        List visible = remoteClass.visibleFields();

        for (int i = 0; i < staticFields.size(); i++) {
            Field field = (Field) staticFields.get(i);

            Value val = remoteClass.getValue(field);

            String valString = getValueString(val);
            String fieldString = "";

            if (includeModifiers) {
                if (field.isPrivate()) {
                    fieldString = "private ";
                }
                if (field.isProtected()) {
                    fieldString = "protected ";
                }
                if (field.isPublic()) {
                    fieldString = "public ";
                }
            }

            fieldString += JavaNames.stripPrefix(field.typeName())
                + " " + field.name()
                + " = " + valString;

            if (!visible.contains(field)) {
                fieldString += " (hidden)";
            }
            fieldStrings.add(fieldString);
        }
        return fieldStrings;
    }

    /**
     *  Get the list of fields for this object.
     */
    private void getRemoteFields()
    {
        staticFields = new ArrayList();

        if (remoteClass != null) {
            List allFields = remoteClass.allFields();
            for (int i = 0; i < allFields.size(); i++) {
                Field field = (Field) allFields.get(i);
                if (field.isStatic())
                    staticFields.add(field);
            }
        }
        else {
            Debug.reportError("cannot get fields for remote class");
        }
    }


    /**
     *  Return the value of a field as as string.
     *
     *@param  val  Description of Parameter
     *@return      The ValueString value
     */
    public static String getValueString(Value val)
    {
        if (val == null) {
            return "<null>";
        }
        else if (val instanceof StringReference) {
            return "\"" + ((StringReference) val).value() + "\"";
            // toString should be okay for this as well once the bug is out...
        }
        else if (val instanceof ObjectReference) {
            return "<object reference>";
        }

        // the following should not be necessary but it seems like
        // the 1.3 beta jpda has a bug in the toString() method.
        // revisit this code when 1.3 is released
        else if (val instanceof BooleanValue) {
            return String.valueOf(((BooleanValue) val).value());
        }
        else if (val instanceof ByteValue) {
            return String.valueOf(((ByteValue) val).value());
        }
        else if (val instanceof CharValue) {
            return String.valueOf(((CharValue) val).value());
        }
        else if (val instanceof DoubleValue) {
            return String.valueOf(((DoubleValue) val).value());
        }
        else if (val instanceof FloatValue) {
            return String.valueOf(((FloatValue) val).value());
        }
        else if (val instanceof IntegerValue) {
            return String.valueOf(((IntegerValue) val).value());
        }
        else if (val instanceof LongValue) {
            return String.valueOf(((LongValue) val).value());
        }
        else if (val instanceof ShortValue) {
            return String.valueOf(((ShortValue) val).value());
        }
        else {
            return val.toString();
        }
    }

}
