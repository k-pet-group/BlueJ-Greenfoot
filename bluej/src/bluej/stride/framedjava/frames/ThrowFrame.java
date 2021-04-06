/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2021 Michael Kölling and John Rosenberg
 
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


import java.util.List;

import bluej.stride.generic.FrameCanvas;
import javafx.beans.property.SimpleIntegerProperty;
import bluej.stride.framedjava.ast.ExpressionSlotFragment;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.canvases.JavaCanvas;
import bluej.stride.framedjava.elements.ThrowElement;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.framedjava.slots.FilledExpressionSlot;
import bluej.stride.generic.FrameFactory;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SingleLineFrame;
import bluej.stride.operations.FrameOperation;

/**
 * A Throw statement
 */
public class ThrowFrame extends SingleLineFrame
  implements CodeFrame<ThrowElement>, DebuggableFrame
{
    private ExpressionSlot<FilledExpressionSlotFragment> param1;
    private ThrowElement element;
    private SimpleIntegerProperty codeVersion;
    
    /**
     * Default constructor.
     * @param editor 
     */
    private ThrowFrame(InteractionManager editor)
    {
        super(editor, "throw", "throw-");
        this.codeVersion = new SimpleIntegerProperty(0);
        //Parameters
        param1 = new FilledExpressionSlot(editor, this, this, getHeaderRow(), "throw-");
        param1.setSimplePromptText("expression");
        setHeaderRow(param1, previewSemi);

        //cherry
        frameName = "throw " + param1.getScreenreaderText();
        param1.setSlotName("exception name");
    }
    
    public ThrowFrame(InteractionManager editor, ExpressionSlotFragment val, boolean enabled)
    {
        this(editor);
        param1.setText(val);
        frameEnabledProperty.set(enabled);
    }

    //cherry
    public String getScreenReaderText() {
        String thrown;
        thrown = (param1.getText().equals(""))? "blank" : param1.getScreenreaderText();
        return "throw " + thrown;
    }

    //cherry
    /**
     * Get the help text of this frame, to pass to setAccessibilityHelp().
     * Calls the parent frame if there is one, to get the parent's description
     * plus the descriptions of that parent's parents.
     */
    public String getScreenReaderHelp() {
        return "you are " + getParentCanvas().getParentLocationDescription();
    }

    @Override
    public void regenerateCode()
    {
        element = new ThrowElement(this, param1.getSlotElement(), frameEnabledProperty.get());
        codeVersion.set(codeVersion.get() + 1);
    }
    
    @Override
    public ThrowElement getCode()
    {
        return element;
    }
    
    public static FrameFactory<ThrowFrame> getFactory()
    {
        return new FrameFactory<ThrowFrame>() {
            @Override
            public ThrowFrame createBlock(InteractionManager editor)
            {
                return new ThrowFrame(editor);
            }
                        
            @Override 
            public Class<ThrowFrame> getBlockClass()
            {
                return ThrowFrame.class;
            }
        };
    }

    //cherry
    @Override
    public void updateAppearance(FrameCanvas parentCanvas) {
        super.updateAppearance(parentCanvas);
        if(getParentCanvas() != null && getParentCanvas().getParent() != null)
        {
            param1.setAccessibilityHelpSlots();
        }
    }
}
