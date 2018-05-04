/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2013,2014,2015,2016,2017,2018  Poul Henriksen and Michael Kolling
 
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

import bluej.Config;
import bluej.extensions.BProject;
import bluej.extensions.BlueJ;
import bluej.extensions.event.CompileEvent;
import bluej.extensions.event.CompileListener;

import java.io.File;
import java.rmi.RemoteException;
import java.util.Properties;

/**
 * Implements the RBlueJ RMI interface.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public class RBlueJImpl extends java.rmi.server.UnicastRemoteObject
    implements RBlueJ
{
    BlueJ blueJ;

    public RBlueJImpl(BlueJ blueJ)
        throws RemoteException
    {
        super();
        this.blueJ = blueJ;
    }
    
    /*
     * @see rmiextension.wrappers.RBlueJ#getBlueJPropertyString(java.lang.String, java.lang.String)
     */
    public String getBlueJPropertyString(String property, String def)
    {
        return blueJ.getBlueJPropertyString(property, def);
    }

    /*
     * @see rmiextension.wrappers.RBlueJ#getExtensionPropertyString(java.lang.String, java.lang.String)
     */
    public String getExtensionPropertyString(String property, String def)
    {
        return blueJ.getExtensionPropertyString(property, def);
    }

    /*
     * @see rmiextension.wrappers.RBlueJ#getSystemLibDir()
     */
    public File getSystemLibDir()
    {
        File f = blueJ.getSystemLibDir();
        //The getAbsoluteFile() fixes a weird bug on win using jdk1.4.2_06
        return f.getAbsoluteFile();
    }
    
    /*
     * @see rmiextension.wrappers.RBlueJ#getInitialCommandLineProperties()
     */
    public Properties getInitialCommandLineProperties()
        throws RemoteException
    {
        return Config.getInitialCommandLineProperties();
    }

    @Override
    public File getUserPrefDir() throws RemoteException
    {
        return Config.getUserConfigDir();
    }

    private class BProjectRef
    {
        public BProject bProject;
    }
}
