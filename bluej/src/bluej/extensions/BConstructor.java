package bluej.extensions;

/**
 * This should behave as much as possible as a reflection COnstructor
 * 
 */
public class BConstructor 
{
  public BConstructor()
  {
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
  public BObject newInstance ( Object [] initargs )
    {
    /*
    invoker = new DirectInvoker (pkg.getRealPackage(), view, instanceName);
    DebuggerObject result = invoker.invoke (args);

    if (result == null) return null;

    String resultName = invoker.getResultName();
    if ( view instanceof ConstructorView) {
        PkgMgrFrame pmf = PkgMgrFrame.findFrame (pkg.getRealPackage());

        if (!result.isNullObject()) {
            ObjectWrapper wrapper = ObjectWrapper.getWrapper(pmf, pmf.getObjectBench(), result, resultName);
            pmf.getObjectBench().add(wrapper);  // might change name

            // load the object into runtime scope
            Debugger.debugger.addObjectToScope(pmf.getPackage().getId(),
                                                wrapper.getName(), result);
        }
    }
    return new BField (pkg, result.getObjectReference(), result.getObjectReference().referenceType().fieldByName (resultName));
*/
    return null;
    }
  
}