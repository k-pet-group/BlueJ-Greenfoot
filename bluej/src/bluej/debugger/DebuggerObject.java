package bluej.debugger;

import bluej.debugger.suntools.SunObject;
import sun.tools.debug.RemoteObject;

/**
 ** A class representing an object in the debugged VM.
 **
 ** @author Michael Cahill
 ** @author Michael Kolling
 **
 ** @version $Id: DebuggerObject.java 86 1999-05-18 02:49:53Z mik $
 **/

public abstract class DebuggerObject
{
   
    /**
     * Get the name of the class of this object.
     */
    public abstract String getClassName();

    /**
     * Return true if this object is an array.
     */
    public abstract boolean isArray();

    /**
     * Return the number of static fields.
     */
    public abstract int getStaticFieldCount();

    /**
     * Return the number of object fields.
     */
    public abstract int getFieldCount();

    /**
     * Return the name of the static field at 'slot'.
     *
     * @arg slot  The slot number to be checked
     */
    public abstract String getStaticFieldName(int slot);

    /**
     * Return the name of the object field at 'slot'.
     *
     * @arg slot  The slot number to be checked
     */
    public abstract String getFieldName(int slot);


    /**
     * Return true if the static field 'slot' is an object (and not
     * a simple type).
     *
     * @arg slot The slot number to be checked
     */
    public abstract boolean staticFieldIsObject(int slot);

    /**
     * Return true if the object field 'slot' is an object (and not
     * a simple type).
     *
     * @arg slot The slot number to be checked
     */
    public abstract boolean fieldIsObject(int slot);
	

    /**
     * Return true if the static field 'slot' is public.
     *
     * @arg slot The slot number to be checked
     */
    public abstract boolean staticFieldIsPublic(int slot);

    /**
     * Return true if the object field 'slot' is public.
     *
     * @arg slot The slot number to be checked
     */
    public abstract boolean fieldIsPublic(int slot);
	

    /**
     * Return the object in static field 'slot'.
     *
     * @arg slot  The slot number to be returned
     */
    public abstract DebuggerObject getStaticFieldObject(int slot);

    /**
     * Return the object in object field 'slot'.
     *
     * @arg slot  The slot number to be returned
     */
    public abstract DebuggerObject getFieldObject(int slot);


    /**
     * Return an array of strings with the description of each static field
     * in the format "<modifier> <type> <name> = <value>".
     */
    public abstract String[] getStaticFields(boolean includeModifiers);

    /**
     * Return an array of strings with the description of each field in the
     * format "<modifier> <type> <name> = <value>" or
     * "<type> <name> = <value>", depending on the parameter.
     */
    public abstract String[] getFields(boolean includeModifiers);

}
