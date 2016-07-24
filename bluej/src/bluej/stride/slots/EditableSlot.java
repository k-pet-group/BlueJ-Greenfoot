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
package bluej.stride.slots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.stride.framedjava.ast.JavaFragment;
import bluej.stride.framedjava.slots.UnderlineContainer;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.binding.DeepListBinding;

import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.ErrorShower;
import bluej.stride.framedjava.slots.ExpressionSlot;
import bluej.stride.generic.Frame;
import bluej.stride.generic.RecallableFocus;
import bluej.utility.Utility;
import bluej.utility.javafx.ErrorUnderlineCanvas.UnderlineInfo;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.binding.ConcatListBinding;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * EditableSlot is used to access functionality common to all slots.  EditableSlot extends HeaderItem,
 * but also several other interfaces (see their documentation).
 */
public interface EditableSlot extends HeaderItem, RecallableFocus, UnderlineInfo, ErrorShower, UnderlineContainer
{
    /**
     * Requests focus on the slot, at whatever position makes sense (should not perform a select-all)
     */
    default public void requestFocus() { requestFocus(Focus.LEFT); }

    /**
     * Requests focus at the given position.
     * @param on Where to focus: LEFT, RIGHT or ALL
     * @see bluej.stride.slots.Focus
     */
    public void requestFocus(Focus on);

    /**
     * Called by the editor to indicate that we have lost focus.  We can't just listen to when our components
     * lose focus, because focus may transfer from one component of the slot to another in the same slot (e.g. in
     * expression slots, cursor may move between text fields in the expression), so a component losing focus doesn't
     * always mean the whole slot has lost focus.  The editor keeps track of who has focus, and calls this method
     * on all slots which do not have focus.
     *
     * Note that this method may currently be called on a slot which did not have focus -- it is more like
     * "notifyHasNoFocus" than "youHadFocusButJustLostIt"
     */
    @OnThread(Tag.FXPlatform)
    public void lostFocus();

    /**
     * A property reflecting whether the field is "effectively focused"
     *
     * "Effectively focused" means that either the field has actual JavaFX GUI
     * focus, or code completion is showing for this slot, meaning it doesn't
     * have GUI focus, but for our purposes it is logically the focus owner
     * within the editor.
     */
    public ObservableBooleanValue effectivelyFocusedProperty();

    /**
     * Called to cleanup any state or overlays when the slot is going to be removed.
     * TODO May not be needed any more; might be covered by lostFocus
     */
    public void cleanup();

    /**
     * Called when the whole top level frame has been saved, so slots can perform any necessary updates
     * (e.g. method prompts)
     */
    @OnThread(Tag.FXPlatform)
    public void saved();

    // No need for any implementing classes to further override this:
    default public @Override
    EditableSlot asEditable() { return this; }

    /**
     * Gets the parent Frame of the slot
     * @return The parent frame
     */
    public Frame getParentFrame();

    /**
     * A method used to check/access this slot as an ExpressionSlot (nicer than using cast/instanceof)
     * @return Get this slot as an expression slot (type cast)
     */
    default public ExpressionSlot asExpressionSlot() { return null; }

    /**
     * Checks whether the slot is blank or close enough.  Definition is context-dependent on the slot
     * @return True, if the slot is (essentially) blank
     */
    public boolean isAlmostBlank();

    /**
     * The amount of effort (roughly, keypresses) required to create this slot's content
     *
     * See the documentation of Frame.calculateEffort for more information.
     */
    public int calculateEffort();

