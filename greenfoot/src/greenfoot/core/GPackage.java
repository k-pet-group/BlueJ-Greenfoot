package greenfoot.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
 * @author Poul Henriksen
 * 
 */
public class GPackage
{
    private RPackage pkg;
    private GProject project; 
    private GClass classes;
    
    private Map classPool = new HashMap();
    private Properties pkgProperties = null;
    
    public GPackage(RPackage pkg)
    {
        if(pkg == null) {
            throw new NullPointerException("Pkg must not be null.");
        }
        this.pkg = pkg;
    }
    
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
     * Get a persistent greenfoot property for this package.
     * @param propName   The name of the property whose value to get
     * @return   The string value of the property
     * 
     * @throws RemoteException
     * @throws PackageNotFoundException
     */
    public String getProperty(String propName)
        throws RemoteException, PackageNotFoundException
    {
        loadProperties();
        return pkgProperties.getProperty(propName);
    }
    
    /**
     * Set a persistent greenfoot property for this package.
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
        pkgProperties.setProperty(propName, value);
        OutputStream os = null;
        
        try {
            File propsFile = new File(getDir(), Greenfoot.GREENFOOT_PKG_NAME);
            os = new FileOutputStream(propsFile);
            pkgProperties.store(os, "Greenfoot properties");
        }
        catch (ProjectNotOpenException pnoe) {
            // can't happen.
        }
        finally {
            if (os != null) {
                os.close();
            }
        }
    }
    
    private void loadProperties()
        throws PackageNotFoundException, RemoteException
    {
        // More than one GPackage may be created which references the same package.
        // Properties for a single package must be shared for all instances of GPackage,
        // so we use a global map maintained by the Greenfoot class.
        if (pkgProperties == null) {
            pkgProperties = Greenfoot.getInstance().getPackageProperties(this);
        }
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
            GClass gClass = (GClass) classPool.get(rClass);
            if(gClass == null) {
                gClass = new GClass(rClass, this);
                classPool.put(rClass, gClass);
            }
            gClasses[i] = gClass;
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
        throws RemoteException, ProjectNotOpenException, PackageNotFoundException, MissingJavaFileException
    {
    	return new GClass(pkg.newClass(className), this);
    }
    
    public GClass getClass(String className) {
        GClass cls = (GClass) classPool.get(className);
        if(cls == null) {
            try {
                RClass rClass = pkg.getRClass(className);
                if(rClass != null) {
                    cls = new GClass(rClass, this);
                    classPool.put(rClass, cls);
                }                
            }
            catch (ProjectNotOpenException e) {
                e.printStackTrace();
            }
            catch (PackageNotFoundException e) {
                e.printStackTrace();
            }
            catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return cls;
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


}
