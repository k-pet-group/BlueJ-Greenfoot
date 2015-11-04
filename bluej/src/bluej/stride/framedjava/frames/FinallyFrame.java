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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bluej.stride.framedjava.frames;


import java.util.ArrayList;
import java.util.List;

import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.canvases.JavaCanvas;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.FinallyElement;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameFactory;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SingleCanvasFrame;
import bluej.stride.operations.FrameOperation;

public class FinallyFrame extends SingleCanvasFrame implements CodeFrame<FinallyElement>, DebuggableParentFrame
{
    private FinallyElement element;
    
    /**
     * Default constructor.
     */
    private FinallyFrame(InteractionManager editor)
    {
        super(editor, "finally", "finally-");

        //Parameters
        
        setSidebar("finally");
    }
    
    public FinallyFrame(InteractionManager editor, boolean enabled)
    {
        this(editor);
        frameEnabledProperty.set(enabled);
    }

    public static FrameFactory<FinallyFrame> getFactory()
    {
        return new FrameFactory<FinallyFrame>() {
            @Override
            public FinallyFrame createBlock(InteractionManager editor)
            {
                return new FinallyFrame(editor);
            }
                        
            @Override 
            public Class<FinallyFrame> getBlockClass()
            { 
                return FinallyFrame.class; 
            }
        };
    }
    
    @Override
    public boolean acceptsType(FrameCanvas canvas, Class<? extends Frame> blockClass)
    {
        return getEditor().getDictionary().isValidStatment(blockClass);
    }
    
    @Override
    public List<FrameOperation> getCutCopyPasteOperations(InteractionManager editor)
    {
        return GreenfootFrameUtil.cutCopyPasteOperations(editor);
    }
    
    @Override
    public FrameCanvas createCanvas(InteractionManager editor, String stylePrefix)
    {
        return new JavaCanvas(editor, this, stylePrefix, false);
    }

    @Override
    public HighlightedBreakpoint showDebugBefore(DebugInfo debug)
    {
        return ((JavaCanvas)getParentCanvas()).showDebugBefore(this, debug);
    }

    @Override
    public HighlightedBreakpoint showDebugAtEnd(DebugInfo debug)
    {
        return ((JavaCanvas) getCanvas()).showDebugBefore(null, debug);
    }

    @Override
    public void regenerateCode()
    {
        List<CodeElement> contents = new ArrayList<CodeElement>();
        for (CodeFrame<?> f : canvas.getBlocksSubtype(CodeFrame.class)) {
            f.regenerateCode();
            contents.add(f.getCode());
        }
        element = new FinallyElement(this, contents, frameEnabledProperty.get());
        
    }

    @Override
    public FinallyElement getCode()
    {
        return element;
    }

}
