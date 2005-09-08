package rmiextension.wrappers;

import java.io.File;
import java.rmi.RemoteException;

import bluej.extensions.*;
import bluej.extensions.ClassNotFoundException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RClass.java 3553 2005-09-08 15:24:52Z polle $
 */
public interface RClass
    extends java.rmi.Remote
{
    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract void compile(boolean waitCompileEnd)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException, CompilationNotStartedException;

    public abstract void edit()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * @param signature
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public abstract RConstructor getConstructor(Class[] signature)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public abstract RConstructor[] getConstructors()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException;

    /**
     * @param methodName
     * @param params
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public abstract BMethod getDeclaredMethod(String methodName, Class[] params)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public abstract BMethod[] getDeclaredMethods()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException;

    /**
     * @param fieldName
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public abstract RField getField(String fieldName)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public abstract BField[] getFields()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public abstract Class getJavaClass()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract RPackage getPackage()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     * @throws ClassNotFoundException
     */
    public abstract RClass getSuperclass()
        throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     * @throws ClassNotFoundException
     */
    public abstract String getSuperclassName()
        throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException, RemoteException;

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public abstract boolean isCompiled()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;

    /**
     * This should actually have been the toString() method, but we cannot add
     * an exception to an inherited method. And to get RMI to work, it must
     * throw RemoteException.
     * 
     * @return
     */
    public String getToString()
        throws RemoteException;

    /**
     * @return
     */
    public abstract String getQualifiedName()
        throws RemoteException;

  
    public File getJavaFile()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException;
    //public MenuSerializer getMenu()
    //    throws RemoteException;
}