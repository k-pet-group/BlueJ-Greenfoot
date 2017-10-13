/*
 This file is part of the Greenfoot program.
 Copyright (C) 2015,2016  Michael Kolling and John Rosenberg

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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.WeakHashMap;

import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.EditableTarget;
import bluej.testmgr.TestDisplayFrame;
import bluej.utility.Debug;
import bluej.utility.Utility;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.web.WebView;

import bluej.Config;
import bluej.utility.javafx.JavaFXUtil;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.Window;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import threadchecker.OnThread;
import threadchecker.Tag;

import javax.swing.*;


/**
 * A tab in the FXTabbedEditor which just contains a WebView.
 */
@OnThread(Tag.FX)
public class WebTab extends FXTab
{
    private final WebView browser;
    private final boolean tutorial;
    private FXTabbedEditor parent;
    private final TabMenuManager menuManager;
    // Red overlays used in tutorial.  We store because when we show a new
    // overlay, we should hide previous one:
    private final WeakHashMap<Project, Popup[]> tutorialOverlays = new WeakHashMap<>();

    /**
     * Constructs a WebTab with a WebView in it
     * @param url The initial URL to display in the WebView.
     */
    @OnThread(Tag.FXPlatform)
    public WebTab(String url, boolean enableTutorial)
    {
        super(false);
        browser = new WebView();
        this.tutorial = enableTutorial;
        if (enableTutorial)
        {
            setClosable(false);
            setupTutorialMangler();
        }
        browser.getEngine().load(url);
        // When user selects Open in New Window, make a new web tab and open there:
        browser.getEngine().setCreatePopupHandler(p -> {
            WebTab newTab = new WebTab(null, enableTutorial);
            parent.addTab(newTab, true, true);
            return newTab.browser.getEngine();
        });
        setGraphic(getWebIcon());
        setContent(browser);
        textProperty().bind(browser.getEngine().titleProperty());

        menuManager = new TabMenuManager(this)
        {
            final List<Menu> menus = Collections.singletonList(JavaFXUtil.makeMenu(Config.getString("frame.webmenu.title"),
                    mainMoveMenu,
                    JavaFXUtil.makeMenuItem(Config.getString("frame.webmenu.open.external"), () -> {
                        String location = browser.getEngine().getLocation();
                        SwingUtilities.invokeLater(() -> Utility.openWebBrowser(location));
                    }, new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN)),
                    JavaFXUtil.makeMenuItem(Config.getString("frame.classmenu.close"), () -> tab.getParent().close(tab), new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN))
            ));

            @Override
            @OnThread(Tag.FXPlatform)
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
    @OnThread(Tag.FXPlatform)
    List<Menu> getMenus()
    {
        return menuManager.getMenus();
    }

    @Override
    @OnThread(Tag.FXPlatform)
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
    void setParent(FXTabbedEditor parent, boolean partOfMove)
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

    @Override
    public void notifySelected()
    {
        // Nothing to do
    }

    @Override
    public void notifyUnselected()
    {
        // Nothing to do
    }

    @Override
    public boolean isTutorial()
    {
        return tutorial;
    }

    @OnThread(Tag.FXPlatform)
    private void setupTutorialMangler()
    {
        JavaFXUtil.addChangeListenerPlatform(this.browser.getEngine().documentProperty(), doc -> {
            if (doc != null)
            {
                // First find the anchors.
                NodeList anchors = doc.getElementsByTagName("a");
                for (int i = 0; i < anchors.getLength(); i++)
                {
                    org.w3c.dom.Node anchorItem = anchors.item(i);
                    org.w3c.dom.Node anchorHref = anchorItem.getAttributes().getNamedItem("href");
                    if (anchorHref != null && anchorHref.getNodeValue() != null)
                    {
                        if (anchorHref.getNodeValue().startsWith("class:"))
                        {
                            ((EventTarget) anchorItem).addEventListener("click", e ->
                            {
                                Debug.message("Anchor clicked");
                                ((EditableTarget) parent.getProject().getTarget(anchorHref.getNodeValue().substring("class:".length()).trim())).getEditor().setEditorVisible(true);
                                e.stopPropagation();
                            }, true);
                        }
                        else if (anchorHref.getNodeValue().startsWith("guicss:"))
                        {
                            ((EventTarget) anchorItem).addEventListener("click", e ->
                            {
                                // Hide previous overlays for project.  Do this even if link doesn't work,
                                // as we don't want to confuse the user by still showing an old popup:
                                for (Popup popup : tutorialOverlays.getOrDefault(parent.getProject(), new Popup[0]))
                                {
                                    popup.hide();
                                }

                                String nodeCSS = anchorHref.getNodeValue().substring("guicss:".length());

                                final Window targetWindow;
                                if (nodeCSS.startsWith("Terminal"))
                                {
                                    targetWindow = parent.getProject().getTerminal().getWindow();
                                    nodeCSS = nodeCSS.substring("Terminal".length());
                                }
                                else if (nodeCSS.startsWith("Editor"))
                                {
                                    targetWindow = parent.getProject().getDefaultFXTabbedEditor().getWindow();
                                    nodeCSS = nodeCSS.substring("Editor".length());
                                }
                                else if (nodeCSS.startsWith("TestResults"))
                                {
                                    targetWindow = TestDisplayFrame.getTestDisplay().getWindow();
                                    nodeCSS = nodeCSS.substring("TestResults".length());
                                }
                                else
                                {
                                    targetWindow = parent.getProject().getPackage("").getEditor().getFXWindow();
                                }
                                Node n = targetWindow.getScene().lookup(nodeCSS);
                                if (n != null)
                                {
                                    // We can't have one popup that's hollow in the middle, so we have one per side:
                                    Bounds screenBounds = n.localToScreen(n.getBoundsInLocal());
                                    Rectangle[] rects = new Rectangle[] {
                                        new Rectangle(screenBounds.getWidth() + 20, 5), // top
                                        new Rectangle(5, screenBounds.getHeight() + 20), // right
                                        new Rectangle(screenBounds.getWidth() + 20, 5), // bottom
                                        new Rectangle(5, screenBounds.getHeight() + 20) // left
                                    };

                                    Popup[] overlays = new Popup[4];
                                    for (int j = 0; j < 4; j++)
                                    {
                                        rects[j].setFill(Color.RED);
                                        overlays[j] = new Popup();
                                        overlays[j].getContent().setAll(rects[j]);
                                    }

                                    overlays[0].show(targetWindow, screenBounds.getMinX() - 10, screenBounds.getMinY() - 10);
                                    overlays[1].show(targetWindow, screenBounds.getMaxX() + 5, screenBounds.getMinY() - 10);
                                    overlays[2].show(targetWindow, screenBounds.getMinX() - 10, screenBounds.getMaxY() + 5);
                                    overlays[3].show(targetWindow, screenBounds.getMinX() - 10, screenBounds.getMinY() - 10);

                                    n.addEventFilter(MouseEvent.MOUSE_PRESSED, ev -> {
                                        for (Popup overlay : overlays)
                                        {
                                            overlay.hide();
                                        }
                                    });

                                    tutorialOverlays.put(parent.getProject(), overlays);

                                    Utility.bringToFrontFX(targetWindow);

                                    //org.scenicview.ScenicView.show(overlay.getScene());
                                }
                                e.stopPropagation();
                            }, true);
                        }
                    }
                }
            }
        });
    }
}
