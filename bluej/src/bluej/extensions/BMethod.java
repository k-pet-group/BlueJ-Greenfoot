package bluej.extensions;

import bluej.debugger.Debugger;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ObjectWrapper;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.views.MethodView;
import bluej.views.ConstructorView;
import bluej.views.CallableView;
import java.lang.reflect.Modifier;
import bluej.pkgmgr.Package;
import bluej.views.*;

/**
 * The BlueJ proxy Method object. This represents a method of a class or object. 
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
     * @return: true if it matches false othervise.
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
     * invoke a method on the given Object.<P>
     * The real problem is how to manipulate the result that come out. The BLueJ behaviour is:<P>
     * - If the result is a primitive then you can vet the value but NOT put it into the bench<P>
     * - If the result is an OBject you can put it into the bench <P>
     * 
     * @param args an array containing the arguments which are parsed in the same way that BlueJ
     * parses input from the user. These should be exactly
     * as they would appear in Java code, for example <BL>
     * <LI><CODE>5</CODE>
     * <LI><CODE>"Fred"</CODE>
     * <LI><CODE>3.5f</CODE>
     * <LI><CODE>person1</CODE>
     * </BL>They can refer to other objects on the object bench too. If there are no parameters,
     * this should be set to <CODE>null</CODE>.
     * @return the resulting OBJect
     */
    public BObject invoke (BObject onThis, String[] params)
        {
        invoker = new DirectInvoker (bluej_pkg, bluej_view );
        DebuggerObject result = invoker.invokeMethod (onThis.getInstanceName(), params);

        // Result can be null if the method returns void. It is Reflection standard
        if (result == null) return null;

        String resultName = invoker.getResultName();
        PkgMgrFrame pmf   = PkgMgrFrame.findFrame(bluej_pkg);
        ObjectWrapper wrapper = ObjectWrapper.getWrapper(pmf, pmf.getObjectBench(), result, resultName);

        return new BObject(wrapper);
        }
    
    /**
     * Gets the last error that occurred<P>
     * This should be called after receiving a <code>null</code> back from an invoke
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