   package bluej.debugger;

   import java.util.List;


/**
 * A class representing an object in the debugged VM.
 *
 * @author  Michael Cahill
 * @author  Michael Kolling
 * @author  Duane Buck
 * @version $Id: DebuggerObject.java 710 2000-11-22 06:33:42Z dbuck $
 */
    public abstract class DebuggerObject
   {
    /**
     * Get the fully qualified name of the class of this object.
     */
       public abstract String getClassName();
   
   
    /**
     * Is an object of this class assignable to the given fully qualified type?
     */
       public abstract boolean isAssignableTo(String type);
   
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
     * Return the object in field 'slot' (counting static and object fields).
     *
     * @param slot  The slot number to be returned
     */
       public abstract DebuggerObject getFieldObject(String name);
   
    /**
     * Return the jdi object. This exposes the jdi to Inspectors.
     * If jdi is not being used, it should return null, which is
     * the default implementation.
     */
       public com.sun.jdi.ObjectReference getObjectReference(){
         return null;
      }
   
    /**
     * Invoke the method on the server thread. This exposes the jdi to Inspectors.
     * Signature codes are as follows (JNI signatures are used):
     * Z -boolean 
     * B -byte
     * C -char
     * S -short
     * I -int
     * J -long
     * F -float
     * D -double
     * L fully-qualified-class ; -fully-qualified-class 
     * [ type -type[]
     * ( arg-types ) ret-type -method type
     * Example: ()[Ljava/lang/Object;
     *    -an empty parameter list method returning an array of objects
     */
       public com.sun.jdi.Value invokeMethod(String methodName, String signature, java.util.List arguments) {
         return null;
      }
   
    /**
     * Return a list of strings with the description of each static field
     * in the format "<modifier> <type> <name> = <value>".
     */
       public abstract List getStaticFields(boolean includeModifiers);
   
    /**
     * Return a list of strings with the description of each instance field
     * in the format "<modifier> <type> <name> = <value>" or
     * "<type> <name> = <value>", depending on the parameter.
     */
       public abstract List getInstanceFields(boolean includeModifiers);
   
    /**
     * Return a list of strings with the description of each field
     * (including static and instance) in the
     * format "<modifier> <type> <name> = <value>" or
     * "<type> <name> = <value>", depending on the parameter.
     */
       public abstract List getAllFields(boolean includeModifiers);
   
   }
