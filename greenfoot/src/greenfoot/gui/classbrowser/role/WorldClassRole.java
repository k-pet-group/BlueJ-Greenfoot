/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011,2013  Poul Henriksen and Michael Kolling 
 
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

import javax.swing.JPopupMenu;

import bluej.Config;

/**
 * A role for world classes.
 * 
 * @author Poul Henriksen
 */
public class WorldClassRole extends ImageClassRole
{
    boolean subWorld;
    private final static String worldTemplate = "worldclass.tmpl";
    private final static String subWorldTemplate = "subworldclass.tmpl";
    
    public WorldClassRole(GProject project, boolean subWorld)
    {
        super(project);
        this.subWorld = subWorld;
    }
    
    @Override
    public String getTemplateFileName()
    {
        if (subWorld) {
            return subWorldTemplate;
        }
        return worldTemplate;
    }
    
    @Override
    public void addPopupMenuItems(JPopupMenu menu, boolean coreClass)
    {
        if (! coreClass) {
            menu.add(createMenuItem(new SelectImageAction(classView, this)));
        }
        else {
            menu.add(createMenuItem(new ShowApiDocAction(Config.getString("show.apidoc"), "greenfoot/World.html")));
        }
    }
}
