package bluej.extensions;


import bluej.debugger.Debugger;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ObjectWrapper;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.views.*;
import java.lang.reflect.Modifier;


/**
 * This should behave as much as possible as a reflection COnstructor
 * 
 */
public class BConstructor 
  {
  private bluej.pkgmgr.Package bluej_pkg;
  private ConstructorView bluej_view;
  private String instanceName;

  /**
   * NOT for public use: to be used from within the xtension package
   */
  BConstructor(bluej.pkgmgr.Package i_pkg, ConstructorView i_view )
    {
    bluej_pkg  = i_pkg;
    bluej_view = i_view;
    }

  /**
   * If you want to name the object that appears in the object bench with something
   * you like, you can do it using this method BEFORE you call the newInstance.
   */
  public String setInstanceName ( String i_instanceName )
    {
    return instanceName = i_instanceName;
    }
    
  /**
   * Get the name of this constructor
   */
  public String getName ()
    {
    return "TODO";  
    }

  /**
   * Creates a new instance of the object described by this constructor
   */
  public BObject newInstance ( String[] initargs )
    {
    DirectInvoker invoker = new DirectInvoker (bluej_pkg, bluej_view, instanceName);
    DebuggerObject result = invoker.invoke (initargs);

    if (result == null) return null;

    if (result.isNullObject()) 
      {
      System.out.println ("BConstructor.newInstance: ERROR isNulObject == true");
      return null;
      }

    String resultName = invoker.getResultName();
    PkgMgrFrame pmf = PkgMgrFrame.findFrame (bluej_pkg);

    ObjectWrapper wrapper = ObjectWrapper.getWrapper(pmf, pmf.getObjectBench(), result, resultName);
    pmf.getObjectBench().add(wrapper);

    // load the object into runtime scope
    Debugger.debugger.addObjectToScope(pmf.getPackage().getId(), wrapper.getName(), result);

    return new BObject(wrapper);
    }
  }