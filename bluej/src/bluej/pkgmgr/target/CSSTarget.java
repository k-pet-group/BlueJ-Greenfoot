/*
 This file is part of the BlueJ program. 
 Copyright (C) 2017  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr.target;

import java.io.File;
import java.util.Properties;

import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;

import bluej.Config;
import bluej.editor.Editor;
import bluej.editor.EditorManager;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PackageEditor;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A CSS file in the package directory.
 */
public class CSSTarget extends NonCodeEditableTarget
{
    private static final String openStr = Config.getString("pkgmgr.cssmenu.open");
    private static final String removeStr = Config.getString("pkgmgr.cssmenu.remove");


    private final File file;

    public CSSTarget(Package aPackage, File file)
    {
        super(aPackage, file.getName());
        this.file = file;

        JavaFXUtil.addStyleClass(pane, "css-target");
        JavaFXUtil.addStyleClass(pane, "css-target-id-" + file.getName().replace(".", "-"));
        Label name = new Label(file.getName());
        BorderPane.setAlignment(name, Pos.CENTER);
        pane.setTop(name);
    }

    @Override
    public @OnThread(Tag.FXPlatform) void doubleClick()
    {
        open();
    }

    /**
     * Disply the context menu.
     */
    @Override
    @OnThread(Tag.FXPlatform)
    public void popupMenu(int x, int y, PackageEditor graphEditor)
    {
        ContextMenu menu = createMenu();
        if (menu != null) {
            showingMenu(menu);
            menu.show(pane, x, y);
        }
    }

    /**
     * Construct a popup menu which displays all our parent packages.
     */
    @OnThread(Tag.FXPlatform)
    private ContextMenu createMenu()
    {
        MenuItem open = new MenuItem(openStr);
        open.setOnAction(e -> {
            open();
        });
        JavaFXUtil.addStyleClass(open, "class-action-inbuilt");
        ContextMenu contextMenu = new ContextMenu(open);

        MenuItem remove = new MenuItem(removeStr);
        remove.setOnAction(e ->
        {
            remove();
        });
        JavaFXUtil.addStyleClass(remove, "class-action-inbuilt");
        contextMenu.getItems().add(remove);

        return contextMenu;
    }

    @Override
    public void remove()
    {
        getPackage().removeTarget(this);
        file.delete();
    }

    @Override
    protected File getSourceFile()
    {
        return file;
    }

    @Override
    public Editor getEditor()
    {
        if(editor == null) {
            editor = EditorManager.getEditorManager().openText(
                    getSourceFile().getPath(),
                    getPackage().getProject().getProjectCharset(),
                    getSourceFile().getName(), getPackage().getProject()::getDefaultFXTabbedEditor);
        }
        return editor;
    }

    @Override
    public @OnThread(Tag.FXPlatform) boolean isMoveable()
    {
        return true;
    }

    @Override
    public @OnThread(Tag.FX) boolean isResizable()
    {
        return true;
    }
    @Override
    @OnThread(Tag.FXPlatform)
    public void save(Properties props, String prefix)
    {
        super.save(props, prefix);

        props.put(prefix + ".type", "CSSTarget");
    }
}
