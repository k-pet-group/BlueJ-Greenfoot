/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011,2012,2013,2014,2015,2016,2017  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterJob;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import bluej.utility.javafx.FXConsumer;
import bluej.utility.javafx.TriangleArrow;
import bluej.utility.javafx.UntitledCollapsiblePane.ArrowLocation;
import javafx.animation.Animation;
import javafx.animation.FillTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingNode;
import javafx.event.EventHandler;
import javafx.geometry.*;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import javafx.util.Duration;

import bluej.BlueJEvent;
import bluej.BlueJEventListener;
import bluej.BlueJTheme;
import bluej.Config;
import bluej.classmgr.BPClassLoader;
import bluej.collect.DataCollector;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugmgr.ExecutionEvent;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.Invoker;
import bluej.debugmgr.LibraryCallDialog;
import bluej.debugmgr.ResultWatcher;
import bluej.debugmgr.codepad.CodePad;
import bluej.debugmgr.objectbench.ObjectBench;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.extensions.SourceType;
import bluej.extmgr.ExtensionsManager;
import bluej.extmgr.FXMenuManager;
import bluej.extmgr.ToolsExtensionMenu;
import bluej.extmgr.ViewExtensionMenu;
import bluej.groupwork.actions.CheckoutAction;
import bluej.groupwork.actions.CommitCommentAction;
import bluej.groupwork.actions.ImportAction;
import bluej.groupwork.actions.StatusAction;
import bluej.groupwork.actions.TeamActionGroup;
import bluej.groupwork.actions.UpdateDialogAction;
import bluej.groupwork.ui.ActivityIndicatorFX;
import bluej.pkgmgr.actions.AddClassAction;
import bluej.pkgmgr.actions.CancelTestRecordAction;
import bluej.pkgmgr.actions.CheckExtensionsAction;
import bluej.pkgmgr.actions.CheckVersionAction;
import bluej.pkgmgr.actions.CloseProjectAction;
import bluej.pkgmgr.actions.CompileAction;
import bluej.pkgmgr.actions.CompileSelectedAction;
import bluej.pkgmgr.actions.EndTestRecordAction;
import bluej.pkgmgr.actions.ExportProjectAction;
import bluej.pkgmgr.actions.GenerateDocsAction;
import bluej.pkgmgr.actions.HelpAboutAction;
import bluej.pkgmgr.actions.ImportProjectAction;
import bluej.pkgmgr.actions.NewClassAction;
import bluej.pkgmgr.actions.NewInheritsAction;
import bluej.pkgmgr.actions.NewPackageAction;
import bluej.pkgmgr.actions.NewProjectAction;
import bluej.pkgmgr.actions.OpenArchiveAction;
import bluej.pkgmgr.actions.OpenNonBlueJAction;
import bluej.pkgmgr.actions.OpenProjectAction;
import bluej.pkgmgr.actions.PageSetupAction;
import bluej.pkgmgr.actions.PkgMgrToggleAction;
import bluej.pkgmgr.actions.PreferencesAction;
import bluej.pkgmgr.actions.PrintAction;
import bluej.pkgmgr.actions.QuitAction;
import bluej.pkgmgr.actions.RebuildAction;
import bluej.pkgmgr.actions.RemoveAction;
import bluej.pkgmgr.actions.RestartVMAction;
import bluej.pkgmgr.actions.RunTestsAction;
import bluej.pkgmgr.actions.SaveProjectAction;
import bluej.pkgmgr.actions.SaveProjectAsAction;
import bluej.pkgmgr.actions.ShowCopyrightAction;
import bluej.pkgmgr.actions.ShowDebuggerAction;
import bluej.pkgmgr.actions.ShowTerminalAction;
import bluej.pkgmgr.actions.ShowTestResultsAction;
import bluej.pkgmgr.actions.StandardAPIHelpAction;
import bluej.pkgmgr.actions.TutorialAction;
import bluej.pkgmgr.actions.UseLibraryAction;
import bluej.pkgmgr.actions.WebsiteAction;
import bluej.pkgmgr.print.PackagePrintManager;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.PackageTarget;
import bluej.pkgmgr.target.Target;
import bluej.pkgmgr.target.role.UnitTestClassRole;
import bluej.prefmgr.PrefMgr;
import bluej.prefmgr.PrefMgrDialog;
import bluej.testmgr.TestDisplayFrame;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.utility.JavaNames;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.FXPlatformSupplier;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.UntitledCollapsiblePane;
import bluej.views.CallableView;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * The main user interface frame which allows editing of packages
 */
