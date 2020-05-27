/*
 This file is part of the BlueJ program.
 Copyright (C) 2015,2016,2020 Michael Kölling and John Rosenberg

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
package bluej.editor.stride;

import java.util.Collections;
import java.util.Map;

import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCanvas;
import bluej.stride.generic.FrameCursor;
import bluej.stride.generic.RecallableFocus;
import bluej.stride.slots.EditableSlot;
import bluej.utility.Utility;
import bluej.utility.javafx.AbstractOperation;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A sum type that holds either an EditableSlot or a FrameCursor.
 *
 * Package-visible
 */
class CursorOrSlot
{
    // Exactly one of these two fields is null, and the other is non-null
    private final EditableSlot slot;
    private final FrameCursor cursor;

    public CursorOrSlot(EditableSlot slot)
    {
        if (slot == null) throw new NullPointerException();
        this.slot = slot;
        this.cursor = null;
    }

    public CursorOrSlot(FrameCursor cursor)
    {
        if (cursor == null) throw new NullPointerException();
        this.slot = null;
        this.cursor = cursor;
    }

    /**
     * Checks whether the frame cursor, or the frame that the slot belongs to,
     * is directly inside the given canvas.
     */
    public boolean isInsideCanvas(FrameCanvas canvas)
    {
        // Find the nearest canvas:
        FrameCanvas ourCanvas;
        if (cursor != null)
            ourCanvas = cursor.getParentCanvas();
        else
            ourCanvas = Utility.orNull(slot.getParentFrame(), Frame::getParentCanvas);

        return ourCanvas == canvas;
    }

    /**
     * Gets the frame that the slot belongs to, or the frame which the cursor's canvas is inside.
     */
    public Frame getParentFrame()
    {
        if (slot != null)
            return slot.getParentFrame();
        else
            return cursor.getParentCanvas().getParent().getFrame();
    }

    /**
     * Gets the menu items for this slot/cursor (for window menu when focused, or right-click menu)
     */
    @OnThread(Tag.FXPlatform)
    public Map<EditableSlot.TopLevelMenu, AbstractOperation.MenuItems> getMenuItems(boolean contextMenu)
    {
        return slot != null ? slot.getMenuItems(contextMenu) : Collections.singletonMap(EditableSlot.TopLevelMenu.EDIT, cursor.getMenuItems(false));
    }

    /**
     * Delegates to .equals on the cursor/slot
     */
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CursorOrSlot that = (CursorOrSlot) o;

        if (cursor != null ? !cursor.equals(that.cursor) : that.cursor != null) return false;
        if (slot != null ? !slot.equals(that.slot) : that.slot != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = slot != null ? slot.hashCode() : 0;
        result = 31 * result + (cursor != null ? cursor.hashCode() : 0);
        return result;
    }

    public boolean matchesSlot(EditableSlot s)
    {
        return this.slot == s;
    }

    // Null if item is a slot
    public FrameCursor getCursor()
    {
        return cursor;
    }

    public RecallableFocus getRecallableFocus()
    {
        if (cursor != null)
            return cursor;
        return slot;
    }
}
