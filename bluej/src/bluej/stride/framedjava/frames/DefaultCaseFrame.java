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
import bluej.stride.framedjava.elements.DefaultCaseElement;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameFactory;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SingleCanvasFrame;
import bluej.stride.operations.FrameOperation;

public class DefaultCaseFrame extends SingleCanvasFrame implements CodeFrame<DefaultCaseElement>, DebuggableParentFrame
{
    private DefaultCaseElement element;
    
    /**
     * Default constructor.
     */
    private DefaultCaseFrame(InteractionManager editor)
    {
        super(editor, "default", "default-case-");

        //Parameters
        
        setSidebar("default");
    }
    
    public DefaultCaseFrame(InteractionManager editor, boolean enabled)
    {
        this(editor);
        frameEnabledProperty.set(enabled);
    }

    public static FrameFactory<DefaultCaseFrame> getFactory()
    {
        return new FrameFactory<DefaultCaseFrame>() {
            @Override
            public DefaultCaseFrame createBlock(InteractionManager editor)
            {
                return new DefaultCaseFrame(editor);
            }
                        
            @Override 
            public Class<DefaultCaseFrame> getBlockClass()
            { 
                return DefaultCaseFrame.class; 
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
        ArrayList<CodeElement> contents = new ArrayList<CodeElement>();
        for (CodeFrame<?> f : canvas.getBlocksSubtype(CodeFrame.class)) {
            f.regenerateCode();
            contents.add(f.getCode());
        }
        element = new DefaultCaseElement(this, contents, frameEnabledProperty.get());
        
    }

    @Override
    public DefaultCaseElement getCode()
    {
        return element;
    }
}
