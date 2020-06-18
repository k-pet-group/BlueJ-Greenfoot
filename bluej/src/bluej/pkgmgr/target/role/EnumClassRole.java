/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010,2016,2017,2020  Michael Kolling and John Rosenberg
 
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

import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.DependentTarget.State;
import bluej.pkgmgr.target.actions.ClassTargetOperation;
import bluej.pkgmgr.target.actions.CreateTestAction;
import bluej.prefmgr.PrefMgr;
import javafx.collections.ObservableList;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.List;

/**
 * A role object to represent the behaviour of enums.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public class EnumClassRole extends ClassRole
{
    public final static String ENUM_ROLE_NAME = "EnumTarget";
    
    /**
     * Create the enum class role.
     */
    public EnumClassRole()
    {
    }

    @OnThread(Tag.Any)
    public String getRoleName()
    {
        return ENUM_ROLE_NAME;
    }

    @OnThread(Tag.Any)
    public String getStereotypeLabel()
    {
        return "enum";
    }

    /**
     * Creates a class menu containing any constructors.
     *
     * Because we are an enum class we cannot have any constructors
     * so we override this method to do nothing.
     *
     * @param cl Class object associated with this class target
     */
    @Override
    public @OnThread(Tag.FXPlatform) List<ClassTargetOperation> getClassConstructorOperations(ClassTarget ct, Class<?> cl)
    {
        return List.of();
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
    public List<ClassTargetOperation> getRoleOperationsEnd(ClassTarget ct, State state)
    {
        if (ct.getAssociation() == null)
        {
            return List.of(new CreateTestAction());
        }
        return List.of();
    }

    @Override
    @OnThread(Tag.Any)
    public boolean canConvertToStride()
    {
        return false; //enums are not supported
    }
}
