/*
 This file is part of the BlueJ program. 
 Copyright (C) 2020 Michael Kölling and John Rosenberg 
 
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
package bluej.pkgmgr.target.actions;

import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.EditableTarget;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Action to remove a classtarget from its package
 */
@OnThread(Tag.FXPlatform)
public class RemoveClassAction extends ClassTargetOperation
{
    public RemoveClassAction()
    {
        super("removeClass", Combine.ALL, null, EditableTarget.removeStr, MenuItemOrder.REMOVE, EditableTarget.MENU_STYLE_INBUILT);
    }

    @OnThread(Tag.FXPlatform)
    protected void execute(ClassTarget target)
    {
        PkgMgrFrame pmf = PkgMgrFrame.findFrame(target.getPackage());
        if (pmf.askRemoveClass())
        {
            target.remove();
        }
    }
}
