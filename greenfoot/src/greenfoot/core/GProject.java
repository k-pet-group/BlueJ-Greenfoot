package greenfoot.core;

import java.io.File;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import rmiextension.wrappers.RPackage;
import rmiextension.wrappers.RProject;
import bluej.extensions.PackageAlreadyExistsException;
import bluej.extensions.ProjectNotOpenException;

/**
 * 
 * Represents a project in greenfoot.
 * 
 * @author Poul Henriksen
 */
public class GProject
{
    private RProject rProject;
    private GPackage pkg;
    private Map classes = new HashMap();
    

    public GProject(RProject rmiProject, GPackage pkg) {
        this.rProject = rmiProject;
        this.pkg = pkg;
    }
    
    GProject(RProject rmiProject) throws ProjectNotOpenException, RemoteException
    {
        this.rProject = rmiProject;
        this.pkg = getDefaultPackage();
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
        if(pkg == null) {
            pkg = new GPackage(rProject.getPackage(""), this);
        }
        return pkg;
    }

    /**
     * returns the greenfoot package.
     * 
     */
    public GPackage getGreenfootPackage() throws ProjectNotOpenException, RemoteException {
        RPackage rPkg = rProject.getPackage("greenfoot");
        if(rPkg == null) {
            return null;
        } else {
            return new GPackage(rProject.getPackage("greenfoot"), this);
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
}
