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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import bluej.stride.framedjava.slots.TypeSlot;
import bluej.stride.generic.ExtensionDescription.ExtensionSource;
import bluej.stride.generic.FrameContentItem;
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
import bluej.stride.slots.VariableNameDefTextSlot;
import bluej.stride.operations.PullUpContentsOperation;
import bluej.utility.Utility;
import bluej.utility.javafx.SharedTransition;
import threadchecker.OnThread;
import threadchecker.Tag;

public class ForeachFrame extends SingleCanvasFrame
  implements CodeFrame<ForeachElement>, DebuggableParentFrame
{
    private final TypeSlot type;
    private final VariableNameDefTextSlot var;
    private final EachExpressionSlot collection;
    private ForeachElement element;
    private final SlotLabel inLabel;
    private final SlotLabel finalLabel;

    private ForeachFrame(InteractionManager editor)
    {
        super(editor, "for each", "foreach-");
        
        type = new TypeSlot(editor, this, this, getHeaderRow(), TypeSlot.Role.DECLARATION, "foreach-type-");
        type.setSimplePromptText("item type");
        type.addClosingChar(' ');

        var = new VariableNameDefTextSlot(editor, this, getHeaderRow(), "foreach-var-");
        var.setPromptText("item name");
        var.addValueListener(SlotTraversalChars.IDENTIFIER);
        
        collection = new EachExpressionSlot(editor, this, this, getHeaderRow(), type, var, "foreach-collection-");
        collection.setSimplePromptText("collection");
        inLabel = new SlotLabel(" in ");
        finalLabel=new SlotLabel("");
        setHeaderRow(new SlotLabel("("),finalLabel, type, var, inLabel, collection, new SlotLabel(")"));

        FXConsumer<String> updateTriple = s -> updateSidebarCurried("for each ").accept(type.getText() + " " + var.getText() + " : " + collection.getText());
        type.onTextPropertyChange(updateTriple);
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
    @OnThread(Tag.FXPlatform)
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
    public FrameCanvas createCanvas(InteractionManager editor, String stylePrefix)
    {
        return new JavaCanvas(editor, this, stylePrefix, false);
    }

    @Override
    @OnThread(Tag.FXPlatform)
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
    public List<ExtensionDescription> getAvailableExtensions(FrameCanvas canvas, FrameCursor cursorInCanvas)
    {
        return Utility.concat(super.getAvailableExtensions(canvas, cursorInCanvas),
                Arrays.asList(new ExtensionDescription('\b', "Remove loop, keep contents", () ->
                        new PullUpContentsOperation(getEditor()).activate(this), false, ExtensionSource.INSIDE_FIRST)));
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
    @OnThread(Tag.FXPlatform)
    public void setView(View oldView, View newView, SharedTransition animateProgress)
    {
        super.setView(oldView, newView, animateProgress);
        String caption = newView == View.JAVA_PREVIEW ? "for" : "for each";
        headerCaptionLabel.setText(caption);
        String finalkeyword = newView == View.JAVA_PREVIEW ? "final " : "";
        finalLabel.setText(finalkeyword);
        inLabel.setText(newView == View.JAVA_PREVIEW ? (collection.isConstantRange() ? " = " : " : ") : " in ");

        if (isFrameEnabled()  && (oldView == View.JAVA_PREVIEW || newView == View.JAVA_PREVIEW))
            canvas.previewCurly(newView == View.JAVA_PREVIEW, header.getLeftFirstItem() + tweakCurlyX(), tweakOpeningCurlyY(), animateProgress);
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public boolean backspaceAtStart(FrameContentItem srcRow, HeaderItem src)
    {
        if (src == type && type.isAlmostBlank())
        {
            new PullUpContentsOperation(getEditor()).activate(getFrame());
            return true;
        }
        return super.backspaceAtStart(srcRow, src);
    }

}
