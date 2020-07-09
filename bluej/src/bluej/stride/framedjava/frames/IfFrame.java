/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2020 Michael Kölling and John Rosenberg
 
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
import java.util.List;
import java.util.stream.Collectors;

import bluej.stride.framedjava.ast.ExpressionSlotFragment;
import bluej.stride.framedjava.ast.FilledExpressionSlotFragment;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.canvases.JavaCanvas;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.IfElement;
import bluej.stride.framedjava.elements.SandwichCanvasesElement;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.framedjava.slots.FilledExpressionSlot;
import bluej.stride.generic.*;
import bluej.stride.operations.PullUpContentsOperation;
import bluej.stride.slots.SlotLabel;
import bluej.utility.Debug;
import bluej.utility.Utility;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Container-block representing an if statement.
 * @author Fraser McKay
 */
public class IfFrame extends SandwichCanvasesFrame
{
    private static final String IF_STYLE_PREFIX = "if-";
    protected final ExpressionSlot<FilledExpressionSlotFragment> ifCondition;
    private final List<ExpressionSlot<FilledExpressionSlotFragment>> elseIfConditions = new ArrayList<>();

    /**
     * Default constructor.
     */
    private IfFrame(InteractionManager editor)
    {
        super(editor, "if", "elseif", "else", IF_STYLE_PREFIX);

        //Condition
        ifCondition = new FilledExpressionSlot(editor, this, this, getHeaderRow(), "if-", FilledExpressionSlot.CONDITION_HINTS){
            @Override
            @OnThread(Tag.FXPlatform)
            public boolean backspaceAtStart()
            {
                if (isAlmostBlank()) {
                    new PullUpContentsOperation(getEditor()).activate(getFrame(), getCursorBefore());
                    return true;
                }
                return super.backspaceAtStart();
            }
        };
        ifCondition.setSimplePromptText("condition");
        ifCondition.setTargetType("boolean");
        ifCondition.onTextPropertyChange(updateSidebarCurried("if "));
        setHeaderRow(new SlotLabel(" (", "if-bracket-opening"), ifCondition, new SlotLabel(")"));


        //Manvi Jain
//        ifCondition.setAccessibility("condition in if statement ");

        //cherry
        frameName = "if block";

    }


    /**
     * Construct an IfFrame by wrapping the given frames
     */
    public IfFrame(InteractionManager editor, List<Frame> contents)
    {
        this(editor);
        getFirstCanvas().getFirstCursor().insertFramesAfter(contents);

      // Manvi Jain
//        ifCondition.setAccessibility("condition in if statement ");
    }

    /**
     * Load an IfFrame using the given conditions and contents
     *
     * @param editor
     * @param condition
     * @param thenContents
     * @param elseIfConditions Same length as elseIfContents
     * @param elseIfContents If empty, no elseIfs.  Cannot be null.
     * @param elseContents If null, no finally.
     */
    public IfFrame(InteractionManager editor, ExpressionSlotFragment condition, List<Frame> thenContents,
                    List<FilledExpressionSlotFragment> elseIfConditions, List<List<Frame>> elseIfContents,
                    List<Frame> elseContents, boolean enabled)
    {
        this(editor, thenContents);
        ifCondition.setText(condition);
        for (int i = 0; i < elseIfContents.size(); i++)
        {
            addIntermediateCanvas(Arrays.asList(elseIfConditions.get(i)), elseIfContents.get(i));
        }
        if (elseContents != null)
        {
            addTailCanvas();
            elseContents.forEach(f -> getTailCanvas().insertBlockAfter(f, null));
        }
        frameEnabledProperty.set(enabled);

       // Manvi Jain
//        ifCondition.setAccessibility("condition in if statement ");
    }

    //cherry
    public String getScreenReaderText() {
        String condition;
        if (ifCondition.getText().equals("")) { condition = "blank"; } else { condition = ifCondition.getScreenreaderText(); }
        String text = "if frame with condition " + condition;
        return text;
    }

    //cherry
    /**
     * Get the help text of this frame, to pass to setAccessibilityHelp().
     * Calls the parent frame if there is one, to get the parent's description
     * plus the descriptions of that parent's parents.
     */
    public String getScreenReaderHelp() {
        String helpText = "you are ";

        helpText += getParentCanvas().getParentLocationDescription();

//        System.out.println(helpText);
        return helpText;
    }

