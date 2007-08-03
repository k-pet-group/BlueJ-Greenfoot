package greenfoot.core;

import greenfoot.ObjectTracker;
import greenfoot.WorldVisitor;
import greenfoot.event.ActorInstantiationListener;
import greenfoot.event.CompileListener;
import greenfoot.event.CompileListenerForwarder;
import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.MessageDialog;
import greenfoot.platforms.ide.ActorDelegateIDE;
import greenfoot.util.GreenfootUtil;
import greenfoot.util.Version;

import java.awt.Frame;
import java.awt.Point;
import java.io.File;
import java.io.FilenameFilter;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;

import rmiextension.wrappers.RBlueJ;
import rmiextension.wrappers.RPackage;
import rmiextension.wrappers.RProject;
import rmiextension.wrappers.event.RCompileEvent;
import rmiextension.wrappers.event.RInvocationListener;
import rmiextension.wrappers.event.RProjectListener;
import bluej.Config;
import bluej.debugmgr.CallHistory;
import bluej.extensions.ProjectNotOpenException;
import bluej.runtime.ExecServer;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.views.View;

/**
 * The main class for greenfoot. This is a singelton (in the JVM). Since each
 * project is opened in its own JVM there can be several Greenfoot instances,
 * but each will be in its own JVM so it is effectively a singleton.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: GreenfootMain.java 5140 2007-08-03 03:14:12Z bquig $
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

    /** Used to ensure that setFinalCompileListener is only called one time. */
    private boolean finalCompileListenerSet;
    
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
        public boolean accept(File dir, String name)
        {
            return name.toLowerCase().endsWith(".class");
        }
    };


    /** Only used for the standalone Greenfoot program viewer*/
    private static ProjectProperties projectProperties;
    
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
                pnoe.printStackTrace();
            }
            catch (RemoteException re) {
                // shouldn't happen
                re.printStackTrace();
            }
        }
    }
    
    /**
     * Initializes the singleton for the stand alone Greenfoot Viewer. This can only be done once - subsequent calls
     * will have no effect.
     */
    public static void initialize(ProjectProperties p)
    {
        projectProperties = p;
    }


    /**
     * Gets the singleton.
     * 
     */
    public static GreenfootMain getInstance()
    {
        return instance;
    }


    /**
     * Gets the properties for the greenfoot project run on this copy of 
     * greenfoot.
     */
    public static ProjectProperties getProjectProperties()
    {
        if(projectProperties == null) {
            return instance.getProject().getProjectProperties();
        }
        else {
            // Return the projecProperties if available. Will only be available
            // if running with the greenfoot viewer.
            return projectProperties;
        }
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
        ActorDelegateIDE.setupAsActorDelegate();
        try {
            // determine the path of the startup project
            File startupProj = rBlueJ.getSystemLibDir();
            startupProj = new File(startupProj, "greenfoot");
            startupProject = new File(startupProj, "startupProject");
            
            this.project = new GProject(proj);
            this.pkg = project.getDefaultPackage();

            frame = GreenfootFrame.getGreenfootFrame(rBlueJ);

            // Config is initialized in GreenfootLauncherDebugVM

            if(!isStartupProject()) {
                try {
                    WorldHandler.getInstance().attachProject(project);
                    frame.openProject(project);
                    Utility.bringToFront();

                    instantiationListener = new ActorInstantiationListener(WorldHandler.getInstance());
                    compileListenerForwarder = new CompileListenerForwarder(compileListeners);
                    GreenfootMain.this.rBlueJ.addCompileListener(compileListenerForwarder, pkg.getProject().getName());
                    
                    classStateManager = new ClassStateManager();
                    rBlueJ.addClassListener(classStateManager);
                }
                catch (Exception exc) {
                    Debug.reportError("failed to open scenario", exc);
                }
            }
            else {
                Utility.bringToFront();
            }
        }
        catch (Exception exc) {
            Debug.reportError("could not create greenfoot main", exc);
        }

    }
    
    
    /**
     * Check whether this instance of greenfoot is running the dummy
     * startup project.
     * @return  true if this is the startup project
     */
    private boolean isStartupProject()
    {
        try {
            return project.getDir().equals(startupProject);
        }
        catch (ProjectNotOpenException pnoe) {
            return false;
        }
        catch (RemoteException re) {
            return false;
        }
    }
    
    /**
     * Opens the project in the given directory.
     */
    public void openProject(String projectDir)
        throws RemoteException
    {
        int versionStatus = GreenfootMain.updateApi(new File(projectDir), frame);
        boolean doOpen = versionStatus != VERSION_BAD;
        if (doOpen) {
            rBlueJ.openProject(projectDir);

            // if this is the dummy startup project, close it now.
            if(isStartupProject()) {
                project.close();
            }
        }

    }
    
    /**
     * Opens a file browser to find a greenfoot project
     */
    public void openProjectBrowser()
    {
        File dirName = GreenfootUtil.getScenarioFromFileBrowser(frame);
    
        if (dirName != null) {
            try {
                openProject(dirName.getAbsolutePath());
            }
            catch (Exception exc) {
                Debug.reportError("Could not open scenario", exc);
            }
        }
    }

    /**
     * Gets the default package for this greenfoot instance.
     */
    public GPackage getPackage()
    {
        return pkg;
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
     * Closes this greenfoot frame
     */
    public void closeThisInstance()
    {
        try {
            if (rBlueJ.getOpenProjects().length <= 1) {
                if (isStartupProject()) {
                    bluej.utility.Debug.message("Is startup project so we will exit");
                    rBlueJ.exit();
                } else {
                        //rBlueJ.
                        frame.closeProject();
                        //getInstance().openProject(startupProject.getPath());
                        
                        //project.close();
                   }
            } else {
                project.close();
            }
        } catch (RemoteException re) {
            re.printStackTrace();
            
        }
    }

    
    /* (non-Javadoc)
     * @see rmiextension.wrappers.event.RProjectListener#projectClosing()
     */
    public void projectClosing()
    {
        try {
            if(!isStartupProject()) {
                rBlueJ.removeCompileListener(compileListenerForwarder);
                rBlueJ.removeClassListener(classStateManager);
                storeFrameState();
                for (RInvocationListener element : invocationListeners) {
                    rBlueJ.removeInvocationListener(element);
                }
            }
        }
        catch (RemoteException re) {
            re.printStackTrace();
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
            re.printStackTrace();
        }
    }
    
    /**
     * Store the current main window size to the project properties.
     */
    private void storeFrameState()
    {
        ProjectProperties projectProperties = getProject().getProjectProperties();
        
        projectProperties.setInt("mainWindow.width", frame.getWidth());
        projectProperties.setInt("mainWindow.height", frame.getHeight());
        Point loc = frame.getLocation();
        projectProperties.setInt("mainWindow.x", loc.x);
        projectProperties.setInt("mainWindow.y", loc.y);

        projectProperties.setInt("simulation.speed", Simulation.getInstance().getSpeed());
        
        Class cls = WorldHandler.getInstance().getLastWorldClass();
        if(cls != null) {
            projectProperties.setString("world.lastInstantiated", WorldHandler.getInstance().getLastWorldClass().getName());
        }
        
        projectProperties.save();
    }

    /**
     * Adds a listener for compile events
     * 
     * @param listener
     */
    public void addCompileListener(CompileListener listener)
    {
        synchronized (compileListeners) {
            compileListeners.add(0, listener);            
        }
    }
    
    /**
     * Adds a listener for compile events that will be the last one in the chain
     * of listeners to recieve the event.
     * 
     * @param listener
     */
    public void setFinalCompileListener(CompileListener listener)
    {
        synchronized (compileListeners) {
            if(finalCompileListenerSet) {
                throw new IllegalStateException("Final compile listener already set.");
            }
            finalCompileListenerSet = true;
            compileListeners.add(listener);            
        }
    }

    /**
     * removes a listener for compile events
     * 
     * @param listener
     */
    public void removeCompileListener(CompileListener listener)
    {
        synchronized (compileListeners) {
            compileListeners.remove(listener);
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
     * Compiles all files
     */
    public void compileAll()
    {
        try {
            // we recompile all files in ccase something has been modified
            // externally and the isCompiled state is no longer up-to-date.
            pkg.compileAll(false);
        }
        catch (Exception exc) {
            Debug.reportError("Compile greenfoot scenario failed", exc);
            exc.printStackTrace();
        }
    }

    /**
     * Creates a new project
     */
    public void newProject()
    {
        String newname = GreenfootUtil.getNewProjectName(frame);
        if (newname != null) {
            try {
                File f = new File(newname);
                rBlueJ.newProject(f);
                // The rest of the project preparation will be done by the
                // ProjectManager on the BlueJ VM.

                // if the project that is already open is the dummy startup project, close it now.
                if(isStartupProject()) {
                    project.close();
                }
            }
            catch (Exception exc) {
                Debug.reportError("Problems when trying to create new scenario...", exc);
            }
        }
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
     * Makes a project a greenfoot project. That is, copy the system classes to
     * the users library.
     * 
     * @param projectDir absolute path to the project
     */
    private static void prepareGreenfootProject(File greenfootLibDir, File projectDir, ProjectProperties p)
    {
        if (isStartupProject(greenfootLibDir, projectDir)) {
            return;
        }
        File src = new File(greenfootLibDir, "skeletonProject");
        File dst = projectDir;

        deleteAllClassFiles(dst);
        GreenfootUtil.copyDir(src, dst);
        
        touchApiClasses(dst);
        
        p.setApiVersion();
        p.save();
    }
    
    /**
     * "Touch" the actor and world class files to ensure that BlueJ/
     * Greenfoot think they are compiled.
     * 
     * @param projectDir  The Greenfoot project directory
     */
    private static void touchApiClasses(File projectDir)
    {
        // touch the Actor and World classes to ensure that they show
        // as being compiled
        File greenfootPkgDir = new File(projectDir, "greenfoot");
        File actorClassFile = new File(greenfootPkgDir, "Actor.class");
        File worldClassFile = new File(greenfootPkgDir, "World.class");
        long currentTime = System.currentTimeMillis();
        actorClassFile.setLastModified(currentTime);
        worldClassFile.setLastModified(currentTime);
    }

    /**
     * Checks whether the API version this project was created with is
     * compatible with the current API version. If it is not, it will attempt to
     * update the project to the current version of the API and present the user
     * with a dialog with instructions on what to do if there is a changes in
     * API version that requires manual modifications of the API.
     * <p>
     * If is considered safe to open this project with the current API version
     * the method will return true.
     * 
     * @param project The project in question.
     * @param parent Frame that should be used to place dialogs.
     * @return One of VERSION_OK, VERSION_UPDATED or VERSION_BAD
     * @throws RemoteException
     */
    public static int updateApi(File projectDir, Frame parent)
    {
        File greenfootLibDir = Config.getGreenfootLibDir();
        ProjectProperties newProperties = new ProjectProperties(projectDir);
        Version projectVersion = newProperties.getAPIVersion();

        Version apiVersion = GreenfootMain.getAPIVersion();

        if (projectVersion.equals(apiVersion)) {
            // If the version number matches everything should be ok.
            //
            // Just to be sure, we check that the greenfoot subdirectory is
            // actually there. This makes it easier to work with, since it will
            // then reinstall the classes after cleaning the scenarios with the
            // ant script.
            File greenfootDir = new File(projectDir, "greenfoot");
            if(! greenfootDir.exists()) {
                GreenfootMain.prepareGreenfootProject(greenfootLibDir, projectDir, newProperties);
            }
            return VERSION_OK;
        }

        if (projectVersion == Version.NO_VERSION) {
            String message = "The scenario that you are trying to open appears to be an old greenfoot scenario (before greenfoot version 0.5). This will most likely result in some errors that will have to be fixed manually.";
            JButton continueButton = new JButton("Continue");
            MessageDialog dialog = new MessageDialog(parent, message, "Versions do not match", 50,
                    new JButton[]{continueButton});
            dialog.displayModal();
            System.out.println(message);
            GreenfootMain.prepareGreenfootProject(greenfootLibDir, projectDir, newProperties);
            return VERSION_UPDATED;
        }
        else if (projectVersion.compareTo(apiVersion) < 0) {
            String message = "The scenario that you are trying to open appears to be an old greenfoot scenario (API version " + projectVersion
                    + "). The scenario will be updated to the current version (API version " + apiVersion
                    + "), but it might require some manual fixing of errors due to API changes.";
            JButton continueButton = new JButton("Continue");
            MessageDialog dialog = new MessageDialog(parent, message, "Versions do not match", 50,
                    new JButton[]{continueButton});
            dialog.displayModal();
            GreenfootMain.prepareGreenfootProject(greenfootLibDir, projectDir, newProperties);
            return VERSION_UPDATED;
        }
        else if (projectVersion.compareTo(apiVersion) > 0) { //
            String message = "The scenario that you are trying to open appears to be a greenfoot scenario created with"
                    + "a newer version of the Greenfoot API (version " + projectVersion + ")."
                    + "Opening the scenario with this version might result in"
                    + "some errors that will have to be fixed manually." + "\n \n"
                    + "Do you want to continue opening the scenario?";

            JButton cancelButton = new JButton("Cancel");
            JButton continueButton = new JButton("Continue");
            MessageDialog dialog = new MessageDialog(parent, message, "Versions do not match", 50, new JButton[]{
                    continueButton, cancelButton});
            JButton pressed = dialog.displayModal();
            if (pressed == cancelButton) {
                return VERSION_BAD;
            }
            else {
                prepareGreenfootProject(greenfootLibDir, projectDir, newProperties);
                return VERSION_UPDATED;
            }
        }
        else {
            String message = "This is not a Greenfoot scenario: " + projectDir;
            JButton continueButton = new JButton("Continue");
            MessageDialog dialog = new MessageDialog(parent, message, "Versions do not match", 50,
                    new JButton[]{continueButton});
            dialog.displayModal();
            return VERSION_BAD;
        }

    }

    /**
     * Deletes all class files in the directory, including the greenfoot subdirectory.
     */
    public static void deleteAllClassFiles(File dst) 
    {
        deleteClassFiles(dst);
        
        File greenfootDir = new File(dst, "greenfoot");
        // the greenfoot dir does not necessarily exist
        if (greenfootDir.canRead()) {
            deleteClassFiles(greenfootDir);
        }
    }

    /**
     * Deletes all class files in the given directory.
     * 
     * @param dir The directory MUST exist
     */
    private static void deleteClassFiles(File dir)
    {
        String[] classFiles = dir.list(classFilter);
        for (int i = 0; i < classFiles.length; i++) {
            String fileName = classFiles[i];
            File file = new File(dir, fileName);
            file.delete();
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
     * Gets the version number of the greenfoot API.
     * 
     * @return
     */
    public static Version getAPIVersion()
    {
        return WorldVisitor.getApiVersion();
    }
    
    public static Class loadAndInitClass(String name)
    {
        return null;
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
        
    public void compileStarted(RCompileEvent event)
    {
        checkClassLoader();
    }
        
    public void compileSucceeded(RCompileEvent event)
    {
        checkClassLoader();
        
    }
    
    public void compileFailed(RCompileEvent event)
    {
        checkClassLoader();
    }
        
    public void compileError(RCompileEvent event) {}

    public void compileWarning(RCompileEvent event){}

}
