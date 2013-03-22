/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2012,2013  Michael Kolling and John Rosenberg 
 
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
package bluej.extensions;

import bluej.*;
import bluej.collect.DataCollector;
import bluej.extensions.event.*;
import bluej.extensions.painter.ExtensionClassTargetPainter;
import bluej.extmgr.*;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.graphPainter.ClassTargetPainter.Layer;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

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
 * @version    $Id: BlueJ.java 10528 2013-03-22 14:09:18Z davmac $
 */

/*
 * Author Clive Miller, University of Kent at Canterbury, 2002
 * Author Damiano Bolla, University of Kent at Canterbury, 2003, 2004, 2005
 */
public final class BlueJ
{
    public static final int SE_PROJECT = 0;
    public static final int ME_PROJECT = 1;
    
    private final ExtensionWrapper myWrapper;
    private final ExtensionPrefManager prefManager;

    private PreferenceGenerator currentPrefGen = null;
    private MenuGenerator currentMenuGen = null;
    private ExtensionClassTargetPainter currentClassTargetPainter;
    private Properties localLabels;

    private ArrayList<ExtensionEventListener> eventListeners;
    // This is the queue for the whole of them
    private ArrayList<ApplicationListener> applicationListeners;
    private ArrayList<PackageListener> packageListeners;
    private ArrayList<CompileListener> compileListeners;
    private ArrayList<InvocationListener> invocationListeners;
    private ArrayList<ClassListener> classListeners;
    private List<DependencyListener> dependencyListeners;
    private List<ClassTargetListener> classTargetListeners;


    /**
     * Constructor for a BlueJ proxy object.
     * See the ExtensionBridge class.
     *
     * @param  aWrapper      Description of the Parameter
     * @param  aPrefManager  Description of the Parameter
     */
    BlueJ(ExtensionWrapper aWrapper, ExtensionPrefManager aPrefManager)
    {
        myWrapper = aWrapper;
        prefManager = aPrefManager;

        eventListeners = new ArrayList<ExtensionEventListener>();
        applicationListeners = new ArrayList<ApplicationListener>();
        packageListeners = new ArrayList<PackageListener>();
        compileListeners = new ArrayList<CompileListener>();
        invocationListeners = new ArrayList<InvocationListener>();
        classListeners = new ArrayList<ClassListener>();
        dependencyListeners = new ArrayList<DependencyListener>();
        classTargetListeners = new ArrayList<ClassTargetListener>();

        // Don't use lazy initialisation here, to avoid multiple reloads
        localLabels = myWrapper.getLabelProperties();
    }

