package bluej.extensions;

import bluej.extensions.event.*;
import bluej.extmgr.ExtensionWrapper;
import bluej.extmgr.PrefManager;
import bluej.extmgr.MenuManager;

import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.Config;
import bluej.utility.DialogManager;
import java.awt.Frame;


import java.util.*;
import java.io.File;

/**
 * A proxy object which provides services to BlueJ extensions. 
 * From this class
 * an extension can obtain the projects and packages which BlueJ is currently displayng
 * and the classes and objects they contain. Fields and methods of these objects 
 * can be inspected and invoked using an API based on Java's reflection API.
 * 
 * Every effort has been made to retain the logic of the Reflection API and to provide
 * methods that behave in a very similar way.
 * 
 * <PRE>
 * BlueJ
 *   |
 *   +---- BProject
 *             |
 *             +---- BPackage
 *                      |
 *                      +--------- BClass
 *                      |            |
 *                      +- BObject   + BConstructor
 *                                   |      |
 *                                   |      +- BObject
 *                                   |
 *                                   +---- BMethod
 *                                   |      |
 *                                   |      +- BObject
 *                                   |
 *                                   +---- BField
 *    
 * </PRE>
 * @version $Id: BlueJ.java 1870 2003-04-22 11:41:27Z damiano $
 */

/*
 * Author Clive Miller, University of Kent at Canterbury, 2002
 * Author Damiano Bolla, University of Kent at Canterbury, 2003
 */

public class BlueJ
{
    private final ExtensionWrapper myWrapper;
    private final PrefManager      prefManager;
    private final MenuManager      menuManager;
    
    private PreferenceGenerator    currentPrefGen=null;
    private MenuGenerator          currentMenuGen=null;
    private Properties             localLabels;


    private ArrayList eventListeners;       // This is the queue for the whole of them
    private ArrayList bluejReadyListeners;  // This is just for the BlueJReady event


    /**
     * Constructor for a BlueJ proxy object.
     * See the ExtensionBridge class
     */
    BlueJ (ExtensionWrapper aWrapper, PrefManager aPrefManager, MenuManager aMenuManager)
    {
        myWrapper   = aWrapper;
        prefManager = aPrefManager;
        menuManager = aMenuManager;

        eventListeners = new ArrayList();
        bluejReadyListeners = new ArrayList();
        
        /* I do NOT want lazy initialization otherwise I may try to load it
         * may times just because I cannof find anything.
         * Or having state variables to know I I did load it but had nothing found
         */
        localLabels = myWrapper.getLabelProperties();
    }
    
    

    /**
     * Opens a project.
     * 
     * @param directory Where the project is stored.
     * @return the BProject that describes the newly opened project or null if it cannot be opened.
     */
    public BProject openProject (File directory)
    {
        // Yes somebody may just call it with null, for fun... TODO: Needs error reporting
        if ( directory == null ) return null;
        
        Project openProj = Project.openProject (directory.getAbsolutePath());
        if(openProj == null) return null;
        
        Package pkg = openProj.getPackage(openProj.getInitialPackageName());
        if ( pkg == null ) return null;

        PkgMgrFrame pmf = getPackageFrame ( pkg );
        pmf.show();
        return new BProject (openProj);
    }


    /**
     * Simple utility to make code cleaner
     */
    private PkgMgrFrame getPackageFrame ( Package thisPkg )
        {
        PkgMgrFrame pmf = PkgMgrFrame.findFrame(thisPkg);
        // If for some reason we already have a frame for this package, return it
        if ( pmf != null ) return pmf;

        PkgMgrFrame recentFrame = PkgMgrFrame.getMostRecent();
        if (recentFrame != null && recentFrame.isEmptyFrame() )
          {
          // If, by chance, the current fram is an empty one, use it !
          recentFrame.openPackage(thisPkg);
          return recentFrame;
          }

        // No empty fram I can use, I need to create a new one
        pmf = PkgMgrFrame.createFrame(thisPkg);
        // Yes, recent frame may teoretically be null.
        if ( recentFrame != null ) DialogManager.tileWindow(pmf, recentFrame);
        return pmf;
        }
        
