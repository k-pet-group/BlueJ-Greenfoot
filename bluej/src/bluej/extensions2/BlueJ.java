/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2013,2014,2016,2018,2019  Michael Kolling and John Rosenberg
 
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
package bluej.extensions2;

import bluej.Config;
import bluej.collect.DataCollector;
import bluej.extensions2.event.*;
import bluej.extensions2.event.CompileEvent.EventType;
import bluej.extmgr.ExtensionMenu;
import bluej.extmgr.ExtensionPrefManager;
import bluej.extmgr.ExtensionWrapper;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;

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
 * Attempts to invoke methods on a BlueJ object made by an extension
 * after its <code>terminate()</code> method has been called will result
 * in an (unchecked) <code>ExtensionUnloadedException</code> being thrown.
 *
 */

/*
 * Author Clive Miller, University of Kent at Canterbury, 2002
 * Author Damiano Bolla, University of Kent at Canterbury, 2003, 2004, 2005
 */
public final class BlueJ
{
    private final ExtensionWrapper myWrapper;
    private final ExtensionPrefManager prefManager;

    private PreferenceGenerator currentPrefGen = null;
    private MenuGenerator currentMenuGen = null;
    private Properties localLabels;

    private ArrayList<ExtensionEventListener> eventListeners;
    // This is the queue for the whole of them
    private ArrayList<ApplicationListener> applicationListeners;
    private ArrayList<PackageListener> packageListeners;
    private ArrayList<CompileListener> compileListeners;
    private ArrayList<InvocationFinishedListener> invocationFinishedListeners;
    private ArrayList<ClassListener> classListeners;

    /**
     * Constructor for a BlueJ proxy object.
     * See {@link ExtensionBridge}
     *
     * @param  aWrapper      the {@link ExtensionWrapper} object associated with this extension.
     * @param  aPrefManager  the ExtensionPrefManager object used by BlueJ to add an extensions's UI in BlueJ's preferences tab.
     */
    BlueJ(ExtensionWrapper aWrapper, ExtensionPrefManager aPrefManager)
    {
        myWrapper = aWrapper;
        prefManager = aPrefManager;

        eventListeners = new ArrayList<ExtensionEventListener>();
        applicationListeners = new ArrayList<ApplicationListener>();
        packageListeners = new ArrayList<PackageListener>();
        compileListeners = new ArrayList<CompileListener>();
        invocationFinishedListeners = new ArrayList<InvocationFinishedListener>();
        classListeners = new ArrayList<ClassListener>();

        // Don't use lazy initialisation here, to avoid multiple reloads
        localLabels = myWrapper.getLabelProperties();
    }

    /**
     * Opens a BlueJ project.
     *
     * @param  directory  location where the project is stored.
     * @return            A {@link BProject} that wraps the newly opened project, or <code>null</code> if it cannot be opened.
     */
    public final BProject openProject(File directory)
    {
        if (!myWrapper.isValid())
            throw new ExtensionUnloadedException();

        // Yes somebody may just call it with null, for fun..
        if (directory == null)
            return null;

        Project openProj = Project.openProject(directory.getAbsolutePath());
        if (openProj == null)
            return null;

        // a hack, since bluej does not handle "opening" of projects correctly.
        // this code should really be into openProject or it should not be possible to open
        // a project is the initial package name is not there.
        Package pkg = openProj.getCachedPackage(openProj.getInitialPackageName());
        if (pkg == null)
            return null;

        // I make a new identifier out of this
        Identifier aProject = new Identifier(openProj, pkg);

        // This will make the frame if not already there. should not be needed...
        try {
            aProject.getPackageFrame();
        } catch (ExtensionException exc) {}

        // Note: the previous Identifier is not used here.
        return openProj.getBProject();
    }


    /**
     * Creates a new BlueJ project.
     *
     * @param  directory    location where the project will be stored, it must be writable.
     * @return              A {@link BProject} object wrapping the newly created project if successful, <code>null</code> otherwise.
     */
    public BProject newProject(File directory)
    {
        if (!myWrapper.isValid())
            throw new ExtensionUnloadedException();

        String pathString = directory.getAbsolutePath();
        if (!pathString.endsWith(File.separator))
            pathString += File.separator;

        if (!Project.createNewProject(pathString))
            return null;

        // In order to create all necessary files, we "open" then project here
        // this is not opening the project in BlueJ's interface
        Project openProj = Project.openProject(directory.getAbsolutePath());
        if (openProj == null)
            return null;

        return openProj.getBProject();
    }
    
