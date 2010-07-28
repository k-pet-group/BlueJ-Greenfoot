/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2010  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.core.GreenfootLauncherDebugVM;
import greenfoot.core.GreenfootMain;
import greenfoot.core.ProjectProperties;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import bluej.Config;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ExceptionDescription;
import bluej.debugmgr.ResultWatcher;
import bluej.extensions.BPackage;
import bluej.extensions.BlueJ;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.event.PackageEvent;
import bluej.extensions.event.PackageListener;
import bluej.pkgmgr.PkgMgrFrame;
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
    implements PackageListener
{
    /** Singleton instance */
    private static ProjectManager instance;

    /** List to keep track of which projects has been opened */
    private List<BPackage> openedPackages = new ArrayList<BPackage>();

    /** List to keep track of which projects are int the process of being created */
    private List<File> projectsInCreation = new ArrayList<File>();

    /** The class that will be instantiated in the greenfoot VM to launch the project */
    private String launchClass = GreenfootLauncherDebugVM.class.getName();
    private static final String launcherName = "greenfootLauncher";

    private static BlueJ bluej;

    private ProjectManager()
    {}

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
        if (instance == null) {
            instance = new ProjectManager();
        }
        return instance;
    }

    /**
     * Initialise. Must be called before the instance is accessed.
     */
    public static void init(BlueJ bluej)
    {
        ProjectManager.bluej = bluej;
    }

    /**
     * Launch the project in the Greenfoot VM if it is a proper Greenfoot
     * project. This is called when a package is opened; because there is
     * no listener interface for project open/close events, we have to keep
     * track of projects manually.
     */
    private void launchProject(final Project project)
    {
        if (!ProjectManager.instance().isProjectOpen(project)) {
            File projectDir;
            try {
                projectDir = new File(project.getDir());
            } catch (ProjectNotOpenException pnoe) {
                // The project must have closed in the meantime
                return;
            }
            int versionOK = checkVersion(projectDir);
            if (versionOK != GreenfootMain.VERSION_BAD) {
                try {
                    if (versionOK == GreenfootMain.VERSION_UPDATED) {
                        project.getPackage().getProject().getPackage("").reload();
                    }
                    openGreenfoot(project);
                } catch (Exception e) {
                    Debug.reportError("Could not create greenfoot launcher.", e);
                    // This is bad, lets exit.
                    greenfootLaunchFailed(project);
                }
            }
            else {
                // If this was the only open project, open the startup project
                // instead.
                if (bluej.getOpenProjects().length == 1) {
                    ((PkgMgrFrame) bluej.getCurrentFrame()).doClose(true, true);
                    File startupProject = new File(bluej.getSystemLibDir(), "startupProject");
                    bluej.openProject(startupProject);
                }
            }
        }
    }

    /**
     * Launch the Greenfoot debug VM code (and tell it where to connect to for RMI purposes).
     * @param project  A just-opened project
     */
    public void openGreenfoot(final Project project)
    {
        ResultWatcher watcher = new ResultWatcher() {
            @Override
            public void putError(String message)
            {
                Debug.message("Greenfoot launch failed with error: " + message);
                greenfootLaunchFailed(project);
            }
            @Override
            public void putException(ExceptionDescription exception)
            {
                Debug.message("Greenfoot launch failed due to exception in debug VM: " + exception.getText());
                greenfootLaunchFailed(project);
            }
            @Override
            public void putResult(DebuggerObject result, String name,
                    InvokerRecord ir)
            {
                // This is ok
            }
            @Override
            public void putVMTerminated()
            {
                Debug.message("Greenfoot launch failed due to debug VM terminating.");
                greenfootLaunchFailed(project);
            }
        };
        
        try {
            ObjectBench.createObject(project, launchClass, launcherName,
                    new String[] {project.getDir(), project.getName(),
                    BlueJRMIServer.getBlueJService()}, watcher);
        } catch (ProjectNotOpenException e) {
            // Not important; project has been closed, so no need to launch
        } catch (PackageNotFoundException e) {
            // likewise
        }
    }
    
    /**
     * Launching Greenfoot failed. Display a dialog, and exit.
     */
    public static void greenfootLaunchFailed(Project project)
    {
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
    private int checkVersion(File projectDir)
    {
        if(isNewProject(projectDir)) {
            ProjectProperties newProperties = new ProjectProperties(projectDir);
            newProperties.setApiVersion(GreenfootMain.getAPIVersion().toString());
            newProperties.save();
        }        
        return GreenfootMain.updateApi(projectDir, null); 
    }

    /**
     * Checks if this is a project that is being created for the first time
     * @param projectDir
     * @return
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

    /**
     * Whether this project is currently open or not, according to our records.
     * 
     */
    private boolean isProjectOpen(Project prj)
    {
        boolean projectIsOpen = false;
        File prjFile = null;
        try {
            prjFile = prj.getPackage().getProject().getDir();
        }
        catch (ProjectNotOpenException pnoe) {
            // If we get a ProjectNotOpenException... then surely the project isn't open?
            // (shouldn't be possible).
            return false;
        }
        for (int i = 0; i < openedPackages.size(); i++) {
            BPackage openPkg = openedPackages.get(i);

            File openPrj = null;
            try {
                openPrj = openPkg.getProject().getDir();
            }
            catch (ProjectNotOpenException e2) {
                // Shouldn't happen; but if it does, it should be
                // safe to ignore.
            }

            if (openPrj != null && openPrj.equals(prjFile)) {
                return true;
            }
        }
        return projectIsOpen;
    }

    //=================================================================
    //bluej.extensions.event.PackageListener implementation
    //=================================================================

    /*
     * @see bluej.extensions.event.PackageListener#packageOpened(bluej.extensions.event.PackageEvent)
     */
    public void packageOpened(PackageEvent event)
    {
        BPackage pkg = event.getPackage();
        
        Project project = new Project(pkg);
        if (! isProjectOpen(project)) {
            launchProject(project);
        }

        openedPackages.add(event.getPackage());
    }

    /*
     * @see bluej.extensions.event.PackageListener#packageClosing(bluej.extensions.event.PackageEvent)
     */
    public void packageClosing(PackageEvent event)
    {
        openedPackages.remove(event.getPackage());
    }
}
