package bluej.editor.stride;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import bluej.Config;
import bluej.stride.generic.Frame.View;
import bluej.stride.slots.EditableSlot.MenuItemOrder;
import bluej.stride.slots.EditableSlot.SortedMenuItem;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import bluej.stride.slots.EditableSlot;
import bluej.utility.Utility;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;

/**
 * Created by neil on 25/03/15.
 */
class FrameMenuManager extends TabMenuManager
{
    private final FrameEditorTab editor;

    private final List<SortedMenuItem> defaultEditItems;
    private final ObservableList<SortedMenuItem> contextualEditItems = FXCollections.observableArrayList();
    private final ObservableList<SortedMenuItem> extraViewItems = FXCollections.observableArrayList();
    private FXRunnable unbindEditItems;
    private FXRunnable unbindViewItems;
    private EditableSlot.MenuItems editMenuListener;
    private EditableSlot.MenuItems viewMenuListener;
    private List<Menu> menus = null;
    private final BooleanProperty birdsEyeViewShowing;
    private final BooleanProperty javaPreviewShowing;

    FrameMenuManager(FrameEditorTab editor)
    {
        super(editor);
        this.editor = editor;
        this.birdsEyeViewShowing = new SimpleBooleanProperty(editor.getView() == View.BIRDSEYE);
        this.javaPreviewShowing = new SimpleBooleanProperty(editor.getView() == View.JAVA_PREVIEW);
        // I don't think this should go in a loop with notifyView because it converges (second listener sets property to current value):
        JavaFXUtil.addChangeListener(birdsEyeViewShowing, b -> {
            if (b) editor.enableBirdseyeView();
            else editor.disableBirdseyeView();
        });
        JavaFXUtil.addChangeListener(javaPreviewShowing, b -> {
            if (b) editor.enableJavaPreview();
            else editor.disableJavaPreview();
        });

        defaultEditItems = Arrays.asList(
                MenuItemOrder.UNDO.item(JavaFXUtil.makeMenuItem(Config.getString("editor.undoLabel"), editor::undo, new KeyCodeCombination(KeyCode.Z, KeyCodeCombination.SHORTCUT_DOWN))),
                MenuItemOrder.REDO.item(JavaFXUtil.makeMenuItem(Config.getString("editor.redoLabel"), editor::redo, Config.isMacOS() ? new KeyCodeCombination(KeyCode.Z, KeyCodeCombination.SHORTCUT_DOWN, KeyCodeCombination.SHIFT_DOWN) : new KeyCodeCombination(KeyCode.Y, KeyCodeCombination.SHORTCUT_DOWN))),
                MenuItemOrder.CUT.item(JavaFXUtil.makeDisabledMenuItem(Config.getString("editor.cutLabel"), new KeyCodeCombination(KeyCode.X, KeyCodeCombination.SHORTCUT_DOWN))),
                MenuItemOrder.COPY.item(JavaFXUtil.makeDisabledMenuItem(Config.getString("editor.copyLabel"), new KeyCodeCombination(KeyCode.C, KeyCodeCombination.SHORTCUT_DOWN))),
                MenuItemOrder.PASTE.item(JavaFXUtil.makeDisabledMenuItem(Config.getString("editor.pasteLabel"), new KeyCodeCombination(KeyCode.V, KeyCodeCombination.SHORTCUT_DOWN)))
        );

    }

    void notifyView(View v)
    {
        birdsEyeViewShowing.set(v == View.BIRDSEYE);
        javaPreviewShowing.set(v == View.JAVA_PREVIEW);
    }

