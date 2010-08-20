/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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

import java.awt.Color;
import java.awt.Paint;

import javax.swing.JPopupMenu;
import bluej.Config;
import bluej.pkgmgr.target.ClassTarget;
import bluej.prefmgr.PrefMgr;

/**
 * A role object to represent the behaviour of enums.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 */
public class EnumClassRole extends ClassRole
{
    public final static String ENUM_ROLE_NAME = "EnumTarget";
    private static final Color enumbg = Config.getOptionalItemColour("colour.class.bg.enum");
    
    /**
     * Create the enum class role.
     */
    public EnumClassRole()
    {
    }

    public String getRoleName()
    {
        return ENUM_ROLE_NAME;
    }

    public String getStereotypeLabel()
    {
        return "enum";
    }

    /**
     * Return the intended background colour for this type of target.
     */
    public Paint getBackgroundPaint(int width, int height)
    {
        if (enumbg != null) {
            return enumbg;
        } else {
            return super.getBackgroundPaint(width, height);
        }
    }

    /**
     * Creates a class menu containing any constructors.
     *
     * Because we are an enum class we cannot have any constructors
     * so we override this method to do nothing.
     *
     * @param menu the popup menu to add the class menu items to
     * @param cl Class object associated with this class target
     */
    public boolean createClassConstructorMenu(JPopupMenu menu, ClassTarget ct, Class<?> cl)
    {
        return false;
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
    public boolean createRoleMenuEnd(JPopupMenu menu, ClassTarget ct, int state)
    {
        if(PrefMgr.getFlag(PrefMgr.SHOW_TEST_TOOLS)) {
            if (ct.getAssociation() == null) {
                menu.addSeparator();
                addMenuItem(menu, ct.new CreateTestAction(), true);
            }
        }
        return true;
    }
    
    
}
