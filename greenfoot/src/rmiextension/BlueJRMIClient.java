/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011  Poul Henriksen and Michael Kolling 
 
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
package rmiextension;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import rmiextension.wrappers.RBlueJ;
import rmiextension.wrappers.RPackage;
import rmiextension.wrappers.RProject;
import bluej.BlueJPropStringSource;
import bluej.Config;
import bluej.extensions.ProjectNotOpenException;

/**
 * The RMI client that establishes the initial connection to the BlueJ RMI server
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public class BlueJRMIClient implements BlueJPropStringSource
{
    RBlueJ blueJ = null;
    private static BlueJRMIClient instance;

    private RPackage pkg;
    private String prjDir;
    
    public BlueJRMIClient(String prjDir, String rmiServiceName)
    {
        BlueJRMIServer.forceHostForServer();
        instance = this;
        this.prjDir = prjDir;

        try {
            URI uri = new URI(rmiServiceName);
            String path = uri.getPath().substring(1); // strip leading '/'
            String host = uri.getHost();
            int port = uri.getPort();
            Registry reg = LocateRegistry.getRegistry(host, port, new LocalSocketFactory());
            blueJ = (RBlueJ) reg.lookup(path);
        }
        catch (URISyntaxException use) {
            use.printStackTrace();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        catch (NotBoundException e) {
            e.printStackTrace();
        }

    }
    
    /**
     * Perform initialisation (which involves calling back into the other VM).
     */
    public void initialise()
    {
        if (blueJ != null) {
            try {
                RProject[] openProjects = blueJ.getOpenProjects();
                RProject prj = null;
                for (int i = 0; i < openProjects.length; i++) {
                    prj = openProjects[i];
                    File passedDir = new File(prjDir);
                    if (prj.getDir().equals(passedDir)) {
                        break;
                    }
                }
                pkg = prj.getPackage("");

            }
            catch (RemoteException e1) {
                e1.printStackTrace();
            }
            catch (ProjectNotOpenException e) {
                e.printStackTrace();
            }
        }
    }

    public static BlueJRMIClient instance()
    {
        return instance;
    }

    /**
     * Returns the remote BlueJ instance.
     */
    public RBlueJ getBlueJ()
    {
        return blueJ;
    }
    
    /**
     * Returns the remote BlueJ package.
     */
    public RPackage getPackage()
    {
        return pkg;
    }
    
    // Implementation for "BlueJPropStringSource" interface
    
    @Override
    public String getBlueJPropertyString(String property, String def)
    {
        try {
            String val = blueJ.getExtensionPropertyString(property, null);
            if (val == null)
                val = blueJ.getBlueJPropertyString(property, def);
            return val;
        }
        catch (RemoteException re) {
            return def;
        }
    }

    @Override
    public String getLabel(String key)
    {
        return Config.getString(key, key);
    }
    
    @Override
    public void setUserProperty(String property, String val)
    {
        try {
            blueJ.setExtensionPropertyString(property, val);
        }
        catch (RemoteException re) {}
    }
}