    /**
     * Returns all currently open projects.
     *
     * @return  An array of {@link BProject} objects representing opened projects, empty array if no project are opened.
     */
    public BProject[] getOpenProjects()
    {
        if (!myWrapper.isValid())
            throw new ExtensionUnloadedException();

        Collection<Project> projects = Project.getProjects();
        BProject[] result = new BProject[projects.size()];

        Iterator<Project> iter; int index;
        for (iter = projects.iterator(), index = 0; iter.hasNext(); index++) {
            Project prj = iter.next();
            result[index] = prj.getBProject();
        }

        return result;
    }


    /**
     * Returns the current package.
     * The current package is the one that is currently selected by the
     * user interface.
     *
     * @return A {@link BPackage} object wrapping the current package, <code>null</code> if there is no currently open package.
     */
    public BPackage getCurrentPackage()
    {
        if (!myWrapper.isValid())
            throw new ExtensionUnloadedException();

        // This is here and NOT into a BProject since it depends on user interface.

        PkgMgrFrame pmf = PkgMgrFrame.getMostRecent();
        // If there is nothing at all open there is no Frame open...
        if (pmf == null)
            return null;

        Package pkg = pmf.getPackage();
        // The frame may be there BUT have no package.
        if (pkg == null)
            return null;

        return pkg.getBPackage();
    }


    /**
     * Returns the current JavaFX Stage being displayed.
     * Can be used (e.g.) as a "parent" frame for positioning modal dialogs.
     * If there is a package currently open, it's probably better to use its {@link BPackage#getWindow()}
     * method to provide better placement.
     *
     * @return  A {@link Stage} object representing the current JavaFX Stage object
     */
    public Stage getCurrentWindow()
    {
        if (!myWrapper.isValid())
            throw new ExtensionUnloadedException();

        return PkgMgrFrame.getMostRecent().getWindow();
    }


    /**
     * Installs a new menu generator for this extension.
     *
     * @param  menuGen a new {@link MenuGenerator}, <code>null</code> to delete a previously installed menu.
     */
    public void setMenuGenerator(MenuGenerator menuGen)
    {
        if (!myWrapper.isValid())
            throw new ExtensionUnloadedException();

        currentMenuGen = menuGen;
    }


    /**
     * Returns the currently registered menu generator
     */
    public MenuGenerator getMenuGenerator()
    {
        return currentMenuGen;
    }


