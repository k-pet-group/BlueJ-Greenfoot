/*
 This file is part of the BlueJ program. 
 Copyright (C) 2021  Michael Kolling and John Rosenberg

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
package bluej.editor.base;

import bluej.editor.base.BaseEditorPane;
import bluej.editor.flow.FlowEditorPane;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An interface for positions within editor components.  Used by {@link BaseEditorPane} and implemented
 * differently by {@link FlowEditorPane} and the terminal.  A position is like a caret position; it is
 * between characters.
 */
@OnThread(Tag.FXPlatform)
public interface EditorPosition
{
    /**
     * Get the position within the overall complete text (0 = before first character).
     */
    public int getPosition();

    /**
     * Get the zero-based line that this position lies on (0 = first line)
     */
    public int getLine();

    /**
     * Get the zero-based column that this position lies in (0 = before first character on the line)
     */
    public int getColumn();
}
