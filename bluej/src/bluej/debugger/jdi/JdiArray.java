package bluej.debugger.jdi;

import bluej.debugger.DebuggerObject;

import bluej.utility.Debug;

import java.util.Vector;
import com.sun.jdi.*;

/**
 ** Represents an array object running on the user (remote) machine.
 **
 ** @author Michael Kolling
 **
 **/

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
	String name = null;

	Debug.message("[JdiArray] getClassName - NYI");
	//    name = Utility.stripPackagePrefix(Utility.typeName(obj.getClazz().getName()));
	return name;
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
	Debug.message("[JdiArray] getFieldCount - NYI");
	return 0;
	//return obj.getElements().length;
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
	Debug.message("[JdiArray] getInstanceFieldName - NYI");
	//return obj.getField(slot).getName();
	return "field";
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
	Debug.message("[JdiArray] instanceFieldIsObject - NYI");

//  	    RemoteValue val = obj.getElement(slot);
//  	    if(val == null)
//  		return false;
//  	    else
//  		return (val instanceof RemoteObject);
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
    public DebuggerObject getInstanceFieldObject(int slot)
    {
	Debug.message("[JdiArray] getInstanceFieldObject - NYI");
//  	    RemoteValue val = obj.getElement(slot);
//  	    return JdiObject.getDebuggerObject((RemoteObject)val);
	return null;
    }


    /**
     * Return an array of strings with the description of each static field
     * in the format "<modifier> <type> <name> = <value>".
     */
    public Vector getStaticFields(boolean includeModifiers)
    {
	return new Vector();
    }
	
    /**
     * Return an array of strings with the description of each field in the
     * format "<modifier> <type> <name> = <value>".
     */
    public Vector getInstanceFields(boolean includeModifiers)
    {
	Debug.message("[JdiArray] getInstanceFields - NYI");

  	Vector fields;

//  	try {
//  	    RemoteValue[] remoteValues = obj.getElements();
//  	    fields = new String[remoteValues.length];

//  	    // prefetch type of first element instead of calling each time
//  	    // later on if type is "Object" further type verification is needed
//  	    // a RemoteValue's typeName() method only returns "Object" all 
//  	    // Object descendants (except String).  arrayTypeName is preset to "Object"
//  	    // in case first element is null.
//  	    String arrayTypeName = "Object";
//  	    if(remoteValues.length > 0)
//  		if(remoteValues[0] != null)
//  		    arrayTypeName = remoteValues[0].typeName();

//  	    // Base array type to use if array elements are null
//  	    String baseArrayType = getArrayClassName();

//  	    String typeName = null; 

//  	    for(int i = 0; i < remoteValues.length; i++) {
//  		RemoteValue val = remoteValues[i];
//  		String valString;
				
//  		if(val == null)
//  		    valString = "<null>";
//  		else if(val.isString()) {
//  		    // Horrible special case:
//  		    if("null".equals(val.toString()))
//  		    	valString = "\"\"";
//  		    else
//  			valString = "\"" + val.toString() + "\"";
//  		}
//  		else if(val instanceof RemoteObject)
//  		    valString = "<object reference>";
//  		else
//  		    valString = val.toString();
		
//  		// If "Object" is typeName then interrogate further for type
//  		if(arrayTypeName.equals("Object")) {
//  		    // if a null element use class name of array
//  		    if(val == null)
//  			typeName = baseArrayType;
//  		    // interrogate the RemoteObject for its exact type
//  		    else 
//  			typeName = Utility.stripPackagePrefix(((RemoteObject)val).getClazz().getName());
//  		}
//  		else
//  		    typeName = arrayTypeName;

//  		fields[i] = typeName + " [" + i + "]" + " = " + valString; 
//  	    }

	fields = new Vector();
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
