/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2020 Michael Kölling and John Rosenberg 
 
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

import java.util.List;
import java.util.function.Consumer;

import bluej.stride.generic.Frame;
import bluej.stride.generic.InteractionManager;
import bluej.utility.Utility;

/**
 * Implementation of FrameOperation, designed to allow you to
 * pass a lambda as the execute method.
 */
public class CustomFrameOperation extends FrameOperation
{
    private List<ItemLabel> labels;
    private Consumer<List<Frame>> action;

    // Constructor for operations with customisable Combine
    public CustomFrameOperation(InteractionManager editor, String identifier, Combine combine, List<String> name, MenuItemOrder menuOrder, Consumer<List<Frame>> action)
    {
        super(editor, identifier, combine);
        this.labels = Utility.mapList(name, n -> l(n, menuOrder));
        this.action = action;
    }
    
    // Constructor for operations that only work on one frame (known a priori)
    public CustomFrameOperation(InteractionManager editor, String identifier, List<String> name, MenuItemOrder menuOrder, Frame f, Runnable action)
    {
        this(editor, identifier, Combine.ONE, name, menuOrder, frames -> {
            if (frames.size() != 1 || frames.get(0) != f) {
                // TODO Next condition is temporary until JDK bug fix
                if (frames.size() != 0)
                    throw new IllegalStateException();
            }
            action.run();
        });
    }
    
    @Override
    protected void execute(List<Frame> frames)
    {
        action.accept(frames);
    }

    @Override
    public List<ItemLabel> getLabels()
    {
        return labels;
    }

    @Override
    public boolean onlyOnContextMenu()
    {
        // Frame-specific operations only appear on the context menu:
        return true;
    }
}
