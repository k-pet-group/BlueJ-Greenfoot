package rmiextension.wrappers.event;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RCompileListener.java 3124 2004-11-18 16:08:48Z polle $
 */
public interface RCompileListener
    extends Remote
{
    /**
     * This method will be called when a compilation starts. If a long operation
     * must be performed you should start a Thread.
     */
    public void compileStarted(RCompileEvent event)
        throws RemoteException;

    /**
     * This method will be called when there is a report of a compile error. If
     * a long operation must be performed you should start a Thread.
     */
    public void compileError(RCompileEvent event)
        throws RemoteException;

    /**
     * This method will be called when there is a report of a compile warning.
     * If a long operation must be performed you should start a Thread.
     */
    public void compileWarning(RCompileEvent event)
        throws RemoteException;

    /**
     * This method will be called when the compile ends successfully. If a long
     * operation must be performed you should start a Thread.
     */
    public void compileSucceeded(RCompileEvent event)
        throws RemoteException;

    /**
     * This method will be called when the compile fails. If a long operation
     * must be performed you should start a Thread.
     */
    public void compileFailed(RCompileEvent event)
        throws RemoteException;
}