    /**
     * Opens a project.
     *
     * @param  directory  Where the project is stored.
     * @return            the BProject that describes the newly opened project,
     *                    or null if it cannot be opened.
     */
    public final BProject openProject(File directory)
    {
        if (!myWrapper.isValid())
            throw new ExtensionUnloadedException();

        // Yes somebody may just call it with null, for fun..
        if (directory == null)
            return null;

        Project openProj = Project.openProject(directory.getAbsolutePath(), null);
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
     * @param  directory    where you want the project be placed, it must be writable.
     * @param  projectType  the type of project, such as ME or SE.
     * @return              the newly created BProject if successful, null otherwise.
     */
    public BProject newProject(File directory, int projectType )
    {
        if (!myWrapper.isValid())
            throw new ExtensionUnloadedException();

        String pathString = directory.getAbsolutePath();
        if (!pathString.endsWith(File.separator))
            pathString += File.separator;
            
        if (!Project.createNewProject(pathString, projectType == ME_PROJECT))
            return null;
            
        return openProject(directory);
    }


    /**
     * Creates a new BlueJ project.
     *
     * @param  directory  where you want the project be placed, it must be writable.
     * @return            the newly created BProject if successful, null otherwise.
     */
    public BProject newProject(File directory)
    {
        return newProject( directory, SE_PROJECT );
    }

    
    /**
     * Returns all currently open projects.
     * Returns an empty array if no projects are open.
     *
     * @return    The openProjects value
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
     * Returns the currently selected package.
     * The current package is the one that is currently selected by the
     * user interface.
     * It can return null if there is no currently open package.
     *
     * @return    The currentPackage value
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
     * Returns the current frame being displayed.
     * Can be used (e.g.) as a "parent" frame for positioning modal dialogs.
     * If there is a package currently open, it's probably better to use its <code>getFrame()</code>
     * method to provide better placement.
     *
     * @return    The currentFrame value
     */
    public Frame getCurrentFrame()
    {
        if (!myWrapper.isValid())
            throw new ExtensionUnloadedException();

        return PkgMgrFrame.getMostRecent();
    }


    /**
     * Install a new menu generator for this extension.
     * If you want to delete a previously installed menu, then set it to null
     *
     *
     * @param  menuGen        The new menuGenerator value
     */
    public void setMenuGenerator(MenuGenerator menuGen)
    {
        if (!myWrapper.isValid())
            throw new ExtensionUnloadedException();

        currentMenuGen = menuGen;
    }


    /**
     * Returns the currently registered menu generator
     *
     * @return    The menuGenerator value
     */
    public MenuGenerator getMenuGenerator()
    {
        return currentMenuGen;
    }


    /**
     * Install a new preference panel for this extension.
     * If you want to delete a previously installed preference panel, then set it to null
     *
     * @param  prefGen  a class instance that implements the PreferenceGenerator interface.
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
     *
     * @return    The preferenceGenerator value
     */
    public PreferenceGenerator getPreferenceGenerator()
    {
        return currentPrefGen;
    }


    /**
     * Installs a new custom class target painter for this extension. If you
     * want to delete a previously installed custom class target painter, then
     * set it to <code>null</code>.
     * 
     * @param classTargetPainter
     *            The {@link ExtensionClassTargetPainter} to set.
     */
    public void setClassTargetPainter(ExtensionClassTargetPainter classTargetPainter)
    {
        if (!myWrapper.isValid()) {
            throw new ExtensionUnloadedException();
        }

        currentClassTargetPainter = classTargetPainter;
    }

    /**
     * Returns the currently registered custom class target painter.
     * 
     * @return The currently registered custom class target painter.
     */
    public ExtensionClassTargetPainter getClassTargetPainter()
    {
        return currentClassTargetPainter;
    }

    /**
     * Returns the path of the <code>&lt;BLUEJ_HOME&gt;/lib</code> system directory.
     * This can be used to locate systemwide configuration files.
     * Having the directory you can then locate a file within it.
     *
     * @return    The systemLibDir value
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
     * Having the directory you can then locate a file within it.
     *
     * @return    The userConfigDir value
     */
    public File getUserConfigDir()
    {
        if (!myWrapper.isValid())
            throw new ExtensionUnloadedException();

        return Config.getUserConfigDir();
    }
    
    /**
     * Returns the data-collection user ID, for use with extensions that
     * aim to augment the BlueJ data collection project.
     * 
     * Since extension version 2.10
     * 
     * @return the user ID, as read from the properties file.
     */
    public String getDataCollectionUniqueID()
    {
        return DataCollector.getUserID();
    }


    /**
     * Returns a property from BlueJ's properties,
     * or the given default value if the property is not currently set.
     *
     * @param  property  The name of the required global property
     * @param  def       The default value to use if the property cannot be found.
     * @return           the value of the property.
     */
    public String getBlueJPropertyString(String property, String def)
    {
        if (!myWrapper.isValid())
            throw new ExtensionUnloadedException();

        return Config.getPropString(property, def);
    }


    /**
     * Return a property associated with this extension from the standard BlueJ property repository.
     * You must use the setExtensionPropertyString to write any property that you want stored.
     * You can then come back and retrieve it using this function.
     *
     * @param  property  The name of the required global property.
     * @param  def       The default value to use if the property cannot be found.
     * @return           the value of that property.
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
     * @param  property  The name of the required global property
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
     * @param  key  Description of the Parameter
     * @return      The label value
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
     */
    public void addApplicationListener(ApplicationListener listener)
    {
        if (listener != null) {
            synchronized (applicationListeners) {
                applicationListeners.add(listener);
            }
        }
    }


    /**
     * Removes the specified listener so that it no longer receives events.
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
     * Removes the specified listener so that it no longer receives events.
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
     * Removes the specified listener so that it no longer receives events.
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
     * Registers a listener for invocation events.
     */
    public void addInvocationListener(InvocationListener listener)
    {
        if (listener != null) {
            synchronized (invocationListeners) {
                invocationListeners.add(listener);
            }
        }
    }


    /**
     * Removes the specified listener so no that it no longer receives events.
     */
    public void removeInvocationListener(InvocationListener listener)
    {
        if (listener != null) {
            synchronized (invocationListeners) {
                invocationListeners.remove(listener);
            }
        }
    }


    /**
     * Register a listener for class events.
     * 
     * @param listener
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
     * Register a listener for dependency events.
     * 
     * @param listener
     *            The listener to register.
     */
    public void addDependencyListener(DependencyListener listener)
    {
        if (listener != null) {
            synchronized (dependencyListeners) {
                dependencyListeners.add(listener);
            }
        }
    }

    /**
     * Removes the specified dependency listener so it no longer receives
     * dependency events.
     */
    public void removeDependencyListener(DependencyListener listener)
    {
        if (listener != null) {
            synchronized (dependencyListeners) {
                dependencyListeners.remove(listener);
            }
        }
    }
    
    /**
     * Register a listener for class target events.
     * 
     * @param listener
     *            The listener to register.
     */
    public void addClassTargetListener(ClassTargetListener listener)
    {
        if (listener != null) {
            synchronized (classTargetListeners) {
                classTargetListeners.add(listener);
            }
        }
    }

    /**
     * Removes the specified class target listener so it no longer receives
     * class target events.
     */
    public void removeClassTargetListener(ClassTargetListener listener)
    {
        if (listener != null) {
            synchronized (classTargetListeners) {
                classTargetListeners.remove(listener);
            }
        }
    }

    /**
     * Dispatch this event to the listeners for the ALL events.
     *
     * @param  event  Description of the Parameter
     */
    private void delegateExtensionEvent(ExtensionEvent event)
    {
        ExtensionEventListener [] listeners;
        
        synchronized (eventListeners) {
            listeners = (ExtensionEventListener []) eventListeners.toArray(new ExtensionEventListener [eventListeners.size()]);
        }
        
        for (int i = 0; i < listeners.length; i++) {
            ExtensionEventListener eventListener = listeners[i];
            eventListener.eventOccurred(event);
        }
    }


    /**
     * Dispatch this event to the listeners for the Application events.
     *
     * @param  event  Description of the Parameter
     */
    private void delegateApplicationEvent(ApplicationEvent event)
    {
        ApplicationListener [] listeners;
        
        synchronized (applicationListeners) {
            listeners = (ApplicationListener []) applicationListeners.toArray(new ApplicationListener[applicationListeners.size()]);
        }
        
        for (int i = 0; i < listeners.length; i++) {
            ApplicationListener eventListener = listeners[i];
            // Just this for the time being.
            eventListener.blueJReady(event);
        }
    }


    /**
     * Dispatch this event to the listeners for the Package events.
     *
     * @param  event  Description of the Parameter
     */
    private void delegatePackageEvent(PackageEvent event)
    {
        PackageListener [] listeners;
        
        synchronized (packageListeners) {
            listeners = (PackageListener []) packageListeners.toArray(new PackageListener[packageListeners.size()]);
        }
        
        int thisEvent = event.getEvent();

        for (int i = 0; i < listeners.length; i++) {
            PackageListener eventListener = listeners[i];
            if (thisEvent == PackageEvent.PACKAGE_OPENED)
                eventListener.packageOpened(event);
            if (thisEvent == PackageEvent.PACKAGE_CLOSING)
                eventListener.packageClosing(event);
        }
    }


    /**
     * Dispatch this event to the listeners for the Compile events.
     *
     * @param  event  Description of the Parameter
     */
    private void delegateCompileEvent(CompileEvent event)
    {
        CompileListener [] listeners;
        
        synchronized (compileListeners) {
            listeners = (CompileListener []) compileListeners.toArray(new CompileListener[compileListeners.size()]);
        }
        
        int thisEvent = event.getEvent();

        for (int i = 0; i < listeners.length; i++) {
            CompileListener eventListener = listeners[i];
            if (thisEvent == CompileEvent.COMPILE_START_EVENT)
                eventListener.compileStarted(event);
            if (thisEvent == CompileEvent.COMPILE_ERROR_EVENT)
                eventListener.compileError(event);
            if (thisEvent == CompileEvent.COMPILE_WARNING_EVENT)
                eventListener.compileWarning(event);
            if (thisEvent == CompileEvent.COMPILE_FAILED_EVENT)
                eventListener.compileFailed(event);
            if (thisEvent == CompileEvent.COMPILE_DONE_EVENT)
                eventListener.compileSucceeded(event);
        }
    }


    /**
     * Dispatch this event to the listeners for the Invocation events.
     *
     * @param  event  The event to dispatch
     */
    private void delegateInvocationEvent(InvocationEvent event)
    {
        InvocationListener [] listeners;
        
        synchronized (invocationListeners) {
            listeners = (InvocationListener []) invocationListeners.toArray(new InvocationListener[invocationListeners.size()]);
        }
        
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].invocationFinished(event);
        }
    }

    /**
     * Dispatch a class event to the appropriate listeners.
     * 
     * @param event  The event to dispatch
     */
    private void delegateClassEvent(ClassEvent event)
    {
        // We'll make a copy of the current list to prevent
        // ConcurrentModification problems.
        ClassListener [] listeners;
        
        synchronized (classListeners) {
            listeners = (ClassListener []) classListeners.toArray(new ClassListener[classListeners.size()]);
        }
        
        for (int i = 0; i < listeners.length; i++) {
            if (event.getEventId() == ClassEvent.REMOVED) {
                if (listeners[i] instanceof ClassListener2) {
                    ((ClassListener2) listeners[i]).classRemoved(event);
                }
            } else {
                listeners[i].classStateChanged(event);
            }
        }
    }

    /**
     * Dispatch this event to the listeners for the Dependency events.
     * 
     * @param event
     *            The event to dispatch
     */
    private void delegateDependencyEvent(DependencyEvent event)
    {
        List<DependencyListener> listeners = new ArrayList<DependencyListener>();
        synchronized (dependencyListeners) {
            listeners.addAll(dependencyListeners);
        }
        
        for (DependencyListener dependencyListener : listeners) {
            switch (event.getEventType()) {
                case DEPENDENCY_ADDED:
                    dependencyListener.dependencyAdded(event);
                    break;
                case DEPENDENCY_REMOVED:
                    dependencyListener.dependencyRemoved(event);
                    break;
                case DEPENDENCY_HIDDEN:
                case DEPENDENCY_SHOWN:
                    dependencyListener.dependencyVisibilityChanged(event);
                    break;
            }
        }
    }
    
    /**
     * Dispatch this event to the listeners for the class target events.
     * 
     * @param event
     *            The event to dispatch
     */
    private void delegateClassTargetEvent(ClassTargetEvent event)
    {
        List<ClassTargetListener> listeners = new ArrayList<ClassTargetListener>();
        synchronized (classTargetListeners) {
            listeners.addAll(classTargetListeners);
        }
        
        for (ClassTargetListener classTargetListener : listeners) {
            classTargetListener.classTargetVisibilityChanged(event);
        }
    }

    /**
     * Informs any registered listeners that an event has occurred.
     * This will call the various dispatcher as needed.
     * Errors will be trapped by the caller.
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
        else if (event instanceof InvocationEvent)
            delegateInvocationEvent((InvocationEvent) event);
        else if (event instanceof ClassEvent)
            delegateClassEvent((ClassEvent) event);
        else if (event instanceof DependencyEvent) {
            delegateDependencyEvent((DependencyEvent) event);
        } else if (event instanceof ClassTargetEvent) {
            delegateClassTargetEvent((ClassTargetEvent) event);
        }
    }



    /**
     * Calls the extension to get the right menu item.
     * This is already wrapped for errors in the caller.
     * It is right for it to create a new wrapped object each time.
     * We do not want extensions to share objects.
     * It is here since it can access all constructors directly.
     *
     * @param  attachedObject  Description of the Parameter
     * @return                 The menuItem value
     */
    JMenuItem getMenuItem(ExtensionMenu attachedObject)
    {
        if ((currentMenuGen == null) || (attachedObject == null)) {
            return null;
        }

        return attachedObject.getMenuItem(currentMenuGen);
    }


    /**
     * Post a notification of a menu going to be displayed
     */
    void postMenuItem(ExtensionMenu attachedObject, JMenuItem onThisItem)
    {
        if ((currentMenuGen != null) && (attachedObject != null)) {
            attachedObject.postMenuItem(currentMenuGen, onThisItem);
        }
    }

    /**
     * Calls the extension to draw its representation of a class target.
     * 
     * @param layer
     *            The layer of the drawing which causes the different methods of
     *            the {@link ExtensionClassTargetPainter} instance to be called.
     * @param bClassTarget
     *            The {@link BClassTarget} which represents the class target
     *            that will be painted.
     * @param graphics
     *            The {@link Graphics2D} instance to draw on.
     * @param width
     *            The width of the area to paint.
     * @param height
     *            The height of the area to paint.
     */
    void drawExtensionClassTarget(Layer layer, BClassTarget bClassTarget, Graphics2D graphics,
            int width, int height)
    {
        if (currentClassTargetPainter != null) {
            switch (layer) {
                case BACKGROUND :
                    currentClassTargetPainter.drawClassTargetBackground(bClassTarget, graphics,
                        width, height);
                    break;
                case FOREGROUND :
                    currentClassTargetPainter.drawClassTargetForeground(bClassTarget, graphics,
                        width, height);
                    break;
            }
        }
    }
}