    /**
     * Installs a new preference panel for this extension.
     *
     * @param  prefGen  a class instance that implements the {@link PreferenceGenerator} interface,
     *                  <code>null</code> to delete a previously installed preference panel.
     */
    public void setPreferenceGenerator(PreferenceGenerator prefGen)
    {
        if (!myWrapper.isValid())
            throw new ExtensionUnloadedException();

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
     * This can be used to locate system-wide configuration files.
     * A file can then be located within this directory.
     */
    public File getSystemLibDir()
    {
        if (!myWrapper.isValid())
            throw new ExtensionUnloadedException();

        return Config.getBlueJLibDir();
    }


    /**
     * Returns the path of the user configuration directory.
     * This can be used to locate user dependent information.
     * A file can then be located within this directory.
     */
    public File getUserConfigDir()
    {
        if (!myWrapper.isValid())
            throw new ExtensionUnloadedException();

        return Config.getUserConfigDir();
    }
    
    /**
     * Returns the data-collection user ID.
     * Used by extensions that aim to augment the BlueJ data collection project.
     *
     * @return the user ID, as read from the properties file.
     */
    public String getDataCollectionUniqueID()
    {
        return DataCollector.getUserID();
    }


    /**
     * Returns a property from BlueJ's properties.
     *
     * @param  property  the name of the required global property
     * @param  def       the default value to use if the property cannot be found.
     * @return           The requested property's value, or the default value if the property is not currently set.
     */
    public String getBlueJPropertyString(String property, String def)
    {
        if (!myWrapper.isValid())
            throw new ExtensionUnloadedException();

        return Config.getPropString(property, def);
    }


    /**
     * Returns a property associated with this extension from the standard BlueJ property repository.
     * Extensions must use {@link #setExtensionPropertyString(String, String)} to write any property that should be stored.
     *
     * @param  property  the name of the required global property.
     * @param  def       the default value to use if the property cannot be found.
     * @return           The value of that property, or the default value if the property is not currently set.
     */
    public String getExtensionPropertyString(String property, String def)
    {
        if (!myWrapper.isValid())
            throw new ExtensionUnloadedException();

        String thisKey = myWrapper.getSettingsString(property);
        return Config.getPropString(thisKey, def);
    }


    /**
     * Sets a property associated with this extension into the standard BlueJ property repository.
     * The property name does not need to be fully qualified since a prefix will be prepended to it.
     *
     *
     * @param  property  the name of the required global property
     * @param  value     the required value of that property (or null to remove the property)
     */
    public void setExtensionPropertyString(String property, String value)
    {
        if (!myWrapper.isValid()) {
            throw new ExtensionUnloadedException();
        }

        String thisKey = myWrapper.getSettingsString(property);
        if (value != null) {
            Config.putPropString(thisKey, value);
        }
        else {
            Config.removeProperty(thisKey);
        }
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
     *
     * @param  key  the name of the required label
     * @return      The label value.
     */
    public String getLabel(String key)
    {
        if (!myWrapper.isValid())
            throw new ExtensionUnloadedException();

        // If there are no label for this extension I can only return the system ones.
        if (localLabels == null)
            return Config.getString(key, key);

        // In theory there are label for this extension let me try to get them
        String aLabel = localLabels.getProperty(key, null);

        // Found what I wanted, job done.
        if (aLabel != null)
            return aLabel;

        // ok, the only hope is to get it from the system
        return Config.getString(key, key);
    }



    /**
     * Registers a listener for all the events generated by BlueJ.
     *
     * @param listener an {@link ExtensionEventListener} object to register.
     */
    public void addExtensionEventListener(ExtensionEventListener listener)
    {
        if (listener != null) {
            synchronized (eventListeners) {
                eventListeners.add(listener);
            }
        }
    }


    /**
     * Removes the specified listener so that it no longer receives events.
     *
     * @param listener an {@link ExtensionEventListener} object to unregister.
     */
    public void removeExtensionEventListener(ExtensionEventListener listener)
    {
        if (listener != null) {
            synchronized (eventListeners) {
                eventListeners.remove(listener);
            }
        }
    }


    /**
     * Registers a listener for application events.
     *
     * @param listener an {@link ApplicationListener} object to register.
     */
    public void addApplicationListener(ApplicationListener listener)
    {
        if (listener != null) {
            synchronized (applicationListeners) {
                applicationListeners.add(listener);
            }

            // Relay a previous given up message:
            if (DataCollector.hasGivenUp())
                listener.dataSubmissionFailed(new ApplicationEvent(ApplicationEvent.EventType.DATA_SUBMISSION_FAILED_EVENT));
        }
    }


    /**
     * Removes the listener specified listener so that it no longer receives application events.
     *
     * @param listener an {@link ApplicationListener} object to unregister.
     */
    public void removeApplicationListener(ApplicationListener listener)
    {
        if (listener != null) {
            synchronized (applicationListeners) {
                applicationListeners.remove(listener);
            }
        }
    }


    /**
     * Registers a listener for package events.
     *
     * @param listener an {@link PackageListener} object to register.
     */
    public void addPackageListener(PackageListener listener)
    {
        if (listener != null) {
            synchronized (packageListeners) {
                packageListeners.add(listener);
            }
        }
    }


    /**
     * Removes the specified listener so that it no longer receives package events.
     *
     * @param listener an {@link PackageListener} object to unregister.
     */
    public void removePackageListener(PackageListener listener)
    {
        if (listener != null) {
            synchronized (packageListeners) {
                packageListeners.remove(listener);
            }
        }
    }


    /**
     * Registers a listener for compile events.
     *
     * @param listener an {@link CompileListener} object to register.
     */
    public void addCompileListener(CompileListener listener)
    {
        if (listener != null) {
            synchronized (compileListeners) {
                compileListeners.add(listener);
            }
        }
    }


    /**
     * Removes the specified listener so that it no longer receives compile events.
     *
     * @param listener an {@link CompileListener} object to unregister.
     */
    public void removeCompileListener(CompileListener listener)
    {
        if (listener != null) {
            synchronized (compileListeners) {
                compileListeners.remove(listener);
            }
        }
    }


    /**
     * Registers a listener for invocation finished events.
     *
     * @param listener an {@link InvocationFinishedListener} object to register.
     */
    public void addInvocationFinishedListener(InvocationFinishedListener listener)
    {
        if (listener != null) {
            synchronized (invocationFinishedListeners) {
                invocationFinishedListeners.add(listener);
            }
        }
    }


    /**
     * Removes the specified listener so no that it no longer receives invocation finished events.
     *
     * @param listener an {@link InvocationFinishedListener} object to unregister.
     */
    public void removeInvocationFinishedListener(InvocationFinishedListener listener)
    {
        if (listener != null) {
            synchronized (invocationFinishedListeners) {
                invocationFinishedListeners.remove(listener);
            }
        }
    }


    /**
     * Registers a listener for class events.
     *
     * @param listener an {@link ClassListener} object to register.
     */
    public void addClassListener(ClassListener listener)
    {
        if (listener != null) {
            synchronized (classListeners) {
                classListeners.add(listener);
            }
        }
    }
    
    /**
     * Removes the specified class listener so no that it no longer receives
     * class events.
     *
     * @param listener an {@link ClassListener} object to unregister.
     */
    public void removeClassListener(ClassListener listener)
    {
        if (listener != null) {
            synchronized (classListeners) {
                classListeners.remove(listener);
            }
        }
    }

    /**
     * Dispatches this event to the listeners for the ALL events.
     *
     * @param event an {@link ExtensionEvent} object to dispatch.

     */
    private void delegateExtensionEvent(ExtensionEvent event)
    {
        ExtensionEventListener[] listeners;
        
        synchronized (eventListeners) {
            listeners = eventListeners.toArray(new ExtensionEventListener[eventListeners.size()]);
        }
        
        for (int i = 0; i < listeners.length; i++) {
            ExtensionEventListener eventListener = listeners[i];
            eventListener.eventOccurred(event);
        }
    }


    /**
     * Dispatches this event to the listeners for the Application events.
     *
     * @param event an {@link ApplicationEvent} object to dispatch.
     */
    private void delegateApplicationEvent(ApplicationEvent event)
    {
        ApplicationListener[] listeners;
        
        synchronized (applicationListeners) {
            listeners = (ApplicationListener[]) applicationListeners.toArray(new ApplicationListener[applicationListeners.size()]);
        }
        
        for (int i = 0; i < listeners.length; i++) {
            ApplicationListener eventListener = listeners[i];
            if (event.getEventType() == ApplicationEvent.EventType.APP_READY_EVENT)
                eventListener.blueJReady(event);
            else if (event.getEventType() == ApplicationEvent.EventType.DATA_SUBMISSION_FAILED_EVENT)
                eventListener.dataSubmissionFailed(event);
        }
    }


    /**
     * Dispatches this event to the listeners for the Package events.
     *
     * @param event an {@link PackageEvent} object to dispatch.
     */
    private void delegatePackageEvent(PackageEvent event)
    {
        PackageListener[] listeners;
        
        synchronized (packageListeners) {
            listeners = packageListeners.toArray(new PackageListener[packageListeners.size()]);
        }
        
        PackageEvent.EventType thisEvent = event.getEventType();

        for (int i = 0; i < listeners.length; i++) {
            PackageListener eventListener = listeners[i];
            switch (thisEvent)
            {
                case PACKAGE_OPENED:
                    eventListener.packageOpened(event);
                    break;
                case PACKAGE_CLOSING:
                    eventListener.packageClosing(event);
                    break;
            }
        }
    }


    /**
     * Dispatches this event to the listeners for the Compile events.
     *
     * @param event an {@link CompileEvent} object to dispatch.
     */
    private void delegateCompileEvent(CompileEvent event)
    {
        CompileListener[] listeners;
        
        synchronized (compileListeners) {
            listeners = (CompileListener[]) compileListeners.toArray(new CompileListener[compileListeners.size()]);
        }
        
        EventType thisEvent = event.getEventType();

        for (int i = 0; i < listeners.length; i++) {
            CompileListener eventListener = listeners[i];
            switch(thisEvent){
                case COMPILE_START_EVENT:
                    eventListener.compileStarted(event);
                    break;
                case COMPILE_ERROR_EVENT:
                    eventListener.compileError(event);
                    break;
                case COMPILE_WARNING_EVENT:
                    eventListener.compileWarning(event);
                    break;
                case COMPILE_FAILED_EVENT:
                    eventListener.compileFailed(event);
                    break;
                case COMPILE_DONE_EVENT:
                    eventListener.compileSucceeded(event);
                    break;
            }
        }
    }


    /**
     * Dispatches this event to the listeners for the InvocationFinished events.
     *
     * @param event an {@link InvocationFinishedEvent} object to dispatch.
     */
    private void delegateInvocationEvent(InvocationFinishedEvent event)
    {
        InvocationFinishedListener[] listeners;
        
        synchronized (invocationFinishedListeners) {
            listeners = (InvocationFinishedListener[]) invocationFinishedListeners.toArray(new InvocationFinishedListener[invocationFinishedListeners.size()]);
        }
        
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].invocationFinished(event);
        }
    }

