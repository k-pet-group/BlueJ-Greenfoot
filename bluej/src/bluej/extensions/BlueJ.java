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
 * This is the top-level object of the proxy hierarchy, bear in mind that
 * there is much similarity between the Reflection API and this API.
 * Every effort has been made to retain the logic of Reflection and to provide
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
 * @version $Id: BlueJ.java 1723 2003-03-21 11:19:28Z damiano $
 */

public class BlueJ
{
    private final ExtensionWrapper myWrapper;
    private final PrefManager      prefManager;
    private final MenuManager      menuManager;
    
    private PrefGen    currentPrefGen=null;
    private MenuGen    currentMenuGen=null;
    private Properties localLabels;

    /**
     * Extensions should not call this constructor.
     * The BlueJ object is given to the Extension by the system.
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
     * Opens a project
     * 
     * @param directory Give the directory where the project is.
     * @return the BProject that describes the newly opened or null if it cannot be opened
     */
    public BProject openProject (File directory)
    {
        // Yes somebody may just call it with null, for fun... TODO: Needs error reporting
        if ( directory == null ) return null;
        
        PkgMgrFrame currentFrame = PkgMgrFrame.getMostRecent();
        if (currentFrame == null) return null;
        
        Project openProj = Project.openProject (directory.getAbsolutePath());
        if(openProj == null) return null;
        
        Package pkg = openProj.getPackage(openProj.getInitialPackageName());
        if ( pkg == null ) return null;

        PkgMgrFrame pmf = currentFrame.findFrame(pkg);
        
        if (pmf == null) 
            {
            if (currentFrame.isEmptyFrame()) 
                {
                pmf = currentFrame;
                currentFrame.openPackage(pkg);
                }
            else 
                {
                pmf = currentFrame.createFrame(pkg);
                DialogManager.tileWindow(pmf, currentFrame);
                }
            }

        pmf.show();
        return new BProject (openProj);
    }
        
    /**
     * Creates new project.
     * Give the directory where you want the project be placed. It must be writable, obviously.
     * 
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
     * Gets all the currently open projects.
     * 
     * @return an array of the currently open project objects. It can be an empty array
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
     * Gets the current package being displayed. 
     * It can return null if this information is not available.
     * This is here and NOT into a BProject since it depends on user interface.
     * Depending on what is the currently selected Frame you may get packages that
     * belongs to different projects.
     *
     * @return the current package
     */
    public BPackage getCurrentPackage()
    {
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
     * Use this one when there are NO packages open
     *
     * @return the current Frame
     */
    public Frame getCurrentFrame()
    {
        return PkgMgrFrame.getMostRecent();
    }


    /**
     * Install a new menu generator for this extension.
     * If you want no menues then just set it to null
     * 
     * @param menuGen a Class instance that implements the MenuGen interface
     */
    public void setMenuGen ( MenuGen menuGen )
    {
        currentMenuGen = menuGen;
        menuManager.menuExtensionRevalidateReq();
    }

    /**
     * @return What you have set with the setMenuGen
     */
    public MenuGen getMenuGen ()
    {
        return currentMenuGen;
    }

    /**
     * Install a new preference panel for this extension.
     * If you want to delete it just set prefGen to null
     * 
     * @param prefGen a class instance that implements the PrefGen interface.
     */
    public void setPrefGen(PrefGen prefGen)
    {
        currentPrefGen = prefGen;
        prefManager.panelRevalidate();
    }
    
    /**
     * @return what you have set with setPrefPanel
     */
    public PrefGen getPrefGen()
    {
        return currentPrefGen;
    }


    /**
     * Returns the arguments with which BlueJ was started.
     * 
     * @return a List of strings
     */
    public List getArgs()
    {
        return myWrapper.getArgs();
    }
    
    /**
     * WARNING: Is this really needed by the extensions ?
     * 
     * @return the path to the BlueJ system library directory
     */
    public File getSystemLib()
    {
        return myWrapper.getBlueJLib();
    }

    /**
     * Returns the path to a file contained in the
     * user's bluej settings <CODE>&lt;user&gt;/bluej/<I>file</I></CODE>
     * @param subpath the name of a file or subpath and file
     * @return the path to the user file, which may not exist.
     */
    public File getUserFile (String subpath)
    {
        return Config.getUserConfigFile (subpath);
    }
    
    /**
     * Registers a listener for events generated by Bluej.
     * 
     * @param listener an instance of a class that implements the BluejEventListener interface
     */
    public void addBluejEventListener (BluejEventListener listener)
    {
        myWrapper.addBluejEventListener (listener);
    }


     /**
      * Gets a property from BlueJ properties, but include a default value.
      * 
      * @param property The name of the required global property
      * @param def The default value to use if the property cannot be found.
      * @return the value of that property.
      */
    public String getBJPropString (String property, String def)
    {
        return Config.getPropString ( property, def);
    }

     /**
      * Gets a property from Extensions properties file, but include a default value.
      * You MUST use the setExtPropString to write the propertie that you want stored.
      * You can then come back and retrieve it using this function.
      * 
      * @param property The name of the required global property
      * @param def The default value to use if the property cannot be found
      * @return the value of that property
      */
    public String getExtPropString (String property, String def)
    {
        String thisKey = myWrapper.getSettingsString ( property );
        return Config.getPropString (thisKey, def);
    }
     
     /**
      * Sets a property into Extensions properties file.
      * The property name does NOT needs to be fully qulified since a prefix will be prepended to it.
      * 
      * @param property The name of the required global property
      * @param value the required value of that property.
      */
    public void setExtPropString (String property, String value)
    {
        String thisKey = myWrapper.getSettingsString ( property );
        Config.putPropString (thisKey, value);
    }
    
    /**
     * Gets a language-independent label. 
     * First the BlueJ library is searched, <CODE>&lt;bluej&gt;/lib/&lt;language&gt;/labels</CODE>,
     * then the local, extension's library (if it has one) is searched:
     * <CODE>lib/&lt;language&gt;/labels</CODE>, for example,
     * <CODE>lib/english/labels</CODE> for the English language settings.
     * If no labels are found for the current language, the default language (english) will be tried.
     * 
     * @param id the id of the label to be searched
     * @return the label appropriate to the current language, or, if that fails, the name of the label will be returned.
     */
    public String getLabel (String wantKey)
    {
        // First try from the standard BlueJ properties
        String label = Config.getString (wantKey, null);
        if ( label != null ) return label;

        if ( localLabels == null ) return wantKey;

        return localLabels.getProperty (wantKey, wantKey);
    }
    
    /**
     * Gets a language-independent label, and replaces the first occurrance
     * of a <code>$</code> symbol with the given replacement string.
     * If there is no occurrance of <code>$</code> then it will be added
     * after a space, to the end of the resulting string
     * 
     * @param id the id of the label to be searched in the dictionaries
     * @param replacement the string to replace the <code>$</code>.
     * @return the label, suitably modified.
     */
    public String getLabelInsert (String id, String replacement)
    {
        String label = getLabel (id);
        int p = label.indexOf ('$');
        if (p == -1) {
            label += " $";
            p = label.indexOf ('$');
        }
        label = label.substring (0, p) + replacement + label.substring (p+1);
        return label;
    }

    
    /**
     * Request for BlueJ to close.
     */
    public void closeBlueJ()
    {
        PkgMgrFrame.getMostRecent().wantToQuit();
    }
}