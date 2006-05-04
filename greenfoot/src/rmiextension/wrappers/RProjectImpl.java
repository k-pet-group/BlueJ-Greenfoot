package rmiextension.wrappers;

import java.io.File;
import java.rmi.RemoteException;

import bluej.extensions.BPackage;
import bluej.extensions.BProject;
import bluej.extensions.PackageAlreadyExistsException;
import bluej.extensions.ProjectNotOpenException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RProjectImpl.java 4088 2006-05-04 20:36:05Z mik $
 */
public class RProjectImpl extends java.rmi.server.UnicastRemoteObject
    implements RProject
{
    //	The BlueJ-package (from extensions) that is wrapped
    BProject bProject;

    public RProjectImpl(BProject bProject)
        throws java.rmi.RemoteException
    {
        super();
        this.bProject = bProject;
    }

    /**
     * @throws ProjectNotOpenException
     */
    public void close()
        throws ProjectNotOpenException
    {
        bProject.close();
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     */
    public File getDir()
        throws ProjectNotOpenException
    {
        return bProject.getDir();
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     */
    public String getName()
        throws ProjectNotOpenException
    {
        return bProject.getName();
    }

    /**
     * @param name
     * @return
     * @throws ProjectNotOpenException
     */
    public RPackage getPackage(String name)
        throws ProjectNotOpenException, RemoteException
    {
        BPackage bPackage = bProject.getPackage(name);
        RPackage wrapper = null;
        wrapper = WrapperPool.instance().getWrapper(bPackage);

        return wrapper;
    }

    public RPackage newPackage(String fullyQualifiedName)
        throws ProjectNotOpenException, PackageAlreadyExistsException, RemoteException
    {
        BPackage bPackage = bProject.newPackage(fullyQualifiedName);
        RPackage wrapper = null;
        wrapper = WrapperPool.instance().getWrapper(bPackage);

        return wrapper;
    }

    /**
     * @return
     * @throws ProjectNotOpenException
     */
    public RPackage[] getPackages()
        throws ProjectNotOpenException, RemoteException
    {
        BPackage[] packages = bProject.getPackages();
        int length = packages.length;
        RPackage[] wrapper = new RPackage[length];

        for (int i = 0; i < length; i++) {
            wrapper[i] = WrapperPool.instance().getWrapper(packages[i]);
        }

        return wrapper;
    }

    /**
     * @throws ProjectNotOpenException
     */
    public void save()
        throws ProjectNotOpenException
    {
        bProject.save();
    }
}
