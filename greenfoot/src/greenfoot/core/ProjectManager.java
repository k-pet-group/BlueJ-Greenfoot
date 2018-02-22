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
package greenfoot.core;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import bluej.Boot;
import bluej.Config;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ExceptionDescription;
import bluej.debugmgr.ResultWatcher;
import bluej.editor.Editor;
import bluej.extensions.BlueJ;
import bluej.extensions.SourceType;
import bluej.pkgmgr.DocPathEntry;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import greenfoot.core.GreenfootMain.ProjectAPIVersionAccess;
import greenfoot.util.Version;
import rmiextension.BlueJRMIServer;
import rmiextension.ConstructorInvoker;
import rmiextension.GreenfootDebugHandler;
import threadchecker.OnThread;
import threadchecker.Tag;

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
    
    /** The class that will be instantiated in the greenfoot VM to launch the project */
    private String launchClass = GreenfootLauncherDebugVM.class.getName();
    private static final String launcherName = "greenfootLauncher";
    
    private static volatile boolean launchFailed = false;

    private boolean wizard;
    private SourceType sourceType;

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
        if (instance == null) {
            instance = new ProjectManager();
        }
        return instance;
    }
    
    /**
     * Open a Greenfoot project.
     */
    @OnThread(Tag.FXPlatform)
    public void launchProject(final Project project)
    {
        File projectDir = project.getProjectDir();
        
        ProjectAPIVersionAccess projectAPIVersionAccess = new ProjectAPIVersionAccess()
        {
            @Override
            public Version getAPIVersion()
            {
                String versionString = project.getUnnamedPackage().getLastSavedProperties().getProperty("version");
                return new Version(versionString);
            }

            @Override
            public void setAPIVersionAndSave(String version)
            {
                Properties props = new Properties(project.getUnnamedPackage().getLastSavedProperties());
                props.put("version", version);
                project.getUnnamedPackage().save(props);
            }
        };
        
        GreenfootMain.VersionCheckInfo versionOK = checkVersion(projectDir, projectAPIVersionAccess);
        if (versionOK.versionInfo != GreenfootMain.VersionInfo.VERSION_BAD) {
            try {
                if (versionOK.versionInfo == GreenfootMain.VersionInfo.VERSION_UPDATED) {
                    project.getPackage("").reload();
                    if (versionOK.removeAWTImports)
                    {
                        for (ClassTarget ctarget : project.getPackage("").getClassTargets())
                        {
                            Editor ed = ctarget.getEditor();
                            
                            if (ed != null)
                            {
                                ed.removeImports(Arrays.asList("java.awt.Color", "java.awt.Font"));
                            }
                        }
                        project.scheduleCompilation(true, CompileReason.EARLY,
                                CompileType.INTERNAL_COMPILE, project.getPackage(""));
                    }
                }

                // Add debugger listener. The listener will launch the Greenfoot GUI.
                GreenfootDebugHandler.addDebuggerListener(project);
                
                // Add Greenfoot API sources to project source path
                List<DocPathEntry> sourcePath = project.getSourcePath();

                String language = Config.getPropString("bluej.language");

                if (! language.equals("english"))
                {
                    // Add the native language sources first
                    File langlib = new File(Config.getBlueJLibDir(), language);
                    File apiDir = new File(new File(langlib, "greenfoot"), "api");
                    sourcePath.add(new DocPathEntry(apiDir, ""));
                }

                File langlib = new File(Config.getBlueJLibDir(), "english");
                File apiDir = new File(new File(langlib, "greenfoot"), "api");
                sourcePath.add(new DocPathEntry(apiDir, ""));
                
            }
            catch (IOException e)
            {
                Debug.reportError("Could not create greenfoot launcher.", e);
                // This is bad, lets exit.
                greenfootLaunchFailed(project);
            }
        }
        else {
            Project.cleanUp(project);
            
            // If this was the only open project, open the startup project
            // instead.
            //if (bluej.getOpenProjects().length == 0)
            //{
            //    File startupProject = new File(bluej.getSystemLibDir(), "startupProject");
            //    bluej.openProject(startupProject);
            //}
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
     * Launching Greenfoot failed. Display a dialog, and exit.
     */
    public static void greenfootLaunchFailed(Project project)
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
        return GreenfootMain.updateApi(projectDir, projectAPIVersionAccess, null, Boot.GREENFOOT_API_VERSION); 
    }
    
    /**
     * Launch the Greenfoot debug VM code (and tell it where to connect to for RMI purposes).
     * 
     * @param project  A just-opened project
     */
    public void openGreenfoot(final Project project, GreenfootDebugHandler greenfootDebugHandler)
    {
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
                // This is ok. May need to store result/name somewhere?
            }
            @Override
            public void putVMTerminated(InvokerRecord ir)
            {
                Debug.message("Greenfoot launch failed due to debug VM terminating.");
                greenfootLaunchFailed(project);
            }
        };
        File shmFile = greenfootDebugHandler.getShmFile();
        String[] consParams = { project.getProjectDir().getPath(),
                BlueJRMIServer.getBlueJService(), shmFile == null ? "" : shmFile.getAbsolutePath(),
                String.valueOf(wizard), String.valueOf(sourceType) };
        
        ConstructorInvoker launcher = new ConstructorInvoker(project.getPackage(""),
                greenfootDebugHandler, launchClass);
        launcher.invokeConstructor(launcherName, consParams, watcher);
        
        // Reset wizard to false so it doesn't affect future loads:
        wizard = false;
    }
}
