package bluej.extensions;


import bluej.debugger.Debugger;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ObjectWrapper;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.views.*;
import java.lang.reflect.Modifier;


/**
 * This should behave as much as possible as a reflection COnstructor
 */
public class BConstructor 
  {
  private bluej.pkgmgr.Package bluej_pkg;
  private ConstructorView bluej_view;
  private DirectInvoker invoker;

  /**
   * NOT for public use: to be used from within the xtension package
   */
  BConstructor(bluej.pkgmgr.Package i_pkg, ConstructorView i_view )
    {
    bluej_pkg  = i_pkg;
    bluej_view = i_view;
    }

  /**
   * Tests if this constructor matches against the given param.
   */
  public boolean matches ( Class[] parameter )
    {
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
     * As From reflection API
     */
    public Class[] getParameterTypes ()
      {
      return bluej_view.getParameters();
      }

    /**
     * Creates a new instance of the object described by this constructor
     */
    public BObject newInstance ( Object[] initargs )
      {
      invoker = new DirectInvoker (bluej_pkg, bluej_view );
      DebuggerObject result = invoker.invokeConstructor (initargs);

      if (result == null) return null;

      String resultName = invoker.getResultName();
      PkgMgrFrame pmf   = PkgMgrFrame.findFrame(bluej_pkg);

      ObjectWrapper wrapper = ObjectWrapper.getWrapper(pmf, pmf.getObjectBench(), result, resultName);

      return new BObject(wrapper);
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
}