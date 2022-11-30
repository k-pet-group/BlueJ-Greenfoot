/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2015,2017,2018,2019,2021  Poul Henriksen and Michael Kolling
 
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
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import bluej.Boot;
import bluej.Config;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerResult;
import bluej.editor.Editor;
import bluej.extensions2.BlueJ;
import bluej.extensions2.SourceType;
import bluej.pkgmgr.DocPathEntry;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import greenfoot.core.GreenfootMain.VersionCheckInfo;
import greenfoot.core.GreenfootMain.VersionInfo;
import greenfoot.util.Version;
import greenfoot.vmcomm.GreenfootDebugHandler;
import javafx.application.Platform;
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
    
    /** Filter that matches class files */
    private static FilenameFilter classFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name)
        {
            return name.toLowerCase().endsWith(".class");
        }
    };

    @OnThread(Tag.FXPlatform)
    public static class ProjectAPIVersionAccess
    {
        /**
         * Attempts to find the version number the greenfoot API that a greenfoot
         * project was created with. If it can not find a version number, it will
         * return Version.NO_VERSION. Thread-safe.
         *
         * @return API version
         */
        public Version getAPIVersion(Project project)
        {
            String versionString = project.getUnnamedPackage().getLastSavedProperties().getProperty("version");
            return new Version(versionString);
        }

        /**
         * Sets the API version and saves this to the project file.
         * @param version
         */
        public void setAPIVersionAndSave(Project project, String version)
        {
            Properties props = new Properties();
            // We don't want to lose any properties, so add the original properties in:
            props.putAll(project.getUnnamedPackage().getLastSavedProperties());
            if (! version.equals(props.get("version")))
            {
                props.put("version", version);
                project.getUnnamedPackage().save(props);
            }
        }
        
    }
    
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
        ProjectAPIVersionAccess projectAPIVersionAccess = new ProjectAPIVersionAccess();
        
        GreenfootMain.VersionCheckInfo versionOK = checkVersion(project, projectAPIVersionAccess);
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
    @OnThread(Tag.FXPlatform)
    public static void greenfootLaunchFailed(Project project)
    {
        launchFailed = true;
        String text = Config.getString("greenfoot.launchFailed");
        DialogManager.showErrorTextFX(null, text);
        System.exit(1);
    }
    
    /**
     * Checks whether the API version this project was created with is
     * compatible with the current API version. If it is not, it will attempt to
     * update the project to the current version of the API and present the user
     * with a dialog with instructions on what to do if there are changes in API
     * version that requires manual modifications of the API.
     * 
     * @param projectDir Directory of the project.
     * @return one of GreenfootMain.VERSION_OK, VERSION_UPDATED or VERSION_BAD
     */
    @OnThread(Tag.FXPlatform)
    private GreenfootMain.VersionCheckInfo checkVersion(Project project,
            ProjectAPIVersionAccess projectAPIVersionAccess)
    {
        File greenfootLibDir = Config.getGreenfootLibDir();
        Version projectVersion = projectAPIVersionAccess.getAPIVersion(project);

        Version apiVersion = GreenfootMain.getAPIVersion();
        String greenfootApiVersion = Boot.GREENFOOT_API_VERSION;

        if (projectVersion.isBad())
        {
            String message = projectVersion.getBadMessage();
            DialogManager.showInfoTextFX(null, Config.getString("project.version.mismatch"), message, false);
            Debug.message("Bad version number in project: " + greenfootLibDir);
            prepareGreenfootProject(greenfootLibDir, project,
                    projectAPIVersionAccess, true, greenfootApiVersion);
            return new VersionCheckInfo(VersionInfo.VERSION_UPDATED, false);
        }
        else if (projectVersion.isOlderAndBreaking(apiVersion))
        {
            String message = projectVersion.getChangesMessage(apiVersion);
            boolean removeAWTImports;
            if (projectVersion.crosses300Boundary(apiVersion))
            {
                // "Would you like to try to automatically update your code?";
                message += "\n";
                removeAWTImports = DialogManager.askQuestionFX(null, "greenfoot-importfix-question",
                        message) == 0;
            }
            else
            {
                DialogManager.showInfoTextFX(null, Config.getString("project.version.mismatch"), message, false);
                removeAWTImports = false;
            }
            prepareGreenfootProject(greenfootLibDir, project,
                    projectAPIVersionAccess, true, greenfootApiVersion);

            return new VersionCheckInfo(VersionInfo.VERSION_UPDATED, removeAWTImports);
        }
        else if (apiVersion.isOlderAndBreaking(projectVersion))
        {
            String message = projectVersion.getNewerMessage();
            int buttonIndex = DialogManager.showInfoTextFX(null, Config.getString("project.version.mismatch"), message, true);

            //Check if "Cancel" button is clicked
            if (buttonIndex == 0)
            {
                return new VersionCheckInfo(VersionInfo.VERSION_BAD, false);
            }
            prepareGreenfootProject(greenfootLibDir, project, projectAPIVersionAccess,
                    true, greenfootApiVersion);
            return new VersionCheckInfo(VersionInfo.VERSION_UPDATED, false);
        }
        else if (projectVersion.isNonBreaking(apiVersion))
        {
            prepareGreenfootProject(greenfootLibDir, project,
                    projectAPIVersionAccess, true, greenfootApiVersion);
            return new VersionCheckInfo(VersionInfo.VERSION_UPDATED, false);
        }
        else if (projectVersion.isInternal(apiVersion))
        {
            prepareGreenfootProject(greenfootLibDir, project,
                    projectAPIVersionAccess, false, greenfootApiVersion);
            return new VersionCheckInfo(VersionInfo.VERSION_UPDATED, false);
        }
        else
        {
            prepareGreenfootProject(greenfootLibDir, project,
                    projectAPIVersionAccess, false, greenfootApiVersion);
            return new VersionCheckInfo(VersionInfo.VERSION_OK, false);
        }
    }
    
    /**
     * Makes a project a greenfoot project. It cleans up the project directory
     * and makes sure everything that needs to be there is there.
     * 
     * @param deleteClassFiles whether the class files in the destination should
     *            be deleted. If true, they will be deleted and appear as
     *            needing a recompile in the Greenfoot class browser.
     */
    @OnThread(Tag.FXPlatform)
    private static void prepareGreenfootProject(File greenfootLibDir, Project project,
                                                ProjectAPIVersionAccess p, boolean deleteClassFiles, String greenfootApiVersion)
    {
        File dst = project.getProjectDir();

        File greenfootDir = new File(dst, "greenfoot");
        
        // Since Greenfoot 1.5.2 we no longer require the greenfoot directory,
        // so we delete everything that we might have had in there previously,
        // and delete the dir if it is empty after that.
        deleteGreenfootDir(greenfootDir);        
        
        if(deleteClassFiles) {
            deleteAllClassFiles(dst);
        }
        
        // Since Greenfoot 1.3.0 we no longer use the bluej.pkg file, so if it
        // exists it should now be deleted.
        try {
            File pkgFile = new File(dst, "bluej.pkg");
            if (pkgFile.exists()) {
                pkgFile.delete();
            }   
            File pkhFile = new File(dst, "bluej.pkh");
            if (pkhFile.exists()) {
                pkhFile.delete();
            }
        }
        catch (SecurityException e) {
            // If we don't have permission to delete, just leave them there.
        }   
        
        try {
            File images = new File(dst, "images");
            images.mkdir();
            File sounds = new File(dst, "sounds");
            sounds.mkdir();
        }
        catch (SecurityException e) {
            Debug.reportError("SecurityException when trying to create images/sounds directories", e);
        }
        
        p.setAPIVersionAndSave(project, greenfootApiVersion);
    }
    
    /**
     * Deletes all class files in the directory, including the greenfoot subdirectory,
     * only if they have a .java file related to them.
     */
    private static void deleteAllClassFiles(File dir)
    {
        String[] classFiles = dir.list(classFilter);
        if(classFiles == null) return;

        for (int i = 0; i < classFiles.length; i++) {
            String fileName = classFiles[i];
            int index = fileName.lastIndexOf('.');
            String javaFileName = fileName.substring(0, index) + "." + SourceType.Java.toString().toLowerCase();
            File file = new File(dir, fileName);
            File javaFile = new File(dir, javaFileName);
            if (javaFile.exists()) {
                file.delete();
            }
        }
    }

    private static void deleteGreenfootDir(File greenfootDir) 
    {
        if (greenfootDir.exists())
        {
            try
            {
                File actorJava = new File(greenfootDir, "Actor.java");
                if (actorJava.exists())
                {
                    actorJava.delete();
                }
            }
            catch (SecurityException e)
            {
                // If we don't have permission to delete, just leave them there.
            }
            
            try
            {
                File worldJava = new File(greenfootDir, "World.java");
                if (worldJava.exists())
                {
                    worldJava.delete();
                }
            }
            catch (SecurityException e)
            {
                // If we don't have permission to delete, just leave them there.
            }
            
            try
            {
                File actorJava = new File(greenfootDir, "Actor.class");
                if (actorJava.exists())
                {
                    actorJava.delete();
                }
            }
            catch (SecurityException e)
            {
                // If we don't have permission to delete, just leave them there.
            }
            
            try
            {
                File worldJava = new File(greenfootDir, "World.class");
                if (worldJava.exists())
                {
                    worldJava.delete();
                }
            }
            catch (SecurityException e)
            {
                // If we don't have permission to delete, just leave them there.
            }
            
            try
            {
                File worldJava = new File(greenfootDir, "project.greenfoot");
                if (worldJava.exists())
                {
                    worldJava.delete();
                }
            }
            catch (SecurityException e)
            {
                // If we don't have permission to delete, just leave them there.
            }
            
            try
            {
                greenfootDir.delete();
            }
            catch (SecurityException e)
            {
                // If we don't have permission to delete, just leave them there.
            }
        }
    }
    
    /**
     * Launch the Greenfoot debug VM code (and tell it where to connect to for RMI purposes).
     * 
     * @param project  A just-opened project
     */
    @OnThread(Tag.FXPlatform)
    public void openGreenfoot(final Project project, GreenfootDebugHandler greenfootDebugHandler)
    {
        File shmFile = greenfootDebugHandler.getShmFile();
        
        
        Properties debugVMProps = new Properties();
        debugVMProps.put("bluej.language", Config.getPropString("bluej.language", ""));
        debugVMProps.put("vm.language", Config.getPropString("vm.language", ""));
        debugVMProps.put("vm.country", Config.getPropString("vm.country", ""));
        File tmpPropsFile = null;
        try
        {
            tmpPropsFile = File.createTempFile("debugvm", "props");
            tmpPropsFile.deleteOnExit();
            debugVMProps.store(new FileOutputStream(tmpPropsFile), "");
        }
        catch (IOException e)
        {
            Debug.reportError(e);
            // We continue even if the properties file will be blank, as it only sets locale and language...
        }

        final String[] consParams = { project.getProjectDir().getPath(),
                Config.getBlueJLibDir().getAbsolutePath(),
                Config.getUserConfigDir().getAbsolutePath(),
                tmpPropsFile == null ? "" : tmpPropsFile.getAbsolutePath(),
                shmFile == null ? "" : shmFile.getAbsolutePath(),
                Integer.toString(greenfootDebugHandler.getShmFileSize()),
                // New VM starts at old last seq so that it's  after any final events we get from the dying VM:
                // (especially since the seq will have had 1000 added when the last VM was terminated)
                Integer.toString(greenfootDebugHandler.getLastSeq()) };

        Package pkg = project.getPackage("");
        final Debugger debugger = pkg.getProject().getDebugger();

        String [] argTypes = new String[consParams.length];
        DebuggerObject [] argObjects = new DebuggerObject[consParams.length];
        for (int i = 0; i < consParams.length; i++)
        {
            argTypes[i] = "java.lang.String";
            argObjects[i] = debugger.getMirror(consParams[i]);
        }
        
        new Thread("Opening Greenfoot")
        {
            public void run()
            {
                DebuggerResult result = debugger.instantiateClass(launchClass, argTypes, argObjects);
                
                Platform.runLater(() -> {
                    final DebuggerObject debugObject = result.getResultObject();
                    
                    if (debugObject != null)
                    {
                        String wrappedName = greenfootDebugHandler.addObject(debugObject, debugObject.getGenType(),
                                launcherName);
                        pkg.getDebugger().addObject(pkg.getQualifiedName(), wrappedName, debugObject);
                        pkg.compileOnceIdle(null, CompileReason.LOADED, CompileType.INTERNAL_COMPILE);
                    }
                    
                    int status = result.getExitStatus();
                    if (status == Debugger.EXCEPTION)
                    {
                        Debug.message("Greenfoot launch failed due to exception in debug VM: " + result.getException().getText());
                        greenfootLaunchFailed(project);
                    }
                    else if (status == Debugger.TERMINATED_BY_USER_SYSTEM_EXIT || status == Debugger.TERMINATED_BY_BLUEJ)
                    {
                        Debug.message("Greenfoot launch failed due to debug VM terminating.");
                        greenfootLaunchFailed(project);
                    }
                });
            }
        }.start();
    }
}
