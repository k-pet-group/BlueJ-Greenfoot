package rmiextension.wrappers;

import java.awt.Rectangle;
import java.io.File;
import java.rmi.RemoteException;

import rmiextension.wrappers.event.RClassListener;
import rmiextension.wrappers.event.RCompileListener;
import rmiextension.wrappers.event.RInvocationListener;
import bluej.extensions.MenuGenerator;
import bluej.extensions.PreferenceGenerator;
import bluej.extensions.event.ApplicationListener;
import bluej.extensions.event.ExtensionEventListener;

/**
 * 
 * Interface for accessing BlueJ-functionality
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RBlueJ.java 4261 2006-05-15 10:54:18Z davmac $
 */
public interface RBlueJ
    extends java.rmi.Remote
{
    /**
     * Register an Application listener
     */
    public void addApplicationListener(ApplicationListener listener)
        throws RemoteException;

    /**
     * Register a Compile event listener
     */
    public void addCompileListener(RCompileListener listener, String project)
        throws RemoteException;

    /**
     * Register an invocation event listener
     */
    public void addInvocationListener(RInvocationListener listener)
        throws RemoteException;

    /**
     * Register a remote class event listener
     */
    public void addClassListener(RClassListener listener)
        throws RemoteException;
    
    
    /**
     * Get a BlueJ property value
     */
    public String getBlueJPropertyString(String property, String def)
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
    public void removeInvocationListener(RInvocationListener listener)
        throws RemoteException;

    /**
     * De-register a remote class event listener.
     */
    public void removeClassListener(RClassListener listener)
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