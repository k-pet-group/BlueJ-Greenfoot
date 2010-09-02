/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package rmiextension.wrappers.event;

import java.rmi.RemoteException;

import rmiextension.wrappers.WrapperPool;
import bluej.extensions.BObject;
import bluej.extensions.BPackage;
import bluej.extensions.event.InvocationEvent;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RInvocationEventImpl.java 8234 2010-09-02 10:17:24Z nccb $
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
    public Class<?>[] getSignature()
        throws RemoteException
    {
        return invocationEvent.getSignature();
    }

}