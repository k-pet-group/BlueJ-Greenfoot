package bluej.debugger.suntools;

import bluej.debugger.*;

import sun.tools.debug.RemoteClass;
import sun.tools.debug.RemoteField;
import sun.tools.debug.RemoteObject;
import sun.tools.debug.RemoteString;
import sun.tools.debug.RemoteValue;
import sun.tools.debug.RemoteArray;

import bluej.utility.Utility;
import bluej.utility.Debug;

/**
 ** @version $Id: SunObject.java 93 1999-05-28 00:54:37Z mik $
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** Represents an object running on the user (remote) machine.
 **/

public class SunObject extends DebuggerObject
{
    RemoteObject obj;

    /**
     * Factory method that returns instances of SunObjects.
     * @param obj the remote debugger object (Sun code) this encapsulates.
     * @return a new SunObject or a new SunArray object if remote object is 
     *         an array
     */	
    public static SunObject getDebuggerObject(RemoteObject obj)
    {
	if(obj instanceof RemoteArray) {
      	    return new SunArray((RemoteArray)obj);
	}	
	else {
	    return new SunObject(obj);
	}
    }
	

    // -- instance methods --

    protected SunObject(){}

    /**
     * Constructor is private so that instances need to use getSunObject factory method.
     * @param obj the remote debugger object (Sun code) this encapsulates.
     */	
    private SunObject(RemoteObject obj)
    {
	this.obj = obj;
    }

    /**
     * Get the name of the class of this object.
     */
    public String getClassName()
    {
	String name = null;

	try {
	    name = Utility.typeName(obj.getClazz().getName());
	} catch(Exception e) {
	    // ignore it
	}
	return name;
    }
	
    /**
     * Return true if this object is an array.
     */
    public boolean isArray()
    {
	String name = null;

	try {
	    name = obj.getClazz().getName();
	    return (name.charAt(0) == '[');
	} catch(Exception e) {
	    // ignore it
	}
	return false;
    }

	
    /**
     * Return the number of static fields.
     */
    public int getStaticFieldCount()
    {
	try {
	    RemoteClass rclass = obj.getClazz();
	    return rclass.getFields().length;
	} catch(Exception e) {
	    return 5;
	}
    }

    /**
     * Return the number of object fields.
     */
    public int getInstanceFieldCount()
    {
	try {
	    return obj.getFields().length;
	} catch(Exception e) {
	    return 5;
	}
    }

    /**
     * Return the name of the static field at 'slot'.
     *
     * @arg slot  The slot number to be checked
     */
    public String getStaticFieldName(int slot)
    {
	try {
	    RemoteClass rclass = obj.getClazz();
	    return rclass.getField(slot).getName();
	} catch(Exception e) {
	    return "field";
	}
    }

    /**
     * Return the name of the object field at 'slot'.
     *
     * @arg slot  The slot number to be checked
     */
    public String getInstanceFieldName(int slot)
    {
	try {
	    return obj.getField(slot).getName();
	} catch(Exception e) {
	    return "field";
	}
    }


    /**
     * Return true if the static field 'slot' is an object (and not
     * a simple type).
     *
     * @arg slot The slot number to be checked
     */
    public boolean staticFieldIsObject(int slot)
    {
	try {
	    RemoteValue val = obj.getClazz().getFieldValue(slot);
	    if(val == null)
		return false;
	    else
		return (val instanceof RemoteObject);
	} catch(Exception e) {
	    return false;
	}
    }
	
    /**
     * Return true if the object field 'slot' is an object (and not
     * a simple type).
     *
     * @arg slot The slot number to be checked
     */
    public boolean instanceFieldIsObject(int slot)
    {
	try {
	    RemoteValue val = obj.getFieldValue(slot);
	    if(val == null)
		return false;
	    else {
		return (val instanceof RemoteObject);
	    }
	} catch(Exception e) {
	    return false;
	}
    }
	
    /**
     * Return true if the static field 'slot' is public.
     *
     * @arg slot The slot number to be checked
     */
    public boolean staticFieldIsPublic(int slot)
    {
	try {
	    RemoteField field = obj.getClazz().getField(slot);
	    return (field.getModifiers().indexOf("public") != -1);
	} catch(Exception e) {
	    return false;
	}
    }
	
    /**
     * Return true if the object field 'slot' is public.
     *
     * @arg slot The slot number to be checked
     */
    public boolean instanceFieldIsPublic(int slot)
    {
	try {
	    RemoteField field = obj.getField(slot);
	    return (field.getModifiers().indexOf("public") != -1);
	} catch(Exception e) {
	    return false;
	}
    }
	
    /**
     * Return the object in static field 'slot'.
     *
     * @param slot  The slot number to be returned
     * @return the object at slot or null if slot does not exist
     */
    public DebuggerObject getStaticFieldObject(int slot)
    {
	try {
	    RemoteValue val = obj.getClazz().getFieldValue(slot);
	    return new SunObject((RemoteObject)val);
	} catch(Exception e) {
	    return null;
	}
    }
	
    /**
     * Return the object in object field 'slot'.
     *
     * @arg slot  The slot number to be returned
     */
    public DebuggerObject getInstanceFieldObject(int slot)
    {
	try {
	    RemoteValue val = obj.getFieldValue(slot);
	    return getDebuggerObject((RemoteObject)val); 
	} catch(Exception e) {
	    return null;
	}
    }


    /**
     * Return an array of strings with the description of each static field
     * in the format "<modifier> <type> <name> = <value>".
     */
    public String[] getStaticFields(boolean includeModifiers)
    {
	Vector fields;
		
	try {
	    RemoteClass rclass = obj.getClazz();

	    RemoteField[] rfields = rclass.getFields();

	    fields = new Vector(rfields.length);

	    for(int i = 0; i < rfields.length; i++) {
		RemoteValue val = rclass.getFieldValue(i);
		String valString;
				
		if(val instanceof RemoteString)
		    valString = "\"" + val.toString() + "\"";
		else if(val instanceof RemoteObject)
		    valString = "<object reference>";
		else
		    valString = val.toString();
				
		String fieldString;
		if(includeModifiers)
		    fieldString = rfields[i].getModifiers();
		else
		    fieldString = "";

		fieldString = fieldString + rfields[i].getTypedName() + 
		            " = " + valString;
		fields.add(fieldString);
	    }
	} catch(Exception e) {
	    e.printStackTrace(System.err);
	    fields = new Vector();
	}
	return fields;
    }
	
    /**
     * Return an array of strings with the description of each field in the
     * format "<modifier> <type> <name> = <value>".
     */
    public Vector getInstanceFields(boolean includeModifiers)
    {
	Vector fields;
	try {
	    RemoteField[] rfields = obj.getFields();
	    fields = new Vector(rfields.length);
			
	    for(int i = 0; i < rfields.length; i++) {
		RemoteValue val = obj.getFieldValue(i);
		String valString;
				
		if(val == null)
		    valString = "<null>";
		else if(val.isString()) {
		    // Horrible special case:
		    if("null".equals(val.toString()))
		    	valString = "\"\"";
		    else
			valString = "\"" + val.toString() + "\"";
		}
		else if(val instanceof RemoteObject)
		    valString = "<object reference>";
		else
		    valString = val.toString();

		String fieldString;
		if(includeModifiers)
		    fieldString = rfields[i].getModifiers();
		else
		    fieldString = "";
		fieldString = fieldString + rfields[i].getTypedName() + 
		            " = " + valString;
		fields.add(fieldString);
	    }
	} catch(Exception e) {
	    e.printStackTrace(System.err);
	    fields = new Vector();
	}
		
	return fields;
    }
	
}
