package bluej.extmgr;

import bluej.extensions.BlueJ;
import bluej.extensions.BMenuItem;
import bluej.extensions.Extension;
import bluej.extensions.event.BJEvent;
import bluej.extensions.event.BJEventListener;

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
 * Individual Extension wrapper.
 *
 * @author Clive Miller
 * @version $Id: ExtensionWrapper.java 1476 2002-10-28 09:41:40Z damiano $
 */
public class ExtensionWrapper
{
    public static final DateFormat timeFormat = new SimpleDateFormat ("yyyy/MM/dd HH:mm:ss");
    /**
     * Searches through the given directory for jar files that contain
     * a valid extension.
     * @param directory a directory of extension files
     * @return a Collection of ExtensionWrapper objects of valid extensions.
     */
    public static Collection findValid (File directory, Project project)
    {
        Collection set = new ArrayList();
        File[] files = directory.listFiles();
        if (files != null) {
            for (int i=0, n=files.length; i<n; i++) {
                if (files[i].isDirectory()) continue;
                if (!files[i].getName().endsWith (".jar")) continue;
                ExtensionWrapper ew = createExtensionWrapper (files[i], project);
                if (ew != null) set.add (ew);
            }
        }
        return set;
    }

    /**
     * Creates an ExtensionWrapper object out of the given file.
     * @param jarFile a <CODE>.jar</CODE> file containing one class that
     * extends bluej.extensions.Extension. This file must be referenced by
     * the Main-Class parameter in the jar Manifest file.
     * @return an ExtensionWrapper object, or <CODE>null</CODE> if the file does not contain
     * a valid extension.
     */
    static ExtensionWrapper createExtensionWrapper (File file, Project project)
    {
        ExtensionWrapper newExtWr = null;
        String name = null;
        try {
            name = file.getPath();
            URL url = file.toURL();
            URLClassLoader ucl = new URLClassLoader (new URL[]{url});

            JarFile jarFile = new JarFile (file);
            Manifest mf = jarFile.getManifest();
            if (mf == null) throw new Exception (Config.getString ("extmgr.error.nomanifest"));
            
            String className = mf.getMainAttributes().getValue (Attributes.Name.MAIN_CLASS);
            if (className == null) throw new Exception (Config.getString ("extmgr.error.nomain"));

            Class c = ucl.loadClass (className);
            if (!Extension.class.isAssignableFrom (c)) throw new Exception (Config.getString ("extmgr.error.notsubclass"));
            
            String time = timeFormat.format (new Date (file.lastModified()));
            newExtWr = new ExtensionWrapper (c, file, project, time);
        } catch (Throwable ex) {
//            ex.printStackTrace();
            DialogManager.showText (null, Config.getString ("extmgr.error.cannotload")+": "+name+"\n"+ex);
        }
        return newExtWr;
    }

    private final Class extensionClass;
    private final File extensionPath;
    private final Project project;
    private String status;
    private boolean valid = false;
    private Extension instance = null;
    private final String timestamp;
    private final Collection menus; // of BMenuItem
    private final Collection eventListeners;
    private final BlueJ bj;

    /**
     * Accessor for the bluej that is created
     */
    public BlueJ getBlueJ ()
      {
      return bj;
      }
      
    /**
     * A brand new extension is born.
     */
    private ExtensionWrapper (Class cl, File path, Project project, String timestamp)
    {
        extensionClass = cl;
        extensionPath = path;
        this.project = project;
        status = Config.getString ("extmgr.status.notused");
        menus = new ArrayList(); // of JMenuItem
        eventListeners = new ArrayList(); // of BJEventListener
        this.timestamp = timestamp;

        bj = new BlueJ (this);
        try {
            Constructor cons = extensionClass.getConstructor (new Class[] {BlueJ.class});
            instance = (Extension)cons.newInstance(new Object[] {bj});
            if (!instance.isCompatibleWith (Extension.VERSION_MAJOR, Extension.VERSION_MINOR)) throw new InstantiationException ("Incorrect version");
            status = Config.getString ("extmgr.status.loaded");
            valid = true;
        } catch (Throwable ex) {
            invalidate();
            DialogManager.showText (PkgMgrFrame.getMostRecent(), Config.getString ("extmgr.error.cannotload")+": "+extensionPath.getPath()+"\n"+ex);
            status = Config.getString ("extmgr.status.error");
        }
    }        
    
