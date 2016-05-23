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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bluej.stride.framedjava.frames;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.TextSlotFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.canvases.JavaCanvas;
import bluej.stride.framedjava.elements.CatchElement;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.slots.TypeSlot;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameFactory;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SingleCanvasFrame;
import bluej.stride.operations.FrameOperation;
import bluej.stride.slots.SlotLabel;
import bluej.stride.slots.TextSlot;
import bluej.stride.slots.VariableNameDefTextSlot;
import bluej.stride.slots.SlotTraversalChars;
import bluej.stride.slots.TypeCompletionCalculator;
import bluej.utility.javafx.FXConsumer;
import bluej.utility.javafx.JavaFXUtil;

import javafx.beans.value.ChangeListener;

/**
 * Container-block representing a catch exception.
 * @author Amjad
 */
public class CatchFrame extends SingleCanvasFrame
  implements CodeFrame<CatchElement>, DebuggableParentFrame
{
    private final TypeSlot exceptionType;
    private final TextSlot<NameDefSlotFragment> exceptionName;
    private CatchElement element;
    
    /**
     * Default constructor.
     */
    private CatchFrame(InteractionManager editor)
    {
        super(editor, "catch", "catch-");
        //Parameters
        exceptionType = new TypeSlot(editor, this, this, getHeaderRow(), new TypeCompletionCalculator(editor, Throwable.class), "catch-");
        exceptionType.setText(new TypeSlotFragment("", ""));
        exceptionType.addFocusListener(this);
        exceptionType.setSimplePromptText("exceptionType");
        exceptionName = initialiseTextSlot("Name", new NameDefSlotFragment(""), new VariableNameDefTextSlot(editor, this, this, getHeaderRow(), "catch-"));
        setHeaderRow(new SlotLabel("("), exceptionType, exceptionName, new SlotLabel(")"));

        exceptionType.addClosingChar(' ');
        
        FXConsumer<String> changeListener = newVal -> updateSidebarCurried("catch ").accept(exceptionType.getText() + " " + exceptionName.getText());
        exceptionType.onTextPropertyChange(changeListener);
        JavaFXUtil.addChangeListener(exceptionName.textProperty(), changeListener);
    }
    
    public CatchFrame(InteractionManager editor, TypeSlotFragment exceptionTypeFragment, NameDefSlotFragment exceptionNameFragment, boolean enabled)
    {
        this(editor);
        exceptionType.setText(exceptionTypeFragment);
        exceptionName.setText(exceptionNameFragment);
        frameEnabledProperty.set(enabled);
    }

    private <F extends TextSlotFragment> TextSlot<F> initialiseTextSlot(String promptText, F value, TextSlot<F> textSlot)
    {
        textSlot.setPromptText("exception" + promptText);
        textSlot.setText(value);
        textSlot.addFocusListener(this);
        textSlot.addValueListener(SlotTraversalChars.IDENTIFIER);
        return textSlot;
    }

    public static FrameFactory<CatchFrame> getFactory()
    {
        return new FrameFactory<CatchFrame>() {
            @Override
            public CatchFrame createBlock(InteractionManager editor)
            {
                return new CatchFrame(editor);
            }
                        
            @Override
            public Class<CatchFrame> getBlockClass()
            {
                return CatchFrame.class;
            }
        };
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
        for (CodeFrame<?> f : canvas.getBlocksSubtype(CodeFrame.class))
        {
            f.regenerateCode();
            contents.add(f.getCode());
        }
        element = new CatchElement(this, exceptionType.getSlotElement(), exceptionName.getSlotElement(), contents, frameEnabledProperty.get());
        
    }

    @Override
    public CatchElement getCode()
    {
        return element;
    }

    public Collection<? extends Frame> getValidPulledStatements()
    {
        canvas.getBlockContents().forEach(frame -> frame.setParentCanvas(null));
        return canvas.getBlockContents();
    }
}
