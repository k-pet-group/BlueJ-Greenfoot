package bluej.extensions;

import bluej.debugger.DebuggerObject;
import bluej.views.MethodView;
import java.lang.reflect.Modifier;
import bluej.pkgmgr.Package;
import bluej.views.*;

import com.sun.jdi.*;

/**
 * This class encapsulate a method. 
 * Its duty is to provide a way to the developer to call a method on a given object that is on the bench.
 * What it returns is an Object that is either a primitive type encapsulation
 * Integer for int, Long for long and so on, or it may return a BObject that can
 * further be sent to the bench.
 */
public class BMethod
{
    private final Package    bluej_pkg;
    private final MethodView bluej_view;
    private       DirectInvoker invoker;
    
    /**
     * A new method. you can get it from the BClass
     */
    BMethod (Package i_bluej_pkg, MethodView i_bluej_view )
    {
        bluej_pkg  = i_bluej_pkg;
        bluej_view = i_bluej_view;
    }

    /**
     * Tests if this mthod matches against the given param.
     * @return true if it matches false othervise.
     */
    boolean matches ( String methodName, Class[] parameter )
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
     * @return the parameters of this method. See Reflection API
     */
    public Class[] getParameterTypes()
      {
      return bluej_view.getParameters();
      }
      
    /**
     * @return name of the method. See Reflection API
     */
    public String getName()
      {
      return bluej_view.getName();
      }
    
    /**
     * Gets the return type of this method
     * @return a string describing the return type, eg <code>int</code>, <code>Object</code>
     */
    public Class getReturnType()
        {
        View aView = bluej_view.getReturnType();
        return aView.getViewClass();
        }
    
    /**
     * @return The modifiers of this method. See Reflection API
     */
    public int getModifiers()
        {
        return bluej_view.getModifiers();
        }

    /**
     * Invoke this method on the given Object.
     * 
     * @param onThis The BObject where you want to apply this method
     * @param params an array containing the arguments. If none then null
     *
     * @return the resulting Object. It can be a primitive wrapper or a BObject
     */
    public Object invoke (BObject onThis, Object[] params)
        {
        invoker = new DirectInvoker (bluej_pkg, bluej_view );
        DebuggerObject result = invoker.invokeMethod (onThis.getInstanceName(), params);

        // Result can be null if the method returns void. It is Reflection standard
        if (result == null) return null;

        String resultName = invoker.getResultName();

        ObjectReference objRef = result.getObjectReference();
        ReferenceType type = objRef.referenceType();

        // It happens that the REAL result is in the result field of this Object...
        Field thisField = type.fieldByName ("result");
        if ( thisField == null ) return null;

        // DOing this is the correct way of returning the right object. Tested 080303, Damiano
        return BField.getVal(bluej_pkg, resultName, objRef.getValue(thisField));
        }
    
    /**
     * Gets the last error that occurred.
     * This should be called after receiving a <code>null</code> back from an invoke.
     * 
     * @return any error as a String
     */
    public String getLastError()
    {
        if (invoker == null) return null;
        return invoker.getError();
    }
    
    /**
     * @return the return type, name and signature of the method
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