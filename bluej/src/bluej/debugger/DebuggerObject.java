package bluej.debugger;

import java.util.List;

import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.GenTypeClass;

/**
 *  A class representing an object in the debugged VM.
 *
 *@author     Michael Kolling
 *@version    $Id: DebuggerObject.java 3463 2005-07-13 01:55:27Z davmac $
 */
public abstract class DebuggerObject
{
    public static final String OBJECT_REFERENCE = "<object reference>";
    
    /**
     *  Get the fully qualified name of the class of this object.
     *
     * @return  the fully qualified class name
     */
    public abstract String getClassName();
    
    /**
     *  Get the name of the class of this object, including generic parameters
     * 
     * @return  the fully qualified class name
     */
    public abstract String getGenClassName();

    /**
     *  Get the name of the class, including generic parameters, with package
     *  prefixes stripped in both the raw name and parameter names.
     * 
     * @return  the stripped class name
     */
    public abstract String getStrippedGenClassName();
    
    /**
     *  Get the class of this object.
     *
     *  @return    The class object.
     */
    public abstract DebuggerClass getClassRef();
    
    /**
     *  Get the complete generic type of this object.
     * 
     *  @return    The object type.
     */
    public abstract GenTypeClass getGenType();

    /**
     *  Return true if this object is an array.
     *
     *@return    The Array value
     */
    public abstract boolean isArray();

    /**
     * Return true if this object has a null value
     */
    public abstract boolean isNullObject();

    /**
     *  Return the number of static fields.
     *
     *@return    The StaticFieldCount value
     */
    public abstract int getStaticFieldCount();

    /**
     *  Return the number of object fields.
     *
     *@return    The InstanceFieldCount value
     */
    public abstract int getInstanceFieldCount();

    /**
     *  Return the name of the static field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The StaticFieldName value
     */
    public abstract String getStaticFieldName(int slot);

    /**
     *  Return the name of the object field at 'slot'.
     *
     *@param  slot  The slot number to be checked
     *@return       The InstanceFieldName value
     */
    public abstract String getInstanceFieldName(int slot);


    /**
     *  Return the object in static field 'slot'.
     *
     *@param  slot  The slot number to be returned
     *@return       The StaticFieldObject value
     */
    public abstract DebuggerObject getStaticFieldObject(int slot);

    /**
     *  Return the object in object field 'slot'.
     *
     *@param  slot  The slot number to be returned
     *@return       The InstanceFieldObject value
     */
    public abstract DebuggerObject getInstanceFieldObject(int slot);

    /**
     * Return the object, about which some static type information is known,
     * in object field 'slot'.
     * 
     * @param slot          The slot number to be returned
     * @param expectedType  The static type of the value in the field
     * @return   The value in the field, as a DebuggerObject.
     */
    public abstract DebuggerObject getInstanceFieldObject(int slot, JavaType expectedType);
    
    /**
     *  Return the object in field 'slot' (counting static and object fields).
     *
     *@param  slot  The slot number to be returned
     *@return       The FieldObject value
     */
    public abstract DebuggerObject getFieldObject(int slot);

    /**
     * Return the object, about which some static type information is known,
     * in the field 'slot' (counting static and instance fields).
     * 
     * @param slot          The slot number to be returned
     * @param expectedType  The static type of the value in the field
     * @return              The field object value (as a DebuggerObject)
     */
    public abstract DebuggerObject getFieldObject(int slot, JavaType expectedType);

    /**
     *  Return the object in field 'slot' (counting static and object fields).
     *
     *@param  name  Description of Parameter
     *@return       The FieldObject value
     */
    public abstract DebuggerObject getFieldObject(String name);

    /**
     * Returns the a string representation of the value in 'slot' (counting static and object fields).
     * @param slot The slot number to be returned
     * @return     The string representation of this value
     */
    public abstract String getFieldValueString(int slot);
    
    /**
     * Returns the a string representation of the type of the value in 'slot' (counting static and object fields).
     * @param slot The slot number to be returned
     * @return     The string representation of this value
     */
    public abstract String getFieldValueTypeString(int slot);
    
    
    /**
     *  Return the jdi object. This exposes the jdi to Inspectors.
     *  If jdi is not being used, it should return null, which is
     *  the default implementation.
     *
     *@return    The ObjectReference value
     */
    public abstract com.sun.jdi.ObjectReference getObjectReference();

    /**
     *  Return a list of strings with the description of each static field
     *  in the format "<modifier> <type> <name> = <value>".
     *
     *@param  includeModifiers  Description of Parameter
     *@return                   The StaticFields value
     */
    public abstract List getStaticFields(boolean includeModifiers);

    /**
     *  Return a list of strings with the description of each instance field
     *  in the format "<modifier> <type> <name> = <value>" or
     *  "<type> <name> = <value>", depending on the parameter.
     *
     *@param  includeModifiers  Description of Parameter
     *@return                   The InstanceFields value
     */
    public abstract List getInstanceFields(boolean includeModifiers);

    /**
     *  Return a list of strings with the description of each field
     *  (including static and instance) in the
     *  format "<modifier> <type> <name> = <value>" or
     *  "<type> <name> = <value>", depending on the parameter.
     *
     *@param  includeModifiers  Description of Parameter
     *@return                   The AllFields value
     */
    public abstract List getAllFields(boolean includeModifiers);


    /**
     *  Return true if the static field 'slot' is public.
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public abstract boolean staticFieldIsPublic(int slot);

    /**
     *  Return true if the object field 'slot' is public.
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public abstract boolean instanceFieldIsPublic(int slot);


    /**
     *  Return true if the static field 'slot' is an object (and not
     *  a simple type).
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public abstract boolean staticFieldIsObject(int slot);

    /**
     *  Return true if the object field 'slot' is an object (and not
     *  a simple type).
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public abstract boolean instanceFieldIsObject(int slot);

    /**
     *  Return true if the field 'slot' is an object (and not
     *  a simple type). Includes static and instance fields.
     *
     *@param  slot  The slot number to be checked
     *@return       Description of the Returned Value
     */
    public abstract boolean fieldIsObject(int slot);
}
