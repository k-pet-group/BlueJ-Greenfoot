/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2012  Poul Henriksen and Michael Kolling 
 
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
package rmiextension.wrappers;

import java.io.IOException;
import java.io.Writer;
import java.rmi.RemoteException;

import bluej.utility.Debug;

/**
 * Remote print stream implementation, forwards printed text to the debug log.
 * 
 * @author Davin McCall
 */
public class RPrintStreamImpl extends java.rmi.server.UnicastRemoteObject
    implements RPrintStream
{
    public RPrintStreamImpl() throws RemoteException
    {
        super();
    }
    
    @Override
    public void print(String text) throws RemoteException
    {
        try {
            Writer writer = Debug.getDebugStream();
            synchronized (writer) {
                writer.write(text);
                writer.flush();
            }
        }
        catch (IOException ioe) {
            System.err.println("IOException writing to debug log.");
        }
    }
}
