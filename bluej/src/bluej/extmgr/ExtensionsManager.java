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
 * Manages extensions and provides the main interface to PkgMgrFrame.
 */
public class ExtensionsManager implements BlueJEventListener
{
    private static ExtensionsManager instance;
    
    /**
     * Processes arguments passed to BlueJ, and takes a note of
     * the location of BlueJLib (<CODE>&lt;bluej&gt;/lib</CODE>).
     * A call to this method must be made before any other action to
     * this class.
     * @param args the String array passed to Main
     * @param bluejLib the directory which is <CODE>&lt;bluej&gt;/lib</CODE>
     */
    public static void initialise (String[] args, File bluejLib)
    {
        if (instance != null) throw new RuntimeException ("ExtensionsManager already initialised!");
        instance = new ExtensionsManager (args, bluejLib);
//        instance.loadExtensions();
    }

    
    public static ExtensionsManager getExtMgr()
    {
        return instance;
    }

   
    /**
     * Convenience method to ensure uniformity of preference items
     * @param ew the wrapper of the extension to which to apply the key
     * @param a key uniquely identifying this item
     * @return an appropriate string to identify the preference item
     */
    public static String getPreferencesString (ExtensionWrapper ew, String key)
    {
        return "extensions."+ew.getExtensionClass().getName()+".preferences."+key;
    }
    
    /**
     * Convenience method to ensure uniformity of settings items
     * @param ew the wrapper of the extension to which to apply the key
     * @param a key uniquely identifying this item
     * @return an appropriate string to identify the setting
     */
    public static String getSettingsString (ExtensionWrapper ew, String key)
    {
        return "extensions."+ew.getExtensionClass().getName()+".settings."+key;
    }
            
    private final List argsList;
    private final List extensions;
    private final File bluejLib;
    private final File systemDir;
    private final File userDir;

    private ExtensionsManager (String[] args, File bluejLib)
    {
        try {
            ClassLoader cl = ExtensionsManager.class.getClassLoader();
            cl.loadClass ("bluej.extensions.Extension");
        } catch (ClassNotFoundException ex) {
            DialogManager.showText (null, "Error: you need bluejext.jar as well as bluej.jar in your path!");
            System.exit (-1);
        }
        
        argsList = Collections.unmodifiableList (Arrays.asList (args));
        this.bluejLib = bluejLib;

        String dir = Config.getPropString ("bluej.extensions.systempath", (String)null);
        if (dir == null) {
            systemDir = new File (bluejLib, "extensions");
        } else {
            systemDir = new File (dir);
        }

        userDir = Config.getUserConfigFile ("extensions");
        ExtPrefPanel.register();

        extensions = new ArrayList(); // of ExtensionWrapper
        BlueJEvent.addListener (this);
    }
    
    public void loadExtensions()
    {
        extensions.addAll(ExtensionWrapper.findValid(systemDir, null));
        extensions.addAll(ExtensionWrapper.findValid(userDir, null));
    }
    
    /**
     * Returns a reminder as to where to find <CODE>&lt;bluej&gt;/lib</CODE>.
     * @return a <CODE>File</CODE> which is an existing directory.
     */
    public File getBlueJLib()
    {
        return bluejLib;
    }
    
    /**
     * Returns an unmodifiable list of the arguments passed to BlueJ.
     * @return a List containing each space-delimited parameter.
     */
    public List getArgs()
    {
        return argsList;
    }
    
    /**
     * Returns an unmodifiable list of extensions.
     * @return an unmodifiable list of the Extensions, but the elements themselves are not protected.
     */
    public synchronized List getExtensions()
    {
        return Collections.unmodifiableList (extensions);
    }

    /**
     * Searches for and loads any new extensions found in the project.
     * @param projectDir the directory of the project being opened
     * @param pmf the frame that will contain the project
     * @param toolsMenu if not <code>null</code>, a currently empty frame is going to be used, so
     * any new menu items must be added at this time
     */
    public void projectOpening (Project project, PkgMgrFrame pmf, JMenu toolsMenu)
    {
        File exts = new File (project.getProjectDir(), "+extensions");
        Collection newExtensions = ExtensionWrapper.findValid (exts, project);
        if (toolsMenu != null) {
            for (Iterator it = newExtensions.iterator(); it.hasNext();) {
                ExtensionWrapper ew = (ExtensionWrapper) it.next();
                ew.addMenuItems (pmf, toolsMenu);
            }
        }
            
        extensions.addAll (newExtensions);
    }        

