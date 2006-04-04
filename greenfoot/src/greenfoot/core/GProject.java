package greenfoot.core;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JFrame;

import rmiextension.wrappers.RPackage;
import rmiextension.wrappers.RProject;
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

/**
 * 
 * Represents a project in greenfoot.
 * 
 * @author Poul Henriksen
 */
public class GProject implements InspectorManager
{
    /** This holds all object inspectors and class inspectors
    for a world. */
    private Map inspectors = new HashMap();
    
    private RProject rProject;

    private ProjectProperties projectProperties = null;
    
    public GProject(RProject rmiProject)
    {
        this.rProject = rmiProject;
    }

    public void close()
        throws ProjectNotOpenException, RemoteException
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
            return new GPackage(rPkg, this);
        }
    }

    public RPackage[] getPackages()
        throws ProjectNotOpenException, RemoteException
    {
        return rProject.getPackages();
    }


    public GPackage newPackage(String fullyQualifiedName)
        throws ProjectNotOpenException, PackageAlreadyExistsException, RemoteException
    {
        return new GPackage(rProject.newPackage(fullyQualifiedName));
    }

    public File getDir()
        throws ProjectNotOpenException,RemoteException
    {
        return rProject.getDir();
    }

    public String getName()
        throws ProjectNotOpenException, RemoteException
    {
        return rProject.getName();
    }

    public ObjectInspector getInspectorInstance(DebuggerObject obj,
            String name, Package pkg, InvokerRecord ir, JFrame parent) {
        ObjectInspector inspector = (ObjectInspector) inspectors.get(obj);
        
        if (inspector == null) {
            inspector = new ObjectInspector(obj, this, name, pkg, ir, parent);
            inspectors.put(obj, inspector);
        }
        
        final ObjectInspector insp = inspector;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                insp.update();
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
            inspector = new ClassInspector(clss, this, pkg, ir, parent);
            inspectors.put(clss.getName(), inspector);
        }

        final Inspector insp = inspector;
        EventQueue.invokeLater(new Runnable() {
                public void run() {
                    insp.update();
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
     * Get a persistent greenfoot property for this project.
     * @param propName   The name of the property whose value to get
     * @return   The string value of the property
     *
     */
    public String getProperty(String propName)
    {
        loadProperties();
        return projectProperties.getProperty(propName);
    }
    
    
    /**
     * Set a persistent greenfoot property for this project.
     * @param propName  The name of the property to set
     * @param value     The string value of the property
     * 
     * @throws PackageNotFoundException
     * @throws RemoteException
     * @throws IOException
     */
    public void setProperty(String propName, String value)
        throws PackageNotFoundException, RemoteException, IOException
    {
        loadProperties();
        projectProperties.setProperty(propName, value);       
    }
    
    private void loadProperties()
    {
        // More than one GProject may be created which references the same project.
        // Properties for a single package must be shared for all instances of GProject,
        // so we use a global map maintained by the Greenfoot class.
        if (projectProperties == null) {
            projectProperties = Greenfoot.getInstance().getProjectProperties();
        }
    }
}
