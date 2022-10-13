/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016,2019  Michael Kolling and John Rosenberg
 
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
package bluej.utility.javafx;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * A set of controls which look like toggleable push buttons, of which
 * only one can be selected.  They are actually implemented as radio buttons,
 * to get the right interface interactions, hence the name of the class.
 * 
 * Useful if you small (2--5) fixed set of choices but want to display them
 * in one row, rather than down a column.
 * 
 * Note: the styling for this is currently in dialogs.css, so it won't work
 * outside a dialog unless you rearrange the CSS files.
 */
public class HorizontalRadio<T>
{
    /** The HBox containing the buttons */
    private final HBox hBox;
    /** A map from value to buttons. */
    private final IdentityHashMap<T, ButtonBase> buttonsByValue = new IdentityHashMap<>();
    /** The currently selected item. */
    private final SimpleObjectProperty<T> selected = new SimpleObjectProperty<T>();

    /**
     * Creates a set of buttons for the given list of choices.
     * 
     * Often this is an enum.  The text on each button is determined by calling toString on the item.
     */
    public HorizontalRadio(List<T> choices)
    {
        List<Node> buttons = new ArrayList<>();
        ToggleGroup toggleGroup = new ToggleGroup();
        for (int i = 0; i < choices.size(); i++)
        {
            T value = choices.get(i);
            ToggleButton button = new RadioButton(value.toString());
            buttonsByValue.put(value, button);
            JavaFXUtil.addChangeListener(button.selectedProperty(), sel -> {
                if (sel.booleanValue())
                    selected.set(value);
            });
            button.setToggleGroup(toggleGroup);
            JavaFXUtil.addStyleClass(button, "radio-check-border");
            button.maxWidthProperty().bind(button.prefWidthProperty());
            button.getStyleClass().addAll("toggle-button", i == 0 ? "left-pill" : (i == choices.size() - 1 ? "right-pill" : "center-pill"));
            buttons.add(button);
        }
        hBox = new HBox(buttons.toArray(new Node[0]));
        hBox.setAlignment(Pos.BASELINE_LEFT);
    }

    /**
     * Gets the actual GUI node containing all the buttons.
     */
    public HBox getButtons()
    {
        return hBox;
    }

    /**
     * Allows you to observe the current selection.
     */
    public ReadOnlyObjectProperty<T> selectedProperty()
    {
        return selected;
    }

    /**
     * Selects the given item.  This must be the same reference as one
     * of the items in the list passed to the constructor.
     */
    public void select(T item)
    {
        buttonsByValue.get(item).fire();
    }

    public void setDisable(boolean disabled)
    {
        for (ButtonBase button : buttonsByValue.values())
        {
            button.setDisable(disabled);
        }
    }
}
