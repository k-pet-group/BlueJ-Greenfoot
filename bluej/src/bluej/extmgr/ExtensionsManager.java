package bluej.extmgr;

import bluej.extensions.event.*;

import bluej.BlueJEvent;
import bluej.BlueJEventListener;
import bluej.Config;
import bluej.utility.Debug;
import bluej.debugger.ExecutionEvent;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.utility.DialogManager;

import java.util.Arrays;
import java.util.ArrayList;
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

        this.bluejLib = bluejLib;
        argsList = Collections.unmodifiableList(Arrays.asList(args));

        String dir = Config.getPropString("bluej.extensions.systempath", (String) null);
        if (dir == null)
            systemDir = new File(bluejLib, "extensions");
        else
            systemDir = new File(dir);

        userDir = Config.getUserConfigFile("extensions");

        extensions = new ArrayList();

        // This will also register the panel with BlueJ
        prefManager = new PrefManager(this);

        BlueJEvent.addListener(this);
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
        if (directory == null) return;

        File[] files = directory.listFiles();
        if (files == null) return;

        for (int index = 0; index < files.length; index++) {
            File thisFile = files[index];

            if (thisFile.isDirectory()) continue;

            if (!thisFile.getName().endsWith(".jar")) continue;

            // Ok, lets try to get a wrapper up and running
            ExtensionWrapper aWrapper = new ExtensionWrapper(this, prefManager, thisFile);

            // Loading this warpper failed miserably, too bad...
            if (!aWrapper.isJarValid()) continue;

            // Let me see if I already have this extension loaded
            if (isWrapperAlreadyLoaded(aWrapper)) continue;

            // This MUST be here in ANY case since othervise this wrapper is NOT on the list..
            extensions.add(aWrapper);

            // Now that all is nice and clean I can safely instantiate the extension
            aWrapper.newExtension(project);
        }
    }


    /**
     * Checks if the loaded wrappers/extensions IF this wrapper/extension is already loaded
     * In case of strange params... we return false, meaning that the given wrapper is NOT
     * loaded in the system... it is a reasonable response, afer all this wrapper is
     * not loaded...
     *
     * @param  thisWrapper  Description of the Parameter
     * @return              The wrapperAlreadyLoaded value
     */
    private boolean isWrapperAlreadyLoaded(ExtensionWrapper thisWrapper)
    {
        if (thisWrapper == null) return false;

        if (!thisWrapper.isJarValid()) return false;

        String thisClassName = thisWrapper.getExtensionClassName();
        String thisJarName   = thisWrapper.getExtensionFileName();

        for (Iterator iter = extensions.iterator(); iter.hasNext(); ) {
            ExtensionWrapper aWrapper = (ExtensionWrapper) iter.next();

            String aClassName = aWrapper.getExtensionClassName();
            if (aClassName == null) continue;

            // Found it, this wrapper is already loaded...
            if (thisClassName.equals(aClassName)) {
                Debug.message("isWrapperAlreadyLoaded==true: className=" +thisClassName+" jarName="+thisJarName);
                return true;
            }
        }

        // This wrapper is not already loaded in the list of wrappers/extensions
        return false;
    }


    /**
     *  Searches for and loads any new extensions found in the project.
     *  TODO: Two params are not used, remove them when you can do it.
     *
     *
     * @param  pmf        NOT USED
     * @param  toolsMenu  NOT USED
     * @param  project    The project I am opening
     */
    public void projectOpening(Project project, PkgMgrFrame pmf, JMenu toolsMenu)
    {
        File exts = new File(project.getProjectDir(), "extensions");
        loadAllExtensions(exts, project);
    }


    /**
     *  Informs extension wrappers that a package has been opened
     *
     * @param  pkg  the package that has just been opened.
     */
    public synchronized void packageOpened(Package pkg)
    {
        delegateEvent(new PackageEvent(PackageEvent.PACKAGE_OPENED, pkg));
    }


    /**
     *  This package frame is about to be closed.
     *  The issue here is to remove the extension if this is the right time to do it..
     *
     * @param  pkg  the package that is about to be closed
     */
    public synchronized void packageClosing(Package pkg)
    {
        delegateEvent(new PackageEvent(PackageEvent.PACKAGE_CLOSING, pkg));

        // Let's assume we are NOT going to delete the extension...
        boolean invalidateExtension = false;

        // Here comes the hard part of deciding IF to release the given wrapper/extension..
        Project thisProject = pkg.getProject();

        // Shurelly I cannot release anything if I don't know what I am talking about...
        if (thisProject == null) return;

        // The following CAN return null....
        PkgMgrFrame[] frameArray = PkgMgrFrame.getAllProjectFrames(thisProject);
        if (frameArray == null)
            invalidateExtension = true;
        else
            invalidateExtension = frameArray.length <= 1;

        // Nothing to do....
        if (!invalidateExtension)
            return;

        // I am closing the last frame of the project, time to invalidate the right extensions
        for (Iterator iter = extensions.iterator(); iter.hasNext(); ) {
            ExtensionWrapper aWrapper = (ExtensionWrapper) iter.next();

            // If the extension did not got loaded with this project skip it...
            if (thisProject != aWrapper.getProject())  continue;

            // The following terminated the Extension
            aWrapper.terminate();
            // and this removes the Wrapper from the list of wrappers.
            iter.remove();
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
    public void addMenuItems(Project project, PkgMgrFrame pmf)
    {
        pmf.toolsExtensionsCheckSeparator();

        for (Iterator iter = extensions.iterator(); iter.hasNext(); ) {
            ExtensionWrapper aWrapper = (ExtensionWrapper) iter.next();

            if (!aWrapper.isValid()) continue;

            MenuManager aManager = aWrapper.getMenuManager();
            if (aManager == null) continue;

            aManager.menuFrameRevalidateReq(pmf);
        }
    }


    /**
     * There is a need to know if there is at least one menu present.
     * AT the moemnt is just not to add a separator, but it may get
     * more useful in the future. The first menu that I find I just return
     * so this approach is not so bad in terms of performance.
     * It may be done in a better way in the future.
     *
     * @param  project  Description of the Parameter
     * @param  pmf      Description of the Parameter
     * @param  menu     Description of the Parameter
     * @return          Description of the Return Value
     */
    public boolean haveMenuItems(Project project, PkgMgrFrame pmf, JMenu menu)
    {
        for (Iterator iter = extensions.iterator(); iter.hasNext(); ) {
            ExtensionWrapper aWrapper = (ExtensionWrapper) iter.next();

            if (!aWrapper.isValid())  continue;
            
            MenuManager aManager = aWrapper.getMenuManager();
            if (aManager == null) continue;

            if (aManager.haveMenuItems(pmf)) return true;
        }

        return false;
    }



    /**
     *  Delegates an event to all known extensions.
     *
     * @param  event  the event to delegate
     */
    public void delegateEvent(BluejEvent event)
    {
        for (Iterator it = extensions.iterator(); it.hasNext(); ) {
            ExtensionWrapper wrapper = (ExtensionWrapper) it.next();
            wrapper.safeEventOccurred(event);
        }
    }



    /**
     *  This is called back when some sort of event occours.
     *  Depending on the event we will send it up adapted to the extension.
     *
     * @param  eventId  Get the list of event id from BlueJEvent
     * @param  arg      This really depends on that event is given
     */
    public void blueJEvent(int eventId, Object arg)
        {
/*        
        if ( eventId == BlueJEvent.EXECUTION_STARTED )
            {
            ExecutionEvent exevent = (ExecutionEvent) arg;
            delegateEvent ( new InvocationEvent ( eventId, exevent ) );
            return;              
            }
*/
        if ( eventId == BlueJEvent.EXECUTION_RESULT )
            {
            ExecutionEvent exevent = (ExecutionEvent) arg;
            delegateEvent ( new ResultEvent ( eventId, exevent ) );
            return;              
            }

        if ( eventId == BlueJEvent.CREATE_VM_DONE) 
            {
            delegateEvent (new ApplicationEvent (ApplicationEvent.APP_READY_EVENT));
            return;
            }

        // I cannot put any warining on unknown events here since I get a bunch of events in any case.
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
}
