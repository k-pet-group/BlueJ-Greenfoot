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
package greenfoot.core;

import greenfoot.platforms.ide.GreenfootUtilDelegateIDE;
import greenfoot.util.GreenfootUtil;

import java.io.File;
import java.rmi.RemoteException;

import javax.swing.ImageIcon;

import rmiextension.BlueJRMIClient;
import rmiextension.wrappers.RBlueJ;
import bluej.BlueJTheme;
import bluej.Config;

/**
 * An object of GreenfootLauncherDebugVM is the first object that is created in the
 * Debug-VM. The launcher object is created from the BlueJ-VM in the Debug-VM.
 * When a new object of the launcher is created, the constructor looks up the
 * BlueJService in the RMI registry and starts the initialisation of greenfoot.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version 22-05-2003
 * @version $Id$
 */
public class GreenfootLauncherDebugVM
{
    public GreenfootLauncherDebugVM(String prjDir, String pkgName, String rmiServiceName)
    {
        BlueJRMIClient client = new BlueJRMIClient(prjDir, rmiServiceName);
        
        RBlueJ blueJ = client.getBlueJ();
        try {
            File libdir = blueJ.getSystemLibDir();
            Config.initializeVMside(libdir, blueJ.getInitialCommandLineProperties(), true, client);
            GreenfootUtil.initialise(new GreenfootUtilDelegateIDE());
            
            ImageIcon icon = new ImageIcon(GreenfootUtil.getGreenfootLogoPath());
            BlueJTheme.setIconImage(icon.getImage());

            GreenfootMain.initialize(blueJ, client.getPackage());
        }
        catch (RemoteException re) {
            re.printStackTrace();
        }
    }
}
