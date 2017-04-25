/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2016  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr.target.role;

import javax.swing.JPopupMenu;

import bluej.pkgmgr.target.DependentTarget.State;
import javafx.collections.ObservableList;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import bluej.pkgmgr.target.ClassTarget;
import bluej.prefmgr.PrefMgr;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A role object which a class target uses to delegate behaviour to.
 * StdClassRole is used to represent standard Java classes.
 *
 * @author Bruce Quig
 */
public class StdClassRole extends ClassRole
{
    /**
     * Create the class role.
     */
    public StdClassRole()
    {
    }

    @OnThread(Tag.Any)
    public String getRoleName()
    {
        return "ClassTarget";
    }
    
    /**
     * Adds role specific items at the bottom of the popup menu for this class target.
     *
     * @param menu the menu object to add to
     * @param ct ClassTarget object associated with this class role
     * @param state the state of the ClassTarget
     *
     * @return true if any menu items have been added
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public boolean createRoleMenuEnd(ObservableList<MenuItem> menu, ClassTarget ct, State state)
    {
       if (ct.getAssociation() == null)
        {
                menu.add(new SeparatorMenuItem());
                menu.add(ct.new CreateTestAction());
        }
        return true;
    }

    @Override
    @OnThread(Tag.Any)
    public boolean canConvertToStride()
    {
        return true;
    }
}
