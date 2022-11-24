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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bluej.stride.framedjava.ast.ExpressionSlotFragment;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.canvases.JavaCanvas;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.WhileElement;
import bluej.stride.framedjava.frames.BreakFrame.BreakEncloser;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.framedjava.slots.FilledExpressionSlot;
import bluej.stride.generic.ExtensionDescription;
import bluej.stride.generic.ExtensionDescription.ExtensionSource;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameCursor;
import bluej.stride.generic.FrameFactory;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SingleCanvasFrame;
import bluej.stride.operations.FrameOperation;
import bluej.stride.operations.PullUpContentsOperation;
import bluej.stride.slots.SlotLabel;
import bluej.utility.Utility;
import bluej.utility.javafx.SharedTransition;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Container-block representing a while loop.
 * @author Fraser McKay
 */
public class WhileFrame extends SingleCanvasFrame
  implements CodeFrame<WhileElement>, DebuggableParentFrame
{
    private final ExpressionSlot<FilledExpressionSlotFragment> paramCondition;
    private WhileElement element;

    /**
     * Default constructor.
     */
    private WhileFrame(InteractionManager editor)
    {
        super(editor, "while", "while-");

        //Parameters
        paramCondition = new FilledExpressionSlot(editor, this, this, getHeaderRow(), "while-") {
            @Override
            @OnThread(Tag.FXPlatform)
            public boolean backspaceAtStart()
            {
                if (isAlmostBlank()) {
                    new PullUpContentsOperation(editor).activate(getFrame());
                    return true;
                }
                return super.backspaceAtStart();
            }
        };
        paramCondition.setSimplePromptText("condition");
        paramCondition.setTargetType("boolean");
        //header.getChildren().add(new Label(" ("));
        setHeaderRow(new SlotLabel("("), paramCondition, new SlotLabel(")"));
        /*
        replaceMenu.getItems().clear();
        //Replace with "for"
        MenuItem forMenu = new MenuItem("for...   (disabled)                     ");
        forMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                thisBlock.replaceFor();
            }
        });
        forMenu.setDisable(true);
        //Replace with "if"
        MenuItem ifMenu = new MenuItem("if...");
        ifMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                thisBlock.replaceIf();
            }
        });
        //Replace with "if-else"
        MenuItem ifElseMenu = new MenuItem("if... else...    (disabled)");
        ifElseMenu.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                thisBlock.replaceIfElse();
            }
        });
        ifElseMenu.setDisable(true);
        //Menu items
        replaceMenu.getItems().addAll(forMenu, new SeparatorMenuItem(), ifMenu, ifElseMenu);
        */
        paramCondition.onTextPropertyChange(updateSidebarCurried("while "));

        //cherry
        frameName = "While loop";
        paramCondition.setSlotName("while condition expression");
    }
    
    public WhileFrame(InteractionManager editor, ExpressionSlotFragment condition, boolean enabled)
    {
        this(editor);
        paramCondition.setText(condition);
        frameEnabledProperty.set(enabled);
    }
    
    public WhileFrame(InteractionManager editor, List<Frame> contents)
    {
        this(editor);
        getCanvas().getFirstCursor().insertFramesAfter(contents);
    }

    //cherry
    public String getScreenReaderText() {
        String condition;
        condition = (paramCondition.getScreenreaderText().equals(""))? "blank" : paramCondition.getScreenreaderText();
        return "while frame with condition " + condition;
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

    //cherry
    public String getLocationDescription(FrameCanvas c) {
        String condition, text;
        if (paramCondition.getText().equals("")) { condition = "blank"; } else { condition = paramCondition.getText(); }
        text = " in a while frame with condition " + condition + ",";
        if (getParentCanvas()!=null && getParentCanvas().getParent() != null) {
            text += getParentCanvas().getParentLocationDescription();
        }
        return text;
    }


    /**
     * Replace statement with a "for" loop, transferring over loop body and header.
     */
    /*
    private void replaceFor()
    {
        ForBlock f = new ForBlock(getEditor());
        //Header can't be copied directly for this type
        //Move body
        getCanvas().moveContentsTo(f.getCanvas());
        replaceWith(f);
    }
    private void replaceIf()
    {
        IfBlock i = new IfBlock(getEditor());
        //Copy header
        i.param1.setText(param1.getText());
        //Move body
        getCanvas().moveContentsTo(i.getCanvas());
        replaceWith(i);
    }
    private void replaceIfElse()
    {
        IfElseBlock i = new IfElseBlock(getEditor());
        //Copy header
        i.param1.setText(param1.getText());
        //Move body
        getCanvas().moveContentsTo(i.getCanvas());
        replaceWith(i);
    }
    */

    @Override
    @OnThread(Tag.FXPlatform)
    public List<FrameOperation> getContextOperations()
    {
        List<FrameOperation> r = super.getContextOperations();
        r.add(new PullUpContentsOperation(getEditor()));
        return r;
    }

    public static FrameFactory<WhileFrame> getFactory()
    {
        return new FrameFactory<WhileFrame>() {
            @Override
            public WhileFrame createBlock(InteractionManager editor)
            {
                return new WhileFrame(editor);
            }
            
            @Override
            public WhileFrame createBlock(InteractionManager editor, List<Frame> contents)
            {
                return new WhileFrame(editor, contents);
            }
                        
            @Override
            public Class<WhileFrame> getBlockClass()
            {
                return WhileFrame.class; 
            }
        };
    }
    
    private List<CodeElement> getContents()
    {
        List<CodeElement> contents = new ArrayList<CodeElement>();
        canvas.getBlocksSubtype(CodeFrame.class).forEach(f -> {
            f.regenerateCode();
            contents.add(f.getCode());
        });
        return contents;
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
        element = new WhileElement(this, paramCondition.getSlotElement(), getContents(), frameEnabledProperty.get());
    }

    @Override
    public WhileElement getCode()
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
    public BreakEncloser asBreakEncloser()
    {
        return BreakEncloser.WHILE;
    }

    @Override
    public @OnThread(Tag.FXPlatform) void setView(View oldView, View newView, SharedTransition animateProgress)
    {
        super.setView(oldView, newView, animateProgress);
        if (isFrameEnabled()  && (oldView == View.JAVA_PREVIEW || newView == View.JAVA_PREVIEW))
            canvas.previewCurly(newView == View.JAVA_PREVIEW, header.getLeftFirstItem() + tweakCurlyX(), tweakOpeningCurlyY(), animateProgress);
    }

    //Manvi jain
    @Override
    public void updateAppearance(FrameCanvas parentCanvas)
    {
        super.updateAppearance(parentCanvas);
        //Set the accessibility help of the slot in the frame
        if(getParentCanvas() != null && getParentCanvas().getParent() != null)
        {
            paramCondition.setAccessibilityHelpSlots();
        }
    }


    /**
     * returns the position of the frame as a String
     * @return
     */
    @Override
    public String getHelpContext()
    {
        String parent = "";
        if(getParentCanvas() != null && getParentCanvas().getParent() != null)
        {
            parent = getParentCanvas().getParent().getHelpContext();
        }
        return "in while loop " + parent ;
    }
}
