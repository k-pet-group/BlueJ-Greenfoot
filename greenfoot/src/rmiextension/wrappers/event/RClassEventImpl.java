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

import rmiextension.wrappers.RClass;
import rmiextension.wrappers.WrapperPool;
import bluej.extensions.event.ClassEvent;

/**
 * Implementation of a remote class event. Wraps a local ClassEvent.
 * 
 * @author Davin McCall
 * @version $Id: RClassEventImpl.java 6216 2009-03-30 13:41:07Z polle $
 */
public class RClassEventImpl extends java.rmi.server.UnicastRemoteObject
    implements RClassEvent
{
    private ClassEvent event;
    
    /**
     * Construct a remote event wrapper for the supplied ClassEvent.
     * 
     * @throws RemoteException
     */
    public RClassEventImpl(ClassEvent event)
        throws RemoteException
    {
        super();
        this.event = event;
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.event.RClassEvent#getEventId()
     */
    public int getEventId()
        throws RemoteException
    {
        return event.getEventId();
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.event.RClassEvent#getRClass()
     */
    public RClass getRClass() throws RemoteException
    {
        return WrapperPool.instance().getWrapper(event.getBClass());
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.event.RClassEvent#isClassCompiled()
     */
    public boolean isClassCompiled() throws RemoteException
    {
        return event.isClassCompiled();
    }
    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.event.RClassEvent#getNewName()
     */
    public String getOldName()
        throws RemoteException
    {
        return event.getOldName();
    }
}
