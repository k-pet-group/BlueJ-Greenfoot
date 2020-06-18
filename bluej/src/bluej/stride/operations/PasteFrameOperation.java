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
package bluej.stride.operations;

import bluej.Config;
import bluej.collect.StrideEditReason;
import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.frames.GreenfootFrameUtil;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCursor;
import bluej.stride.generic.InteractionManager;
import threadchecker.OnThread;
import threadchecker.Tag;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.Arrays;
import java.util.List;

public class PasteFrameOperation extends FrameOperation
{
    public PasteFrameOperation(InteractionManager editor)
    {
        super(editor, "PASTE", Combine.ALL, new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN));
    }

    @Override
    @OnThread(Tag.FXPlatform)
    protected void execute(List<Frame> frames)
    {
        List<CodeElement> elements = GreenfootFrameUtil.getClipboardElements(editor.getFocusedCursor().getParentCanvas().getContext());
        if (elements != null && elements.size() > 0) {
            editor.recordEdits(StrideEditReason.FLUSH);
            if (frames.size() > 0) {
                FrameCursor insertionCursor = frames.get(0).getCursorBefore();
                DeleteFrameOperation.deleteFrames(frames, editor);
                editor.getSelection().clear();
                insertionCursor.requestFocus();
            }

            boolean shouldDisable = !editor.getFocusedCursor().getParentCanvas().getParent().getFrame().isFrameEnabled();


            elements.forEach(codeElement -> {
                final Frame frame = codeElement.createFrame(editor);
                if (editor.getFocusedCursor().check().canPlace(frame.getClass()))
                {
                    editor.getFocusedCursor().insertBlockAfter(frame);
                    if (shouldDisable)
                    {
                        frame.setFrameEnabled(false);
                    }
                    frame.getCursorAfter().requestFocus();
                }
            });
            editor.recordEdits(StrideEditReason.PASTE_FRAMES);
        }
    }

    @Override
    public List<ItemLabel> getLabels()
    {
        return Arrays.asList(l(Config.getString("frame.operation.paste"), MenuItemOrder.PASTE));
    }
}
