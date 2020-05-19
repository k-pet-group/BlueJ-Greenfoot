/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016,2017,2019,2020  Michael Kolling and John Rosenberg

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
package bluej.editor.stride;

import bluej.Config;
import bluej.pkgmgr.print.PrintProgressDialog;
import bluej.prefmgr.PrefMgr;
import bluej.stride.generic.Frame;
import bluej.stride.generic.Frame.View;
import bluej.stride.slots.EditableSlot;
import bluej.utility.javafx.AbstractOperation.SortedMenuItem;
import bluej.utility.DialogManager;
import bluej.utility.Utility;
import bluej.utility.javafx.AbstractOperation;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.print.PrinterJob;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A class to manage the menus for a frame editor.
 */
class FrameMenuManager extends TabMenuManager
{
    // The editor we are managing the menu for:
    private final FrameEditorTab editor;

    // The edit menu items which are always shown:
    private final List<SortedMenuItem> defaultEditItems;
    // The edit menu items which depend on where the focus currently lies:
    private final ObservableList<SortedMenuItem> contextualEditItems = FXCollections.observableArrayList();
    // The view menu items which depend on where the focus currently lies.
    // Note: currently unused!
    private final ObservableList<SortedMenuItem> extraViewItems = FXCollections.observableArrayList();
    // An action to unbind the current binding on contextualEditItems.  May be null if no binding.
    private FXRunnable unbindEditItems;
    // An action to unbind the current binding on extraViewItems.  May be null if no binding.
    private FXRunnable unbindViewItems;
    // A listener for when the edit menu is shown:
    private AbstractOperation.MenuItems editMenuListener;
    // A listener for when the view menu is shown:
    private AbstractOperation.MenuItems viewMenuListener;
    // A list of all the menus to display in the menu bar
    private List<Menu> menus = null;
    // Keeps track of whether Java preview mode is currently enabled:
    private final BooleanProperty javaPreviewShowing;

    FrameMenuManager(FrameEditorTab editor)
    {
        super(editor);
        this.editor = editor;
        this.javaPreviewShowing = new SimpleBooleanProperty(editor.getView() == View.JAVA_PREVIEW);
        // I don't think this will cause a loop with notifyView because it converges (second listener sets property to current value):
        JavaFXUtil.addChangeListener(javaPreviewShowing, b -> {
            if (b) JavaFXUtil.runNowOrLater(() -> editor.enableJavaPreview(Frame.ViewChangeReason.MENU_OR_SHORTCUT));
            else JavaFXUtil.runNowOrLater(() -> editor.disableJavaPreview(Frame.ViewChangeReason.MENU_OR_SHORTCUT));
        });

        defaultEditItems = Arrays.asList(
                AbstractOperation.MenuItemOrder.UNDO.item(JavaFXUtil.makeMenuItem(Config.getString("editor.undoLabel"), editor::undo, new KeyCodeCombination(KeyCode.Z, KeyCodeCombination.SHORTCUT_DOWN))),
                AbstractOperation.MenuItemOrder.REDO.item(JavaFXUtil.makeMenuItem(Config.getString("editor.redoLabel"), editor::redo, Config.isMacOS() ? new KeyCodeCombination(KeyCode.Z, KeyCodeCombination.SHORTCUT_DOWN, KeyCodeCombination.SHIFT_DOWN) : new KeyCodeCombination(KeyCode.Y, KeyCodeCombination.SHORTCUT_DOWN))),
                AbstractOperation.MenuItemOrder.CUT.item(JavaFXUtil.makeDisabledMenuItem(Config.getString("editor.cutLabel"), new KeyCodeCombination(KeyCode.X, KeyCodeCombination.SHORTCUT_DOWN))),
                AbstractOperation.MenuItemOrder.COPY.item(JavaFXUtil.makeDisabledMenuItem(Config.getString("editor.copyLabel"), new KeyCodeCombination(KeyCode.C, KeyCodeCombination.SHORTCUT_DOWN))),
                AbstractOperation.MenuItemOrder.PASTE.item(JavaFXUtil.makeDisabledMenuItem(Config.getString("editor.pasteLabel"), new KeyCodeCombination(KeyCode.V, KeyCodeCombination.SHORTCUT_DOWN)))
        );

    }

    void notifyView(View v)
    {
        javaPreviewShowing.set(v == View.JAVA_PREVIEW);
    }

