package bluej.extmgr;

import bluej.extensions.*;
import bluej.extensions.event.*;

import bluej.Config;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.DialogManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.lang.reflect.Constructor;

/**
 *  This is the wrapper for an extension. Its duties are - Keep track of an
 *  extension class, this is to allow loading and unloading - Given a jar try to
 *  load an extension that is in it (if any) - Hold all state that is needed to
 *  get the whole system working There is a timing problem and is the following
 *  - When an extension is loaded a BlueJ object is given to it This object MUST
 *  be fully usable by the extension AND all associate components ! This means
 *  the following - The creation of an extension Wrapper is disjoint from the
 *  "creation" of an extension In ANY case we crete a wrapper for the given
 *  filename We then load the extension ONLY if somebody else requets it...
 */
public class ExtensionWrapper
{
    private ExtensionsManager extensionsManager;
    private PrefManager prefManager;

    // If != null the jar is good
    private Class extensionClass;
    // If != null the extension is loaded
    private Extension extensionInstance;
    private String extensionStatusString;
    private File extensionJarFileName;
    private long extensionLastModified;
    private BlueJ extensionBluej;
    private MenuManager menuManager;


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
        if ((extensionClass = getExtensionClass(jarFile)) == null)
            return;

        extensionJarFileName = jarFile;
        extensionLastModified = jarFile.lastModified();
    }


    /**
     *  This is in charge of returning a valid extension class, if any in the
     *  given jar file, it will return the Class or null if none is found
     *
     * @param  jarFileName  Description of the Parameter
     * @return              The extensionClass value
     */
    private Class getExtensionClass(File jarFileName)
    {
        Class classRisul = null;

        // It may happen, no reaso to core dump for this...
        if (jarFileName == null)
            return null;

        // Also this may happen, again, no reason to continue further
        if (!jarFileName.getName().endsWith(".jar"))
            return null;

        try {
            JarFile jarFile = new JarFile(jarFileName);
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                extensionStatusString = Config.getString("extmgr.error.nomanifest");
                return null;
            }

            String className = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
            if (className == null) {
                extensionStatusString = Config.getString("extmgr.error.nomain");
                return null;
            }

            URL url = jarFileName.toURL();
            URLClassLoader ucl = new URLClassLoader(new URL[]{url});

            classRisul = ucl.loadClass(className);
            if (!Extension.class.isAssignableFrom(classRisul)) {
                extensionStatusString = Config.getString("extmgr.error.notsubclass");
                return null;
            }
        } catch (Throwable exc) {
            extensionStatusString = "loadExtensionClass: Exception=" + exc.getMessage();
            return null;
        }

        return classRisul;
    }


    /**
     *  Now, assume you have the class and you want to "istantiate" the
     *  extension You have to call this. NOTE that the extension wrapper is
     *  ALREADY UP and running
     *
     * @param  project  Description of the Parameter
     * @return          Description of the Return Value
     */
    public Extension newExtension(Project project)
    {
        this.project = project;
        extensionStatusString = Config.getString("extmgr.status.notused");
        eventListeners = new ArrayList();
        // of BJEventListener

        extensionBluej = new BlueJ(this, prefManager);
        menuManager = new MenuManager(this);

        // It may happen, unfortunately...
        if (extensionClass == null)
            return null;

        try {
            Constructor cons = extensionClass.getConstructor(new Class[]{});
            extensionInstance = (Extension) cons.newInstance(new Object[]{});
        } catch (Throwable ex) {
            extensionInstance = null;
            extensionStatusString = "newExtension: Exception=" + ex.getMessage();
            return null;
        }

        // Let me see if this extension is somewhat compatible...
        if (!extensionInstance.isCompatibleWith(Extension.VERSION_MAJOR, Extension.VERSION_MINOR)) {
            extensionStatusString = "Incorrect version";
            extensionInstance = null;
        }

        // Ok, time to really start everything... This MUST be here.... after all is initialzed
        extensionInstance.startup(extensionBluej);

        extensionStatusString = Config.getString("extmgr.status.loaded");
        return extensionInstance;
    }


    private Project project;
    private Collection eventListeners;


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
    public BlueJ getBlueJ()
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
     *  Is this extension valid?
     *
     * @return    <CODE>true</CODE> if this extension has been successfully
     *      loaded, has no errors and has not yet been invalidated.
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
     */
    public void invalidate()
    {
        extensionStatusString = Config.getString("extmgr.status.unloaded");
        extensionInstance = null;

        prefManager.panelRevalidate();
        menuManager.menuExtensionRevalidateReq();
    }


    /**
     *  Gets the current status of this extension.
     *
     * @return    something like 'Loaded' or 'Error'.
     */
    public String getStatus()
    {
        return extensionStatusString;
    }


    /**
     *  Gets the fully-qualified name of this extension.
     *
     * @return    the fully-qualified class name of the extension (the class
     *      that extends <CODE>bluej.extensions.Extension</CODE>).
     */
    public String getName()
    {
        if (extensionClass == null)
            return "null";

        return extensionClass.getName();
    }


    /**
     *  Gets a String representation of the path to the <CODE>.jar</CODE> file
     *  containing the extension.
     *
     * @return    a String like <CODE>C:/bluej/lib/extensions/fun.jar</CODE>
     */
    public String getLocation()
    {
        if (extensionJarFileName == null)
            return "";

        return extensionJarFileName.getPath();
    }


    /**
     *  Gets the Class for this extension
     *
     * @return    the Class for this extension
     */
    public Class getExtensionClass()
    {
        return extensionClass;
    }


    /**
     *  Gets the extension's description of itself.
     *
     * @return    the extension's description, perhaps even <CODE>null</CODE>.
     */
    public String getDescription()
    {
        if (extensionInstance == null)
            return null;

        return extensionInstance.getDescription();
    }


    /**
     *  Gets the extension's 'further information' URL
     *
     * @return    the extension's URL, or <CODE>null</CODE>.
     */
    public URL getURL()
    {
        if (extensionInstance == null)
            return null;

        return extensionInstance.getURL();
    }


    /**
     *  Gets the timestamp of the jar file.
     *
     * @return    yyyy/mm/dd hh:mm:ss
     */
    public String getDate()
    {
        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return timeFormat.format(new Date(extensionLastModified));
    }


    /**
     *  Gets the formal version number of this extension.
     *
     * @return    a String containing the major version, followed by a dot,
     *      followed by the minor version number.
     */
    public String getVersion()
    {
        if (extensionInstance == null)
            return Config.getString("extmgr.version.unknown");

        return extensionInstance.getVersionMajor() + "." + extensionInstance.getVersionMinor();
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
        if (!isValid())
            return;

        if (eventListeners.isEmpty())
            return;

        for (Iterator it = eventListeners.iterator(); it.hasNext(); ) {
            BJEventListener el = (BJEventListener) it.next();
            el.eventOccurred(event);
        }
    }
}
