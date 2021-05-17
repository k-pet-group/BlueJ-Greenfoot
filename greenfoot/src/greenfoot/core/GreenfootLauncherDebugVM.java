/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2013,2014,2015,2018,2019,2021  Poul Henriksen and Michael Kolling
 
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

import bluej.BlueJPropStringSource;
import greenfoot.platforms.ide.GreenfootUtilDelegateIDE;
import greenfoot.util.GreenfootUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import bluej.Config;
import bluej.utility.Debug;

/**
 * An object of GreenfootLauncherDebugVM is the first object that is created in the
 * Debug-VM. The launcher object is created from the BlueJ-VM in the Debug-VM.
 * When a new object of the launcher is created, the constructor looks up the
 * BlueJService in the RMI registry and starts the initialisation of Greenfoot.
 * 
 * @author Poul Henriksen
 */
public class GreenfootLauncherDebugVM
{
    private static GreenfootLauncherDebugVM instance;
    
    @SuppressWarnings("unused")
    private Object transportField;
    
    /**
     * Constructor for the Greenfoot Launcher. This connects to the RMI service on the
     * primary VM, and starts the Greenfoot UI.
     * 
     * @param prjDir         The project directory
     * @param rmiServiceName  The name of the RMI service to connect to
     */
    public GreenfootLauncherDebugVM(String prjDir, String libDirPath, String userPrefDirPath, String propsFilePath, String shmFilePath, String shmFileSize, String seqStart)
    {
        instance = this;
        
        // This constructor is called on BlueJ's "server" thread, so we do the rest in
        // another thread to avoid holding the server thread lock:
        new Thread("Launching Greenfoot VM") {
            public void run() {     
                Properties properties = new Properties();
                try
                {
                    properties.load(new FileInputStream(new File(propsFilePath)));
                }
                catch (IOException e)
                {
                    // We can survive without the properties...
                }
                    
                Config.initializeVMside(new File(libDirPath), new File(userPrefDirPath),
                        new BlueJPropStringSource()
                        {
                            @Override
                            public String getBlueJPropertyString(String property, String def)
                            {
                                return properties.getProperty(property, def);
                            }

                            @Override
                            public String getLabel(String key)
                            {
                                return properties.getProperty(key, key);
                            }
                        });
                Debug.setDebugStream(new PrintWriter(System.err));
                
                GreenfootUtil.initialise(GreenfootUtilDelegateIDE.getInstance());
                GreenfootMain.initialize(prjDir, shmFilePath, Integer.parseInt(shmFileSize), Integer.parseInt(seqStart));
            }
        }.start();
    }
    
    /**
     * Get the GreenfootLauncherDebugVM instance.
     */
    public static GreenfootLauncherDebugVM getInstance()
    {
        return instance;
    }
    
    /**
     * Set the transport field to some object. It is then possible to obtain a remote
     * reference to the object, via RProject.getRemoteObject().
     */
    public void setTransportField(Object transportField)
    {
        this.transportField = transportField;
    }
}
