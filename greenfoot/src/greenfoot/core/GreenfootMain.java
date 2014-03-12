/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2013,2014  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.ObjectTracker;
import greenfoot.event.ActorInstantiationListener;
import greenfoot.event.CompileListener;
import greenfoot.event.CompileListenerForwarder;
import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.MessageDialog;
import greenfoot.importer.scratch.ScratchImport;
import greenfoot.platforms.ide.ActorDelegateIDE;
import greenfoot.util.FileChoosers;
import greenfoot.util.Version;

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Point;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.rmi.ServerError;
import java.rmi.ServerException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JOptionPane;

import rmiextension.wrappers.RBlueJ;
import rmiextension.wrappers.RClass;
import rmiextension.wrappers.RPackage;
import rmiextension.wrappers.RProject;
import rmiextension.wrappers.event.RCompileEvent;
import rmiextension.wrappers.event.RInvocationListener;
import rmiextension.wrappers.event.RProjectListener;
import bluej.Config;
import bluej.debugmgr.CallHistory;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.pkgmgr.GreenfootProjectFile;
import bluej.pkgmgr.Project;
import bluej.runtime.ExecServer;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.utility.Utility;
import bluej.views.View;

/**
 * The main class for greenfoot. This is a singelton (in the JVM). Since each
 * project is opened in its own JVM there can be several Greenfoot instances,
 * but each will be in its own JVM so it is effectively a singleton.
 * 
 * @author Poul Henriksen
 */
public class GreenfootMain extends Thread implements CompileListener, RProjectListener
{
    /* Constants for return from updateApi method */
    /** The project API version matches the greenfoot API version */
    public static final int VERSION_OK = 0;
    /** The project API version was different, and has been updated */
    public static final int VERSION_UPDATED = 1;
    /** The project was not a greenfoot project, or the user chose to cancel the open */
    public static final int VERSION_BAD = 2;

    /** Version of the API for this Greenfoot release. */
    private static Version version = null;

    /** Greenfoot is a singleton - this is the instance. */
    private static GreenfootMain instance;

    /** The connection to BlueJ via RMI */
    private RBlueJ rBlueJ;

    /** The main frame of greenfoot. */
    private GreenfootFrame frame;

    /** The project this Greenfoot singelton refers to. */
    private GProject project;

    /** The package this Greenfoot singelton refers to. */
    private GPackage pkg;

    /** The path to the dummy startup project */
    private File startupProject;

    /**
     * Forwards compile events to all the compileListeners that has registered
     * to reccieve compile events.
     */
    private CompileListenerForwarder compileListenerForwarder;
    private List<CompileListener> compileListeners = new LinkedList<CompileListener>();

    /** The class state manager notifies GClass objects when their compilation state changes */
    private ClassStateManager classStateManager;

    /** Listens for instantiations of Actor objects. */
    private ActorInstantiationListener instantiationListener;

    /** List of invocation listeners that has been registered. */
    private List<RInvocationListener> invocationListeners = new ArrayList<RInvocationListener>();

    /** History of parameters passed to methods. */
    private CallHistory callHistory = new CallHistory();

