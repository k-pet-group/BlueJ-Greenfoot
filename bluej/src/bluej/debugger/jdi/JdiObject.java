package bluej.debugger.jdi;

import bluej.debugger.DebuggerObject;
import bluej.utility.Debug;
import bluej.utility.Utility;

import java.util.List;
import java.util.Vector;

import com.sun.jdi.*;

/**
 ** Represents an object running on the user (remote) machine.
 **
 ** @author Michael Kolling
 **/

public class JdiObject extends DebuggerObject
{
    ObjectReference obj;    // the remote object represented
    List fields;            // list of fields of the object

    /**
     * Factory method that returns instances of JdiObjects.
     *
     * @param obj	the remote object this encapsulates.
     * @return		a new JdiObject or a new JdiArray object if 
     *			remote object is an array
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
    public Vector getStaticFields(boolean includeModifiers)
    {
        return getFields(false, true, includeModifiers);
    }
	
    /**
     * Return a vector of strings with the description of each instance field
     * in the format "<modifier> <type> <name> = <value>".
     */
    public Vector getInstanceFields(boolean includeModifiers)
    {
        return getFields(false, false, includeModifiers);
    }


    /**
     * Return a vector of strings with the description of each field
     * in the format "<modifier> <type> <name> = <value>".
     */
    public Vector getAllFields(boolean includeModifiers)
    {
	return getFields(true, true, includeModifiers);
    }


    /**
     * Return a vector of strings with the description of each field
     * in the format "<modifier> <type> <name> = <value>".
     * If 'getAll' is true, both static and instance fields are returned
     * ('getStatic' is ignored). If 'getAll' is false, then 'getStatic'
     * determines whether static fields or instance fields are returned.
     */
    private Vector getFields(boolean getAll, boolean getStatic, 
                                boolean includeModifiers)
    {
        Vector fieldStrings = new Vector(fields.size());

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

                fieldString += Utility.stripPackagePrefix(field.typeName())
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
	    fields = new Vector();
	}
    }

    /**
     *  Return the value of a field as as string.
     */
    public static String getValueString(Value val)
    {
        if(val == null)
            return "<null>";
        else if((val instanceof ObjectReference) &&
                    !(val instanceof StringReference))
        {
            return "<object reference>";
        }
        else
            return val.toString();
    }
}