    public static enum TopLevelMenu { EDIT, VIEW }

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
    }

    public static class SortedMenuItem
    {
        private final MenuItem item;
        private MenuItemOrder sortOrder;

        private SortedMenuItem(MenuItem item, MenuItemOrder sortOrder)
        {
            this.item = item;
            this.sortOrder = sortOrder;
        }

        public MenuItem getItem()
        {
            return item;
        }

        private static final Comparator<SortedMenuItem> COMPARATOR = (a, b) -> a.sortOrder.compareTo(b.sortOrder);

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
            all.sort(COMPARATOR);

            for (int i = 0; i < all.size() - 1; i++)
            {
                if (all.get(i).getMenuItemOrder().block != all.get(i+1).getMenuItemOrder().block)
                {
                    all.add(i + 1, new SortedMenuItem(new SeparatorMenuItem(), null));
                    // then skip it:
                    i += 1;
                }
            }
            return all.stream().map(SortedMenuItem::getItem);
        }
    }

    /**
     * A class to keep track of items to display in a top-level/context menu.  If you want to listen
     * for the menu containing the items being shown or hidden, override the class and implement
     * onShowing/onHidden.
     */
    public static class MenuItems
    {
        protected final ObservableList<SortedMenuItem> items;
        
        public MenuItems(ObservableList<SortedMenuItem> items) { this.items = items; }

        @OnThread(Tag.FXPlatform)
        public void onShowing() {}

        @OnThread(Tag.FXPlatform)
        public void onHidden() {}
        
        public static MenuItems concat(MenuItems... src)
        {
            List<MenuItems> nonNull = Arrays.stream(src).filter(m -> m != null).collect(Collectors.toList());
            ObservableList<SortedMenuItem> joinedItems = FXCollections.observableArrayList();
            ConcatListBinding.bind(joinedItems, FXCollections.observableArrayList(nonNull.stream().map(m -> m.items).collect(Collectors.toList())));
            return new MenuItems(joinedItems) {
                @Override
                public void onShowing() {nonNull.forEach(MenuItems::onShowing);}
                @Override
                public void onHidden() {nonNull.forEach(MenuItems::onHidden);}
            };
        }
        
        public Menu makeSubMenu()
        {
            Menu menu = new Menu();
            JavaFXUtil.bindMap(menu.getItems(), items, SortedMenuItem::getItem, FXRunnable::run);
            menu.onShowingProperty().set(e -> onShowing());
            menu.onHiddenProperty().set(e -> onHidden());
            return menu;
        }
        
        public static ContextMenu makeContextMenu(Map<TopLevelMenu, MenuItems> allItems)
        {
            return makeContextMenu(allItems.entrySet().stream().sorted((a, b) -> a.getKey().compareTo(b.getKey())).map(e -> e.getValue()).collect(Collectors.toList()));
        }
        
        private static ContextMenu makeContextMenu(List<MenuItems> allItems)
        {
            ContextMenu menu = new ContextMenu();

            ObservableList<SortedMenuItem> sorted = FXCollections.observableArrayList();
            ConcatListBinding.bind(sorted, FXCollections.observableArrayList(Utility.mapList(allItems, MenuItems::getItems)));
            JavaFXUtil.bindList(menu.getItems(), SortedMenuItem.sortAndAddDividers(sorted, Collections.emptyList()));
            
            menu.onShowingProperty().set(e -> allItems.forEach(MenuItems::onShowing));
            menu.onHiddenProperty().set(e -> allItems.forEach(MenuItems::onHidden));

            return menu;
        }

        public boolean isEmpty()
        {
            return items.isEmpty();
        }

        public ObservableList<SortedMenuItem> getItems()
        {
            return items;
        }
    }

    /**
     * Gets the menu items that might appear in top-level menus or context menu.  If shown in a top-level
     * menu, the key on the Map is used to organise them; if
     * @param contextMenu Whether this is a context menu or top level
     * @return The menu items
     */
    default public Map<TopLevelMenu, MenuItems> getMenuItems(boolean contextMenu) { return Collections.emptyMap(); }

    /**
     * Gets the relevant graphical node related to the given error, used for scrolling to the error.
     * By default, just gets the first graphical component in the slot.
     * @param err The error to look for
     * @return The Node where the error is
     */
    @Override
    default public Node getRelevantNodeForError(CodeError err)
    {
        return getComponents().stream().findFirst().orElse(null);
    }

    /**
     * Adds the given error to the slot
     * @param error The error to add
     */
    @OnThread(Tag.FXPlatform)
    public void addError(CodeError error);

    /**
     * Removes any errors that were present during previous calls to flagErrorsAsOld,
     * and have not since been added with addError
     */
    @OnThread(Tag.FXPlatform)
    public void removeOldErrors();

    /**
     * Flags all errors as old.  Generally, the pattern is:
     *  - flagErrorsAsOld
     *  - addError [for all compile errors]
     *  - removeOldErrors [leaves those just added by addError]
     *
     * This avoids an annoying blinking out/in of errors that happens if we just did removeAll/add;
     * this way, an error that is still present, never gets removed
     */
    @OnThread(Tag.FXPlatform)
    public void flagErrorsAsOld();

    /**
     * Gets any errors currently on the slot
     * @return A stream of errors
     */
    @OnThread(Tag.FXPlatform)
    public Stream<CodeError> getCurrentErrors();

    /**
     * Gets the JavaFragment of code that corresponds to this slot
     * @return The Java fragment
     */
    public JavaFragment getSlotElement();

    /**
     * Makes the slots editable/non-editable, e.g. in the case that the surrounding frame is disabled.
     * @param editable True to make this editable
     */
    public void setEditable(boolean editable);

    /**
     * Checks whether the slot is editable (see setEditable, setView), e.g. for determining where to place focus next.
     * @return True if this is editable
     */
    public boolean isEditable();
}