    /**
     * Informs extension wrappers that a package has been opened
     * @param pkg the package that has just been opened.
     */
    public synchronized void packageOpened (Package pkg)
    {
        delegateEvent (new PackageEvent (PackageEvent.PACKAGE_OPENED, new BPackage (pkg)));
    }
    
    /**
     * This package frame is about to be closed.
     * @param pkg the package that is about to be closed
     */
    public synchronized void packageClosing (Package pkg)
    {
        delegateEvent (new PackageEvent (PackageEvent.PACKAGE_CLOSING, new BPackage (pkg)));

        boolean invalidate = PkgMgrFrame.getAllProjectFrames (pkg.getProject()).length == 1; // last package of this project
        for (Iterator it = extensions.iterator();it.hasNext();) {
            ExtensionWrapper ew = (ExtensionWrapper)it.next();
            if (invalidate && ew.getProject() == pkg.getProject()) {
                ew.invalidate();
                it.remove();
            }
        }
    }
    
    /**
     * Adds extension menu items to a newly created frame
     * @param menu the menu to which to add menu items
     * @param project the project, so that extensions belonging to other projects
     * do not get their menu items added
     */
    public void addMenuItems (Project project, PkgMgrFrame pmf, JMenu menu)
    {
        for (Iterator it = extensions.iterator();it.hasNext();) {
            ExtensionWrapper ew = (ExtensionWrapper)it.next();
            if (ew.getProject() == project) { // NB remember null==null !!
                ew.addMenuItems (pmf, menu);
            }
        }
    }
    
    /**
     * Adds an extension to the manager's list. This does not copy any files,
     * nor does it load it. <CODE>loadExtensions</CODE> should then be called.
     * @param path the path where the extension can be found. This should be in
     * <CODE><user.home>/bluej/extensions/*.jar</CODE>, but could be anywhere.
     */
    public synchronized void addExtension (File path)
    {
        ExtensionWrapper ew = ExtensionWrapper.createExtensionWrapper (path, null);
        if (ew != null) extensions.add (ew);
    }
        
    /**
     * Removes this extension from the manager's list and invalidates it.
     */
    public synchronized void removeExtension (ExtensionWrapper ew)
    {
        ew.invalidate();
        extensions.remove (ew);
    }


    /**
     * Delegates an event to all known extensions
     * @param event the event to delegate
     */
    private void delegateEvent (BJEvent event)
    {
        for (Iterator it = extensions.iterator(); it.hasNext();) {
            ExtensionWrapper ew = (ExtensionWrapper) it.next();
            ew.eventOccurred (event);
        }
    }
    
    public void blueJEvent (int eventId, Object arg)
    {
        if (eventId == BlueJEvent.EXECUTION_STARTED) {
            ExecutionEvent exevent = (ExecutionEvent)arg;
            delegateEvent (new InvocationEvent (new BPackage (exevent.getPackage()),
                                                exevent.getClassName(), 
                                                exevent.getObjectName(), 
                                                exevent.getMethodName(), 
                                                exevent.getSignature(), 
                                                exevent.getParameters()));
        }
        else if (eventId == BlueJEvent.EXECUTION_RESULT) {
            ExecutionEvent exevent = (ExecutionEvent)arg;
            String result = "Unknown";
            if (exevent.getResult() == ExecutionEvent.NORMAL_EXIT) result = InvocationEvent.NORMAL_EXIT;
            else if (exevent.getResult() == ExecutionEvent.FORCED_EXIT) result = InvocationEvent.FORCED_EXIT;
            else if (exevent.getResult() == ExecutionEvent.EXCEPTION_EXIT) result = InvocationEvent.EXCEPTION_EXIT;
            else if (exevent.getResult() == ExecutionEvent.TERMINATED_EXIT) result = InvocationEvent.TERMINATED_EXIT;
            delegateEvent (new InvocationEvent (new BPackage (exevent.getPackage()), result));
        }
    }
}