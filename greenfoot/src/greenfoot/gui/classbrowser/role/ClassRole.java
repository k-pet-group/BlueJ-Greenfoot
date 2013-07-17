/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2013  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui.classbrowser.role;

import greenfoot.actions.EditClassAction;
import greenfoot.actions.InspectClassAction;
import greenfoot.actions.NewSubclassAction;
import greenfoot.actions.RemoveClassAction;
import greenfoot.core.GClass;
import greenfoot.core.GProject;
import greenfoot.core.GreenfootMain;
import greenfoot.core.WorldHandler;
import greenfoot.core.WorldInvokeListener;
import greenfoot.event.WorldEvent;
import greenfoot.event.WorldListener;
import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.localdebugger.LocalClass;
import greenfoot.record.InteractionListener;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import bluej.Config;
import bluej.debugmgr.ConstructAction;
import bluej.debugmgr.inspector.InspectorManager;
import bluej.debugmgr.objectbench.ObjectBenchInterface;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import bluej.views.View;
import bluej.views.ViewFilter;


/**
 * A class role in Greenfoot. There are different roles for actors, worlds, and
 * "normal" classes.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public abstract class ClassRole implements WorldListener
{
    protected final static Dimension iconSize = new Dimension(16, 16);
    
    private final Color envOpColour = new Color(152,32,32);
    
    /**
     * Set the text and icon of a ClassView as appropriate for the given class
     */
    public abstract void buildUI(ClassView classView, GClass rClass);

    /**
     * Get the name for the template file used to create the initial source for a new class.
     */
    public abstract String getTemplateFileName();

    /**
     * Create a list of actions for invoking the constructors of the given class
     */
    public List<Action> createConstructorActions(Class<?> realClass, GProject project,
            InteractionListener interactionListener)
    {
        View view = View.getView(realClass);
        List<Action> actions = new ArrayList<Action>();
        ConstructorView[] constructors = view.getConstructors();

        for (int i = 0; i < constructors.length; i++) {
            try {
                ConstructorView m = constructors[constructors.length - i - 1];

                ViewFilter filter = new ViewFilter(ViewFilter.INSTANCE | ViewFilter.PUBLIC);
                if (!filter.accept(m))
                    continue;

                ObjectBenchInterface ob = WorldHandler.getInstance().getObjectBench();
                GreenfootFrame frame = GreenfootMain.getInstance().getFrame();
                InspectorManager inspectorManager = frame.getInspectorManager();
                
                WorldInvokeListener invocListener = new WorldInvokeListener(frame, realClass, ob,
                        inspectorManager, interactionListener, project);

                String prefix = "new ";
                Action callAction = new ConstructAction(m, invocListener, prefix + m.getLongDesc());
                actions.add(callAction);
            }
            catch (Exception e) {
                Debug.reportError("Exception accessing methods: " + e);
                e.printStackTrace();
            }
        }
        return actions;
    }
    
    /**
     * Create the popup menu for the given class
     */
    public JPopupMenu createPopupMenu(ClassBrowser classBrowser, ClassView classView,
            InteractionListener interactionListener, boolean isUncompiled)
    {
        GClass gClass = classView.getGClass();
        JPopupMenu popupMenu = new JPopupMenu();
        GProject project = null;
        project = gClass.getPackage().getProject();
        
        if (isUncompiled) {
            JMenuItem needsCompileItem = popupMenu.add(Config.getString("classPopup.needsCompile"));
            needsCompileItem.setEnabled(false);
            needsCompileItem.setFont(PrefMgr.getPopupMenuFont());
            popupMenu.addSeparator();
        }

        Class<?> realClass = gClass.getJavaClass();
        if (realClass != null) {

            // Constructors
            if (!java.lang.reflect.Modifier.isAbstract(realClass.getModifiers())) {
                List<Action> constructorItems = createConstructorActions(realClass, project, interactionListener);

                boolean hasEntries = false;
                for (Action callAction : constructorItems) {
                    JMenuItem item = popupMenu.add(callAction);
                    item.setFont(PrefMgr.getPopupMenuFont());
                    hasEntries = true;
                }

                if (hasEntries) {
                    popupMenu.addSeparator();
                }
            }

            // Static methods
            ViewFilter filter = new ViewFilter(ViewFilter.STATIC | ViewFilter.PUBLIC);
            View view = View.getView(realClass);
            MethodView[] allMethods = view.getAllMethods();

            ObjectBenchInterface ob = WorldHandler.getInstance().getObjectBench();
            GreenfootFrame frame = GreenfootMain.getInstance().getFrame();
            InspectorManager inspectorManager = frame.getInspectorManager();
            
            WorldInvokeListener invocListener = new WorldInvokeListener(frame, realClass, ob,
                    inspectorManager, interactionListener, project);
            if (bluej.pkgmgr.target.role.ClassRole.createMenuItems(popupMenu, allMethods, filter, 0,
                    allMethods.length, "", invocListener)) {
                popupMenu.addSeparator();
            }
        }


        if (!classView.isCoreClass()) {
            
            if (gClass.hasSourceCode()) {
                popupMenu.add(createMenuItem(new EditClassAction(classBrowser)));
            }

            addPopupMenuItems(popupMenu, false);

            if (classView.getRealClass() != null) {
                popupMenu.add(createMenuItem(new InspectClassAction(new LocalClass(classView.getRealClass()), null,
                        classBrowser.getFrame().getInspectorManager(), classBrowser.getFrame())));
            }

            popupMenu.add(createMenuItem(new RemoveClassAction(classView, classBrowser.getFrame())));
        }
        else {
            addPopupMenuItems(popupMenu, true);
        }
        
        popupMenu.addSeparator();
        popupMenu.add(createMenuItem(new NewSubclassAction(classView, classBrowser, interactionListener)));

        return popupMenu;
    }
    
    protected JMenuItem createMenuItem(Action action)
    {
        JMenuItem item = new JMenuItem(action);
        item.setFont(PrefMgr.getPopupMenuFont());
        item.setForeground(envOpColour);
        return item;
    }
    
    /**
     * Add any role-specific menu items to the given popup menu
     * @param menu  The meny to add the menu items to
     * @param coreClass  Whether the class is a "core" class (Actor, World) or not
     */
    public void addPopupMenuItems(JPopupMenu menu, boolean coreClass)
    {
        // default implementation does nothing
    }

    public void worldCreated(WorldEvent e) {
        // Do nothing - only want to handle this for actors
    }

    public void worldRemoved(WorldEvent e) {
        // Do nothing - only want to handle this for actors
    }

    /**
     * Called when this role is being removed. Do any cleanup that is needed here.
     */
    public abstract void remove();
    
}
