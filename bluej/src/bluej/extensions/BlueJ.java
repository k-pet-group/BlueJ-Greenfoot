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
 * Provides services to BlueJ extensions. 
 * This is the top-level object of the proxy hierarchy, from this class
 * an extension can obtain the projects and packages which BlueJ is currently displayng
 * and the Classes and Objects they contain. Fields and Mehods of these Objects 
 * can be inspected and invoked using API based on the logic of Reflection API.
 * 
 * Every effort has been made to retain the logic of Reflection API and to provide
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
 *                      +- BObject   + BConstuctor
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
 * @version $Id: BlueJ.java 1807 2003-04-10 10:28:21Z damiano $
 */

public class BlueJ
{
    private final ExtensionWrapper myWrapper;
    private final PrefManager      prefManager;
    private final MenuManager      menuManager;
    
    private PreferenceGenerator    currentPrefGen=null;
    private MenuGenerator          currentMenuGen=null;
    private Properties             localLabels;

    /**
     * NOT to be used by Extension writer.
     */
    public BlueJ (ExtensionWrapper i_myWrapper, PrefManager i_prefManager, MenuManager i_menuManager)
    {
        myWrapper   = i_myWrapper;
        prefManager = i_prefManager;
        menuManager = i_menuManager;

        /* I do NOT want lazy initialization othervise I may try to load it
         * may times just because I cannof find anything.
         * Or having state variables to know I I did load it but had nothing found
         */
        localLabels = myWrapper.getLabelProperties();
    }
    
    

    /**
     * Opens a project.
     * 
     * @param directory Give the directory where the project is.
     * @return the BProject that describes the newly opened project or null if it cannot be opened
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
     * Creates new BlueJ project.
     *
     * @param directory where you want the project be placed. It must be writable.
     * @return the newly created BProject if successful. null otherwise.
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
     * It can be an empty array if no projects are open.
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
     * Returns the currently selected Package.
     * It can return null if this information is not available.
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
     * Gets the current Frame being displayed. 
     * Used for modal dialog or similar.
     * Use this one when there are NO packages open.
     */
    public Frame getCurrentFrame()
    {
        return PkgMgrFrame.getMostRecent();
    }


    /**
     * Install a new menu generator for this extension.
     * If you want no menus then set it to null
     * 
     * @param MenuGenerator a Class instance that implements the MenuGenerator interface
     */
    public void setMenuGenerator ( MenuGenerator menuGen )
    {
        currentMenuGen = menuGen;
        menuManager.menuExtensionRevalidateReq();
    }

    /**
     * Returns the currently registered MenuGen instance
     */
    public MenuGenerator getMenuGenerator ()
    {
        return currentMenuGen;
    }

    /**
     * Install a new preference panel for this extension.
     * If you want to delete it set prefGen to null
     * 
     * @param prefGen a class instance that implements the PrefGen interface.
     */
    public void setPreferenceGenerator(PreferenceGenerator prefGen)
    {
        currentPrefGen = prefGen;
        prefManager.panelRevalidate();
    }
    
    /**
     * Returns the currently registered PreferenceGenerator instance.
     */
    public PreferenceGenerator getPreferenceGenerator()
    {
        return currentPrefGen;
    }


    /**
     * Returns the arguments with which BlueJ was started.
     * The return value is a List of Strings.
     */
    public List getArgs()
    {
        return myWrapper.getArgs();
    }
    
    /**
     * Returns the path to the BlueJ/lib system directory.
     * This is used to locate systemwide configuration files.
     * Having the Directory you can then located a file within it.
     */
    public File getSystemLibDir()
    {
        return myWrapper.getBlueJLib();
    }

    /**
     * Returns the path of the user configuration directory.
     * This is used to locate user dependent information.
     * Having the Directory you can then located a file within it.
     */
    public File getUserConfigDir ()
    {
        return Config.getUserConfigDir();
    }
    
    /**
     * Registers an event listener for events generated by Bluej.
     * 
     * @param listener an instance of a class that implements the BluejEventListener interface
     */
    public void addBlueJExtensionEventListener (BlueJExtensionEventListener listener)
    {
        myWrapper.addBluejEventListener (listener);
    }


     /**
      * Returns a property from BlueJ properties, includes a default value.
      * 
      * @param property The name of the required global property
      * @param def The default value to use if the property cannot be found.
      * @return the value of that property.
      */
    public String getBlueJPropertyString (String property, String def)
    {
        return Config.getPropString ( property, def);
    }

     /**
      * Returns a property from Extensions properties file, includes a default value.
      * You must use the setExtensionPropertyString to write the property that you want stored.
      * You can then come back and retrieve it using this function.
      * 
      * @param property The name of the required global property
      * @param def The default value to use if the property cannot be found
      * @return the value of that property
      */
    public String getExtensionPropertyString (String property, String def)
    {
        String thisKey = myWrapper.getSettingsString ( property );
        return Config.getPropString (thisKey, def);
    }
     
     /**
      * Sets a property into Extensions properties file.
      * The property name does not needs to be fully qulified since a prefix will be prepended to it.
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
     * Returns a language-dependent label.
     * The search order is to look first into extensions labels and if not found into 
     * systems label.
     * Extensions labels are stored in a Property format and must be jarred together
     * with the extension. The path being searched is equivalent to the bluej/lib/[language]
     * style used for the bluej system labels. An Example can be:
     * <pre>
     * lib/english/label
     * lib/italian/label
     * lib/german/label
     * </pre>
     * In the above example "label" at the end is a file that caontains the actual label values.
     */
    public String getLabel (String thisId )
    {
        // If there are no label for this extension I can only return the system ones.
        if ( localLabels == null ) return Config.getString (thisId, thisId);

        // In theory there are label for this extension let me try to get them
        String aLabel = localLabels.getProperty (thisId, null);

        // Found what I wanted, job done.
        if ( aLabel != null ) return aLabel;

        // ok, the only hope is to get it from the system
        return Config.getString (thisId, thisId);
    }
    
    
    /**
     * Request for BlueJ to close.
     */
    public void closeBlueJ()
    {
        PkgMgrFrame.getMostRecent().wantToQuit();
    }
}