    //cherry
    public String getLocationDescription(FrameCanvas c) {
        String condition, text;
        if (ifCondition.getText().equals("")) { condition = "blank"; } else { condition = ifCondition.getText(); }
        int sectionIndex = canvases.indexOf(c);
        if (sectionIndex==0) {
            // "then" section
            text = " in the 'then' section,";
        } else if (sectionIndex > 0 && sectionIndex < canvases.size()-1) {
            // "elseif" section
            text = " in the 'elseif' section with condition " + elseIfConditions.get(sectionIndex-1).getText() + ",";
        } else {
            // "else" section
            text = " in the 'else' section,";
        }
        text += " in an if frame with condition " + condition + ",";
        if (getParentCanvas()!=null && getParentCanvas().getParent() != null) {
            text += getParentCanvas().getParentLocationDescription();
        }
        //        System.out.println(text);
        return text;
    }


    @Override
    protected FrameContentRow getFrameContentRow(List<SlotFragment> slots, JavaCanvas canvas, int at)
    {
        FrameContentRow row = new FrameContentRow(this, "else-if-");
        ExpressionSlot<FilledExpressionSlotFragment> elseIfCondition = new FilledExpressionSlot(editor, this, this, row, "if-", FilledExpressionSlot.CONDITION_HINTS){
            @Override
            @OnThread(Tag.FXPlatform)
            public boolean backspaceAtStart()
            {
                if (isAlmostBlank()) {
                    pullUpCanvasContents(canvas.getFirstCursor().getUp(), canvas);
                    return true;
                }
                return super.backspaceAtStart();
            }
        };
        elseIfCondition.setSimplePromptText("condition");
        elseIfCondition.setTargetType("boolean");
        elseIfCondition.onTextPropertyChange(updateSidebarCurried("elseif "));

        if (slots != null) {
            if (slots.size() != 1) {
                Debug.printCallStack("slots has to include only the condition slot, but the size is " + slots.size());
            }
            elseIfCondition.setText((ExpressionSlotFragment) slots.get(0));
        }

        row.setHeaderItems(Arrays.asList(new SlotLabel("else if", "caption", "else-if-caption"),
                new SlotLabel(" (", "if-bracket-opening"),elseIfCondition, new SlotLabel(")")));

//        SlotLabel elseIfLabel = new SlotLabel("else if");
//        JavaFXUtil.addStyleClass(elseIfLabel, "divider-else");

        // TODO condition not added yet, waiting for a refactoring
//        HBox divider = new HBox(elseIfLabel.getNode(), new Label(" ("));
//        divider.getChildren().addAll(elseIfCondition.getComponents());
//        divider.getChildren().add(new Label(")"));
//        addCanvas(divider, elseIfCanvas);

        elseIfConditions.add(at, elseIfCondition); //add condition to the main ifFrame.


        elseIfCondition.setAccessibility("else if statement condition");
        return row;
    }

    @Override
    public boolean focusWhenJustAdded()
    {
        ifCondition.requestFocus();
        return true;
    }

    public static FrameFactory<IfFrame> getFactory()
    {
        return new FrameFactory<IfFrame>() {
            @Override
            public IfFrame createBlock(InteractionManager editor)
            {
                return new IfFrame(editor);
            }
            
            @Override
            public IfFrame createBlock(InteractionManager editor, List<Frame> contents)
            {
                return new IfFrame(editor, contents);
            }

            @Override
            public Class<IfFrame> getBlockClass()
            {
                return IfFrame.class;
            }
        };
    }

    protected SandwichCanvasesElement regenerateCodeElement(List<CodeElement> firstCanvasContents,
                 List<List<CodeElement>> intermediateCanvasesContents, List<CodeElement> tailCanvasContents, boolean enabled)
    {
        List<FilledExpressionSlotFragment> elseIfConditionsCode = Utility.mapList(elseIfConditions, ExpressionSlot::getSlotElement);
        return new IfElement(this, ifCondition.getSlotElement(), firstCanvasContents, elseIfConditionsCode,
                intermediateCanvasesContents, tailCanvasContents, enabled);
    }


    //manvi
    @Override
    public void updateAppearance(FrameCanvas parentCanvas)
    {
        super.updateAppearance(parentCanvas);
        if(getParentCanvas() != null && getParentCanvas().getParent() != null)
        {
            ifCondition.setAccessibilityHelpSlots("if condition slot");
            for (ExpressionSlot slot : elseIfConditions) {
                slot.setAccessibilityHelpSlots("else if condition slot");
            }
        }
    }


    //Manvi jain
    @Override
    public String getHelpContext()
    {
        String parent = "";
        if(getParentCanvas() != null && getParentCanvas().getParent() != null)
        {
            parent = getParentCanvas().getParent().getHelpContext();
        }
        return "in if statement " + parent;
    }


}
