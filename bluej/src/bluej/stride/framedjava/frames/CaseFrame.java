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
import java.util.Arrays;
import java.util.List;

import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.ast.HighlightedBreakpoint;
import bluej.stride.framedjava.canvases.JavaCanvas;
import bluej.stride.framedjava.elements.CaseElement;
import bluej.stride.framedjava.elements.CodeElement;
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
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.SlotLabel;
import bluej.utility.Utility;
import bluej.utility.javafx.SharedTransition;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Container-block representing a case condition.
 * @author Fraser McKay
 */
public class CaseFrame extends SingleCanvasFrame
  implements CodeFrame<CaseElement>, DebuggableParentFrame
{
    
    private final ExpressionSlot<FilledExpressionSlotFragment> paramCondition;
    private CaseElement element;
    private final SlotLabel opening;
    private final SlotLabel closing;

    /**
     * Default constructor.
     */
    private CaseFrame(InteractionManager editor)
    {
        super(editor, "case", "case-");

        //Parameters
        paramCondition = new FilledExpressionSlot(editor, this, this, getHeaderRow(), "case-condition-");
        paramCondition.setSimplePromptText("value");

        opening = new SlotLabel("(");
        closing = new SlotLabel(")");
        setHeaderRow(opening, paramCondition, closing);
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
        paramCondition.onTextPropertyChange(updateSidebarCurried("case "));
    }
    
    public CaseFrame(InteractionManager editor, FilledExpressionSlotFragment condition, boolean enabled)
    {
        this(editor);
        paramCondition.setText(condition);
        frameEnabledProperty.set(enabled);
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

    public static FrameFactory<CaseFrame> getFactory()
    {
        return new FrameFactory<CaseFrame>() {
            @Override
            public CaseFrame createBlock(InteractionManager editor)
            {
                CaseFrame caseFrame = new CaseFrame(editor);
                caseFrame.getFirstInternalCursor().insertBlockAfter(BreakFrame.getFactory().createBlock(editor));
                return caseFrame;
            }


            @Override 
            public Class<CaseFrame> getBlockClass()
            { 
                return CaseFrame.class;
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
        for (CodeFrame<?> f : canvas.getBlocksSubtype(CodeFrame.class))
        {
            f.regenerateCode();
            contents.add(f.getCode());
        }
        element = new CaseElement(this, paramCondition.getSlotElement(), contents, frameEnabledProperty.get());
        
    }

    @Override
    public CaseElement getCode()
    {
        return element;
    }
    
    @Override
    public List<ExtensionDescription> getAvailableExtensions(FrameCanvas canvas, FrameCursor cursorInCanvas)
    {
        return Utility.concat(super.getAvailableExtensions(canvas, cursorInCanvas),
                Arrays.asList(new ExtensionDescription('\b', "Delete case", () -> {
                    SwitchFrame parent = (SwitchFrame) this.getCursorBefore().getParentCanvas().getParent();
                    parent.pullUpInnerCaseContents(this);
                }, false, ExtensionSource.INSIDE_FIRST)));
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void setView(View oldView, View newView, SharedTransition animateProgress)
    {
        // no curly brackets enclosing the inner statements
        super.setView(oldView, newView, animateProgress);
        opening.setText(newView == View.JAVA_PREVIEW ? "" : "(");
        // Re-use closing as colon syntax:
        closing.setText(newView == View.JAVA_PREVIEW ? ":" : ")");
    }

    public List<Frame> getValidPulledStatements()
    {
        List<Frame> contents = canvas.getBlockContents().filtered(f -> !(f instanceof BreakFrame));
        contents.forEach(frame -> frame.setParentCanvas(null));
        return contents;
    }

    public boolean isAlmostBlank()
    {
        return getEditableSlotsDirect().allMatch(EditableSlot::isAlmostBlank) &&
                canvas.getBlockContents().stream().allMatch(f -> (f instanceof BlankFrame || f instanceof BreakFrame));
    }
}
