package greenfoot;

import greenfoot.event.CompileListener;
import greenfoot.event.CompileListenerForwarder;
import greenfoot.event.GreenfootObjectInstantiationListener;
import greenfoot.gui.FrameBoundsManager;
import greenfoot.gui.GreenfootFrame;

import java.awt.Rectangle;
import java.io.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import rmiextension.wrappers.RBlueJ;
import rmiextension.wrappers.RPackage;
import rmiextension.wrappers.RProject;
import rmiextension.wrappers.event.RInvocationListener;
import bluej.Config;
import bluej.debugmgr.CallHistory;
import bluej.extensions.CompilationNotStartedException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.utility.FileUtility;

/**
 * The main class for greenfoot. This is a singelton.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: Greenfoot.java 3462 2005-06-20 14:00:42Z polle $
 */
public class Greenfoot
{
    private static Greenfoot instance;
    private transient final static Logger logger = Logger.getLogger("greenfoot");

    private RBlueJ rBlueJ;
    private GreenfootFrame frame;
    private RProject project;
    private RPackage pkg;

    private CompileListenerForwarder compileListenerForwarder;
    private List compileListeners = new ArrayList();

    private GreenfootObjectInstantiationListener instantiationListener;
    private List invocationListeners = new ArrayList();
    
    private CallHistory callHistory = new CallHistory();

    private Greenfoot(RBlueJ rBlueJ, RProject project, RPackage package1)
    {

        this.project = project;
        this.pkg = package1;
        this.rBlueJ = rBlueJ;
        try {
            File libFile = rBlueJ.getSystemLibDir();
            logger.info("Found systemlib: " + libFile);
            Config.initialise(libFile, new Properties());
            logger.info("BlueJ Config initialized");
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }

        //Threading avoids deadlock when classbrowser tries to instantiate
        // objects to get images. this is necessy beacsuse greenfoot is started
        // from BlueJ-VM which waits for this call to return.
        final RProject finalProject = project;
        Thread t = new Thread() {
            public void run()
            {
                long t1 = System.currentTimeMillis();
                prepareGreenfootProject(finalProject);
                try {
                    frame = new GreenfootFrame(Greenfoot.this.rBlueJ, finalProject);
                }
                catch (ProjectNotOpenException e2) {
                    e2.printStackTrace();
                }
                catch (RemoteException e2) {
                    e2.printStackTrace();
                }
                logger.info("Frame created");
                new FrameBoundsManager(frame);
                frame.setVisible(true);
                frame.toFront();
                logger.info("Frame visible");
                try {
                    instantiationListener = new GreenfootObjectInstantiationListener(WorldHandler.instance());
                    Greenfoot.this.rBlueJ.addInvocationListener(instantiationListener);
                    compileListenerForwarder = new CompileListenerForwarder(compileListeners);
                    Greenfoot.this.rBlueJ.addCompileListener(compileListenerForwarder);
                }
                catch (RemoteException e) {
                    e.printStackTrace();
                }
                long t2 = System.currentTimeMillis();
                logger.info("Creation of frame took: " + (t2 - t1));
            }
        };
        SwingUtilities.invokeLater(t);
    }

    /**
     * Initializes the singleton. This can only be done once - subsequent calls
     * will have no effect.
     */
    public static void initialize(RBlueJ rBlueJ, RProject project, RPackage package1)
    {
        if (instance == null) {
            instance = new Greenfoot(rBlueJ, project, package1);
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }
    }

    /**
     * Gets the singleton.
     *  
     */
    public static Greenfoot getInstance()
    {
        return instance;
    }

    /**
     * Opens a project in the given directory
     * 
     * @param projectDir
     */
    public void openProject(String projectDir)
        throws RemoteException
    {
        rBlueJ.openProject(projectDir);
    }

