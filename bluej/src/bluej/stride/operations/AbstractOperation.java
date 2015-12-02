/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015 Michael Kölling and John Rosenberg 
 
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

import bluej.stride.slots.EditableSlot.MenuItemOrder;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import bluej.stride.generic.InteractionManager;
import javafx.scene.control.MenuItem;

public abstract class AbstractOperation
{
    /**
     * If you select multiple frames, and right click, each will have
     * a set of operations that can be performed.  This enum determines how the different
     * sets should be combined:
     * 
     *   - ALL means that all sets must feature this item before it is offered
     *     (e.g. for copying)
     *   - ANY means that only one operation need have this available
     *     (e.g. disabling, where you want to allow disabling even if some frames are already disabled) 
     *   - ONE means that it only appears in single frame selections
     *     (e.g. frame transformations)
     */
    public enum Combine
    {
        ANY, ALL, ONE;
    }

    protected final String identifier;
    protected final Combine combine;
    private boolean wideCustomItem = false;
    protected InteractionManager editor;
    
    public AbstractOperation(InteractionManager editor, String identifier, Combine combine)
    {
        this.identifier = identifier;
        this.combine = combine;
        this.editor = editor;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public Combine combine()
    {
        return combine;
    }

    public static class ItemLabel
    {
        private ObservableValue<String> label;
        private MenuItemOrder order;

        public ItemLabel(ObservableValue<String> label, MenuItemOrder order)
        {
            this.label = label;
            this.order = order;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof ItemLabel)
            {
                return label.getValue().equals(((ItemLabel) obj).label.getValue());
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return label.getValue().hashCode();
        }

        public ObservableValue<String> getLabel()
        {
            return label;
        }

        public MenuItemOrder getOrder()
        {
            return order;
        }
    }

    /**
     * One item in list => appears at top-level
     * Two items => First item is sub-menu title, second item is label
     * .. and so on, but you surely don't want more than two levels!
     */
    public abstract List<ItemLabel> getLabels();
    
    // Helper function:
    protected ItemLabel l(String s, MenuItemOrder order)
    {
        return new ItemLabel(new ReadOnlyStringWrapper(s){
            @Override
            public boolean equals(Object obj)
            {
                if (!(obj instanceof ObservableValue<?>)){
                    return false;
                }
                return getValue().equals(((ObservableValue) obj).getValue());
            }
            
            @Override
            public int hashCode()
            {
                return getValue().hashCode();
            }
        }, order);
    }

    protected CustomMenuItem initializeCustomItem()
    {
        Label d = new Label();
        d.textProperty().bind(getLabels().get(getLabels().size() - 1).label);
        if (!wideCustomItem)
            d.setPrefWidth(150);
        else
            d.setPrefWidth(300);
        CustomMenuItem item = new CustomMenuItem(d);
        // TODO next line is added due to a bug in Mac SystemMenuBar where MenuItem text showed as blank. 
        item.textProperty().bind(d.textProperty());
        return item;
    }

    public void setWideCustomItem(boolean wide)
    {
        wideCustomItem = wide;
    }

    protected MenuItem initializeNormalItem()
    {
        MenuItem item = new MenuItem();
        item.textProperty().bind(getLabels().get(getLabels().size() - 1).label);
        return item;
    }
}
