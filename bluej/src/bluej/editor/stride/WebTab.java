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

import java.util.Collections;
import java.util.List;
import javafx.beans.value.ObservableStringValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.web.WebView;

import bluej.Config;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A tab in the FXTabbedEditor which just contains a WebView.
 */
@OnThread(Tag.FX)
public class WebTab extends FXTab
{
    private final WebView browser;
    private FXTabbedEditor parent;
    private final TabMenuManager menuManager;

    /**
     * Constructs a WebTab with a WebView in it
     * @param url The initial URL to display in the WebView.
     */
    public WebTab(String url)
    {
        browser = new WebView();
        browser.getEngine().load(url);
        setGraphic(getWebIcon());
        setContent(browser);
        textProperty().bind(browser.getEngine().titleProperty());

        menuManager = new TabMenuManager(this)
        {
            final List<Menu> menus = Collections.singletonList(JavaFXUtil.makeMenu(Config.getString("frame.webmenu.title"),
                    mainMoveMenu,
                    JavaFXUtil.makeMenuItem(Config.getString("frame.classmenu.close"), () -> tab.getParent().close(tab), new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN))
            ));

            @Override
            List<Menu> getMenus()
            {
                updateMoveMenus();
                return menus;
            }
        };
    }

    /**
     * Gets an icon to display next to web view tabs
     */
    private Node getWebIcon()
    {
        Label j = new Label("W");
        JavaFXUtil.addStyleClass(j, "icon-label");
        return j;
    }

    @Override
    void focusWhenShown()
    {
        // Nothing to do
    }

    @Override
    List<Menu> getMenus()
    {
        return menuManager.getMenus();
    }

    @Override
    String getWebAddress()
    {
        return browser.getEngine().getLocation();
    }

    @Override
    void initialiseFX()
    {
        //We do our initialisation in the constructor
    }

    @Override
    void setParent(FXTabbedEditor parent)
    {
        this.parent = parent;
    }

    @Override
    FXTabbedEditor getParent()
    {
        return parent;
    }

    @Override
    ObservableStringValue windowTitleProperty()
    {
        // Take it from the tab title:
        return textProperty();
    }
}
