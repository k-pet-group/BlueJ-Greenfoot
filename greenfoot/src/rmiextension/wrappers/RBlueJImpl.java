package rmiextension.wrappers;

import java.awt.Rectangle;
import java.io.File;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Map;

import rmiextension.ProjectManager;
import rmiextension.wrappers.event.RClassListener;
import rmiextension.wrappers.event.RClassListenerWrapper;
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
import bluej.extensions.event.ClassListener;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RBlueJImpl.java 4261 2006-05-15 10:54:18Z davmac $
 */
public class RBlueJImpl extends java.rmi.server.UnicastRemoteObject
    implements RBlueJ
{
    BlueJ blueJ;

    Map listeners = new Hashtable();

    
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
        // TODO this is not robust if more than one project with the
        // same name is open
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
    public void addInvocationListener(RInvocationListener listener)
    {
        RInvocationListenerWrapper wrapper = new RInvocationListenerWrapper(listener);
        listeners.put(listener, wrapper);
        blueJ.addInvocationListener(wrapper);
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#addClassListener(rmiextension.wrappers.event.RClassListener)
     */
    public void addClassListener(RClassListener listener) throws RemoteException
    {
        ClassListener wrapper = new RClassListenerWrapper(listener);
        listeners.put(listener, wrapper);
        blueJ.addClassListener(wrapper);
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
        ProjectManager.instance().addNewProject(directory);
        BProject wrapped = blueJ.newProject(directory);
        RProject wrapper = WrapperPool.instance().getWrapper(wrapped);
        ProjectManager.instance().removeNewProject(directory);
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
    public void removeInvocationListener(RInvocationListener listener)
    {
        RInvocationListenerWrapper wrapper = (RInvocationListenerWrapper) listeners.get(listener);
        listeners.remove(listener);
        blueJ.removeInvocationListener(wrapper);
    }

    /* (non-Javadoc)
     * @see rmiextension.wrappers.RBlueJ#removeClassListener(rmiextension.wrappers.event.RClassListener)
     */
    public void removeClassListener(RClassListener listener) throws RemoteException
    {
        ClassListener wrapper = (ClassListener) listeners.remove(listener);
        blueJ.removeClassListener(wrapper);
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
        PkgMgrFrame [] frames = PkgMgrFrame.getAllFrames();
        for (int i = 0; i < frames.length; i++) {
            frames[i].doClose(false);
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