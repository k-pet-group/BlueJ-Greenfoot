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

import bluej.utility.Utility;
import bluej.utility.javafx.binding.ConcatListBinding;
import bluej.utility.javafx.binding.DeepListBinding;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is a base class that is useful for when you have a possible multi-select and you want
 * to show a context menu with the appropriate operations depending on whether it's a single
 * selection, or a multi-selection.
 * 
 * This class ultimately maps to an item in the context menu.  The details held by this class
 * are to support combining or eliminating menu items in a multi-select.  When a context menu
 * is triggered, we ask all selected "things" (which all extend the ContextualItem inner class)
 * for their supported operations.  These operations can have one of three possible Combine enum values:
 *  - ANY: will appear in the context menu if any selected items support it,
 *      e.g. Compile.  Useful if you want to be able to do it on multiple items
 *      but it's not a problem if there's an item in the selection which doesn't support it.
 *  - ALL: will appear in the context menu only if all selected items support it,
 *      e.g. Cut.  Useful if you want to be able to do it on multiple items, but it
 *      only makes sense if the whole selection supports it.
 *  - ONE: will appear in the context menu only if there is exactly one item selected,
 *      e.g. Set Image.  Useful for operations where a manual follow-up is needed
 *      (such as picking the image) per-item, and thus it doesn't make sense to do it
 *      in bulk, or for operations like Inspect where the user is quite unlikely to
 *      want to do it on a large selection of objects.  Also for things like invoking
 *      constructors, where it will never be shared between objects.
 *      
 *  Sometimes the combine value is a bit of a heuristic, based on guesses at what users
 *  would expect.  For example, Compile could be ALL or ANY, the choice of ANY is
 *  mainly convenience (it it was ALL and you accidentally select the README, then
 *  you wouldn't see Compile in the list, which is a bit frustrating).
 *  
 *  AbstractOperation instances are identified by their identifier for purposes of combining.
 *  If two AbstractOperation instances have the same identifier, they are assumed to be
 *  totally interchangeable.  So in general instances should not keep outer state
 *  (unless they use Combine.ONE which means they will only be used for the single instance
 *  they arise from).  An operation acts on the instances passed to the activate method,
 *  not just the ContextualItem that created them.
 */
@OnThread(Tag.FXPlatform)
public abstract class AbstractOperation<ITEM extends AbstractOperation.ContextualItem<ITEM>>
{
    protected final KeyCombination shortcut;

    @OnThread(Tag.FXPlatform)
    public static interface ContextualItem<ITEM extends ContextualItem<ITEM>>
    {
        /**
         * Get the list of operations supported by this item.
         */
        public List<? extends AbstractOperation<ITEM>> getContextOperations();
    }

    private static <ITEM extends ContextualItem<ITEM>> MenuItems asMenuItems(Supplier<List<ITEM>> getSelected, List<? extends AbstractOperation<ITEM>> originalOps, int depth, boolean contextMenu)
    {
        // Only keep ones that fit context menu flag:
        List<AbstractOperation<ITEM>> ops = originalOps.stream().filter(op -> contextMenu || !op.onlyOnContextMenu()).collect(Collectors.toList());

        List<SortedMenuItem> r = new ArrayList<>();
        Set<ItemLabel> subMenuNames = ops.stream().filter(op -> op.getLabels().size() > depth + 1).map(op -> op.getLabels().get(depth)).collect(Collectors.toSet());
        subMenuNames.forEach(subMenuName -> {
            final MenuItems menuItems = asMenuItems(getSelected, ops.stream().filter(op -> op.getLabels().get(depth).equals(subMenuName)).collect(Collectors.toList()), depth + 1, contextMenu);
            Menu subMenu = menuItems.makeSubMenu();
            subMenu.textProperty().bind(subMenuName.getLabel());
            r.add(new SortedMenuItem(subMenu, subMenuName.getOrder()));
        });
        
        List<AbstractOperation<ITEM>> opsAtRightLevel = ops.stream().filter(op -> op.getLabels().size() == depth + 1).collect(Collectors.toList());

        Map<AbstractOperation<ITEM>, SortedMenuItem> opsAtRightLevelItems = new IdentityHashMap<>();

        for (AbstractOperation<ITEM> op : opsAtRightLevel)
        {
            SortedMenuItem item = op.getMenuItem(contextMenu, getSelected);
            r.add(item);
            opsAtRightLevelItems.put(op, item);
        }
        
        return new MenuItems(FXCollections.observableArrayList(r)) {

            @Override
            @OnThread(Tag.FXPlatform)
            public void onShowing()
            {
                opsAtRightLevel.forEach(op -> {
                    final SortedMenuItem sortedMenuItem = opsAtRightLevelItems.get(op);
                    final MenuItem item = sortedMenuItem.getItem();
                    if (item instanceof CustomMenuItem)
                        op.onMenuShowing((CustomMenuItem) item);
                });
            }

            @Override
            @OnThread(Tag.FXPlatform)
            public void onHidden()
            {
                opsAtRightLevel.forEach(op -> {
                    final SortedMenuItem sortedMenuItem = opsAtRightLevelItems.get(op);
                    final MenuItem item = sortedMenuItem.getItem();
                    if (item instanceof CustomMenuItem)
                        op.onMenuHidden((CustomMenuItem) item);
                });
            }
            
        };
    }

    /**
     * Gets the context menu items which are valid across the whole selection
     */
    @OnThread(Tag.FXPlatform)
    public static <ITEM extends ContextualItem<ITEM>> MenuItems getMenuItems(List<ITEM> selection, boolean contextMenu)
    {
        if (selection.size() == 0) {
            return new MenuItems(FXCollections.observableArrayList());
        }
        else if (selection.size() == 1) {
            // Everything appears as-is in a selection of size 1:
            return asMenuItems(() -> selection, selection.get(0).getContextOperations(), 0, contextMenu);
        }

        HashMap<String, List<AbstractOperation<ITEM>>> ops = new HashMap<>();
        for (ITEM f : selection)
        {
            for (AbstractOperation<ITEM> op : f.getContextOperations())
            {
                ops.computeIfAbsent(op.identifier, k -> new ArrayList<>()).add(op);
            }
        }

        List<AbstractOperation<ITEM>> r = new ArrayList<>();

        for (final List<AbstractOperation<ITEM>> opEntry : ops.values()) {
            // If all blocks had this operation:
            AbstractOperation<ITEM> frameOperation = opEntry.get(0);
            if ((frameOperation.combine() == Combine.ALL && opEntry.size() == selection.size())
                    || frameOperation.combine() == Combine.ANY
                    || (frameOperation.combine() == Combine.ONE && selection.size() == 1)) {
                r.add(frameOperation);
            }
        }
        return asMenuItems(() -> selection, r, 0, contextMenu);
    }

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

    @OnThread(Tag.FXPlatform)
    public void onMenuShowing(CustomMenuItem item) { }

    @OnThread(Tag.FXPlatform)
    public void onMenuHidden(CustomMenuItem item) { }

    /**
     * Actually performs the operation on the selected list.  For Combine.ONE items
     * this list will always be length 1.  For ANY and ALL, it could be any length,
     * and for ANY it may contain items that do not support this operation, so you
     * may need to filter accordingly (e.g. with instanceof checks or other guards)
     */
    public abstract void activate(List<ITEM> items);

    @OnThread(Tag.FXPlatform)
    protected void enablePreview() { }

    @OnThread(Tag.FXPlatform)
    protected void disablePreview() { }

    /**
     * Should this item appear only on context menus?
     */
    public boolean onlyOnContextMenu()
    {
        return false;
    }

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

    // This is an ordering across all menus.  Really it's a sort of merge between frame menus and class target menus, but that's much easier
    // than having two separate enums and making everything generic on the enum
    public static enum MenuItemOrder
    {
        // The integer is a block number, which is used to group and add dividers.
        // The ordering of the block numbers doesn't matter, it just needs to be a different number for each block,
        // and blocks should only be of adjacent enum values.
        // Dividers will be shown between items that don't have the same integer value
        // The menu will be ordered by the order in this enum.
        CLOSE(0),
        UNDO(10), REDO(10),
        RECENT_VALUES(20),
        CUT(30), COPY(30), PASTE(30),
        DELETE(40), ENABLE_FRAME(40), DISABLE_FRAME(40),
        INSERT_FRAME(60),
        TRANSFORM(70), TOGGLE_BOOLEAN(70), TOGGLE_ABSTRACT(70), TOGGLE_EXTENDS(70), TOGGLE_IMPLEMENTS(70), OVERRIDE(70),
        GOTO_DEFINITION(80), GOTO_OVERRIDE(80), SHOW_HIDE_USES(80),
        
        TEST_ALL(100),
        RUN_FX(100),
        RUN_CONSTRUCTOR(103),
        RUN_METHOD(105),
        EDIT(110),
        COMPILE(110),
        INSPECT(110),
        REMOVE(110),
        SET_IMAGE(110),
        DUPLICATE(110),
        CONVERT_TO_STRIDE(110),
        CONVERT_TO_JAVA(110),
        MAKE_TEST_CASE(130),
        BENCH_TO_FIXTURE(130),
        FIXTURE_TO_BENCH(130),
        NEW_SUBCLASS(140),
        CREATE_TEST(140),;

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
    protected boolean enabled = true;
    @OnThread(Tag.FX)
    private boolean wideCustomItem = false;
    
    public AbstractOperation(String identifier, Combine combine, KeyCombination shortcut)
    {
        this.identifier = identifier;
        this.combine = combine;
        this.shortcut = shortcut;
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
    @OnThread(Tag.FX)
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
        item.getStyleClass().addAll(getStyleClasses());
        item.setDisable(!isEnabled());
        return item;
    }

    @OnThread(Tag.FX)
    public void setWideCustomItem(boolean wide)
    {
        wideCustomItem = wide;
    }

    protected MenuItem initializeNormalItem()
    {
        MenuItem item = new MenuItem();
        item.textProperty().bind(getLabels().get(getLabels().size() - 1).label);
        item.getStyleClass().addAll(getStyleClasses());
        item.setDisable(!isEnabled());
        return item;
    }

    // Can be over-ridden in subclasses
    protected final boolean isEnabled()
    {
        return enabled;
    }

    public KeyCombination getShortcut()
    {
        return shortcut;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    // Can be over-ridden in subclasses
    public List<String> getStyleClasses()
    {
        return List.of();
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
        
        public static <T extends Comparable<T>> ContextMenu makeContextMenu(Map<T, MenuItems> allItems)
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
}
