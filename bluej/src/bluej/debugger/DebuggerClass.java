package bluej.debugger;

import java.util.List;

/**
 *  A class representing an object in the debugged VM.
 *
 *@author     Michael Kolling
 *@version    $Id: DebuggerClass.java 2830 2004-08-03 09:26:06Z polle $
 */
public abstract class DebuggerClass
{
    /**
     *  Return the name of this class (fully qualified).
     *
     *@return    The class name
     */
    public abstract String getName();

    /**
     *  Return the number of static fields.
     *
     *@return    The StaticFieldCount value
     */
    public abstract int getStaticFieldCount();

    /**
     *  Return the name of the static field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The StaticFieldName value
     */
    public abstract String getStaticFieldName(int slot);

    /**
     *  Return the object in static field 'slot'.
     *
     *@param  slot  The slot number to be returned
     *@return       The StaticFieldObject value
     */
    public abstract DebuggerObject getStaticFieldObject(int slot);

    /**
     *  Return a list of strings with the description of each static field
     *  in the format "<modifier> <type> <name> = <value>".
     *
     *@param  includeModifiers  Whether to include modifiers (private,etc.)
     *@return                   The StaticFields value
     */
    public abstract List getStaticFields(boolean includeModifiers);

    /**
     *  Return true if the static field 'slot' is public.
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public abstract boolean staticFieldIsPublic(int slot);

    /**
     *  Return true if the static field 'slot' is an object (and not
     *  a simple type).
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public abstract boolean staticFieldIsObject(int slot);
    
    /**
     * Returns true if this represents a Java interface
     * 
     */
    public abstract boolean isInterface();

    /**
     * Returns true if this represents an enum
     * 
     */
    public abstract boolean isEnum();

}
