package bluej.extmgr;

import java.io.File;
import java.util.*;

import javax.swing.JFrame;

import bluej.*;
import bluej.debugmgr.ExecutionEvent;
import bluej.extensions.event.*;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.utility.Debug;
import javax.swing.*;

/**
 *  Manages extensions and provides the main interface to PkgMgrFrame.
 *  
 *  Author Clive Miller, University of Kent at Canterbury, 2002
 *  Author Damiano Bolla, University of Kent at Canterbury, 2003
 */
public class ExtensionsManager implements BlueJEventListener
{
    private static ExtensionsManager instance;

    /**
     * Create a new singleton instance of the ExtensionsManager.
     */
    public static ExtensionsManager initialise( )
    {
        if (instance != null) 
          {
          // Really, there is no need to trow an exception.
          Debug.message("ExtensionManager is already initilized");
          return instance;
          }

        return  instance = new ExtensionsManager( );
    }

    /**
     * Returns the ExtensionManager instance.
     */
    public static ExtensionsManager get()
    {
        return instance;
    }


    private List extensions;
    private PrefManager prefManager;

    /**
     *  Constructor for the ExtensionsManager object.
     *  It is private to be a singleton.
     */
    private ExtensionsManager()
    {
        // Sync issues should be clear... 
        extensions = new ArrayList();

        // This will also register the panel with BlueJ.
        prefManager = new PrefManager(extensions);

        // This must be here, after all has been initialized.
        BlueJEvent.addListener(this);
    }


    /**
     * Loads extensions that are in system and user location.
     */
    public void loadExtensions()
    {
        // Most of the time the systemDirectory will be this.
        File systemDir = new File(Config.getBlueJLibDir(), "extensions");

        String dirPath = Config.getPropString("bluej.extensions.systempath", null);
        // But we allow one to override the default location of system extension.
        if (dirPath != null ) systemDir = new File(dirPath);

        // Now we try to load the extensions from the BlueJ system repository.
        loadDirectoryExtensions(systemDir, null);

        // Load extensions that are in a user space location.
        loadDirectoryExtensions(Config.getUserConfigFile("extensions"), null);
    }

    /**
     * Unloads all extensions that are loaded.
     * Normally called just before bluej is closing.
     */
    public synchronized void unloadExtensions ()
      {
      for (Iterator iter = extensions.iterator(); iter.hasNext(); ) 
        {
        ExtensionWrapper aWrapper = (ExtensionWrapper) iter.next();

        aWrapper.terminate();         // The following terminated the Extension
        iter.remove();
        }
      }

    /**
     *  Searches through the given directory for jar files that contain a valid
     *  extension. If it finds a loadable extension it will add it to the loaded
     *  extensions. This IS the function that should be called to load
     *  extensions
     *  
     *  NOTE: This one will either ADD or delete stuffe from the list of extensions
     *  so it must be syncronized....
     *
     * @param  directory  Where to look for extensions
     * @param  project    A project this extension is bound to
     */
    private synchronized void loadDirectoryExtensions(File directory, Project project)
    {
        if (directory == null) return;

        File[] files = directory.listFiles();
        if (files == null) return;

        for (int index = 0; index < files.length; index++) {
            File thisFile = files[index];

            // We do not want to try to make sense of directories
            if (thisFile.isDirectory()) continue;

            // Skip also files that do not end in .jar
            if (!thisFile.getName().endsWith(".jar")) continue;

            // Ok, lets try to get a wrapper up and running
            ExtensionWrapper aWrapper = new ExtensionWrapper(this, prefManager, thisFile);

            // Loading this warpper failed miserably, too bad...
            if (!aWrapper.isJarValid()) continue;

            // Let me see if I already have this extension loaded
            if (isWrapperAlreadyLoaded(aWrapper)) continue;

            // This MUST be here in ANY case since otherwise this wrapper is NOT on the list..
            extensions.add(aWrapper);

            // Now that all is nice and clean I can safely try to instantiate the extension
            aWrapper.newExtension(project);

            // but wait, it is not finished. If the wrapper is invalid we got to remove it
            if ( !aWrapper.isValid() ) extensions.remove(aWrapper);
        }
    }


