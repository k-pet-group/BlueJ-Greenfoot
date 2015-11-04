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
package bluej.stride.framedjava.frames;


import java.util.List;

import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.canvases.JavaCanvas;
import bluej.stride.framedjava.elements.BreakpointElement;
import bluej.stride.generic.FrameFactory;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SingleLineFrame;
import bluej.stride.operations.FrameOperation;

public class BreakpointFrame extends SingleLineFrame implements CodeFrame<BreakpointElement>, DebuggableFrame
{
    private BreakpointElement element;

    /**
     * Default constructor.
     */
    private BreakpointFrame(InteractionManager editor)
    {
        super(editor, "break point", "breakpoint-");
    }
    
    public BreakpointFrame(InteractionManager editor, boolean enabled)
    {
        this(editor);
        frameEnabledProperty.set(enabled);
    }

    public static FrameFactory<BreakpointFrame> getFactory()
    {
        return new FrameFactory<BreakpointFrame>() {
            @Override
            public BreakpointFrame createBlock(InteractionManager editor)
            {
                return new BreakpointFrame(editor);
            }
                        
            @Override public Class<BreakpointFrame> getBlockClass() { return BreakpointFrame.class; }
        };
    }

    @Override
    public BreakpointElement getCode()
    {
        return element;
    }
    
    @Override
    public void regenerateCode()
    {
        element = new BreakpointElement(this, frameEnabledProperty.get());
    }
    
    @Override
    public List<FrameOperation> getCutCopyPasteOperations(InteractionManager editor)
    {
        return GreenfootFrameUtil.cutCopyPasteOperations(editor);
    }
    

    @Override
    public HighlightedBreakpoint showDebugBefore(DebugInfo debug)
    {
        return ((JavaCanvas)getParentCanvas()).showDebugBefore(this, debug);        
    }

}
