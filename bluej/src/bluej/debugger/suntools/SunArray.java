package bluej.debugger.suntools;

import bluej.debugger.*;

import sun.tools.debug.RemoteClass;
import sun.tools.debug.RemoteField;
import sun.tools.debug.RemoteObject;
import sun.tools.debug.RemoteArray;
import sun.tools.debug.RemoteString;
import sun.tools.debug.RemoteValue;

import bluej.utility.Utility;
import bluej.utility.Debug;
/**
 ** Represents an array object running on the user (remote) machine.
 **
 ** @version $Id: SunArray.java 86 1999-05-18 02:49:53Z mik $
 ** @author Bruce Quig
 **
 **/

public class SunArray extends SunObject
{
	
    private RemoteArray obj;
    
    protected SunArray(RemoteArray obj)
    {
	this.obj = obj;
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
    public int getFieldCount()
    {

	try {
	    return obj.getElements().length;
	} catch(Exception e) {
	    return 5;
	}
    }


    /**
     * Get the name of the class of this object.
     * @return String representing the Class name.
     */
    public String getClassName()
    {
	String name = null;

	try {
	    name = Utility.stripPackagePrefix(Utility.typeName(obj.getClazz().getName()));
	} catch(Exception e) {
	    // ignore it
	}
	return name;
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
    public String getFieldName(int slot)
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
    public boolean fieldIsObject(int slot)
    {
	try {
	    RemoteValue val = obj.getElement(slot);
	    if(val == null)
		return false;
	    else
		return (val instanceof RemoteObject);
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
	return false;
    }
	
    /**
     * Return true if the object field 'slot' is public.
     *
     * @param slot The slot number to be checked
     */
    public boolean fieldIsPublic(int slot)
    {
	return false;
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
    public DebuggerObject getFieldObject(int slot)
    {
	try {
	    RemoteValue val = obj.getElement(slot);
	    return SunObject.getDebuggerObject((RemoteObject)val);
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
	return new String[0];
    }
	
    /**
     * Return an array of strings with the description of each field in the
     * format "<modifier> <type> <name> = <value>".
     */
    public String[] getFields(boolean includeModifiers)
    {
	String[] fields;
	try {
	    RemoteValue[] remoteValues = obj.getElements();
	    fields = new String[remoteValues.length];

	    // prefetch type of first element instead of calling each time
	    // later on if type is "Object" further type verification is needed
	    // a RemoteValue's typeName() method only returns "Object" all 
	    // Object descendants (except String).  arrayTypeName is preset to "Object"
	    // in case first element is null.
	    String arrayTypeName = "Object";
	    if(remoteValues.length > 0)
		if(remoteValues[0] != null)
		    arrayTypeName = remoteValues[0].typeName();

	    // Base array type to use if array elements are null
	    String baseArrayType = getArrayClassName();

	    String typeName = null; 

	    for(int i = 0; i < remoteValues.length; i++) {
		RemoteValue val = remoteValues[i];
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
		
		// If "Object" is typeName then interrogate further for type
		if(arrayTypeName.equals("Object")) {
		    // if a null element use class name of array
		    if(val == null)
			typeName = baseArrayType;
		    // interrogate the RemoteObject for its exact type
		    else 
			typeName = Utility.stripPackagePrefix(((RemoteObject)val).getClazz().getName());
		}
		else
		    typeName = arrayTypeName;

		fields[i] = typeName + " [" + i + "]" + " = " + valString; 
	    }
	} catch(Exception e) {
	    e.printStackTrace(System.err);
	    fields = new String[0];
	}
		
	return fields;
    }

    /**
     * Get the name of the array class of this object.  
     * Similar to getClassName() but excludes array brackets [].
     * @return String representing the array Class name.
     */
    private String getArrayClassName()
    {
	String name = null;
	name = getClassName();
	if(name != null) {
	    int index = name.indexOf("[");
	    name = name.substring(0, index);
	}
	return name;
    }    


}