    List<Menu> getMenus()
    {
        if (menus == null)
        {
            Menu editMenu = JavaFXUtil.makeMenu(Config.getString("frame.editmenu.title"));
            JavaFXUtil.bindList(editMenu.getItems(), SortedMenuItem.sortAndAddDividers(contextualEditItems, defaultEditItems));
            editMenu.setOnShowing(e -> Utility.ifNotNull(editMenuListener, EditableSlot.MenuItems::onShowing));
            editMenu.setOnHidden(e -> Utility.ifNotNull(editMenuListener, EditableSlot.MenuItems::onHidden));

            ObservableList<MenuItem> standardViewMenuItems = FXCollections.observableArrayList(
                    JavaFXUtil.makeMenuItem(Config.getString("frame.viewmenu.nextError"), editor::nextError, new KeyCharacterCombination("k", KeyCombination.SHORTCUT_DOWN))
                    ,new SeparatorMenuItem()
                    ,JavaFXUtil.makeCheckMenuItem(Config.getString("frame.viewmenu.cheatsheet"), editor.cheatSheetShowingProperty(), new KeyCodeCombination(KeyCode.F1))
                    ,JavaFXUtil.makeCheckMenuItem(Config.getString("frame.viewmenu.birdseye"), birdsEyeViewShowing, new KeyCharacterCombination("d", KeyCombination.SHORTCUT_DOWN))
                    ,JavaFXUtil.makeCheckMenuItem(Config.getString("frame.viewmenu.java"), javaPreviewShowing, new KeyCharacterCombination("j", KeyCombination.SHORTCUT_DOWN))
                    ,new SeparatorMenuItem()
                    ,JavaFXUtil.makeMenuItem(Config.getString("frame.viewmenu.fontbigger"), editor::increaseFontSize, new KeyCharacterCombination("=", KeyCombination.SHORTCUT_DOWN))
                    ,JavaFXUtil.makeMenuItem(Config.getString("frame.viewmenu.fontsmaller"), editor::decreaseFontSize, new KeyCharacterCombination("-", KeyCombination.SHORTCUT_DOWN))
                    ,JavaFXUtil.makeMenuItem(Config.getString("frame.viewmenu.fontdefault"), editor::resetFontSize, new KeyCharacterCombination("0", KeyCombination.SHORTCUT_DOWN))
            );

            Menu viewMenu = new Menu(Config.getString("frame.viewmenu.title"));
            //ConcatListBinding.bind(viewMenu.getItems(), FXCollections.observableArrayList(standardViewMenuItems /*, extraViewItems*/));
            JavaFXUtil.bindList(viewMenu.getItems(), standardViewMenuItems);
            viewMenu.setOnShowing(e -> Utility.ifNotNull(viewMenuListener, EditableSlot.MenuItems::onShowing));
            viewMenu.setOnHidden(e -> Utility.ifNotNull(viewMenuListener, EditableSlot.MenuItems::onHidden));

            updateMoveMenus();

            menus = Arrays.asList(
                    JavaFXUtil.makeMenu(Config.getString("frame.classmenu.title")
                            , mainMoveMenu
                            , JavaFXUtil.makeMenuItem(Config.getString("frame.classmenu.close"), () -> editor.getParent().close(editor), new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN))
                    ),
                    editMenu,
                    viewMenu
            );
        }
        else
        {
            updateMoveMenus();
        }
        return menus;
    }

    void setMenuItems(Map<EditableSlot.TopLevelMenu, EditableSlot.MenuItems> items)
    {
        if (unbindEditItems != null)
        {
            unbindEditItems.run();
            unbindEditItems = null;
            contextualEditItems.clear();
            editMenuListener = null;
        }

        EditableSlot.MenuItems editItems = items.get(EditableSlot.TopLevelMenu.EDIT);
        if (editItems != null) {
            editMenuListener = editItems;
            unbindEditItems = JavaFXUtil.bindList(contextualEditItems, editItems.getItems());
        }

        if (unbindViewItems != null)
        {
            unbindViewItems.run();
            unbindViewItems = null;
            extraViewItems.clear();
            viewMenuListener = null;
        }

        EditableSlot.MenuItems viewItems = items.get(EditableSlot.TopLevelMenu.VIEW);
        if (viewItems != null)
        {
            viewMenuListener = viewItems;
            unbindViewItems = JavaFXUtil.bindList(extraViewItems, viewItems.getItems());
        }
    }
}
