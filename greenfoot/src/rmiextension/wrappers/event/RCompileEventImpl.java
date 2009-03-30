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

import java.io.File;
import java.rmi.RemoteException;

import bluej.extensions.event.CompileEvent;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: RCompileEventImpl.java 6216 2009-03-30 13:41:07Z polle $
 */
public class RCompileEventImpl extends java.rmi.server.UnicastRemoteObject
    implements RCompileEvent
{
    private CompileEvent compileEvent;

    /**
     * @param event
     */
    public RCompileEventImpl(CompileEvent event)
        throws RemoteException
    {
        super();
        this.compileEvent = event;
    }

    /**
     * @return
     */
    public int getErrorLineNumber()
    {
        return compileEvent.getErrorLineNumber();
    }

    /**
     * @return
     */
    public String getErrorMessage()
    {
        return compileEvent.getErrorMessage();
    }

    /**
     * @return
     */
    public int getEvent()
    {
        return compileEvent.getEvent();
    }

    /**
     * @return
     */
    public File[] getFiles()
    {
        return compileEvent.getFiles();
    }

    /**
     * @param aLineNumber
     */
    public void setErrorLineNumber(int aLineNumber)
    {
        compileEvent.setErrorLineNumber(aLineNumber);
    }

    /**
     * @param anErrorMessage
     */
    public void setErrorMessage(String anErrorMessage)
    {
        compileEvent.setErrorMessage(anErrorMessage);
    }
}