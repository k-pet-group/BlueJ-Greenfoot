package rmiextension.wrappers.event;

import java.rmi.RemoteException;

import rmiextension.wrappers.WrapperPool;
import bluej.extensions.BObject;
import bluej.extensions.BPackage;
import bluej.extensions.event.InvocationEvent;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RInvocationEventImpl.java 3124 2004-11-18 16:08:48Z polle $
 */
public class RInvocationEventImpl extends java.rmi.server.UnicastRemoteObject
    implements RInvocationEvent
{

    private InvocationEvent invocationEvent;

    /**
     * @throws RemoteException
     */
    protected RInvocationEventImpl(InvocationEvent invocationEvent)
        throws RemoteException
    {
        super();
        this.invocationEvent = invocationEvent;
    }

    /**
     * @return
     */
    public String getClassName()
        throws RemoteException
    {
        return invocationEvent.getClassName();
    }

    /**
     * @return
     */
    public int getInvocationStatus()
        throws RemoteException
    {
        return invocationEvent.getInvocationStatus();
    }

    /**
     * @return
     */
    public String getMethodName()
        throws RemoteException
    {
        return invocationEvent.getMethodName();
    }

    /**
     * @return
     */
    public String getObjectName()
        throws RemoteException
    {
        return invocationEvent.getObjectName();
    }

    /**
     * @return
     */
    public BPackage getPackage()
        throws RemoteException
    {
        return invocationEvent.getPackage();
    }

    /**
     * @return
     */
    public String[] getParameters()
        throws RemoteException
    {
        return invocationEvent.getParameters();
    }

    /**
     * @return
     */
    public Object getResult()
        throws RemoteException
    {
        Object result = invocationEvent.getResult();
        if (result == null) {
            return null;
        }

        if (result instanceof BObject) {
            result = WrapperPool.instance().getWrapper((BObject) result);
        }

        return result;
    }

    /**
     * @return
     */
    public Class[] getSignature()
        throws RemoteException
    {
        return invocationEvent.getSignature();
    }

}