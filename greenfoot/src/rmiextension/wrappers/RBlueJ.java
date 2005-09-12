package rmiextension.wrappers;

import java.awt.Frame;
import java.awt.Rectangle;
import java.io.File;
import java.rmi.RemoteException;

import rmiextension.wrappers.event.RCompileListener;
import rmiextension.wrappers.event.RInvocationListener;
import bluej.extensions.MenuGenerator;
import bluej.extensions.PreferenceGenerator;
import bluej.extensions.event.ApplicationListener;
import bluej.extensions.event.ExtensionEventListener;
import bluej.extensions.event.PackageListener;

/**
 * 
 * Interface for accessing BlueJ-functionality
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RBlueJ.java 3562 2005-09-12 15:51:02Z polle $
 */
public interface RBlueJ
    extends java.rmi.Remote
{
    /**
     * @param listener
     */
    public void addApplicationListener(ApplicationListener listener)
        throws RemoteException;

    /**
     * @param listener
     * @param project 
     */
    public void addCompileListener(RCompileListener listener, String project)
        throws RemoteException;

    /**
     * @param listener
     */
    public void addExtensionEventListener(ExtensionEventListener listener)
        throws RemoteException;

    public void addInvocationListener(RInvocationListener listener)
        throws RemoteException;

    /**
     * @param listener
     */
    public void addPackageListener(PackageListener listener)
        throws RemoteException;

    /**
     * @param property
     * @param def
     * @return
     */

    public String getBlueJPropertyString(String property, String def)
        throws RemoteException;

    /**
     * @return
     */
    public Frame getCurrentFrame()
        throws RemoteException;

    /**
     * @return
     */
    public RPackage getCurrentPackage()
        throws RemoteException;

    /**
     * @param property
     * @param def
     * @return
     */
    public String getExtensionPropertyString(String property, String def)
        throws RemoteException;

    /**
     * @param key
     * @return
     */
    public String getLabel(String key)
        throws RemoteException;

    /**
     * @return
     */
    public MenuGenerator getMenuGenerator()
        throws RemoteException;

    /**
     * @return
     */
    public RProject[] getOpenProjects()
        throws RemoteException;

    /**
     * @return
     */
    public PreferenceGenerator getPreferenceGenerator()
        throws RemoteException;

    /**
     * @return
     */
    public File getSystemLibDir()
        throws RemoteException;

    /**
     * @return
     */
    public File getUserConfigDir()
        throws RemoteException;

    /**
     * @param directory
     * @return
     */
    public RProject newProject(File directory)
        throws RemoteException;

    /**
     * @param directory
     * @return
     */
    public RProject openProject(String directory)
        throws RemoteException;

    /**
     * @param listener
     */
    public void removeApplicationListener(ApplicationListener listener)
        throws RemoteException;

    /**
     * @param listener
     */
    public void removeCompileListener(RCompileListener listener)
        throws RemoteException;

    /**
     * @param listener
     */
    public void removeExtensionEventListener(ExtensionEventListener listener)
        throws RemoteException;

    /**
     * @param listener
     */
    public void removeInvocationListener(RInvocationListener listener)
        throws RemoteException;

    /**
     * @param listener
     */
    public void removePackageListener(PackageListener listener)
        throws RemoteException;

    /**
     * @param property
     * @param value
     */
    public void setExtensionPropertyString(String property, String value)
        throws RemoteException;

    /**
     * @param menuGen
     */
    public void setMenuGenerator(MenuGenerator menuGen)
        throws RemoteException;

    /**
     * @param prefGen
     */
    public void setPreferenceGenerator(PreferenceGenerator prefGen)
        throws RemoteException;

    /**
     * Exits the entire application.
     * 
     * @throws RemoteException
     */
    public void exit()
        throws RemoteException;

    /**
     * @param bounds
     */
    public void frameResized(Rectangle bounds)
        throws RemoteException;

}