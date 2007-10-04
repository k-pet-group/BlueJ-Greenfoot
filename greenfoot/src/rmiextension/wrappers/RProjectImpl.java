package rmiextension.wrappers;

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import rmiextension.wrappers.event.RProjectListener;
import bluej.extensions.BPackage;
import bluej.extensions.BProject;
import bluej.extensions.PackageAlreadyExistsException;
import bluej.extensions.ProjectNotOpenException;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ReadmeTarget;
import bluej.utility.Debug;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RProjectImpl.java 5288 2007-10-04 04:47:23Z davmac $
 */
public class RProjectImpl extends java.rmi.server.UnicastRemoteObject
    implements RProject
{
    /**	The BlueJ-package (from extensions) that is wrapped */
    private BProject bProject;
    
    private List listeners = new ArrayList();

    /**
     * Construct an RProjectImpl - generally only should be called from
     * WrapperPool (use WrapperPool.instance().getWrapper(...)).
     * 
     * @param bProject  The project to wrap
     * @throws java.rmi.RemoteException
     */
    public RProjectImpl(BProject bProject)
        throws java.rmi.RemoteException
    {
        super();
        this.bProject = bProject;
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RProject#close()
     */
    public void close()
    {
        notifyClosing();
        
        try {
            bProject.close();
        }
        catch (ProjectNotOpenException pnoe) {
            // this isn't a big deal; after all, we were trying to close
            // the project...
        }
    }
    
    /**
     * Inform listeners that this project will close. This should be called if the
     * project will be closed other than by calling RProjectImpl.close().
     */
    public void notifyClosing()
    {
        List listeners = new ArrayList(this.listeners);
        Iterator i = listeners.iterator();
        while (i.hasNext()) {
            RProjectListener listener = (RProjectListener) i.next();
            try {
                listener.projectClosing();
            }
            catch (RemoteException re) {
                Debug.reportError("Error when scenario closing: ", re);
            }
        }
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
     * Request a save of all open files in the project.
     * @throws ProjectNotOpenException
     */
    public void save()
        throws ProjectNotOpenException
    {
        bProject.save();
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.RProject#openReadmeEditor()
     */
    public void openReadmeEditor()
        throws ProjectNotOpenException
    {
        Project thisProject = Project.getProject(bProject.getDir());
        bluej.pkgmgr.Package defaultPackage = thisProject.getPackage("");
        ReadmeTarget readmeTarget = defaultPackage.getReadmeTarget();
        readmeTarget.open();
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.RProject#addListener(rmiextension.wrappers.event.RProjectListener)
     */
    public void addListener(RProjectListener listener)
        throws RemoteException
    {
        listeners.add(listener);
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.RProject#removeListener(rmiextension.wrappers.event.RProjectListener)
     */
    public void removeListener(RProjectListener listener)
        throws RemoteException
    {
        listeners.remove(listener);
    }
}
