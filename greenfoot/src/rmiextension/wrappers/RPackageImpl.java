package rmiextension.wrappers;

import java.io.File;
import java.rmi.RemoteException;

import bluej.extensions.BClass;
import bluej.extensions.BObject;
import bluej.extensions.BPackage;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.MissingJavaFileException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * This class is a wrapper for a BlueJ package
 * 
 * @see bluej.extensions.BPackage
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RPackageImpl.java 3124 2004-11-18 16:08:48Z polle $
 */
public class RPackageImpl extends java.rmi.server.UnicastRemoteObject
    implements RPackage
{

    //The BlueJ-package (from extensions) that is wrapped
    BPackage bPackage;

    public RPackageImpl(BPackage bPackage)
        throws java.rmi.RemoteException
    {
        super();
        this.bPackage = bPackage;
    }

    /////////////////////
    // Wrapper Methods
    /////////////////////

    /**
     * @param forceAll
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public void compile(boolean waitCompileEnd)
        throws ProjectNotOpenException, PackageNotFoundException, CompilationNotStartedException
    {
        bPackage.compile(waitCompileEnd);
    }

    /**
     * @param name
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public RClass getRClass(String name)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        return WrapperPool.instance().getWrapper(bPackage.getBClass(name));

    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public RClass[] getRClasses()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {

        BClass[] bClasses = bPackage.getClasses();
        int length = bClasses.length;
        RClass[] rClasses = new RClass[length];
        for (int i = 0; i < length; i++) {
            rClasses[i] = WrapperPool.instance().getWrapper(bClasses[i]);
        }

        return rClasses;
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public String getName()
        throws ProjectNotOpenException, PackageNotFoundException
    {
        return bPackage.getName();
    }

    /**
     * @param instanceName
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public RObject getObject(String instanceName)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {

        BObject wrapped = bPackage.getObject(instanceName);
        RObject wrapper = WrapperPool.instance().getWrapper(wrapped);
        return wrapper;

    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public BObject[] getObjects()
        throws ProjectNotOpenException, PackageNotFoundException
    {
        return bPackage.getObjects();
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     */
    public RProject getProject()
        throws RemoteException, ProjectNotOpenException
    {
        return WrapperPool.instance().getWrapper(bPackage.getProject());
    }

    /**
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public void reload()
        throws ProjectNotOpenException, PackageNotFoundException
    {
        bPackage.reload();
    }

    public RClass getSelectedClass()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        //TODO Hack for testing:
        return getRClasses()[0];
        //return selectedClass;
    }

    /*
     * (non-Javadoc)
     * 
     * @see greenfoot.remote.RPackage#getDir()
     */
    public File getDir()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        return bPackage.getDir();
    }

    /*
     * (non-Javadoc)
     * 
     * @see greenfoot.remote.RPackage#newClass(java.lang.String)
     */
    public RClass newClass(String className)
        throws RemoteException, ProjectNotOpenException, PackageNotFoundException, MissingJavaFileException
    {
        BClass wrapped = bPackage.newClass(className);
        RClass wrapper = WrapperPool.instance().getWrapper(wrapped);
        bPackage.reload();
        return wrapper;
    }

}