    /**
     * Dispatches a class event to the appropriate listeners.
     *
     * @param event an {@link ClassEvent} object to dispatch.
     */
    private void delegateClassEvent(ClassEvent event)
    {
        // We'll make a copy of the current list to prevent
        // ConcurrentModification problems.
        ClassListener[] listeners;
        
        synchronized (classListeners) {
            listeners = (ClassListener[]) classListeners.toArray(new ClassListener[classListeners.size()]);
        }

        for (int i = 0; i < listeners.length; i++)
        {
            switch (event.getEventType())
            {
                case STATE_CHANGED:
                    listeners[i].classStateChanged(event);
                    break;
                case CHANGED_NAME:
                    listeners[i].classNameChanged(event);
                    break;
                case REMOVED:
                    listeners[i].classRemoved(event);
                    break;
            }
        }
    }

    /**
     * Informs any registered listeners that an event has occurred.
     * This will call the various dispatcher as needed.
     * Errors will be trapped by the caller.
     *
     * @param event an {@link ExtensionEvent} object to dispatch.
     */
    void delegateEvent(ExtensionEvent event)
    {
        delegateExtensionEvent(event);
        if (event instanceof ApplicationEvent)
            delegateApplicationEvent((ApplicationEvent) event);
        else if (event instanceof PackageEvent)
            delegatePackageEvent((PackageEvent) event);
        else if (event instanceof CompileEvent)
            delegateCompileEvent((CompileEvent) event);
        else if (event instanceof InvocationFinishedEvent)
            delegateInvocationEvent((InvocationFinishedEvent) event);
        else if (event instanceof ClassEvent)
            delegateClassEvent((ClassEvent) event);
    }

