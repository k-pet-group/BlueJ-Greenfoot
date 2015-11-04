/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.operations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import bluej.stride.slots.EditableSlot.MenuItemOrder;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CustomMenuItem;

import bluej.stride.generic.Frame;
import bluej.stride.generic.InteractionManager;

public class EnableFrameOperation extends FrameOperation
{
    public EnableFrameOperation(InteractionManager editor)
    {
        super(editor, "ENABLE", Combine.ANY);
    }

    @Override
    public void enablePreview()
    {
        editor.getSelection().getSelected().forEach(frame -> frame.setFrameEnablePreview(Optional.of(true)));
    }

    @Override
    public void disablePreview()
    {
        editor.getSelection().getSelected().forEach(frame -> frame.setFrameEnablePreview(Optional.empty()));
    }

    @Override
    protected void execute(List<Frame> frames)
    {
        frames.forEach(frame -> frame.setFrameEnabled(true));
    }

    @Override
    public List<ItemLabel> getLabels()
    {
        return Arrays.asList(l("Enable", MenuItemOrder.ENABLE_FRAME));
    }

    @Override
    public void onMenuShowing(CustomMenuItem item)
    {
        // We grey out enable if not all frames can be enabled:
        boolean canAllBeEnabled = editor.getSelection().getSelected().stream().allMatch(f -> f.canHaveEnabledState(true));
        item.setDisable(!canAllBeEnabled);
    }

    @Override
    public boolean onlyOnContextMenu()
    {
        return true;
    }
}