    @OnThread(Tag.FXPlatform)
    List<Menu> getMenus()
    {
        if (menus == null)
        {
            // The edit menu consists of defaultEditItems plus contextualEditItems:
            Menu editMenu = JavaFXUtil.makeMenu(Config.getString("frame.editmenu.title"));
            JavaFXUtil.bindList(editMenu.getItems(), AbstractOperation.SortedMenuItem.sortAndAddDividers(contextualEditItems, defaultEditItems));
            editMenu.setOnShowing(e -> Utility.ifNotNull(editMenuListener, AbstractOperation.MenuItems::onShowing));
            editMenu.setOnHidden(e -> Utility.ifNotNull(editMenuListener, AbstractOperation.MenuItems::onHidden));

            MenuItem birdsEyeItem = JavaFXUtil.makeMenuItem("", editor::enableCycleBirdseyeView, new KeyCharacterCombination("d", KeyCombination.SHORTCUT_DOWN));
            birdsEyeItem.textProperty().bind(new StringBinding()
            {
                {super.bind(editor.viewProperty());}
                @Override
                protected String computeValue()
                {
                    switch (editor.viewProperty().get())
                    {
                        case BIRDSEYE_NODOC: return Config.getString("frame.viewmenu.birdseye.doc");
                        default: return Config.getString("frame.viewmenu.birdseye");
                    }
                }
            });

            FXPlatformConsumer<? super Boolean> frameCatalogueShownListener = newValue ->
                    editor.recordShowHideFrameCatalogue(newValue, FrameCatalogue.ShowReason.MENU_OR_SHORTCUT);
            ObservableList<MenuItem> standardViewMenuItems = FXCollections.observableArrayList(
                    JavaFXUtil.makeMenuItem(Config.getString("frame.viewmenu.nextError"), editor::nextError, new KeyCharacterCombination("k", KeyCombination.SHORTCUT_DOWN))
                    ,new SeparatorMenuItem()
                    ,JavaFXUtil.makeCheckMenuItem(Config.getString("frame.viewmenu.cheatsheet"), editor.cheatSheetShowingProperty(), new KeyCodeCombination(KeyCode.F1), frameCatalogueShownListener)
                    ,birdsEyeItem
                    ,JavaFXUtil.makeCheckMenuItem(Config.getString("frame.viewmenu.java"), javaPreviewShowing, new KeyCharacterCombination("j", KeyCombination.SHORTCUT_DOWN))
                    ,new SeparatorMenuItem()
                    ,JavaFXUtil.makeMenuItem(Config.getString("frame.viewmenu.fontbigger"), editor::increaseFontSize, new KeyCharacterCombination("=", KeyCombination.SHORTCUT_DOWN))
                    ,JavaFXUtil.makeMenuItem(Config.getString("frame.viewmenu.fontsmaller"), editor::decreaseFontSize, new KeyCharacterCombination("-", KeyCombination.SHORTCUT_DOWN))
                    ,JavaFXUtil.makeMenuItem(Config.getString("frame.viewmenu.fontdefault"), editor::resetFontSize, new KeyCharacterCombination("0", KeyCombination.SHORTCUT_DOWN))
            );

            Menu viewMenu = new Menu(Config.getString("frame.viewmenu.title"));
            //ConcatListBinding.bind(viewMenu.getItems(), FXCollections.observableArrayList(standardViewMenuItems /*, extraViewItems*/));
            JavaFXUtil.bindList(viewMenu.getItems(), standardViewMenuItems);
            viewMenu.setOnShowing(e -> Utility.ifNotNull(viewMenuListener, AbstractOperation.MenuItems::onShowing));
            viewMenu.setOnHidden(e -> Utility.ifNotNull(viewMenuListener, AbstractOperation.MenuItems::onHidden));

            updateMoveMenus();

            menus = Arrays.asList(
                    JavaFXUtil.makeMenu(Config.getString("frame.classmenu.title")
                            , mainMoveMenu
                            , JavaFXUtil.makeMenuItem(Config.getString("frame.classmenu.print"), () -> print(), new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN))
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

    /**
     * Print the Stride class of the associated editor.
     */
    @OnThread(Tag.FXPlatform)
    private void print()
    {
        PrinterJob job = JavaFXUtil.createPrinterJob();
        Window parentWindow = editor.getParent().getWindow();
        if (job == null)
        {
            DialogManager.showErrorFX(parentWindow,"print-no-printers");
        }
        else if (job.showPrintDialog(parentWindow))
        {
            PrintProgressDialog printProgressDialog = new PrintProgressDialog(parentWindow, false);
            FXRunnable printAction = editor.getFrameEditor().printTo(job, PrefMgr.PrintSize.STANDARD, false, false, printProgressDialog.getWithinFileUpdater());
            new Thread()
            {
                @Override
                @OnThread(value = Tag.FX, ignoreParent = true)
                public void run()
                {
                    printAction.run();
                    job.endJob();
                    printProgressDialog.finished();
                }
            }.start();
            printProgressDialog.showAndWait();
        }
    }

    // Updates our menu items using binding.
    void setMenuItems(Map<EditableSlot.TopLevelMenu, AbstractOperation.MenuItems> items)
    {
        if (unbindEditItems != null)
        {
            unbindEditItems.run();
            unbindEditItems = null;
            contextualEditItems.clear();
            editMenuListener = null;
        }

        AbstractOperation.MenuItems editItems = items.get(EditableSlot.TopLevelMenu.EDIT);
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

        AbstractOperation.MenuItems viewItems = items.get(EditableSlot.TopLevelMenu.VIEW);
        if (viewItems != null)
        {
            viewMenuListener = viewItems;
            unbindViewItems = JavaFXUtil.bindList(extraViewItems, viewItems.getItems());
        }
    }
}
