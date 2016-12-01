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


import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bluej.stride.generic.CanvasParent;
import bluej.stride.generic.DocumentationTextArea;
import bluej.stride.framedjava.elements.CommentElement;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameFactory;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.RecallableFocus;
import bluej.stride.generic.SingleLineFrame;
import bluej.stride.operations.FrameOperation;
import bluej.stride.slots.EditableSlot;
import bluej.utility.Debug;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.SharedTransition;
import threadchecker.OnThread;
import threadchecker.Tag;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;

/**
 * A statement with // for comments
 * @author Fraser McKay
 */
public class CommentFrame extends SingleLineFrame implements CodeFrame<CommentElement>
{
    private static final String COMMENT_STYLE_PREFIX = "comment-";
    private DocumentationTextArea comment;
    private CommentElement element;
    private Canvas diagonalLinesCanvas;

    /**
     * Default constructor.
     */
    private CommentFrame(InteractionManager editor)
    {
        super(editor, null, COMMENT_STYLE_PREFIX);
        //Parameters
        //param1.getStyleClass().add("comment");
        comment = new DocumentationTextArea(editor, this, getHeaderRow(), COMMENT_STYLE_PREFIX, () -> focusEnter(getHeaderRow()));
        comment.setDocComment(false);
        comment.setPromptText("Comment...");
        comment.bindPrefMaxWidth(getHeaderRow().flowPaneWidth().subtract(1.0));
        setHeaderRow(comment);
        JavaFXUtil.onceInScene(comment.getNode(), comment::hackFixSizing);
        comment.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode())
            {
                case BACK_SPACE:
                    backspaceAtStart(getHeaderRow(), comment);
                    break;
                case DELETE:
                    backspaceAtStart(getHeaderRow(), comment);
                    break;
                case ESCAPE:
                    escape(null, null);
                    break;
            }
        });
        // Make sure diagonal line canvas is created and drawn on:
        getDiagonalLineCanvas();
    }

    /**
     * Creates a comment with a specific message.
     * @param comment Text of the comment
     */
    public CommentFrame(InteractionManager editor, String comment)
    {
        this(editor);
        //UncaughtExceptionHandler oldHandler = Thread.currentThread().getUncaughtExceptionHandler();
        //Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
        //    Debug.reportError("Setting text: \"" + comment + "\"", e);
        //});
        //try
        //{
            this.comment.setText(comment);
        //}
        //catch (Exception e)
        //{
        //    Debug.reportError("Setting text: \"" + comment + "\"", e);
        //}
        //Thread.currentThread().setUncaughtExceptionHandler(oldHandler);
    }

    public static FrameFactory<CommentFrame> getFactory()
    {
        return new FrameFactory<CommentFrame>() {
            
            @Override
            public CommentFrame createBlock(InteractionManager editor)
            {
               return new CommentFrame(editor);
            }
            
            @Override 
            public Class<CommentFrame> getBlockClass()
            { 
                return CommentFrame.class;
            }
        };
    }
    
    private Canvas getDiagonalLineCanvas()
    {
        if (diagonalLinesCanvas == null)
        {
            diagonalLinesCanvas = new Canvas(16, 16);
            diagonalLinesCanvas.setManaged(false);
            GraphicsContext gc = diagonalLinesCanvas.getGraphicsContext2D();
            gc.setStroke(Color.rgb(180, 180, 180));
            gc.strokeLine(6, 0, 0, 10);
            gc.strokeLine(9, 0, 0, 15);
            header.addOverlay(diagonalLinesCanvas);
        }
        return diagonalLinesCanvas;
    }

    @Override
    public void updateAppearance(FrameCanvas parentCanvas)
    {
        super.updateAppearance(parentCanvas);
        if (parentCanvas == null) {
            // When deleting the frame or remove old copy due to drag.
            return;
        }

        JavaFXUtil.setPseudoclass("bj-fields", !isField(parentCanvas), getNode());

        JavaFXUtil.setPseudoclass("bj-before-local-var",!isField(parentCanvas) && isBeforeVarFrame(parentCanvas), getNode());

        if (Arrays.asList(CanvasParent.CanvasKind.FIELDS, CanvasParent.CanvasKind.CONSTRUCTORS, CanvasParent.CanvasKind.METHODS)
                .contains(parentCanvas.getParent().getChildKind(parentCanvas)))
        {
            addStyleClass(isInInterface(parentCanvas) ? "interface-comment-frame" : "class-comment-frame");
        }
        else
        {
            removeStyleClass(isInInterface(parentCanvas) ? "interface-comment-frame" : "class-comment-frame");
        }
    }

    private boolean isField(FrameCanvas parentCanvas)
    {
        if (parentCanvas == null) {
            bluej.utility.Debug.printCallStack("parentCanvas shouldn't be null");
            return false;
        }
        return parentCanvas.getParent().getChildKind(parentCanvas) == CanvasParent.CanvasKind.FIELDS;
    }

    private boolean isBeforeVarFrame(FrameCanvas parentCanvas)
    {
        Frame frameAfter = parentCanvas.getFrameAfter(getCursorAfter());
        int counter = 0;
        while ( frameAfter != null && !frameAfter.isEffectiveFrame() && counter < 2) {
            counter++;
            frameAfter = parentCanvas.getFrameAfter(frameAfter.getCursorAfter());
        }
        return frameAfter instanceof VarFrame;
    }

    @Override
    public void setElementEnabled(boolean enabled)
    {
    }

    @Override
    public boolean canHaveEnabledState(boolean enabled)
    {
        // Makes no sense to disable/enable a comment frame:
        return enabled;
    }

    public String getComment()
    {
        return comment.getText();
    }

    @Override
    public void regenerateCode()
    {
        element = new CommentElement(this);
    }

    @Override
    public CommentElement getCode()
    {
        return element;
    }

    public RecallableFocus getFocusable()
    {
        return comment;
    }

    @Override
    public EditableSlot getErrorShowRedirect()
    {
        return null; // Don't put error on the comment field
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void setView(View oldView, View newView, SharedTransition animation)
    {
        super.setView(oldView, newView, animation);
        if (newView == View.JAVA_PREVIEW)
        {
            // Don't show lines in Java preview:
            getDiagonalLineCanvas().opacityProperty().bind(animation.getOppositeProgress());
            animation.addOnStopped(getDiagonalLineCanvas().opacityProperty()::unbind);
        }
        else
        {
            // Show lines:
            getDiagonalLineCanvas().opacityProperty().bind(animation.getProgress());
            animation.addOnStopped(getDiagonalLineCanvas().opacityProperty()::unbind);
        }
        
        if (newView == View.BIRDSEYE_NODOC)
        {
            getDiagonalLineCanvas().setVisible(false);
            // We have to set this to unmanaged, rather than solely relying on DocumentationTextArea's own behaviour
            // of collapsing itself, because HangingFlowPane with a non-zero baseline doesn't
            // give zero height for a zero-height child, unless it's unmanaged:
            comment.getNode().setManaged(false);
        }
        else
        {
            getDiagonalLineCanvas().setVisible(true);
            comment.getNode().setManaged(true);
        }
    }

    public boolean isEffectiveFrame()
    {
        return false;
    }
}
