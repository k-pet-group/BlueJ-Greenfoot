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
import java.util.List;
import java.util.stream.Collectors;

import bluej.stride.framedjava.ast.NameDefSlotFragment;
import bluej.stride.framedjava.ast.SlotFragment;
import bluej.stride.framedjava.ast.TypeSlotFragment;
import bluej.stride.framedjava.canvases.JavaCanvas;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.SandwichCanvasesElement;
import bluej.stride.framedjava.elements.TryElement;
import bluej.stride.framedjava.slots.TypeSlot;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameContentItem;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.FrameFactory;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.SandwichCanvasesFrame;
import bluej.stride.slots.EditableSlot;
import bluej.stride.slots.FocusParent;
import bluej.stride.slots.HeaderItem;
import bluej.stride.slots.SlotLabel;
import bluej.stride.slots.SlotTraversalChars;
import bluej.stride.slots.SlotValueListener;
import bluej.stride.slots.TypeCompletionCalculator;
import bluej.stride.slots.VariableNameDefTextSlot;
import bluej.utility.Debug;
import bluej.utility.Utility;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Container-block representing a try-catch statement.
 * @author Fraser McKay
 */
public class TryFrame extends SandwichCanvasesFrame
{
    private static final String TRY_STYLE_PREFIX = "try-";
    private final List<TypeSlot> catchTypes = new ArrayList<>();
    private final List<VariableNameDefTextSlot> catchVars = new ArrayList<>();

    /**
     * Default constructor.
     */
    private TryFrame(InteractionManager editor)
    {
        super(editor, "try", "catch", "finally", TRY_STYLE_PREFIX);
    }

    /**
     * Construct an TryFrame by wrapping the given frames
     */
    public TryFrame(InteractionManager editor, List<Frame> contents)
    {
        this(editor);
        getFirstCanvas().getFirstCursor().insertFramesAfter(contents);
    }

    /**
     *
     * @param editor
     * @param tryContents
     * @param catchTypes Same length as catchContents
     * @param catchNames Same length as catchContents
     * @param catchContents If empty, no catches.  Cannot be null.
     * @param finallyContents If null, no finally.
     */
    public TryFrame(InteractionManager editor, List<Frame> tryContents, List<TypeSlotFragment> catchTypes,
                    List<NameDefSlotFragment> catchNames, List<List<Frame>> catchContents, List<Frame> finallyContents,
                    boolean enabled)
    {
        this(editor, tryContents);
        for (int i = 0; i < catchContents.size(); i++)
        {
            addIntermediateCanvas(Arrays.asList(catchTypes.get(i), catchNames.get(i)), catchContents.get(i));
        }
        if (finallyContents != null)
        {
            addTailCanvas();
            finallyContents.forEach(f -> getTailCanvas().insertBlockAfter(f, null));
        }
        frameEnabledProperty.set(enabled);
    }

    @Override
    protected FrameContentRow getFrameContentRow(List<SlotFragment> slots, JavaCanvas canvas, int at)
    {
        FrameContentRow row = new FrameContentRow(this, "catch-");

        TypeSlot type = new TypeSlot(editor, this, this, row, TypeSlot.Role.THROWS_CATCH, "catch-type-");
        type.setSimplePromptText("type");
        type.addClosingChar(' ');

        VariableNameDefTextSlot var = new VariableNameDefTextSlot(editor, this, this, row, "catch-var-");
        var.setPromptText("name");
        var.addValueListener(new SlotValueListener() {
            @Override
            public boolean valueChanged(HeaderItem slot, String oldValue, String newValue, FocusParent<HeaderItem> parent) {
                if (newValue.contains(",")) {
                    return false;
                }
                if (newValue.contains(")")) {
                    if (newValue.endsWith(")")) {
                        parent.focusRight(var);
                    }
                    return false;
                }
                return true;
            }

            @Override
            public void backSpacePressedAtStart(HeaderItem slot) {
                // Just move left, to end of type slot:
                row.focusLeft(slot);
            }

        });

        if (slots != null) {
            if (slots.size() != 2) {
                Debug.printCallStack("slots has to include the exception type and var name, but the size is " + slots.size());
            }
            type.setText((TypeSlotFragment) slots.get(0));
            var.setText((NameDefSlotFragment) slots.get(1));
        }

        row.setHeaderItems(Arrays.asList(new SlotLabel("catch", "catch-caption", "caption"),
                new SlotLabel(" (", "if-bracket-opening"), type, var, new SlotLabel(")")));

        catchTypes.add(at, type);
        catchVars.add(at, var);
        return row;
    }

    @Override
    public @OnThread(Tag.FXPlatform) boolean backspaceAtStart(FrameContentItem srcRow, HeaderItem src)
    {
        if (catchTypes.contains(src))
        {
            if (((FrameContentRow)srcRow).getSlotsDirect().allMatch(EditableSlot::isAlmostBlank)) {
                FrameCanvas canvas = getCanvases().collect(Collectors.toList()).get(1 + catchTypes.indexOf(src));
                pullUpCanvasContents(canvas.getFirstCursor().getUp(), canvas);
            }
        }
        return super.backspaceAtStart(srcRow, src);
    }

    public static FrameFactory<TryFrame> getFactory()
    {
        return new FrameFactory<TryFrame>() {
            @Override
            public TryFrame createBlock(InteractionManager editor)
            {
                TryFrame tryFrame = new TryFrame(editor);
                tryFrame.addIntermediateCanvas();
                tryFrame.getFirstCanvas().getFirstCursor().requestFocus();
                return tryFrame;
            }

            @Override
            public TryFrame createBlock(InteractionManager editor, List<Frame> contents)
            {
                final TryFrame tryFrame = new TryFrame(editor, contents);
                tryFrame.addIntermediateCanvas();
                tryFrame.getFirstCanvas().getFirstCursor().requestFocus();
                return tryFrame;
            }

            @Override
            public Class<TryFrame> getBlockClass()
            {
                return TryFrame.class;
            }
        };
    }

    @Override
    protected SandwichCanvasesElement regenerateCodeElement(List<CodeElement> firstCanvasContents,
             List<List<CodeElement>> intermediateCanvasesContents, List<CodeElement> tailCanvasContents, boolean enabled)
    {
        return new TryElement(this, firstCanvasContents, Utility.mapList(catchTypes, TypeSlot::getSlotElement),
                Utility.mapList(catchVars, VariableNameDefTextSlot::getSlotElement), intermediateCanvasesContents,
                tailCanvasContents, enabled);
    }
}