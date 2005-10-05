package bluej.extensions;

import java.util.logging.Logger;

import bluej.debugger.DebuggerObject;
import bluej.debugmgr.objectbench.ObjectBench;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.views.ConstructorView;
import bluej.views.View;

/**
 * 
 * This is an Invoker that can instantiate objects of classes that are NOT a part
 * of a project.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public class ConstructorInvoker {
    private transient final static Logger logger = Logger.getLogger("greenfoot");
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

   
    
    public ObjectWrapper invokeConstructor(String instanceNameOnObjectBench, Object[] args)
    throws InvocationArgumentException, InvocationErrorException {
        DirectInvoker di =
            new DirectInvoker(pkgFrame, getConstructor(view,args));
        ObjectBench objBench = pkgFrame.getObjectBench();
        Package pkg =pkgFrame.getPackage();    
        
        for (int i = 0; i < args.length; i++) {
            Object object = args[i];
            if(object instanceof String) {
                args[i] = ((String) object).replace('\\','/');
            }
        }
        
        DebuggerObject debugObject = di.invokeConstructor(args);
        ObjectWrapper wrapper = ObjectWrapper.getWrapper(
                pkgFrame, objBench,
                debugObject,
                instanceNameOnObjectBench);       
        
        objBench.addObject(wrapper);        
        pkg.getDebugger().addObject(pkg.getQualifiedName(), wrapper.getName(), debugObject);  
        
        return wrapper;         
    }

    private ConstructorView getConstructor(View simulationView, Object[] args) {
        ConstructorView[] constructors = simulationView.getConstructors();
        for (int i = 0; i < constructors.length; i++) {
            Class[] parameters = constructors[i].getParameters();
            boolean argMatch=true;
            for (int j = 0; j < parameters.length; j++) {
                Class class1 = parameters[j];
                if(args[j]==null || !args[j].getClass().equals(class1)) {
                    argMatch=false;
                    break;
                }
            }
            if(argMatch) {
                return constructors[i];
            }
        }
        return null;
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
