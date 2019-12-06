/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2012,2013,2014,2016,2018,2019  Michael Kolling and John Rosenberg
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.extmgr;

import bluej.Config;
import bluej.extensions2.BlueJ;
import bluej.extensions2.Extension;
import bluej.extensions2.ExtensionBridge;
import bluej.extensions2.PreferenceGenerator;
import bluej.extensions2.event.ExtensionEvent;
import bluej.pkgmgr.Project;
import bluej.utility.Debug;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * This is the wrapper for an extension. Its duties are: 
 * <ul>
 * <li>Keep track of an extension class, this is to allow loading and unloading 
 * <li>Given a jar try to load an extension that is in it (if any) 
 * <li>Hold all state that is needed to get the whole system working
 * </ul> 
 *  
 * <p>Note: When an extension is loaded a BlueJ object is given to it. This object MUST
 * be fully usable by the extension AND all associated components !
 * 
 * <p>The creation of an extension Wrapper is disjoint from the loading of an extension.
 *  
 * @author Damiano Bolla, 2002,2003,2004
 */
public class ExtensionWrapper
{
    private final ExtensionPrefManager prefManager;

    private File extensionJarFileName;

    // If != null the jar is good.
    private Class<?> extensionClass;

    // If != null the extension is loaded
    private Extension extensionInstance;

    private BlueJ   extensionBluej;
    private String  extensionStatusString;
    private Project project;

    /**
     * Construct a new ExtensionWrapper for the given jar file.
     * 
     * <p>This may fail; in that case isJarValid() will return false.
     * 
     * <p>The extension is not actually loaded: call newExtension() for that.
     */
    public ExtensionWrapper(ExtensionPrefManager prefManager, File jarFile)
    {
        this.prefManager = prefManager;

        // Let me try to load the extension class
        extensionClass = getExtensionClass(jarFile);
        if (extensionClass == null) {
            return;
        }

        extensionJarFileName  = jarFile;
    }


    /**
     * Get the extension class for an extension .jar file. Return the Class or
     * null if none is found.
     * 
     * <p>Some messages are logged in case of failure since otherwise a user may never
     * understand why his lovely extension was not loaded.
     *
     * @param  jarFileName  The name of the (potential) extension jar file name to load
     * @return              The extension class.
     */
    private Class<?> getExtensionClass(File jarFileName)
    {
        Class<?> extensionClass = null;
        extensionStatusString = Config.getString("extmgr.status.loading");

        // It may happen, no reason to core dump for this...
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

            URL url = jarFileName.toURI().toURL();
            URL[] urlList=new URL[]{url};
            FirewallLoader fireLoader = new FirewallLoader(getClass().getClassLoader());
            URLClassLoader ucl = new URLClassLoader(urlList,fireLoader);

            extensionClass = ucl.loadClass(className);
            if (!Extension.class.isAssignableFrom(extensionClass)) {
                Debug.message(errorPrefix+Config.getString("extmgr.error.notsubclass"));
                return null;
            }
        } catch (Throwable exc) {
            Debug.message(errorPrefix+"Exception="+exc.getMessage());
            exc.printStackTrace();
            return null;
        }

