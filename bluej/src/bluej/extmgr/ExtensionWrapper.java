package bluej.extmgr;

import bluej.extensions.*;
import bluej.extensions.event.*;

import bluej.Config;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.DialogManager;
import bluej.utility.Debug;

import java.util.*;
import java.util.jar.*;
import java.io.*;

import java.net.URL;
import java.net.URLClassLoader;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.lang.reflect.Constructor;

/**
 * <PRE>
 *  This is the wrapper for an extension. Its duties are 
 *  - Keep track of an extension class, this is to allow loading and unloading 
 *  - Given a jar try to load an extension that is in it (if any) 
 *  - Hold all state that is needed to get the whole system working 
 *  
 *  NOTE: - When an extension is loaded a BlueJ object is given to it This object MUST
 *  be fully usable by the extension AND all associate components ! This means
 *  the following 
 *  
 *  - The creation of an extension Wrapper is disjoint from the "creation" of an extension 
 *  In ANY case we crete a wrapper for the given filename 
 *  We then load the extension ONLY if somebody else requets it...
 *  </PRE>
 */
public class ExtensionWrapper
{
    private ExtensionsManager extensionsManager;
    private PrefManager prefManager;

    private File extensionJarFileName;
    private long extensionLastModified;

    // If != null the jar is good. DO NOT expose this unless REALLY needed
    private Class extensionClass;

    // If != null the extension is loaded. do NOT expose this unless REALLY needed
    private Extension extensionInstance;

    private String extensionStatusString;
    private BlueJ extensionBluej;
    private MenuManager menuManager;
    private Project project;
    private Collection eventListeners;

    /**
     *  We try to load the given jar, there is nothing wrong if it is NOT a good
     *  one Simply the extension will be marked as invalid and nobody will be
     *  able to use SInce it is not static if nobody is usin it will be garbage
     *  collected...
     *
     * @param  extensionsManager  Description of the Parameter
     * @param  prefManager        Description of the Parameter
     * @param  jarFile            Description of the Parameter
     */
    public ExtensionWrapper(ExtensionsManager extensionsManager, PrefManager prefManager, File jarFile)
    {
        this.extensionsManager = extensionsManager;
        this.prefManager = prefManager;

        // Let me try to load the extension class
        if ((extensionClass = getExtensionClass(jarFile)) == null)  return;

        extensionJarFileName  = jarFile;
        extensionLastModified = jarFile.lastModified();
    }


    /**
     *  This is in charge of returning a valid extension class, if any in the
     *  given jar file, it will return the Class or null if none is found
     *  NOTE: I am showing some messages in case of failure since othervise a user may never
     *  understand WHY his lovely extension is not loaded.
     *
     * @param  jarFileName  I want a jar file name to load
     * @return              The extension class, NOT an instance of it !
     */
    private Class getExtensionClass(File jarFileName)
    {
        Class classRisul = null;
        extensionStatusString = Config.getString("extmgr.status.loading");

        // It may happen, no reaso to core dump for this...
        if (jarFileName == null) return null;

        // Also this may happen, again, no reason to continue further
        if (!jarFileName.getName().endsWith(".jar")) return null;

        // Needed so on error I know which file is trowing it
        String errorPrefix = "getExtensionsClass: jarFile="+jarFileName.getName()+" ";

        try {
            JarFile jarFile = new JarFile(jarFileName);
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                Debug.message(errorPrefix+Config.getString("extmgr.error.nomanifest"));
                return null;
            }

            String className = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
            if (className == null) {
                Debug.message(errorPrefix+Config.getString("extmgr.error.nomain"));
                return null;
            }

            URL url = jarFileName.toURL();
            URLClassLoader ucl = new URLClassLoader(new URL[]{url});

            classRisul = ucl.loadClass(className);
            if (!Extension.class.isAssignableFrom(classRisul)) {
                Debug.message(errorPrefix+Config.getString("extmgr.error.notsubclass"));
                return null;
            }
        } catch (Throwable exc) {
            Debug.message(errorPrefix+"Exception="+exc.getMessage());
            return null;
        }

