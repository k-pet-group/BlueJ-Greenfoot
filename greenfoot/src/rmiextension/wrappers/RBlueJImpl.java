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

import bluej.Boot;
import bluej.Config;
import bluej.extensions.BProject;
import bluej.extensions.BlueJ;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.event.ApplicationEvent;
import bluej.extensions.event.ApplicationListener;
import bluej.extensions.event.ClassListener;
import bluej.extensions.event.CompileEvent;
import bluej.extensions.event.CompileListener;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.prefmgr.PrefMgrDialog;
import bluej.utility.Debug;
import bluej.utility.Utility;
import javafx.application.Platform;
import rmiextension.wrappers.event.RApplicationListener;

import java.awt.*;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
        blueJ.addCompileListener(new CompileListener() {
            
            @Override
            public void compileWarning(CompileEvent event) { }
            
            @Override
            public void compileSucceeded(CompileEvent event)
            {
                // Do a Garbage Collection to finalize any garbage JdiObjects, thereby
                // allowing objects on the remote VM to be garbage collected.
                System.gc();
            }
            
            @Override
            public void compileStarted(CompileEvent event) { }
            
            @Override
            public void compileFailed(CompileEvent event) { }
            
            @Override
            public void compileError(CompileEvent event) { }
        });
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

    @Override
    public void addApplicationListener(RApplicationListener listener) throws RemoteException
    {
        blueJ.addApplicationListener(new ApplicationListener()
        {
            @Override
            public void blueJReady(ApplicationEvent event)
            {

            }

            @Override
            public void dataSubmissionFailed(ApplicationEvent event)
            {
                try
                {
                    listener.dataSubmissionFailed();
                }
                catch (RemoteException e)
                {
                    Debug.reportError(e);
                }
            }
        });
    }
}
