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

import greenfoot.actions.SelectImageAction;
import greenfoot.actions.ShowApiDocAction;
import greenfoot.core.GProject;
import greenfoot.event.WorldEvent;
import greenfoot.record.InteractionListener;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPopupMenu;

import bluej.Config;

/**
 * A role for Actor classes 
 * 
 * @author Poul Henriksen
 */
public class ActorClassRole extends ImageClassRole
{
    protected final Color envOpColour = new Color(152,32,32);

    private String template = "actorclass.tmpl";

    private static final String newline = System.getProperty("line.separator");
    public static final String imports = "import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)" + newline;
    
    private List<Action> constructorItems = new ArrayList<Action>();
    private boolean enableConstructors = false;
    
    public ActorClassRole(GProject project)
    {
        super(project);
    }
    
    /*
     * Need to overide this method in order to delay the invocation of the
     * constructor until the object is placed into the world.
     */
    @Override
    public List<Action> createConstructorActions(Class<?> realClass, GProject project,
            InteractionListener interactionListener)
    {
        List<Action> realActions = super.createConstructorActions(realClass, project, interactionListener);
        constructorItems = new ArrayList<Action>();
        for (Action realAction : realActions) {
            Action tempAction = createDragProxyAction(realAction);
            tempAction.setEnabled(enableConstructors);
            constructorItems.add(tempAction);
        }
 
        return constructorItems;
    }
        
    @Override
    public void addPopupMenuItems(JPopupMenu menu, boolean coreClass)
    {
        if (!coreClass) {
            menu.add(createMenuItem(new SelectImageAction(classView, this)));
        }
        else {
            menu.add(createMenuItem(new ShowApiDocAction(Config.getString("show.apidoc"), "greenfoot/Actor.html")));
        }
    }

    @Override
    public String getTemplateFileName()
    {
        return template;
    }

    @Override
    public void worldCreated(WorldEvent e) {
        enableConstructors = true;
        for (Action action : constructorItems) {
            action.setEnabled(true);
        }
    }

    @Override
    public void worldRemoved(WorldEvent e) {
        enableConstructors = false;
        for (Action action : constructorItems) {
            action.setEnabled(false);
        }
    }   
}
