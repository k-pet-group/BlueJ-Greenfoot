/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kšlling and John Rosenberg 
 
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

import bluej.*;
import bluej.extensions.*;
import bluej.extensions.event.*;
import bluej.pkgmgr.*;
import bluej.utility.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import javax.swing.*;
import java.lang.ClassNotFoundException;

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
 *  
 *  Author: Damiano Bolla: 2002,2003,2004
 */
public class ExtensionWrapper
{
    private final ExtensionsManager extensionsManager;
    private final ExtensionPrefManager prefManager;

    private File extensionJarFileName;

    // If != null the jar is good. DO NOT expose this unless REALLY needed
    private Class extensionClass;

    // If != null the extension is loaded. do NOT expose this unless REALLY needed
    private Extension extensionInstance;

    private BlueJ   extensionBluej;
    private String  extensionStatusString;
    private Project project;

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
    public ExtensionWrapper(ExtensionsManager extensionsManager, ExtensionPrefManager prefManager, File jarFile)
    {
        this.extensionsManager = extensionsManager;
        this.prefManager = prefManager;

        // Let me try to load the extension class
        if ((extensionClass = getExtensionClass(jarFile)) == null)  return;

        extensionJarFileName  = jarFile;
    }


    /**
     *  This is in charge of returning a valid extension class, if any in the
     *  given jar file, it will return the Class or null if none is found
     *  NOTE: I am showing some messages in case of failure since otherwise a user may never
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

            URL url = jarFileName.toURI().toURL();
            URL[] urlList=new URL[]{url};
            FirewallLoader fireLoader = new FirewallLoader(getClass().getClassLoader());
            URLClassLoader ucl = new URLClassLoader(urlList,fireLoader);

            classRisul = ucl.loadClass(className);
            if (!Extension.class.isAssignableFrom(classRisul)) {
                Debug.message(errorPrefix+Config.getString("extmgr.error.notsubclass"));
                return null;
            }
        } catch (Throwable exc) {
            Debug.message(errorPrefix+"Exception="+exc.getMessage());
            exc.printStackTrace();
            return null;
        }

        return classRisul;
    }

    /**
     * It is a bit of magic, really :-)
     */
    class FirewallLoader extends ClassLoader
    {
        ClassLoader myParent;
      
        FirewallLoader ( ClassLoader parent )
        {
            myParent = parent;
        }

        public Class findClass(String name) throws ClassNotFoundException
        {
            if ( name.startsWith("bluej.") ) {
//              Debug.message("Firewall OK: "+name);
                return myParent.loadClass(name);
            }

//          if ( name.startsWith("antlr.") ) System.out.println ("Firewall =="+name);
            throw new ClassNotFoundException();
        }
    }
  
