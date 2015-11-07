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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import bluej.stride.generic.FrameCursor;
import bluej.utility.javafx.FXConsumer;
import bluej.utility.javafx.JavaFXUtil;
import bluej.stride.framedjava.ast.ExpressionSlotFragment;
import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.canvases.JavaCanvas;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.ForeachElement;
import bluej.stride.framedjava.frames.BreakFrame.BreakEncloser;
import bluej.stride.framedjava.slots.EachExpressionSlot;
import bluej.stride.generic.ExtensionDescription;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameFactory;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SingleCanvasFrame;
import bluej.stride.operations.FrameOperation;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.SlotLabel;
import bluej.stride.slots.SlotTraversalChars;
import bluej.stride.slots.TypeCompletionCalculator;
import bluej.stride.slots.TypeTextSlot;
import bluej.stride.slots.VariableNameDefTextSlot;
import bluej.stride.operations.PullUpContentsOperation;
import bluej.utility.Utility;
import bluej.utility.javafx.SharedTransition;

public class ForeachFrame extends SingleCanvasFrame
  implements CodeFrame<ForeachElement>, DebuggableParentFrame
{
    private final TypeTextSlot type;
    private final VariableNameDefTextSlot var;
    private final EachExpressionSlot collection;
    private ForeachElement element;
    private final SlotLabel inLabel;

    private ForeachFrame(InteractionManager editor)
    {
        super(editor, "for each", "foreach-");
        
        type = new TypeTextSlot(editor, this, getHeaderRow(), new TypeCompletionCalculator(editor), "foreach-type-");
        type.setPromptText("item type");
        type.addValueListener(SlotTraversalChars.IDENTIFIER);
        type.addValueListener(new SlotTraversalChars() {
            @Override
            public void backSpacePressedAtStart(HeaderItem slot) {
                if (type.isAlmostBlank()) {
                    new PullUpContentsOperation(editor).activate(getFrame());
                }
            }
        });

        var = new VariableNameDefTextSlot(editor, this, getHeaderRow(), "foreach-var-");
        var.setPromptText("item name");
        var.addValueListener(SlotTraversalChars.IDENTIFIER);
        
        collection = new EachExpressionSlot(editor, this, this, getHeaderRow(), type, var, "foreach-collection-");
        collection.setSimplePromptText("collection");
        inLabel = new SlotLabel(" in ");
        setHeaderRow(new SlotLabel("("), type, var, inLabel, collection, new SlotLabel(")"));

        FXConsumer<String> updateTriple = s -> updateSidebarCurried("for each ").accept(type.getText() + " " + var.getText() + " : " + collection.getText());
        JavaFXUtil.addChangeListener(type.textProperty(), updateTriple);
        JavaFXUtil.addChangeListener(var.textProperty(), updateTriple);
        collection.onTextPropertyChange(updateTriple);
    }
    
    public ForeachFrame(InteractionManager editor, TypeSlotFragment type, NameDefSlotFragment var, ExpressionSlotFragment collection, boolean enabled) {
        this(editor);
        this.type.setText(type);
        this.var.setText(var);
        this.collection.setText(collection);
        frameEnabledProperty.set(enabled);
    }
    
    public ForeachFrame(InteractionManager editor, List<Frame> contents)
    {
        this(editor);
        getCanvas().getFirstCursor().insertFramesAfter(contents);
    }

    @Override
    public boolean acceptsType(FrameCanvas canvas, Class<? extends Frame> blockClass)
    {
        return getEditor().getDictionary().isValidStatment(blockClass);
    }

    @Override
    public List<FrameOperation> getContextOperations()
    {
        List<FrameOperation> r = super.getContextOperations();
        r.add(new PullUpContentsOperation(getEditor()));
        return r;
    }

    public static FrameFactory<ForeachFrame> getFactory()
    {
        return new FrameFactory<ForeachFrame>() {
            
            @Override
            public ForeachFrame createBlock(InteractionManager editor)
            {
                return new ForeachFrame(editor);
            }
            
            @Override
            public ForeachFrame createBlock(InteractionManager editor, List<Frame> contents)
            {
                return new ForeachFrame(editor, contents);
            }
                        
            @Override 
            public Class<ForeachFrame> getBlockClass()
            {
                return ForeachFrame.class;
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
        for (CodeFrame<?> f : canvas.getBlocksSubtype(CodeFrame.class)) {
            f.regenerateCode();
            contents.add(f.getCode());
        }
        element = new ForeachElement(this, type.getSlotElement(), var.getSlotElement(), 
                collection.getSlotElement(), contents, frameEnabledProperty.get());
    }

    @Override
    public ForeachElement getCode()
    {
        return element;
    }

    @Override
    public List<ExtensionDescription> getAvailableInnerExtensions(FrameCanvas canvas, FrameCursor cursor)
    {
        return Utility.concat(super.getAvailableInnerExtensions(canvas, cursor),
                Arrays.asList(new ExtensionDescription('\b', "Remove loop, keep contents", () ->
                        new PullUpContentsOperation(getEditor()).activate(this), false, false)));
    }
    
    @Override
    public List<String> getDeclaredVariablesWithin(FrameCanvas c)
    {
        if (c != getCanvas())
            throw new IllegalArgumentException("Canvas does not exist in this frame");
        
        String name = var.getText();
        if (name.isEmpty())
            return Collections.emptyList();
        else
            return Arrays.asList(name);
    }
    
    @Override
    public BreakEncloser asBreakEncloser()
    {
        return BreakEncloser.FOREACH;
    }

    @Override
    public void setView(View oldView, View newView, SharedTransition animateProgress)
    {
        super.setView(oldView, newView, animateProgress);
        String caption = newView == View.JAVA_PREVIEW ? "for" : "for each";
        headerCaptionLabel.setText(caption);
        inLabel.setText(newView == View.JAVA_PREVIEW ? (collection.isConstantRange() ? " = " : " : ") : " in ");
    }
}
