package rmiextension.wrappers;

import java.io.File;
import java.rmi.RemoteException;

import bluej.extensions.BObject;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.MissingJavaFileException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
/**
 * The interface for a package.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RPackage.java 3124 2004-11-18 16:08:48Z polle $
 */
public interface RPackage
    extends java.rmi.Remote
{

    public abstract void compile(boolean waitCompileEnd)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException, CompilationNotStartedException;

    /**
     * @param name
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract RClass getRClass(String name)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract RClass[] getRClasses()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract String getName()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * @param instanceName
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract RObject getObject(String instanceName)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract BObject[] getObjects()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     */
    public abstract RProject getProject()
        throws ProjectNotOpenException, RemoteException;

    /**
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract void reload()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    public abstract RClass getSelectedClass()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * Returns the directory where this package is stored.
     * 
     * @throws ProjectNotOpenException
     *             if the project this package is part of has been closed by the
     *             user.
     * @throws PackageNotFoundException
     *             if the package has been deleted by the user.
     */
    public abstract File getDir()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * Creates a new Class with the given name. The class name must not be a
     * fully qualified name, and the .java file must already exist.
     * 
     * @throws ProjectNotOpenException
     *             if the project this package is part of has been closed by the
     *             user.
     * @throws PackageNotFoundException
     *             if the package has been deleted by the user.
     * @throws MissingJavaFileException
     *             if the .java file for the new class does not exist.
     */
    public abstract RClass newClass(String className)
        throws RemoteException, ProjectNotOpenException, PackageNotFoundException, MissingJavaFileException;

}