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
 * This could be a method, or constructor.
 *
 * @author Clive Miller
 * @version $Id: BMethod.java 1651 2003-03-05 17:03:15Z damiano $
 * @see bluej.extensions.BObject#getMethod(java.lang.String,java.lang.Class[])
 * @see bluej.extensions.BObject#getMethods(boolean)
 * @see bluej.extensions.BClass#getConstructor(java.lang.Class[])
 * @see bluej.extensions.BClass#getConstructors()
 * @see bluej.extensions.BClass#getStaticMethod(java.lang.String,java.lang.Class[])
 * @see bluej.extensions.BClass#getStaticMethods()
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
     * Gets the name of this method
     * @return the name of the method
     */
    public String getName()
      {
      return bluej_view.getName();
      }
    
    /**
     * Gets the signature of this method
     * @return the signature of this method
     */
    public Class[] getSignature()
    {
        return bluej_view.getParameters();
    }

    /**
     * Gets the return type of this method
     * @return a string describing the return type, eg <code>int</code>, <code>Object</code>
     */
    public Class getReturnType()
    {
        View aView = bluej_view.getDeclaringView();
        return aView.getViewClass();
    }
    
    /**
     * Invokes the method and places the result on the object bench, with
     * the given name. It is performed synchronously, so this method
     * blocks until the invocation is complete.
     * <P>The returning value is returned as a BField. If this is a constructor,
     * the new object can be extracted from BField and placed on the object
     * bench, or have further methods called on it.
     * @param args an array containing the arguments which are parsed in the same way that BlueJ
     * parses input from the user. These should be exactly
     * as they would appear in Java code, for example <BL>
     * <LI><CODE>5</CODE>
     * <LI><CODE>"Fred"</CODE>
     * <LI><CODE>3.5f</CODE>
     * <LI><CODE>person1</CODE>
     * </BL>They can refer to other objects on the object bench too. If there are no parameters,
     * this should be set to <CODE>null</CODE>.
     * @return the resulting BField
     */
    public BObject invoke (BObject onThisObject, String[] params)
    {
        String instanceName = onThisObject.getInstanceName();
        invoker = new DirectInvoker (bluej_pkg, bluej_view, instanceName);
        DebuggerObject result = invoker.invoke (params);

        if (result == null) return null;

        String resultName = invoker.getResultName();
        PkgMgrFrame pmf   = PkgMgrFrame.findFrame(bluej_pkg);
        ObjectWrapper wrapper = ObjectWrapper.getWrapper(pmf, pmf.getObjectBench(), result, resultName);

        // WARNING: Do I need to add it to the Debugger ?? like this
        // Debugger.debugger.addObjectToScope(pmf.getPackage().getId(), wrapper.getName(), result);
        return new BObject(wrapper);
    }
    
    /**
     * Gets the last error that occurred. This should be called after receiving a <code>null</code> back from an
     * invoke
     * @return any error as a String
     */
    public String getLastError()
    {
        if (invoker == null) return null;
        return invoker.getError();
    }
    
    /**
     * Determines the modifiers for this method.
     * Use <code>java.lang.reflect.Modifiers</code> to
     * decode the meaning of this integer
     * @return The modifiers of this method, encoded in
     * a standard Java language integer.
     */
    public int getModifiers()
    {
        return bluej_view.getModifiers();
    }

    /**
     * Gets a description of this method
     * @return the return type, name and signature of the method
     */
    public String toString()
    {
        Class[] signature = getSignature();
        String sig = "";
        for (int i=0; i<signature.length; i++) {
            sig += signature[i].getName() + (i==signature.length-1?"":", ");
        }
        String mod = Modifier.toString (getModifiers());
        if (mod.length() > 0) mod += " ";
        return mod+getReturnType()+" "+getName()+"("+sig+")";
    }
}