    /**
     * Creates a new BlueJ project.
     *
     * @param directory where you want the project be placed, it must be writable.
     * @return the newly created BProject if successful, null otherwise.
     */
    public BProject newProject (File directory)
    {
        String pathString = directory.getAbsolutePath();
        if (!pathString.endsWith (File.separator)) pathString += File.separator;
        if  ( ! Project.createNewProject(pathString) ) return null;
        return openProject ( directory );
    }



    /**
     * Returns all currently open projects.
     * Returns an empty array if no projects are open.
     */
    public BProject[] getOpenProjects()
        {
        // If this extension is not valid return an empty array.
        if (!myWrapper.isValid()) return new BProject[0];

        Set projSet = Project.getProjectKeySet();
        BProject [] result = new BProject[projSet.size()];
        Iterator iter = projSet.iterator();
        int insIndex = 0;
        
        while ( iter.hasNext() )
            {
            Object projKey = iter.next();
            result[insIndex++] = new BProject ((File)projKey);
            }

        return result;
        }


    /**
     * Returns the currently selected package.
     * The current package is the one that is currently selected by the
     * user interface.
     * It can return null if there is no currently open package.
     */
    public BPackage getCurrentPackage()
    {
        // This is here and NOT into a BProject since it depends on user interface.
        
        PkgMgrFrame pmf = PkgMgrFrame.getMostRecent();
        // If there is nothing at all open there is no Frame open...
        if (pmf == null) return null;

        Package pkg = pmf.getPackage();
        // The frame may be there BUT have no package. 
        // I do NOT want to create what is NOT there
        if ( pkg == null ) return null;

        return new BPackage (pkg);
    }

    /**
     * Returns the current frame being displayed. 
     * Can be used (e.g.) as a "parent" frame for positioning modal dialogs.
     * If there is a package currently open, it's probably better to use its <code>getFrame()</code>
     * method to provide better placement.
     */
    public Frame getCurrentFrame()
    {
        return PkgMgrFrame.getMostRecent();
    }


    /**
     * Install a new menu generator for this extension.
     * If you want to delete a previously installed menu, then set it to null
     * 
     * @param MenuGenerator a Class instance that implements the MenuGenerator interface
     */
    public void setMenuGenerator ( MenuGenerator menuGen )
    {
        currentMenuGen = menuGen;
        menuManager.menuExtensionRevalidateReq();
    }

    /**
     * Returns the currently registered menu generator
     */
    public MenuGenerator getMenuGenerator ()
    {
        return currentMenuGen;
    }

    /**
     * Install a new preference panel for this extension.
     * If you want to delete a previously installed preference panel, then set it to null
     * 
     * @param prefGen a class instance that implements the PreferenceGenerator interface.
     */
    public void setPreferenceGenerator(PreferenceGenerator prefGen)
    {
        currentPrefGen = prefGen;
        prefManager.panelRevalidate();
    }
    
    /**
     * Returns the currently registered preference generator.
     */
    public PreferenceGenerator getPreferenceGenerator()
    {
        return currentPrefGen;
    }


    /**
     * Returns the path of the <code>&lt;BLUEJ_HOME&gt;/lib</code> system directory.
     * This can be used to locate systemwide configuration files.
     * Having the directory you can then locate a file within it.
     */
    public File getSystemLibDir()
    {
        return Config.getBlueJLibDir();
    }

    /**
     * Returns the path of the user configuration directory.
     * This can be used to locate user dependent information.
     * Having the directory you can then locate a file within it.
     */
    public File getUserConfigDir ()
    {
        return Config.getUserConfigDir();
    }
    

    /**
     * Returns a property from BlueJ's properties, 
     * or the given default value if the property is not currently set.
     * 
     * @param property The name of the required global property
     * @param def The default value to use if the property cannot be found.
     * @return the value of the property.
     */
    public String getBlueJPropertyString (String property, String def)
    {
        return Config.getPropString ( property, def);
    }

