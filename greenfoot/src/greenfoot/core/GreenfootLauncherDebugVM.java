/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2013  Poul Henriksen and Michael Kolling 
 
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
import java.io.IOException;
import java.io.Writer;
import java.rmi.RemoteException;

import rmiextension.BlueJRMIClient;
import rmiextension.wrappers.RBlueJ;
import rmiextension.wrappers.RPrintStream;
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
    public GreenfootLauncherDebugVM(String prjDir, String rmiServiceName)
    {
        instance = this;
        if (BlueJRMIClient.instance() != null) {
            // Already launched.
            return;
        }
        
        final BlueJRMIClient client = new BlueJRMIClient(prjDir, rmiServiceName);
        
        final RBlueJ blueJ = client.getBlueJ();
        if (blueJ == null) {
            System.exit(1);
        }
        
        // This constructor is called on BlueJ's "server" thread, so we do the rest in
        // another thread to avoid holding the server thread lock:
        new Thread() {
            public void run() {
                try {           
                    client.initialise();
                    File libdir = blueJ.getSystemLibDir();
                    Config.initializeVMside(libdir, blueJ.getUserPrefDir(),
                            blueJ.getInitialCommandLineProperties(), true, client);
                    final RPrintStream rprintStream = blueJ.getDebugPrinter();
                    Debug.setDebugStream(new Writer() {
                        @Override
                        public void write(char[] cbuf, int off, int len)
                                throws IOException
                        {
                            String s = new String(cbuf, off, len);
                            rprintStream.print(s);
                        }
                        
                        @Override
                        public void flush() throws IOException
                        {
                        }
                        
                        @Override
                        public void close() throws IOException
                        {
                        }
                    });
                    
                    GreenfootUtil.initialise(GreenfootUtilDelegateIDE.getInstance());
                    GreenfootMain.initialize(blueJ, client.getPackage());
                }
                catch (RemoteException re) {
                    re.printStackTrace();
                }
            };
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
