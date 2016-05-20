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
package bluej.stride.operations;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.CustomMenuItem;
import bluej.stride.generic.FrameCursor;
import bluej.stride.generic.InteractionManager;

public abstract class FrameCursorOperation extends AbstractOperation
{
    public FrameCursorOperation(InteractionManager editor, String identifier, Combine combine)
    {
        super(editor, identifier, combine);
    }

    public abstract void execute(FrameCursor frameCursor);

    public CustomMenuItem getMenuItem(final FrameCursor frameCursor)
    {
        CustomMenuItem item = initializeCustomItem();
        // Delete (with hover preview)
        item.setOnAction(e -> {
            editor.beginRecordingState(frameCursor);
            execute(frameCursor);
            editor.endRecordingState(frameCursor);
            editor.getSelection().clear();
            e.consume();
        });

        return item;
    }

}