    /** Filter that matches class files */
    private static FilenameFilter classFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name)
        {
            return name.toLowerCase().endsWith(".class");
        }
    };

    private ClassLoader currentLoader;

    // ----------- static methods ------------

    /**
     * Initializes the singleton. This can only be done once - subsequent calls
     * will have no effect.
     */
    public static void initialize(RBlueJ rBlueJ, RPackage pkg)
    {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        if (instance == null) {
            try {
                instance = new GreenfootMain(rBlueJ, pkg.getProject());
            }
            catch (ProjectNotOpenException pnoe) {
                // can't happen
                Debug.reportError("Getting remote project", pnoe);
            }
            catch (RemoteException re) {
                // shouldn't happen
                Debug.reportError("Getting remote project", re);
            }
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
    private GreenfootMain(final RBlueJ rBlueJ, final RProject proj)
    {
        instance = this;
        this.rBlueJ = rBlueJ;
        currentLoader = ExecServer.getCurrentClassLoader();
        addCompileListener(this);
        try {
            // determine the path of the startup project
            File startupProj = rBlueJ.getSystemLibDir();
            startupProj = new File(startupProj, "greenfoot");
            startupProject = new File(startupProj, "startupProject");

            this.project = GProject.newGProject(proj);
            addCompileListener(project);
            this.pkg = project.getDefaultPackage();
            ActorDelegateIDE.setupAsActorDelegate(project);

            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (!isStartupProject()) {
                        try {
                            classStateManager = new ClassStateManager(project);
                        } catch (RemoteException exc) {
                            Debug.reportError("Error when opening scenario", exc);
                        }
                    }
                    
                    frame = GreenfootFrame.getGreenfootFrame(rBlueJ, classStateManager);

                    // Config is initialized in GreenfootLauncherDebugVM

                    if (!isStartupProject()) {
                        try {
                            instantiationListener = new ActorInstantiationListener(WorldHandler.getInstance());

                            frame.openProject(project);
                            // bringToFront is done automatically by BlueJ
                            // Utility.bringToFront(frame);

                            compileListenerForwarder = new CompileListenerForwarder(compileListeners);
                            GreenfootMain.this.rBlueJ.addCompileListener(compileListenerForwarder, pkg.getProject().getDir());

                            
                            rBlueJ.addClassListener(classStateManager);
                        }
                        catch (Exception exc) {
                            Debug.reportError("Error when opening scenario", exc);
                        }
                    }
                    
                    frame.setVisible(true);
                    Utility.bringToFront(frame);
                }
            });
        }
        catch (Exception exc) {
            Debug.reportError("could not create greenfoot main", exc);
        }
    }

    /**
     * Check whether this instance of greenfoot is running the dummy
     * startup project.
     * @return true if this is the startup project
     */
    private boolean isStartupProject()
    {
        return project.getDir().equals(startupProject);
    }

    /**
     * Opens the project in the given directory. The project launches in a
     * new VM.
     */
    public void openProject(String projectDir)
        throws RemoteException
    {
        File projectDirFile = new File(projectDir);

        // Display msg dialog of project does not exist.
        if (!projectDirFile.exists()) {
            JOptionPane.showMessageDialog(frame, 
                    Config.getString("noproject.dialog.msg") + System.getProperty("line.separator") + projectDir,
                    Config.getString("noproject.dialog.title"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        // It's possible that the user re-opened a project which they previously closed,
        // resulting in an empty frame (because no other open projects). In that case the
        // project is actually still running, behind the scenes; so just re-display it.
        if (project.getDir().equals(projectDirFile)) {
            frame.openProject(project);
            return;
        }
        
        // Used for some imports:
        boolean autoIndentAllFiles = false;

        if (!projectDirFile.isDirectory() && !Project.isProject(projectDirFile.toString())) {
            if (projectDirFile.getName().endsWith(".sb")) {
                projectDirFile = ScratchImport.convert(projectDirFile);
                autoIndentAllFiles = true;
            }
            else {
                projectDirFile = Utility.maybeExtractArchive(projectDirFile, frame);
                if (projectDirFile == null) {
                    return;
                }
                if (! GreenfootProjectFile.exists(projectDirFile)) {
                    // Archive doesn't appear to be a Greenfoot project
                    DialogManager.showError(frame, "archive-not-greenfoot-project");
                    FileUtility.deleteDir(projectDirFile);
                    return;
                }
            }
        }
                
        int versionStatus = GreenfootMain.updateApi(projectDirFile, frame, getAPIVersion().toString());
        boolean doOpen = versionStatus != VERSION_BAD;
        if (doOpen) {
            RProject proj = rBlueJ.openProject(projectDirFile);

            if (autoIndentAllFiles) {
                try {
                    for (RPackage pkg : proj.getPackages()) {
                        for (RClass cls : pkg.getRClasses()) {
                            cls.autoIndent();
                        }
                    }
                // If there any problems, never mind about it:
                }
                catch(ProjectNotOpenException e) { }
                catch (PackageNotFoundException e) { }
            }

            // if this is the dummy startup project and there is a valid project to open, close it now
            if (proj != null && frame.getProject() == null) {
                project.close();
            }
        }
    }

    /**
     * Opens a file browser to find a greenfoot project
     */
    public void openProjectBrowser()
    {
        File dirName = FileChoosers.getScenario(frame);

        if (dirName != null) {
            if (Config.isZipFile(dirName)) {
                JOptionPane.showMessageDialog(frame, Config.getString("open.project.message.zip"));
            }
            else{
                try {
                    openProject(dirName.getAbsolutePath());
                }
                catch (Exception exc) {
                    Debug.reportError("Could not open scenario", exc);
                }
            }
        }
    }

    /**
     * Get the project for this greenfoot instance.
     * @return
     */
    public GProject getProject()
    {
        return project;
    }

    /**
     * Closes this greenfoot frame, or handle it closing.
     * 
     * <p>If this is called with the windowClosing parameter false, and there is only one project open,
     * then the frame won't be closed but will instead be turned into an empty frame.
     */
    private void closeThisInstance(boolean windowClosing)
    {
        try {
            if (rBlueJ.getOpenProjects().length <= 1) {
                if (windowClosing) {
                    // This happens to be the only way the startup project can be closed
                    rBlueJ.exit();
                }
                else {
                    frame.closeProject();
                }
            }
            else {
                project.close();
            }
        }
        catch (RemoteException re) {
            Debug.reportError("Error while closing", re);
        }
    }

    /**
     * Close the project in the given frame. This will also close the frame, or (if
     * the windowClosing parameter is false, and no other projects are open) make it
     * empty.
     */
    public static void closeProject(GreenfootFrame frame, boolean windowClosing)
    {
        instance.closeThisInstance(windowClosing);
    }

    /*
     * @see rmiextension.wrappers.event.RProjectListener#projectClosing()
     */
    @Override
    public void projectClosing()
    {
        try {
            if (!isStartupProject()) {
                rBlueJ.removeCompileListener(compileListenerForwarder);
                rBlueJ.removeClassListener(classStateManager);
                storeFrameState();
                for (RInvocationListener element : invocationListeners) {
                    rBlueJ.removeInvocationListener(element);
                }
            }
        }
        catch (RemoteException re) {
            Debug.reportError("Closing project", re);
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
     * Store the current main window size to the project properties.
     */
    private void storeFrameState()
    {
        ProjectProperties projectProperties = project.getProjectProperties();

        projectProperties.setInt("mainWindow.width", frame.getWidth());
        projectProperties.setInt("mainWindow.height", frame.getHeight());
        Point loc = frame.getLocation();
        projectProperties.setInt("mainWindow.x", loc.x);
        projectProperties.setInt("mainWindow.y", loc.y);

        projectProperties.save();
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
     * Adds a listener for invocation events
     * 
     * @param listener
     */
    public void addInvocationListener(RInvocationListener listener)
        throws RemoteException
    {
        invocationListeners.add(listener);
        rBlueJ.addInvocationListener(listener);
    }

    /**
     * Creates a new project
     */
    public RProject newProject()
    {
        File newFile = FileUtility.getDirName(frame,
                Config.getString("greenfoot.utilDelegate.newScenario"),
                Config.getString("pkgmgr.newPkg.buttonLabel"),
                false, true);
        if (newFile != null) {
            if (newFile.exists() && (!newFile.isDirectory() || newFile.list().length > 0)) {
                DialogManager.showError(frame, "project-already-exists");
                return null;
            }
            try {
                RProject rproj = rBlueJ.newProject(newFile);
                if (rproj != null) {
                    // The rest of the project preparation will be done by the
                    // ProjectManager on the BlueJ VM.

                    // if the project that is already open is the dummy startup project
                    // or if there is an empty project, close it now
                    if (isStartupProject()|| frame.isClosedProject()) {
                        project.close();
                    }
                    
                    return rproj;
                }
                DialogManager.showError(frame, "cannot-create-project");
            }
            catch (ServerError se) {
                Debug.reportError("Problems when trying to create new scenario", se);
            }
            catch (ServerException se) {
                Debug.reportError("Problems when trying to create new scenario", se);
            }
            catch (RemoteException re) {
                Debug.reportError("Problems when trying to create new scenario", re);
            }
        }
        return null;
    }

    /**
     * Get a reference to the CallHistory instance.
     */
    public CallHistory getCallHistory()
    {
        return callHistory;
    }

    /**
     * Get a reference to the invocation listener.
     */
    public ActorInstantiationListener getInvocationListener()
    {
        return instantiationListener;
    }

    /**
     * Get a reference to the greenfoot frame.
     */
    public GreenfootFrame getFrame()
    {
        return frame;
    }

    /**
     * Makes a project a greenfoot project. It cleans up the project directory
     * and makes sure everything that needs to be there is there.
     * 
     * @param deleteClassFiles whether the class files in the destination should
     *            be deleted. If true, they will be deleted and appear as
     *            needing a recompile in the Greenfoot class browser.
     */
    private static void prepareGreenfootProject(File greenfootLibDir, File projectDir,
            ProjectProperties p, boolean deleteClassFiles, String greenfootApiVersion)
    {
        if (isStartupProject(greenfootLibDir, projectDir)) {
            return;
        }
        File dst = projectDir;

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
        
        p.setApiVersion(greenfootApiVersion);
        p.save();
    }

    private static void deleteGreenfootDir(File greenfootDir) 
    {
        if (greenfootDir.exists()) {
            try {
                File actorJava = new File(greenfootDir, "Actor.java");
                if (actorJava.exists()) {
                    actorJava.delete();
                }
            }
            catch (SecurityException e) {
                // If we don't have permission to delete, just leave them there.
            }
            
            try {
                File worldJava = new File(greenfootDir, "World.java");
                if (worldJava.exists()) {
                    worldJava.delete();
                }
            }
            catch (SecurityException e) {
                // If we don't have permission to delete, just leave them there.
            }
            
            try {
                File actorJava = new File(greenfootDir, "Actor.class");
                if (actorJava.exists()) {
                    actorJava.delete();
                }
            }
            catch (SecurityException e) {
                // If we don't have permission to delete, just leave them there.
            }
            
            try {
                File worldJava = new File(greenfootDir, "World.class");
                if (worldJava.exists()) {
                    worldJava.delete();
                }
            }
            catch (SecurityException e) {
                // If we don't have permission to delete, just leave them there.
            }
            
            try {
                File worldJava = new File(greenfootDir, "project.greenfoot");
                if (worldJava.exists()) {
                    worldJava.delete();
                }
            }
            catch (SecurityException e) {
                // If we don't have permission to delete, just leave them there.
            }
            
            try {
                greenfootDir.delete();
            }
            catch (SecurityException e) {
                // If we don't have permission to delete, just leave them there.
            }
        }
    }

    /**
     * Checks whether the API version this project was created with is
     * compatible with the current API version. If it is not, it will attempt to
     * update the project to the current version of the API and present the user
     * with a dialog with instructions on what to do if there are changes in API
     * version that requires manual modifications of the API.
     * <p>
     * If is considered safe to open this project with the current API version
     * the method will return true.
     * 
     * @param project The project in question.
     * @param parent Frame that should be used to place dialogs.
     * @return One of VERSION_OK, VERSION_UPDATED or VERSION_BAD
     */
    public static int updateApi(File projectDir, Frame parent, String greenfootApiVersion)
    {
        File greenfootLibDir = Config.getGreenfootLibDir();
        ProjectProperties newProperties = new ProjectProperties(projectDir);
        Version projectVersion = newProperties.getAPIVersion();

        Version apiVersion = GreenfootMain.getAPIVersion();

        if (projectVersion.isBad()) {
            String message = projectVersion.getBadMessage();
            JButton continueButton = new JButton(Config.getString("greenfoot.continue"));
            MessageDialog dialog = new MessageDialog(parent, message, Config.getString("project.version.mismatch"), 50,
                    new JButton[]{continueButton});
            dialog.displayModal();
            Debug.message("Bad version number in project: " + greenfootLibDir);
            GreenfootMain.prepareGreenfootProject(greenfootLibDir, projectDir,
                    newProperties, true, greenfootApiVersion);
            return VERSION_UPDATED;
        }
        else if (projectVersion.isOlderAndBreaking(apiVersion)) {
            String message = projectVersion.getChangesMessage(apiVersion);
            JButton continueButton = new JButton(Config.getString("greenfoot.continue"));
            MessageDialog dialog = new MessageDialog(parent, message, Config.getString("project.version.mismatch"), 80,
                    new JButton[]{continueButton});
            dialog.displayModal();
            GreenfootMain.prepareGreenfootProject(greenfootLibDir, projectDir,
                    newProperties, true, greenfootApiVersion);

            return VERSION_UPDATED;
        }
        else if (apiVersion.isOlderAndBreaking(projectVersion)) {
            String message = projectVersion.getNewerMessage();

            JButton cancelButton = new JButton(Config.getString("greenfoot.cancel"));
            JButton continueButton = new JButton(Config.getString("greenfoot.continue"));
            MessageDialog dialog = new MessageDialog(parent, message, Config.getString("project.version.mismatch"), 50,
                    new JButton[]{continueButton, cancelButton});
            JButton pressed = dialog.displayModal();

            if (pressed == cancelButton) {
                return VERSION_BAD;
            }
            prepareGreenfootProject(greenfootLibDir, projectDir, newProperties, true, greenfootApiVersion);
            return VERSION_UPDATED;
        }
        else if (projectVersion.isNonBreaking(apiVersion) ) {
            prepareGreenfootProject(greenfootLibDir, projectDir,
                    newProperties, true, greenfootApiVersion);
            return VERSION_UPDATED;
        }
        else if (projectVersion.isInternal(apiVersion)) {
            prepareGreenfootProject(greenfootLibDir, projectDir,
                    newProperties, false, greenfootApiVersion);
            return VERSION_UPDATED;
        }
        else {       
            prepareGreenfootProject(greenfootLibDir, projectDir,
                    newProperties, false, greenfootApiVersion);
            return VERSION_OK;            
        }
    }

    /**
     * Deletes all class files in the directory, including the greenfoot subdirectory,
     * only if they have a .java file related to them.
     */
    public static void deleteAllClassFiles(File dir)
    {
        String[] classFiles = dir.list(classFilter);
        if(classFiles == null) return;

        for (int i = 0; i < classFiles.length; i++) {
            String fileName = classFiles[i];
            int index = fileName.lastIndexOf('.');
            String javaFileName = fileName.substring(0, index) + ".java";
            File file = new File(dir, fileName);
            File javaFile = new File(dir, javaFileName);
            if (javaFile.exists()) {
                file.delete();
            }
        }
    }

    /**
     * Checks if the project is the default startup project that is used when no
     * other project is open. It is necessary to have this dummy project,
     * becuase we must have a project in order to launch the DebugVM.
     * 
     */
    public static boolean isStartupProject(File blueJLibDir, File projectDir)
    {
        File startupProject = new File(blueJLibDir, "startupProject");
        if (startupProject.equals(projectDir)) {
            return true;
        }

        return false;
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

    /**
     * See if there is a new class loader in place. If so, we want to
     * clear all views (BlueJ views) which refer to classes loaded by the previous
     * loader.
     */
    private void checkClassLoader()
    {
        ClassLoader newLoader = ExecServer.getCurrentClassLoader();
        if (newLoader != currentLoader) {
            View.removeAll(currentLoader);
            currentLoader = newLoader;
            ObjectTracker.clearRObjectCache();
        }
    }

    // ------------ CompileListener interface -------------

    @Override
    public void compileStarted(RCompileEvent event)
    {
        checkClassLoader();
    }

    @Override
    public void compileSucceeded(RCompileEvent event)
    {
        checkClassLoader();

    }

    @Override
    public void compileFailed(RCompileEvent event)
    {
        checkClassLoader();
    }

    @Override
    public void compileError(RCompileEvent event) {}

    @Override
    public void compileWarning(RCompileEvent event){}

    public void showPreferences()
    {
        try {
            rBlueJ.showPreferences();
        }
        catch (RemoteException e) {
            Debug.reportError("Problem showing preferences dialog", e);
        }
    }
}
