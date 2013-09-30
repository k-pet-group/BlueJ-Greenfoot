/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2012,2013  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui.classbrowser;

import greenfoot.core.GClass;
import greenfoot.core.GPackage;
import greenfoot.core.GProject;
import greenfoot.core.GreenfootMain;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.event.ActorInstantiationListener;
import greenfoot.gui.classbrowser.role.ActorClassRole;
import greenfoot.gui.classbrowser.role.ClassRole;
import greenfoot.gui.classbrowser.role.NormalClassRole;
import greenfoot.gui.classbrowser.role.WorldClassRole;
import greenfoot.gui.input.mouse.LocationTracker;
import greenfoot.platforms.ide.GreenfootUtilDelegateIDE;
import greenfoot.record.InteractionListener;

import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JPopupMenu;
import javax.swing.JRootPane;

import bluej.Config;
import bluej.utility.DialogManager;

/**
 * A class visualisation.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public class ClassView extends ClassButton
    implements Selectable
{
    GClass gClass;
    private ClassRole role;
    ClassBrowser classBrowser;
    private String superclass; //Holds the current superclass. Used to determine wether the superclass has changed.
    private InteractionListener interactionListener;
        
    /**
     * Creates a new ClassView with the role determined from gClass.
     */
    public ClassView(ClassBrowser classBrowser, GClass gClass, InteractionListener interactionListener)
    {
        this.classBrowser = classBrowser;
        this.interactionListener = interactionListener;
        init(gClass);
    }    
    
    /**
     * Check whether this class is a core class (can't be removed or have
     * image set).
     */
    public boolean isCoreClass()
    {
        return gClass.isActorClass() || gClass.isWorldClass();
    }
    
    /**
     * Updates this ClassView to reflect a change in super class.
     * 
     * <p>Will also update the UI, but can be called from any thread.
     */
    public void updateSuperClass()
    {
        String superClassGuess = getSuperclass();
        if (superClassGuess == null || superClassGuess.equals(superclass)) {
            // If super class has not changed, we do not want to update
            // anything.
            return;
        }
        else {
            superclass = getSuperclass();
        }

        if (classBrowser != null) {
            // If we are in a classBrowser, tell it to update the location
            // of this classview in the tree.
            classBrowser.consolidateLayout(ClassView.this);
        }
    }

    /**
     * Determines the role of this class based on the backing GClass.
     */
    private ClassRole determineRole(GProject project)
    {
        ClassRole classRole = null;
        if (gClass.isActorClass()) {
            classRole = new ActorClassRole(project);
        }
        else if (gClass.isWorldClass()) {
            classRole = new WorldClassRole(project, false);
        }
        else if (gClass.isActorSubclass()) {
            classRole = new ActorClassRole(project);
        }
        else if (gClass.isWorldSubclass()) {
            classRole = new WorldClassRole(project, true);
        }
        else {
            // everything else
            classRole = NormalClassRole.getInstance();
        }
        return classRole;
    }

    public void setSuperclass(String superclass)
    {
        this.superclass = superclass;
    }

    private void init(GClass gClass)
    {
        this.gClass = gClass;
        gClass.setClassView(this);
        
        superclass = getSuperclass();
        
        initUI();
        
        update();
    }

        
    /**
     * Return the real Java class that this class view represents.
     */
    public Class<?> getRealClass()
    {
        return gClass.getJavaClass();
    }

    public GClass getGClass()
    {
        return gClass;
    }

    private JPopupMenu getPopupMenu()
    {
        JPopupMenu popupMenu = role.createPopupMenu(classBrowser, this, interactionListener, isUncompiled());
        popupMenu.setInvoker(this);
        return popupMenu;
    }

    /**
     * Sets the role of this ClassLabel. Does not update UI.
     * 
     * @param role
     */
    private void setRole(ClassRole role)
    {
        WorldHandler.getInstance().removeWorldListener(this.role);
        this.role = role;
        WorldHandler.getInstance().addWorldListener(this.role);
    }

    /**
     * Rebuild the UI of this ClassView from scratch.
     */
    private void update()
    {
        clearUI();
        setRole(determineRole(classBrowser.getProject()));
        role.buildUI(this, gClass);
        
        updateSuperClass();
    }

    /**
     * Get the class role for this class view.  
     */
    public ClassRole getRole()
    {
        return role;
    }

    public String getQualifiedClassName()
    {
        return gClass.getQualifiedName();
    }

    /**
     * Get the (non-qualified) name  of the class represented by this ClassView.
     */
    public String getClassName()
    {
        return gClass.getName();
    }

    /*
     * @see greenfoot.gui.classbrowser.Selectable#select()
     */
    public void select()
    {
        this.setSelected(true);
        fireSelectionChangeEvent();
    }

    /*
     * @see greenfoot.gui.classbrowser.Selectable#deselect()
     */
    public boolean deselect()
    {
        if (isSelected()) {
            setSelected(false);
            fireSelectionChangeEvent();
            return true;
        }
        return false;
    }

    /**
     * Creates an instance of this class. The default constructor is used. This
     * method is used for creating instances when clicking on the world.
     */
    public void createInstance()
    {
        Simulation.getInstance().runLater(new Runnable() {
            @Override
            public void run()
            {
                Class<?> realClass = getRealClass();
                try {
                    if (realClass == null) {
                        return;
                    }
                    
                    Constructor<?> constructor = realClass.getConstructor(new Class[]{});
                    constructor.setAccessible(true);

                    Object newObject = Simulation.newInstance(constructor);
                    interactionListener.createdActor(newObject, new String[0], null);
                    ActorInstantiationListener invocationListener = GreenfootMain.getInstance().getInvocationListener();
                    if(invocationListener != null) {
                        invocationListener.localObjectCreated(newObject, LocationTracker.instance().getMouseButtonEvent());
                    }
                    return;
                }
                catch (LinkageError le) {
                    // This could be NoClassDefFound or similar. It really means the
                    // class needs to be recompiled.
                    le.printStackTrace();
                }
                catch (NoSuchMethodException e) {
                    // This might happen if there is no default constructor
                    // e.printStackTrace();
                    // TODO prevent this by checking for the constructor beforehand (before
                    // this method is called)
                }
                catch (IllegalArgumentException e) {
                    // Shouldn't happen - we pass the correct arguments always
                }
                catch (InstantiationException e) {
                    e.printStackTrace();
                }
                catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause != null) {
                        // Filter the stack trace. Take it from the first point
                        // at which code from this class was being executed.
                        StackTraceElement [] strace = cause.getStackTrace();
                        for (int i = strace.length; i > 0; i--) {
                            if (strace[i-1].getClassName().equals(realClass.getName())) {
                                StackTraceElement [] newStrace = new StackTraceElement[i];
                                System.arraycopy(strace, 0, newStrace, 0, i);
                                cause.setStackTrace(newStrace);
                                break;
                            }
                        }
                        cause.printStackTrace();
                    }
                    else {
                        e.printStackTrace();
                    }
                }

                return;
            }
        });
    }

    public GClass createSubclass(String className)
    {
        try {
            //get the default package which is the one containing the user
            // code.
            GProject proj = gClass.getPackage().getProject();
            GPackage pkg = proj.getDefaultPackage();
            //write the java file as this is required to exist
            File dir = proj.getDir();
            File newJavaFile = new File(dir, className + ".java");
            String superClassName = getClassName();            
            GreenfootUtilDelegateIDE.getInstance().createSkeleton(className, superClassName, newJavaFile,
                    role.getTemplateFileName(), proj.getCharsetName());
            
            GClass newClass = pkg.newClass(className, false);
            //We know what the superclass should be, so we set it.
            newClass.setSuperclassGuess(this.getQualifiedClassName());
            return newClass;
        }
        catch (IOException ioe) {
            DialogManager.showErrorText(this,
                    Config.getString("greenfoot.cannotCreateClass") +
                    ": " + ioe.getLocalizedMessage());
        }
        return null;
    }
        
    /**
     * Removes this class.
     * <br>
     * Must be called from the event thread.
     */
    public void remove()
    {
        WorldHandler.getInstance().removeWorldListener(this.role);
        role.remove();
        classBrowser.removeClass(this);
        gClass.remove();
        gClass = null;
    }

    /**
     * Get the super class for this class.
     * 
     */
    public String getSuperclass()
    {
        return gClass.getSuperclassGuess();
    }


    protected void fireSelectionChangeEvent()
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == SelectionListener.class) {
                ((SelectionListener) listeners[i + 1]).selectionChange(this);
            }
        }
    }

    /**
     * Add a changeListener to listen for changes.
     * 
     * @param l
     *            Listener to add
     */
    public void addSelectionChangeListener(SelectionListener l)
    {
        listenerList.add(SelectionListener.class, l);
    }

    /**
     * Remove a changeListener.
     * 
     * @param l
     *            Listener to remove
     */
    public void removeSelectionChangeListener(SelectionListener l)
    {
        listenerList.remove(SelectionListener.class, l);
    }


    /**
     * Notify the class view that the underlying class has changed name.
     * @param oldName  The original name of the class
     */
    public void nameChanged(String oldName)
    {
        classBrowser.renameClass(this, oldName);
        updateView();
    }
    
    /**
     * Rebuild the UI of this ClassView from scratch.
     */
    public void updateView()
    {
        update();

        JRootPane rootPane = getRootPane();
        if(rootPane != null) {
            getRootPane().revalidate();
        }
    }

    
    protected boolean isValidClass()
    {
        return gClass != null;
    }
    
    protected boolean isUncompiled()
    {
        return !gClass.isCompiled();
    }
    
    protected void doubleClick()
    {
        gClass.edit();
    }

    protected void maybeShowPopup(MouseEvent e)
    {
        if (e.isPopupTrigger()) {
            getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
        }
    }
}
