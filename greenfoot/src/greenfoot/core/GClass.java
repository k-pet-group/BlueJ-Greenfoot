package greenfoot.core;

import java.rmi.RemoteException;

import rmiextension.wrappers.RClass;
import rmiextension.wrappers.RConstructor;
import rmiextension.wrappers.RField;
import bluej.extensions.BField;
import bluej.extensions.BMethod;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;


/**
 * Represents a class in Greenfoot. This class wraps the RMI class and contains
 * some extra functionality. The main reason for createing this class is to have
 * a place to store information about inheritance realtions between classes that
 * have not been compiled.
 * 
 * @author Poul Henriksen
 * 
 */
public class GClass
{
    private RClass rmiClass;
    private GPackage pkg;

    public GClass(RClass cls, GPackage pkg)
    {
        this.rmiClass = cls;
        this.pkg = pkg;
    }

    public void compile(boolean waitCompileEnd)
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException, CompilationNotStartedException
    {
        rmiClass.compile(waitCompileEnd);
    }

    public void edit()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        rmiClass.edit();
    }

    public RConstructor getConstructor(Class[] signature)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getConstructor(signature);
    }

    public RConstructor[] getConstructors()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getConstructors();
    }

    public BMethod getDeclaredMethod(String methodName, Class[] params)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getDeclaredMethod(methodName, params);
    }

    public BMethod[] getDeclaredMethods()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getDeclaredMethods();
    }

    public RField getField(String fieldName)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getField(fieldName);
    }

    public BField[] getFields()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getFields();
    }

    public Class getJavaClass()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getJavaClass();
    }

    public GPackage getPackage()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        return pkg;
    }

    public String getQualifiedName()
        throws RemoteException
    {
        return rmiClass.getQualifiedName();
    }

    public String getSuperclassName()
        throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException, RemoteException
    {
        return rmiClass.getSuperclassName();
    }

    public String getToString()
        throws RemoteException
    {
        return rmiClass.getToString();
    }

    public boolean isCompiled()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        return rmiClass.isCompiled();
    }

    public boolean isSubclassOf(String className)
        throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException, RemoteException
    {
        return rmiClass.isSubclassOf(className);
    }
}
