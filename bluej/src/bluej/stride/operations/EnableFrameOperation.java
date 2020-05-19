/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2020 Michael Kölling and John Rosenberg
 
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
import javafx.scene.control.CustomMenuItem;

import bluej.Config;
import bluej.stride.generic.Frame;
import bluej.stride.generic.InteractionManager;

import threadchecker.OnThread;
import threadchecker.Tag;

public class EnableFrameOperation extends FrameOperation
{
    public EnableFrameOperation(InteractionManager editor)
    {
        super(editor, "ENABLE", Combine.ANY);
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void enablePreview()
    {
        editor.getSelection().getSelected().forEach(frame -> frame.setFrameEnablePreview(Frame.FramePreviewEnabled.PREVIEW_ENABLED));
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void disablePreview()
    {
        editor.getSelection().getSelected().forEach(frame -> frame.setFrameEnablePreview(Frame.FramePreviewEnabled.PREVIEW_NONE));
    }

    @Override
    @OnThread(Tag.FXPlatform)
    protected void execute(List<Frame> frames)
    {
        frames.forEach(frame -> frame.setFrameEnabled(true));
    }

    @Override
    public List<ItemLabel> getLabels()
    {
        return Arrays.asList(l(Config.getString("frame.operation.enable"), MenuItemOrder.ENABLE_FRAME));
    }

    @Override
    @OnThread(Tag.FXPlatform)
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