    /**
     * Checks if the loaded wrappers/extensions if is already loaded.
     * In case of strange params we return false, meaning that the given wrapper is NOT
     * loaded in the system. 
     * It is a reasonable response, afer all this wrapper is not loaded...
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
                Debug.message("Extension is already loaded: " +thisClassName+" jarName="+thisJarName);
                return true;
            }
        }

        // This wrapper is not already loaded in the list of wrappers/extensions
        return false;
    }

    /**
     * Ask for extension manager to show the help dialog for extensions.
     * This is here to be shure that the help dialog is called when extension manager is valid.
     */
    public void showHelp ( JFrame parentFrame )
      {
      // It should be constructed and then destroyed, since it is associated with a frame...
      // I give it an unmodifiable collection so I am not LOCKING the list of extensions.
      HelpDialog aHelp = new HelpDialog ( Collections.unmodifiableList(extensions), parentFrame );
      }

    /**
     *  Searches for and loads any new extensions found in the project.
     */
    public void projectOpening( Project project )
    {
        File exts = new File(project.getProjectDir(), "extensions");
        loadDirectoryExtensions(exts, project);
    }


    /**
     * Informs extension that a package has been opened
     */
    public void packageOpened(Package pkg)
    {
        delegateEvent(new PackageEvent(PackageEvent.PACKAGE_OPENED, pkg));
    }


    /**
     *  This package frame is about to be closed.
     *  The issue here is to remove the extension if this is the right time to do it.
     *  NOTA: This must be syncronized since it changes the extensionslist
     */
    public synchronized void packageClosing(Package pkg)
    {
        // Before removing the extension let signl that this package is closing
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
        if (!invalidateExtension) return;

        // I am closing the last frame of the project, time to invalidate the right extensions
        for (Iterator iter = extensions.iterator(); iter.hasNext(); ) {
            ExtensionWrapper aWrapper = (ExtensionWrapper) iter.next();

            // If the extension did not got loaded with this project skip it...
            if (thisProject != aWrapper.getProject())  continue;

            // The following terminated the Extension
            aWrapper.terminate();
            iter.remove();
        }
    }


    /**
     * Returns true if I should not ask for a menu to this extensions
     */
    private boolean skipThisMenu ( Project onThisProject, Project extensionProject )
      {
      // I want menus if nothing it is a sys loaded extension on an empty frame
      if ( onThisProject == null && extensionProject == null ) return false;

      // Menu should not be generated if an extension belongs to a project
      if ( onThisProject == null && extensionProject != null ) return true;

      // Menu should be generated if we are openieng a project and an extension is not boukd to any
      if ( onThisProject != null && extensionProject == null ) return false;

      // now both are not null, generate a menu if they are the same.
      if ( onThisProject == extensionProject ) return false;

      // None of the above cases, do not generate a menu
      return true;
      }
      
    /**
     * Returns a List of menues currently provided by extensions.
     */
    LinkedList getMenuItems( Object attachedObject, Project onThisProject )
    {
        LinkedList menuItems = new LinkedList();

        for (Iterator iter = extensions.iterator(); iter.hasNext(); ) {
            ExtensionWrapper aWrapper = (ExtensionWrapper) iter.next();

            if (!aWrapper.isValid()) continue;

            if ( skipThisMenu(onThisProject, aWrapper.getProject() )) continue;
            
            JMenuItem anItem = aWrapper.safeGetMenuItem(attachedObject);
            if ( anItem == null ) continue;

            anItem.putClientProperty("bluej.extmgr.ExtensionWrapper",aWrapper);
            
            menuItems.add(anItem);
            }

        return menuItems;
    }

    /**
     * Delegates an event to all known extensions.
     */
    public void delegateEvent(ExtensionEvent event)
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

        if ( eventId == BlueJEvent.EXECUTION_RESULT )
            {
            ExecutionEvent exevent = (ExecutionEvent) arg;
            delegateEvent ( new InvocationEvent ( exevent ) );
            return;              
            }

        if ( eventId == BlueJEvent.CREATE_VM_DONE) 
            {
            delegateEvent (new ApplicationEvent (ApplicationEvent.APP_READY_EVENT));
            return;
            }

        // I cannot put any warining on unknown events here since I get a bunch of events in any case.
        }

    } // End of class
