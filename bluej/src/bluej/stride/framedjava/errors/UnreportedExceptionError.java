/*
 This file is part of the BlueJ program.
 Copyright (C) 2020 Michael Kölling and John Rosenberg

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
package bluej.stride.framedjava.errors;

import bluej.Config;
import bluej.compiler.Diagnostic.DiagnosticOrigin;
import bluej.editor.fixes.EditorFixesManager.FixSuggestionBase;
import bluej.editor.fixes.FixSuggestion;
import bluej.editor.stride.FrameEditor;
import bluej.stride.framedjava.ast.*;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.frames.*;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameContentRow;
import bluej.stride.generic.InteractionManager;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public class UnreportedExceptionError extends DirectSlotError
{
    private final List<FixSuggestion> corrections = new ArrayList<>();
    private final String exceptionType;

    /**
     * Creates an error about an unreported exception raised by a method.
     * The quick fixes are surrounding the method block with a try/catch or add a throws statement.
     *
     * @param slotFragment The fragment with the error.
     */
    @OnThread(Tag.FX)
    public UnreportedExceptionError(SlotFragment slotFragment, int startErrorPos, FrameEditor editor, String exceptionType, Set<String> vars)
    {
        super(slotFragment, DiagnosticOrigin.STRIDE_LATE);
        this.exceptionType = exceptionType;
        // Prepare the first correction: surrounding with try/catch
        // we first find a non-used variable name for the exception in the catch statement
        String exceptionVarNameRoot = exceptionType.substring(exceptionType.lastIndexOf(".") + 1).replaceAll("[^A-Z]", "").toLowerCase();
        boolean foundVarName = false;
        int varSuffix = 0;
        String exceptionVarName = exceptionVarNameRoot;
        do
        {
            foundVarName = vars.contains(exceptionVarName);
            if (foundVarName)
            {
                varSuffix++;
                exceptionVarName = exceptionVarNameRoot + varSuffix;
            }
        } while (foundVarName);

        // get frame informations for updates
        String finalExceptionVarName = exceptionVarName;
        final String catchStrideExpression = exceptionVarName + ".printStackTrace()";
        final InteractionManager finalInteractionMgr = editor.getSource().getEditor();
        final Frame errorFrame = ((CallExpressionSlotFragment) slotFragment).getSlot().getParentFrame();


        corrections.add(new FixSuggestionBase(Config.getString("editor.quickfix.unreportedException.fixMsg.trycatch"),
                () -> {
                    // prepare copy of the error frame and delete original
                    List<CodeElement> elements = GreenfootFrameUtil.getElementsForMultipleFrames(Collections.singletonList(errorFrame));
                    final Frame errorFrameCopy = elements.get(0).createFrame(finalInteractionMgr);

                    // Prepare try/catch block:
                    TryFrame newTryFrame = new TryFrame(finalInteractionMgr,
                            Collections.singletonList(errorFrameCopy),
                            Collections.singletonList(new TypeSlotFragment(exceptionType, exceptionType)),
                            Collections.singletonList(new NameDefSlotFragment(finalExceptionVarName)),
                            Collections.singletonList(Collections.singletonList(new CallFrame(finalInteractionMgr, catchStrideExpression, ""))),
                            null,
                            true);

                    // move to frame above the error frame
                    FrameContentRow currRow = (FrameContentRow) ((CallExpressionSlotFragment) slotFragment).getSlot().getSlotParent();
                    currRow.focusUp((((ExpressionSlotFragment) slotFragment).getSlot()), false);

                    finalInteractionMgr.beginRecordingState(finalInteractionMgr.getFocusedCursor());
                    // Add try/catch block
                    finalInteractionMgr.getFocusedCursor().insertBlockBefore(newTryFrame);
                    // delete the frame containing the error
                    errorFrame.getParentCanvas().removeBlock(errorFrame);
                    //position frame cursor above the catch content
                    newTryFrame.getIntermediateCanvases().get(0).getFirstCursor().requestFocus();
                    finalInteractionMgr.endRecordingState(finalInteractionMgr.getFocusedCursor());

                }));

        // Prepare the second correction: throws statement
        MethodFrameWithBody methodFrame = null;
        FrameCanvas c = errorFrame.getParentCanvas();
        while (c != null && c.getParent() != null && c.getParent().getFrame() != null)
        {
            if (c.getParent().getFrame() instanceof MethodFrameWithBody)
            {
                methodFrame = (MethodFrameWithBody) c.getParent().getFrame();
            }
            c = c.getParent().getFrame().getParentCanvas();
        }
        final MethodFrameWithBody finalMethodFrame = methodFrame;

        corrections.add(new FixSuggestionBase(Config.getString("editor.quickfix.unreportedException.fixMsg.throws"),
                () -> finalMethodFrame.addThrows(exceptionType)));
    }

    @Override
    @OnThread(Tag.Any)
    public String getMessage()
    {
        return Config.getString("editor.quickfix.unreportedException.errorMsg.part1") + exceptionType
                + Config.getString("editor.quickfix.unreportedException.errorMsg.part2");
    }

    @Override
    @OnThread(Tag.Any)
    public int getItalicMessageStartIndex()
    {
        return getMessage().indexOf(exceptionType);
    }

    @Override
    @OnThread(Tag.Any)
    public int getItalicMessageEndIndex()
    {
        return getMessage().indexOf(exceptionType) + exceptionType.length();
    }

    @Override
    public List<? extends FixSuggestion> getFixSuggestions()
    {
        return corrections;
    }

    @Override
    public boolean isJavaPos()
    {
        return true;
    }
}
