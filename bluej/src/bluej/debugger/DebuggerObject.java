package bluej.debugger;

import java.util.Vector;

/**
 * A class representing an object in the debugged VM.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @version $Id: DebuggerObject.java 334 2000-01-02 13:32:56Z ajp $
 */
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
    public abstract int getInstanceFieldCount();

    /**
     * Return the name of the static field at 'slot'.
     *
     * @param slot  The slot number to be checked
     */
    public abstract String getStaticFieldName(int slot);

    /**
     * Return the name of the object field at 'slot'.
     *
     * @param slot  The slot number to be checked
     */
    public abstract String getInstanceFieldName(int slot);


    /**
     * Return true if the static field 'slot' is public.
     *
     * @param slot The slot number to be checked
     */
    public abstract boolean staticFieldIsPublic(int slot);

    /**
     * Return true if the object field 'slot' is public.
     *
     * @param slot The slot number to be checked
     */
    public abstract boolean instanceFieldIsPublic(int slot);


    /**
     * Return true if the static field 'slot' is an object (and not
     * a simple type).
     *
     * @param slot The slot number to be checked
     */
    public abstract boolean staticFieldIsObject(int slot);

    /**
     * Return true if the object field 'slot' is an object (and not
     * a simple type).
     *
     * @param slot The slot number to be checked
     */
    public abstract boolean instanceFieldIsObject(int slot);

    /**
     * Return true if the field 'slot' is an object (and not
     * a simple type). Includes static and instance fields.
     *
     * @param slot The slot number to be checked
     */
    public abstract boolean fieldIsObject(int slot);


    /**
     * Return the object in static field 'slot'.
     *
     * @param slot  The slot number to be returned
     */
    public abstract DebuggerObject getStaticFieldObject(int slot);

    /**
     * Return the object in object field 'slot'.
     *
     * @param slot  The slot number to be returned
     */
    public abstract DebuggerObject getInstanceFieldObject(int slot);

    /**
     * Return the object in field 'slot' (counting static and object fields).
     *
     * @param slot  The slot number to be returned
     */
    public abstract DebuggerObject getFieldObject(int slot);


    /**
     * Return an array of strings with the description of each static field
     * in the format "<modifier> <type> <name> = <value>".
     */
    public abstract Vector getStaticFields(boolean includeModifiers);

    /**
     * Return an array of strings with the description of each instance field
     * in the format "<modifier> <type> <name> = <value>" or
     * "<type> <name> = <value>", depending on the parameter.
     */
    public abstract Vector getInstanceFields(boolean includeModifiers);

    /**
     * Return an array of strings with the description of each field
     * (including static and instance) in the
     * format "<modifier> <type> <name> = <value>" or
     * "<type> <name> = <value>", depending on the parameter.
     */
    public abstract Vector getAllFields(boolean includeModifiers);

}
