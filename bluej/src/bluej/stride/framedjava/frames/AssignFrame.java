/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2018 Michael Kölling and John Rosenberg
 
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


import bluej.stride.framedjava.ast.ExpressionSlotFragment;
import bluej.stride.generic.FrameContentItem;
import bluej.utility.javafx.JavaFXUtil;
import javafx.application.Platform;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.elements.AssignElement;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.framedjava.slots.FilledExpressionSlot;
import bluej.stride.generic.FrameFactory;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SingleLineFrame;
import bluej.stride.slots.Focus;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.SlotLabel;
import bluej.utility.javafx.SharedTransition;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A set statement for assignment, e.g. "set x = 1"
 * @author Fraser McKay
 */
public class AssignFrame extends SingleLineFrame
  implements CodeFrame<AssignElement>, DebuggableFrame
{

    public static final String ASSIGN_SYMBOL = "\u21D0";
    private final ExpressionSlot<FilledExpressionSlotFragment> slotLHS;
    private final ExpressionSlot<FilledExpressionSlotFragment> slotRHS;
    private AssignElement element;
    private SlotLabel assignLabel;

    /**
     * Default constructor.
     * @param editor 
     */
    private AssignFrame(InteractionManager editor)
    {
        super(editor, null, "set-");
        //Parameters
        slotRHS = new FilledExpressionSlot(editor, this, this, getHeaderRow(), "", FilledExpressionSlot.SRC_HINTS);
        slotRHS.setSimplePromptText("new-value");
        slotLHS = new FilledExpressionSlot(editor, this, this, getHeaderRow(), "assign-lhs-");
        slotLHS.setSimplePromptText("variable");
        assignLabel = new SlotLabel(ASSIGN_SYMBOL);
        setHeaderRow(slotLHS, assignLabel, slotRHS, previewSemi);
        
        slotLHS.addClosingChar('=');
        slotLHS.addClosingChar(' ');
    }
    
    // For replacement of a method call frame:
    AssignFrame(InteractionManager editor, String lhs, String rhs)
    {
        this(editor);
        slotLHS.setText(lhs);
        slotRHS.setText(rhs);
        if (Platform.isFxApplicationThread())
        {
            JavaFXUtil.runPlatformLater(() -> slotRHS.requestFocus(Focus.LEFT));
        }
    }
    
    public AssignFrame(InteractionManager editor, FilledExpressionSlotFragment lhs, FilledExpressionSlotFragment rhs, boolean enabled)
    {
        this(editor);
        slotLHS.setText(lhs);
        slotRHS.setText(rhs);
        frameEnabledProperty.set(enabled);
    }

    @Override
    public void regenerateCode()
    {
        element = new AssignElement(this, slotLHS.getSlotElement(), slotRHS.getSlotElement(), 
                frameEnabledProperty.get());
    }
    
    @Override
    public AssignElement getCode()
    {
        return element;
    }
    
    public static FrameFactory<AssignFrame> getFactory()
    {
        return new FrameFactory<AssignFrame>() {
            @Override
            public AssignFrame createBlock(InteractionManager editor)
            {
                return new AssignFrame(editor);
            }
                        
            @Override 
            public Class<AssignFrame> getBlockClass()
            { 
                return AssignFrame.class;
            }
        };
    }
    
    public ExpressionSlot<? extends ExpressionSlotFragment> getLHS()
    {
        return slotLHS;
    }
    
    public ExpressionSlot<? extends ExpressionSlotFragment> getRHS()
    {
        return slotRHS;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public boolean backspaceAtStart(FrameContentItem row, HeaderItem src)
    {
        if (src == slotRHS)
        {
            collapseIntoMethodCall();
            return true;
        }
        else
            return super.backspaceAtStart(row, src);
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public boolean deleteAtEnd(FrameContentItem row, HeaderItem src)
    {
        if (src == slotLHS)
        {
            collapseIntoMethodCall();
            return true;
        }
        return false;
    }

    @OnThread(Tag.FXPlatform)
    private void collapseIntoMethodCall()
    {
        getParentCanvas().replaceBlock(this, new CallFrame(getEditor(), slotLHS.getText(), slotRHS.getText()));        
    }

    @Override
    public @OnThread(Tag.FXPlatform) void setView(View oldView, View newView, SharedTransition animation)
    {
        super.setView(oldView, newView, animation);
        assignLabel.setText(newView == View.JAVA_PREVIEW ? "=" : ASSIGN_SYMBOL);
    }
}
