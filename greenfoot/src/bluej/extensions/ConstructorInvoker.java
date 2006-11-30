package bluej.extensions;

import bluej.debugger.Debugger;
import bluej.debugger.DebuggerObject;
import bluej.debugmgr.objectbench.ObjectBench;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.views.View;

/**
 * 
 * This is an Invoker that can instantiate objects of classes that are NOT a part
 * of a project.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public class ConstructorInvoker {
    private PkgMgrFrame pkgFrame;
    private BProject prj;

    private View view;

    public ConstructorInvoker(BPackage bPackage, String className)
        throws ProjectNotOpenException, PackageNotFoundException {
        pkgFrame = (PkgMgrFrame) bPackage.getFrame();
        prj = bPackage.getProject();
        Class launcherClass = null;
        try {
            launcherClass = getClass(className);
        } catch (java.lang.ClassNotFoundException e) {
            e.printStackTrace();
        }
        view = View.getView(launcherClass);
    }
   
    
    /**
     * Invoke a constructor which takes String arguments only.
     * 
     * @param instanceNameOnObjectBench  Name of the created object as it
     *                                   should appear on the bench
     * @param args  Arguments to supply to the constructor
     * @return  The newly created object
     * 
     * @throws InvocationArgumentException
     * @throws InvocationErrorException
     */
    public ObjectWrapper invokeConstructor(String instanceNameOnObjectBench, String[] args)
        throws InvocationArgumentException, InvocationErrorException
    {
        ObjectBench objBench = pkgFrame.getObjectBench();
        Package pkg = pkgFrame.getPackage();    
        
        Debugger debugger = pkgFrame.getProject().getDebugger();
        String [] argTypes = new String[args.length];
        DebuggerObject [] argObjects = new DebuggerObject[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = "java.lang.String";
            argObjects[i] = debugger.getMirror(args[i]);
        }
        
        DebuggerObject debugObject = debugger.instantiateClass(view.getQualifiedName(),
                argTypes, argObjects).getResultObject();
        
        ObjectWrapper wrapper = ObjectWrapper.getWrapper(
                pkgFrame, objBench,
                debugObject,
                debugObject.getGenType(),
                instanceNameOnObjectBench);       
        
        objBench.addObject(wrapper);        
        pkg.getDebugger().addObject(pkg.getQualifiedName(), wrapper.getName(), debugObject);  
        
        return wrapper;         
    }

    private Class getClass(String fullClassname)
        throws java.lang.ClassNotFoundException {
        Class cls = null;
        try {
            cls = prj.getClassLoader().loadClass(fullClassname);
        }
        catch (ProjectNotOpenException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return cls;
    }
}
