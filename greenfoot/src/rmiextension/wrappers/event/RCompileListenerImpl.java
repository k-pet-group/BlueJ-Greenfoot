package rmiextension.wrappers.event;

import java.rmi.RemoteException;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RCompileListenerImpl.java 3124 2004-11-18 16:08:48Z polle $
 */
public abstract class RCompileListenerImpl extends java.rmi.server.UnicastRemoteObject
    implements RCompileListener
{

    public RCompileListenerImpl()
        throws RemoteException
    {
        super();
    }

    /**
     * This method will be called when a compilation starts. If a long operation
     * must be performed you should start a Thread.
     */
    public abstract void compileStarted(RCompileEvent event)
        throws RemoteException;

    /**
     * This method will be called when there is a report of a compile error. If
     * a long operation must be performed you should start a Thread.
     */
    public abstract void compileError(RCompileEvent event)
        throws RemoteException;

    /**
     * This method will be called when there is a report of a compile warning.
     * If a long operation must be performed you should start a Thread.
     */
    public abstract void compileWarning(RCompileEvent event)
        throws RemoteException;

    /**
     * This method will be called when the compile ends successfully. If a long
     * operation must be performed you should start a Thread.
     */
    public abstract void compileSucceeded(RCompileEvent event)
        throws RemoteException;

    /**
     * This method will be called when the compile fails. If a long operation
     * must be performed you should start a Thread.
     */
    public abstract void compileFailed(RCompileEvent event)
        throws RemoteException;
}