     /**
      * Return a property associated with this extension from the standard BlueJ property repository.
      * You must use the setExtensionPropertyString to write any property that you want stored.
      * You can then come back and retrieve it using this function.
      * 
      * @param property The name of the required global property.
      * @param def The default value to use if the property cannot be found.
      * @return the value of that property.
      */
    public String getExtensionPropertyString (String property, String def)
    {
        String thisKey = myWrapper.getSettingsString ( property );
        return Config.getPropString (thisKey, def);
    }
     
     /**
      * Sets a property associated with this extension into the standard BlueJ property repository.
      * The property name does not need to be fully qualified since a prefix will be prepended to it.
      * 
      * @param property The name of the required global property
      * @param value the required value of that property.
      */
    public void setExtensionPropertyString (String property, String value)
    {
        String thisKey = myWrapper.getSettingsString ( property );
        Config.putPropString (thisKey, value);
    }
    
    /**
     * Returns the language-dependent label with the given key.
     * The search order is to look first in the extension's <code>label</code> files and 
     * if the requested label is not found in the BlueJ system <code>label</code> files.
     * Extensions' labels are stored in a Property format and must be jarred together
     * with the extension. The path searched is equivalent to the bluej/lib/[language]
     * style used for the BlueJ system labels. E.g. to create a set of labels which can be used
     * by English, Italian and German users of an extension, the following files would need to
     * be present in the extension's Jar file:
     * <pre>
     * lib/english/label
     * lib/italian/label
     * lib/german/label
     * </pre>
     * The files named <code>label</code> would contain the actual label key/value pairs.
     */
    public String getLabel (String key)
    {
        // If there are no label for this extension I can only return the system ones.
        if ( localLabels == null ) return Config.getString (key, key);

        // In theory there are label for this extension let me try to get them
        String aLabel = localLabels.getProperty (key, null);

        // Found what I wanted, job done.
        if ( aLabel != null ) return aLabel;

        // ok, the only hope is to get it from the system
        return Config.getString (key, key);
    }



    /**
     * Registers a listener for events generated by BlueJ.
     * This listener will get all events generated by BlueJ.
     */
    public void addBlueJExtensionEventListener (BlueJExtensionEventListener listener)
    {
        if (listener == null) return;

        eventListeners.add(listener);
    }


    /**
     * Removes the specified listener so no that it no longer receives BlueJExtensionsEvents.
     */
    public void removeBlueJExtensionEventListener (BlueJExtensionEventListener listener)
    {
        if (listener == null) return;

        eventListeners.remove(listener);
    }

    /**
     * Registers a listener for BlueJReady events.
     */
    public void addBlueJReadyEventListener (BlueJReadyEventListener listener)
    {
        if (listener == null) return;

        bluejReadyListeners.add(listener);
    }


    /**
     * Removes the specified listener so no that it no longer receives BlueJExtensionsEvents.
     */
    public void removeBlueJReadyEventListener (BlueJReadyEventListener listener)
    {
        if (listener == null) return;

        bluejReadyListeners.remove(listener);
    }



    /**
     * Dispatch this event to the listeners for the ALL events.
     */
    private void delegateExtensionEvent ( BlueJExtensionEvent event )
        {
        // I do not bother to check for emptiness, the iterator will fail quick !

        for (Iterator iter = eventListeners.iterator(); iter.hasNext(); ) 
            {
            BlueJExtensionEventListener eventListener = (BlueJExtensionEventListener)iter.next();
            eventListener.eventOccurred(event);
            }
        }

    /**
     * Dispatch this event to the listeners for the bluejReady events.
     */
    private void delegateBluejReadyEvent ( BlueJReadyEvent event )
        {
        // I do not bother to check for emptiness, the iterator will fail quick !

        for (Iterator iter = bluejReadyListeners.iterator(); iter.hasNext(); ) 
            {
            BlueJReadyEventListener eventListener = (BlueJReadyEventListener)iter.next();
            eventListener.BlueJReady(event);
            }
        }



    /**
     * Informs any registered listeners that an event has occurred.
     * This will call the various dispatcher as needed.
     * Errors will be trapped by the caller.
     */
    void delegateEvent (BlueJExtensionEvent event)
      {
      delegateExtensionEvent ( event );
      if ( event instanceof BlueJReadyEvent ) delegateBluejReadyEvent ((BlueJReadyEvent)event);
      }
    
}
