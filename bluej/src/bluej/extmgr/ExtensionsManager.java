package bluej.extmgr;

import bluej.extensions.BPackage;
import bluej.extensions.event.BJEvent;
import bluej.extensions.event.InvocationEvent;
import bluej.extensions.event.PackageEvent;

import bluej.BlueJEvent;
import bluej.BlueJEventListener;
import bluej.Config;
import bluej.debugger.ExecutionEvent;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.utility.DialogManager;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.io.File;
import javax.swing.JMenu;

/**
 *  Manages extensions and provides the main interface to PkgMgrFrame.
 */
public class ExtensionsManager implements BlueJEventListener
{
    private static ExtensionsManager instance;


    /**
     *  Processes arguments passed to BlueJ, and takes a note of the location of
     *  BlueJLib (<CODE>&lt;bluej&gt;/lib</CODE>). A call to this method must be
     *  made before any other action to this class.
     *
     * @param  args      the String array passed to Main
     * @param  bluejLib  the directory which is <CODE>&lt;bluej&gt;/lib</CODE>
     */
    public static void initialise(String[] args, File bluejLib)
    {
        if (instance != null)
            throw new RuntimeException("ExtensionsManager already initialised!");
        instance = new ExtensionsManager(args, bluejLib);
    }


    /**
     *  Gets the extMgr attribute of the ExtensionsManager class
     *
     * @return    The extMgr value
     */
    public static ExtensionsManager getExtMgr()
    {
        return instance;
    }



    private final List argsList;
    private final List extensions;
    private final File bluejLib;
    private final File systemDir;
    private final File userDir;
    private PrefManager prefManager;


    /**
     *  Constructor for the ExtensionsManager object
     *
     * @param  args      Description of the Parameter
     * @param  bluejLib  Description of the Parameter
     */
    private ExtensionsManager(String[] args, File bluejLib)
    {
        try {
            ClassLoader cl = ExtensionsManager.class.getClassLoader();
            cl.loadClass("bluej.extensions.Extension");
        } catch (ClassNotFoundException ex) {
            DialogManager.showText(null, "Error: you need bluejext.jar as well as bluej.jar in your path!");
            System.exit(-1);
        }

        argsList = Collections.unmodifiableList(Arrays.asList(args));
        this.bluejLib = bluejLib;

        String dir = Config.getPropString("bluej.extensions.systempath", (String) null);
        if (dir == null)
            systemDir = new File(bluejLib, "extensions");
        else
            systemDir = new File(dir);

        userDir = Config.getUserConfigFile("extensions");

        extensions = new ArrayList();
        // of ExtensionWrapper

        // This will also register the panel with BlueJ
        prefManager = new PrefManager(this);

        BlueJEvent.addListener(this);
    }



    /**
     *  Convenience method to ensure uniformity of preference items
     *
     * @param  ew   the wrapper of the extension to which to apply the key
     * @param  key  Description of the Parameter
     * @return      an appropriate string to identify the preference item
     */
    public static String getPreferencesString(ExtensionWrapper ew, String key)
    {
        return "extensions." + ew.getExtensionClass().getName() + ".preferences." + key;
    }


    /**
     *  Convenience method to ensure uniformity of settings items
     *
     * @param  ew   the wrapper of the extension to which to apply the key
     * @param  key  Description of the Parameter
     * @return      an appropriate string to identify the setting
     */
    public static String getSettingsString(ExtensionWrapper ew, String key)
    {
        return "extensions." + ew.getExtensionClass().getName() + ".settings." + key;
    }


    /**
     *  Quite a simple one, just not to change the main BlueJ code.
     */
    public void loadExtensions()
    {
        loadAllExtensions(systemDir, null);
        loadAllExtensions(userDir, null);
    }


    /**
     *  Searches through the given directory for jar files that contain a valid
     *  extension. If it finds a loadable extension it will add it to the loaded
     *  extensions... This IS the function that should be called to load
     *  extensions
     *
     * @param  directory  Description of the Parameter
     * @param  project    Description of the Parameter
     */
    private void loadAllExtensions(File directory, Project project)
    {
        if (directory == null)
            return;

        File[] files = directory.listFiles();
        if (files == null)
            return;

        for (int index = 0; index < files.length; index++) {
            File thisFile = files[index];

            if (thisFile.isDirectory())
                continue;

            if (!thisFile.getName().endsWith(".jar"))
                continue;

            // I should REALLY look for already loaded extensions !!!
            System.out.println("loading=" + thisFile.toString());

            // We may argue endlessely if I should add it or not...
            ExtensionWrapper aWrapper = new ExtensionWrapper(this, prefManager, thisFile);

            // This MUST be here in ANY case since othervise this wrapper is NOT on the list..
            extensions.add(aWrapper);

            // Wehn a new extension is loaded its wrapper MUST already be on the list of wrappers!
            if (aWrapper.isJarValid())
                aWrapper.newExtension(project);

        }
    }


