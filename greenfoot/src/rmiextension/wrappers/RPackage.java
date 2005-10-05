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
 * @version $Id: RPackage.java 3648 2005-10-05 16:22:34Z polle $
 */
public interface RPackage
    extends java.rmi.Remote
{

    public abstract void compile(boolean waitCompileEnd)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException, CompilationNotStartedException;

    public abstract void compileAll(boolean waitCompileEnd)
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

    /**
     * Invoke a constructor. Put the resulting object on the bench.<p>
     * 
     * Return is the compiler error message preceded by '!' in the case of
     * a compile time error, or the name of the constructed object, or null
     * if a run-time error occurred.
     * 
     * @param className   The fully qualified name of the class to instantiate
     * @param argTypes    The (raw) argument types of the constructor
     * @param args        The argument strings to use
     * @return   The name of the constructed object (see notes).
     */
    public String invokeConstructor(String className, String [] argTypes, String [] args)
        throws RemoteException;
    
    /**
     * Invoke a static method.
     * 
     * Return is the compiler error message preceded by '!' in the case of
     * a compile time error, or the name of the constructed object, or null
     * if a run-time error occurred.
     * 
     * @param className  The class for which to invoke the method
     * @param methodName The name of the method
     * @param argTypes   The argument types of the method (class names)
     * @param args       The argument strings to use
     * @return   The name of the returned object (see notes above).
     */
    public String invokeMethod(String className, String methodName, String [] argTypes, String [] args)
        throws RemoteException;

    public void close() throws RemoteException;

}
