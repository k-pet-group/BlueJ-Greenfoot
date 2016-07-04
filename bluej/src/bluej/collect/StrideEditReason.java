/*
 This file is part of the BlueJ program.
 Copyright (C) 2016  Michael Kolling and John Rosenberg

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
package bluej.collect;

/**
 * Tracks the cause of an edit, i.e. what user interaction actually
 * caused the edit.  A mouse click, a shortcut key, etc.
 */
public enum StrideEditReason
{
    // A key was used to insert a frame which wrapped a selection:
    SELECTION_WRAP_KEY("selection_wrap_key"),
    // The frame catalogue/cheat sheet was clicked in order to wrap a selection:
    SELECTION_WRAP_CHEAT("selection_wrap_cheat"),
    // A key was used to insert a frame on its own:
    SINGLE_FRAME_INSERTION_KEY("frame_insert_key"),
    // The frame catalogue/cheat sheet was clicked in order to insert a single frame:
    SINGLE_FRAME_INSERTION_CHEAT("frame_insert_cheat"),
    // The right-click context menu was used on a frame cursor to insert a frame:
    SINGLE_FRAME_INSERTION_CONTEXT_MENU("frame_insert_menu"),

    DELETE_FRAMES_MENU("delete_frames_menu"),
    DELETE_FRAMES_KEY_BKSP("delete_frames_key_backspace"),
    DELETE_FRAMES_KEY_DELETE("delete_frames_key_delete"),

    // Frames have been moved or copied by dragging:
    FRAMES_DRAG("drag_frames"),
    // Frames have been moved or copied by dragging from shelf:
    FRAMES_DRAG_SHELF("drag_frames_shelf"),

    // Frame was removed because user pressed escape immediately after inserting a frame:
    ESCAPE_FRESH("escape_fresh"),

    // Code completion was used
    CODE_COMPLETION("code_completion"),

    // Undo/redo, either editor-wide (global) or using the local frame/slot undo.
    UNDO_GLOBAL("undo_global"),
    REDO_GLOBAL("redo_global"),
    UNDO_LOCAL("undo_local"),

    CUT_FRAMES("cut_frames"),
    PASTE_FRAMES("paste_frames"),

    // Just used to flush any old edits; no recorded reason for edit:
    FLUSH(null);

    private final String text;

    StrideEditReason(String text)
    {
        this.text = text;
    }

    public String getText()
    {
        return text;
    }
}