    /**
     *  Returns a reminder as to where to find <CODE>&lt;bluej&gt;/lib</CODE>.
     *
     * @return    a <CODE>File</CODE> which is an existing directory.
     */
    public File getBlueJLib()
    {
        return bluejLib;
    }


    /**
     *  Returns an unmodifiable list of the arguments passed to BlueJ.
     *
     * @return    a List containing each space-delimited parameter.
     */
    public List getArgs()
    {
        return argsList;
    }


    /**
     *  Returns an unmodifiable list of extensions.
     *
     * @return    an unmodifiable list of the Extensions, but the elements
     *      themselves are not protected.
     */
    public synchronized List getExtensions()
    {
        return Collections.unmodifiableList(extensions);
    }


    /**
     *  Searches for and loads any new extensions found in the project.
     *
     * @param  pmf        the frame that will contain the project
     * @param  toolsMenu  if not <code>null</code>, a currently empty frame is
     *      going to be used, so any new menu items must be added at this time
     * @param  project    Description of the Parameter
     */
    public void projectOpening(Project project, PkgMgrFrame pmf, JMenu toolsMenu)
    {
        File exts = new File(project.getProjectDir(), "+extensions");
        loadAllExtensions(exts, project);
    }


    /**
     *  Informs extension wrappers that a package has been opened
     *
     * @param  pkg  the package that has just been opened.
     */
    public synchronized void packageOpened(Package pkg)
    {
        delegateEvent(new PackageEvent(PackageEvent.PACKAGE_OPENED, new BPackage(pkg)));
    }


    /**
     *  This package frame is about to be closed.
     *
     * @param  pkg  the package that is about to be closed TODO: Manage the
     *      release of extensions.....
     */
    public synchronized void packageClosing(Package pkg)
    {
        delegateEvent(new PackageEvent(PackageEvent.PACKAGE_CLOSING, new BPackage(pkg)));

        boolean invalidate = PkgMgrFrame.getAllProjectFrames(pkg.getProject()).length == 1;
        // last package of this project
        for (Iterator it = extensions.iterator(); it.hasNext(); ) {
            ExtensionWrapper ew = (ExtensionWrapper) it.next();
            if (invalidate && ew.getProject() == pkg.getProject()) {
                ew.invalidate();
                it.remove();
            }
        }
    }


    /**
     *  Adds extension menu items to a newly created frame
     *
     * @param  menu     TO BE DELETED, it is always the tools menu
     * @param  project  the project, so that extensions belonging to other
     *      projects do not get their menu items added, we need o talk about
     *      it... What this should do is to go trough all valid extensions and
     *      revalidate their menu against this frame. This MUST be called from a
     *      swing thread...
     * @param  pmf      The feature to be added to the MenuItems attribute
     */
    public void addMenuItems(Project project, PkgMgrFrame pmf, JMenu menu)
    {
        Iterator iter = extensions.iterator();
        while (iter.hasNext()) {
            ExtensionWrapper aWrapper = (ExtensionWrapper) iter.next();
            if (!aWrapper.isValid())
                continue;

            MenuManager aManager = aWrapper.getMenuManager();
            if (aManager == null)
                continue;

            aManager.menuFrameRevalidateReq(pmf);
        }
    }


    /**
     *  Delegates an event to all known extensions
     *
     * @param  event  the event to delegate
     */
    private void delegateEvent(BJEvent event)
    {
        for (Iterator it = extensions.iterator(); it.hasNext(); ) {
            ExtensionWrapper ew = (ExtensionWrapper) it.next();
            ew.eventOccurred(event);
        }
    }


    /**
     *  Description of the Method
     *
     * @param  eventId  Description of the Parameter
     * @param  arg      Description of the Parameter
     */
    public void blueJEvent(int eventId, Object arg)
    {
        if (eventId == BlueJEvent.EXECUTION_STARTED) {
            ExecutionEvent exevent = (ExecutionEvent) arg;
            delegateEvent(new InvocationEvent(new BPackage(exevent.getPackage()),
                    exevent.getClassName(),
                    exevent.getObjectName(),
                    exevent.getMethodName(),
                    exevent.getSignature(),
                    exevent.getParameters()));
            return;
        }

        if (eventId == BlueJEvent.EXECUTION_RESULT) {
            ExecutionEvent exevent = (ExecutionEvent) arg;
            String result = "Unknown";
            if (exevent.getResult() == ExecutionEvent.NORMAL_EXIT)
                result = InvocationEvent.NORMAL_EXIT;
            else if (exevent.getResult() == ExecutionEvent.FORCED_EXIT)
                result = InvocationEvent.FORCED_EXIT;
            else if (exevent.getResult() == ExecutionEvent.EXCEPTION_EXIT)
                result = InvocationEvent.EXCEPTION_EXIT;
            else if (exevent.getResult() == ExecutionEvent.TERMINATED_EXIT)
                result = InvocationEvent.TERMINATED_EXIT;

            delegateEvent(new InvocationEvent(new BPackage(exevent.getPackage()), result));
        }
    }
}
