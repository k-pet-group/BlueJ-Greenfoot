/*
 This file is part of the Greenfoot program.
 Copyright (C) 2015  Michael Kolling and John Rosenberg

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

import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import bluej.Config;
import bluej.utility.javafx.JavaFXUtil;

/**
 * Created by neil on 26/11/15.
 */
public abstract class TabMenuManager
{
    protected final FXTab tab;
    private final MenuItem contextMoveNew;
    private final Menu contextMoveMenu;
    protected final Menu mainMoveMenu;
    private final MenuItem mainMoveNew;

    public TabMenuManager(FXTab tab)
    {
        this.tab = tab;
        contextMoveMenu = JavaFXUtil.makeMenu(Config.getString("frame.classmenu.move"));
        contextMoveNew = JavaFXUtil.makeMenuItem(Config.getString("frame.classmenu.move.new"), () -> tab.getParent().moveToNewLater(tab), null);
        mainMoveNew = JavaFXUtil.makeMenuItem(Config.getString("frame.classmenu.move.new"), () -> tab.getParent().moveToNewLater(tab), null);
        mainMoveMenu = JavaFXUtil.makeMenu(Config.getString("frame.classmenu.move"));

        // We may not have a parent yet, so use runLater:
        Platform.runLater(() -> {
            updateMoveMenus();

            tab.setContextMenu(new ContextMenu(
                    contextMoveMenu
                    , JavaFXUtil.makeMenuItem(Config.getString("frame.classmenu.close"), () -> tab.getParent().close(tab), null)
            ));
        });
    }

    protected void updateMoveMenus()
    {
        // Have to do everything double because the menus don't share:
        ArrayList<MenuItem> classMoveItems = new ArrayList<>();
        ArrayList<MenuItem> contextMoveItems = new ArrayList<>();
        classMoveItems.add(mainMoveNew);
        contextMoveItems.add(contextMoveNew);
        mainMoveNew.setDisable(tab.getParent().hasOneTab());
        contextMoveNew.setDisable(tab.getParent().hasOneTab());
        List<FXTabbedEditor> allWindows = tab.getParent().getProject().getAllFXTabbedEditorWindows();
        if (allWindows.size() > 1)
        {
            classMoveItems.add(new SeparatorMenuItem());
            contextMoveItems.add(new SeparatorMenuItem());
            allWindows.stream().filter(w -> w != tab.getParent()).forEach(w -> {
                StringExpression itemText = new ReadOnlyStringWrapper(Config.getString("frame.classmenu.move.existing") + ": ").concat(w.titleProperty());
                contextMoveItems.add(JavaFXUtil.makeMenuItem(itemText, () -> tab.getParent().moveTabTo(tab, w), null));
                classMoveItems.add(JavaFXUtil.makeMenuItem(itemText, () -> tab.getParent().moveTabTo(tab, w), null));
            });
        }

        if (mainMoveMenu != null)
        {
            mainMoveMenu.getItems().setAll(classMoveItems);
        }
        if (contextMoveMenu != null)
        {
            contextMoveMenu.getItems().setAll(contextMoveItems);
        }
    }

    abstract List<Menu> getMenus();
}
