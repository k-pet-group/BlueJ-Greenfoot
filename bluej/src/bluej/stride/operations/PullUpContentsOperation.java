/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015 Michael Kölling and John Rosenberg
 
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

import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCursor;
import bluej.stride.generic.InteractionManager;
import bluej.stride.slots.EditableSlot.MenuItemOrder;
import javafx.beans.value.ObservableValue;

import java.util.Arrays;
import java.util.List;

/**
 * Put the contents of canvas frames in place of these frames.
 * @author Amjad Altadmri
 */
public class PullUpContentsOperation extends FrameOperation
{

    public PullUpContentsOperation(InteractionManager editor)
    {
        super(editor, "PULL_CONTENTS", Combine.ALL);
    }

    @Override
    public List<ItemLabel> getLabels()
    {
        return Arrays.asList(l("Delete outer frame", MenuItemOrder.DELETE));
    }

    @Override
    public void enablePreview()
    {
        editor.getSelection().setPullUpPreview(true);
    }

    @Override
    public void disablePreview()
    {
        editor.getSelection().setPullUpPreview(false);
    }

    @Override
    protected void execute(List<Frame> frames)
    {
        if (!frames.isEmpty()) {
            FrameCursor cursorBefore = frames.get(0).getCursorBefore();
            frames.forEach(frame -> {
                frame.pullUpContents();
                frame.getParentCanvas().removeBlock(frame);
            });
            cursorBefore.requestFocus();
            //??
            //editor.getSelection().clear();
        }
    }

    @Override
    public boolean onlyOnContextMenu()
    {
        return true;
    }
}