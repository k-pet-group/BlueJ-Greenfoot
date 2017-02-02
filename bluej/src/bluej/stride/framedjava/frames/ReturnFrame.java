/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2017 Michael Kölling and John Rosenberg
 
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
import java.util.stream.Stream;

import bluej.stride.slots.EditableSlot;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Cursor;
import bluej.stride.framedjava.ast.ExpressionSlotFragment;
import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.canvases.JavaCanvas;
import bluej.stride.framedjava.elements.ReturnElement;
import bluej.stride.framedjava.slots.OptionalExpressionSlot;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameFactory;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SingleLineFrame;
import bluej.stride.operations.FrameOperation;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.SlotLabel;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;

/**
 * A return statement
 * @author Fraser McKay
 */
public class ReturnFrame extends SingleLineFrame
  implements CodeFrame<ReturnElement>, DebuggableFrame
{
    private final OptionalExpressionSlot value;
    private ReturnElement element;
    private final SimpleStringProperty returnType = new SimpleStringProperty();
    private final SimpleBooleanProperty showingValue = new SimpleBooleanProperty(true);
    
    // We have to keep a reference to negated version, to prevent it getting GCed:
    private final BooleanExpression notShowingValue = showingValue.not();
    
    /**
     * Default constructor.
     * @param editor 
     */
    private ReturnFrame(InteractionManager editor)
    {
        super(editor, "return", "return-");
        
        SlotLabel spacer = new SlotLabel(" ");
        spacer.setOpacity(0.0);
        spacer.setCursor(Cursor.TEXT);
        //Parameters
        value = new OptionalExpressionSlot(editor, this, this, getHeaderRow(), "return-");
        value.setSimplePromptText("expression");
        value.bindTargetType(returnType);

        JavaFXUtil.addChangeListener(showingValue, showing -> {
            if (!showing)
                value.cleanup();
        });

        spacer.setOnMouseClicked(e -> {
            showingValue.set(true);
            value.requestFocus();
            e.consume();
        });
        
        getHeaderRow().bindContentsConcat(FXCollections.<ObservableList<? extends HeaderItem>>observableArrayList(
                FXCollections.observableArrayList(headerCaptionLabel),
                JavaFXUtil.listBool(notShowingValue, spacer),
                JavaFXUtil.listBool(showingValue, value),
                FXCollections.observableArrayList(previewSemi)
        ));
        
        // Remove value slot if return type is void:
        value.onLostFocus(() -> {
            if ("void".equals(returnType.get()) && value.getText().isEmpty())
                showingValue.set(false);
        } );
    }
    
    public ReturnFrame(InteractionManager editor, ExpressionSlotFragment val, boolean enabled)
    {
        this(editor);
        if (val != null)
        {
            showingValue.set(true);
            value.setText(val);
        }
        else
        {
            showingValue.set(false);
        }
        frameEnabledProperty.set(enabled);
    }

    @Override
    public void regenerateCode()
    {
        // We generate the return value iff:
        //   - The value is currently visible, AND
        //     - the text is non-empty, OR
        //     - we have triggered code completion in the slot
        final boolean generateReturnValue = showingValue.get() && (!value.getText().isEmpty() || value.isCurrentlyCompleting());
        element = new ReturnElement(this, generateReturnValue ? value.getSlotElement() : null, frameEnabledProperty.get());
    }
    
    @Override
    public ReturnElement getCode()
    {
        return element;
    }
    
    public static FrameFactory<ReturnFrame> getFactory()
    {
        return new FrameFactory<ReturnFrame>() {
            @Override
            public ReturnFrame createBlock(InteractionManager editor)
            {
                return new ReturnFrame(editor);
            }
                        
            @Override 
            public Class<ReturnFrame> getBlockClass()
            {
                return ReturnFrame.class;
            }
        };
    }

    @Override
    public void updateAppearance(FrameCanvas parentCanvas)
    {
        super.updateAppearance(parentCanvas);
        // Find method that we are in, and bind to its return type
        for (Frame f = parentCanvas == null ? null : parentCanvas.getParent().getFrame(); f != null; f = f.getParentCanvas() == null ? null : f.getParentCanvas().getParent().getFrame())
        {
            if (f instanceof NormalMethodFrame)
            {
                NormalMethodFrame mf = (NormalMethodFrame)f;
                returnType.unbind();
                returnType.bind(mf.returnTypeProperty());
                // We will automatically add value, but not remove:
                if (!"void".equals(returnType.get()))
                    showingValue.set(true);
                return;
            }
        }
        
        // If there is no normal method frame we may be in constructor or whatever, so no value return:
        returnType.unbind();
        returnType.set(null);
        showingValue.set(false);
    }

    // Called when method's return type changes to void and user tells follow-up to remove values
    // If there is empty value field to remove, just removes it
    // Returns null if there is no filled value field to remove
    public FXRunnable getRemoveFilledValueAction()
    {
        if (value.getText().equals(""))
        {
            showingValue.set(false);
            return null;
        }
        else if (showingValue.get())
            return () -> showingValue.set(false);
        else
            return null;
    }

    @Override
    public Stream<EditableSlot> getPossiblyHiddenSlotsDirect()
    {
        return Stream.of(value);
    }

    // Called when method's return type changes to non-blank, non-void:
    public void showValue()
    {
        showingValue.set(true);
    }
}
