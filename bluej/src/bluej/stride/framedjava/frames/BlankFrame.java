/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
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
package bluej.stride.framedjava.frames;

import java.util.List;

import bluej.stride.framedjava.elements.BlankElement;
import bluej.stride.generic.CanvasParent;
import bluej.stride.generic.DefaultFrameFactory;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameFactory;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SingleLineFrame;
import bluej.stride.operations.FrameOperation;

public class BlankFrame extends SingleLineFrame implements CodeFrame<BlankElement>
{
    // Never changes (can't even be disabled/enabled):
    private final BlankElement blankElement = new BlankElement();
    
    public BlankFrame(InteractionManager editor)
    {
        super(editor, "", "blank-");
    }

    @Override
    public void regenerateCode()
    {
    }

    @Override
    public BlankElement getCode()
    {
        return blankElement;
    }

    @Override
    public void setElementEnabled(boolean enabled)
    {
        // Ignore
    }

    @Override
    public boolean canHaveEnabledState(boolean enabled)
    {
        // Makes no sense to disable blank frame:
        return enabled;
    }

    public static FrameFactory<BlankFrame> getFactory()
    {
        return new DefaultFrameFactory<>(BlankFrame.class, BlankFrame::new);
    }
    
    @Override
    public void updateAppearance(FrameCanvas parentCanvas)
    {
        super.updateAppearance(parentCanvas);
        if (parentCanvas == null) {
            // When deleting the frame or remove old copy due to drag.
            return;
        }
        
        if (parentCanvas.getParent().getChildKind(parentCanvas) == CanvasParent.CanvasKind.FIELDS) {
            addStyleClass(isInInterface(parentCanvas) ? "interface-blank-frame" : "class-blank-frame");
        }
        else
        {
            removeStyleClass(isInInterface(parentCanvas) ? "interface-blank-frame" : "class-blank-frame");
        }
    }

    @Override
    protected void saveAsRecent()
    {
        // Do nothing; can never change value
    }

    public boolean isEffectiveFrame()
    {
        return false;
    }
}
