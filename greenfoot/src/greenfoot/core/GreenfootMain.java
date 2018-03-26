/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2013,2014,2015,2016,2017,2018  Poul Henriksen and Michael Kolling
 
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

import bluej.Boot;
import bluej.collect.DataSubmissionFailedDialog;
import greenfoot.event.CompileListener;
import greenfoot.event.CompileListenerForwarder;
import greenfoot.gui.GreenfootFrame;
import greenfoot.platforms.ide.ActorDelegateIDE;
import greenfoot.util.Version;

import java.awt.EventQueue;
import java.io.File;
import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

import rmiextension.wrappers.RBlueJ;
import rmiextension.wrappers.event.RApplicationListenerImpl;
import bluej.Config;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.SourceType;
import bluej.runtime.ExecServer;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.utility.Utility;

/**
 * The main class for greenfoot. This is a singelton (in the JVM). Since each
 * project is opened in its own JVM there can be several Greenfoot instances,
 * but each will be in its own JVM so it is effectively a singleton.
 * 
 * @author Poul Henriksen
 */
public class GreenfootMain extends Thread
{
    public static enum VersionInfo {
        /** The project API version matches the greenfoot API version */
        VERSION_OK,
        /** The project API version was different, and has been updated */
        VERSION_UPDATED,
        /** The project was not a greenfoot project, or the user chose to cancel the open */
        VERSION_BAD }
    
    public static class VersionCheckInfo
    {
        public final VersionInfo versionInfo;
        public final boolean removeAWTImports;

        public VersionCheckInfo(VersionInfo versionInfo, boolean removeAWTImports)
        {
            this.removeAWTImports = removeAWTImports;
            this.versionInfo = versionInfo;
        }
    }

    /** Version of the API for this Greenfoot release. */
    private static Version version = null;

    /** Greenfoot is a singleton - this is the instance. */
    private static GreenfootMain instance;

    /** The connection to BlueJ via RMI */
    private RBlueJ rBlueJ;

    /** The main frame of greenfoot. */
    private GreenfootFrame frame;

    /** The path to the dummy startup project */
    private File startupProject;

    /**
     * Forwards compile events to all the compileListeners that has registered
     * to reccieve compile events.
     */
    private CompileListenerForwarder compileListenerForwarder;
    private List<CompileListener> compileListeners = new LinkedList<CompileListener>();

    // ----------- static methods ------------

    /**
     * Initializes the singleton. This can only be done once - subsequent calls
     * will have no effect.
     * 
     * @param rBlueJ   remote BlueJ instance
     * @param pkg      remote reference to the unnamed package of the project corresponding to this Greenfoot instance
     * @param shmFilePath The path to the shared-memory file to be mmap-ed for communication
     * @param wizard   whether to run the "new project wizard"
     * @param sourceType  default source type for the new project
     */
    public static void initialize(RBlueJ rBlueJ, String projDir, String shmFilePath, boolean wizard, SourceType sourceType)
    {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        if (instance == null) {
            instance = new GreenfootMain(rBlueJ, projDir, shmFilePath, wizard, sourceType);
        }
    }

    /**
     * Gets the singleton.
     */
    public static GreenfootMain getInstance()
    {
        return instance;
    }

    // ----------- instance methods ------------

