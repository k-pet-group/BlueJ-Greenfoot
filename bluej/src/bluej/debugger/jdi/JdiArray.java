package bluej.debugger.jdi;

import bluej.debugger.DebuggerObject;

import bluej.utility.Debug;

import java.util.List;
import java.util.ArrayList;

import com.sun.jdi.*;

/**
 * Represents an array object running on the user (remote) machine.
 *
 * @author  Michael Kolling
 * @version $Id: JdiArray.java 589 2000-06-28 04:31:40Z mik $
 */
public class JdiArray extends JdiObject
{

    private ArrayReference obj;

    protected JdiArray(ArrayReference obj)
    {
	this.obj = obj;
    }


    /**
     * Get the name of the class of this object.
     * @return String representing the Class name.
     */
    public String getClassName()
    {
	return obj.referenceType().name();
    }

    /**
     * Return true if this object is an array.
     */
    public boolean isArray()
    {
	return true;
    }


    /**
     * Return the number of static fields.
     */
    public int getStaticFieldCount()
    {
	return 0;
    }

    /**
     * Return the number of object fields.
     */
    public int getInstanceFieldCount()
    {
	return obj.length();
    }


    /**
     * Return the name of the static field at 'slot'.
     *
     * @param slot  The slot number to be checked
     */
    public String getStaticFieldName(int slot)
    {
	return "field";
    }

    /**
     * Return the name of the object field at 'slot'.
     *
     * @param slot  The slot number to be checked
     */
    public String getInstanceFieldName(int slot)
    {
	return "[...]";
    }


    /**
     * Return true if the static field 'slot' is public.
     *
     * @arg slot The slot number to be checked
     */
    public boolean staticFieldIsPublic(int slot)
    {
	return false;
    }

    /**
     * Return true if the object field 'slot' is public.
     *
     * @param slot The slot number to be checked
     */
    public boolean instanceFieldIsPublic(int slot)
    {
	return false;
    }


    /**
     * Return true if the static field 'slot' is an object (and not
     * a simple type).
     *
     * @param slot The slot number to be checked
     */
    public boolean staticFieldIsObject(int slot)
    {
	return false;
    }

    /**
     * Return true if the object field 'slot' is an object (and not
     * a simple type).
     *
     * @param slot The slot number to be checked
     */
    public boolean instanceFieldIsObject(int slot)
    {
	Value val = obj.getValue(slot);
	if(val == null)
	    return false;
	else
	    return (val instanceof ObjectReference);
    }

    /**
     * Return the object in static field 'slot'.
     *
     * @param slot  The slot number to be returned
     * @return the object at slot or null if slot does not exist
     */
    public DebuggerObject getStaticFieldObject(int slot)
    {
	return null;
    }

    /**
     * Return the object in object field 'slot'.
     *
     * @param slot  The slot number to be returned
     */
    public DebuggerObject getInstanceFieldObject(int slot)
    {
  	Value val = obj.getValue(slot);
	return JdiObject.getDebuggerObject((ObjectReference)val);
    }


    /**
     * Return an array of strings with the description of each static field
     * in the format "<modifier> <type> <name> = <value>".
     */
    public List getStaticFields(boolean includeModifiers)
    {
	return new ArrayList(0);
    }

    /**
     * Return an array of strings with the description of each field in the
     * format "<modifier> <type> <name> = <value>".
     */
    public List getInstanceFields(boolean includeModifiers)
    {
	List values = obj.getValues();
	List fields = new ArrayList(values.size());

	String typeName = null;

	for(int i = 0; i < values.size(); i++) {
	    Value val = (Value)values.get(i);
	    String valString;

	    if(val == null)
		valString = "<null>";
	    else if((val instanceof ObjectReference) &&
		    !(val instanceof StringReference))
		valString = "<object reference>";
	    else
		valString = val.toString();

	    fields.add("[" + i + "]" + " = " + valString);
	}
	return fields;
    }

}
