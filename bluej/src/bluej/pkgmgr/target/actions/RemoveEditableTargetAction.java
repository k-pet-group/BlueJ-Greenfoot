/*
 This file is part of the BlueJ program. 
 Copyright (C) 2020,2021 Michael Kölling and John Rosenberg 
 
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
import bluej.pkgmgr.target.EditableTarget;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.List;

/**
 * Action to remove a target
 */
@OnThread(Tag.FXPlatform)
public class RemoveEditableTargetAction extends EditableTargetOperation
{
    public RemoveEditableTargetAction()
    {
        super("removeEditable", Combine.ANY, null, EditableTarget.removeStr, MenuItemOrder.EDIT, EditableTarget.MENU_STYLE_INBUILT);
    }

    @Override
    protected void executeEditable(EditableTarget target)
    {
        target.remove();
    }

    @Override
    protected boolean confirm(List<EditableTarget> editableTargets)
    {
        if (!editableTargets.isEmpty())
        {
            PkgMgrFrame pmf = PkgMgrFrame.findFrame(editableTargets.get(0).getPackage());
            if (pmf != null && pmf.askRemoveFiles())
                return true;
        }
        return false;
    }
}
