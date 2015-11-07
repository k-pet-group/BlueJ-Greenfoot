/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2014,2015  Michael Kolling and John Rosenberg 
 
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

import bluej.BlueJTheme;
import java.awt.Rectangle;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;

import bluej.Main;
import bluej.utility.javafx.FXRunnable;
//import org.scenicview.ScenicView;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.Config;
import bluej.pkgmgr.Project;
import bluej.prefmgr.PrefMgr;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCursor;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.javafx.FXConsumer;
import bluej.utility.javafx.JavaFXUtil;


/**
 * FXTabbedEditor is the editor window that contains all the editors in JavaFX tabs (currently,
 * this is the Stride editors only).
 *
 * Because you can drag between tabs, it is responsible for handling dragging, rather than the individual editor tabs doing it.
 *
 * It is also responsible for changing the menus on the window when the tab changes.
 */
public @OnThread(Tag.FX) class FXTabbedEditor
{
    /** Are we currently showing the frame catalogue/cheat sheet? */
    private final SimpleBooleanProperty showingCatalogue = new SimpleBooleanProperty(true);
    /** The associated project (one window always maps to single project */
    private final Project project;
    /** The menu calculations for each tab (used when different tab selected to swap out menus) */
    private final IdentityHashMap<Tab, Callable<List<Menu>>> tabMenus = new IdentityHashMap<>();
    /** The frames which are currently being dragged, if any */
    private final ArrayList<Frame> dragSourceFrames = new ArrayList<Frame>();
    /** Relative to window overlay, not to scene */
    private final SimpleDoubleProperty mouseDragXProperty = new SimpleDoubleProperty();
    private final SimpleDoubleProperty mouseDragYProperty = new SimpleDoubleProperty();
    /** The actual window */
    private Stage stage;
    /** The scene within the stage */
    private Scene scene;
    /** The tabs container */
    private TabPane tabPane;
    /** The Pane used for frames being dragged */
    private Pane dragPane;
    /** The Pane in front of dragPane used for showing cursor drop destinations
     *  (so they appear in front of the dragged frames): */
    private Pane dragCursorPane;
    /** The window overlay pane, covers whole window */
    private WindowOverlayPane overlayPane;
    /** The right-hand side FrameCatalogue */
    private FrameCatalogue cataloguePane;
    /** The menu bar at the top of the window (or system menu bar on Mac) */
    private MenuBar menuBar;
    /** The tab that is being hovered over to switch tabs (while dragging frames): */
    private Tab hoverTab;
    /** The scheduled task to switch to another tab after enough time has passed while hovering */
    private ScheduledFuture<?> hoverTabTask;
    /** The picture being shown of the currently dragged frames */
    private ImageView dragIcon = null;
    /** A map from (web view) tab to the URL currently being shown in that tab */
    private IdentityHashMap<Tab, ReadOnlyStringProperty> tabAddresses = new IdentityHashMap<>();

    // Neither the constructor nor any initialisers should do any JavaFX work until
    // initialise is called.
    @OnThread(Tag.Any)
    public FXTabbedEditor(Project project)
    {
        this.project = project;
    }

    /**
     * Initialises the FXTabbedEditor.  Called from Swing, it blocks waiting for
     * the FX thread to complete the loading, so that further calls from Swing
     * know it has been initialised
     */
    @OnThread(Tag.Swing)
    public void initialise()
    {
        if (Platform.isFxApplicationThread())
            throw new IllegalStateException("Unexpected call of initialise from FX thread");
        
        Object o = new Object();
        final String windowTitle = project.getProjectName() + " - Stride";
        synchronized (o)
        {
            Platform.runLater(() -> {
                stage = new Stage();
                stage.setTitle(windowTitle);
                //add the greenfoot icon to the Stride editor.
                stage.getIcons().add(BlueJTheme.getApplicationFxIcon("greenfoot", true));
                
                initialiseFX();
                synchronized (o) {
                    o.notify();
                }
            });
            
            try
            {
                o.wait();
            }
            catch (InterruptedException e)
            {
                Debug.reportError(e);
            }
        }
    }

    /**
     * The actual initialisation, on the FX thread
     */
    private void initialiseFX()
    {
        // See comment in Main class:
        if (Thread.currentThread().getContextClassLoader() == null)
        {
            Thread.currentThread().setContextClassLoader(Main.getStoredContextClassLoader());
        }

        Config.loadFXFonts();

        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
        menuBar = new MenuBar();
        JavaFXUtil.addStyleClass(menuBar, "editor-menubar");
        menuBar.setUseSystemMenuBar(true);
        dragPane = new Pane();
        dragPane.setMouseTransparent(true);
        dragCursorPane = new Pane();
        dragCursorPane.setMouseTransparent(true);
        cataloguePane = new FrameCatalogue();


        BorderPane menuAndTabPane = new BorderPane();
        menuAndTabPane.setTop(menuBar);
        overlayPane = new WindowOverlayPane();
        menuAndTabPane.setCenter(new StackPane(tabPane, dragPane, dragCursorPane));
        ScrollPane catalogueScrollPane = new ScrollPane(cataloguePane) {
            @Override
            public void requestFocus() {
                // Do nothing
            }

        };
        catalogueScrollPane.setMaxWidth(200.0);
        catalogueScrollPane.setMinWidth(0.0);
        catalogueScrollPane.setFitToWidth(true);
        catalogueScrollPane.setFocusTraversable(false);
        catalogueScrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        catalogueScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        JavaFXUtil.addStyleClass(catalogueScrollPane, "catalogue-scroll");
        BorderPane catalogueBackground = new BorderPane();
        JavaFXUtil.addStyleClass(catalogueBackground, "catalogue-background");
        Label title = new Label(Config.getString("frame.catalogue.title"));
        BorderPane.setAlignment(title, Pos.BOTTOM_RIGHT);
        catalogueBackground.setBottom(title);
        StackPane catalogueScrollPaneStacked = new StackPane(catalogueBackground, catalogueScrollPane);
        BorderPane collapsibleCatalogueScrollPane = new BorderPane();
        collapsibleCatalogueScrollPane.setCenter(catalogueScrollPaneStacked);
        catalogueScrollPaneStacked.setMinWidth(0.0);
        CollapseControl collapseControl = new CollapseControl(catalogueScrollPaneStacked, showing -> {
            catalogueScrollPane.setVbarPolicy(showing ? ScrollBarPolicy.AS_NEEDED : ScrollBarPolicy.NEVER);
        });
        JavaFXUtil.addChangeListener(showingCatalogue, expanded -> PrefMgr.setFlag(PrefMgr.STRIDE_SIDEBAR_SHOWING, expanded));
        // runLater, after it has been put in scene:
        Platform.runLater(() -> showingCatalogue.set(PrefMgr.getFlag(PrefMgr.STRIDE_SIDEBAR_SHOWING)));
        collapsibleCatalogueScrollPane.setLeft(collapseControl);
        JavaFXUtil.addStyleClass(collapsibleCatalogueScrollPane, "catalogue-scroll-collapsible");
        menuAndTabPane.setRight(collapsibleCatalogueScrollPane);
        scene = new Scene(new StackPane(menuAndTabPane, overlayPane.getNode()), 800, 700);
        stage.setScene(scene);
        Config.addEditorStylesheets(scene);

        tabPane.getStyleClass().add("tabbed-editor");

        tabPane.getSelectionModel().selectedItemProperty().addListener((a, b, selTab) -> {
            if (tabMenus.containsKey(selTab)) {
                List<Menu> menus;
                try {
                    menus = tabMenus.get(selTab).call();
                    menuBar.getMenus().setAll(menus);
                }
                catch (Exception e) {
                    Debug.reportError(e);
                }

            }
            if (isWindowVisible())
            {
                if (!(selTab instanceof FrameEditorTab))
                {
                    scheduleUpdateCatalogue(null, null, CodeCompletionState.NOT_POSSIBLE, false, Frame.View.NORMAL, Collections.emptyList());
                }
            }
        });
        
        tabPane.getTabs().addListener((ListChangeListener<? super Tab>) e -> {
            // When tabs change, make sure stale entry not left in tabAddresses set:

            // Need to take copy to prevent concurrent modification:
            List<Tab> tabAddressKeysToRemove = tabAddresses.entrySet().stream().map(ta -> ta.getKey()).collect(Collectors.toList());
            // Now remove all those which are still open:
            tabAddressKeysToRemove.removeIf(tabPane.getTabs()::contains);
            // Now, tabAddressKeysToRemove has all the old tabs.  Get rid of them:
            tabAddressKeysToRemove.forEach(tabAddresses::remove);

            if (tabPane.getTabs().isEmpty())
                stage.close();
        });
        
        // Add shortcuts for Ctrl-1, Ctrl-2 etc and Ctrl-Tab and Ctrl-Shift-Tab to move between tabs
        // On Mac, it should still be Ctrl-Tab (not Cmd-Tab), but should it be Cmd-1?
        tabPane.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (!e.isShortcutDown())
                return;
            
            int tab = tabPane.getSelectionModel().getSelectedIndex();
            switch (e.getCode())
            {
            // First tab is index 0, but selected with Ctrl+1
            case DIGIT1: tab = 0; break;
            case DIGIT2: tab = 1; break;
            case DIGIT3: tab = 2; break;
            case DIGIT4: tab = 3; break;
            case DIGIT5: tab = 4; break;
            case DIGIT6: tab = 5; break;
            case DIGIT7: tab = 6; break;
            case DIGIT8: tab = 7; break;
            case DIGIT9: tab = 8; break;
            // We don't handle 0
            // Ctrl-Tab is done by default by TabPane, but only when the tab has focus.
            // So we must do it here:
            case TAB:
                if (e.isShiftDown())
                {
                    tab = tab - 1;
                    if (tab < 0)
                        tab = tabPane.getTabs().size() - 1;
                }
                else
                {
                    tab = (tab + 1) % tabPane.getTabs().size();
                }
                break;
            // For all other keys, return and do not process event:
            default: return;
            }
            
            if (tab < tabPane.getTabs().size())
            {
                tabPane.getSelectionModel().select(tabPane.getTabs().get(tab));
            }
            e.consume();
        });
        
        Config.loadFXFonts();
    }

    /**
     * Show the frame catalogue/cheat sheet
     */
    public void showCatalogue()
    {
        showingCatalogue.set(true);
    }

    /**
     * Property for whether the catalogue/cheat sheet is currently showing
     */
    public BooleanProperty catalogueShowingProperty()
    {
        return showingCatalogue;
    }

    /**
     * Adds the given FrameEditorTab to this FXTabbedEditor window
     * @param panel The FrameEditorTab to add
     * @param visible Whether to make the FXTabbedEditor window visible 
     * @param toFront Whether to bring the tab to the front (i.e. select the tab)
     */
    @OnThread(Tag.FX)
    public void addFrameEditor(final FrameEditorTab panel, boolean visible, boolean toFront)
    {
        tabMenus.put(panel, panel::getMenus);
        // This is ok to call multiple times:
        panel.initialiseFX(scene);
        if (!tabPane.getTabs().contains(panel)) {
            tabPane.getTabs().add(panel);
            //ScenicView.show(scene);
            if (toFront)
            {
                setWindowVisible(visible, panel);
                bringToFront(panel);
                Platform.runLater(panel::focusWhenShown);
            }
        }
    }

    /** 
     * Opens a Javadoc tab for a core Java class (i.e. comes with the JRE)
     * with given qualified name.
     * 
     * If there already exists a tab viewing that page, that page is selected rather than
     * a new tab being opened.
     */
    public void openJavaCoreDocTab(String qualifiedClassName)
    {
        openJavaCoreDocTab(qualifiedClassName, "");
    }

    /** Opens a Javadoc tab for a core Java class (i.e. comes with the JRE) with given qualified name,
     *  and the given URL suffix (typically, "#method_anchor")
     *  
     *  If there already exists a tab viewing that page, that page is selected rather than
     * a new tab being opened.
     */
    public void openJavaCoreDocTab(String qualifiedClassName, String suffix)
    {
        String target = Utility.getDocURL(qualifiedClassName, suffix);
        openWebViewTab(target);
    }

    /**
     * Opens a Javadoc tab for a Greenfoot class (e.g. greenfoot.Actor)
     * with given qualified name.
     *
     * If there already exists a tab viewing that page, that page is selected rather than
     * a new tab being opened.
     */
    public void openGreenfootDocTab(String qualifiedClassName)
    {
        openGreenfootDocTab(qualifiedClassName, "");
    }

    /**
     * Opens a Javadoc tab for a Greenfoot class (e.g. greenfoot.Actor)
     * with given qualified name and URL suffix (typically, "#method_anchor")
     *
     * If there already exists a tab viewing that page, that page is selected rather than
     * a new tab being opened.
     */
    public void openGreenfootDocTab(String qualifiedClassName, String suffix)
    {
        try
        {
            String target = Utility.getGreenfootApiDocURL(qualifiedClassName.replace('.', '/') + ".html");
            openWebViewTab(target + suffix);
        }
        catch (IOException e)
        {
            Debug.reportError(e);
        }

    }

    /**
     * Sets the window visible, and adds/removes the given tab
     * @param visible Whether to add the tab and make window visible (true), or remove the tab (false).
     *                Window is only hidden if no tabs remain (handled elsewhere in code)
     * @param tab     The tab in question
     */
    public void setWindowVisible(boolean visible, Tab tab)
    {
        if (visible)
        {
            if (!stage.isShowing()) {
                stage.show();
            }
            if (!tabPane.getTabs().contains(tab))
            {
                tabPane.getTabs().add(tab);
            }
        }
        else
        {
            tabPane.getTabs().remove(tab);
        }
    }
    
    /** Returns whether the window is currently shown */
    public boolean isWindowVisible()
    {
        return stage.isShowing();
    }

    /**
     * Brings the tab to the front: unminimises window, brings window to the front, and selects the tab
     */
    public void bringToFront(Tab tab)
    {
        stage.setIconified(false);
        Utility.bringToFrontFX(stage);
        tabPane.getSelectionModel().select(tab);
    }

    /**
     * Gets the project this window is associated with
     */
    @OnThread(Tag.Any)
    public Project getProject()
    {
        return project;
    }

    /**
     * Schedules a future compilation (@see Project.scheduleCompilation)
     */
    @OnThread(Tag.Any)
    public void scheduleCompilation()
    {
        project.scheduleCompilation(false);
    }

    /**
     * Removes the given tab from this tabbed editor window
     */
    public void close(Tab tab)
    {
        tabPane.getTabs().remove(tab);
        tabMenus.remove(tab);
        tabAddresses.remove(tab);
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

    /**
     * Opens a web view tab to display the given URL.
     * 
     * If a web view tab already exists which is displaying that URL (sans anchors),
     * that tab is displayed and a new tab is not opened.
     */
    public void openWebViewTab(String url)
    {
        // First, check if any tab is already showing that URL:
        try
        {
            URI target = new URI(url);
            for (Map.Entry<Tab, ReadOnlyStringProperty> e : tabAddresses.entrySet())
            {
                // Use URI comparison so that URIs get canonicalised:
                URI tabURI = new URI(e.getValue().get());
                if (tabURI.equals(target))
                {
                    // Focus that tab and stop:
                    bringToFront(e.getKey());
                    return;
                }
            }
        }
        catch (URISyntaxException e)
        {
            Debug.reportError("Error in URI when opening web view tab: \"" + url + "\"");
        }


        WebView browser = new WebView();
        Debug.message("Loading webpage: " + url);
        browser.getEngine().load(url);
        Tab tab = new Tab();
        tab.setGraphic(getWebIcon());
        tab.setContent(browser);
        tab.textProperty().bind(browser.getEngine().titleProperty());
        tabAddresses.put(tab, browser.getEngine().locationProperty());
        tabMenus.put(tab, () -> {
            return Arrays.asList(new Menu("Documentation", null, JavaFXUtil.makeMenuItem("Close", () -> close(tab), new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN))));
        });
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
        if (!stage.isShowing())
            stage.show();
        bringToFront(tab);
    }

    /**
     * The list of currently open tabs
     */
    public ObservableList<Tab> tabsProperty()
    {
        return tabPane.getTabs();
    }

    /**
     * The pane on which the dragged frames are shown
     */
    public Pane getDragPane()
    {
        return dragPane;
    }

    /**
     * The pane on which dragged frame cursor destinations are shown
     * (so that they appear in front of the dragged frames
     */
    public Pane getDragCursorPane()
    {
        return dragCursorPane;
    }

    /**
     * Begin dragging the given list of frames, starting at the given scene position
     */
    public void frameDragBegin(List<Frame> srcFrames, double mouseSceneX, double mouseSceneY)
    {
        if (dragIcon != null || !dragSourceFrames.isEmpty())
        {
            throw new IllegalStateException("Drag begun while drag in progress");
        }
        
        dragSourceFrames.clear();
        dragSourceFrames.addAll(srcFrames);

        Image img = Frame.takeShot(dragSourceFrames, null);
        if (dragSourceFrames.stream().allMatch(Frame::canDrag) && img != null)
        {
            ImageView icon = new ImageView(img);
            icon.setEffect(new DropShadow(7.0, 2.0, 2.0, Color.BLACK));
            for (Frame src : dragSourceFrames)
            {
                src.setDragSourceEffect(true);
            }
            
            double srcSceneX = dragSourceFrames.get(0).getNode().localToScene(0, 0).getX();
            double srcSceneY = dragSourceFrames.get(0).getNode().localToScene(0, 0).getY();
            icon.layoutXProperty().bind(mouseDragXProperty.subtract(mouseSceneX - srcSceneX));
            icon.layoutYProperty().bind(mouseDragYProperty.subtract(mouseSceneY - srcSceneY));
            getDragPane().getChildren().add(icon);
            dragIcon = icon;
            scene.setCursor(Cursor.CLOSED_HAND);
        }
        else
        {
            dragSourceFrames.clear();
        }
    }

    /**
     * Notify us that the current frame has reached the given position
     * @param sceneX Scene X position of mouse
     * @param sceneY Scene Y position of mouse
     * @param copying Whether the copy-drag key is being held (true) or not (false)
     */
    public void draggedTo(double sceneX, double sceneY, boolean copying)
    {
        if (!dragSourceFrames.isEmpty()) {
            Point2D p = dragPane.sceneToLocal(sceneX, sceneY);
            mouseDragXProperty.set(p.getX());
            mouseDragYProperty.set(p.getY());

            checkHoverDuringDrag(sceneX, sceneY, copying);
            
            if (tabPane.getSelectionModel().getSelectedItem() instanceof FrameEditorTab)
            {
                ((FrameEditorTab)tabPane.getSelectionModel().getSelectedItem()).draggedToTab(dragSourceFrames, sceneX, sceneY, copying);
            }
        }
    }

    /**
     * Returns whether a frame drag is currently taking place
     */
    public boolean isDragging()
    {
        return !dragSourceFrames.isEmpty();
    }

    /**
     * During a drag, checks if the mouse is hovering over a tab header.
     * (If so, after a little delay, that tab is switched to)
     * @param sceneX The mouse scene X position
     * @param sceneY The mouse scene Y position
     * @param copying Whether the copy-drag key is being held down
     */
    private void checkHoverDuringDrag(double sceneX, double sceneY, boolean copying)
    {
        // Check if a tab is underneath:
        for (Tab t : tabPane.getTabs())
        {
            Bounds b = t.getGraphic().localToScene(t.getGraphic().getBoundsInLocal());
            if (b.contains(sceneX, sceneY))
            {
                if (hoverTab != t && t instanceof FrameEditorTab)
                {
                    hoverTab = t;
                    if (hoverTabTask != null)
                        hoverTabTask.cancel(false);
                    
                    hoverTabTask = Utility.getBackground().schedule(() -> Platform.runLater(() -> {
                        ((FrameEditorTab)tabPane.getSelectionModel().getSelectedItem()).draggedToAnotherTab();
                        tabPane.getSelectionModel().select(t);
                        ((FrameEditorTab)t).draggedTo(sceneX, sceneY, copying);
                        //TODO tell the new tab about the drag and make it take over somehow
                    }), 500, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    /**
     * Notify us that a frame drag has ended
     * @param copying Whether the copy-drag key was held down as the drag finished
     */
    public void frameDragEnd(boolean copying)
    {
        if (hoverTabTask != null)
            hoverTabTask.cancel(false);
        hoverTab = null;
        
        if (!dragSourceFrames.isEmpty())
        {
            // Stop showing drag icons:
            getDragPane().getChildren().remove(dragIcon);
            dragIcon = null;

            if (tabPane.getSelectionModel().getSelectedItem() instanceof FrameEditorTab)
            {
                ((FrameEditorTab)tabPane.getSelectionModel().getSelectedItem()).dragEndTab(dragSourceFrames, copying);
            }
            dragSourceFrames.clear();
            scene.setCursor(Cursor.DEFAULT);
        }
    }

    /**
     * Schedule a future update to the frame catalogue.
     * 
     * @param editor The editor associated with the currently shown tab.
     *               Pass null if a different kind of tab (e.g. web tab) is showing.
     * @param c The frame cursor currently focused.
     *          Pass null if there is no currently focused cursor or a different kind of tab is showing
     * @param codeCompletion Whether code completion is currently possible
     * @param selection Whether there is currently a frame selection
     * @param viewMode The current view mode (if a frame editor tab is showing)
     * @param hints The list of hints that should be displayed
     */
    public void scheduleUpdateCatalogue(FrameEditorTab editor, FrameCursor c, CodeCompletionState codeCompletion, boolean selection, Frame.View viewMode, List<FrameCatalogue.Hint> hints)
    {
        cataloguePane.scheduleUpdateCatalogue(editor, viewMode == Frame.View.NORMAL ? c : null, codeCompletion, selection, viewMode, hints);
    }

    /**
     * Gets the window overlay pane
     */
    public WindowOverlayPane getOverlayPane()
    {
        return overlayPane;
    }

    public static enum CodeCompletionState
    {
        NOT_POSSIBLE, SHOWING, POSSIBLE;
    }
            
    /**
     * The triangle control to the left of the frame catalogue.  Clicking on it
     * toggles the visibility of the frame catalogue by sliding it in or out
     */
    private class CollapseControl extends BorderPane
    {
        private final Duration EXPAND_COLLAPSE_DURATION = Duration.millis(200);
        private final Scale scale;
        private FXRunnable cancelHover;
        public CollapseControl(Region collapse, FXConsumer<Boolean> listener)
        {
            JavaFXUtil.addStyleClass(this, "catalogue-collapse");
            Canvas control = new Canvas(8, 12);
            GraphicsContext gc = control.getGraphicsContext2D();
            gc.setFill(Color.DARKGRAY);
            gc.fillPolygon(new double[] { 1, control.getWidth() - 1, 1, 1}, new double[] {1, 6, 11, 1}, 4);
            setCenter(control);
            setFocusTraversable(false);
            setMinWidth(10.0);
            scale = new Scale(1.0, 1.0, control.getWidth() / 2.0, control.getHeight() / 2.0);
            control.getTransforms().add(scale);
            
            DoubleProperty shrinkExpand = new SimpleDoubleProperty(1.0);
            collapse.maxWidthProperty().bind(shrinkExpand.multiply(collapse.getMaxWidth()));
            collapse.prefWidthProperty().bind(collapse.maxWidthProperty());
            
            // Click can be anywhere down the divider:
            setOnMouseClicked(e -> showingCatalogue.set(!showingCatalogue.get()));
            JavaFXUtil.addChangeListener(showingCatalogue, new FXConsumer<Boolean>()
            {
                private Timeline animation = null;

                @Override
                public void accept(Boolean nowExpanded)
                {
                    listener.accept(nowExpanded);
                    animate(nowExpanded);
                }

                private void animate(boolean expand)
                {
                    if (animation != null)
                    {
                        animation.stop();
                        animation = null;
                    }

                    animation = new Timeline(
                            new KeyFrame(EXPAND_COLLAPSE_DURATION,
                                    new KeyValue(shrinkExpand, expand ? 1.0 : 0.0, Interpolator.EASE_OUT),
                                    new KeyValue(scale.xProperty(), expand ? 1.0 : -1.0))
                    );
                    animation.play();
                }
            });
            // We use a delay before setting our hover class, to avoid flashes as the user moves their mouse cursor
            // across the screen, to and from the frame catalogue:
            setOnMouseEntered(e -> {
                // Shouldn't be non-null, but just in case:
                if (cancelHover != null)
                    cancelHover.run();
                cancelHover = JavaFXUtil.runAfter(Duration.millis(200), () -> JavaFXUtil.setPseudoclass("bj-hover-long", true, this));
            });
            setOnMouseExited(e -> {
                if (cancelHover != null)
                {
                    cancelHover.run();
                    cancelHover = null;
                }
                JavaFXUtil.setPseudoclass("bj-hover-long", false, this);
            });
        }
    }
}
