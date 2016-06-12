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
package bluej.utility.javafx;

import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;

import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Created by neil on 11/06/2016.
 */
@OnThread(Tag.FXPlatform)
public class GrowableList<T extends Node>
{
    private final FXPlatformSupplier<T> newNodeFactory;
    private final List<T> items = new ArrayList<T>();
    // Will always be of size (1 + 3 * items.size())
    // Positions 3n will be plus buttons
    // Positions 1 + 3n will be item
    // Positions 2 + 3n will be cross button
    private final ObservableList<Node> inclButtons = FXCollections.observableArrayList();

    public GrowableList(FXPlatformSupplier<T> newNodeFactory)
    {
        this.newNodeFactory = newNodeFactory;
        inclButtons.setAll(makePlusButton());
        addNewItem(0);
    }

    private Node makeCrossButton()
    {
        Button button = new Button("x");
        button.setOnAction(e -> {
            int index = inclButtons.indexOf(button);
            removeItem((index - 2) / 3);
        });
        HangingFlowPane.setBreakBefore(button, false);
        return button;
    }

    private void removeItem(int index)
    {
        items.remove(index);
        // Important to remove downwards as indexes will change:
        inclButtons.remove(index * 3 + 3);
        inclButtons.remove(index * 3 + 2);
        inclButtons.remove(index * 3 + 1);
    }

    private Node makePlusButton()
    {
        Button button = new Button("+");
        button.setOnAction(e -> {
            int index = inclButtons.indexOf(button);
            addNewItem(index / 3);
        });
        HangingFlowPane.setBreakBefore(button, true);
        return button;
    }

    private void addNewItem(int itemIndex)
    {
        T newItem = newNodeFactory.get();
        HangingFlowPane.setBreakBefore(newItem, false);
        items.add(itemIndex, newItem);
        inclButtons.add(itemIndex * 3 + 1, newItem);
        inclButtons.add(itemIndex * 3 + 2, makeCrossButton());
        inclButtons.add(itemIndex * 3 + 3, makePlusButton());
    }

    public T getItem(int index)
    {
        return items.get(index);
    }

    public ObservableList<? extends Node> getNodes()
    {
        return inclButtons;
    }

    public int size()
    {
        return items.size();
    }
}