    /**
     * Opens a file browser to find a greenfoot project
     *  
     */
    public void openProjectBrowser()
    {
        File dirName = FileUtility.getPackageName(frame);

        if (dirName != null) {
            try {
                openProject(dirName.getAbsolutePath());
            }
            catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets the package for this.
     */
    public RPackage getPackage()
    {
        return pkg;
    }

    /**
     * Closes this greenfoot frame
     * 
     * TODO This sometimes leaves proceses hanging? Seems to be fixed with later
     * BlueJ versions
     */
    public void closeThisInstance()
    {
        try {
            logger.info("closeThisInstance(): " + project.getName());
            rBlueJ.removeCompileListener(compileListenerForwarder);
            rBlueJ.removeInvocationListener(instantiationListener);
            for (Iterator iter = invocationListeners.iterator(); iter.hasNext();) {
                RInvocationListener element = (RInvocationListener) iter.next();
                rBlueJ.removeInvocationListener(element);
            }
            RProject[] openProjects = rBlueJ.getOpenProjects();
            if (openProjects.length <= 1) {
                //Close everything
                //TODO maybe open dummy project instead
                logger.info("exit greenfoot");
                rBlueJ.exit();
            }
            else {
                logger.info("closing project: " + project.getName());
                project.close();
            }
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a listener for compile events
     * 
     * @param listener
     */
    public void addCompileListener(CompileListener listener)
    {
        compileListeners.add(listener);
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
     * Compiles all files that needs compilation
     */
    public void compileAll()
    {
        try {
            pkg.compile(false);
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            e.printStackTrace();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        catch (CompilationNotStartedException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method updates BlueJ so that dialogs a showed at the correct
     * positions.
     * 
     * HACK to get dialogs positioned correctly
     * 
     * @param bounds
     */
    public void frameResized(Rectangle bounds)
    {
        try {
            rBlueJ.frameResized(bounds);
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new project
     */
    public void newProject()
    {

        String newname = FileUtility.getFileName(frame, Config.getString("pkgmgr.newPkg.title"), Config
                .getString("pkgmgr.newPkg.buttonLabel"), false, null, true);
        if (newname != null) {
            try {
                RProject newProject = rBlueJ.newProject(new File(newname));
                prepareGreenfootProject(newProject);
            }
            catch (RemoteException e) {
                e.printStackTrace();
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
    public GreenfootObjectInstantiationListener getInvocationListener()
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
     * @param project
     */
    private void prepareGreenfootProject(RProject project)
    {
        try {
            File projectDir = project.getDir();

            if (isStartupProject(projectDir)) {
                logger.info("GreenfootLauncher: This is startupProject... ");
                return;
            }

            File blueJLibDir = rBlueJ.getSystemLibDir();
            File src = new File(blueJLibDir, "skeletonProject");
            File dst = projectDir;
            copyDir(src, dst);

        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    /**
     * Checks if the project is the default startup project that is used when no
     * other project is open. It is necessary to have this dummy project,
     * becuase we must have a project in order to launch the DebugVM.
     *  
     */
    private boolean isStartupProject(File projectDir)
    {
        try {
            File blueJLibDir = rBlueJ.getSystemLibDir();
            File startupProject = new File(blueJLibDir, "startupProject");
            if (startupProject.equals(projectDir)) {
                return true;
            }
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 
     * Copies the src-DIR recursively into dst.
     * 
     * TODO make files read-only?
     * 
     * @param src
     * @param dst
     */
    private void copyDir(File src, File dst)
    {
        if (!src.isDirectory()) {
            return;
        }
        if (!dst.exists()) {
            dst.mkdirs();
        }
        File[] files = src.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            File newDst = new File(dst, file.getName());
            if (file.isDirectory()) {
                copyDir(file, newDst);
            }
            else {
                copyFile(file, newDst);
            }
        }
    }

    /**
     * Copies the src to dst. Creating parent dirs for dst. If dst exist it
     * overrides it.
     * 
     * @param src
     *            The source. It must be a file
     * @param dst
     *            Must not exist as a DIR
     */
    private void copyFile(File src, File dst)
    {
        if (!src.isFile() || dst.isDirectory()) {
            return;
        }
        dst.getParentFile().mkdirs();
        if (dst.exists()) {
            dst.delete();
        }
        try {
            BufferedInputStream is = new BufferedInputStream(new FileInputStream(src));
            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(dst));

            byte[] buffer = new byte[8192];
            int read = 0;
            while (read != -1) {
                os.write(buffer, 0, read);
                read = is.read(buffer);
            }
            os.flush();
            is.close();
            os.close();
        }
        catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {

        }
    }
}