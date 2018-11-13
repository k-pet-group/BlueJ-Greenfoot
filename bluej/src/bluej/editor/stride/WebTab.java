/*
 This file is part of the Greenfoot program.
 Copyright (C) 2015,2016,2018  Michael Kolling and John Rosenberg

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
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.EditableTarget;
import bluej.testmgr.TestDisplayFrame;
import bluej.utility.Utility;
import bluej.utility.javafx.JavaFXUtil;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import javafx.stage.Popup;
import javafx.stage.Window;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import threadchecker.OnThread;
import threadchecker.Tag;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;


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
    private final WeakHashMap<Project, OverlayInfo> tutorialOverlays = new WeakHashMap<>();

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
            JavaFXUtil.addChangeListenerPlatform(this.browser.getEngine().documentProperty(), this::tutorialDocumentMangle);
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

    /**
     * Modifies the HTML document to turn tutorial-specific links into actions which highlight or open items.
     */
    @OnThread(Tag.FXPlatform)
    private void tutorialDocumentMangle(Document doc)
    {
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
                            ((EditableTarget) parent.getProject().getTarget(anchorHref.getNodeValue().
                                    substring("class:".length()).trim())).getEditor().
                                    setEditorVisible(true, false);
                            e.stopPropagation();
                        }, true);
                    }
                    else if (anchorHref.getNodeValue().startsWith("guicss:"))
                    {
                        ((EventTarget) anchorItem).addEventListener("click", e ->
                        {
                            // Hide previous overlays for project.  Do this even if new link doesn't work,
                            // as we don't want to confuse the user by still showing an old popup:
                            OverlayInfo prevOverlay = tutorialOverlays.get(parent.getProject());
                            if (prevOverlay != null)
                            {
                                prevOverlay.hide();
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

                                OverlayInfo overlayInfo = new OverlayInfo(targetWindow, n, overlays);

                                tutorialOverlays.put(parent.getProject(), overlayInfo);

                                Utility.bringToFrontFX(targetWindow);
                                targetWindow.requestFocus();

                                //org.scenicview.ScenicView.show(overlay.getScene());
                            }
                            e.stopPropagation();
                        }, true);
                    }
                }
            }
        }
    }

    /**
     * A class which keeps track of the red-line overlays, and on certain actions
     * (clicking target, window losing focus, window changing size/position)
     * hides the overlays.
     */
    private static class OverlayInfo implements EventHandler<MouseEvent>, ChangeListener<Object>
    {
        private final Popup[] overlays;
        private final Node target;
        private final Window targetWindow;

        public OverlayInfo(Window targetWindow, Node target, Popup[] overlays)
        {
            this.targetWindow = targetWindow;
            this.target = target;
            this.overlays = overlays;
            // Install listeners for clicking on target item, window losing focus, or being moved/resized:
            target.addEventFilter(MouseEvent.MOUSE_PRESSED, this);
            targetWindow.focusedProperty().addListener(this);
            targetWindow.xProperty().addListener(this);
            targetWindow.yProperty().addListener(this);
            targetWindow.widthProperty().addListener(this);
            targetWindow.heightProperty().addListener(this);
        }

        /**
         * Implement EventHandler interface
         */
        @Override
        @OnThread(value = Tag.FXPlatform, ignoreParent = true)
        public void handle(MouseEvent event)
        {
            // Mouse has been pressed on target item, hide us:
            hide();
        }

        /**
         * Hides the red line popups, and cleans up listeners.
         */
        @OnThread(Tag.FXPlatform)
        public void hide()
        {
            for (Popup overlay : overlays)
            {
                overlay.hide();
            }
            // Need to clean up all the listeners:
            target.removeEventFilter(MouseEvent.MOUSE_PRESSED, this);
            targetWindow.focusedProperty().removeListener(this);
            targetWindow.xProperty().removeListener(this);
            targetWindow.yProperty().removeListener(this);
            targetWindow.widthProperty().removeListener(this);
            targetWindow.heightProperty().removeListener(this);
        }

        /**
         * Implement ChangeListener interface:
         */
        @Override
        @OnThread(value = Tag.FXPlatform, ignoreParent = true)
        public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue)
        {
            // Note we are a listener for several different observable properties, so we need to work out which one
            // caused us to be called to take the appropriate action:
            if (observable == targetWindow.focusedProperty() && ((Boolean)newValue) == false)
            {
                hide();
            }
            if (observable == targetWindow.xProperty() || observable == targetWindow.yProperty()
                    || observable == targetWindow.widthProperty() || observable == targetWindow.heightProperty())
            {
                // If they move or resize the window, hide ourselves:
                hide();
            }
        }
    }
}