    /**
     * Adds menu items that the extension is responsible for to the JMenu
     * @param pmf the frame that this menu is going to end up on
     * @param menu the tools menu, for extension's menu items to be added to
     */
    public void addMenuItems (PkgMgrFrame pmf, JMenu menu)
    {
        for (Iterator it = menus.iterator(); it.hasNext();) {
            BMenuItem item = (BMenuItem)it.next();
            JMenuItem mi = item.getJMenuItem (pmf);
            menu.add (mi);
        }
    }
    
    /**
     * Registers a menu item to be added to the tools menu on request.
     * @param item the menu item to register
     */
    public void registerMenuItem (BMenuItem item)
    {
        PkgMgrFrame []frames = PkgMgrFrame.getAllFrames();
        for ( int index=0; index < frames.length; index ++ )
          {
              JMenu aMenu = frames[index].getToolsMenu();
              if ( aMenu != null ) aMenu.add(item.getJMenuItem(frames[index]));
          }
        
        menus.add (item);
    }
    
    String[] getMenuNames()
    {
        String[] names;
        synchronized (menus) {
            names = new String [menus.size()];
            int i=0;
            for (Iterator it = menus.iterator(); it.hasNext(); i++) {
                names[i] = ((BMenuItem)it.next()).getText();
            }
        }
        return names;
    }   
    
    /**
     * Gets the project this extension is associated with (if any)
     * @return the project owning this extension, or <code>null</code> if the extension
     * is system-wide (more likely)
     */
    public Project getProject()
    {
        return project;
    }
    
    /**
     * Is this extension valid?
     * @return <CODE>true</CODE> if this extension has been successfully loaded, has no errors
     * and has not yet been invalidated.
     */
    public boolean isValid()
    {
        return valid;
    }
    
    /**
     * Kills off this extension as much as possible, including removing menu items and making
     * access to BlueJ no longer possible.
     */
    public void invalidate()
    {
        for (Iterator it = menus.iterator(); it.hasNext();) {
            BMenuItem item = (BMenuItem)it.next();
            item.removeMenuItems();
        }
        menus.clear();
        status = Config.getString ("extmgr.status.unloaded");
        valid = false;
        instance = null;
        ExtPrefPanel.INSTANCE.panelRevalidate();;
    }
    
    /**
     * Gets the current status of this extension.
     * @return something like 'Loaded' or 'Error'.
     */
    public String getStatus()
    {
        return status;
    }
    
    /**
     * Gets the fully-qualified name of this extension. 
     * @return the fully-qualified class name of the extension (the class that extends 
     * <CODE>bluej.extensions.Extension</CODE>).
     */
    public String getName()
    {
        return extensionClass.getName();
    }
    
    /**
     * Gets a String representation of the path to the <CODE>.jar</CODE> file containing
     * the extension.
     * @return a String like <CODE>C:/bluej/lib/extensions/fun.jar</CODE>
     */
    public String getLocation()
    {
        return extensionPath.getPath();
    }
    
    /**
     * Gets the Class for this extension
     * @return the Class for this extension
     */
    public Class getExtensionClass()
    {
        return extensionClass;
    }
    
    /**
     * Gets the extension's description of itself.
     * @return the extension's description, perhaps even <CODE>null</CODE>.
     */
    public String getDescription()
    {
        return instance==null ? null : (instance.getDescription());
    }
    
    /**
     * Gets the extension's 'further information' URL
     * @return the extension's URL, or <CODE>null</CODE>.
     */
    public URL getURL()
    {
        return instance==null ? null : (instance.getURL());
    }
    
    /**
     * Gets the timestamp of the jar file.
     * @return yyyy/mm/dd hh:mm:ss
     */
    public String getDate()
    {
        return timestamp;
    }
    
    /**
     * Gets the formal version number of this extension.
     * @return a String containing the major version, followed by a dot, followed by the minor version number.
     */
    public String getVersion()
    {
        if (instance == null) return Config.getString ("extmgr.version.unknown");
        try {
            return instance.getVersionMajor() +"."+ instance.getVersionMinor();
        } catch (Throwable ex) {
            return Config.getString ("extmgr.status.error");
        } 
    }
    
    /**
     * Registers a package listener for this extension.
     */
    public void addBJEventListener (BJEventListener el)
    {
        if (el != null) {
            eventListeners.add (el);
        }
    }
    
    /**
     * Informs any registered listeners that an event has occurred.
     */
    void eventOccurred (BJEvent event)
    {
        if (!isValid()) return;
        if (eventListeners.isEmpty()) return;
        for (Iterator it = eventListeners.iterator(); it.hasNext();) {
            BJEventListener el = (BJEventListener) it.next();
            el.eventOccurred (event);
        }
    }

}