    /**
     *  Now, assume you have the class and you want to "istantiate" the
     *  extension You have to call this. NOTE that the extension wrapper is
     *  ALREADY UP and running. I do not return a value, you may check
     *  how this went by using the isValid() method...
     *
     * @param  project  The project this extensionis linked to, null if none
     */
    void newExtension(Project aProject)
    {
        // It may happen
        if (extensionClass == null)  return;

        project = aProject;
        extensionBluej = ExtensionBridge.newBluej(this, prefManager);

        try {
            extensionInstance = (Extension)extensionClass.newInstance();
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

        // Ok, time to really start everything... This MUST be here.... after all is initialzed
        safeStartup(extensionBluej);
        extensionStatusString = Config.getString("extmgr.status.loaded");
    }




    /**
     *  Gets the project this extension is associated with.
     *  This happens in case of extensions loaded with a Project.
     *  If it is a systemwhide extension this will be null.
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
    boolean isJarValid()
    {
        return (extensionClass != null);
    }


    /**
     *  Kills off this extension as much as possible
     *  items and making access to BlueJ no longer possible.
     *  Not only ! we are even going to release the wrapper after this.
     *  So it can be loaded again, hopefully from a clean environment
     */
    void terminate()
    {
//        Debug.message("Extension.terminate(): class="+getExtensionClassName());

        safeTerminate();

        // Needed to signal to the revalidate that this instance is no longer here.            
        extensionInstance = null;

        // Time to clean up things from the visul point of view.
        prefManager.panelRevalidate();

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
        if (extensionClass == null) 
            return null;

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
        if ( ! isValid() ) return "ExtensionWrapper: invalid";

        return "ExtensionWrapper: "+ extensionClass.getName();
    }

    /* ====================== ERROR WRAPPED CALLS HERE =========================
     * I need to wrapp ALL calls from BlueJ to the Extension into a try/catch
     * Othervise an error in the extension will render BlueJ unusable. Damiano
     */

    /**
     * Informs any registered listeners that an event has occurred.
     */
    public void safeEventOccurred(ExtensionEvent event)
    {
        if (!isValid()) 
            return;

        try {
            ExtensionBridge.delegateEvent(extensionBluej,event);
        }
        catch (Exception exc)  {
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
        if (extensionInstance == null) 
            return null;

        try {
            return extensionInstance.getDescription();
        }
        catch (Exception exc)  {
            Debug.message("ExtensionWrapper.safeGetExtensionDescription: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
            return null;
        }
    }

    
    /**
     * Returns the extension's name.
     * It would be far more reliable to use the full class name of the extension. Damiano
     */
    public String safeGetExtensionName()
    {
        if (extensionInstance == null) 
            return "";

        try {
            return extensionInstance.getName();
        }
        catch (Exception exc) {
            Debug.message("ExtensionWrapper.safeGetExtensionName: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
            return "";
        }
    }

    
    /**
     *  Gets the extension's 'further information' URL
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
        catch (Exception exc) {
            Debug.message("ExtensionWrapper.safeGetURL: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
            return null;
        }
    }


    /**
     *  Gets the formal version of this extension.
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
        catch (Exception exc) {
            Debug.message("ExtensionWrapper.safeGetExtensionVersion: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
            return null;
        }
    }


    /**
     *  Ask to the extension if it thinks if it si compatible.
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
        catch (Exception exc) {
            Debug.message("ExtensionWrapper.safeIsCompatible: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
            // If one bombs at me it shurely is not compatilbe 
            return false;
        }
    }

    
    /**
     *  Call the startup method in a safe way
     *
     * @return  true if it is, false otherwise
     */
    private void safeStartup(BlueJ bluejProxy)
    {
        if (extensionInstance == null) 
            return;

        try {
            extensionInstance.startup(bluejProxy);
        }
        catch (Exception exc)  {
            Debug.message("ExtensionWrapper.safeStartup: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
        }
    }


    /**
     *  Call the terminate method in a safe way
     *
     * @return  true if it is, false otherwise
     */
    private void safeTerminate()
    {
        if (extensionInstance == null) 
            return;

        try {
            // Give a chance to extension to clear up after itself.
            extensionInstance.terminate();
        }
        catch (Exception exc) {
            Debug.message("ExtensionWrapper.safeTerminate: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
        }
    }


    /**
     *  Calls the EXTENSION preference panel loadValues in a sfe way
     */
    public void safePrefGenLoadValues()
    {
        if (extensionBluej == null) 
            return;

        PreferenceGenerator aPrefGen = extensionBluej.getPreferenceGenerator();
        // The above is dafe. An extension may not have a preference panel
        if (aPrefGen == null) 
            return;

        try {
            aPrefGen.loadValues();
        }
        catch (Exception exc) {
            Debug.message("ExtensionWrapper.safePrefGenLoadValues: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
        }
    }

    
    /**
     *  Calls the EXTENSION preference panel saveValues in a sfe way
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
        catch (Exception exc) {
            Debug.message("ExtensionWrapper.safePrefGenSaveValues: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
        }
    }

    
    /**
     *  Calls the EXTENSION preference panel getPanel in a sfe way
     */
    public JPanel safePrefGenGetPanel()
    {
        if (extensionBluej == null) 
            return null;

        PreferenceGenerator aPrefGen = extensionBluej.getPreferenceGenerator();
        // The above is dafe. An extension may not have a preference panel
        if (aPrefGen == null) 
            return null;

        try {
            return aPrefGen.getPanel();
        }
        catch (Exception exc) {
            Debug.message("ExtensionWrapper.safePrefGenGetPanel: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
            return null;
        }
    }


    /**
     *  Calls the EXTENSION getMenuItem in a safe way
     */
    public JMenuItem safeGetMenuItem(Object attachedObject)
    {
        if (extensionBluej == null) 
            return null;

        try {
            return ExtensionBridge.getMenuItem(extensionBluej, attachedObject);
        }
        catch (Exception exc) {
            Debug.message("ExtensionWrapper.safeMenuGenGetMenuItem: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
            return null;
        }
    }

    /**
     *  Calls the EXTENSION postMenuItem in a safe way
     */
    public void safePostMenuItem(Object attachedObject, JMenuItem onThisItem)
    {
        if (extensionBluej == null) 
            return;

        try {
            ExtensionBridge.postMenuItem(extensionBluej, attachedObject, onThisItem );
        }
        catch (Exception exc) {
            Debug.message("ExtensionWrapper.safePostGenGetMenuItem: Class="+getExtensionClassName()+" Exception="+exc.getMessage());
            exc.printStackTrace();
        }
    }




}
