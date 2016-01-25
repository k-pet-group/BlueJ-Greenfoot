/*
 This file is part of the Greenfoot program.
 Copyright (C) 2016  Poul Henriksen and Michael Kolling

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

/**
 * Created by neil on 25/01/16.
 */
public abstract class RApplicationListenerImpl extends java.rmi.server.UnicastRemoteObject
    implements RApplicationListener
{
    public RApplicationListenerImpl() throws RemoteException
    {
        super();
    }

    @Override
    public abstract void dataSubmissionFailed() throws RemoteException;
}
