package greenfoot.core;

import greenfoot.util.GreenfootUtil;

import java.io.File;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import rmiextension.wrappers.RClass;
import rmiextension.wrappers.RObject;
import rmiextension.wrappers.RPackage;
import bluej.extensions.BObject;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.MissingJavaFileException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * Represents a package in Greenfoot.
 * 
 * <p>A GPackage is essentially a reference to a remote package (RPackage), together
 * with a pool of GClass objects representing the classes in the package. 
 * 
 * @author Poul Henriksen
 * 
 */
public class GPackage
{
    private RPackage pkg;
    private GProject project; 
    
    private Map<RClass,GClass> classPool = new HashMap<RClass,GClass>();
    
    /**
     * Construct a new GPackage; this should generally only be called by
     * GProject.
     * 
     * @param pkg  The reference to the remote package
     * @param project  The project
     */
    public GPackage(RPackage pkg, GProject project)
    {
        if(pkg == null) {
            throw new NullPointerException("Pkg must not be null.");
        }
        if(project == null) {
            throw new NullPointerException("Project must not be null.");
        }
        this.pkg = pkg;
        this.project = project;
    }
    
    /**
     * Get the GClass wrapper for a remote class in this package.
     */
    public GClass getGClass(RClass remoteClass)
    {
        if (remoteClass == null) {
            return null;
        }
        
        GClass gClass;
        synchronized (classPool) {
            gClass = classPool.get(remoteClass);
            if (gClass == null) {
                gClass = new GClass(remoteClass, this);
                classPool.put(remoteClass, gClass);
            }
        }
        return gClass;
    }
    
    public void compile(boolean waitCompileEnd)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException, CompilationNotStartedException
    {
        pkg.compile(waitCompileEnd);
    }
    

    public void compileAll(boolean waitCompileEnd) throws ProjectNotOpenException, PackageNotFoundException, RemoteException, CompilationNotStartedException
    {
        pkg.compileAll(waitCompileEnd);
    }

    public File getDir()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        return pkg.getDir();
    }

    public String getName()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        return pkg.getName();
    }

    public RObject getObject(String instanceName)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        return pkg.getObject(instanceName);
    }

    public BObject[] getObjects()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        return pkg.getObjects();
    }

    public GProject getProject()
        throws ProjectNotOpenException, RemoteException
    {
        if(project == null) {
            project = new GProject(pkg.getProject());
        }
        return project;
    }

    public GClass[] getClasses()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        RClass[] rClasses = pkg.getRClasses();
        GClass[] gClasses = new GClass[rClasses.length];
        for (int i = 0; i < rClasses.length; i++) {
            RClass rClass = rClasses[i];
            gClasses[i] = getGClass(rClass);
        }
        return gClasses;
    }

    public String invokeConstructor(String className, String[] argTypes, String[] args)
        throws RemoteException
    {
        return pkg.invokeConstructor(className, argTypes, args);
    }

    public String invokeMethod(String className, String methodName, String[] argTypes, String[] args)
        throws RemoteException
    {
        return pkg.invokeMethod(className, methodName, argTypes, args);
    }

    public GClass newClass(String className)
    {
    	GClass newClass = null;
        try {
            RClass newRClass = pkg.newClass(className);
            newClass = new GClass(newRClass, this);
            synchronized (classPool) {
                classPool.put(newRClass, newClass);
            }
        }
        catch (RemoteException re) {
            re.printStackTrace();
        }
        catch (ProjectNotOpenException pnoe) {
            pnoe.printStackTrace();
        }
        catch (PackageNotFoundException pnfe) {
            pnfe.printStackTrace();
        }
        catch (MissingJavaFileException mjfe) {
            mjfe.printStackTrace();
        }
        return newClass;
    }
    
    /**
     * Get the named class.
     */
    public GClass getClass(String className)
    {
        try {
            RClass rClass = pkg.getRClass(className);
            return getGClass(rClass);
        }
        catch (RemoteException re) {
            re.printStackTrace();
        }
        catch (ProjectNotOpenException pnoe) {
            pnoe.printStackTrace();
        }
        catch (PackageNotFoundException pnfe) {
            pnfe.printStackTrace();
        }
        
        return null;
    }

    public void reload()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        pkg.reload();
    }

    /**
     * Delete class files for all classes in the project.
     *
     */
    public void deleteClassFiles()
    {
        try {
            GClass[] classes = getClasses();
            for (int i = 0; i < classes.length; i++) {
                GClass cls = classes[i];
                File classFile = new File(getDir(), cls.getName() + ".class");
                classFile.delete();
            }

            this.reload();
        }
        catch (ProjectNotOpenException e) {
        }
        catch (PackageNotFoundException e) {
        }
        catch (RemoteException e) {
        }
    }

    public void close()
    {
        try {
            pkg.close();
        }
        catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /** 
     * Returns all the world sub-classes in this package that can be instantiated.
     * 
     * @return
     */
    public List<Class> getWorldClasses()
    {
        List<Class> worldClasses= new LinkedList<Class>();
        try {
            GClass[] classes = getClasses();
            for (int i = 0; i < classes.length; i++) {
                GClass cls = classes[i];
                if(cls.isWorldSubclass()) {
                    Class realClass = cls.getJavaClass();   
                    if (GreenfootUtil.canBeInstantiated(realClass)) {                  
                        worldClasses.add(realClass);
                    }                    
                }
            }
        }
        catch (ProjectNotOpenException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (PackageNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        return worldClasses;
    }

}
