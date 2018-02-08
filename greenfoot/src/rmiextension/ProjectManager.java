/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2015,2017,2018  Poul Henriksen and Michael Kolling
 
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

import bluej.extensions.BClass;
import bluej.extensions.ExtensionBridge;
import bluej.extensions.editor.Editor;
import bluej.extensions.editor.EditorBridge;
import greenfoot.core.GreenfootLauncherDebugVM;
import greenfoot.core.GreenfootMain;
import greenfoot.core.GreenfootMain.ProjectAPIVersionAccess;

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import greenfoot.guifx.GreenfootGuiHandler;
import greenfoot.util.Version;
import javafx.application.Platform;
import rmiextension.wrappers.RProjectImpl;
import rmiextension.wrappers.WrapperPool;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.Boot;
import bluej.Config;
import bluej.Main;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ExceptionDescription;
import bluej.debugmgr.ResultWatcher;
import bluej.extensions.BObject;
import bluej.extensions.BPackage;
import bluej.extensions.BProject;
import bluej.extensions.BlueJ;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.event.PackageEvent;
import bluej.extensions.SourceType;
import bluej.pkgmgr.DocPathEntry;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.Debug;
import bluej.utility.DialogManager;

/**
 * The ProjectManager is on the BlueJ-VM. It monitors pacakage events from BlueJ
 * and launches the greenfoot project in the greenfoot-VM.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public class ProjectManager
{
    /** Singleton instance */
    private static ProjectManager instance;

    /** List to keep track of which projects are in the process of being created */
    private List<File> projectsInCreation = new ArrayList<File>();
    
    /** The class that will be instantiated in the greenfoot VM to launch the project */
    private String launchClass = GreenfootLauncherDebugVM.class.getName();
    private static final String launcherName = "greenfootLauncher";

    private static BlueJ bluej;
    
    private static volatile boolean launchFailed = false;
    
    boolean wizard;
    SourceType sourceType;

    private ProjectManager()
    {
    }

    /**
     * Get the singleton instance. Make sure it is initialised first.
     * 
     * @see #init(BlueJ)
     */
    public static ProjectManager instance()
    {
        if (bluej == null) {
            throw new IllegalStateException("Projectmanager has not been initialised.");
        }
        return instance;
    }

    /**
     * Initialise. Must be called before the instance is accessed.
     */
    public static void init(BlueJ bluej)
    {
        ProjectManager.bluej = bluej;
        instance = new ProjectManager();
        Main.setGuiHandler(new GreenfootGuiHandler());
    }

    /**
     * Launch the project in the Greenfoot VM if it is a proper Greenfoot
     * project. This is called when a package is opened; because there is
     * no listener interface for project open/close events, we have to keep
     * track of projects manually.
     */
    @OnThread(Tag.FXPlatform)
    public void launchProject(final BProject project)
    {
        File projectDir;
        Project unwrapped;
        try {
            projectDir = project.getDir();
            unwrapped = ExtensionBridge.getProject(project);
        } catch (ProjectNotOpenException pnoe) {
            // The project must have closed in the meantime
            return;
        }
        ProjectAPIVersionAccess projectAPIVersionAccess = new ProjectAPIVersionAccess()
        {
            @Override
            public Version getAPIVersion()
            {
                String versionString = unwrapped.getUnnamedPackage().getLastSavedProperties().getProperty("version");
                return new Version(versionString);
            }

            @Override
            public void setAPIVersionAndSave(String version)
            {
                Properties props = new Properties(unwrapped.getUnnamedPackage().getLastSavedProperties());
                props.put("version", version);
                unwrapped.getUnnamedPackage().save(props);
            }
        };
        
        GreenfootMain.VersionCheckInfo versionOK = checkVersion(projectDir, projectAPIVersionAccess);
        if (versionOK.versionInfo != GreenfootMain.VersionInfo.VERSION_BAD) {
            try {
                if (versionOK.versionInfo == GreenfootMain.VersionInfo.VERSION_UPDATED) {
                    project.getPackage("").reload();
                    if (versionOK.removeAWTImports)
                    {
                        for (BClass bClass : project.getPackage("").getClasses())
                        {
                            Editor bClassEditor = bClass.getEditor();
                            if (bClassEditor != null)
                            {
                                Platform.runLater(() ->
                                {
                                    bluej.editor.Editor ed = EditorBridge.getEditor(bClassEditor);
                                    ed.removeImports(Arrays.asList("java.awt.Color", "java.awt.Font"));
                                });
                            }
                        }
                        project.getPackage("").scheduleCompilation(true);
                    }
                }

                // Add debugger listener. The listener will launch the Greenfoot GUI.
                GreenfootDebugHandler.addDebuggerListener(project);
                
                // Add Greenfoot API sources to project source path
                Project bjProject = Project.getProject(project.getDir());
                List<DocPathEntry> sourcePath = bjProject.getSourcePath();

                String language = Config.getPropString("bluej.language");

                if (! language.equals("english")) {
                    // Add the native language sources first
                    File langlib = new File(Config.getBlueJLibDir(), language);
                    File apiDir = new File(new File(langlib, "greenfoot"), "api");
                    sourcePath.add(new DocPathEntry(apiDir, ""));
                }

                File langlib = new File(Config.getBlueJLibDir(), "english");
                File apiDir = new File(new File(langlib, "greenfoot"), "api");
                sourcePath.add(new DocPathEntry(apiDir, ""));
                
            }
            catch (ProjectNotOpenException | PackageNotFoundException e) {
                Debug.reportError("Could not create greenfoot launcher.", e);
                // This is bad, lets exit.
                greenfootLaunchFailed(project);
            }
        }
        else {
            try {
                project.close();
            }
            catch (ProjectNotOpenException pnoe) {}
            
            // If this was the only open project, open the startup project
            // instead.
            if (bluej.getOpenProjects().length == 0) {
                File startupProject = new File(bluej.getSystemLibDir(), "startupProject");
                bluej.openProject(startupProject);
            }
        }
    }

    /**
     * Check whether failure to launch has been recorded.
     */
    public static boolean checkLaunchFailed()
    {
        return launchFailed;
    }
    
    /**
     * Launch the Greenfoot debug VM code (and tell it where to connect to for RMI purposes).
     * 
     * @param project  A just-opened project
     */
    public void openGreenfoot(final BProject project, GreenfootDebugHandler greenfootDebugHandler)
    {
        try {
            final BPackage pkg = project.getPackage("");
            ResultWatcher watcher = new ResultWatcher() {
                @Override
                public void beginCompile()
                {
                    // Nothing needs doing
                }
                @Override
                public void beginExecution(InvokerRecord ir)
                {
                    // Nothing needs doing
                }
                @Override
                public void putError(String message, InvokerRecord ir)
                {
                    Debug.message("Greenfoot launch failed with error: " + message);
                    greenfootLaunchFailed(project);
                }
                @Override
                public void putException(ExceptionDescription exception, InvokerRecord ir)
                {
                    Debug.message("Greenfoot launch failed due to exception in debug VM: " + exception.getText());
                    greenfootLaunchFailed(project);
                }
                @Override
                public void putResult(DebuggerObject result, String name,
                        InvokerRecord ir)
                {
                    // This is ok
                    try {
                        BObject bObject = pkg.getObject(name);
                        RProjectImpl rProject = WrapperPool.instance().getWrapper(project);
                        rProject.setTransportObject(bObject);
                    }
                    catch (ProjectNotOpenException e) {
                        // I guess we can ignore this.
                    }
                    catch (PackageNotFoundException e) {
                        // And this.
                    }
                    catch (RemoteException re) {
                        Debug.reportError("Unexpected exception getting remote project wrapper", re);
                    }
                }
                @Override
                public void putVMTerminated(InvokerRecord ir)
                {
                    Debug.message("Greenfoot launch failed due to debug VM terminating.");
                    greenfootLaunchFailed(project);
                }
            };
            File shmFile = greenfootDebugHandler.getShmFile();
            String[] consParams = { project.getDir().getPath(),
                    BlueJRMIServer.getBlueJService(), shmFile == null ? "" : shmFile.getAbsolutePath(),
                    String.valueOf(wizard), String.valueOf(sourceType) };
            
            Project bjProj = ExtensionBridge.getProject(project);
            PkgMgrFrame pmf = PkgMgrFrame.createFrame(bjProj.getPackage(""), null);
            
            ConstructorInvoker launcher = new ConstructorInvoker(bjProj.getPackage(""), pmf.getObjectBench(), launchClass);
            launcher.invokeConstructor(launcherName, consParams, watcher);
            
            // Reset wizard to false so it doesn't affect future loads:
            wizard = false;
        }
        catch (ProjectNotOpenException e) {
            // Not important; project has been closed, so no need to launch
        }
    }
    
    /**
     * Launching Greenfoot failed. Display a dialog, and exit.
     */
    public static void greenfootLaunchFailed(BProject project)
    {
        launchFailed = true;
        String text = Config.getString("greenfoot.launchFailed");
        DialogManager.showErrorText(null, text);
        System.exit(1);
    }

    /**
     * Handles the check of the project version. It will notify the user if the
     * project has to be updated.
     * 
     * @param projectDir Directory of the project.
     * @return one of GreenfootMain.VERSION_OK, VERSION_UPDATED or VERSION_BAD
     */
    private GreenfootMain.VersionCheckInfo checkVersion(File projectDir, ProjectAPIVersionAccess projectAPIVersionAccess)
    {
        if(isNewProject(projectDir))
        {
            projectAPIVersionAccess.setAPIVersionAndSave(Boot.GREENFOOT_API_VERSION);
        }        
        return GreenfootMain.updateApi(projectDir, projectAPIVersionAccess, null, Boot.GREENFOOT_API_VERSION); 
    }

    /**
     * Checks if this is a project that is being created for the first time
     */
    private boolean isNewProject(File projectDir)
    {
        return projectsInCreation.contains(projectDir);        
    }
    
    /**
     * Flags that this project is in the process of being created.
     */
    public void addNewProject(File projectDir)
    {
        projectsInCreation.add(projectDir);
    }

    /**
     * Flags that this project is no longer in the process of being created.
     */
    public void removeNewProject(File projectDir)
    {
        projectsInCreation.remove(projectDir);
    }

    //=================================================================
    //bluej.extensions.event.PackageListener implementation
    //=================================================================

    /*
     * @see bluej.extensions.event.PackageListener#packageOpened(bluej.extensions.event.PackageEvent)
     */
    public void packageOpened(PackageEvent event)
    {
    }

    /*
     * @see bluej.extensions.event.PackageListener#packageClosing(bluej.extensions.event.PackageEvent)
     */
    public void packageClosing(PackageEvent event)
    {
    }

    public void setWizard(boolean wizard)
    {
        this.wizard = wizard;
    }

    public void setSourceType(SourceType sourceType)
    {
        this.sourceType = sourceType;
    }
}
