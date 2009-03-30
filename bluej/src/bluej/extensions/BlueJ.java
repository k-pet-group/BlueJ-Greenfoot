/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
import bluej.debugmgr.objectbench.*;
import bluej.extensions.event.*;
import bluej.extmgr.*;
import bluej.pkgmgr.*;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.target.*;
import java.awt.*;
import java.io.*;
import java.util.*;
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
 * @version    $Id: BlueJ.java 6215 2009-03-30 13:28:25Z polle $
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
    private Properties localLabels;

    private ArrayList eventListeners;
    // This is the queue for the whole of them
    private ArrayList applicationListeners;
    private ArrayList packageListeners;
    private ArrayList compileListeners;
    private ArrayList invocationListeners;
    private ArrayList classListeners;


    /**
     * Constructor for a BlueJ proxy object.
     * See the ExtensionBridge class
     *
     * @param  aWrapper      Description of the Parameter
     * @param  aPrefManager  Description of the Parameter
     */
    BlueJ(ExtensionWrapper aWrapper, ExtensionPrefManager aPrefManager)
    {
        myWrapper = aWrapper;
        prefManager = aPrefManager;

        eventListeners = new ArrayList();
        applicationListeners = new ArrayList();
        packageListeners = new ArrayList();
        compileListeners = new ArrayList();
        invocationListeners = new ArrayList();
        classListeners = new ArrayList();

        /* I do NOT want lazy initialization otherwise I may try to load it
         * may times just because I cannof find anything.
         * Or having state variables to know I I did load it but had nothing found
         */
        localLabels = myWrapper.getLabelProperties();
    }



    /**
     * Opens a project.
     *
     *
     * @param  directory  Where the project is stored.
     * @return            the BProject that describes the newly opened project or null if it cannot be opened.
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

        Collection projects = Project.getProjects();
        BProject[] result = new BProject[projects.size()];

        Iterator iter; int index;
        for (iter = projects.iterator(), index = 0; iter.hasNext(); index++) {
            Project prj = (Project) iter.next();
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
     * @param  value     the required value of that property.
     */
    public void setExtensionPropertyString(String property, String value)
    {
        if (!myWrapper.isValid())
            throw new ExtensionUnloadedException();

        String thisKey = myWrapper.getSettingsString(property);
        Config.putPropString(thisKey, value);
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
            listeners[i].classStateChanged(event);
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
    JMenuItem getMenuItem(Object attachedObject)
    {
        if (currentMenuGen == null)
            return null;

        if (attachedObject == null) {
            JMenuItem anItem = currentMenuGen.getToolsMenuItem(null);
            if ( anItem != null ) 
                return anItem;

            // Try to use the old deprecated method.
            return currentMenuGen.getMenuItem();
        }

        if (attachedObject instanceof Package) {
            Package attachedPkg = (Package) attachedObject;
            Identifier anId = new Identifier(attachedPkg.getProject(), attachedPkg);
            return currentMenuGen.getToolsMenuItem(new BPackage(anId));
        }

        if (attachedObject instanceof ClassTarget) {
            ClassTarget aTarget = (ClassTarget) attachedObject;
            String qualifiedClassName = aTarget.getQualifiedName();
            Package attachedPkg = aTarget.getPackage();
            Identifier anId = new Identifier(attachedPkg.getProject(), attachedPkg, qualifiedClassName);
            return currentMenuGen.getClassMenuItem(new BClass(anId));
        }

        if (attachedObject instanceof ObjectWrapper) {
            ObjectWrapper aWrapper = (ObjectWrapper) attachedObject;
            return currentMenuGen.getObjectMenuItem(new BObject(aWrapper));
        }

        return null;
    }


    /**
     * Post a notification of a menu going to be display
     */
    void postMenuItem(Object attachedObject, JMenuItem onThisItem )
    {
        if (currentMenuGen == null)
            return;

        if (attachedObject == null) {
            // Only BPackages can be null when a menu is invoked
            currentMenuGen.notifyPostToolsMenu(null,onThisItem);
            return;
            }

        if (attachedObject instanceof Package) {
            Package attachedPkg = (Package) attachedObject;
            Identifier anId = new Identifier(attachedPkg.getProject(), attachedPkg);
            currentMenuGen.notifyPostToolsMenu(new BPackage(anId),onThisItem);
        }

        if (attachedObject instanceof ClassTarget) {
            ClassTarget aTarget = (ClassTarget) attachedObject;
            String qualifiedClassName = aTarget.getQualifiedName();
            Package attachedPkg = aTarget.getPackage();
            Identifier anId = new Identifier(attachedPkg.getProject(), attachedPkg, qualifiedClassName);
            currentMenuGen.notifyPostClassMenu(new BClass(anId),onThisItem);
        }

        if (attachedObject instanceof ObjectWrapper) {
            ObjectWrapper aWrapper = (ObjectWrapper) attachedObject;
            currentMenuGen.notifyPostObjectMenu(new BObject(aWrapper),onThisItem);
        }
    }




}