    /**
     * Contructor is private. This class is initialised via the 'initialize'
     * method (above).
     */
    private GreenfootMain(final RBlueJ rBlueJ, String projDir, String shmFilePath, boolean wizard, SourceType sourceType)
    {
        instance = this;
        this.rBlueJ = rBlueJ;
        try {
            // determine the path of the startup project
            File startupProj = rBlueJ.getSystemLibDir();
            startupProj = new File(startupProj, "greenfoot");
            startupProject = new File(startupProj, "startupProject");

            ShadowProjectProperties projectProperties = new ShadowProjectProperties();
            ActorDelegateIDE.setupAsActorDelegate(projectProperties);

            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    // Initialise JavaFX:
                    new JFXPanel();
                    Platform.setImplicitExit(false);

                    frame = GreenfootFrame.getGreenfootFrame(rBlueJ, projectProperties, shmFilePath);

                    // Want to execute this after the simulation has been initialised:
                    ExecServer.setCustomRunOnThread(r -> Simulation.getInstance().runLater(r));

                    // Config is initialized in GreenfootLauncherDebugVM

                    try {
                        // bringToFront is done automatically by BlueJ
                        // Utility.bringToFront(frame);

                        compileListenerForwarder = new CompileListenerForwarder(compileListeners);
                        GreenfootMain.this.rBlueJ.addCompileListener(compileListenerForwarder, new File(projDir));
                    }
                    catch (RemoteException exc) {
                        Debug.reportError("Error when opening scenario", exc);
                    }
                    
                    try
                    {
                        rBlueJ.hideSplash();
                    }
                    catch (RemoteException e)
                    {
                        Debug.reportError(e);
                    }
                    frame.setVisible(true);
                    Utility.bringToFront(frame);
                    
                    EventQueue.invokeLater(() -> {
                        if (wizard) {
                            //new NewSubWorldAction(frame, true, sourceType).actionPerformed(null);
                        }
                    });

                    // We can do this late on, because although the submission failure may have already
                    // happened, the event is re-issued to new listeners.  And we don't want to accidentally
                    // show the dialog during load because we may interrupt important processes:
                    try
                    {
                        GreenfootMain.this.rBlueJ.addApplicationListener(new RApplicationListenerImpl() {
                            @Override
                            public void dataSubmissionFailed() throws RemoteException
                            {
                                if (Boot.isTrialRecording())
                                {
                                    Platform.runLater(() -> {
                                        new DataSubmissionFailedDialog().show();
                                    });
                                }
                            }
                        });
                    }
                    catch (RemoteException e)
                    {
                        Debug.reportError(e);
                        // Show the dialog anyway; probably best to restart:
                        Platform.runLater(() -> {
                            new DataSubmissionFailedDialog().show();
                        });
                    }

                }
            });
        }
        catch (Exception exc) {
            Debug.reportError("could not create greenfoot main", exc);
        }
    }

    /**
     * Close all open Greenfoot project instances, i.e. exit the application.
     */
    public static void closeAll()
    {
        try {
            getInstance().rBlueJ.exit();
        }
        catch (RemoteException re) {
            Debug.reportError("Closing all projects", re);
        }
    }

    /**
     * Adds a listener for compile events
     * 
     * @param listener
     */
    private void addCompileListener(CompileListener listener)
    {
        synchronized (compileListeners) {
            compileListeners.add(0, listener);
        }
    }

    /**
     * Get a reference to the greenfoot frame.
     */
    public GreenfootFrame getFrame()
    {
        return frame;
    }
    
    /**
     * Gets the version number of the Greenfoot API for this Greenfoot release.
     */
    public static Version getAPIVersion()
    {
        if (version == null) {
            try {
                Class<?> bootCls = Class.forName("bluej.Boot");
                Field field = bootCls.getField("GREENFOOT_API_VERSION");
                String versionStr = (String) field.get(null);
                version = new Version(versionStr);
            }
            catch (ClassNotFoundException e) {
                Debug.reportError("Could not get Greenfoot API version", e);
                throw new InternalGreenfootError(e);
            }
            catch (SecurityException e) {
                Debug.reportError("Could not get Greenfoot API version", e);
                throw new InternalGreenfootError(e);
            }
            catch (NoSuchFieldException e) {
                Debug.reportError("Could not get Greenfoot API version", e);
                throw new InternalGreenfootError(e);
            }
            catch (IllegalArgumentException e) {
                Debug.reportError("Could not get Greenfoot API version", e);
                throw new InternalGreenfootError(e);
            }
            catch (IllegalAccessException e) {
                Debug.reportError("Could not get Greenfoot API version", e);
                throw new InternalGreenfootError(e);
            }
        }

        return version;
    }
}
