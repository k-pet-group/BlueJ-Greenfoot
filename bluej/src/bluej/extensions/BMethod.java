package bluej.extensions;

import bluej.debugger.DebuggerObject;
import bluej.views.MethodView;
import java.lang.reflect.Modifier;
import bluej.pkgmgr.Package;
import bluej.views.*;

import com.sun.jdi.*;

/**
 * A wrapper for a method of a BlueJ class.
 * Allows an extension to invoke a method on an object that is on the BlueJ object bench.
 * When values representing types are returned, there are two cases:
 * In the case that the returned value is of primitive type (<code>int</code> etc.), 
 * it is represented in the appropriate Java wrapper type (<code>Integer</code> etc.).
 * In the case that the returned value is an object type then an appropriate BObject will 
 * be returned, allowing the returned object itself to be placed on the BlueJ object bench.
 *
 * @version $Id: BMethod.java 1968 2003-05-21 09:59:49Z damiano $
 */

/*
 * The same reasoning as of BConstructor applies here.
 * AUthor Clive Miller, University of Kent at Canterbury, 2002
 * Author Damiano Bolla, University of Kent at Canterbury 2003
 */
public class BMethod
{
    private MethodView bluej_view;
    private DirectInvoker invoker;
    private Identifier parentId;
    
    /**
     * Constructor.
     */
    BMethod ( Identifier aParentId, MethodView i_bluej_view )
    {
        parentId = aParentId;
        bluej_view = i_bluej_view;
    }

    /**
     * Tests if this method matches against the given signature.
     * This is similar to reflection API.
     * Returns true if there is a match, false otherwise.
     * Pass a zero length parameter array if the method takes no arguments.
     */
    public boolean matches ( String methodName, Class[] parameter )
      {
      // If someone is crazy enough to do this he deserves it :-)
      if ( methodName == null ) return false;

      // Let me se if the named method is OK
      if ( ! methodName.equals(bluej_view.getName() ) ) return false;
     
      Class[] thisArgs = bluej_view.getParameters();

      // An empty array is equivalent to a null array
      if (thisArgs  != null && thisArgs.length  <= 0)  thisArgs  = null;
      if (parameter != null && parameter.length <= 0 ) parameter = null;

      // If both are null the we are OK
      if ( thisArgs == null && parameter == null ) return true;

      // If ANY of them is null we are in trouble now. (They MUST be both NOT null)
      if ( thisArgs == null || parameter == null ) return false;

      // Now I know that BOTH are NOT empty. They MUST be the same length
      if ( thisArgs.length != parameter.length ) return false;
    
      for ( int index=0; index<thisArgs.length; index++ )
        if ( ! thisArgs[index].isAssignableFrom(parameter[index]) ) return false;

      return true;
      }

    /**
     * Returns the types of the parameters of this method.
     * Similar to Reflection API
     */
    public Class[] getParameterTypes()
      {
      return bluej_view.getParameters();
      }
      
    /**
     * Returns the name of this method.
     * Similar to Reflection API
     */
    public String getName()
      {
      return bluej_view.getName();
      }
    
    /**
     * Returns the return type of this method
     * Similar to Reflection API
     */
    public Class getReturnType()
        {
        View aView = bluej_view.getReturnType();
        return aView.getViewClass();
        }
    
    /**
     * Returns the modifiers for this method.
     * The <code>java.lang.reflect.Modifier</code> class can be used to decode the modifiers.
     */
    public int getModifiers()
        {
        return bluej_view.getModifiers();
        }

    /**
     * Invoke this method on the given object.
     * 
     * @param onThis The BObject to which the method call should be applied, null if a static method.
     * @param params an array containing the arguments, or null if there are none
     * @return the resulting Object. It can be a wrapper for a primitive type or a BObject
     */
    public Object invoke (BObject onThis, Object[] params) 
        throws ProjectNotOpenException, PackageNotFoundException
        {
        Package bluejPkg = parentId.getBluejPackage();
        
        String instanceName=null;
        // If it is a method call on a live object get the identifier for it.
        if ( onThis != null ) instanceName = onThis.getInstanceName();
        
        invoker = new DirectInvoker (bluejPkg, bluej_view );
        DebuggerObject result = invoker.invokeMethod (instanceName, params);

        // Result can be null if the method returns void. It is Reflection standard
        if (result == null) return null;

        String resultName = invoker.getResultName();

        ObjectReference objRef = result.getObjectReference();
        ReferenceType type = objRef.referenceType();

        // It happens that the REAL result is in the result field of this Object...
        Field thisField = type.fieldByName ("result");
        if ( thisField == null ) return null;

        // DOing this is the correct way of returning the right object. Tested 080303, Damiano
        return BField.doGetVal(bluejPkg, resultName, objRef.getValue(thisField));
        }
    
    /**
     * Returns the last error that occurred during invocation.
     * This should be called after receiving a <code>null</code> back from a call on <code>invoke()</code>.
     * It returns a descriptive reason for the error.
     */
    public String getLastError()
    {
        if (invoker == null) return null;
        return invoker.getError();
    }
    
    /**
     * Returns a string representing the return type, name and signature of this method
     */
    public String toString()
    {
        Class[] signature = getParameterTypes();
        String sig = "";
        for (int i=0; i<signature.length; i++) {
            sig += signature[i].getName() + (i==signature.length-1?"":", ");
        }
        String mod = Modifier.toString (getModifiers());
        if (mod.length() > 0) mod += " ";
        return mod+getReturnType()+" "+getName()+"("+sig+")";
    }
}
