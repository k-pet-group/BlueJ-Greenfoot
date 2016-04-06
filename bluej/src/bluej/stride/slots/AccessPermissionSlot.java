/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.slots;

import bluej.stride.framedjava.ast.AccessPermission;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by neil on 22/02/2015.
 */
public class AccessPermissionSlot extends ChoiceSlot<AccessPermission>
{
    private static Map<AccessPermission, String> hints;

    public AccessPermissionSlot(InteractionManager editor, Frame parentFrame, FrameContentRow row, String stylePrefix)
    {
        super(editor, parentFrame, row, AccessPermission.all(), AccessPermission::isValid, stylePrefix, getHints());
    }

    private static Map<AccessPermission, String> getHints()
    {
        if (hints == null)
        {
            hints = new HashMap<>();
            hints.put(AccessPermission.PRIVATE, "Accessible only from this class");
            hints.put(AccessPermission.PROTECTED, "Accessible from this class and subclasses");
            hints.put(AccessPermission.PUBLIC, "Accessible from all classes");
        }
        return hints;
    }

}
