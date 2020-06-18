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

import java.util.Collections;
import java.util.List;

import bluej.utility.javafx.AbstractOperation;
import bluej.stride.generic.Frame;
import bluej.stride.generic.InteractionManager;
import bluej.stride.generic.RecallableFocus;
import threadchecker.OnThread;
import threadchecker.Tag;

import javafx.scene.input.KeyCombination;

public abstract class FrameOperation extends AbstractOperation<Frame>
{
    protected final InteractionManager editor;

    public FrameOperation(InteractionManager editor, String identifier, Combine combine)
    {
        this(editor, identifier, combine, null);
    }

    public FrameOperation(InteractionManager editor, String identifier, Combine combine, KeyCombination shortcut)
    {
        super(identifier, combine, shortcut);
        this.editor = editor;
    }

    @OnThread(Tag.FXPlatform)
    public final void activate(Frame frame)
    {
        // TODO "editor.getSelection().getSelected()" fired a NPE, make next line activate(frame, null);
        activate(Collections.singletonList(frame));
    }

    @OnThread(Tag.FXPlatform)
    public final void activate(Frame frame, RecallableFocus focus)
    {
        activate(Collections.singletonList(frame), focus);
    }

    @OnThread(Tag.FXPlatform)
    public final void activate(List<Frame> frames)
    {
        Frame first = editor.getSelection().getSelected().isEmpty() ? null : editor.getSelection().getSelected().get(0);
        RecallableFocus focus = first == null ? null : first.getCursorBefore();
        activate(frames, focus);
    }

    @OnThread(Tag.FXPlatform)
    public final void activate(List<Frame> frames, RecallableFocus focus)
    {
        editor.beginRecordingState(focus);
        // Delete (with hover preview)
        disablePreview();
        execute(frames);
        editor.endRecordingState(focus);
    }

    /**
     * Executes the class operation on targeted frames
     *
     * @param frames targeted frames that will receive the operation
     */
    @OnThread(Tag.FXPlatform)
    protected abstract void execute(List<Frame> frames);

    public KeyCombination getShortcut()
    {
        return shortcut;
    }
}