        return extensionClass;
    }

    /**
     * A ClassLoader which only finds bluej.* classes and system classes. This is used to
     * prevent extensions from seeing other libraries which might be bundled with and used by
     * BlueJ, so that they can use their own versions of those libraries if they wish.
     */
    @OnThread(Tag.Any)
    static class FirewallLoader extends ClassLoader
    {
        ClassLoader myParent;
      
        /**
         * Constructor. Note that this classloader breaks from the delegation model; the parent
         * is not the actual parent classloader, it is only used by findClass() below. 
         */
        FirewallLoader ( ClassLoader parent )
        {
            myParent = parent;
        }

        public Class<?> findClass(String name) throws ClassNotFoundException
        {
            if ( name.startsWith("bluej.") || name.startsWith("rmiextension.") || name.startsWith("greenfoot.")) {
                return myParent.loadClass(name);
            }

            throw new ClassNotFoundException();
        }
    }
  
    /**
     * Now, assume you have the class and you want to "instantiate" the
     * extension You have to call this. NOTE that the extension wrapper is
     * ALREADY UP and running. I do not return a value, you may check
     * how this went by using the isValid() method...
     *
     * @param  aProject  The project this extension is linked to, null if none
     */
    void newExtension(Project aProject)
    {
        // It may happen
        if (extensionClass == null)  return;

        project = aProject;
        extensionBluej = ExtensionBridge.newBluej(this, prefManager);

        try {
            extensionInstance = (Extension)extensionClass.getDeclaredConstructor().newInstance();
        } catch (Throwable ex) {
            extensionInstance = null;
            extensionStatusString = "newExtension: Exception=" + ex.getMessage();
            return;
        }

        // Let me see if this extension is somewhat compatible...
        if ( ! safeIsCompatible() ) {
            extensionStatusString = Config.getString("extmgr.status.badversion");
            extensionInstance = null;
            return;
        }

        // Ok, time to really start everything... This MUST be here.... after all is initialised
        safeStartup(extensionBluej);
        extensionStatusString = Config.getString("extmgr.status.loaded");
    }


    /**
     * Gets the project this extension is associated with.
     * This happens in case of extensions loaded with a Project.
     * If it is a system wide extension this will be null.
     *
     * @return    the project owning this extension.
     */
    Project getProject()
    {
        return project;
    }


    /**
     *  Checks if a this extension is valid
     *
     * @return true if it is instantiated, false if it is not.
     */
    public boolean isValid()
    {
        return (extensionInstance != null);
    }


    /**
     * Gets the jarValid attribute of the ExtensionWrapper object
     *
     * @return    The jarValid value
     */
    boolean isJarValid()
    {
        return (extensionClass != null);
    }


    /**
     * Kills off this extension as much as possible
     * items and making access to BlueJ no longer possible.
     * Not only ! we are even going to release the wrapper after this.
     * So it can be loaded again, hopefully from a clean environment
     */
    void terminate()
    {
        safeTerminate();

        // Needed to signal to the revalidate (below) that this instance is no longer here.            
        extensionInstance = null;

        // Time to clean up things from the visual point of view.
        prefManager.panelRevalidate();

        // Ok, I am ready to get erased from the world.
    }


    /**
     * Gets the current status of this extension.
     *
     * @return    something like 'Loaded' or 'Error'.
     */
    public String getExtensionStatus()
    {
        return extensionStatusString;
    }


    /**
     * Gets the fully-qualified name of this extension class.
     *
     * @return This extension class name or null if nothing is loaded
     */
    public String getExtensionClassName()
    {
        if (extensionClass == null) {
            return null;
        }

        return extensionClass.getName();
    }


    /**
     * Tries to return a reasonable Properties instance of the extension labels
     * It may return null if nothing reasonable can be found in the extension jar
     * 
     * @return the properties or null if nothing can be found
     */
    public Properties getLabelProperties ()
    {
        String localLanguage = Config.getPropString("bluej.language", Config.DEFAULT_LANGUAGE);

        // Let me try to get the properties using the local language
        Properties extensionsProps = getLabelProperties (localLanguage);
        if ( extensionsProps != null ) return extensionsProps;

        // Nothing found, let me try to get them using the default one...
        extensionsProps = getLabelProperties (Config.DEFAULT_LANGUAGE);

        return extensionsProps;
    }

    
    /**
     * Returns the label that are language dependents as a Properties instance
     * 
     * @return the equivalent properties if found, null if nothing
     */
    private Properties getLabelProperties (String language)
    {
        if ( extensionClass == null ) {
            return null;
        }

        String languageFileName = "lib/" + language + "/labels";
        
        InputStream inStream = extensionClass.getClassLoader().getResourceAsStream (languageFileName);
        if ( inStream == null ) return null;

        Properties props = new Properties();

        try {
            props.load(inStream);
        } catch(Exception ex) {
            // Really it should never happen, if it does there is really something weird going on
            Debug.message("ExtensionWrapper.getLabelProperties(): Exception="+ex.getMessage());
        } 
        closeInputStream ( inStream );
        return props;
    }


    /**
     * UFF, this is here but it really ought to be in a public util.
     * Simply close a stream without complaining too much.
     * Just to avoid the Nth level of try catch with no value added
     */
    public static void closeInputStream(InputStream aStream)
    {
        try {
            aStream.close();
        } catch ( Exception ee ) {
            // Do nothing, really
        }
    }

    
    /**
     * Gets a String representation of the path to the <CODE>.jar</CODE> file
     * containing the extension.
     *
     * @return    String like <CODE>C:/bluej/lib/extensions/fun.jar</CODE> or null 
     */
    public String getExtensionFileName()
    {
        if (extensionJarFileName == null) return null;
        return extensionJarFileName.getPath();
    }


    /**
     *  Convenience method to ensure uniformity of settings items.
     */
    public String getSettingsString( String key)
    {
        return "extensions." + getExtensionClassName() + ".settings." + key;
    }


    /**
     * Returns useful information about this wrapper
     */
    public String toString()
    {
        if (! isValid()) {
            return "ExtensionWrapper: invalid";
        }

        return "ExtensionWrapper: "+ extensionClass.getName();
    }

    /* 
     * ====================== ERROR WRAPPED CALLS HERE =========================
     * We need to wrap all calls from BlueJ to the Extension into a try/catch;
     * otherwise an error in the extension will render BlueJ unusable.
     */

    /**
     * Informs any registered listeners that an event has occurred.
     */
    public void safeEventOccurred(ExtensionEvent event)
    {
        if (!isValid()) {
            return;
        }

        try {
            ExtensionBridge.delegateEvent(extensionBluej,event);
        }
        catch (Throwable exc)  {
            Debug.message("ExtensionWrapper.safeEventOccurred: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
            return;
        }
    }

    
    /**
     * Returns the extension's description.
     */
    public String safeGetExtensionDescription()
    {
        if (extensionInstance == null) {
            return null;
        }

        try {
            return extensionInstance.getDescription();
        }
        catch (Throwable exc)  {
            Debug.message("ExtensionWrapper.safeGetExtensionDescription: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
            return null;
        }
    }

    
    /**
     * Returns the extension's name.
     */
    public String safeGetExtensionName()
    {
        if (extensionInstance == null) 
            return "";

        try {
            return extensionInstance.getName();
        }
        catch (Throwable exc) {
            Debug.message("ExtensionWrapper.safeGetExtensionName: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
            return "";
        }
    }

    
    /**
     * Gets the extension's 'further information' URL
     *
     * @return    the extension's URL, or <CODE>null</CODE>.
     */
    public URL safeGetURL()
    {
        if (extensionInstance == null) 
            return null;

        try {
            return extensionInstance.getURL();
        }
        catch (Throwable exc) {
            Debug.message("ExtensionWrapper.safeGetURL: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
            return null;
        }
    }


    /**
     * Gets the formal version of this extension.
     *
     * @return  the version of the extension
     */
    public String safeGetExtensionVersion()
    {
        if (extensionInstance == null) 
            return null;

        try {
          return extensionInstance.getVersion();
        }
        catch (Throwable exc) {
            Debug.message("ExtensionWrapper.safeGetExtensionVersion: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
            return null;
        }
    }


    /**
     * Ask the extension if it thinks it is compatible.
     *
     * @return  true if it is, false otherwise
     */
    private boolean safeIsCompatible()
    {
        if (extensionInstance == null) 
            return false;

        try {
            return extensionInstance.isCompatible();
        }
        catch (Throwable exc) {
            Debug.message("ExtensionWrapper.safeIsCompatible: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
            // If one bombs at me it shurely is not compatilbe 
            return false;
        }
    }

    
    /**
     * Call the startup method in a safe way
     */
    private void safeStartup(BlueJ bluejProxy)
    {
        if (extensionInstance == null) {
            return;
        }

        try {
            extensionInstance.startup(bluejProxy);
        }
        catch (Throwable exc)  {
            Debug.message("ExtensionWrapper.safeStartup: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
        }
    }


    /**
     * Call the terminate method in a safe way
     */
    private void safeTerminate()
    {
        if (extensionInstance == null) {
            return;
        }

        try {
            // Give a chance to extension to clear up after itself.
            extensionInstance.terminate();
        }
        catch (Throwable exc) {
            Debug.message("ExtensionWrapper.safeTerminate: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
        }
    }


    /**
     * Calls the EXTENSION preference panel loadValues in a safe way
     */
    public void safePrefGenLoadValues()
    {
        if (extensionBluej == null) { 
            return;
        }

        PreferenceGenerator aPrefGen = extensionBluej.getPreferenceGenerator();
        // The above is safe. An extension may not have a preference panel
        if (aPrefGen == null)  {
            return;
        }

        try {
            aPrefGen.loadValues();
        }
        catch (Throwable exc) {
            Debug.message("ExtensionWrapper.safePrefGenLoadValues: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
        }
    }

    
    /**
     * Calls the EXTENSION preference panel saveValues in a safe way
     */
    public void safePrefGenSaveValues()
    {
        if (extensionBluej == null) 
            return;

        PreferenceGenerator aPrefGen = extensionBluej.getPreferenceGenerator();
        // The above is dafe. An extension may not have a preference panel
        if (aPrefGen == null) 
            return;

        try {
            aPrefGen.saveValues();
        }
        catch (Throwable exc) {
            Debug.message("ExtensionWrapper.safePrefGenSaveValues: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
        }
    }

    
    /**
     *  Calls the EXTENSION preference Pane getWindow in a safe way
     */
    public Pane safePrefGenGetWindow()
    {
        if (extensionBluej == null) 
            return null;

        PreferenceGenerator aPrefGen = extensionBluej.getPreferenceGenerator();
        // The above is dafe. An extension may not have a preference panel
        if (aPrefGen == null) 
            return null;

        try {
            return aPrefGen.getWindow();
        }
        catch (Throwable exc) {
            Debug.message("ExtensionWrapper.safePrefGenGetWindow: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
            return null;
        }
    }


    /**
     *  Calls the EXTENSION getMenuItem in a safe way
     */
    public MenuItem safeGetMenuItem(ExtensionMenu attachedObject)
    {
        if (extensionBluej == null) 
            return null;

        try {
            return ExtensionBridge.getMenuItem(extensionBluej, attachedObject);
        }
        catch (Throwable exc) {
            Debug.message("ExtensionWrapper.safeMenuGenGetMenuItem: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
            return null;
        }
    }

    /**
     *  Calls the EXTENSION postMenuItem in a safe way
     */
    public void safePostMenuItem(ExtensionMenu attachedObject, MenuItem onThisItem)
    {
        if (extensionBluej == null) 
            return;

        try {
            ExtensionBridge.postMenuItem(extensionBluej, attachedObject, onThisItem );
        }
        catch (Throwable exc) {
            Debug.message("ExtensionWrapper.safePostGenGetMenuItem: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
        }
    }
}
