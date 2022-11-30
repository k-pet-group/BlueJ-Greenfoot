/*
 This file is part of the BlueJ program.
 Copyright (C) 2016,2020 Michael Kölling and John Rosenberg

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
import bluej.stride.generic.Frame;
import bluej.stride.generic.InteractionManager;
import bluej.utility.javafx.AbstractOperation;
import threadchecker.OnThread;
import threadchecker.Tag;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.CustomMenuItem;

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
    private final char key;

    public ToggleBooleanProperty(InteractionManager editor, String identifier, String name, char key)
    {
        super(editor, identifier, Combine.ALL, null);
        this.name = name;
        this.label = new SimpleStringProperty(Config.getString("frame.operation.toggle").replace("$", name));
        this.key = key;
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
        // The variable is created to solve a bug:
        // If the method is called inside lambda, the method return value may changes after toggling the first frame.
        boolean targetedAllTrue = targetedAllTrue(frames);
        frames.forEach(f -> f.setModifier(name, !targetedAllTrue));
    }

    @Override
    public List<ItemLabel> getLabels()
    {
        return Arrays.asList(new ItemLabel(label, AbstractOperation.MenuItemOrder.TOGGLE_BOOLEAN));
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void onMenuShowing(CustomMenuItem item)
    {
        super.onMenuShowing(item);
        updateName();
    }

    @OnThread(Tag.FXPlatform)
    private void updateName()
    {
        label.set(targetedAllTrue(editor.getSelection().getSelected()) ?
                Config.getString("frame.operation.remove").replace("$", name) :
                Config.getString("frame.operation.make").replace("$", name));
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

    public String getLabel()
    {
        return label.get();
    }

    public char getKey()
    {
        return key;
    }
}
