package greenfoot.core;

import greenfoot.gui.inspector.UpdatingClassInspector;
import greenfoot.gui.inspector.UpdatingObjectInspector;

import java.awt.EventQueue;
import java.io.File;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JFrame;

import rmiextension.wrappers.RClass;
import rmiextension.wrappers.RPackage;
import rmiextension.wrappers.RProject;
import rmiextension.wrappers.event.RProjectListenerImpl;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerObject;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.inspector.ClassInspector;
import bluej.debugmgr.inspector.Inspector;
import bluej.debugmgr.inspector.InspectorManager;
import bluej.debugmgr.inspector.ObjectInspector;
import bluej.debugmgr.inspector.ResultInspector;
import bluej.extensions.PackageAlreadyExistsException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.pkgmgr.Package;
import bluej.testmgr.record.ClassInspectInvokerRecord;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.Debug;

/**
 * 
 * Represents a project in greenfoot.
 * 
 * @author Poul Henriksen
 */
public class GProject extends RProjectListenerImpl
    implements InspectorManager
{
    /** This holds all object inspectors and class inspectors
    for a world. */
    private Map inspectors = new HashMap();
    
    private Map<RPackage,GPackage> packagePool = new HashMap<RPackage,GPackage>();
    
    private RProject rProject;

    private ProjectProperties projectProperties;
    
    
    /**
     * Create a G(reenfoot)Project object. This is a singleton for every
     * running Greenfoot project (for every VM).
     */
    public GProject(RProject rmiProject) throws RemoteException
    {
        this.rProject = rmiProject;
        rmiProject.addListener(this);
        try {
            projectProperties = new ProjectProperties(getDir());
        }
        catch (Exception exc) {
            Debug.reportError("Could not open greenfoot project properties");
            exc.printStackTrace();
        }
    }
    
    public void close()
        throws RemoteException
    {
        rProject.close();
    }

    public void save()
        throws ProjectNotOpenException, RemoteException
    {
        rProject.save();
    }
    
    /**
     * returns the default package.
     * 
     */
    public GPackage getDefaultPackage() throws ProjectNotOpenException, RemoteException {
    	return getPackage("");
    }

    /**
     * returns the greenfoot package.
     * 
     */
    public GPackage getGreenfootPackage() throws ProjectNotOpenException, RemoteException {
        return getPackage("greenfoot");
    }
    
    /**
     * returns the named package.
     */
    public GPackage getPackage(String packageName) throws ProjectNotOpenException, RemoteException
    {
        RPackage rPkg = rProject.getPackage(packageName);
        if (rPkg == null) {
            return null;
        }
        else {
            return getPackage(rPkg);
        }
    }

    /**
     * Get a GPackage wrapper for an RPackage object
     */
    public GPackage getPackage(RPackage pkg)
    {
        GPackage ret = packagePool.get(pkg);
        if (ret == null) {
            ret = new GPackage(pkg, this);
            packagePool.put(pkg, ret);
        }
        return ret;
    }
    
    public RPackage[] getPackages()
        throws ProjectNotOpenException, RemoteException
    {
        return rProject.getPackages();
    }


    public GPackage newPackage(String fullyQualifiedName)
        throws ProjectNotOpenException, PackageAlreadyExistsException, RemoteException
    {
        return getPackage(rProject.newPackage(fullyQualifiedName));
    }

    /**
     * Get a remote reference to a class in this project.
     * 
     * @param fullyQualifiedName  The fully-qualified class name
     * @return  A remote reference to the class
     */
    public RClass getRClass(String fullyQualifiedName)
    {
        try {
            int lastDotIndex = fullyQualifiedName.lastIndexOf('.');
            RPackage pkg;
            String className;
            if (lastDotIndex == -1) {
                pkg = rProject.getPackage("");
                className = fullyQualifiedName;
            }
            else {
                pkg = rProject.getPackage(fullyQualifiedName.substring(0, lastDotIndex));
                className = fullyQualifiedName.substring(lastDotIndex + 1);
            }
            if (pkg == null) {
                return null;
            }
            return pkg.getRClass(className);
        }
        catch (RemoteException re) {
            throw new InternalGreenfootError(re);
        }
        catch (ProjectNotOpenException pnoe) {
            throw new InternalGreenfootError(pnoe);
        }
        catch (PackageNotFoundException pnfe) {
            throw new InternalGreenfootError(pnfe);
        }
    }

    public File getDir()
        throws ProjectNotOpenException,RemoteException
    {
        return rProject.getDir();
    }

    
    /**
     * Get the project name (the name of the directory containing it)
     */
    public String getName()
    {
        try {
            return rProject.getName();
        }
        catch (ProjectNotOpenException pnoe) {
            // this exception should never happen
            pnoe.printStackTrace();
        }
        catch (RemoteException re) {
            // this should also not happen
            re.printStackTrace();
        }
        return null;
    }

    
    public ObjectInspector getInspectorInstance(DebuggerObject obj,
            String name, Package pkg, InvokerRecord ir, JFrame parent) {
        ObjectInspector inspector = (ObjectInspector) inspectors.get(obj);
        
        if (inspector == null) {
            inspector = new UpdatingObjectInspector(obj, this, name, pkg, ir, parent);
            inspectors.put(obj, inspector);
        }
        
        final ObjectInspector insp = inspector;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                insp.update();
                insp.updateLayout();
                insp.setVisible(true);
                insp.bringToFront();
            }
        });
        
        return inspector;
    }
    
    
    /**
     * Return an ObjectInspector for an object. The inspector is visible.
     * 
     * @param obj The object displayed by this viewer
     * @param name The name of this object or "null" if the name is unobtainable
     * @param pkg The package all this belongs to
     * @param ir the InvokerRecord explaining how we got this result/object if
     *            null, the "get" button is permanently disabled
     * @param info The information about the the expression that gave this
     *            result
     * @param parent The parent frame of this frame
     * @return The Viewer value
     */
    public ResultInspector getResultInspectorInstance(DebuggerObject obj,
        String name, Package pkg, InvokerRecord ir, ExpressionInformation info,
        JFrame parent) {
       /* System.out.println("pkg:" + pkg);
        System.out.println("prj: " + pkg.getProject());
        System.out.println("insp: " + pkg.getProject().getInspector(obj));
        System.out.println("");
        */
        
        ResultInspector inspector = (ResultInspector) inspectors.get(obj);
        
      
        if (inspector == null) {
            inspector = new ResultInspector(obj, this, name, pkg, ir, info, parent);
            inspectors.put(obj, inspector);
        }

        final ResultInspector insp = inspector;
        insp.update();
        insp.updateLayout();
        EventQueue.invokeLater(new Runnable() {
                public void run() {
                    insp.setVisible(true);
                    insp.bringToFront();
        }
            });

        return inspector;
    }
    
    
    /**
     * Return a ClassInspector for a class. The inspector is visible.
     *
     * @param clss
     *            The class displayed by this viewer
     * @param name
     *            The name of this object or "null" if it is not on the object
     *            bench
     * @param pkg
     *            The package all this belongs to
     * @param getEnabled
     *            if false, the "get" button is permanently disabled
     * @param parent
     *            The parent frame of this frame
     * @return The Viewer value
     */
    public ClassInspector getClassInspectorInstance(DebuggerClass clss,
        Package pkg, JFrame parent) {
        ClassInspector inspector = (ClassInspector) inspectors.get(clss.getName());

        if (inspector == null) {
            ClassInspectInvokerRecord ir = new ClassInspectInvokerRecord(clss.getName());
            inspector = new UpdatingClassInspector(clss, this, pkg, ir, parent);
            inspectors.put(clss.getName(), inspector);
        }

        final Inspector insp = inspector;
        EventQueue.invokeLater(new Runnable() {
                public void run() {
                    insp.update();
                    insp.updateLayout();
                    insp.setVisible(true);
                    insp.bringToFront();
                }
            });

        return inspector;
    }


    public boolean inTestMode()
    {
        //Greenfoot does not support testing:
        return false;
    }
    
    
    /**
     * Removes all inspector instances for this project.
     * This is used when VM is reset or the project is recompiled.
     *
     */
    public void removeAllInspectors() {
        for (Iterator it = inspectors.values().iterator(); it.hasNext();) {
            Inspector inspector = (Inspector) it.next();
            inspector.setVisible(false);
            inspector.dispose();
        }

        inspectors.clear();
    }

    
    /**
     * Remove an inspector from the list of inspectors for this project
     * @param obj the inspector.
     */
    public void removeInspector(DebuggerObject obj) {
        inspectors.remove(obj);
    }
    
    
    /**
     * Remove an inspector from the list of inspectors for this project
     * @param obj the inspector. 
     */
    public void removeInspector(DebuggerClass cls) {
        inspectors.remove(cls.getName());
    }
    
    
    /**
     * Retrieve the properties for a package. Loads the properties if necessary.
     */
    public ProjectProperties getProjectProperties()
    {
        return projectProperties;
    }
    
    /**
     * Show the readme file for this project in an editor window.
     */
    public void openReadme()
    {
        try {
            rProject.openReadmeEditor();
        }
        catch (RemoteException re) {
            re.printStackTrace();
        }
        catch (ProjectNotOpenException pnoe) {
            pnoe.printStackTrace();
        }
    }
    
    /**
     * Get the remote project reference which this GProject wraps.
     */
    public RProject getRProject()
    {
        return rProject;
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.event.RProjectListener#projectClosing()
     */
    public void projectClosing()
    {
        GreenfootMain.getInstance().projectClosing();
    }
}
