/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016  Michael Kolling and John Rosenberg 
 
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

import java.awt.*;

import bluej.Config;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A role object to represent the behaviour of interfaces.
 *
 * @author  Andrew Patterson 
 */
public class InterfaceClassRole extends ClassRole
{
    public final static String INTERFACE_ROLE_NAME = "InterfaceTarget";
    private static final Color interfacebg = Config.getOptionalItemColour("colour.class.bg.interface");

    /**
     * Create the interface class role.
     */
    public InterfaceClassRole()
    {
    }

    @OnThread(Tag.Any)
    public String getRoleName()
    {
        return INTERFACE_ROLE_NAME;
    }

    @OnThread(Tag.Any)
    public String getStereotypeLabel()
    {
        return "interface";
    }

    /**
     * Return the intended background colour for this type of target.
     */
    public Paint getBackgroundPaint(int width, int height)
    {
        if (interfacebg != null) {
            return interfacebg;
        } else {
            return super.getBackgroundPaint(width, height);
        }
    }

    @Override
    @OnThread(Tag.Any)
    public boolean canConvertToStride()
    {
        return true; // interfaces are supported
    }
}
