package rmiextension.wrappers;

import java.io.File;
import java.rmi.RemoteException;

import rmiextension.wrappers.event.RClassListener;
import rmiextension.wrappers.event.RCompileListener;
import rmiextension.wrappers.event.RInvocationListener;

/**
 * 
 * Interface for accessing BlueJ-functionality
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RBlueJ.java 4349 2006-06-12 03:07:04Z davmac $
 */
public interface RBlueJ
    extends java.rmi.Remote
{
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
     * Get a BlueJ extensions property value
     * 
     * @param property  The property whose value to retrieve
     * @param def       The default value to return
     * @return   The property value
     */
    public String getExtensionPropertyString(String property, String def)
        throws RemoteException;

    /**
     * Get a language label
     * @param key  The label key
     * @return     The label value
     */
    public String getLabel(String key)
        throws RemoteException;

    /**
     * Get a list of all open projects.
     */
    public RProject[] getOpenProjects()
        throws RemoteException;

    /**
     * Get the Bluej "lib" dir.
     */
    public File getSystemLibDir()
        throws RemoteException;

    /**
     * Create and open a new Project
     * @param directory  The directory to create the project in
     * @return   A reference to the newly created project
     */
    public RProject newProject(File directory)
        throws RemoteException;

    /**
     * Open an existing project
     * @param directory  The directory containing the project to open
     * @return  A reference to the project
     */
    public RProject openProject(String directory)
        throws RemoteException;

    /**
     * Remove a compile listener.
     * @param listener  The listener to remove
     */
    public void removeCompileListener(RCompileListener listener)
        throws RemoteException;

    /**
     * Remove an invocation listener.
     * @param listener  The listener to remove
     */
    public void removeInvocationListener(RInvocationListener listener)
        throws RemoteException;

    /**
     * De-register a remote class event listener.
     */
    public void removeClassListener(RClassListener listener)
        throws RemoteException;

    /**
     * Set an extension property value.
     * @param property  The property key
     * @param value     The value to set
     */
    public void setExtensionPropertyString(String property, String value)
        throws RemoteException;

    /**
     * Exits the entire application.
     * 
     * @throws RemoteException
     */
    public void exit()
        throws RemoteException;

}