public class PkgMgrFrame
    implements BlueJEventListener, PackageEditorListener
{
    /** Frame most recently having focus */
    @OnThread(Tag.Any)
    private static PkgMgrFrame recentFrame = null;

    // instance fields:
    private static final AtomicInteger nextTestIdentifier = new AtomicInteger(0); 
    // set PageFormat for default page for default printer
    // this variable is lazy initialised
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private static PageFormat pageFormat = null;
    @OnThread(value = Tag.Any, requireSynchronized = true)
    private static final List<PkgMgrFrame> frames = new ArrayList<>(); // of PkgMgrFrames
    private static final ExtensionsManager extMgr = ExtensionsManager.getInstance();
    @OnThread(Tag.FXPlatform)
    private TitledPane testPanel;
    @OnThread(Tag.FXPlatform)
    private TitledPane teamPanel;
    @OnThread(Tag.FXPlatform)
    private ButtonBase imgExtendsButton;
    private @OnThread(Tag.FX) ButtonBase runButton;
    @OnThread(Tag.FX)
    private Label statusbar;
    // Initialised once, effectively final thereafter:
    @OnThread(Tag.Any)
    private ActivityIndicatorFX progressbar;
    @OnThread(Tag.FX)
    private Label testStatusMessage;
    @OnThread(Tag.FXPlatform)
    private Label recordingLabel;
    @OnThread(Tag.Any) private final EndTestRecordAction endTestRecordAction = new EndTestRecordAction(this);
    @OnThread(Tag.Any) private final CancelTestRecordAction cancelTestRecordAction = new CancelTestRecordAction(this);
    private ClassTarget testTarget = null;
    private String testTargetMethod;
    private int testIdentifier = 0;
    private JMenu recentProjectsMenu;
    private JMenu testingMenu;
    @OnThread(Tag.FXPlatform)
    private final SimpleObjectProperty<FXMenuManager> toolsMenuManager;
    @OnThread(Tag.FXPlatform)
    private final SimpleObjectProperty<FXMenuManager> viewMenuManager;
    private JMenu teamMenu;
    private JMenuItem shareProjectMenuItem;
    private JMenuItem teamSettingsMenuItem;
    private JMenuItem showLogMenuItem;
    private JMenuItem updateMenuItem;
    private JMenuItem commitMenuItem;
    private JMenuItem statusMenuItem;
    private @OnThread(Tag.FX) ButtonBase updateButton;
    private @OnThread(Tag.FX) ButtonBase commitButton;
    private @OnThread(Tag.FX) ButtonBase teamStatusButton;
    private @OnThread(Tag.FX) ButtonBase teamShareButton;
    private TeamActionGroup teamActions;
    @OnThread(Tag.FX)
    private final List<Node> itemsToDisable = new ArrayList<>();
    @OnThread(Tag.FX)
    private final List<MenuItem> menuItemsToDisable = new ArrayList<>();
    private final List<Action> actionsToDisable = new ArrayList<>();
    @OnThread(Tag.Any)
    private MachineIcon machineIcon;
    /* UI actions */
    private final Action closeProjectAction = new CloseProjectAction(this);
    private final Action saveProjectAction = new SaveProjectAction(this);
    private final Action saveProjectAsAction = new SaveProjectAsAction(this);
    private final Action importProjectAction = new ImportProjectAction(this);
    private final Action exportProjectAction = new ExportProjectAction(this);
    private final Action pageSetupAction = new PageSetupAction(this);
    private final Action printAction = new PrintAction(this);
    @OnThread(Tag.Any)
    private final Action newClassAction = new NewClassAction(this);
    private final Action newPackageAction = new NewPackageAction(this);
    private final Action addClassAction = new AddClassAction(this);
    private final Action removeAction = new RemoveAction(this);
    @OnThread(Tag.Any)
    private final Action newInheritsAction = new NewInheritsAction(this);
    @OnThread(Tag.Any)
    private final Action compileAction = new CompileAction(this);
    private final Action compileSelectedAction = new CompileSelectedAction(this);
    private final Action rebuildAction = new RebuildAction(this);
    @OnThread(Tag.Any)
    private final RestartVMAction restartVMAction = new RestartVMAction(this);
    private final Action useLibraryAction = new UseLibraryAction(this);
    private final Action generateDocsAction = new GenerateDocsAction(this);
    private final PkgMgrToggleAction showDebuggerAction = new ShowDebuggerAction(this) {
        @Override
        public void setEnabled(boolean newValue)
        {
            super.setEnabled(newValue);
        }
    };
    private final PkgMgrToggleAction showTerminalAction = new ShowTerminalAction(this);
    @OnThread(Tag.Any)
    private final Action runTestsAction = new RunTestsAction(this);
    /*
     * The package that this frame is working on or null for the case where
     * there is no package currently being edited (check with isEmptyFrame())
     */
    @OnThread(value = Tag.Any,requireSynchronized = true)
    private Package pkg = null;
    /*
     * The graph editor which works on the package or null for the case where
     * there is no package current being edited (isEmptyFrame() == true)
     */
    @OnThread(Tag.Any)
    private PackageEditor editor = null;
    @OnThread(Tag.Any)
    // Effectively final, but can't mark it as such because initialised on other thread
    private ObjectBench objbench;
    @OnThread(Tag.FXPlatform)
    private CodePad codePad;
    @OnThread(Tag.FXPlatform)
    private SimpleBooleanProperty showingTextEval;

    // static methods to create and remove frames
    // lazy initialised dialogs
    @OnThread(Tag.FXPlatform)
    private LibraryCallDialog libraryCallDialog = null;
    private ProjectPrintDialog projectPrintDialog = null;
    private ExportManager exporter;

    @OnThread(Tag.FX)
    private Property<Stage> stageProperty;
    @OnThread(Tag.FX)
    private Property<BorderPane> paneProperty;
    @OnThread(Tag.FXPlatform)
    private FXPlatformRunnable cancelWiggle;
    @OnThread(Tag.FXPlatform)
    private VBox toolPanel;
    @OnThread(Tag.FXPlatform)
    private EventHandler<javafx.scene.input.MouseEvent> editorMousePressed;
    @OnThread(Tag.FXPlatform)
    private ScrollPane pkgEditorScrollPane;
    // We keep these properties here because we need them for creating
    // the menu, but the menu may get created before a project (and PackageEditor) has been opened:
    @OnThread(Tag.Any)
    private SimpleBooleanProperty showUsesProperty;
    @OnThread(Tag.Any)
    private SimpleBooleanProperty showInheritsProperty;
    // The horizontal split pane containing the object bench and (sometimes) codepad.
    @OnThread(Tag.FX)
    private SplitPane bottomPane;
    @OnThread(Tag.FX)
    private double bottomPaneLastDividerPos = 0.6; // default split
    @OnThread(Tag.FX)
    private Pane bottomOverlay;
    // A dummy SwingNode to enable us to get the AWT frame ancestor which underpins
    // the JavaFX window, mainly for the extensions
    @OnThread(Tag.Any)
    private SwingNode dummySwingNode;
    // The vertical split pane with controls+class diagram on top,
    // and bench+codepad split pane on the bottom.
    @OnThread(Tag.FX)
    private SplitPane topBottomSplit;
    // The overlay in front of the top half of topBottomSplit, which we draw on when creating extends arrows.
    @OnThread(Tag.Any)
    private final MouseTrackingOverlayPane topOverlay = new MouseTrackingOverlayPane();
    @OnThread(Tag.FX)
    private UntitledCollapsiblePane teamAndTestFoldout;
    @OnThread(Tag.FX)
    private BooleanExpression teamShowSharedButtons;

    /**
     * Create a new PkgMgrFrame which does not show a package.
     * 
     * This constructor can only be called via createFrame().
     */
    private PkgMgrFrame()
    {
        stageProperty = new SimpleObjectProperty<>(null);
        paneProperty = new SimpleObjectProperty<>(null);
        showingTextEval = new SimpleBooleanProperty(false);
        showUsesProperty = new SimpleBooleanProperty(true);
        showInheritsProperty = new SimpleBooleanProperty(true);
        toolsMenuManager = new SimpleObjectProperty<>(null);
        viewMenuManager = new SimpleObjectProperty<>(null);
        this.pkg = null;
        this.editor = null;
        if(!Config.isGreenfoot()) {
            teamActions = new TeamActionGroup(false);
            teamActions.setAllDisabled(this);

            setupActionDisableSet();
            makeFrame();
            setStatus(bluej.Boot.BLUEJ_VERSION_TITLE);

            new JFXPanel();
            Platform.runLater(() -> {
                Stage stage = new Stage();
                BlueJTheme.setWindowIconFX(stage);

                objbench = new ObjectBench(this);
                addCtrlTabShortcut(objbench);
                itemsToDisable.add(objbench);
                
                BorderPane topPane = new BorderPane();
                pkgEditorScrollPane = new ScrollPane(null) {
                    @Override
                    @OnThread(Tag.FX)
                    public void requestFocus()
                    {
                        // Override default behaviour (in which clicking on scroll pane
                        // gives it focus).
                        // Don't let the pane request focus.
                    }
                };
                pkgEditorScrollPane.setVisible(false);
                pkgEditorScrollPane.visibleProperty().bind(pkgEditorScrollPane.contentProperty().isNotNull());
                pkgEditorScrollPane.setFitToWidth(true);
                pkgEditorScrollPane.setFitToHeight(true);
                Label emptyProjectMessage = new Label(Config.getString("pkgmgr.noProjectOpened.message"));
                JavaFXUtil.addStyleClass(emptyProjectMessage, "pmf-empty-project-msg");
                StackPane centralPane = new StackPane(emptyProjectMessage, pkgEditorScrollPane);
                JavaFXUtil.addStyleClass(centralPane, "pmf-central-pane");
                topPane.setCenter(centralPane);
                //topPane.setMinHeight(minSize.getHeight());
                topPane.setLeft(toolPanel);
                TriangleArrow triangleLabel = new TriangleArrow(Orientation.HORIZONTAL);
                StackPane.setAlignment(triangleLabel, Pos.CENTER_RIGHT);
                StackPane.setMargin(triangleLabel, new Insets(0, 5, 0, 0));
                triangleLabel.setOnMouseClicked(e -> {
                    // Toggle it:
                    showingTextEval.set(!showingTextEval.get());
                });
                triangleLabel.scaleProperty().bind(Bindings.when(showingTextEval).then(-1.0).otherwise(1.0));
                FXPlatformRunnable addScrollBarListener = new FXPlatformRunnable()
                {
                    @Override
                    public @OnThread(Tag.FXPlatform) void run()
                    {
                        Region scrollBar = (Region)objbench.lookup(".scroll-bar:horizontal");
                        if (scrollBar == null)
                        {
                            // Keep going until the scroll bar is ready:
                            JavaFXUtil.runAfterCurrent(this);
                            return;
                        }
                        // The visibility of the scrollbar is set mid-layout pass so we run
                        // afterwards to make sure everything is up-to-date:
                        FXConsumer<Object> update = showing -> JavaFXUtil.runPlatformLater(() ->
                        {
                            if (scrollBar.isVisible())
                                StackPane.setMargin(triangleLabel, new Insets(0, 5 + scrollBar.getWidth(), 0, 0));
                            else
                                StackPane.setMargin(triangleLabel, new Insets(0, 5, 0, 0));
                        });
                        JavaFXUtil.addChangeListener(scrollBar.visibleProperty(), update);
                        // Not sure if scroll bar can change width, but guard against it:
                        JavaFXUtil.addChangeListener(scrollBar.widthProperty(), update);
                    }
                };
                JavaFXUtil.runAfterCurrent(addScrollBarListener);


                bottomOverlay = new Pane();
                bottomOverlay.setMouseTransparent(true);
                bottomPane = new SplitPane(new StackPane(objbench, triangleLabel));
                bottomPane.setOrientation(Orientation.HORIZONTAL);
                // Wait until the codepad appears, then set it:
                bottomPane.getDividers().addListener((ListChangeListener<? super SplitPane.Divider>)c -> {
                    c.next();
                    if (c.wasAdded())
                    {
                        // Use last saved divider positions:
                        c.getAddedSubList().get(0).setPosition(1.0);
                        Timeline t = new Timeline(new KeyFrame(Duration.millis(2000), new KeyValue(c.getAddedSubList().get(0).positionProperty(), bottomPaneLastDividerPos)));
                        t.play();
                    }
                    else if (c.wasRemoved())
                    {
                        // Store divider position:
                        bottomPaneLastDividerPos = c.getRemoved().get(0).getPosition();
                    }
                });

                StackPane bottomPaneAndOverlay = new StackPane(bottomPane, bottomOverlay);
                SplitPane.setResizableWithParent(bottomPaneAndOverlay, false);
                StackPane topPaneAndOverlay = new StackPane(topPane, topOverlay);
                // SplitPane has a really odd behaviour.  If you set the divider position
                // and it starts a layout pass where the top and bottom both have enough
                // room to be their min sizes, but not enough for preferred, SplitPane
                // will resize, even if the bottom pane has setResizableWithParent set to false.
                // Which just seems wrong -- and it causes the divider to not resume
                // in the position which we faithfully saved and restored.  Over time the divider
                // creeps upward.  Here's the work-around: we set the top pane to also
                // not be resizable, which prevents SplitPane messing with the divider position.
                // But we only want to disable resizing of the top during loading; if the user
                // later resizes the pane, we do want to resize the top.  There's not an obvious
                // event to listen for to decide when to allow resizing (showing isn't the right time
                // because a layout pulse happens later), so here we use a timer.  Ugly but can't
                // see a better option.  5 seconds seems to be enough even on my slow machine
                // that it's loaded up, but hopefully short enough it doesn't interfere with actual
                // user resizes:
                SplitPane.setResizableWithParent(topPaneAndOverlay, false);
                JavaFXUtil.runAfter(Duration.seconds(5.0), () -> SplitPane.setResizableWithParent(topPaneAndOverlay, true));
                topBottomSplit = new SplitPane(topPaneAndOverlay, bottomPaneAndOverlay);
                JavaFXUtil.addStyleClass(topBottomSplit, "top-bottom-split");
                topBottomSplit.setOrientation(Orientation.VERTICAL);
                // Default position:
                topBottomSplit.setDividerPositions(0.8);
                // We add a mouse handler so that if you drag at the intersection of the split
                // panes' dividers, you move both dividers at once.
                EventHandler<MouseEvent> handler = new EventHandler<MouseEvent>()
                {
                    // We only want to do the CSS lookup once; then we stash the cursor property:
                    public ObjectProperty<Cursor> cursorProperty;
                    // Are we resizing both when dragging?
                    private boolean isResizingBoth;

                    @Override
                    @OnThread(Tag.FXPlatform)
                    public void handle(MouseEvent e)
                    {
                        if (cursorProperty == null)
                        {
                            cursorProperty = topBottomSplit.lookup(".top-bottom-split > .split-pane-divider").cursorProperty();
                        }

                        if (codePad == null)
                        {
                            cursorProperty.set(Cursor.V_RESIZE);
                            isResizingBoth = false;
                            return; // No special consideration needed.
                        }
                        if (e.getEventType() == MouseEvent.MOUSE_MOVED)
                        {
                            // Are we on top of the divider of the bottom pane?
                            double sceneX = bottomPane.getItems().get(0).localToScene(bottomPane.getItems().get(0).getBoundsInLocal()).getMaxX();
                            if (e.getSceneX() >= sceneX - 1 && e.getSceneX() <= sceneX + 8)
                            {
                                isResizingBoth = true;
                                // Show four pointed arrow:
                                cursorProperty.set(Cursor.MOVE);
                            }
                            else
                            {
                                isResizingBoth = false;
                                cursorProperty.set(Cursor.V_RESIZE);
                            }
                        }
                        else if (e.getEventType() == MouseEvent.MOUSE_DRAGGED)
                        {
                            if (isResizingBoth)
                            {
                                Bounds bottomBounds = bottomPane.localToScene(bottomPane.getBoundsInLocal());
                                bottomPane.setDividerPositions((e.getSceneX() - bottomBounds.getMinX()) / bottomBounds.getWidth());
                            }
                        }
                    }
                };
                topBottomSplit.addEventFilter(MouseEvent.MOUSE_MOVED, handler);
                topBottomSplit.addEventFilter(MouseEvent.MOUSE_DRAGGED, handler);

                BorderPane contentRoot = new BorderPane(topBottomSplit);
                JavaFXUtil.addStyleClass(contentRoot, "pmf-root");

                statusbar = new Label();
                BorderPane.setAlignment(statusbar, Pos.CENTER_LEFT);
                
                // create the bottom status area

                BorderPane statusArea = new BorderPane();
                BorderPane.setMargin(statusArea, new Insets(2, 0, 0, 0));
                statusArea.setCenter(statusbar);

                testStatusMessage = new Label();
                JavaFXUtil.addStyleClass(testStatusMessage, "test-status-message");
                // Hide when empty so padding doesn't show:
                testStatusMessage.managedProperty().bind(testStatusMessage.textProperty().isNotEmpty());
                testStatusMessage.visibleProperty().bind(testStatusMessage.textProperty().isNotEmpty());
                BorderPane.setAlignment(testStatusMessage, Pos.CENTER_LEFT);
                statusArea.setLeft(testStatusMessage);

                progressbar = new ActivityIndicatorFX();
                progressbar.setRunning(false);
                statusArea.setRight(new HBox(progressbar, machineIcon));
                
                contentRoot.setBottom(statusArea);
                BorderPane rootPlusMenu = new BorderPane(contentRoot);
                Scene scene = new Scene(rootPlusMenu);
                Config.addPMFStylesheets(scene);
                stage.setScene(scene);
                stage.setWidth(800.0);
                stage.setHeight(600.0);
                // This sets the window position so call it before showing:
                stageProperty.setValue(stage);
                paneProperty.setValue(rootPlusMenu);
                // Delay to let window positioner be ready:
                JavaFXUtil.runAfterCurrent(() -> {
                    if (stage.getX() < 0)
                        stage.setX(10);
                    if (stage.getY() < 0)
                        stage.setY(10);
                    stage.show();
                });
                //org.scenicview.ScenicView.show(stage.getScene());

                // If it should already be showing, do that now:
                if (showingTextEval.get())
                {
                    // This will enable it if it ends up showing:
                    showHideTextEval(true);
                }
                // Listen for future updates:
                JavaFXUtil.addChangeListener(showingTextEval, this::showHideTextEval);
                updateWindow();
            });

            // grey out certain functions if package not open.
            if (isEmptyFrame()) {
                enableFunctions(false);
            }
        }
        else
        {
            Platform.runLater(() -> {objbench = new ObjectBench(this);});
        }
    }

    /**
     * Open a PkgMgrFrame with no package. Packages can be installed into this
     * frame using the methods openPackage/closePackage.
     * @return The new, empty frame
     */
    public static PkgMgrFrame createFrame()
    {
        PkgMgrFrame frame = new PkgMgrFrame();
        BlueJEvent.addListener(frame);

        synchronized (PkgMgrFrame.class)
        {
            frames.add(frame);
        }

        Platform.runLater(() -> {
            JavaFXUtil.onceNotNull(frame.stageProperty, stage ->
                JavaFXUtil.addChangeListener(stage.focusedProperty(), focused -> {
                    if (focused.booleanValue())
                    {
                        recentFrame = frame;
                    }
                })
            );
        });
        
        return frame;
    }

    /**
     * Open a PkgMgrFrame with a package. This may create a new frame or return
     * an existing frame if this package is already being edited by a frame. If
     * an empty frame exists, that frame will be used to show the package.
     * @param aPkg The package to show in the frame
     * @return The new frame
     */
    @OnThread(Tag.Swing)
    public static PkgMgrFrame createFrame(Package aPkg, PkgMgrFrame parentWindow)
    {
        PkgMgrFrame pmf = findFrame(aPkg);

        if (pmf == null) {
            // check whether we've got an empty frame

            if (frameCount() == 1)
            {
                synchronized (PkgMgrFrame.class)
                {
                    pmf = frames.get(0);
                }
            }

            if ((pmf == null) || !pmf.isEmptyFrame())
                pmf = createFrame();

            pmf.openPackage(aPkg, parentWindow);
        }

        return pmf;
    }

    /**
     * Remove a frame from the set of currently open PkgMgrFrames. The
     * PkgMgrFrame must not be editing a package when this function is called.
     * @param frame The frame to close
     */
    @OnThread(Tag.FXPlatform)
    public static void closeFrame(PkgMgrFrame frame)
    {
        if (!frame.isEmptyFrame())
            throw new IllegalArgumentException();

        synchronized (PkgMgrFrame.class)
        {
            frames.remove(frame);
        }

        SwingUtilities.invokeLater(() -> BlueJEvent.removeListener(frame));

        PrefMgr.setFlag(PrefMgr.SHOW_TEXT_EVAL, frame.showingTextEval.get());
        javafx.stage.Window window = frame.getFXWindow();
        if (window != null)
            window.hide();
    }

    /**
     * Find a frame which is editing a particular Package and return it or
     * return null if it is not being edited
     * @param aPkg The package to search for
     * @return The frame editing this package, or null
     */
    @OnThread(Tag.Any)
    public synchronized static PkgMgrFrame findFrame(Package aPkg)
    {
        for (PkgMgrFrame pmf : frames) {
            if (!pmf.isEmptyFrame() && pmf.getPackage() == aPkg)
                return pmf;
        }
        return null;
    }

    /**
     * @return the number of currently open top level frames
     */
    @OnThread(Tag.Any)
    public synchronized static int frameCount()
    {
        return frames.size();
    }

    /**
     * Returns an array of all PkgMgrFrame objects. It can be an empty array if
     * none is found.
     * @return An array of all existing frames
     */
    @OnThread(Tag.Any)
    public synchronized static PkgMgrFrame[] getAllFrames()
    {
        PkgMgrFrame[] openFrames = new PkgMgrFrame[frames.size()];
        frames.toArray(openFrames);

        return openFrames;
    }

    /**
     * Find all PkgMgrFrames which are currently editing a particular project
     * 
     * @param proj
     *            the project whose packages to look for
     * 
     * @return an array of open PkgMgrFrame objects which are currently editing
     *         a package from this project, or null if none exist
     */
    public static PkgMgrFrame[] getAllProjectFrames(Project proj)
    {
        return getAllProjectFrames(proj, "");
    }

    /**
     * Find all PkgMgrFrames which are currently editing a particular project,
     * and which are below a certain point in the package hierarchy.
     * 
     * @param proj
     *            the project whose packages to look for
     * @param pkgPrefix
     *            the package name of a package to look for it and all its
     *            children ie if passed java.lang we would return frames for
     *            java.lang, and java.lang.reflect if they exist
     * 
     * @return an array of open PkgMgrFrame objects which are currently editing
     *         a package from this project and which have the package prefix
     *         specified, or null if none exist
     */
    public static PkgMgrFrame[] getAllProjectFrames(Project proj, String pkgPrefix)
    {
        List<PkgMgrFrame> list = new ArrayList<>();
        String pkgPrefixWithDot = pkgPrefix + ".";

        for (PkgMgrFrame pmf : getAllFrames()) {
            if (!pmf.isEmptyFrame() && pmf.getProject() == proj) {

                String fullName = pmf.getPackage().getQualifiedName();

                // we either match against the package prefix with a
                // dot added (this stops false matches against similarly
                // named package ie java.lang and java.language) or we
                // match the full name against the package prefix
                if (fullName.startsWith(pkgPrefixWithDot))
                    list.add(pmf);
                else if (fullName.equals(pkgPrefix) || (pkgPrefix.length() == 0))
                    list.add(pmf);
            }
        }

        if (list.isEmpty())
            return null;

        return list.toArray(new PkgMgrFrame[list.size()]);
    }

    /**
     * Gets the most recently used PkgMgrFrame
     * 
     * @return the PkgMgrFrame that currently has the focus
     */
    public static PkgMgrFrame getMostRecent()
    {
        if (recentFrame != null) {
            return recentFrame;
        }
        
        PkgMgrFrame[] allFrames = getAllFrames();

        // If there are no frames open, yet...
        if (allFrames.length < 1) {
            return null;
        }

        // Assume that the most recent is the first one. Not really the best
        // thing to do...
        PkgMgrFrame mostRecent = allFrames[0];

        return mostRecent;
    }
    
    /**
     * Handle a "display about dialog" request generated by the OS
     */
    public static void handleAbout()
    {
        new HelpAboutAction(getMostRecent()).actionPerformed(getMostRecent());
    }
    
    /**
     * Handle a "show preferences" request generated by the OS
     */
    public static void handlePreferences()
    {
        new PreferencesAction(getMostRecent()).actionPerformed(getMostRecent());
    }
    
    /**
     * Handle a quite request generated by the OS
     */
    public static void handleQuit()
    {
        new QuitAction(getMostRecent()).actionPerformed(getMostRecent());
    }

    /**
     * Display a short text message to the user. Without specifying a package,
     * this is done by showing the message in the status bars of all open
     * package windows.
     * @param message The message to show
     */
    public static void displayMessage(String message)
    {
        for (PkgMgrFrame frame : getAllFrames())
            frame.setStatus(message);
    }

    /**
     * Display a short text message in the frame of the specified package.
     * @param sourcePkg The package in whose window to display
     * @param message The message to show
     */
    public static void displayMessage(Package sourcePkg, String message)
    {
        PkgMgrFrame pmf = findFrame(sourcePkg);

        if (pmf != null)
            pmf.setStatus(message);
    }

    /**
     * Display a short text message in the frames of the specified project.
     * @param sourceProj The project whose frames to use to display
     * @param message The messahe to show
     */
    public static void displayMessage(Project sourceProj, String message)
    {
        PkgMgrFrame pmf[] = getAllProjectFrames(sourceProj);

        if (pmf != null) {
            for (PkgMgrFrame pmf1 : pmf) {
                if (pmf1 != null) {
                    pmf1.setStatus(message);
                }
            }
        }
    }

    /**
     * Display an error message in a dialogue attached to the specified package
     * frame.
     * @param sourcePkg The package whose frame to use
     * @param msgId The error message to display
     */
    public static void showError(Package sourcePkg, String msgId)
    {
        PkgMgrFrame pmf = findFrame(sourcePkg);

        if (pmf != null)
            Platform.runLater(() -> DialogManager.showErrorFX(pmf.getFXWindow(), msgId));
    }

    /**
     * Display a message in a dialogue attached to the specified package frame.
     * @param sourcePkg The package whose frame to use
     * @param msgId The message to display
     */
    public static void showMessage(Package sourcePkg, String msgId)
    {
        PkgMgrFrame pmf = findFrame(sourcePkg);

        if (pmf != null)
        {
            Platform.runLater(() -> DialogManager.showMessageFX(pmf.getFXWindow(), msgId));
        }
    }

    /**
     * Display a parameterised message in a dialogue attached to the specified
     * package frame.
     * @param sourcePkg The package whose frame to use
     * @param msgId The message to display
     * @param text The text parameter to insert into the message
     */
    public static void showMessageWithText(Package sourcePkg, String msgId, String text)
    {
        PkgMgrFrame pmf = findFrame(sourcePkg);

        if (pmf != null)
            Platform.runLater(() -> DialogManager.showMessageWithTextFX(pmf.getFXWindow(), msgId, text));
    }

    /**
     * Opens either a project from a directory or an archive.
     * 
     * @param projectPath The project to open.
     * @param pmf Optional parameter. Used for displaying dialogs and reuse
     *            if it is the empty frame.
     * @return True is successful
     */
    public static boolean doOpen(File projectPath, PkgMgrFrame pmf)
    {     
        boolean createdNewFrame = false;
        if(pmf == null && PkgMgrFrame.frames.size() > 0) {
            pmf = PkgMgrFrame.frames.get(0);
        }
        else if(pmf == null) {
            pmf = PkgMgrFrame.createFrame();
            createdNewFrame = true;
        }

        boolean openedProject = false;
        if (projectPath != null) {
            if (projectPath.isDirectory() || Project.isProject(projectPath.toString())) {
                if(pmf.openProject(projectPath.getAbsolutePath())) {
                    openedProject = true;
                }
            }
            else {
                if(pmf.openArchive(projectPath)) {
                    openedProject = true;
                }
            }
        }
        if(createdNewFrame && !openedProject) {
            // Close newly created frame if it was never used.
            PkgMgrFrame pmfFinal = pmf;
            Platform.runLater(() -> PkgMgrFrame.closeFrame(pmfFinal));
        }
        return openedProject;
    }
    
    /**
     * Close all frames which show packages from the specified project. This
     * causes the project itself to close.
     * @param project The project to be closed
     */
    public static void closeProject(Project project) 
    {
        PkgMgrFrame[] allFrames = getAllProjectFrames(project);

        if (allFrames != null) {
            for (PkgMgrFrame allFrame : allFrames) {
                Platform.runLater(() -> allFrame.doClose(true, true));
            }
        }
    }
    
    /**
     * accessor method for PageFormat object that can be used by various
     * printing subsystems eg. source code printing from editor
     * 
     * @return common PageFormat object representing page preferences
     */
    @OnThread(Tag.Any)
    public static synchronized PageFormat getPageFormat()
    {
        if (pageFormat == null) {
            pageFormat = PrinterJob.getPrinterJob().defaultPage();

        }
        //Important that this is set before the margins:
        int orientation = Config.getPropInteger("bluej.printer.paper.orientation", pageFormat.getOrientation());
        pageFormat.setOrientation(orientation);
        
        Paper paper = pageFormat.getPaper();
        int x = Config.getPropInteger("bluej.printer.paper.x", 72);
        int y = Config.getPropInteger("bluej.printer.paper.y", 72);
        int width = Config.getPropInteger("bluej.printer.paper.width", (int)paper.getWidth() - 72 - x);
        int height = Config.getPropInteger("bluej.printer.paper.height", (int)paper.getHeight() - 72 - y);
        paper.setImageableArea(x, y, width, height);
        //paper is a copy of pageFormat's paper, so we must use set again to make the changes:
        pageFormat.setPaper(paper);
        return pageFormat;
    }
    
    /**
     * set method for printing PageFormat. Called by other elements that may
     * manipulate pageformat, at this stage the source editor is the only
     * component that does. The assumption is that the PageFormat should be
     * uniform between all components that may want to send output to a printer.
     * 
     * @param page
     *            the new PageFormat
     */
    public static synchronized void setPageFormat(PageFormat page)
    {
        pageFormat = page;
        // We must get the measurements from the paper (which ignores orientation)
        // rather than page format (which takes it into account) because ultimately
        // we will use paper.setImageableArea to load the dimensions again
        Paper paper = pageFormat.getPaper();
        double x = paper.getImageableX();
        double y = paper.getImageableY();
        double width = paper.getImageableWidth();
        double height = paper.getImageableHeight();
        //The sizes are in points, so saving them as an integer should be precise enough:
        Config.putPropInteger("bluej.printer.paper.x", (int)x);
        Config.putPropInteger("bluej.printer.paper.y", (int)y);
        Config.putPropInteger("bluej.printer.paper.width", (int)width);
        Config.putPropInteger("bluej.printer.paper.height", (int)height);
        int orientation = pageFormat.getOrientation();
        Config.putPropInteger("bluej.printer.paper.orientation", orientation);

    }

    /**
     * Displays the package in the frame for editing
     * @param aPkg The package to edit
     */
    @OnThread(Tag.Swing)
    public void openPackage(Package aPkg, PkgMgrFrame parentWindow)
    {
        if (aPkg == null) {
            throw new NullPointerException();
        }

        // if we are already editing a package, close it and
        // open the new one
        if (this.pkg != null) {
            closePackage();
        }

        this.pkg = aPkg;

        if(! Config.isGreenfoot()) {
            this.editor = new PackageEditor(this, aPkg, this, showUsesProperty, showInheritsProperty, topOverlay);
            Platform.runLater(() -> {
                pkgEditorScrollPane.setContent(editor);
                editor.setOnDragOver(event -> {
                        Dragboard db = event.getDragboard();
                        if (db.hasFiles()) {
                            event.acceptTransferModes(TransferMode.COPY);
                        } else {
                            event.consume();
                        }
                });
    
                editor.setOnDragDropped(event -> {
                        Dragboard db = event.getDragboard();
                        boolean success = false;
                        if (db.hasFiles()) {
                            success = true;
                            SwingUtilities.invokeLater(() -> {addFiles(db.getFiles());});
                        }
                        event.setDropCompleted(success);
                        event.consume();
                });
                editorMousePressed = e -> clearStatus();
                editor.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, editorMousePressed);  // This mouse listener MUST be before
                editor.startMouseListening();   //  the editor's listener itself!
                addCtrlTabShortcut(editor);
            });
            aPkg.setEditor(this.editor);
            
            // fetch some properties from the package that interest us
            Properties p = aPkg.getLastSavedProperties();
            
            try {
                // -1 means use parent window
                String x_str = p.getProperty("package.editor.x", parentWindow == null ? "30" : "-1");
                String y_str = p.getProperty("package.editor.y", parentWindow == null ? "30" : "-1");
                
                int x = Integer.parseInt(x_str);
                int y = Integer.parseInt(y_str);
                
                if (x > (Config.screenBounds.width - 80))
                    x = Config.screenBounds.width - 80;
                
                if (y > (Config.screenBounds.height - 80))
                    y = Config.screenBounds.height - 80;

                int xFinal = x;
                int yFinal = y;
                
                String width = p.getProperty("package.frame.width");
                String height = p.getProperty("package.frame.height");
                
                String mainDivPos = p.getProperty("package.divider.vertical");
                String bottomDivPos = p.getProperty("package.divider.horizontal");
                
                Platform.runLater(() -> {
                    JavaFXUtil.onceNotNull(stageProperty, s -> {
                        if (xFinal == -1 || yFinal == -1)
                        {
                            s.setX(parentWindow.stageProperty.getValue().getX() + 20.0);
                            s.setY(parentWindow.stageProperty.getValue().getY() + 20.0);
                        }
                        else
                        {
                            s.setX(xFinal);
                            s.setY(yFinal);
                        }
                        if (width != null && height != null)
                        {
                            s.setWidth(Integer.parseInt(width));
                            s.setHeight(Integer.parseInt(height));
                        }
                        else
                        {
                            // Reasonable default size:
                            s.setWidth(800.0);
                            s.setHeight(600.0);
                        }
                        if (mainDivPos != null)
                        {
                            topBottomSplit.setDividerPositions(Double.parseDouble(mainDivPos));
                        }
                        else
                        {
                            topBottomSplit.setDividerPositions(0.8);
                        }
                        if (bottomDivPos != null)
                        {
                            // If code pad is already showing, just set the divider position:
                            if (bottomPane.getDividers().size() == 1)
                            {
                                bottomPane.setDividerPositions(Double.parseDouble(bottomDivPos));
                            }
                            else
                            {
                                // Set the last pos; our listener added to bottomPane's dividers
                                // will pick it up once a divider shows.
                                bottomPaneLastDividerPos = Double.parseDouble(bottomDivPos);
                            }
                        }
                    });
                });
            } catch (NumberFormatException e) {
                Debug.reportError("Could not read preferred project screen position");
            }
            
            String uses_str = p.getProperty("package.showUses", "true");
            String extends_str = p.getProperty("package.showExtends", "true");
            
            Platform.runLater(() -> {
                editor.setShowUses(uses_str.equals("true"));
                editor.setShowExtends(extends_str.equals("true"));
                editor.requestFocus();
                updateWindow();
            });
            
            enableFunctions(true); // changes menu items
            setVisible(true);
            
            Package pkgFinal = aPkg;
            // I hate FX/Swing GUI threading...
            Platform.runLater(() ->
                {
                    // runAfterCurrent so that FX finishes initialising the menu,
                    // then hop to Swing thread to actually change things:
                    JavaFXUtil.onceNotNull(this.viewMenuManager, vm -> JavaFXUtil.runPlatformLater(() -> SwingUtilities.invokeLater(() ->
                    {
                        vm.setMenuGenerator(new ViewExtensionMenu(pkgFinal));
                        vm.addExtensionMenu(pkgFinal.getProject());
                    })));
                    JavaFXUtil.onceNotNull(this.toolsMenuManager, vm -> JavaFXUtil.runPlatformLater(() -> SwingUtilities.invokeLater(() ->
                    {
                        vm.setMenuGenerator(new ToolsExtensionMenu(pkgFinal));
                        vm.addExtensionMenu(pkgFinal.getProject());
                    })));
                });
        
            teamActions = aPkg.getProject().getTeamActions();
            resetTeamActions();
            
            //update TeamSettings menu items.
            if (aPkg.getProject().getTeamSettingsController() != null && aPkg.getProject().getTeamSettingsController().isDVCS()) {
                commitMenuItem.setText(Config.getString("team.menu.commitPush"));
            } else {
                commitMenuItem.setText(Config.getString("team.menu.commit"));
            }

            aPkg.getProject().scheduleCompilation(true, CompileReason.LOADED, Config.isGreenfoot() ? CompileType.INDIRECT_USER_COMPILE : CompileType.ERROR_CHECK_ONLY, aPkg);
        }
        
        DataCollector.packageOpened(aPkg);

        extMgr.packageOpened(aPkg);
    }

    /**
     * Set the team controls to use the team actions for the project.
     */
    private void resetTeamActions()
    {
        // The reason this is necessary is because team actions are tied to
        // a project, not to a PkgMgrFrame. However, a PkgMgrFrame may be
        // empty and not associated with a project - in that case it has its
        // own TeamActionGroup. When a project is opened, the actions from
        // the project then need to be associated with the appropriate controls.

        StatusAction statusAction = teamActions.getStatusAction(this);
        UpdateDialogAction updateAction = teamActions.getUpdateAction(this);
        CommitCommentAction commitCommentAction = teamActions.getCommitCommentAction(this);
        ImportAction shareAction = teamActions.getImportAction(this);
        Platform.runLater(() -> {
            setButtonAction(statusAction, teamStatusButton, false);
            setButtonAction(updateAction, updateButton, false);
            setButtonAction(commitCommentAction, commitButton, false);
            setButtonAction(shareAction, teamShareButton, false);
        });
        teamSettingsMenuItem.setAction(teamActions.getTeamSettingsAction(this));
        
        shareProjectMenuItem.setAction(teamActions.getImportAction(this));
        statusMenuItem.setAction(teamActions.getStatusAction(this));
        commitMenuItem.setAction(teamActions.getCommitCommentAction(this));
        commitMenuItem.setText(Config.getString("team.menu.commit"));
        updateMenuItem.setAction(teamActions.getUpdateAction(this));
        updateMenuItem.setText(Config.getString("team.menu.update"));
        showLogMenuItem.setAction(teamActions.getShowLogAction(this));
    }

    /**
     * Closes the current package.
     */
    @OnThread(Tag.Swing)
    public void closePackage()
    {
        if (isEmptyFrame()) {
            return;
        }
        Package thePkg = getPackage();
        
        extMgr.packageClosing(thePkg);

        if(! Config.isGreenfoot()) {
            this.toolsMenuManager.get().setMenuGenerator(new ToolsExtensionMenu(thePkg));
            this.viewMenuManager.get().setMenuGenerator(new ViewExtensionMenu(thePkg));
            
            ObjectBench bench = getObjectBench();
            String uniqueId = getProject().getUniqueId();
            Platform.runLater(() -> {
                bench.removeAllObjects(uniqueId);
                clearTextEval();
                if (codePad != null)
                    codePad.setDisable(true);
            });

            // Take a copy because we're about to null it:
            PackageEditor oldEd = editor;
            Platform.runLater(() -> {
                oldEd.removeEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, editorMousePressed);
                oldEd.graphClosed();
                pkgEditorScrollPane.setContent(null);
            });
            enableFunctions(false);
        }

        getPackage().closeAllEditors();
        
        DataCollector.packageClosed(thePkg);

        Project proj = getProject();

        editor = null;
        synchronized (this)
        {
            this.pkg = null;
        }

        // if there are no other frames editing this project, we close
        // the project
        if (PkgMgrFrame.getAllProjectFrames(proj) == null) {
            Project.cleanUp(proj);
        }
    }

    /**
     * Override standard show to add de-iconify and bring-to-front.
     * @param visible True to make this visible; false to hide.
     */
    public void setVisible(boolean visible)
    {
        if(!visible) {
            Platform.runLater(() -> {
                JavaFXUtil.onceNotNull(stageProperty, Stage::hide);
            });
        }
        else if (!Config.isGreenfoot()) {
            //setState(Frame.NORMAL);
            Platform.runLater(() -> {
                JavaFXUtil.onceNotNull(stageProperty, s -> {
                    s.show();
                    s.toFront();
                });
            });
        }
    }
    
    /**
     * Return the package shown by this frame.
     * 
     * This call should be bracketed by a call to isEmptyFrame() before use.
     * @return The package shown by this frame
     */
    @OnThread(Tag.Any)
    public synchronized Package getPackage()
    {
        return pkg;
    }

    /**
     * Return the project of the package shown by this frame.
     * @return The project of the package shown by this frame
     */
    @OnThread(Tag.Any)
    public synchronized Project getProject()
    {
        return pkg == null ? null : pkg.getProject();
    }
       
    /**
     * A call to this should bracket all uses of getPackage() and editor.
     * @return True is this frame is currently empty
     */
    @OnThread(Tag.Any)
    public synchronized boolean isEmptyFrame()
    {
        return pkg == null;
    }

    /**
     * Set the window title to show the current package name.
     */
    @OnThread(Tag.FXPlatform)
    private void updateWindowTitle()
    {
        
        if (isEmptyFrame()) {
            setTitle("BlueJ");
        }
        else {
            String title = Config.getString("pkgmgr.title") + getProject().getProjectName();

            if (!getPackage().isUnnamedPackage())
                title = title + "  [" + getPackage().getQualifiedName() + "]";
            
            if(getProject().isTeamProject())
                title = title + " (" + Config.getString("team.project.marker") + ")";

            setTitle(title);
        }
    }

    @OnThread(Tag.FXPlatform)
    private void setTitle(String title)
    {
        JavaFXUtil.onceNotNull(stageProperty, stage -> stage.setTitle(title));
    }

    /**
     * Update the window title and show needed messages
     */
    @OnThread(Tag.FXPlatform)
    private void updateWindow()
    {
        if (isEmptyFrame()) {
            //TODO
            //Platform.runLater(() -> classScroller.setContent(noProjectMessagePanel));
        }
        updateWindowTitle();
    }

    /**
     * Display a message in the status bar of the frame
     * @param status The status to display
     */
    @OnThread(value = Tag.Any)
    public final void setStatus(final String status)
    {
         JavaFXUtil.runNowOrLater(() -> {
             if (statusbar != null)
                 statusbar.setText(status);
         });
        
    }

    /**
     * Start the activity indicator. Call from any thread.
     */
    @OnThread(Tag.Any)
    public void startProgress()
    {
        JavaFXUtil.runNowOrLater(() -> progressbar.setRunning(true));
    }

    /**
     * Stop the activity indicator. Call from any thread.
     */
    @OnThread(Tag.Any)
    public void stopProgress()
    {
        JavaFXUtil.runNowOrLater(() -> progressbar.setRunning(false));
    }

    /**
     * Clear status bar of the frame.  Call from any thread.
     */
    @OnThread(Tag.Any)
    public void clearStatus()
    {
       JavaFXUtil.runNowOrLater(() -> {
           if (statusbar != null)
               statusbar.setText(" ");
       });
    }

    /**
     * Set the frames cursor to a WAIT_CURSOR while system is busy
     * @param wait If true, show wait cursor; otherwise back to default cursor
     */
    public void setWaitCursor(boolean wait)
    {
        Platform.runLater(() -> {
            stageProperty.getValue().getScene().setCursor(wait ? Cursor.WAIT : null);
        });
    }

    /**
     * Return the object bench.
     * @return The object bench of this frame
     */
    public ObjectBench getObjectBench()
    {
        return objbench;
    }

    /**
     * Return the Code Pad component.
     * @return The code pad of this frame
     */
    @OnThread(Tag.FXPlatform)
    public CodePad getCodePad()
    {
        return codePad;
    }



    // --- below are implementations of particular user actions ---
    // These are broken into "interactive" methods (which can display dialogs
    // etc) and "non-interactive". In general interactive methods delegate to
    // the non-interactive variants.

    // --- non-interactive methods ---

    /**
     * Deal with an event generated by a target in the package we are currently
     * editing.
     * @param e The event to process
     */
    @Override
    public void targetEvent(PackageEditorEvent e)
    {
        int evtId = e.getID();

        switch(evtId) {
            case PackageEditorEvent.TARGET_CALLABLE :
                // user has initiated method call or constructor
                callMethod(e.getCallable());
                break;

            case PackageEditorEvent.TARGET_REMOVE :
                // user has initiated target "remove" option
                ((Target) e.getSource()).remove();
                break;

            case PackageEditorEvent.TARGET_OPEN :
                // user has initiated a package open operation
                openPackageTarget(e.getName());
                break;

            case PackageEditorEvent.TARGET_RUN :
                // user has initiated a run operation
                ClassTarget ct = (ClassTarget) e.getSource();
                ct.getRole().run(this, ct, e.getName());
                break;

            case PackageEditorEvent.TARGET_BENCHTOFIXTURE :
                // put objects on object bench into fixtures
                objectBenchToTestFixture((ClassTarget) e.getSource());
                break;

            case PackageEditorEvent.TARGET_FIXTURETOBENCH :
                // put objects on object bench into fixtures
                testFixtureToObjectBench((ClassTarget) e.getSource());
                break;

            case PackageEditorEvent.TARGET_MAKETESTCASE :
                // start recording a new test case
                makeTestCase((ClassTarget) e.getSource());
                break;

            case PackageEditorEvent.OBJECT_PUTONBENCH :
                // "Get" object from object inspector
                DebuggerObject gotObj = e.getDebuggerObject();

                String name = getProject().getDebugger().guessNewName(gotObj);
                Platform.runLater(() -> {
                    boolean tryAgain = true;
                    do
                    {
                        String newObjectName = e.askForName() ? DialogManager.askStringFX((javafx.stage.Window)e.getSource(), "getobject-new-name",
                            name) : name;

                        if (newObjectName == null)
                        {
                            tryAgain = false; // cancelled
                        }
                        else if (JavaNames.isIdentifier(newObjectName))
                        {
                            SwingUtilities.invokeLater(() -> {
                                DataCollector.benchGet(getPackage(), newObjectName, e.getDebuggerObject().getClassName(), getTestIdentifier());
                                putObjectOnBench(newObjectName, e.getDebuggerObject(), e.getIType(), e.getInvokerRecord(), e.getAnimateFromScenePoint());
                            });
                            tryAgain = false;
                        }
                        else
                        {
                            DialogManager.showErrorFX((javafx.stage.Window)e.getSource(), "must-be-identifier");
                        }
                    } while (tryAgain);
                });
                break;
        }
    }
    
    /* (non-Javadoc)
     * @see bluej.pkgmgr.PackageEditorListener#recordInteraction(bluej.testmgr.record.InvokerRecord)
     */
    @Override
    public void recordInteraction(InvokerRecord ir)
    {
        getObjectBench().addInteraction(ir);
    }

    // --- interactive methods ---
    
    /**
     * Gets the current test identifier (used to identify tests during the data recording)
     * @return The current test id
     */
    public int getTestIdentifier()
    {
        return testIdentifier;
    }
   
    /**
     * Create a new project and display it in a frame.
     * @param dirName           The directory to create the project in
     * @return     true if successful, false otherwise
     */
    @OnThread(Tag.Swing)
    public boolean newProject(String dirName)
    {
        if (Project.createNewProject(dirName)) {
            Project proj = Project.openProject(dirName);
            
            Package unNamedPkg = proj.getPackage("");
            
            if (isEmptyFrame()) {
                openPackage( unNamedPkg, this );
            }
            else {
                PkgMgrFrame pmf = createFrame( unNamedPkg, this);
                pmf.setVisible(true);
            }    
            return true;
        }
        return false;
    }

    /**
     * This is primarily needed for the extensions, who want a reference to a AWT frame.
     * Thankfully, JavaFX does use a AWT frame to back the JavaFX window, we
     * just need to do some jiggery-pokery to get the reference.
     * @return
     */
    public Frame getWindow()
    {
        Window windowAncestor = SwingUtilities.getWindowAncestor(dummySwingNode.getContent());
        return (Frame) windowAncestor;
    }

    /**
     * Import a project from a directory into the current package. 
     * @param dir               The directory to import
     * @param showFailureDialog True to show a dialog with files which failed
     *                          to copy
     * @return An array of Files which failed to copy
     */
    public File[] importProjectDir(File dir, boolean showFailureDialog)
    {
        // recursively copy files from import directory to package directory
        File[] fails = FileUtility.recursiveCopyFile(dir, getPackage().getPath());

        // if we have any files which failed the copy, we show them now
        if (fails != null && showFailureDialog) {
            SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
            File[] failsFinal = fails;
            Platform.runLater(() -> {
                ImportFailedDialog importFailedDlg = new ImportFailedDialog(getFXWindow(), Arrays.asList(failsFinal));
                importFailedDlg.showAndWait();
                loop.exit();
            });
            loop.enter();
        }

        // add bluej.pkg files through the imported directory structure
        List<File> dirsToConvert = Import.findInterestingDirectories(getPackage().getPath());
        Import.convertDirectory(dirsToConvert);

        // reload all the packages (which discovers classes which may have
        // been added by the import)
        getProject().reloadAll();
        
        return fails;
    }

    /**
     * Creates a new class using the given name and template
     * 
     * @param name
     *            is not a fully qualified class name
     * @param template
     *            can be null in this case no template will be generated
     * @param showErr
     *            true if a "duplicate name" dialog should be shown if
     *            the named class already exists
     * @param x The X coordinate in the class diagram, or -1 for auto-place
     * @param y The Y coordinate in the class diagram, or -1 for auto-place
     * @return  true if successful, false is the named class already exists
     */
    public boolean createNewClass(String name, String template, SourceType sourceType, boolean showErr, double x, double y)
    {
        Package thePkg = getPackage();
        // check whether name is already used
        if (thePkg.getTarget(name) != null) {
            Platform.runLater(() -> DialogManager.showErrorFX(getFXWindow(), "duplicate-name"));
            return false;
        }

        //check if there already exists a class in a library with that name 
        String[] conflict=new String[1];
        Class<?> c = thePkg.loadClass(thePkg.getQualifiedName(name));
        if (c != null){
            if (! Package.checkClassMatchesFile(c, new File(getPackage().getPath(), name + ".class"))) {
                conflict[0]=Package.getResourcePath(c);
                AtomicBoolean shouldContinue = new AtomicBoolean();
                SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
                Platform.runLater(() -> {
                    boolean cont = DialogManager.askQuestionFX(getFXWindow(), "class-library-conflict", conflict) != 0;
                    shouldContinue.set(cont);
                    loop.exit();
                });
                loop.enter();

                if (!shouldContinue.get())
                    return false;
            }
        }

        ClassTarget target = new ClassTarget(thePkg, name, template);

        if ( template != null ) { 
            boolean success = target.generateSkeleton(template, sourceType);
            if (! success)
                return false;
        }

        thePkg.addTarget(target);
        
        Platform.runLater(() -> {
            if (editor != null) {
                if (x == -1)
                    editor.findSpaceForVertex(target);
                else
                    target.setPos((int)x, (int)y);
                JavaFXUtil.scrollTo(pkgEditorScrollPane, target.getNode());
            }
        });

        if (target.getRole() instanceof UnitTestClassRole) {
            thePkg.compileQuiet(target, CompileReason.NEW_CLASS, CompileType.INDIRECT_USER_COMPILE);
        }

        // Schedule compilation of new class:
        thePkg.getProject().scheduleCompilation(false, CompileReason.NEW_CLASS, CompileType.INDIRECT_USER_COMPILE, target);
        
        DataCollector.addClass(thePkg, target);
        
        return true;
    }

    /**
     * Allow the user to select a directory into which we create a project.
     */
    public void doNewProject()
    {
        String title = Config.getString( "pkgmgr.newPkg.title" );

        Platform.runLater(() -> {
            File newnameFile = FileUtility.getSaveProjectFX(getFXWindow(), title);
            if (newnameFile == null)
                return;
            SwingUtilities.invokeLater(() -> {
                if (! newProject(newnameFile.getAbsolutePath()))
                {
                    Platform.runLater(() -> DialogManager.showErrorWithTextFX(null, "cannot-create-directory", newnameFile.getPath()));
                }
            });
        });
    }

    /**
     * Open a dialog that lets the user choose a project. The project selected
     * is opened in a frame.
     */
    public void doOpen()
    {
        Platform.runLater(() -> {
            File choice = FileUtility.getOpenProjectFX(getFXWindow());
            if (choice != null)
            {
                SwingUtilities.invokeLater(() -> {
                    PkgMgrFrame.doOpen(choice, this);
                });
            }
        });
    }

    public void doOpenNonBlueJ()
    {
        Platform.runLater(() -> {
            File choice = FileUtility.getOpenDirFX(getFXWindow(), Config.getString("pkgmgr.openNonBlueJPkg.title"), true);
            if (choice != null)
            {
                SwingUtilities.invokeLater(() -> {
                    PkgMgrFrame.doOpenNonBlueJ(choice, this);
                });
            }
        });
    }

    public void doOpenArchive()
    {
        Platform.runLater(() -> {
            File archiveFile = FileUtility.getOpenArchiveFX(getFXWindow(), null, true);
            SwingUtilities.invokeLater(() -> PkgMgrFrame.doOpen(archiveFile, this));
        });
    }

    /**
     * Open the project specified by 'projectPath'. Return false if not
     * successful. Displays a warning dialog if the opened project resides in
     * a read-only directory.
     */
    private boolean openProject(String projectPath)
    {
        Project openProj = Project.openProject(projectPath);
        if (openProj == null)
            return false;
        else {
            Package initialPkg = openProj.getPackage(openProj.getInitialPackageName());

            PkgMgrFrame pmf = findFrame(initialPkg);

            if (pmf == null) {
                if (isEmptyFrame()) {
                    pmf = this;
                    openPackage(initialPkg, this);
                }
                else {
                    pmf = createFrame(initialPkg, this);
                }
            }

            pmf.setVisible(true);

            if (Config.isGreenfoot())
            {
                for (PkgMgrFrame pkgMgrFrame : getAllFrames())
                {
                    if (Config.isGreenfootStartupProject(pkgMgrFrame.getProject().getProjectDir()))
                    {
                        Platform.runLater(() -> pkgMgrFrame.doClose(false, false));
                        break; // Will only be one, and don't want concurrent modification exception
                    }
                }
            }

            return true;
        }
    }
    
    /**
     * Open a dialog that lets a user convert existing Java source into a BlueJ
     * project.
     * 
     * The project selected is opened in a frame.
     */
    public static void doOpenNonBlueJ(File dirName, PkgMgrFrame pmf)
    {
        File absDirName = dirName.getAbsoluteFile();

        // First confirm the chosen file exists
        if (! absDirName.exists()) {
            // file doesn't exist
            Platform.runLater(() -> DialogManager.showErrorFX(pmf.getFXWindow(), "file-does-not-exist"));
            return;
        }
        
        if (absDirName.isDirectory()) {
            // Check to make sure it's not already a project
            if (Project.isProject(absDirName.getPath())) {
                Platform.runLater(() -> DialogManager.showErrorFX(pmf.getFXWindow(), "open-non-bluej-already-bluej"));
                return;
            }

            // Try and convert it to a project
            if (! Import.convertNonBlueJ(pmf::getFXWindow, absDirName))
                return;
            
            // then construct it as a project
            pmf.openProject(absDirName.getPath());
        }
    }

    /**
     * Open an archive file (jar or same contents with other extensions) as a
     * BlueJ project. The file contents are extracted, the containing directory
     * is then converted into a BlueJ project if necessary, and opened.
     */
    private boolean openArchive(File archive)
    {
        // Determine the output path.
        File oPath = Utility.maybeExtractArchive(archive, this::getFXWindow);
        
        if (oPath == null)
            return false;
        
        if (Project.isProject(oPath.getPath())) {
            return openProject(oPath.getPath());
        }
        else {
            // Convert to a BlueJ project
            if (Import.convertNonBlueJ(this::getFXWindow, oPath)) {
                return openProject(oPath.getPath());
            }
            else {
                return false;
            }
        }        
    }

    /**
     * Perform a user initiated close of this frame/package.
     * 
     * There are two different methods for the user to initiate a close. One is
     * through the "Close" menu item and the other is with the windows close
     * button. We want slightly different behaviour for these two cases.
     * @param keepLastFrame If true, keep the frame visible.
     * @param doSave If true, do a save before closing
     */
    @OnThread(Tag.FXPlatform)
    public void doClose(boolean keepLastFrame, boolean doSave)
    {
        if (doSave) {
            doSave();
        }

        // If only one frame and this was from the menu
        // "close", close should close existing package rather
        // than remove frame

        if (frameCount() == 1) {
            if (keepLastFrame && !Config.isGreenfoot()) { // close package, leave frame, but not for greenfoot
                SwingUtilities.invokeLater(() -> {
                    closePackage();
                    testRecordingEnded(); // disable test controls
                    updateRecentProjects();
                }); // changes menu items
                updateWindow();

                FXMenuManager vm = viewMenuManager.get();
                FXMenuManager tm = toolsMenuManager.get();
                SwingUtilities.invokeLater(() -> {
                    tm.addExtensionMenu(null);
                    vm.addExtensionMenu(null);
                });
            }
            else { // all frames gone, lets quit
                bluej.Main.doQuit();
            }
        }
        else {
            SwingUtilities.invokeLater(() -> {
                closePackage();
                Platform.runLater(() -> PkgMgrFrame.closeFrame(this));
            }); // remove package and frame

        }
    }
    
    /**
     * Save this package. Don't ask questions - just do it.
     */
    @OnThread(Tag.FXPlatform)
    public synchronized void doSave()
    {
        if (isEmptyFrame()) {
            return;
        }
        
        // store the current editor size in the bluej.pkg file
        Properties p;
        if (pkg.isUnnamedPackage()) {
            // The unnamed package also contains project properties
            p = getProject().getProjectProperties();
            getProject().saveEditorLocations(p);
            getProject().getImportScanner().saveCachedImports();
        }
        else {
            p = new Properties();
        }
        
        if(!Config.isGreenfoot()) {
            // When we were using Swing, we stored the package editor width and height
            // and set them on load.  We don't do that any more in FX, but in case
            // this project is saved in BlueJ 4 and loaded in BlueJ 3, we don't want to 
            // have the position go wild, so we still store them even though we never use them:
            p.put("package.editor.width", Integer.toString((int)pkgEditorScrollPane.getViewportBounds().getWidth()));
            p.put("package.editor.height", Integer.toString((int)pkgEditorScrollPane.getViewportBounds().getHeight()));
            p.put("objectbench.width", Integer.toString((int)objbench.getViewportBounds().getWidth()));
            p.put("objectbench.height", Integer.toString((int)objbench.getViewportBounds().getHeight()));

            // These are the actual ones we use in FX:
            p.put("package.editor.x", Integer.toString((int)stageProperty.getValue().getX()));
            p.put("package.editor.y", Integer.toString((int)stageProperty.getValue().getY()));

            p.put("package.frame.width", Integer.toString((int)stageProperty.getValue().getWidth()));
            p.put("package.frame.height", Integer.toString((int)stageProperty.getValue().getHeight()));

            p.put("package.divider.vertical", Double.toString(topBottomSplit.getDividerPositions()[0]));
            if (bottomPane.getDividers().size() == 1)
            {
                p.put("package.divider.horizontal", Double.toString(bottomPane.getDividerPositions()[0]));
            }
            else
            {
                // If it's not showing, use the position from last time it was showing:
                p.put("package.divider.horizontal", Double.toString(bottomPaneLastDividerPos));
            }
    
            p.put("package.showUses", Boolean.toString(showUsesProperty.get()));
            p.put("package.showExtends", Boolean.toString(showInheritsProperty.get()));
        }
        pkg.save(p);
    }
        
    /**
     * Import into a new project or import into the current project.
     */
    public void doImport()
    {
        Platform.runLater(() -> {
            // prompt for the directory to import from
            File importDir = FileUtility.getOpenDirFX(getFXWindow(), Config.getString("pkgmgr.importPkg.title"), false);

            if (importDir == null)
                return;

            if (!importDir.isDirectory())
                return;

            SwingUtilities.invokeLater(() -> {
                // if we are an empty then we shouldn't go on (we shouldn't get
                // here)
                if (isEmptyFrame())
                    return;

                // recursively copy files from import directory to package directory
                importProjectDir(importDir, true);
            });
        });
    }
    
    /**
     * Implementation of the "Add Class from File" user function
     */
    public void doAddFromFile()
    {
        Platform.runLater(() -> {
            // multi selection file dialog that shows .java and .class files
            List<File> classes = FileUtility.getMultipleFilesFX(getFXWindow(), Config.getString("pkgmgr.addClass.title"), FileUtility.getJavaStrideSourceFilterFX());

            if (classes == null || classes.isEmpty())
                return;

            SwingUtilities.invokeLater(() -> importFromFile(classes));
        });
    }

    /**
     * Add a given set of Java source files as classes to this package.
     * @param classes The classes to add
     */
    public void addFiles(List<File> classes)
    {
        importFromFile(classes);
    }

    /**
     * Add the given set of Java source files as classes to this package.
     */
    private void importFromFile(List<File> classes)
    {
        Map<Integer, String> errorNames = new HashMap<>();
        errorNames.put(Package.FILE_NOT_FOUND, "file-does-not-exist");
        errorNames.put(Package.ILLEGAL_FORMAT, "cannot-import");
        errorNames.put(Package.CLASS_EXISTS, "duplicate-name");
        errorNames.put(Package.COPY_ERROR, "error-in-import");

        // if there are errors this will potentially bring up multiple error
        // dialogs
        // these could be aggregated however the error messages may be different
        // for each error
        for (File cls : classes) {
            int result = getPackage().importFile(cls);
            if (errorNames.containsKey(result))
            {
                Platform.runLater(() -> DialogManager.showErrorWithTextFX(getFXWindow(), errorNames.get(result), cls.getName()));
            }
        }
    }

    /**
     * Implementation of the "Export" user function
     */
    public void doExport()
    {
        if (exporter == null) {
            exporter = new ExportManager(this);
        }
        exporter.export();
    }

    /**
     * Creates a page setup dialog to alter page dimensions.
     *  
     */
    public void doPageSetup()
    {
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pfmt = job.pageDialog(getPageFormat());
        setPageFormat(pfmt);
    }

    /**
     * Implementation of the "print" user function
     */
    public void doPrint()
    {
        if (projectPrintDialog == null)
            projectPrintDialog = new ProjectPrintDialog(this);

        if (projectPrintDialog.display()) {
            PackagePrintManager printManager = new PackagePrintManager(this.getPackage(), getPageFormat(),
                    projectPrintDialog);
            printManager.start();
        }
    }

    /**
     * Preferences menu was chosen.
     */
    public void showPreferences( )
    {
        Platform.runLater(() -> PrefMgrDialog.showDialog());
    }

    /**
     * About menu was chosen.
     */
    public void aboutBlueJ()
    {
        Platform.runLater(() -> {
            AboutBlueJ about = new AboutBlueJ(stageProperty.getValue(), bluej.Boot.BLUEJ_VERSION);
            about.showAndWait();
        });
    }

    /**
     * Copyright menu item was chosen.
     */
    public void showCopyright()
    {
        Platform.runLater(() -> 
            DialogManager.showTextFX(getFXWindow(), Arrays.asList(
                Config.getString("menu.help.copyright.line0"), " ",
                Config.getString("menu.help.copyright.line1"), Config.getString("menu.help.copyright.line2"),
                Config.getString("menu.help.copyright.line3"), Config.getString("menu.help.copyright.line4")
                ).stream().collect(Collectors.joining("\n")))
        );
    }

    /**
     * Interactively call a class (ie static) method or a class constructor
     */
    private void callMethod(final CallableView cv)
    {
        ResultWatcher watcher = null;

        if (cv instanceof ConstructorView) {
            // if we are constructing an object, create a watcher that waits for
            // completion of the call and then places the object on the object
            // bench
            watcher = new ResultWatcher() {
                @Override
                public void beginCompile()
                {
                    setWaitCursor(true);
                    setStatus(Config.getString("pkgmgr.creating"));
                }
                
                @Override
                public void beginExecution(InvokerRecord ir)
                {
                    BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, ir);
                    setWaitCursor(false);
                }
                
                @Override
                public void putResult(DebuggerObject result, String name, InvokerRecord ir)
                {
                    ExecutionEvent executionEvent = new ExecutionEvent(pkg, cv.getClassName(), null);
                    executionEvent.setParameters(cv.getParamTypes(false), ir.getArgumentValues());
                    executionEvent.setResult(ExecutionEvent.NORMAL_EXIT);
                    executionEvent.setResultObject(result);
                    BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);

                    Project proj = getPackage().getProject();
                    Platform.runLater(() -> proj.updateInspectors());
                    setStatus(Config.getString("pkgmgr.createDone"));
                    
                    // this shouldn't ever happen!! (ajp 5/12/02)
                    if ((name == null) || (name.length() == 0))
                        name = "result";

                    if (result != null) {
                        ObjectWrapper wrapper = ObjectWrapper.getWrapper(PkgMgrFrame.this, getObjectBench(), result,
                                result.getGenType(), name);
                        getObjectBench().addObject(wrapper);

                        getPackage().getDebugger().addObject(pkg.getId(), wrapper.getName(), result);

                        getObjectBench().addInteraction(ir);
                    }
                    else {
                        // This shouldn't happen, but let's play it safe.
                    }
                }

                @Override
                public void putError(String msg, InvokerRecord ir)
                {
                    setStatus("");
                    setWaitCursor(false);
                }
                
                @Override
                public void putException(ExceptionDescription exception, InvokerRecord ir)
                {
                    ExecutionEvent executionEvent = new ExecutionEvent(pkg, cv.getClassName(), null);
                    executionEvent.setParameters(cv.getParamTypes(false), ir.getArgumentValues());
                    executionEvent.setResult(ExecutionEvent.EXCEPTION_EXIT);
                    executionEvent.setException(exception);
                    BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);
                    
                    setStatus("");
                    getPackage().exceptionMessage(exception);
                    Project proj = getPackage().getProject();
                    Platform.runLater(() -> proj.updateInspectors());
                }
                
                @Override
                public void putVMTerminated(InvokerRecord ir)
                {
                    ExecutionEvent executionEvent = new ExecutionEvent(pkg, cv.getClassName(), null);
                    executionEvent.setParameters(cv.getParamTypes(false), ir.getArgumentValues());
                    executionEvent.setResult(ExecutionEvent.TERMINATED_EXIT);
                    BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);
                    
                    setStatus("");
                }
            };
        }
        else if (cv instanceof MethodView) {
            final MethodView mv = (MethodView) cv;

            // create a watcher
            // that waits for completion of the call and then displays the
            // result (or does nothing if void)
            watcher = new ResultWatcher() {
                private final ExpressionInformation expressionInformation = new ExpressionInformation(mv, mv.getName());

                @Override
                public void beginCompile()
                {
                    setWaitCursor(true);
                    if (mv.isMain()) {
                        getProject().removeClassLoader();
                        getProject().newRemoteClassLoaderLeavingBreakpoints();
                    }
                }
                
                @Override
                public void beginExecution(InvokerRecord ir)
                {
                    BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, ir);
                    setWaitCursor(false);
                }
                
                @Override
                public void putResult(DebuggerObject result, String name, InvokerRecord ir)
                {
                    ExecutionEvent executionEvent = new ExecutionEvent(pkg, cv.getClassName(), null);
                    executionEvent.setMethodName(mv.getName());
                    executionEvent.setParameters(cv.getParamTypes(false), ir.getArgumentValues());
                    executionEvent.setResult(ExecutionEvent.NORMAL_EXIT);
                    executionEvent.setResultObject(result);
                    BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);

                    Project proj = getPackage().getProject();
                    Platform.runLater(() -> proj.updateInspectors());
                    expressionInformation.setArgumentValues(ir.getArgumentValues());
                    getObjectBench().addInteraction(ir);

                    // a void result returns a name of null
                    if (name == null)
                        return;

                    //The result can be null when terminating the program while
                    // at a breakpoint in a method that has a return value.
                    if (result == null)
                        return;

                    Project project = getProject();
                    Package pkg = getPackage();
                    Platform.runLater(() -> {
                        project.getResultInspectorInstance(result, name, pkg, ir,
                            expressionInformation, PkgMgrFrame.this.getFXWindow());
                    });
                }

                @Override
                public void putError(String msg, InvokerRecord ir)
                {
                    setWaitCursor(false);
                }
                
                @Override
                public void putException(ExceptionDescription exception, InvokerRecord ir)
                {
                    ExecutionEvent executionEvent = new ExecutionEvent(pkg, cv.getClassName(), null);
                    executionEvent.setParameters(cv.getParamTypes(false), ir.getArgumentValues());
                    executionEvent.setResult(ExecutionEvent.EXCEPTION_EXIT);
                    executionEvent.setException(exception);
                    BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);

                    Project proj = getPackage().getProject();
                    Platform.runLater(() -> proj.updateInspectors());
                    getPackage().exceptionMessage(exception);
                }
                
                @Override
                public void putVMTerminated(InvokerRecord ir)
                {
                    ExecutionEvent executionEvent = new ExecutionEvent(pkg, cv.getClassName(), null);
                    executionEvent.setParameters(cv.getParamTypes(false), ir.getArgumentValues());
                    executionEvent.setResult(ExecutionEvent.TERMINATED_EXIT);
                    BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);
                }
            };
        }

        // create an Invoker to handle the actual invocation
        if (checkDebuggerState()) {
            new Invoker(this, cv, watcher).invokeInteractive();
        }
    }

    /**
     * Open a package target.
     */
    private void openPackageTarget(String newname)
    {
        PkgMgrFrame pmf;
        Package p = getPackage().getProject().getPackage(newname);

        if ((pmf = findFrame(p)) == null) {
            pmf = createFrame(p, this);
        }
        pmf.setVisible(true);
    }

    /**
     * Create the text fixture method in the indicated target on from the
     * current objects on the object bench.
     */
    private void objectBenchToTestFixture(ClassTarget target)
    {
        if (target.getRole() instanceof UnitTestClassRole) {
            UnitTestClassRole utcr = (UnitTestClassRole) target.getRole();

            utcr.doBenchToFixture(this, target);
        }
    }

    /**
     * Build the text fixture specified in the indicated target on the object
     * bench.
     */
    private void testFixtureToObjectBench(ClassTarget target)
    {
        if (target.getRole() instanceof UnitTestClassRole) {
            UnitTestClassRole utcr = (UnitTestClassRole) target.getRole();
            utcr.doFixtureToBench(this, target);
        }
    }

    /**
     * Create a test method for the indicated target.
     */
    private void makeTestCase(ClassTarget target)
    {
        if (target.getRole() instanceof UnitTestClassRole) {
            UnitTestClassRole utcr = (UnitTestClassRole) target.getRole();
            Platform.runLater(() -> {
                teamAndTestFoldout.expandedProperty().set(true);
                utcr.doMakeTestCase(this, target);
            });
        }
    }


    /**
     * Place a given object onto the object bench. This is done by creating an object wrapper
     * for the internal object, which can then be added to the bench.
     * 
     * @param newInstanceName  Name for the instance on the bench.
     * @param object    The internal object to be placed.
     * @param iType    The "interface type" of the object. This is the type of the object
     *               for purposes of method calls etc if the actual type is inaccessible
     *               (private to another package or class).
     * @param ir    The invoker record (for recording interaction). May be null.
     * @return The actual instance name (which might be different from parameter, if there was a name clash)
     */
    public String putObjectOnBench(String newInstanceName, DebuggerObject object, GenTypeClass iType, InvokerRecord ir, Optional<Point2D> animateFromScenePoint)
    {
        if (!object.isNullObject()) {
            ObjectWrapper wrapper = ObjectWrapper.getWrapper(this, getObjectBench(), object, iType, newInstanceName);
            getObjectBench().addObject(wrapper, animateFromScenePoint); // might change name
            newInstanceName = wrapper.getName();

            // load the object into runtime scope
            getPackage().getDebugger().addObject(getPackage().getId(), newInstanceName, object);

            if (ir != null) {
                ir.setBenchName(newInstanceName, wrapper.getTypeName());
            }
            return newInstanceName;
        }
        else
        {
            return null;
        }
    }

    /**
     * Implementation of the "New Class" user function.
     * @param x The X coordinate in the class diagram, or -1 for auto-place
     * @param y The Y coordinate in the class diagram, or -1 for auto-place
     */
    public void doCreateNewClass(double x, double y)
    {
        // Must take reference on Swing thread:
        SourceType sourceType = this.pkg.getDefaultSourceType();
        Platform.runLater(() -> {
            NewClassDialog dlg = new NewClassDialog(getFXWindow(), sourceType);
            Optional<NewClassDialog.NewClassInfo> result = dlg.showAndWait();

            result.ifPresent(info -> 
                SwingUtilities.invokeLater(() ->
                    createNewClass(info.className, info.templateName, info.sourceType, true, x, y)
                )
            );
        });
    }

    /**
     * Prompts the user with a dialog asking for the name of a package to
     * create. Package name can be fully qualified in which case all
     * intermediate packages will also be created as necessary.
     *
     * @param x The X coordinate in the class diagram, or -1 for auto-place
     * @param y The Y coordinate in the class diagram, or -1 for auto-place
     */
    public void doCreateNewPackage(double x, double y)
    {
        Platform.runLater(() -> {
            NewPackageDialog dlg = new NewPackageDialog(stageProperty.getValue());
            Optional<String> pkgName = dlg.showAndWait();

            pkgName.ifPresent(name -> SwingUtilities.invokeLater(() -> createNewPackage(name, true, x, y)));
        });
    }
    
    /**
     * Create a package. Package name can be fully qualified in which case all
     * intermediate packages will also be created as necessary.
     * 
     * @param name    The name of the package to create
     * @param showErrDialog   If true, and a duplicate name exists, a dialog
     *                    will be displayed informing the user of the error.
     * @param x The X coordinate in the class diagram, or -1 for auto-place
     * @param y The Y coordinate in the class diagram, or -1 for auto-place
     * @return true if successful
     */
    public boolean createNewPackage(String name, boolean showErrDialog, double x, double y)
    {
        String fullName;

        // if the name is fully qualified then we leave it as is but
        // if it is not we assume they want to create a package in the
        // current package
        if (name.indexOf('.') > -1) {
            fullName = name;
        }
        else {
            fullName = getPackage().getQualifiedName(name);
        }

        // check whether name is already used for a class or package
        // in the parent package
        String prefix = JavaNames.getPrefix(fullName);
        String base = JavaNames.getBase(fullName);

        Package basePkg = getProject().getPackage(prefix);
        if (basePkg != null) {
            if (basePkg.getTarget(base) != null) {
                if (showErrDialog)
                    Platform.runLater(() -> DialogManager.showErrorFX(getFXWindow(), "duplicate-name"));
                return false;
            }
        }

        getProject().createPackageDirectory(fullName);

        // check that everything has gone well and instruct all affected
        // packages to reload (to make them notice the new sub packages)
        Package newPackage = getProject().getPackage(fullName);

        if (newPackage == null) {
            Debug.reportError("creation of new package failed unexpectedly");
            // TODO propagate a more informative exception
            return false;
        }

        newPackage = newPackage.getParent();
        while (newPackage != null) {
            newPackage.reload();
            newPackage = newPackage.getParent();
        }

        synchronized (this)
        {
            for (Target t : pkg.getVertices())
            {
                if (t instanceof PackageTarget)
                {
                    PackageTarget pt = (PackageTarget) t;
                    if (pt.getQualifiedName().equals(fullName) && x != -1)
                        Platform.runLater(() -> pt.setPos((int) x, (int) y));
                }
            }
        }
        
        return true;
    }

    /**
     * Remove the selected targets. Ask before deletion. If nothing is selected
     * display an errormessage.
     */
    public void doRemove()
    {
        Package pkgFinal = getPackage();
        String pkgId = pkgFinal.getId();
        Platform.runLater(() -> {
            if (editor.targetHasFocus())
            {
                if (!(doRemoveTargets(pkgFinal) || editor.doRemoveDependency())) {
                    DialogManager.showErrorFX(getFXWindow(), "no-class-selected");
                }
            }
            else if (objbench.objectHasFocus()) { // focus in object bench
                objbench.removeSelectedObject(pkgId);
            }
        });
        // Otherwise ignore the command - focus is probably in text eval area
    }

    @OnThread(Tag.FXPlatform)
    private boolean doRemoveTargets(Package thePkg)
    {
        List<Target> targets = thePkg.getSelectedTargets();
        if (targets.size() <= 0) {
            return false;
        }
        if (askRemoveClass())
        {
            SwingUtilities.invokeLater(() ->
            {
                for (Target target : targets)
                {
                    target.remove();
                }
            });
        }
        return true;
    }

    /**
     * The user function to remove an arrow from the dagram was invoked.
     * 
     * public void doRemoveArrow() { pkg.setState(Package.S_DELARROW);
     * setStatus(Config.getString("pkgmgr.chooseArrow")); }
     */

    /**
     * The user function to test all classes in a package
     */
    public void doTest()
    {
        Platform.runLater(() -> runButton.setDisable(true));

        List<ClassTarget> l = getPackage().getTestTargets();

        // Find the number of tests
        int numTests = 0;
        ListIterator<ClassTarget> i = l.listIterator();
        while (i.hasNext()) {
            ClassTarget ct = i.next();
            if (ct.isCompiled() && ! ct.isAbstract()) {
                UnitTestClassRole utcr = (UnitTestClassRole) ct.getRole();
                numTests += utcr.getTestCount(ct);
            }
            else {
                i.remove();
            }
        }
        
        Iterator<ClassTarget> it = l.iterator();
        int numTestsFinal = numTests;
        Project projFinal = getProject();
        Platform.runLater(() -> TestDisplayFrame.getTestDisplay().startMultipleTests(projFinal, numTestsFinal));

        TestRunnerThread trt = new TestRunnerThread(this, it);
        trt.start();
    }
    
    /**
     * Called by the test runner thread when the test run has finished.
     * Re-enables the "run all tests" button.
     */
    public void endTestRun()
    {
        Platform.runLater(() -> {
            TestDisplayFrame.getTestDisplay().endMultipleTests();
            runButton.setDisable(false);
        });
    }

    /**
     * The 'end test recording' button was clicked - end the recording.
     */
    public void doEndTest()
    {
        if (testTarget != null) {
            testRecordingEnded();
            
            DataCollector.endTestMethod(getPackage(), testIdentifier);
            
            if (testTarget.getRole() instanceof UnitTestClassRole) {
                UnitTestClassRole utcr = (UnitTestClassRole) testTarget.getRole();
                
                utcr.doEndMakeTestCase(this, testTarget, testTargetMethod);
            }
            
            // try to compile the test class we have just changed. Do this before
            // installing the new class loader, because that causes a short machine
            // execution during which compilation fails with an error message
            getPackage().compileQuiet(testTarget, CompileReason.MODIFIED, CompileType.INDIRECT_USER_COMPILE);

            // remove objects from object bench
            getProject().removeClassLoader();
            getProject().newRemoteClassLoaderLeavingBreakpoints();
            
            testTarget = null;
        }
    }

    /**
     * The 'cancel test recording' button was clicked - cancel the recording.
     */
    public void doCancelTest()
    {
        testRecordingEnded();
        
        DataCollector.cancelTestMethod(getPackage(), testIdentifier);

        // remove objects from object bench (may have been put there
        // when testing was started)
        getProject().removeClassLoader();
        getProject().newRemoteClassLoaderLeavingBreakpoints();

        testTarget = null;
    }

    /**
     * Recording of a test case started - set the interface appropriately.
     * @param message The user message to display
     */
    public void testRecordingStarted(String message)
    {
        Platform.runLater(() -> {
            testStatusMessage.setText(message);
            recordingLabel.setDisable(false);
        });
        endTestRecordAction.setEnabled(true);
        cancelTestRecordAction.setEnabled(true);

        getProject().setTestMode(true);
    }

    /**
     * Recording of a test case ended - set the interface appropriately.
     */
    private void testRecordingEnded()
    {
        Platform.runLater(() -> {
            testStatusMessage.setText("");
            recordingLabel.setDisable(true);
        });
        endTestRecordAction.setEnabled(false);
        cancelTestRecordAction.setEnabled(false);

        Project proj = getProject();
        if (proj != null) {
            proj.setTestMode(false);
        }
    }

    /**
     * Store information about the currently recorded test method.
     * @param testName The name of the test
     * @param testClass The class the test belongs to
     */
    public void setTestInfo(String testName, ClassTarget testClass)
    {
        this.testTargetMethod = testName;
        this.testTarget = testClass;
        this.testIdentifier = nextTestIdentifier.incrementAndGet(); // Allocate next test identifier
        DataCollector.startTestMethod(getPackage(), testIdentifier, testClass.getSourceFile(), testName);
    }

    /**
     * Ask the user to confirm removal of package.
     * 
     * @return zero if the user confirms removal.
     */
    @OnThread(Tag.FXPlatform)
    public boolean askRemoveClass()
    {
        int response = DialogManager.askQuestionFX(getFXWindow(), "really-remove-class");
        return response == 0;
    }

    /**
     * Compile the currently selected class targets.
     */
    public void compileSelected()
    {
        Package thePkg = getPackage();
        List<Target> targets = thePkg.getSelectedTargets();
        if (targets.size() > 0) {
            for (Target target : targets) {
                if (target instanceof ClassTarget) {
                    ClassTarget t = (ClassTarget) target;
                    if (t.hasSourceCode())
                        thePkg.compile(t, CompileReason.USER, CompileType.EXPLICIT_USER_COMPILE);
                }
            }
        }
        else {
            Platform.runLater(() -> DialogManager.showErrorFX(getFXWindow(), "no-class-selected-compile"));
        }
    }

    /**
     * User function "Use Library Class...". Pop up the dialog that allows users
     * to invoke library classes.
     */
    public void callLibraryClass()
    {
        Package pkgRef = getPackage();
        BPClassLoader classLoader = getProject().getClassLoader();
        Platform.runLater(() -> {
            if (libraryCallDialog == null)
            {
                libraryCallDialog = new LibraryCallDialog(getFXWindow(), pkgRef, classLoader);
            }
            libraryCallDialog.setResult(null);
            Optional<CallableView> result = libraryCallDialog.showAndWait();
            result.ifPresent(viewToCall -> SwingUtilities.invokeLater(() -> {
                pkgRef.getEditor().raiseMethodCallEvent(pkgRef, viewToCall);
            }));
        });
    }

    /**
     * User function "Generate Documentation...".
     */
    public void generateProjectDocumentation()
    {
        String message = getPackage().generateDocumentation();
        if (message.length() != 0) {
            Platform.runLater(() -> DialogManager.showTextFX(getFXWindow(), message));
        }
    }

    /**
     * Check the debugger state is suitable for execution: that is, it is not already
     * executing anything or stuck at a breakpoint.
     * 
     * <P>Returns true if the debugger is currently idle, or false if it is already
     * executing, in which case an error dialog is also displayed and the debugger
     * controls window is made visible.
     * @return True if the debugger is currently idle
     */
    public boolean checkDebuggerState()
    {
        Debugger debugger = getProject().getDebugger();
        if (debugger.getStatus() == Debugger.SUSPENDED) {
            setVisible(true);
            Platform.runLater(() -> DialogManager.showErrorFX(getFXWindow(), "stuck-at-breakpoint"));
            return false;
        }
        else if (debugger.getStatus() == Debugger.RUNNING) {
            setVisible(true);
            Platform.runLater(() -> DialogManager.showErrorFX(getFXWindow(), "already-executing"));
            return false;
        }
        
        return true;
    }

    /**
     * Restart the debugger VM associated with this project.
     */
    public void restartDebugger()
    {
        if (!isEmptyFrame())
        {
            getProject().restartVM();
            DataCollector.restartVM(getProject());
        }
    }

    /**
     * Notify the frame that the "shared" status of the project has changed,
     * i.e. the project has become shared or unshared.
     * 
     * @param shared  The new shared status of the project
     */
    public void updateSharedStatus(boolean shared)
    {
        Platform.runLater(() -> updateWindow());
    }

    /**
     * Show or hide the text evaluation component.
     * @param show True to show; false to hide
     */
    @OnThread(Tag.FXPlatform)
    private void showHideTextEval(boolean show)
    {
        if (show)
        {
            if (codePad == null)
            {
                codePad = new CodePad(this, bottomOverlay);
                addCtrlTabShortcut(codePad);
                CodePad cpFinal = codePad;
                itemsToDisable.add(cpFinal);
                bottomPane.getItems().add(codePad);
                codePad.focusInputField();
            }
            codePad.setDisable(false);
        }
        else
        {
            CodePad cpFinal = codePad;
            itemsToDisable.remove(cpFinal);
            bottomPane.getItems().remove(codePad);
            codePad = null;
        }
    }

    /**
     * Clear the text evaluation component (if it exists).
     */
    @OnThread(Tag.FXPlatform)
    public void clearTextEval()
    {
        if (codePad != null) {
            codePad.clear();
        }
    }

    // ---- BlueJEventListener interface ----
    
    /**
     * A BlueJEvent was raised. Check whether it is one that we're interested
     * in.
     */
    @Override
    public void blueJEvent(int eventId, Object arg)
    {
        switch(eventId) {
            case BlueJEvent.CREATE_VM :
                setStatus(Config.getString("pkgmgr.creatingVM"));
                break;
            case BlueJEvent.CREATE_VM_DONE :
                setStatus(Config.getString("pkgmgr.creatingVMDone"));
                break;
            case BlueJEvent.GENERATING_DOCU :
                setStatus(Config.getString("pkgmgr.generatingDocu"));
                break;
            case BlueJEvent.DOCU_GENERATED :
                setStatus(Config.getString("pkgmgr.docuGenerated"));
                break;
            case BlueJEvent.DOCU_ABORTED :
                setStatus(Config.getString("pkgmgr.docuAborted"));
                break;
            case BlueJEvent.CREATE_VM_FAILED :
                Platform.runLater(() -> DialogManager.showErrorFX(getFXWindow(), "error-create-vm"));
                break;
        }
    }

    // ---- end of BlueJEventListener interface ----

    /**
     * The debugger state has changed. Indicate the state in our interface and
     * change the system state accordingly (e.g. enable/disable terminal).
     * 
     * NOTE: The current implementation assumes that user VMs DO NOT run
     * concurrently!
     * @param state The state to set
     */
    public void setDebuggerState(int state)
    {
        switch(state) {
            case Debugger.NOTREADY :
            case Debugger.LAUNCH_FAILED:
                break;

            case Debugger.IDLE :
                if(machineIcon != null) {
                    machineIcon.setIdle();
                }
                getProject().getTerminal().activate(false);
                break;

            case Debugger.RUNNING :
                if(machineIcon != null) {
                    machineIcon.setRunning();
                }
                getProject().getTerminal().activate(true);
                break;

            case Debugger.SUSPENDED :
                if(machineIcon != null) {
                    machineIcon.setStopped();
                }
                break;
        }
    }

    // --- general support functions for user function implementations ---

    /**
     * String representation for debugging only.
     */
    @Override
    public String toString()
    {
        String str = "PkgMgrFrame(): ";

        if (isEmptyFrame())
            str += "empty";
        else
            str += getPackage().toString() + " " + getProject().toString();

        return str;
    }

    /**
     * showWebPage - show a page in a web browser and display a message in the
     * status bar.
     * @param url Address of the page to show
     */
    public void showWebPage(String url)
    {
        if (Utility.openWebBrowser(url))
            setStatus(Config.getString("pkgmgr.webBrowserMsg"));
        else
            setStatus(Config.getString("pkgmgr.webBrowserError"));
    }

    // --- the following methods set up the GUI frame ---

    private void makeFrame()
    {   
        setupMenus();

        UpdateDialogAction updateAction = teamActions.getUpdateAction(this);
        CommitCommentAction commitCommentAction = teamActions.getCommitCommentAction(this);
        StatusAction statusAction = teamActions.getStatusAction(this);
        ImportAction shareAction = teamActions.getImportAction(this);

        endTestRecordAction.setEnabled(false);
        cancelTestRecordAction.setEnabled(false);

        JLabel dummyContent = new JLabel("");
        Platform.runLater(() -> {
            // create the left hand side toolbar
            toolPanel = new VBox();
            JavaFXUtil.addStyleClass(toolPanel, "pmf-tools");
            
            VBox topButtons = new VBox();
            JavaFXUtil.addStyleClass(topButtons, "pmf-tools-top");
            ButtonBase button = createButton(newClassAction, false, false);
            topButtons.getChildren().add(button);
            imgExtendsButton = createButton(newInheritsAction, false, true);
            imgExtendsButton.setText(null);
            imgExtendsButton.setGraphic(new ImageView(Config.getImageAsFXImage("image.build.extends")));
            topButtons.getChildren().add(imgExtendsButton);
            button = createButton(compileAction, false, false);
            topButtons.getChildren().add(button);
            toolPanel.getChildren().add(topButtons);

            dummySwingNode = new SwingNode();
            dummySwingNode.setContent(dummyContent);
            dummySwingNode.setFocusTraversable(false);
            toolPanel.getChildren().add(dummySwingNode);
            
            Pane space = new Pane();
            VBox.setVgrow(space, Priority.ALWAYS);
            toolPanel.getChildren().add(space);
            
            testPanel = new TitledPane();
            testPanel.setFocusTraversable(false);
            JavaFXUtil.addStyleClass(testPanel, "pmf-tools-test");
            testPanel.setText(Config.getString("pkgmgr.test.title"));
            VBox testPanelItems = new VBox();
            JavaFXUtil.addStyleClass(testPanelItems, "pmf-tools-test-items");
            testPanel.setContent(testPanelItems);
            runButton = createButton(runTestsAction, false, false);
            runButton.setText(Config.getString("pkgmgr.test.run"));
            testPanelItems.getChildren().add(runButton);

            Shape recordingIcon = new Ellipse(7.0, 7.0);
            recordingIcon.setFill(Color.RED);
            recordingLabel = new Label(Config.getString("pkgmgr.test.record"), recordingIcon);
            recordingLabel.disabledProperty().addListener(new ChangeListener<Boolean>()
            {
                private Animation pulseAnimation;

                @Override
                @OnThread(Tag.FX)
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
                {
                    if (pulseAnimation != null)
                    {
                        pulseAnimation.stop();
                        pulseAnimation = null;
                    }

                    if (newValue == false) // enabled
                    {
                        pulseAnimation = new FillTransition(Duration.millis(2000), recordingIcon, new Color(1.0, 0.2, 0.2, 1.0), Color.DARKRED);
                        pulseAnimation.setAutoReverse(true);
                        pulseAnimation.setCycleCount(Animation.INDEFINITE);
                        pulseAnimation.playFromStart();
                    }
                    else
                    {
                        recordingIcon.setFill(Color.DARKGRAY);
                    }
                }
            });
            recordingLabel.setDisable(true);
            testPanelItems.getChildren().add(recordingLabel);

            ButtonBase endTestButton = createButton(endTestRecordAction, false, false);
            //make the button use a different label than the one from
            // action:
            endTestButton.setText(Config.getString("pkgmgr.test.end"));

            testPanelItems.getChildren().add(JavaFXUtil.withStyleClass(new VBox(endTestButton), "pmf-tools-test-recording-button"));

            ButtonBase cancelTestButton = createButton(cancelTestRecordAction, false, false);
            //make the button use a different label than the one from
            // action
            cancelTestButton.setText(Config.getString("cancel"));

            testPanelItems.getChildren().add(JavaFXUtil.withStyleClass(new VBox(cancelTestButton), "pmf-tools-test-recording-button"));

            //testItems.add(testPanel);

            teamPanel = new TitledPane();
            teamPanel.setFocusTraversable(false);
            teamPanel.setText(Config.getString("pkgmgr.team.title"));
            JavaFXUtil.addStyleClass(teamPanel, "pmf-tools-team");
            VBox teamPanelItemsOnceShared = new VBox();
            JavaFXUtil.addStyleClass(teamPanelItemsOnceShared, "pmf-tools-team-items");
            teamPanelItemsOnceShared.setPickOnBounds(false);
            VBox teamPanelItemsUnshared = new VBox();
            JavaFXUtil.addStyleClass(teamPanelItemsUnshared, "pmf-tools-team-items");
            teamPanelItemsUnshared.setPickOnBounds(false);
            teamPanel.setContent(new StackPane(teamPanelItemsUnshared, teamPanelItemsOnceShared));
            
            updateButton = createButton(updateAction, false, false);
            updateButton.visibleProperty().bind(updateButton.disableProperty().not());
            teamPanelItemsOnceShared.getChildren().add(updateButton);
            
            commitButton = createButton(commitCommentAction, false, false);
            commitButton.visibleProperty().bind(commitButton.disableProperty().not());
            teamPanelItemsOnceShared.getChildren().add(commitButton);
            
            teamStatusButton = createButton(statusAction, false, false);
            teamStatusButton.visibleProperty().bind(teamStatusButton.disableProperty().not());
            teamPanelItemsOnceShared.getChildren().add(teamStatusButton);

            teamShareButton = createButton(shareAction, false, false);
            teamShareButton.visibleProperty().bind(teamShareButton.disableProperty().not());
            teamPanelItemsUnshared.getChildren().add(teamShareButton);
            teamShareButton.setText(Config.getString("team.import.short"));

            // Don't reserve space for all three once-shared buttons if we are not yet shared:
            teamShowSharedButtons = teamShareButton.disableProperty().and(teamStatusButton.disableProperty().not());
            teamPanelItemsOnceShared.managedProperty().bind(teamShowSharedButtons);


            VBox foldout = new VBox(teamPanel, testPanel);
            teamPanel.setCollapsible(false);
            teamPanel.setExpanded(true);
            testPanel.setCollapsible(false);
            testPanel.setExpanded(true);
            teamAndTestFoldout = new UntitledCollapsiblePane(foldout, ArrowLocation.TOP, !PrefMgr.getFlag(PrefMgr.SHOW_TEST_TOOLS) && !PrefMgr.getFlag(PrefMgr.SHOW_TEAM_TOOLS)) {
                @Override
                @OnThread(Tag.FX)
                protected double computeMinHeight(double width)
                {
                    return TriangleArrow.TRIANGLE_DEPTH + 2 * arrowPadding;
                }
            };
            JavaFXUtil.addStyleClass(foldout, "team-test-foldout-content");
            teamAndTestFoldout.addArrowWrapperStyleClass("pmf-triangle-foldout-wrapper");
            // When the user toggles the pane, we record that as the new preference.
            // But we deliberately don't toggle the pane when the preference changes;
            // each PkgMgrFrame is independent while showing, but we store the last user-triggered
            // state as the future default.
            JavaFXUtil.addChangeListener(teamAndTestFoldout.expandedProperty(), expanded -> {
                PrefMgr.setFlag(PrefMgr.SHOW_TEAM_TOOLS, expanded);
                PrefMgr.setFlag(PrefMgr.SHOW_TEST_TOOLS, expanded);
            });
            toolPanel.getChildren().add(teamAndTestFoldout);
            machineIcon = new MachineIcon(this, restartVMAction);
            itemsToDisable.add(machineIcon);
        });

        // show the text evaluation pane if needed
        if (PrefMgr.getFlag(PrefMgr.SHOW_TEXT_EVAL)) {
            Platform.runLater(() -> showingTextEval.set(true));
        }

        Platform.runLater(() -> JavaFXUtil.onceNotNull(stageProperty, stage -> {
            stage.setOnCloseRequest(e -> PkgMgrFrame.this.doClose(false, true));
        }));
    }

    /**
     * Create a button for the interface.
     * 
     * @param action
     *            the Action abstraction dictating text, icon, tooltip, action.
     * @param toggle
     *            true if this is a toggle button, false otherwise
     * @return the new button
     */
    @OnThread(Tag.FXPlatform)
    public static ButtonBase createButton(Action action, boolean toggle, boolean noText)
    {
        ButtonBase button;
        if (toggle) {
            button = new ToggleButton();
        }
        else {
            button = new Button();
        }
        setButtonAction(action, button, noText);
        button.setFocusTraversable(false); // buttons shouldn't get focus
        //if (notext)
        //    button.setText(null);

        return button;
    }

    @OnThread(Tag.FXPlatform)
    private static void setButtonAction(Action action, ButtonBase button, boolean noText)
    {
        SwingUtilities.invokeLater(() ->
        {
            String name = (String) action.getValue(Action.NAME);
            boolean startEnabled = action.isEnabled();
            action.addPropertyChangeListener(c -> {
                if (c.getPropertyName().equals("enabled"))
                {
                    boolean enabled = action.isEnabled();
                    Platform.runLater(() -> button.setDisable(!enabled));
                }
            });
            Platform.runLater(() ->
            {
                if (!noText && (button.getText() == null || button.getText().isEmpty()))
                    button.setText(name);
                button.setDisable(!startEnabled);
            });
        });
        button.setOnAction(e -> {
            SwingUtilities.invokeLater(() -> {
                action.actionPerformed(new ActionEvent(button, ActionEvent.ACTION_PERFORMED, null));
            });
        });
    }

    /**
     * setupMenus - Create the menu bar
     */
    private void setupMenus()
    {
        List<JavaFXUtil.SwingOrFXMenu> menubar = new ArrayList<>();
        
        {
            JMenu menu = new JMenu(Config.getString("menu.package"));
            int mnemonic = Config.getMnemonicKey("menu.package");
            menu.setMnemonic(mnemonic);
            menubar.add(new JavaFXUtil.SwingMenu(menu));
            createMenuItem(new NewProjectAction(this), menu);
            createMenuItem(new OpenProjectAction(this), menu);
            recentProjectsMenu = new JMenu(Config.getString("menu.package.openRecent"));
            menu.add(recentProjectsMenu);
            createMenuItem(new OpenNonBlueJAction(this), menu);
            createMenuItem(new OpenArchiveAction(this), menu);
            createMenuItem(closeProjectAction, menu);
            createMenuItem(saveProjectAction, menu);
            createMenuItem(saveProjectAsAction, menu);
            menu.addSeparator();

            createMenuItem(importProjectAction, menu);
            createMenuItem(exportProjectAction, menu);
            menu.addSeparator();

            createMenuItem(pageSetupAction, menu);
            createMenuItem(printAction, menu);

            if (!Config.usingMacScreenMenubar()) { // no "Quit" here for Mac
                menu.addSeparator();
                createMenuItem(new QuitAction(this), menu);
            }
        }

        {
            JMenu menu = new JMenu(Config.getString("menu.edit"));
            menu.setMnemonic(Config.getMnemonicKey("menu.edit"));
            menubar.add(new JavaFXUtil.SwingMenu(menu));
            createMenuItem(newClassAction, menu);
            createMenuItem(newPackageAction, menu);
            createMenuItem(addClassAction, menu);
            createMenuItem(removeAction, menu);
            menu.addSeparator();

            createMenuItem(newInheritsAction, menu);
        }

        ExtensionsManager extMgr = ExtensionsManager.getInstance();

        //menu = new JMenu(Config.getString("menu.tools"));
        //menu.setMnemonic(Config.getMnemonicKey("menu.tools"));
        //menubar.add(new JavaFXUtil.SwingMenu(menu));
        {
            JavaFXUtil.FXPlusSwingMenu mixedMenu = new JavaFXUtil.FXPlusSwingMenu(() -> {
                Menu fxMenu = new Menu(Config.getString("menu.tools"));
                // Create the menu manager that looks after extension tools menus
                toolsMenuManager.set(new FXMenuManager(fxMenu, extMgr, null));
                return fxMenu;
            });

            List<JMenuItem> swingItems = new ArrayList<>();
            swingItems.add(new JMenuItem(compileAction));
            swingItems.add(new JMenuItem(compileSelectedAction));
            swingItems.add(new JMenuItem(rebuildAction));
            swingItems.add(new JMenuItem(restartVMAction));
            mixedMenu.addSwing(swingItems);
            mixedMenu.addFX(SeparatorMenuItem::new);

            swingItems.clear();
            swingItems.add(new JMenuItem(useLibraryAction));
            swingItems.add(new JMenuItem(generateDocsAction));

            testingMenu = new JMenu(Config.getString("menu.tools.testing"));
            testingMenu.setMnemonic(Config.getMnemonicKey("menu.tools"));
            {
                createMenuItem(runTestsAction, testingMenu);
                createMenuItem(endTestRecordAction, testingMenu);
                createMenuItem(cancelTestRecordAction, testingMenu);
            }
            swingItems.add(testingMenu);
            
            //team menu setup
            teamMenu = new JMenu(Config.getString("menu.tools.teamwork"));
            teamMenu.setMnemonic(Config.getMnemonicKey("menu.tools"));
            {
                Action checkoutAction = new CheckoutAction(this);
                createMenuItem(checkoutAction , teamMenu);
                shareProjectMenuItem = createMenuItem(teamActions.getImportAction(this), teamMenu);               
                
                teamMenu.addSeparator();
                
                updateMenuItem = createMenuItem(teamActions.getUpdateAction(this), teamMenu);
                updateMenuItem.setText(Config.getString("team.menu.update"));
                commitMenuItem = createMenuItem(teamActions.getCommitCommentAction(this), teamMenu);
                commitMenuItem.setText(Config.getString("team.menu.commit"));
                statusMenuItem = createMenuItem(teamActions.getStatusAction(this), teamMenu);
                showLogMenuItem = createMenuItem(teamActions.getShowLogAction(this), teamMenu);
                
                teamMenu.addSeparator();
                
                teamSettingsMenuItem = createMenuItem(teamActions.getTeamSettingsAction(this), teamMenu);
            }
            swingItems.add(teamMenu);

            mixedMenu.addSwing(swingItems);
            swingItems.clear();

            if (!Config.usingMacScreenMenubar()) { // no "Preferences" here for
                                                   // Mac
                mixedMenu.addFX(SeparatorMenuItem::new);
                swingItems.add(new JMenuItem(new PreferencesAction(this)));
                mixedMenu.addSwing(swingItems);
            }

            // If this is the first frame create the extension tools menu now.
            // (Otherwise, it will be created during project open.)
            // (Otherwise, it will be created during project open.)
            if (frameCount() <= 1)
            {
                mixedMenu.runAtEnd(() -> Platform.runLater(() -> {
                    FXMenuManager tm = toolsMenuManager.get();
                    SwingUtilities.invokeLater(() -> {tm.addExtensionMenu(null);});
                }));
            }

            menubar.add(mixedMenu);
        }

        //menu = new JMenu(Config.getString("menu.view"));
        //menu.setMnemonic(Config.getMnemonicKey("menu.view"));
        {
            JavaFXUtil.FXPlusSwingMenu mixedMenu = new JavaFXUtil.FXPlusSwingMenu(() -> {
                Menu fxMenu = new Menu(Config.getString("menu.view"));
                // Create the menu manager that looks after extension view menus
                viewMenuManager.set(new FXMenuManager(fxMenu, extMgr, null));
                return fxMenu;
            });
            mixedMenu.addFX(() -> {
                CheckMenuItem item = JavaFXUtil.makeCheckMenuItem(Config.getString("menu.view.showUses"), showUsesProperty, null);
                menuItemsToDisable.add(item);
                return item;
            });
            mixedMenu.addFX(() -> {
                CheckMenuItem item = JavaFXUtil.makeCheckMenuItem(Config.getString("menu.view.showInherits"), showInheritsProperty, null);
                menuItemsToDisable.add(item);
                return item;
            });
            mixedMenu.addFX(SeparatorMenuItem::new);
            List<JMenuItem> swingItems = new ArrayList<>();
            createCheckboxMenuItem(showDebuggerAction, swingItems, false);
            createCheckboxMenuItem(showTerminalAction, swingItems, false);
            mixedMenu.addSwing(swingItems);
            mixedMenu.addFX(() -> JavaFXUtil.makeCheckMenuItem(Config.getString("menu.view.showTextEval"), showingTextEval, Config.hasAcceleratorKey("menu.view.showTextEval") ? Config.getAcceleratorKeyFX("menu.view.showTextEval") : null));
            mixedMenu.addFX(() -> JavaFXUtil.makeCheckMenuItem(Config.getString("menu.view.showTeamTest"), teamAndTestFoldout.expandedProperty(), Config.hasAcceleratorKey("menu.view.showTeamTest") ? Config.getAcceleratorKeyFX("menu.view.showTeamTest") : null));
            mixedMenu.addFX(SeparatorMenuItem::new);

            swingItems = new ArrayList<>();
            createCheckboxMenuItem(new ShowTestResultsAction(this), swingItems, false);
            mixedMenu.addSwing(swingItems);

            // (Otherwise, it will be created during project open.)
            if (frameCount() <= 1)
            {
                mixedMenu.runAtEnd(() -> Platform.runLater(() -> {
                    FXMenuManager vm = viewMenuManager.get();
                    SwingUtilities.invokeLater(() -> {vm.addExtensionMenu(null);});
                }));
            }

            menubar.add(mixedMenu);
        }

        {
            JMenu menu = new JMenu(Config.getString("menu.help"));
            menu.setMnemonic(Config.getMnemonicKey("menu.help"));
            menubar.add(new JavaFXUtil.SwingMenu(menu));
            if (!Config.usingMacScreenMenubar()) { // no "About" here for Mac
                createMenuItem(new HelpAboutAction(this), menu);
            }
            createMenuItem(new CheckVersionAction(this), menu);
            createMenuItem(new CheckExtensionsAction(this), menu);
            createMenuItem(new ShowCopyrightAction(this), menu);
            menu.addSeparator();

            createMenuItem(new WebsiteAction(this), menu);
            createMenuItem(new TutorialAction(this), menu);
            createMenuItem(new StandardAPIHelpAction(this), menu);
            addUserHelpItems(menu);
        }

        updateRecentProjects();

        FXPlatformSupplier<MenuBar> fxMenuBarSupplier = JavaFXUtil.swingMenuBarToFX(menubar, PkgMgrFrame.this);
        Platform.runLater(() -> JavaFXUtil.onceNotNull(paneProperty, pane -> {
            JavaFXUtil.runNowOrLater(() ->
            {
                MenuBar fxMenuBar = fxMenuBarSupplier.get();
                fxMenuBar.setUseSystemMenuBar(true);
                pane.setTop(fxMenuBar);
            });
        }));
    }

    /**
     * Add a new menu item to a menu.
     */
    private JMenuItem createMenuItem(Action action, JMenu menu)
    {
        JMenuItem item = menu.add(action);
        item.setIcon(null);
        return item;
    }

    /**
     * Add a new menu item to a menu.
     */
    private JCheckBoxMenuItem createCheckboxMenuItem(PkgMgrToggleAction action, List<JMenuItem> menu, boolean selected)
    {
        ButtonModel bmodel = action.getToggleModel();

        JCheckBoxMenuItem item = new JCheckBoxMenuItem(action);
        if (bmodel != null)
            item.setModel(bmodel);
        else
            item.setState(selected);
        menu.add(item);
        return item;
    }

    /**
     * Return the menu tool bar.
     * 
     * public JMenu getToolsMenu() { return toolsMenu; }
     * 
     * /** Add or remove a separator in the tools menu for extensions as needed.
     * To be deleted, Damiano public void toolsExtensionsCheckSeparator() {
     * if(extMgr.haveMenuItems( )) { // do we need one? if
     * (toolsExtensionsSeparatorIndex > 0) // have one already return;
     * 
     * toolsExtensionsSeparatorIndex = toolsMenu.getItemCount();
     * toolsMenu.addSeparator(); } else { // don't need one if
     * (toolsExtensionsSeparatorIndex <= 0) // don't have one return;
     * 
     * toolsMenu.remove(toolsExtensionsSeparatorIndex);
     * toolsExtensionsSeparatorIndex = 0; } }
     */

    /**
     * Called on (almost) every menu invocation to clean up.
     */
    public void menuCall()
    {
        if (!isEmptyFrame())
        {
            synchronized (this)
            {
                PackageEditor pkgEd = pkg.getEditor();
                Platform.runLater(() ->
                {
                    pkgEd.clearState();
                });
            }
        }
        clearStatus();
    }

    /**
     * Define which actions are to be disabled when no project is open
     */
    private void setupActionDisableSet()
    {
        actionsToDisable.add(closeProjectAction);
        actionsToDisable.add(saveProjectAction);
        actionsToDisable.add(saveProjectAsAction);
        actionsToDisable.add(importProjectAction);
        actionsToDisable.add(exportProjectAction);
        actionsToDisable.add(pageSetupAction);
        actionsToDisable.add(printAction);
        actionsToDisable.add(newClassAction);
        actionsToDisable.add(newPackageAction);
        actionsToDisable.add(addClassAction);
        actionsToDisable.add(removeAction);
        actionsToDisable.add(newInheritsAction);
        actionsToDisable.add(compileAction);
        actionsToDisable.add(compileSelectedAction);
        actionsToDisable.add(rebuildAction);
        actionsToDisable.add(restartVMAction);
        actionsToDisable.add(useLibraryAction);
        actionsToDisable.add(generateDocsAction);
        actionsToDisable.add(showDebuggerAction);
        actionsToDisable.add(showTerminalAction);
        actionsToDisable.add(runTestsAction);
    }

    /**
     * Add user defined help menus. Users can add help menus via the
     * bluej.help.items property. See comment in bluej.defs.
     */
    private void addUserHelpItems(JMenu menu)
    {
        String helpItems = Config.getPropString("bluej.help.items", "");

        if (helpItems != null && helpItems.length() > 0) {
            menu.addSeparator();
            URLDisplayer urlDisplayer = new URLDisplayer();

            StringTokenizer t = new StringTokenizer(helpItems);

            while (t.hasMoreTokens()) {
                String itemID = t.nextToken();
                String itemName = Config.getPropString("bluej.help." + itemID + ".label");
                String itemURL = Config.getPropString("bluej.help." + itemID + ".url");
                JMenuItem item = new JMenuItem(itemName);
                item.setActionCommand(itemURL);
                item.addActionListener(urlDisplayer);
                menu.add(item);
            }
        }
    }

    /**
     * Update the 'Open Recent' menu
     */
    private void updateRecentProjects()
    {
        ProjectOpener opener = new ProjectOpener();
        recentProjectsMenu.removeAll();

        List<String> projects = PrefMgr.getRecentProjects();
        for (Iterator<String> it = projects.iterator(); it.hasNext();) {
            JMenuItem item = recentProjectsMenu.add(it.next());
            item.addActionListener(opener);
        }
    }

    /**
     * Enable/disable functionality. Enable or disable all the interface
     * elements that should change when a project is or is not open.
     * @param enable True to enable; false to disable
     */
    protected void enableFunctions(boolean enable)
    {
        if (! enable) {
            teamActions.setAllDisabled(this);
        }

        Platform.runLater(() -> {
            for (Node component : itemsToDisable)
                component.setDisable(!enable);
            for (MenuItem component : menuItemsToDisable)
                component.setDisable(!enable);
        });
    
        actionsToDisable.stream().forEach((action) -> {
            action.setEnabled(enable);
        });
    }
    
    /**
     * Adds shortcuts for Ctrl-TAB and Ctrl-Shift-TAB to the given pane, which move to the
     * next/previous pane of the main three (package editor, object bench, code pad) that are visible
     */
    @OnThread(Tag.FX)
    private void addCtrlTabShortcut(final PkgMgrPane srcPane)
    {
        srcPane.asNode().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.TAB && e.isControlDown())
            {
                if (!e.isShiftDown())
                {
                    // Try to focus next pane.
                    if (srcPane == editor)
                    {
                        if (!tryFocusObjBench())
                            tryFocusCodePad();
                        // If codepad can't be focused, do nothing

                    }
                    else if (srcPane == objbench)
                    {
                        if (!tryFocusCodePad())
                            tryFocusClassDiagram();
                    }
                    else
                    {
                        if (!tryFocusClassDiagram())
                            tryFocusObjBench();
                    }
                }
                else
                {
                    // Try to focus prev pane
                    if (srcPane == editor)
                    {
                        if (!tryFocusCodePad())
                            tryFocusObjBench();
                    }
                    else if (srcPane == objbench)
                    {
                        if (!tryFocusClassDiagram())
                            tryFocusCodePad();
                    }
                    else
                    {
                        if (!tryFocusObjBench())
                            tryFocusClassDiagram();
                    }
                }
                e.consume();
            }
        });
    }

    @OnThread(Tag.FXPlatform)
    private boolean tryFocusClassDiagram()
    {
        if (editor != null)
        {
            return editor.focusSelectedOrArbitrary();
        }
        return false;
    }

    @OnThread(Tag.FXPlatform)
    private boolean tryFocusCodePad()
    {
        if (codePad != null)
        {
            if (!codePad.isDisabled())
            {
                codePad.focusInputField();
                return true;
            }
        }
        return false;
    }

    @OnThread(Tag.FXPlatform)
    private boolean tryFocusObjBench()
    {
        // Focus first object, if bench is non-empty:
        if (objbench.getObjectCount() > 0)
        {
            objbench.getObjects().get(0).requestFocus();
            return true;
        }
        
        return false;
    }

    @OnThread(Tag.FX)
    public Stage getFXWindow()
    {
        return stageProperty.getValue();
    }

    void bringToFront()
    {
        Platform.runLater(() -> Utility.bringToFrontFX(getFXWindow()));
    }

    // Copied from FXTabbedEditor, only needed until we swap to FX for the whole window:
    @OnThread(Tag.FXPlatform)
    private void scheduleWindowWiggle(Stage stage)
    {
        if (cancelWiggle != null)
        {
            cancelWiggle.run();
        }
        cancelWiggle = JavaFXUtil.runAfter(Duration.seconds(0.5),() -> {
            if (!stage.isMaximized() && !stage.isIconified())
            {
                // Left and right one pixel:
                final double x = stage.getX();
                stage.setX(x + 1);
                // Must wait before reversing, so that SwingNode sees change:
                JavaFXUtil.runAfterCurrent(() -> stage.setX(x));
            }
        });
    }

    @OnThread(Tag.Swing)
    public synchronized void doNewInherits()
    {
        if (pkg != null && pkg.getEditor() != null)
        {
            PackageEditor pkgEg = pkg.getEditor();
            Platform.runLater(() -> pkgEg.doNewInherits());
        }
    }

    @OnThread(Tag.Swing)
    public void graphChanged()
    {
        int numClassTargets;
        int numClassTargetsWithSource;
        int numPackagesNested;
        synchronized (this)
        {
            if (pkg != null)
            {
                ArrayList<ClassTarget> classTargets = pkg.getClassTargets();
                numClassTargets = classTargets.size();
                numClassTargetsWithSource = (int) classTargets.stream().filter(ClassTarget::hasSourceCode).count();
                numPackagesNested = pkg.getChildren(true).size();
            }
            else
                return;
        }
        // Can only compile if you have a class target with source:
        compileAction.setEnabled(numClassTargetsWithSource > 0);
        // Isn't a perfect detection of whether inherits is possible, but close enough
        // You must have two targets, one of which must have source code:
        newInheritsAction.setEnabled(numClassTargets >= 2 && numClassTargetsWithSource >= 1);

        Platform.runLater(() -> {
            getPackage().getEditor().noClassesExistedMessage.setVisible(numClassTargets + numPackagesNested == 0);
        });
    }

    class URLDisplayer
        implements ActionListener
    {
        public URLDisplayer()
        {}

        @Override
        public void actionPerformed(ActionEvent evt)
        {
            String url = evt.getActionCommand();
            showWebPage(url);
        }
    }

    class ProjectOpener
        implements ActionListener
    {
        public ProjectOpener()
        {}

        @Override
        public void actionPerformed(ActionEvent evt)
        {
            String project = evt.getActionCommand();
            if (!openProject(project))
                setStatus(Config.getString("pkgmgr.error.open"));
        }
    }
    
    // Used as a way to tag the three main panes in the PkgMgrFrame window
    public static interface PkgMgrPane
    {
        @OnThread(Tag.FX)
        public default Node asNode() { return (Node)this;}
    }
}