    /**
     * Calls the extension to get the right menu item.
     * This is already wrapped for errors in the caller.
     * It is right for it to create a new wrapped object each time.
     * Extensions do not to share objects.
     * It is here since it can access all constructors directly.
     *
     * @param  attachedObject  an {@link ExtensionMenu} object that will contain the generated {@link MenuItem}.
     * @return                 The generated {@link MenuItem}, <code>null</code> if the extension has not installed any {@link MenuGenerator},
     *                          or if the argument <code>attachedObject</code> is <code>null</code>.
     */
    MenuItem getMenuItem(ExtensionMenu attachedObject)
    {
        if ((currentMenuGen == null) || (attachedObject == null)) {
            return null;
        }

        return attachedObject.getMenuItem(currentMenuGen);
    }


    /**
     * Posts a notification of a menu going to be displayed
     *
     * @param  attachedObject  an {@link ExtensionMenu} object that contains extension generated {@link MenuItem} object.
     * @param  onThisItem  an {@link MenuItem} object that triggered a call to this method.
     */
    void postMenuItem(ExtensionMenu attachedObject, MenuItem onThisItem)
    {
        if ((currentMenuGen != null) && (attachedObject != null)) {
            attachedObject.postMenuItem(currentMenuGen, onThisItem);
        }
    }
}
