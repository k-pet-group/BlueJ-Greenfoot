package rmiextension.wrappers;

import java.rmi.RemoteException;

import javax.swing.SwingUtilities;

import bluej.extensions.*;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.editor.Editor;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RClassImpl.java 3262 2005-01-12 03:30:49Z davmac $
 */
public class RClassImpl extends java.rmi.server.UnicastRemoteObject
    implements RClass
{

    BClass bClass;

    /**
     * @param class1
     */
    public RClassImpl(BClass bClass)
        throws RemoteException
    {
        this.bClass = bClass;
        if (bClass == null) {
            throw new NullPointerException("Argument can't be null");
        }
    }

    public RClassImpl()
        throws RemoteException
    {

    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public void compile(boolean waitCompileEnd)
        throws ProjectNotOpenException, PackageNotFoundException, CompilationNotStartedException
    {
        bClass.compile(waitCompileEnd);
    }

    public void edit()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {
        final Editor editor = bClass.getEditor();
        Thread t = new Thread() {
            public void run()
            {
                editor.setVisible(true);
            }
        };
        SwingUtilities.invokeLater(t);
    }

    /**
     * @param signature
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public RConstructor getConstructor(Class[] signature)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {

        BConstructor bConstructor = bClass.getConstructor(signature);

        RConstructor rConstructor = WrapperPool.instance().getWrapper(bConstructor);
        return rConstructor;
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public RConstructor[] getConstructors()
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {

        BConstructor[] bConstructors = bClass.getConstructors();
        int length = bConstructors.length;
        RConstructor[] rConstructors = new RConstructor[length];
        for (int i = 0; i < length; i++) {
            rConstructors[i] = WrapperPool.instance().getWrapper(bConstructors[i]);
        }

        return rConstructors;
    }

    /**
     * @param methodName
     * @param params
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public BMethod getDeclaredMethod(String methodName, Class[] params)
        throws ProjectNotOpenException, ClassNotFoundException
    {
        return null;
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public BMethod[] getDeclaredMethods()
        throws ProjectNotOpenException, ClassNotFoundException
    {
        return bClass.getDeclaredMethods();

    }

    /**
     * @param fieldName
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public RField getField(String fieldName)
        throws ProjectNotOpenException, ClassNotFoundException, RemoteException
    {

        BField wrapped = bClass.getField(fieldName);
        RField wrapper = WrapperPool.instance().getWrapper(wrapped);
        return wrapper;

    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public BField[] getFields()
        throws ProjectNotOpenException, ClassNotFoundException
    {
        return bClass.getFields();
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws ClassNotFoundException
     */
    public Class getJavaClass()
        throws ProjectNotOpenException, ClassNotFoundException
    {
        return bClass.getJavaClass();
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public RPackage getPackage()
        throws ProjectNotOpenException, PackageNotFoundException, RemoteException
    {

        BPackage wrapped = bClass.getPackage();
        RPackage wrapper = WrapperPool.instance().getWrapper(wrapped);
        return wrapper;
    }

    /**
     * Gets the superclass of this class if it is a part of the project.
     * 
     * @see #getSuperclassName()
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     * @throws ClassNotFoundException
     */
    public RClass getSuperclass()
        throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException, RemoteException
    {
        BClass wrapped = bClass.getSuperclass();
        RClass wrapper = WrapperPool.instance().getWrapper(wrapped);
        return wrapper;
    }

    /**
     * Gets the name of the superclass.
     * 
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     * @throws ClassNotFoundException
     */
    public String getSuperclassName()
        throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException, RemoteException
    {
        return bClass.getSuperclassName();
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     */
    public boolean isCompiled()
        throws ProjectNotOpenException, PackageNotFoundException
    {
        return bClass.isCompiled();
    }

    public String getToString()
    {
        return bClass.getQualifiedName();
    }

    public String getQualifiedName()
        throws RemoteException
    {
        return bClass.getQualifiedName();
    }

    /**
     * Checks if the class represented by this object is an instanceof
     * className.
     * 
     * @param className
     *            Fully qualified classname
     * @return
     * @throws ProjectNotOpenException
     * @throws PackageNotFoundException
     * @throws ClassNotFoundException
     * @throws RemoteException
     */
    public boolean isSubclassOf(String className)
        throws ProjectNotOpenException, PackageNotFoundException, ClassNotFoundException, RemoteException
    {

        RClass superclass = this;

        //Recurse through superclasses
        while (superclass != null) {
            String superclassName = superclass.getSuperclassName();
            //TODO bug: also matches partly. ex Beeper and SubBeeper
            if (superclassName != null && className.endsWith(superclassName)) {
                return true;
            }
            superclass = superclass.getSuperclass();
        }

        return false;
    }

    // No longer needed. TODO: remove.
//    public MenuSerializer getMenu()
//        throws RemoteException
//    {
//
//        try {
//            JPopupMenu menu = (JPopupMenu) bClass.getMenu();
//            return new MenuSerializer(menu);
//        }
//        catch (ProjectNotOpenException e) {
//            e.printStackTrace();
//        }
//        catch (PackageNotFoundException e) {
//            e.printStackTrace();
//        }
//        catch (ClassNotFoundException e) {
//            //e.printStackTrace();
//            //to be expected
//        }
//        return null;
//    }

}