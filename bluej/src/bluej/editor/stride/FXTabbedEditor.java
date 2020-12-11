/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2014,2015,2016,2017,2018,2019,2020  Michael Kolling and John Rosenberg
 
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
import bluej.Config;
import bluej.Main;
import bluej.collect.DataCollector;
import bluej.editor.stride.FrameCatalogue.Hint;
import bluej.pkgmgr.Project;
import bluej.prefmgr.PrefMgr;
import bluej.stride.generic.ExtensionDescription;
import bluej.stride.generic.Frame;
import bluej.stride.generic.FrameCursor;
import bluej.stride.generic.InteractionManager;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.FXSupplier;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.UnfocusableScrollPane;
import bluej.utility.javafx.UntitledCollapsiblePane;
import bluej.utility.javafx.UntitledCollapsiblePane.ArrowLocation;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
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
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * FXTabbedEditor is the editor window that contains all the editors in JavaFX tabs
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
    /** The frames which are currently being dragged, if any */
    private final ArrayList<Frame> dragSourceFrames = new ArrayList<Frame>();
    /** Relative to window overlay, not to scene */
    private final SimpleDoubleProperty mouseDragXProperty = new SimpleDoubleProperty();
    private final SimpleDoubleProperty mouseDragYProperty = new SimpleDoubleProperty();
    /** The starting size of the window.  May be null. Updated on window close. */
    private Rectangle startSize;
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
    /** The cancellation action for the scheduled task to switch to another tab after enough time has passed while hovering */
    private FXPlatformRunnable hoverTabTask;
    /** The picture being shown of the currently dragged frames */
    private ImageView dragIcon = null;
    /** Cached so it can be read from any thread.  Written to once on Swing thread in initialise,
     * then effectively final thereafter */
    @OnThread(Tag.Any) private String projectTitle;
    private StringProperty titleStatus = new SimpleStringProperty("");
    private UntitledCollapsiblePane collapsibleCatalogueScrollPane;
    private FrameShelf shelf;
    private boolean dragFromShelf;


    // Neither the constructor nor any initialisers should do any JavaFX work until
    // initialise is called.
    @OnThread(Tag.Any)
    public FXTabbedEditor(Project project, Rectangle startSize)
    {
        this.project = project;
        this.startSize = startSize;
    }

    static boolean isUselessDrag(FrameCursor dragTarget, List<Frame> dragging, boolean copying)
    {
        return !copying && (dragging.contains(dragTarget.getFrameAfter()) || dragging.contains(dragTarget.getFrameBefore()));
    }

    /**
     * Initialises the FXTabbedEditor.
     */
    @OnThread(Tag.FXPlatform)
    public void initialise()
    {
        projectTitle = project.getProjectName();
        stage = new Stage();
        //add the greenfoot icon to the Stride editor.
        BlueJTheme.setWindowIconFX(stage);

        initialiseFX();
    }

    /**
     * The actual initialisation, on the FX thread
     */
    @OnThread(Tag.FXPlatform)
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
        menuAndTabPane.setCenter(new StackPane(tabPane));
        shelf = new FrameShelf(this, project.getShelfStorage());
        // For testing we put shelf on top:
        //Accordion catalogueShelfPane = new Accordion(new TitledPane("Shelf", shelf.getNode()), new TitledPane("Catalogue", cataloguePane));
        ScrollPane catalogueScrollPane = new UnfocusableScrollPane(cataloguePane);
        catalogueScrollPane.setMaxWidth(FrameCatalogue.CATALOGUE_FRAME_WIDTH);
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
        catalogueScrollPaneStacked.setMinWidth(0.0);

        FXPlatformConsumer<? super Boolean> frameCatalogueShownListener =
                show -> recordShowHideFrameCatalogue(show, FrameCatalogue.ShowReason.ARROW);
        collapsibleCatalogueScrollPane = new UntitledCollapsiblePane(catalogueScrollPaneStacked, ArrowLocation.LEFT,
                PrefMgr.getFlag(PrefMgr.STRIDE_SIDEBAR_SHOWING), frameCatalogueShownListener);
        collapsibleCatalogueScrollPane.addArrowWrapperStyleClass("catalogue-collapse");
        showingCatalogue.bindBidirectional(collapsibleCatalogueScrollPane.expandedProperty());
        JavaFXUtil.addChangeListener(showingCatalogue, expanded -> PrefMgr.setFlag(PrefMgr.STRIDE_SIDEBAR_SHOWING, expanded));
        // runLater, after it has been put in scene:
        JavaFXUtil.runAfterCurrent(() -> {
            boolean flag = PrefMgr.getFlag(PrefMgr.STRIDE_SIDEBAR_SHOWING);
            showingCatalogue.set(flag);
            recordShowHideFrameCatalogue(flag, FrameCatalogue.ShowReason.PROPERTIES);
        });
        JavaFXUtil.addStyleClass(collapsibleCatalogueScrollPane, "catalogue-scroll-collapsible");
        menuAndTabPane.setRight(collapsibleCatalogueScrollPane);

        scene = new Scene(new StackPane(menuAndTabPane, dragPane, dragCursorPane, overlayPane.getNode()), 800, 700);
        stage.setScene(scene);
        Config.addEditorStylesheets(scene);

        tabPane.getStyleClass().add("tabbed-editor");

        tabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>()
        {
            @Override
            @OnThread(value = Tag.FXPlatform, ignoreParent = true)
            public void changed(ObservableValue<? extends Tab> a, Tab prevSel, Tab selTab)
            {
                if (selTab != null)
                    FXTabbedEditor.this.updateMenusForTab((FXTab) selTab);

                if (prevSel != null && prevSel != selTab)
                    ((FXTab) prevSel).notifyUnselected();

                if (selTab != null && stage.isFocused())
                    ((FXTab) selTab).notifySelected();

                if (selTab != null && ((FXTab) selTab).shouldShowCatalogue())
                {
                    collapsibleCatalogueScrollPane.setVisible(true);
                    collapsibleCatalogueScrollPane.setManaged(true);
                }
                else
                {
                    collapsibleCatalogueScrollPane.setVisible(false);
                    collapsibleCatalogueScrollPane.setManaged(false);
                }

                if (FXTabbedEditor.this.isWindowVisible())
                {
                    if (!(selTab instanceof FrameEditorTab))
                    {
                        JavaFXUtil.runPlatformLater(() -> FXTabbedEditor.this.scheduleUpdateCatalogue(null, null, CodeCompletionState.NOT_POSSIBLE, false, Frame.View.NORMAL, Collections.emptyList(), Collections.emptyList()));
                    }
                }
            }
        });
        
        tabPane.getTabs().addListener((ListChangeListener<? super Tab>) e -> {
            if (tabPane.getTabs().isEmpty())
            {
                startSize = new Rectangle((int)stage.getX(), (int)stage.getY(), (int)stage.getWidth(), (int)stage.getHeight());
                stage.close();
                project.removeFXTabbedEditor(this);
            }
        });

        stage.setOnHidden(e -> {
            // Close all tabs, which also trigger above code to call removeFXTabbedEditor on us
            // Must take a copy to avoid concurrent modification:
            List<Tab> tabs = new ArrayList<>(tabPane.getTabs());
            tabs.forEach(t -> close((FXTab)t));
        });

        JavaFXUtil.addChangeListenerPlatform(stage.focusedProperty(), focused -> {
            Tab selectedItem = tabPane.getSelectionModel().getSelectedItem();
            // It is possible during shutdown that the window becomes focused while no tabs are present, so guard against that:
            if (selectedItem != null && selectedItem instanceof FXTab)
            {
                if (focused)
                {
                    ((FXTab) selectedItem).notifySelected();
                }
                else
                {
                    // if 'selectedItem' is null, that mean it has been already notified unselected
                    // by the selectedItemProperty Listener added above.
                    ((FXTab) selectedItem).notifyUnselected();
                }
            }
        });

        JavaFXUtil.addChangeListenerPlatform(stage.iconifiedProperty(), minimised -> {
            if (minimised)
                ((FXTab)tabPane.getSelectionModel().getSelectedItem()).notifyUnselected();
        });

        // Add shortcuts for Ctrl-1, Ctrl-2 etc and Ctrl-Tab and Ctrl-Shift-Tab to move between tabs
        // On Mac, it should still be Ctrl-Tab (not Cmd-Tab), but should it be Cmd-1?
        tabPane.addEventFilter(KeyEvent.KEY_PRESSED, e -> {

            // Consume presses of AltGr key on Windows to stop it focusing the menu
            // (the user may just be inserting a foreign character, in which case
            // they don't want to trigger the menu)
            if (Config.isWinOS() && (e.getCode() == KeyCode.ALT_GRAPH || (e.getCode() == KeyCode.ALT && e.isControlDown())))
            {
                e.consume();
                return;
            }

            // Also consume any keypress involving Ctrl+Alt on Windows, as this is likely
            // an AltGr shortcut (which maps to Ctrl+Alt on Windows).  This will break
            // any menu accelerators involving Ctrl+Alt, but we shouldn't have any anyway,
            // because they will conflict with AltGr behaviour on Windows.
            if (Config.isWinOS() && e.isAltDown() && e.isControlDown())
            {
                e.consume();
                return;
            }

            // We only listen for Ctrl/Cmd shortcuts, but we avoid the case when
            // Alt is held down, because AltGr on Windows maps to Ctrl-Alt, and we
            // don't want AltGr-6 to move to tab 6 (only Ctrl-6)
            if (!e.isShortcutDown() || e.isAltDown())
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

        stage.titleProperty().bind(Bindings.concat(
            JavaFXUtil.applyPlatform(tabPane.getSelectionModel().selectedItemProperty(), t -> ((FXTab)t).windowTitleProperty(), "Unknown")
                ," - ", projectTitle, titleStatus));
    }

    /**
     * It prepares the recordShowHideFrameCatalogue event and invoke the appropriate method to register it.
     * If there is an appropriate selected tab, will invoke this tab method after looking for a possible focused cursor.
     * If there is no an appropriate selected tab, will invoke the DataCollector's recordShowHideFrameCatalogue method,
     * without any info about any editor or a focused cursor.
     *
     * @param show    true for showing and false for hiding
     * @param reason  The event which triggers the change.
     *                It is one of the values in the FrameCatalogue.ShowReason enum.
     */
    @OnThread(Tag.FXPlatform)
    private void recordShowHideFrameCatalogue(boolean show, FrameCatalogue.ShowReason reason)
    {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab instanceof InteractionManager) {
            ((InteractionManager) selectedTab).recordShowHideFrameCatalogue(show, reason);
        }
        else {
            DataCollector.showHideFrameCatalogue(getProject(), null, null, -1, show, reason);
        }
    }

    @OnThread(Tag.FXPlatform)
    private void updateMenusForTab(FXTab selTab)
    {
        menuBar.getMenus().setAll(selTab.getMenus());
    }

    /**
     * Property for whether the catalogue/cheat sheet is currently showing
     */
    public BooleanProperty catalogueShowingProperty()
    {
        return showingCatalogue;
    }

    /**
     * Adds the given FXTab to this FXTabbedEditor window
     * @param panel The FXTab to add
     * @param visible Whether to make the FXTabbedEditor window visible 
     * @param toFront Whether to bring the tab to the front (i.e. select the tab)
     */
    @OnThread(Tag.FXPlatform)
    public void addTab(final FXTab panel, boolean visible, boolean toFront)
    {
        addTab(panel, visible, toFront, false);
    }

    @OnThread(Tag.FXPlatform)
    public void addTab(final FXTab panel, boolean visible, boolean toFront, boolean partOfMove)
    {
        panel.setParent(this, partOfMove);
        // This is ok to call multiple times:
        panel.initialiseFX();
        //Debug.time("initialisedFX");
        if (!tabPane.getTabs().contains(panel)) {
            tabPane.getTabs().add(panel);
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
    @OnThread(Tag.FXPlatform)
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
    @OnThread(Tag.FXPlatform)
    public void openJavaCoreDocTab(String qualifiedClassName, String suffix)
    {
        Class<?> theClass = project.loadClass(qualifiedClassName);
        // Guess java.base if we don't know the module:
        String moduleName = theClass == null ? "java.base" : theClass.getModule().getName();
        
        String target = Utility.getDocURL(moduleName, qualifiedClassName, suffix);
        openWebViewTab(target);
    }

    /**
     * Opens a Javadoc tab for a Greenfoot class (e.g. greenfoot.Actor)
     * with given qualified name.
     *
     * If there already exists a tab viewing that page, that page is selected rather than
     * a new tab being opened.
     */
    @OnThread(Tag.FXPlatform)
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
    @OnThread(Tag.FXPlatform)
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
     * @return        True if the visible state needed to be changed, false if there was nothing
     *                that needed to be done.
     */
    public boolean setWindowVisible(boolean visible, Tab tab)
    {
        if (visible)
        {
            boolean wasAlreadyShowing = stage.isShowing();
            if (!wasAlreadyShowing)
            {
                if (startSize != null)
                {
                    stage.setX(startSize.getX());
                    stage.setY(startSize.getY());
                    stage.setWidth(startSize.getWidth());
                    stage.setHeight(startSize.getHeight());
                }
                //Debug.time("Showing");
                stage.show();
                //Debug.time("Shown");
                //org.scenicview.ScenicView.show(stage.getScene());
            }
            if (!tabPane.getTabs().contains(tab))
            {
                tabPane.getTabs().add(tab);
                return true;
            }
            else
            {
                return !wasAlreadyShowing;
            }
        }
        else
        {
            return tabPane.getTabs().remove(tab);
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
    @OnThread(Tag.FXPlatform)
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
     * Removes the given tab from this tabbed editor window
     */
    @OnThread(Tag.FXPlatform)
    public void close(FXTab tab)
    {
        close(tab, false);
    }

    @OnThread(Tag.FXPlatform)
    private void close(FXTab tab, boolean partOfMove)
    {
        tabPane.getTabs().remove(tab);
        tab.setParent(null, partOfMove);
    }


    /**
     * Opens a web view tab to display the given URL.
     * 
     * If a web view tab already exists which is displaying that URL (sans anchors),
     * that tab is displayed and a new tab is not opened.
     */
    @OnThread(Tag.FXPlatform)
    public void openWebViewTab(String url)
    {
        openWebViewTab(url, false);
    }

    /**
     * Opens a web view tab to display the given URL.
     *
     * If a web view tab already exists which is displaying that URL (sans anchors),
     * that tab is displayed and a new tab is not opened.
     *
     * @param isTutorial True if this is a special web tab containing the interactive tutorial
     */
    @OnThread(Tag.FXPlatform)
    public void openWebViewTab(String url, boolean isTutorial)
    {
        // First, check if any tab is already showing that URL:
        try
        {
            URI target = new URI(url);
            for (FXTab tab : getFXTabs())
            {
                if (tab.getWebAddress() == null)
                    continue;

                // Use URI comparison so that URIs get canonicalised:
                URI tabURI = new URI(tab.getWebAddress());
                if (tabURI.equals(target))
                {
                    // Focus that tab and stop:
                    bringToFront(tab);
                    return;
                }
            }
        }
        catch (URISyntaxException e)
        {
            Debug.reportError("Error in URI when opening web view tab: \"" + url + "\"");
        }

        addTab(new WebTab(url, isTutorial), true, true);
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
     * (so that they appear in front of the dragged frames)
     */
    public Pane getDragCursorPane()
    {
        return dragCursorPane;
    }

    /**
     * Begin dragging the given list of frames, starting at the given scene position
     */
    public void frameDragBegin(List<Frame> srcFrames, boolean fromShelf, double mouseSceneX, double mouseSceneY)
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
            dragFromShelf = fromShelf;
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
     * @param dragType Whether the copy-drag key is being held (true) or not (false)
     */
    @OnThread(Tag.FXPlatform)
    public void draggedTo(double sceneX, double sceneY, JavaFXUtil.DragType dragType)
    {
        if (!dragSourceFrames.isEmpty()) {
            Point2D p = dragPane.sceneToLocal(sceneX, sceneY);
            mouseDragXProperty.set(p.getX());
            mouseDragYProperty.set(p.getY());

            checkHoverDuringDrag(sceneX, sceneY, dragType);
            
            // We must notify the tab regardless of whether we're in bounds.
            // If we're out of bounds, they will need to turn off drag target:
            if (tabPane.getSelectionModel().getSelectedItem() instanceof FrameEditorTab)
            {
                ((FrameEditorTab)tabPane.getSelectionModel().getSelectedItem()).draggedToTab(dragSourceFrames, sceneX, sceneY, calcDragCopy(dragType, false));
            }
            shelf.draggedTo(dragSourceFrames, sceneX, sceneY, calcDragCopy(dragType, true));
        }
    }

    /**
     * Given the current state of this.dragFromShelf and the two params,
     * works out if the drag operation would be a copy or a move.
     * @param toShelf
     * @return true if copying, false if moving
     */
    private boolean calcDragCopy(JavaFXUtil.DragType dragType, boolean toShelf)
    {
        switch (dragType)
        {
            case FORCE_MOVING:
                return false;
            case FORCE_COPYING:
                return true;
            default:
                // Do the default:
                if (dragFromShelf && toShelf)
                    return false; // Move within shelf
                else if (dragFromShelf && !toShelf)
                    return true; // Copy from shelf to editor
                else
                    return false; // Move from editor to shelf, or editor to editor
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
     * @param dragType Whether the copy-drag key is being held down
     */
    @OnThread(Tag.FXPlatform)
    private void checkHoverDuringDrag(double sceneX, double sceneY, JavaFXUtil.DragType dragType)
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
                        hoverTabTask.run();
                    
                    hoverTabTask = JavaFXUtil.runAfter(Duration.millis(500), () -> {
                        ((FrameEditorTab)tabPane.getSelectionModel().getSelectedItem()).draggedToAnotherTab();
                        tabPane.getSelectionModel().select(t);
                        draggedTo(sceneX, sceneY, dragType);
                    });
                }
            }
        }
    }

    /**
     * Notify us that a frame drag has ended
     * @param dragType Whether the copy-drag key was held down as the drag finished
     */
    @OnThread(Tag.FXPlatform)
    public void frameDragEnd(JavaFXUtil.DragType dragType)
    {
        if (hoverTabTask != null)
            hoverTabTask.run();
        hoverTab = null;
        
        if (!dragSourceFrames.isEmpty())
        {
            // Stop showing drag icons:
            getDragPane().getChildren().remove(dragIcon);
            dragIcon = null;

            
            // Turn off the highlight:
            dragSourceFrames.forEach(f -> f.setDragSourceEffect(false));
            
            // Must notify both tab and shelf:
            if (tabPane.getSelectionModel().getSelectedItem() instanceof FrameEditorTab)
            {
                ((FrameEditorTab)tabPane.getSelectionModel().getSelectedItem()).dragEndTab(dragSourceFrames, dragFromShelf, calcDragCopy(dragType, false));
            }
            shelf.dragEnd(dragSourceFrames, dragFromShelf, calcDragCopy(dragType, true));
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
    @OnThread(Tag.FXPlatform)
    public void scheduleUpdateCatalogue(FrameEditorTab editor, FrameCursor c, CodeCompletionState codeCompletion, boolean selection, Frame.View viewMode, List<ExtensionDescription> altExtensions, List<Hint> hints)
    {
        cataloguePane.scheduleUpdateCatalogue(editor, viewMode == Frame.View.NORMAL ? c : null, codeCompletion, selection, viewMode, altExtensions, hints);
    }

    /**
     * Gets the window overlay pane
     */
    public WindowOverlayPane getOverlayPane()
    {
        return overlayPane;
    }

    /**
     * Returns true when, and only when, this editor window has a single tab in it
     */
    public boolean hasOneTab()
    {
        return tabPane.getTabs().size() == 1;
    }

    public boolean containsTab(Tab tab)
    {
        return tabPane.getTabs().contains(tab);
    }

    public StringExpression titleProperty()
    {
        return stage.titleProperty();
    }

    private List<FXTab> getFXTabs()
    {
        return Utility.mapList(tabPane.getTabs(), t -> (FXTab)t);
    }

    @OnThread(Tag.Swing)
    public void setPosition(int x, int y)
    {
        Platform.runLater(() -> {
            stage.setX(x);
            stage.setY(y);
        });
    }

    @OnThread(Tag.Swing)
    public void setSize(int width, int height)
    {
        Platform.runLater(() -> {
            stage.setWidth(width);
            stage.setHeight(height);
        });
    }

    public void setTitleStatus(String status)
    {
        this.titleStatus.set(status);
    }

    public Window getWindow()
    {
        return stage;
    }

    public static void setupFrameDrag(Frame f, boolean isShelf, FXSupplier<FXTabbedEditor> parent, FXSupplier<Boolean> canDrag, FXSupplier<FrameSelection> selection)
    {
        f.getNode().setOnDragDetected(event -> {
            double mouseSceneX = event.getSceneX();
            double mouseSceneY = event.getSceneY();
            // No dragging allowed while showing Java preview:
            if (canDrag.get())
            {
                if (selection.get().contains(f))
                {
                    // Drag the whole selection:
                    parent.get().frameDragBegin(selection.get().getSelected(), isShelf, mouseSceneX, mouseSceneY);
                }
                else
                {
                    parent.get().frameDragBegin(Arrays.asList(f), isShelf, mouseSceneX, mouseSceneY);
                }
            }
            event.consume();
        });

        f.getNode().setOnMouseDragged(event -> {
            parent.get().draggedTo(event.getSceneX(), event.getSceneY(), JavaFXUtil.getDragModifiers(event));
            event.consume();
        });

        f.getNode().setOnMouseReleased(event -> {
            if (!parent.get().isDragging())
                return;

            // Make sure we're using the latest position:
            parent.get().draggedTo(event.getSceneX(), event.getSceneY(), JavaFXUtil.getDragModifiers(event));
            parent.get().frameDragEnd(JavaFXUtil.getDragModifiers(event));

            event.consume();
        });
    }

    /**
     * Called when this window is no longer going to be used
     */
    public void cleanup()
    {
        shelf.cleanup();
    }

    /**
     * Does one of the tabs in this window contain a tutorial web view tab?
     */
    public boolean hasTutorial()
    {
        return tabPane.getTabs().stream().anyMatch(t -> t instanceof FXTab && ((FXTab)t).isTutorial());
    }

    public static enum CodeCompletionState
    {
        NOT_POSSIBLE, SHOWING, POSSIBLE;
    }

    @OnThread(Tag.FXPlatform)
    public void moveToNewLater(FXTab tab)
    {
        FXTabbedEditor newWindow = project.createNewFXTabbedEditor();
        moveTabTo(tab, newWindow);
    }

    @OnThread(Tag.FXPlatform)
    public void moveTabTo(FXTab tab, FXTabbedEditor destination)
    {
        close(tab, true);
        destination.addTab(tab, true, true, true);
    }

    @OnThread(Tag.FXPlatform)
    public void updateMoveMenus()
    {
        tabPane.getTabs().forEach(t -> updateMenusForTab((FXTab)t));
    }

    @OnThread(Tag.FX)
    public int getX()
    {
        return (int)stage.getX();
    }

    @OnThread(Tag.FX)
    public int getY()
    {
        return (int)stage.getY();
    }

    @OnThread(Tag.FX)
    public int getWidth()
    {
        return (int)stage.getWidth();
    }

    @OnThread(Tag.FX)
    public int getHeight()
    {
        return (int)stage.getHeight();
    }
}
