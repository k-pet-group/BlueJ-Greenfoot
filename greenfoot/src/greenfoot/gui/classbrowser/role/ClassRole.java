/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010,2011,2013,2014,2015,2016,2017  Poul Henriksen and Michael Kolling
 
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

import bluej.views.ViewFilter.StaticOrInstance;
import greenfoot.actions.ConvertToJavaClassAction;
import greenfoot.actions.ConvertToStrideClassAction;
import greenfoot.actions.DuplicateClassAction;
import greenfoot.actions.EditClassAction;
import greenfoot.actions.NewSubActorAction;
import greenfoot.actions.NewSubWorldAction;
import greenfoot.actions.NewSubclassAction;
import greenfoot.actions.RemoveClassAction;
import greenfoot.core.GClass;
import greenfoot.core.GProject;
import greenfoot.core.GreenfootMain;
import greenfoot.core.WorldHandler;
import greenfoot.event.WorldEvent;
import greenfoot.event.WorldListener;
import greenfoot.gui.GreenfootFrame;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassView;

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
import bluej.extensions.SourceType;
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
    
    protected final String templateExtension = ".tmpl";
    
    /**
     * Set the text and icon of a ClassView as appropriate for the given class
     */
    public abstract void buildUI(ClassView classView, GClass rClass);

    /**
     * Get the name for the template file used to create the initial source for a new class.
     */
    public abstract String getTemplateFileName(boolean useInterface, SourceType language);
    
    /**
     * Create the popup menu for the given class
     */
    public JPopupMenu createPopupMenu(ClassBrowser classBrowser, ClassView classView,
            boolean isUncompiled, boolean hasKnownError)
    {
        GClass gClass = classView.getGClass();
        JPopupMenu popupMenu = new JPopupMenu();
        GProject project = null;
        project = gClass.getPackage().getProject();
        
        if (isUncompiled) {
            JMenuItem needsCompileItem = popupMenu.add(Config.getString(hasKnownError ? "classPopup.containsError" : "classPopup.needsCompile"));
            needsCompileItem.setEnabled(false);
            needsCompileItem.setFont(PrefMgr.getPopupMenuFont());
            popupMenu.addSeparator();
        }

        Class<?> realClass = gClass.getJavaClass();
        if (realClass != null) {

            // Constructors
            if (!java.lang.reflect.Modifier.isAbstract(realClass.getModifiers())) {
                List<Action> constructorItems = new ArrayList<>();

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
            ViewFilter filter = new ViewFilter(StaticOrInstance.STATIC, "");
            View view = View.getView(realClass);
            MethodView[] allMethods = view.getAllMethods();

            ObjectBenchInterface ob = WorldHandler.getInstance().getObjectBench();
            GreenfootFrame frame = GreenfootMain.getInstance().getFrame();
            
        }


        if (!classView.isCoreClass()) {
            
            SourceType srcType = gClass.getSourceType();
            if (srcType != null && srcType != SourceType.NONE ) {
                popupMenu.add(createMenuItem(new EditClassAction(classBrowser)));
            }

            addPopupMenuItems(popupMenu, false);

            popupMenu.addSeparator();
            popupMenu.add(createMenuItem(new DuplicateClassAction(classView, classBrowser)));
            
            if (srcType != null)
            {
                if (srcType == SourceType.Stride )
                    popupMenu.add(createMenuItem(new ConvertToJavaClassAction(classView, classBrowser.getFrame())));
                else if (srcType == SourceType.Java )
                    popupMenu.add(createMenuItem(new ConvertToStrideClassAction(classView, classBrowser.getFrame())));
            }

            popupMenu.add(createMenuItem(new RemoveClassAction(classView, classBrowser.getFrame())));
            
        }
        else {
            addPopupMenuItems(popupMenu, true);
        }
        
        NewSubclassAction action;
        if (gClass.isActorClass()) {
            action = new NewSubActorAction(classBrowser.getFrame(), false);
        }
        else if (gClass.isWorldClass()) {
            // The sourceType is null as it will be decided internally based on the other files in the scenario.
            action = new NewSubWorldAction(classBrowser.getFrame(), false, null);
        }
        else {
            action = new NewSubclassAction(classView, classBrowser);
        }
        popupMenu.add(createMenuItem(action));

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

    @Override
    public void worldCreated(WorldEvent e) {
        // Do nothing - only want to handle this for actors
    }

    @Override
    public void worldRemoved(WorldEvent e) {
        // Do nothing - only want to handle this for actors
    }

    /**
     * Called when this role is being removed. Do any cleanup that is needed here.
     */
    public abstract void remove();
    
}
