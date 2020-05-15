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
package bluej.utility.javafx;

import bluej.stride.slots.EditableSlot;
import bluej.utility.javafx.binding.DeepListBinding;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCombination;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * This is a base class that is useful for when you have a possible multi-select and you want
 * to show a context menu with the appropriate operations depending on whether it's a single
 * selection, or a multi-selection.
 */
public abstract class AbstractOperation<ITEM>
{
    protected final KeyCombination shortcut;

    public SortedMenuItem getMenuItem(boolean contextMenu, Supplier<List<ITEM>> getSelection)
    {
        MenuItem item;
        if (contextMenu)
        {
            CustomMenuItem customItem = initializeCustomItem();

            customItem.getContent().setOnMouseEntered(e -> enablePreview());
            customItem.getContent().setOnMouseExited(e -> disablePreview());

            item = customItem;
        }
        else
        {
            item = initializeNormalItem();
        }

        item.setOnAction(e -> {
            activate(getSelection.get());
            e.consume();
        });

        if (shortcut != null) {
            item.setAccelerator(shortcut);
        }

        return new SortedMenuItem(item, getLabels().get(0).getOrder());
    }

    protected abstract void activate(List<ITEM> items);

    @OnThread(Tag.FXPlatform)
    protected void enablePreview() { }

    @OnThread(Tag.FXPlatform)
    protected void disablePreview() { }

    /**
     * If you select multiple items, and right click, each will have
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

    // This is an ordering across all menus
    public static enum MenuItemOrder
    {
        // The integer is a block number, which is used to group and add dividers.
        // The ordering of the block numbers doesn't matter, it just needs to be a different number for each block,
        // and blocks should only be of adjacent enum values.
        CLOSE(0),
        UNDO(10), REDO(10),
        RECENT_VALUES(20),
        CUT(30), COPY(30), PASTE(30),
        DELETE(40), ENABLE_FRAME(40), DISABLE_FRAME(40),
        INSERT_FRAME(60),
        TRANSFORM(70), TOGGLE_BOOLEAN(70), TOGGLE_ABSTRACT(70), TOGGLE_EXTENDS(70), TOGGLE_IMPLEMENTS(70), OVERRIDE(70),
        GOTO_DEFINITION(80), GOTO_OVERRIDE(80), SHOW_HIDE_USES(80);

        private final int block;

        MenuItemOrder(int block)
        {
            this.block = block;
        }

        public SortedMenuItem item(MenuItem fxItem)
        {
            return new SortedMenuItem(fxItem, this);
        }

        public int getBlock()
        {
            return block;
        }
    }
    
    protected final String identifier;
    protected final Combine combine;
    private boolean wideCustomItem = false;
    
    public AbstractOperation(String identifier, Combine combine, KeyCombination shortcut)
    {
        this.identifier = identifier;
        this.combine = combine;
        this.shortcut = shortcut;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public Combine combine()
    {
        return combine;
    }

    /**
     * The label for a menu item
     */
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

    public static class SortedMenuItem
    {
        private final MenuItem item;
        private MenuItemOrder sortOrder;

        public SortedMenuItem(MenuItem item, MenuItemOrder sortOrder)
        {
            this.item = item;
            this.sortOrder = sortOrder;
        }

        public MenuItem getItem()
        {
            return item;
        }

        public MenuItemOrder getMenuItemOrder()
        {
            return sortOrder;
        }

        public static ObservableList<MenuItem> sortAndAddDividers(ObservableList<SortedMenuItem> primaryItems, List<SortedMenuItem> defaultItems)
        {
            ObservableList<MenuItem> r = FXCollections.observableArrayList();
            new DeepListBinding<MenuItem>(r) {
                @Override
                protected Stream<MenuItem> calculateValues()
                {
                    return calculateList(primaryItems, defaultItems);
                }

                @Override
                protected Stream<ObservableList<?>> getListenTargets()
                {
                    return Stream.of(primaryItems);
                }
            }.startListening();
            return r;
        }

        private static Stream<MenuItem> calculateList(ObservableList<SortedMenuItem> primaryItems, List<SortedMenuItem> defaultItems)
        {
            List<SortedMenuItem> all = new ArrayList<>(primaryItems);
            for (SortedMenuItem def : defaultItems)
            {
                // Add any defaults where their item is not already present:
                if (!all.stream().anyMatch(item -> item.getMenuItemOrder() == def.getMenuItemOrder()))
                    all.add(def);
            }
            all.sort(Comparator.comparing(a -> a.sortOrder));

            for (int i = 0; i < all.size() - 1; i++)
            {
                if (all.get(i).getMenuItemOrder().getBlock() != all.get(i+1).getMenuItemOrder().getBlock())
                {
                    all.add(i + 1, new SortedMenuItem(new SeparatorMenuItem(), null));
                    // then skip it:
                    i += 1;
                }
            }
            return all.stream().map(SortedMenuItem::getItem);
        }
    }
}
