package rmiextension.wrappers.event;

import java.rmi.Remote;
import java.rmi.RemoteException;

import bluej.extensions.BPackage;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RInvocationEvent.java 3124 2004-11-18 16:08:48Z polle $
 */
public interface RInvocationEvent
    extends Remote
{
    /**
     * @return
     */
    public String getClassName()
        throws RemoteException;

    /**
     * @return
     */
    public int getInvocationStatus()
        throws RemoteException;

    /**
     * @return
     */
    public String getMethodName()
        throws RemoteException;

    /**
     * @return
     */
    public String getObjectName()
        throws RemoteException;

    /**
     * @return
     */
    public BPackage getPackage()
        throws RemoteException;

    /**
     * @return
     */
    public String[] getParameters()
        throws RemoteException;

    /**
     * @return
     */
    public Object getResult()
        throws RemoteException;

    /**
     * @return
     */
    public Class[] getSignature()
        throws RemoteException;
}