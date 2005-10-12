package rmiextension.wrappers;

import java.awt.Frame;
import java.awt.Rectangle;
import java.io.File;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import rmiextension.wrappers.event.RCompileListener;
import rmiextension.wrappers.event.RCompileListenerWrapper;
import rmiextension.wrappers.event.RInvocationListener;
import rmiextension.wrappers.event.RInvocationListenerWrapper;
import bluej.extensions.BProject;
import bluej.extensions.BlueJ;
import bluej.extensions.MenuGenerator;
import bluej.extensions.PreferenceGenerator;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.event.ApplicationListener;
import bluej.extensions.event.ExtensionEventListener;
import bluej.extensions.event.PackageListener;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RBlueJImpl.java 3664 2005-10-12 10:21:20Z polle $
 */
public class RBlueJImpl extends java.rmi.server.UnicastRemoteObject
    implements RBlueJ
{
    BlueJ blueJ;

    Map listeners = new Hashtable();

    private transient final static Logger logger = Logger.getLogger("greenfoot");

    public RBlueJImpl(BlueJ blueJ)
        throws RemoteException
    {
        super();
        this.blueJ = blueJ;
    }

    /**
     * @param listener
     */
    public void addApplicationListener(ApplicationListener listener)
    {
        blueJ.addApplicationListener(listener);
    }

    /**
     * @param listener
     */
    public void addCompileListener(RCompileListener listener, String projectName)
    {
        BProject[] projects = blueJ.getOpenProjects();
        BProject project = null;
        for (int i = 0; i < projects.length; i++) {
            BProject prj = projects[i];
            try {
                if(prj.getName().equals(projectName)) {
                    project = prj;
                }
            }
            catch (ProjectNotOpenException e) {
                e.printStackTrace();
            }
        }
        RCompileListenerWrapper wrapper = new RCompileListenerWrapper(listener, project);
        listeners.put(listener, wrapper);
        blueJ.addCompileListener(wrapper);
    }

    /**
     * @param listener
     */
    public void addExtensionEventListener(ExtensionEventListener listener)
    {
        blueJ.addExtensionEventListener(listener);
    }

    /**
     * @param listener
     */
    public void addInvocationListener(RInvocationListener listener)
    {
        RInvocationListenerWrapper wrapper = new RInvocationListenerWrapper(listener);
        listeners.put(listener, wrapper);
        blueJ.addInvocationListener(wrapper);
    }

    /**
     * @param listener
     */
    public void addPackageListener(PackageListener listener)
    {
        blueJ.addPackageListener(listener);
    }

    /**
     * @param property
     * @param def
     * @return
     */
    public String getBlueJPropertyString(String property, String def)
    {
        return blueJ.getBlueJPropertyString(property, def);
    }

    /**
     * @return
     */
    public Frame getCurrentFrame()
    {
        return blueJ.getCurrentFrame();
    }

    /**
     * @return
     */
    public RPackage getCurrentPackage()
        throws RemoteException
    {
        return WrapperPool.instance().getWrapper(blueJ.getCurrentPackage());

    }

    /**
     * @param property
     * @param def
     * @return
     */
    public String getExtensionPropertyString(String property, String def)
    {
        return blueJ.getExtensionPropertyString(property, def);
    }

    /**
     * @param key
     * @return
     */
    public String getLabel(String key)
    {
        return blueJ.getLabel(key);
    }

    /**
     * @return
     */
    public MenuGenerator getMenuGenerator()
    {
        return blueJ.getMenuGenerator();
    }

    /**
     * @return
     */
    public RProject[] getOpenProjects()
        throws RemoteException
    {

        BProject[] bProjects = blueJ.getOpenProjects();
        int length = bProjects.length;
        RProject[] rProjects = new RProject[length];
        for (int i = 0; i < length; i++) {
            rProjects[i] = WrapperPool.instance().getWrapper(bProjects[i]);
        }

        return rProjects;
    }

    /**
     * @return
     */
    public PreferenceGenerator getPreferenceGenerator()
    {
        return blueJ.getPreferenceGenerator();
    }

    /**
     * @return
     */
    public File getSystemLibDir()
    {
        File f = blueJ.getSystemLibDir();
        //The getAbsoluteFile() fixes a weird bug on win using jdk1.4.2_06
        return f.getAbsoluteFile();
    }

    /**
     * @return
     */
    public File getUserConfigDir()
    {
        return blueJ.getUserConfigDir();
    }

    /**
     * @param directory
     * @return
     */
    public RProject newProject(File directory)
        throws RemoteException
    {
        BProject wrapped = blueJ.newProject(directory);
        RProject wrapper = WrapperPool.instance().getWrapper(wrapped);
        return wrapper;
    }

    /**
     * @param directory
     * @return
     */
    public RProject openProject(String directory)
        throws RemoteException
    {

        return new RProjectImpl(blueJ.openProject(new File(directory)));
    }

    /**
     * @param listener
     */
    public void removeApplicationListener(ApplicationListener listener)
    {
        blueJ.removeApplicationListener(listener);
    }

    /**
     * @param listener
     */
    public void removeCompileListener(RCompileListener listener)
    {
        RCompileListenerWrapper wrapper = (RCompileListenerWrapper) listeners.get(listener);
        listeners.remove(listener);
        blueJ.removeCompileListener(wrapper);
    }

    /**
     * @param listener
     */
    public void removeExtensionEventListener(ExtensionEventListener listener)
    {
        blueJ.removeExtensionEventListener(listener);
    }

    /**
     * @param listener
     */
    public void removeInvocationListener(RInvocationListener listener)
    {
        RInvocationListenerWrapper wrapper = (RInvocationListenerWrapper) listeners.get(listener);
        listeners.remove(listener);
        blueJ.removeInvocationListener(wrapper);
    }

    /**
     * @param listener
     */
    public void removePackageListener(PackageListener listener)
    {
        blueJ.removePackageListener(listener);
    }

    /**
     * @param property
     * @param value
     */
    public void setExtensionPropertyString(String property, String value)
    {
        blueJ.setExtensionPropertyString(property, value);
    }

    /**
     * @param menuGen
     */
    public void setMenuGenerator(MenuGenerator menuGen)
    {
        blueJ.setMenuGenerator(menuGen);
    }

    /**
     * @param prefGen
     */
    public void setPreferenceGenerator(PreferenceGenerator prefGen)
    {
        blueJ.setPreferenceGenerator(prefGen);
    }

    /*
     * (non-Javadoc)
     * 
     * @see dk.sdu.mip.dit.remote.RBlueJ#exit()
     */
    public void exit()
        throws RemoteException
    {
        PkgMgrFrame pkgMgrFrame = (PkgMgrFrame) blueJ.getCurrentFrame();
        pkgMgrFrame.doClose(false);
    }

    private void closeAllProjects()
    {
        try {
            RProject[] projects = getOpenProjects();
            for (int i = 0; i < projects.length; i++) {
                projects[i].close();
            }
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see greenfoot.remote.RBlueJ#frameResized(java.awt.Rectangle)
     */
    public void frameResized(Rectangle bounds)
        throws RemoteException
    {
     //   DialogManager.setParentBounds(bounds);
    }

}