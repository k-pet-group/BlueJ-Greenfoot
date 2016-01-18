/*
 This file is part of the BlueJ program.
 Copyright (C) 2016 Michael Kölling and John Rosenberg

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

import bluej.stride.generic.Frame;
import bluej.stride.generic.InteractionManager;
import bluej.stride.slots.EditableSlot;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;

import java.util.Arrays;
import java.util.List;

/**
 * An operation to toggle boolean properties
 *
 * @author Amjad Altadmri
 */
public class ToggleBooleanProperty extends FrameOperation
{
    private String name;
    private SimpleStringProperty label;

    public ToggleBooleanProperty(InteractionManager editor, String identifier, String name, KeyCode keyCode)
    {
        super(editor, identifier, Combine.ALL, new KeyCodeCombination(keyCode));
        this.name = name;
        this.label = new SimpleStringProperty("Toggle " + name);
    }

    public ToggleBooleanProperty(InteractionManager editor, String identifier, String name)
    {
        this(editor, identifier, name, null);
    }

    /**
     * If all values of specific modifier in targeted frames true,
     * make them false. Otherwise, make all true.
     *
     * @param frames targeted frames that will receive the operation
     */
    @Override
    protected void execute(List<Frame> frames)
    {
        frames.forEach(f -> f.setModifier(name, !targetedAllTrue(frames)));
    }

    @Override
    public List<ItemLabel> getLabels()
    {
        return Arrays.asList(new ItemLabel(label, EditableSlot.MenuItemOrder.TOGGLE_BOOLEAN));
    }

    @Override
    public void onMenuShowing(CustomMenuItem item)
    {
        super.onMenuShowing(item);
        updateName();
    }

    private void updateName()
    {
        label.set(targetedAllTrue(editor.getSelection().getSelected()) ? "Remove " + name : "Make " + name);
    }

    private boolean targetedAllTrue(List<Frame> frames)
    {
        return frames.stream().allMatch(f -> f.getModifier(name).get());
    }

    @Override
    public boolean onlyOnContextMenu()
    {
        return true;
    }
}