        return classRisul;
    }


    /**
     *  Now, assume you have the class and you want to "istantiate" the
     *  extension You have to call this. NOTE that the extension wrapper is
     *  ALREADY UP and running. I do not return a value, you may check
     *  how this went by using the isValid() method...
     *
     * @param  project  The project this extensionis linked to, null if none
     */
    public void newExtension(Project project)
    {
        // It may happen
        if (extensionClass == null)  return;

        this.project = project;

        menuManager    = new MenuManager(this);
        eventListeners = new ArrayList();
        extensionBluej = new BlueJ(this, prefManager);

        extensionStatusString = Config.getString("extmgr.status.notused");

        try {
            Constructor cons = extensionClass.getConstructor(new Class[]{});
            extensionInstance = (Extension) cons.newInstance(new Object[]{});
        } catch (Throwable ex) {
            extensionInstance = null;
            extensionStatusString = "newExtension: Exception=" + ex.getMessage();
            return;
        }

        // Let me see if this extension is somewhat compatible...
        if (!extensionInstance.isCompatibleWith(Extension.VERSION_MAJOR, Extension.VERSION_MINOR)) {
            extensionStatusString = Config.getString("extmgr.status.badversion");
            extensionInstance = null;
        }

        // Ok, time to really start everything... This MUST be here.... after all is initialzed
        extensionInstance.startup(extensionBluej);
        extensionStatusString = Config.getString("extmgr.status.loaded");
    }




    /**
     *  Gets the project this extension is associated with (if any)
     *
     * @return    the project owning this extension, or <code>null</code> if the
     *      extension is system-wide (more likely)
     */
    public Project getProject()
    {
        return project;
    }


    /**
     *  Accessor for the bluej that is created
     *
     * @return    The blueJ value
     */
    public BlueJ getBluej()
    {
        return extensionBluej;
    }


    /**
     *  Accessor for the MenuManager
     *
     * @return    The menuManager value
     */
    public MenuManager getMenuManager()
    {
        return menuManager;
    }


    /**
     *  Checks if a this extension is valid
     *
     * @return true if it is istantiated, false if it is not.
     */
    public boolean isValid()
    {
        return (extensionInstance != null);
    }


    /**
     *  Gets the jarValid attribute of the ExtensionWrapper object
     *
     * @return    The jarValid value
     */
    public boolean isJarValid()
    {
        return (extensionClass != null);
    }


    /**
     *  Kills off this extension as much as possible, including removing menu
     *  items and making access to BlueJ no longer possible.
     *  Not only ! we are even going to release the wrapper after this.
     *  So it can be loaded again, hopefully from a clean environment
     */
    public void terminate()
    {
        Debug.message("Extension.terminate(): class="+getExtensionClassName());

        if ( extensionInstance != null ) {
            // Give a chance to extension to clear up after itself.
            String terminateRisul = extensionInstance.terminate();
            Debug.message("Extension.terminate()="+terminateRisul);
        }

        // Needed to signal to the revalidate that this instance is no longer here.            
        extensionInstance = null;

        // Time to clean up things from the visul point of view.
        prefManager.panelRevalidate();
        menuManager.menuExtensionRevalidateReq();

        // Ok, I am ready to get erased from the world.
    }


    /**
     *  Gets the current status of this extension.
     *
     * @return    something like 'Loaded' or 'Error'.
     */
    public String getExtensionStatus()
    {
        return extensionStatusString;
    }


    /**
     *  Gets the fully-qualified name of this extension class.
     *
     * @return This extension class name or null if nothing is loaded
     */
    public String getExtensionClassName()
    {
        if (extensionClass == null) return null;
        return extensionClass.getName();
    }


    /**
     * Tryes to return a reasonable Properties instance of the extension labels
     * It MAY return null if nothing reasonable can be found in the EXTENSION jar
     * 
     * @return the properties or null if nothing can be found
     */
    public Properties getLabelProperties ()
    {
        String localLanguage = Config.getPropString("bluej.language", Config.DEFAULT_LANGUAGE);

        // Let me try to get the properties using the local language
        Properties risulProp = getLabelProperties (localLanguage);
        if ( risulProp != null ) return risulProp;

        // Nothing found, let me try to get them using the default one...
        risulProp = getLabelProperties (Config.DEFAULT_LANGUAGE);
        if ( risulProp != null ) return risulProp;

        // Hmmm, this is debatable, should I return null or an empty instance ?
        return null;
    }

    /**
     * Returns the label that are language dependents as a Properies instance
     * 
     * @return the equivalent properties if found, null if nothing
     */
    private Properties getLabelProperties (String language)
    {
        if ( extensionClass == null ) {
            // This is really not normal, better say that it is not normal.
            Debug.message("ExtensionWrapper.getLabelProperties(): ERROR: extensionClass==null");
            return null;
        }

        String languageFileName = "lib/" + language + "/labels";
        
        InputStream inStream = extensionClass.getClassLoader().getResourceAsStream (languageFileName);
        if ( inStream == null ) return null;

        Properties risul = new Properties();

        try {
            risul.load(inStream);
        } catch(Exception ex) {
            // Really it should never happen, if it does there is really something weird going on
            Debug.message("ExtensionWrapper.getLabelProperties(): Exception="+ex.getMessage());
        } 
        closeInputStream ( inStream );
        return risul;
    }


    /**
     * UFF, this is here but it really ougth to be in a public util 
     * SImply close a stream without complaining too much.
     * Just to avoid the Nth level of try catch with no value added
     */
    public static void closeInputStream ( InputStream aStream )
    {
        try {
            aStream.close();
        } catch ( Exception ee ) {
        // Do nothing, really
        }
    }

    /**
     *  Gets a String representation of the path to the <CODE>.jar</CODE> file
     *  containing the extension.
     *
     * @return    a String like <CODE>C:/bluej/lib/extensions/fun.jar</CODE>
     */
    public String getExtensionFileName()
    {
        if (extensionJarFileName == null) return null;
        return extensionJarFileName.getPath();
    }


    /**
     *  Gets the extension's description.
     *
     * @return    the extension's description, or null
     */
    public String getExtensionDescription()
    {
        if (extensionInstance == null) return null;

        return extensionInstance.getDescription();
    }


    /**
     *  Gets the extension's 'further information' URL
     *
     * @return    the extension's URL, or <CODE>null</CODE>.
     */
    public URL getURL()
    {
        if (extensionInstance == null) return null;

        return extensionInstance.getURL();
    }


    /**
     *  Gets the timestamp of the jar file.
     *  NOTE: Need to return the date in locale format...
     *  
     * @return    yyyy/mm/dd hh:mm:ss
     */
    public String getExtensionModifiedDate()
    {
        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return timeFormat.format(new Date(extensionLastModified));
    }


    /**
     *  Gets the formal version number of this extension.
     *
     * @return    a String containing the major version, followed by a dot,
     *      followed by the minor version number. or null
     */
    public String getExtensionVersion()
    {
        if (extensionInstance == null)
            return Config.getString("extmgr.version.unknown");

        return extensionInstance.getVersionMajor() + "." + extensionInstance.getVersionMinor();
    }


    /**
     *  Convenience method to ensure uniformity of preference items
     *
     * @param  ew   the wrapper of the extension to which to apply the key
     * @param  key  Description of the Parameter
     * @return      an appropriate string to identify the preference item
     */
    public String getPreferencesString( String key)
    {
        return "extensions." + getExtensionClassName() + ".preferences." + key;
    }


    /**
     *  Convenience method to ensure uniformity of settings items
     *
     * @param  ew   the wrapper of the extension to which to apply the key
     * @param  key  Description of the Parameter
     * @return      an appropriate string to identify the setting
     */
    public String getSettingsString( String key)
    {
        return "extensions." + getExtensionClassName() + ".settings." + key;
    }

    /**
     * Returns a reminder as to where to find <CODE>&lt;bluej&gt;/lib</CODE>.
     * This is here to expose a method from the ExtensionsManager.
     * 
     * @return    a <CODE>File</CODE> which is an existing directory.
     */
    public File getBlueJLib()
    {
        return extensionsManager.getBlueJLib();
    }


    /**
     * Returns an unmodifiable list of the arguments passed to BlueJ.
     * This is here to expose a method from the ExtensionsManager.
     *
     * @return    a List containing each space-delimited parameter.
     */
    public List getArgs()
    {
        return extensionsManager.getArgs();
    }

    /**
     *  Registers a package listener for this extension.
     *
     * @param  el  The feature to be added to the BJEventListener attribute
     */
    public void addBJEventListener(BJEventListener el)
    {
        if (el != null)
            eventListeners.add(el);
    }


    /**
     *  Informs any registered listeners that an event has occurred.
     *
     * @param  event  Description of the Parameter
     */
    void eventOccurred(BJEvent event)
    {
        if (!isValid()) return;

        if (eventListeners.isEmpty()) return;

        for (Iterator it = eventListeners.iterator(); it.hasNext(); ) {
            BJEventListener el = (BJEventListener) it.next();
            el.eventOccurred(event);
        }
    }
}
