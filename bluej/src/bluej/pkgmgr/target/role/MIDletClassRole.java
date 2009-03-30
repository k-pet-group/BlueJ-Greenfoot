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
package bluej.pkgmgr.target.role;

import java.awt.Color;
import java.util.Properties;
import bluej.Config;

/**
 * A MIDlet class role in a package, i.e. a target that is a MIDlet class file
 * built from Java source code.
 *
 * @author Cecilia Vargas
 * @version $Id: AppletClassRole.java 4746 2006-12-07 02:26:53Z davmac $
 */
public class MIDletClassRole extends ClassRole
{
    public static final String MIDLET_ROLE_NAME = "MIDletTarget";
    
    private static final Color bckgrndColor = Config.getItemColour("colour.class.bg.midlet");

    
    public MIDletClassRole()  { }

    public String getRoleName()
    {
        return MIDLET_ROLE_NAME;
    }

    public String getStereotypeLabel()
    {
        return "MIDlet";
    }
    
    public Color getBackgroundColour()
    {
        return bckgrndColor;
    }
 }
