/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2017,2018  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.guifx;

import bluej.BlueJEvent;
import bluej.BlueJEventListener;
import bluej.Config;
import bluej.Main;
import bluej.collect.DataCollector;
import bluej.collect.GreenfootInterfaceEvent;
import bluej.compiler.CompileInputFile;
import bluej.compiler.CompileReason;
import bluej.compiler.CompileType;
import bluej.compiler.Diagnostic;
import bluej.compiler.FXCompileObserver;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerResult;
import bluej.debugger.gentype.JavaType;
import bluej.debugger.gentype.Reflective;
import bluej.debugmgr.ExecutionEvent;
import bluej.debugmgr.Invoker;
import bluej.debugmgr.ResultWatcher;
import bluej.debugmgr.objectbench.InvokeListener;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.debugmgr.objectbench.ObjectResultWatcher;
import bluej.editor.Editor;
import bluej.extensions.SourceType;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.Target;
import bluej.testmgr.record.InvokerRecord;
import bluej.testmgr.record.ObjectInspectInvokerRecord;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.utility.JavaReflective;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.JavaFXUtil;
import bluej.utility.javafx.UnfocusableScrollPane;
import bluej.views.ConstructorView;
import bluej.views.MethodView;

import greenfoot.GreenfootImage;
import greenfoot.World;
import greenfoot.WorldVisitor;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.guifx.classes.ClassDisplay;
import greenfoot.guifx.classes.GClassDiagram;
import greenfoot.guifx.classes.GClassNode;
import greenfoot.guifx.classes.ImportClassDialog;
import greenfoot.guifx.images.NewImageClassFrame;
import greenfoot.guifx.images.SelectImageFrame;
import greenfoot.guifx.images.ImageSelectionWatcher;
import greenfoot.guifx.soundrecorder.SoundRecorderControls;
import greenfoot.platforms.ide.GreenfootUtilDelegateIDE;
import greenfoot.record.GreenfootRecorder;

import greenfoot.util.GreenfootUtil;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import rmiextension.GreenfootDebugHandler;
import rmiextension.GreenfootDebugHandler.SimulationStateListener;
import rmiextension.ProjectManager;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static bluej.pkgmgr.target.ClassTarget.MENU_STYLE_INBUILT;

/**
 * Greenfoot's main window: a JavaFX replacement for GreenfootFrame which lives on the server VM.
 */
public class GreenfootStage extends Stage implements BlueJEventListener, FXCompileObserver, SimulationStateListener
{
    // These are the constants passed in the shared memory between processes,
    // hence they cannot be enums.  They are not persisted anywhere, so can
    // be changed at will (as long as they don't overlap).

    /*
     * Key events.  Followed by one integer which is the key code
     * (using the JavaFX KeyCode enum's ordinal method).
     */
    public static final int KEY_DOWN = 1;
    public static final int KEY_UP = 2;
    public static final int KEY_TYPED = 3;

    /*
     * Mouse events.  Followed by four integers:
     * X pos, Y pos, button index, click count
     */
    public static final int MOUSE_CLICKED = 11;
    public static final int MOUSE_PRESSED = 12;
    public static final int MOUSE_DRAGGED = 13;
    public static final int MOUSE_RELEASED = 14;
    public static final int MOUSE_MOVED = 15;

    /*
     * Commands or requests.  Unless otherwise specified,
     * followed by no integers.
     */
    public static final int COMMAND_RUN = 21;
    // Followed by drag-ID, X, Y:
    public static final int COMMAND_CONTINUE_DRAG = 22;
    // Followed by drag-ID:
    public static final int COMMAND_END_DRAG = 23;
    public static final int COMMAND_PAUSE = 24;
    public static final int COMMAND_ACT = 25;
    public static final int COMMAND_INSTANTIATE_WORLD = 26;
    // Followed by one integer per character in String answer.
    public static final int COMMAND_ANSWERED = 27;
    // Followed by an integer count of key size, then that many integer codepoints,
    // Then same again for value.  If value count is -1,
    // that means value is null (and thus was removed)
    public static final int COMMAND_PROPERTY_CHANGED = 28;
    // Discard the world, but don't make a new one
    public static final int COMMAND_DISCARD_WORLD = 29;
    
    private static int numberOfOpenProjects = 0;

    private final Project project;
    // The glass pane used to show a new actor while it is being placed:
    private final Pane glassPane;
    // Details of the new actor while it is being placed (null otherwise):
    private final ObjectProperty<NewActor> newActorProperty = new SimpleObjectProperty<>(null);
    private final WorldDisplay worldDisplay;
    private final GClassDiagram classDiagram;
    // The currently-showing context menu, or null if none
    private ContextMenu contextMenu;
    // Last mouse position, in scene coordinates:
    private Point2D lastMousePosInScene = new Point2D(0, 0);

    private final Button actButton;
    private final Button runButton;
    private final Button resetButton;
    
    private final List<Command> pendingCommands;
    private boolean instantiateWorldAfterDiscarded;
    
    public static enum State
    {
        RUNNING, RUNNING_REQUESTED_PAUSE, PAUSED, PAUSED_REQUESTED_ACT_OR_RUN, UNCOMPILED;
    }
    
    private final ObjectProperty<State> stateProperty = new SimpleObjectProperty<>(State.PAUSED);
    private boolean atBreakpoint = false;
    
    // Details for pick requests that we have sent to the debug VM:
    private static enum PickType
    {
        LEFT_CLICK, CONTEXT_MENU, DRAG;
    }

    // The next free pick ID that we will use
    private int nextPickId = 1;
    // The most recent pick ID that we are waiting on from the debug VM.
    private int curPickRequest;
    // The point at which the most recent pick happened.
    private Point2D curPickPoint;
    // If true, most recent pick was for right-click menu.  If false, was for a left-click drag.
    private PickType curPickType;
    // The current drag request ID, or -1 if not currently dragging:
    private int curDragRequest;

    /**
     * Because the ask request is sent as a continuous status rather than
     * a one-off event that we explicitly acknowledge, we keep track of the
     * last answer we sent so that we know if an ask request is newer than
     * the last answer or not.  That way we don't accidentally ask again
     * after the answer has been sent.
     */
    private int lastAnswer = -1;

    private final GreenfootRecorder saveTheWorldRecorder;

    @OnThread(Tag.FXPlatform)
    private final SoundRecorderControls soundRecorder;

    
    /**
     * Details for a new actor being added to the world, after you have made it
     * but before it is ready to be placed.
     */
    private static class NewActor
    {
        // The actual image node (will be a child of glassPane)
        private final Region previewNode;
        // Property tracking whether current location is valid or not
        private final BooleanProperty cannotDrop = new SimpleBooleanProperty(true);
        // The execution event that created the actor, if the actor has already been created:
        private final ExecutionEvent creationEvent;
        // The type name if the actor has not been constructed:
        private final String typeName;

        private Region makePreviewNode(ImageView imageView)
        {
            ImageView cannotDropIcon = new ImageView(this.getClass().getClassLoader().getResource("noParking.png").toExternalForm());
            cannotDropIcon.visibleProperty().bind(cannotDrop);
            StackPane.setAlignment(cannotDropIcon, Pos.TOP_RIGHT);
            StackPane stackPane = new StackPane(imageView, cannotDropIcon);
            stackPane.setEffect(new DropShadow(10.0, 3.0, 3.0, Color.BLACK));
            return stackPane;
        }

        public NewActor(ImageView imageView, ExecutionEvent creationEvent)
        {
            this.previewNode = makePreviewNode(imageView);
            this.creationEvent = creationEvent;
            this.typeName = null;
        }

        public NewActor(ImageView imageView, String typeName)
        {
            this.previewNode = makePreviewNode(imageView);
            this.creationEvent = null;
            this.typeName = typeName;
        }
    }


    /**
     * A command or event from the server VM to the debug VM, such
     * as keyboard/mouse event, Run, Reset, etc
     */
    private static class Command
    {
        // Commands are assigned a stricly increasing ID:
        private static int nextCommandSequence = 1;

        public final int commandSequence;
        public final int commandType;
        public final int[] extraInfo;

        private Command(int commandType, int... extraInfo)
        {
            this.commandSequence = nextCommandSequence++;
            this.commandType = commandType;
            this.extraInfo = extraInfo;
        }
    }


    /**
     * Check if an event is a key event.
     */
    public static boolean isKeyEvent(int event)
    {
        return event >= KEY_DOWN && event <= KEY_TYPED;
    }

    /**
     * Check if an even is a mouse event.
     */
    public static boolean isMouseEvent(int event)
    {
        return event >= MOUSE_CLICKED && event <= MOUSE_MOVED;
    }
    
    /**
     * Creates a GreenfootStage which receives a world image to draw from the
     * given shared memory buffer, protected by the given lock.
     * @param sharedMemoryLock The lock to claim before accessing sharedMemoryByte
     * @param sharedMemoryByte The shared memory buffer used to communicate with the debug VM
     */
    public GreenfootStage(Project project, GreenfootDebugHandler greenfootDebugHandler, FileChannel sharedMemoryLock, MappedByteBuffer sharedMemoryByte)
    {
        numberOfOpenProjects++;
        
        this.project = project;
        BlueJEvent.addListener(this);
        project.getUnnamedPackage().addCompileObserver(this);
        greenfootDebugHandler.setPickListener(this::pickResults);
        greenfootDebugHandler.setSimulationListener(this);
        this.saveTheWorldRecorder = new GreenfootRecorder();
        greenfootDebugHandler.setGreenfootRecorder(saveTheWorldRecorder);
        soundRecorder = new SoundRecorderControls(project);

        worldDisplay = new WorldDisplay();
        actButton = new Button(Config.getString("run.once"));
        runButton = new Button(Config.getString("controls.run.button"));
        resetButton = new Button(Config.getString("reset.world"));
        Node buttonAndSpeedPanel = new HBox(actButton, runButton, resetButton);
        pendingCommands = new ArrayList<>();
        actButton.setOnAction(e -> {
            act(pendingCommands);
        });
        runButton.setOnAction(e -> {
            if (stateProperty.get() == State.PAUSED)
            {
                pendingCommands.add(new Command(COMMAND_RUN));
                stateProperty.set(State.PAUSED_REQUESTED_ACT_OR_RUN);
            }
            else if (stateProperty.get() == State.RUNNING)
            {
                pendingCommands.add(new Command(COMMAND_PAUSE));
                stateProperty.set(State.RUNNING_REQUESTED_PAUSE);
            }
        });
        resetButton.setOnAction(e -> {
            if (stateProperty.get() != State.UNCOMPILED)
            {
                doReset();
                stateProperty.set(State.UNCOMPILED);
            }
        });
        classDiagram = new GClassDiagram(this, project);
        ScrollPane classDiagramScroll = new UnfocusableScrollPane(classDiagram);
        JavaFXUtil.expandScrollPaneContent(classDiagramScroll);

        ScrollPane worldViewScroll = new UnfocusableScrollPane(worldDisplay);
        JavaFXUtil.expandScrollPaneContent(worldViewScroll);
        BorderPane root = new BorderPane(worldViewScroll, makeMenu(pendingCommands), classDiagramScroll, buttonAndSpeedPanel, null);
        glassPane = new Pane();
        glassPane.setMouseTransparent(true);
        StackPane stackPane = new StackPane(root, glassPane);
        setupMouseForPlacingNewActor(stackPane);
        Scene scene = new Scene(stackPane);
        Config.addGreenfootStylesheets(scene);
        Config.addPMFStylesheets(scene);
        setScene(scene);

        setupWorldDrawingAndEvents(sharedMemoryLock, sharedMemoryByte, worldDisplay::setImage, pendingCommands);
        loadAndMirrorProperties(pendingCommands);
        // We send a reset to make a new world after the project properties have been sent across:
        pendingCommands.add(new Command(COMMAND_INSTANTIATE_WORLD));
        JavaFXUtil.addChangeListenerPlatform(stateProperty, this::updateGUIState);
        JavaFXUtil.addChangeListenerPlatform(focusedProperty(), focused -> {
            if (project != null)
            {
                DataCollector.recordGreenfootEvent(project, GreenfootInterfaceEvent.WINDOW_ACTIVATED);
                for (ClassTarget classTarget : project.getUnnamedPackage().getClassTargets())
                {
                    Editor editor = classTarget.getEditorIfOpen();
                    if (editor != null)
                    {
                        editor.cancelFreshState();
                    }
                }
                if (worldDisplay.isGreyedOut() && stateProperty.get() != State.UNCOMPILED)
                {
                    doReset();
                }
            }
        });
        
        setOnCloseRequest((e) -> {
            doClose(false);
        });
        
        /* Uncomment this to use ScenicView temporarily during development (use reflection to avoid needing to mess with Ant classpath)
        try
        {
            getClass().getClassLoader().loadClass("org.scenicview.ScenicView").getMethod("show", Scene.class).invoke(null, scene);
        }
        catch (Exception e)
        {
            Debug.reportError(e);
        }*/
    }

    /**
     * Perform a reset.  This is done by sending a discard-world command, and
     * setting a flag noting that we want to send an instantiate-world command
     * once the discard-world command has taken effect.
     */
    private void doReset()
    {
        pendingCommands.add(new Command(COMMAND_DISCARD_WORLD));
        instantiateWorldAfterDiscarded = true;
    }

    private void loadAndMirrorProperties(List<Command> pendingCommands)
    {
        Properties props = project.getUnnamedPackage().getLastSavedProperties();
        props.forEach((key, value) -> {
            if (key instanceof String && (value == null || value instanceof String))
            {
                // We need an array with key-length, key codepoints, value-length (-1 if null), value codepoints
                int[] keyCodepoints = ((String) key).codePoints().toArray();
                int[] valueCodepoints = value == null ? new int[0] : ((String)value).codePoints().toArray();
                int[] combined = new int[1 + keyCodepoints.length + 1 + valueCodepoints.length];
                combined[0] = keyCodepoints.length;
                System.arraycopy(keyCodepoints, 0, combined, 1, keyCodepoints.length);
                combined[1 + keyCodepoints.length] = value == null ? -1 : valueCodepoints.length;
                System.arraycopy(valueCodepoints, 0, combined, 2 + keyCodepoints.length, valueCodepoints.length);
                pendingCommands.add(new Command(COMMAND_PROPERTY_CHANGED, combined));
            }
        });
        
    }

    /**
     * Allow the user to choose a scenario to open, and open it.
     */
    private void doOpenScenario()
    {
        File choice = FileUtility.getOpenProjectFX(this);
        if (choice != null)
        {
            Project p = Project.openProject(choice.getAbsolutePath());
            if (p != null) {
                ProjectManager.instance().launchProject(p.getBProject());
            }
        }                            
    }
    
    /**
     * Close the scenario that this stage is showing.
     * @param keepLast  if true, don't close the last stage; leave it open without a scenario. If
     *                  false, quit BlueJ when the last stage is closed.
     */
    private void doClose(boolean keepLast)
    {
        // Remove inspectors, terminal, etc:
        Project.cleanUp(project);

        numberOfOpenProjects--;
        if (numberOfOpenProjects == 0)
        {
            if (keepLast)
            {
                // TODO: remove the project details from this frame but keep frame open
            }
            else
            {
                close();
                Main.doQuit();
            }
        }
        else
        {
            close();
        }
    }
    
    /**
     * Perform a single act step, if paused, by adding to the list of pending commands.
     */
    private void act(List<Command> pendingCommands)
    {
        if (stateProperty.get() == State.PAUSED)
        {
            pendingCommands.add(new Command(COMMAND_ACT));
            stateProperty.set(State.PAUSED_REQUESTED_ACT_OR_RUN);
        }
    }

    /**
     * Make the menu bar for the whole window.
     */
    private MenuBar makeMenu(List<Command> pendingCommands)
    {
        Menu scenarioMenu = new Menu(Config.getString("menu.scenario"), null,
                makeMenuItem("stride.new.project",
                        new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN),
                        () -> {} // TODO
                    ),
                    makeMenuItem("java.new.project",
                        new KeyCodeCombination(KeyCode.J, KeyCombination.SHORTCUT_DOWN),
                        () -> {} // TODO
                    ),
                    makeMenuItem("open.project",
                        new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN),
                        this::doOpenScenario
                    ),
                    new Menu(Config.getString("menu.openRecent"), null), // TODO
                    makeMenuItem("project.close",
                        new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN),
                        () -> { doClose(true); }
                    ),
                    makeMenuItem("project.save",
                        new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN),
                        () -> {} // TODO
                    ),
                    makeMenuItem("project.saveAs",
                        null,
                        () -> {} // TODO
                    ),
                    new SeparatorMenuItem(),
                    makeMenuItem("export.project",
                        new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN),
                        () -> {} // TODO
                    )
                );
        
        if (! Config.isMacOS()) {
            scenarioMenu.getItems().add(new SeparatorMenuItem());
            scenarioMenu.getItems().add(makeMenuItem("greenfoot.quit",
                    new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN),
                    () -> {}) // TODO
                );
        }
        
        return new MenuBar(
            scenarioMenu,
            new Menu(Config.getString("menu.edit"), null,
                makeMenuItem("new.other.class", new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
                            () -> newNonImageClass(project.getUnnamedPackage(), null)),
                makeMenuItem("import.action", new KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN), () -> doImportClass())
            ),
            new Menu(Config.getString("menu.controls"), null,
                    makeMenuItem("run.once", new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN), () -> act(pendingCommands)),
                    JavaFXUtil.makeCheckMenuItem(Config.getString("menu.soundRecorder"), soundRecorder.getShowingProperty(),
                            new KeyCodeCombination(KeyCode.U, KeyCombination.SHORTCUT_DOWN), this::toggleSoundRecorder)
            ),
            new Menu(Config.getString("menu.tools"), null,
                    makeMenuItem("menu.tools.generateDoc", new KeyCodeCombination(KeyCode.G, KeyCombination.SHORTCUT_DOWN), this::generateDocumentation)
            )
        );
    }

    /**
     * Show/hide the sound soundRecorder.
     *
     * @param showing if true show the soundRecorder, hide for false.
     */
    private void toggleSoundRecorder(Boolean showing)
    {
        if (showing)
        {
            soundRecorder.show();
        }
        else
        {
            soundRecorder.close();
        }
    }

    /**
     * Generates the Documentation for the current scenario
     */
    private void generateDocumentation()
    {
        String message = project.generateDocumentation();
        if (message.length() != 0) {
            DialogManager.showTextFX(this, message);
        }
    }

    /**
     * Make a single menu item
     * @param nameKey The key to lookup via Config.getString for the name
     * @param accelerator The accelerator if any (null if none)
     * @param action The action to perform when the menu item is activated
     * @return The MenuItem combining all these items.
     */
    private MenuItem makeMenuItem(String nameKey, KeyCombination accelerator, FXPlatformRunnable action)
    {
        return JavaFXUtil.makeMenuItem(Config.getString(nameKey), action, accelerator);
    }

    private void updateGUIState(State newState)
    {
        actButton.setDisable(newState != State.PAUSED || atBreakpoint);
        runButton.setDisable((newState != State.PAUSED && newState != State.RUNNING) || atBreakpoint);
        if (newState == State.RUNNING || newState == State.RUNNING_REQUESTED_PAUSE)
        {
            runButton.setText(Config.getString("controls.pause.button"));
        }
        else
        {
            runButton.setText(Config.getString("controls.run.button"));
        }
    }

    /**
     * Setup mouse listeners for showing a new actor underneath the mouse cursor
     */
    private void setupMouseForPlacingNewActor(StackPane stackPane)
    {
        stackPane.setOnMouseMoved(e -> {
            lastMousePosInScene = new Point2D(e.getSceneX(), e.getSceneY());
            if (newActorProperty.get() != null)
            {
                // We use e.getX/getY here, which is already local to StackPane:
                // TranslateX/Y seems to have a bit less lag than LayoutX/Y:
                newActorProperty.get().previewNode.setTranslateX(e.getX() - newActorProperty.get().previewNode.getWidth() / 2.0);
                newActorProperty.get().previewNode.setTranslateY(e.getY() - newActorProperty.get().previewNode.getHeight() / 2.0);

                newActorProperty.get().cannotDrop.set(!worldDisplay.worldContains(worldDisplay.sceneToWorld(lastMousePosInScene)));
            }
        });
        stackPane.setOnMouseClicked(e -> {
            lastMousePosInScene = new Point2D(e.getSceneX(), e.getSceneY());
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1 && newActorProperty.get() != null)
            {
                Point2D dest = worldDisplay.sceneToWorld(lastMousePosInScene);
                if (worldDisplay.worldContains(dest))
                {
                    // Bit hacky to pass positions as strings, but mirroring the values as integers
                    // would have taken a lot of code changes to route through to VMReference:
                    DebuggerObject xObject = project.getDebugger().getMirror("" + (int) dest.getX());
                    DebuggerObject yObject = project.getDebugger().getMirror("" + (int) dest.getY());
                    DebuggerObject actor = null;
                    ExecutionEvent executionEvent = newActorProperty.get().creationEvent;
                    if (executionEvent != null)
                    {
                        // Place the already-constructed actor:
                        actor = executionEvent.getResultObject();
                        // Can't place same instance twice:
                        newActorProperty.set(null);
                        saveTheWorldRecorder.createActor(executionEvent.getResultObject(), executionEvent.getParameters(), executionEvent.getSignature());
                    }
                    else
                    {
                        // Must be shift-clicking; will need to make a new instance:
                        DebuggerResult result = project.getDebugger().instantiateClass(newActorProperty.get().typeName).get();
                        if (result.getResultObject() != null)
                        {
                            actor = result.getResultObject();
                            saveTheWorldRecorder.createActor(actor, new String[0], new JavaType[0]);
                        }
                    }
                    if (actor != null)
                    {
                        // TODO these coordinates aren't right for cell-based worlds (>1 pixel per cell)
                        saveTheWorldRecorder.addActorToWorld(actor, (int)dest.getX(), (int)dest.getY());

                        project.getDebugger().instantiateClass(
                                "greenfoot.core.AddToWorldHelper",
                                new String[]{"java.lang.Object", "java.lang.String", "java.lang.String"},
                                new DebuggerObject[]{actor, xObject, yObject});
                    }

                }
            }
        });
        newActorProperty.addListener((prop, oldVal, newVal) -> {
            if (oldVal != null)
            {
                glassPane.getChildren().remove(oldVal.previewNode);
            }

            if (newVal != null)
            {
                glassPane.getChildren().add(newVal.previewNode);
                // Need to do a layout to get the correct width and height:
                glassPane.requestLayout();
                glassPane.layout();

                newVal.previewNode.setTranslateX(lastMousePosInScene.getX() - newVal.previewNode.getWidth() / 2.0);
                newVal.previewNode.setTranslateY(lastMousePosInScene.getY() - newVal.previewNode.getHeight() / 2.0);
                newVal.cannotDrop.set(!worldDisplay.worldContains(worldDisplay.sceneToWorld(lastMousePosInScene)));
            }
        });
    }

    /**
     * Update our latest mouse position to the given *SCREEN* (not Scene!) position.
     * Used by GClassDiagram to track position even while a context menu is showing.
     */
    public void setLatestMousePosOnScreen(double screenX, double screenY)
    {
        // There is no screenToScene call, so we must go via this convoluted path:
        Point2D rootPoint = getScene().getRoot().screenToLocal(screenX, screenY);
        lastMousePosInScene = getScene().getRoot().localToScene(rootPoint);
    }

    /**
     * Sets up the drawing of the world from the shared memory buffer, and the writing
     * of keyboard and mouse events back to the buffer.
     *
     * @param sharedMemoryLock The lock to use to lock the shared memory buffer before access.
     * @param sharedMemoryByte The shared memory buffer to read/write from/to
     * @param setImage The function to call to set the new image
     * @param pendingCommands The list of pending commands to send to the debug VM
     */
    private void setupWorldDrawingAndEvents(FileChannel sharedMemoryLock, MappedByteBuffer sharedMemoryByte, FXPlatformConsumer<Image> setImage, List<Command> pendingCommands)
    {
        IntBuffer sharedMemory = sharedMemoryByte.asIntBuffer();

        getScene().addEventFilter(KeyEvent.ANY, e -> {
            // Ignore keypresses if we are currently waiting for an ask-answer:
            if (worldDisplay.isAsking())
                return;
            
            int eventType;
            if (e.getEventType() == KeyEvent.KEY_PRESSED)
            {
                if (e.getCode() == KeyCode.ESCAPE && newActorProperty.get() != null)
                {
                    newActorProperty.set(null);
                    return;
                }
                
                // We only want fully paused; if they've requested a run, don't allow a shift-click:
                boolean paused = stateProperty.get() == State.PAUSED;
                ClassTarget selectedClassTarget = classDiagram.getSelectedClassTarget();
                if (e.getCode() == KeyCode.SHIFT && newActorProperty.get() == null && selectedClassTarget != null && paused)
                {
                    // Holding shift, so show actor preview if it is an actor with no-arg constructor:
                    Reflective type = selectedClassTarget.getTypeReflective();
                    if (getActorReflective().isAssignableFrom(type) && type.getDeclaredConstructors().stream().anyMatch(c -> c.getParamTypes().isEmpty() && !Modifier.isPrivate(c.getModifiers())))
                    {
                        newActorProperty.set(new NewActor(getImageViewForClass(type), selectedClassTarget.getBaseName()));
                    }
                }

                eventType = KEY_DOWN;
            }
            else if (e.getEventType() == KeyEvent.KEY_RELEASED)
            {
                eventType = KEY_UP;

                if (e.getCode() == KeyCode.SHIFT && newActorProperty.get() != null && newActorProperty.get().creationEvent == null)
                {
                    newActorProperty.set(null);
                }
            }
            else if (e.getEventType() == KeyEvent.KEY_TYPED)
            {
                eventType = KEY_TYPED;
            }
            else
            {
                return;
            }

            pendingCommands.add(new Command(eventType, e.getCode().ordinal()));
            e.consume();
        });

        worldDisplay.setOnContextMenuRequested(e -> {
            boolean paused = stateProperty.get() == State.PAUSED;
            if (paused)
            {
                // We don't want to block GUI while waiting for pick: 
                Utility.runBackground(() -> pickRequest(e.getX(), e.getY(), PickType.CONTEXT_MENU));
            }
        });
        worldDisplay.addEventFilter(MouseEvent.ANY, e -> {
            boolean paused = stateProperty.get() == State.PAUSED;
            int eventType;
            if (e.getEventType() == MouseEvent.MOUSE_CLICKED)
            {
                if (e.getButton() == MouseButton.PRIMARY)
                {
                    hideContextMenu();
                    if (paused)
                    {
                        // We don't want to block GUI while waiting for pick:
                        Utility.runBackground(() -> pickRequest(e.getX(), e.getY(), PickType.LEFT_CLICK));
                    }
                }
                eventType = MOUSE_CLICKED;
            }
            else if (e.getEventType() == MouseEvent.MOUSE_PRESSED)
            {
                eventType = MOUSE_PRESSED;
            }
            else if (e.getEventType() == MouseEvent.MOUSE_RELEASED)
            {
                eventType = MOUSE_RELEASED;
                // Finish any current drag:
                if (curDragRequest != -1)
                {
                    pendingCommands.add(new Command(COMMAND_END_DRAG, curDragRequest));
                    curDragRequest = -1;
                }
            }
            else if (e.getEventType() == MouseEvent.MOUSE_DRAGGED)
            {
                // Continue the drag if one is going:
                if (e.getButton() == MouseButton.PRIMARY && paused && curDragRequest != -1)
                {
                    pendingCommands.add(new Command(COMMAND_CONTINUE_DRAG, curDragRequest, (int)e.getX(), (int)e.getY()));
                }

                eventType = MOUSE_DRAGGED;
            }
            else if (e.getEventType() == MouseEvent.MOUSE_MOVED)
            {
                eventType = MOUSE_MOVED;
            }
            else
            {
                if (e.getEventType() == MouseEvent.DRAG_DETECTED && paused)
                {
                    // Begin a drag:
                    pickRequest(e.getX(), e.getY(), PickType.DRAG);
                }
                return;
            }
            pendingCommands.add(new Command(eventType, (int)e.getX(), (int)e.getY(), e.getButton().ordinal(), e.getClickCount()));
        });

        new AnimationTimer()
        {
            int lastSeq = 0;
            WritableImage img;
            @Override
            public void handle(long now)
            {
                boolean sizeToScene = false;
                try (FileLock fileLock = sharedMemoryLock.lock())
                {
                    int seq = sharedMemory.get(1);
                    if (seq > lastSeq)
                    {
                        // The client VM has painted a new frame for us:
                        lastSeq = seq;
                        sharedMemory.position(1);
                        sharedMemory.put(-seq);
                        int width = sharedMemory.get();
                        int height = sharedMemory.get();
                        if (width != 0 && height != 0)
                        {
                            if (img == null || img.getWidth() != width || img.getHeight() != height)
                            {
                                img = new WritableImage(width == 0 ? 1 : width, height == 0 ? 1 : height);
                                sizeToScene = true;
                            }
                            sharedMemoryByte.position(sharedMemory.position() * 4);
                            img.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getByteBgraPreInstance(), sharedMemoryByte, width * 4);
                            setImage.accept(img);
                        }
                        // Have to move sharedMemory position manually because
                        // the sharedMemory buffer doesn't share position with sharedMemoryByte buffer:
                        sharedMemory.position(sharedMemory.position() + width * height);
                        int lastAckCommand = sharedMemory.get();
                        // Get rid of all commands that the client has confirmed it has seen:
                        if (lastAckCommand != -1)
                        {
                            // Get rid of any acknowledged commands, and record if
                            // any of them was a discard command:
                            boolean discarded = false;
                            for (Iterator<Command> iterator = pendingCommands.iterator(); iterator.hasNext(); )
                            {
                                Command pendingCommand = iterator.next();
                                if (pendingCommand.commandSequence <= lastAckCommand)
                                {
                                    if (pendingCommand.commandType == COMMAND_DISCARD_WORLD)
                                    {
                                        discarded = true;
                                    }
                                    iterator.remove();
                                }
                            }
                            if (discarded)
                            {
                                worldDisplay.greyOutWorld();
                                // Were we waiting for discard before then instantiating?
                                if (instantiateWorldAfterDiscarded)
                                {
                                    instantiateWorldAfterDiscarded = false;
                                    pendingCommands.add(new Command(COMMAND_INSTANTIATE_WORLD));
                                }
                            }
                        }
                        
                        int askId = sharedMemory.get();
                        if (askId >= 0 && askId > lastAnswer)
                        {
                            // Length followed by codepoints for the prompt string:
                            int askLength = sharedMemory.get();
                            int[] promptCodepoints = new int[askLength];
                            sharedMemory.get(promptCodepoints);
                            
                            // Tell worldDisplay to ask:
                            worldDisplay.ensureAsking(new String(promptCodepoints, 0, promptCodepoints.length), (String s) -> {
                                Command answerCommand = new Command(COMMAND_ANSWERED, s.codePoints().toArray());
                                pendingCommands.add(answerCommand);
                                // Remember that we've now answered:
                                lastAnswer = answerCommand.commandSequence;
                            });
                        }
                        sharedMemory.position(2);
                        writeCommands(pendingCommands);
                    }
                    else if (!pendingCommands.isEmpty())
                    {
                        // The debug VM hasn't painted a new frame, but we have new commands to send:
                        sharedMemory.position(1);
                        sharedMemory.put(-lastSeq);
                        writeCommands(pendingCommands);
                    }
                }
                catch (IOException ex)
                {
                    Debug.reportError(ex);
                }
                // WARNING: sizeToScene can actually re-enter AnimationTimer.handle!
                // Hence we must call this after releasing the lock and completing
                // all other code, so that a re-entry doesn't cause any conflicts:
                if (sizeToScene)
                {
                    sizeToScene();
                }
            }

            private void writeCommands(List<Command> pendingCommands)
            {
                // Number of commands:
                sharedMemory.put(pendingCommands.size());
                for (Command pendingCommand : pendingCommands)
                {
                    // Start with sequence ID:
                    sharedMemory.put(pendingCommand.commandSequence);
                    // Put size of this command (measured in integers), including command type:
                    sharedMemory.put(pendingCommand.extraInfo.length + 1);
                    // Then put that many integers:
                    sharedMemory.put(pendingCommand.commandType);
                    sharedMemory.put(pendingCommand.extraInfo);
                }
            }
        }.start();
    }

    /**
     * Performs a pick request on the debug VM at given coordinates.
     */
    @OnThread(Tag.Worker)
    private void pickRequest(double x, double y, PickType pickType)
    {
        curPickType = pickType;
        // Bit hacky to pass positions as strings, but mirroring the values as integers
        // would have taken a lot of code changes to route through to VMReference:
        DebuggerObject xObject = project.getDebugger().getMirror("" + (int) x);
        DebuggerObject yObject = project.getDebugger().getMirror("" + (int) y);
        int thisPickId = nextPickId++;
        DebuggerObject pickIdObject = project.getDebugger().getMirror("" + thisPickId);
        String requestTypeString = pickType == PickType.DRAG ? "drag" : "";
        DebuggerObject requestTypeObject = project.getDebugger().getMirror(requestTypeString);
        // One pick at a time only:
        curPickRequest = thisPickId;
        curPickPoint = new Point2D(x, y);

        // Need to find out which actors are at the point:
        project.getDebugger().instantiateClass("greenfoot.core.PickActorHelper", new String[]{"java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String"}, new DebuggerObject[]{xObject, yObject, pickIdObject, requestTypeObject});
        // Once that completes, pickResults(..) will be called.
    }

    /**
     * Callback when a pick has completed (i.e. a request to find actors at given position)
     * @param pickId The ID of the pick has requested
     * @param actors The list of actors found.  May be any size.
     * @param world The world -- only relevant if actors list is empty.
     */
    @OnThread(Tag.Any)
    public void pickResults(int pickId, List<DebuggerObject> actors, DebuggerObject world)
    {
        Platform.runLater(() -> {
            if (curPickRequest != pickId)
            {
                return; // Pick has been cancelled by a more recent pick, so ignore
            }

            if (curPickType == PickType.CONTEXT_MENU)
            {
                // If single actor, show simple context menu:
                if (!actors.isEmpty())
                {
                    // This is a list of menus; if there's only one we'll display
                    // directly in context menu.  If there's more than one, we'll
                    // have a higher level menu to pick between them.
                    List<Menu> actorMenus = new ArrayList<>();
                    for (DebuggerObject actor : actors)
                    {
                        Target target = project.getTarget(actor.getClassName());
                        // Should always be ClassTarget, but check in case:
                        if (target instanceof ClassTarget)
                        {
                            Menu menu = new Menu(actor.getClassName());
                            ObjectWrapper.createMethodMenuItems(menu.getItems(), project.loadClass(actor.getClassName()), new RecordInvoke(actor), "", true);
                            menu.getItems().add(makeInspectMenuItem(actor));

                            MenuItem removeItem = new MenuItem(Config.getString("world.handlerDelegate.remove"));
                            JavaFXUtil.addStyleClass(removeItem, MENU_STYLE_INBUILT);
                            removeItem.setOnAction(e -> {
                                project.getDebugger().instantiateClass(
                                        "greenfoot.core.RemoveFromWorldHelper",
                                        new String[]{"java.lang.Object"},
                                        new DebuggerObject[]{actor});
                                saveTheWorldRecorder.removeActor(actor);
                            });
                            menu.getItems().add(removeItem);
                            actorMenus.add(menu);
                        }
                    }
                    hideContextMenu();
                    contextMenu = new ContextMenu();
                    contextMenu.setOnHidden(e -> {
                        contextMenu = null;
                    });
                    if (actorMenus.size() == 1)
                    {
                        // No point showing higher-level menu with one item, collapse:
                        contextMenu.getItems().addAll(actorMenus.get(0).getItems());
                    }
                    else
                    {
                        contextMenu.getItems().addAll(actorMenus);
                    }
                    Point2D screenLocation = worldDisplay.localToScreen(curPickPoint);
                    contextMenu.show(worldDisplay, screenLocation.getX(), screenLocation.getY());
                }
                else
                {
                    Target target = project.getTarget(world.getClassName());
                    // Should always be ClassTarget, but check in case:
                    if (target instanceof ClassTarget)
                    {
                        hideContextMenu();
                        contextMenu = new ContextMenu();
                        contextMenu.setOnHidden(e -> {
                            contextMenu = null;
                        });
                        ObjectWrapper.createMethodMenuItems(contextMenu.getItems(), project.loadClass(world.getClassName()), new RecordInvoke(world), "", true);
                        contextMenu.getItems().add(makeInspectMenuItem(world));

                        MenuItem saveTheWorld = new MenuItem(Config.getString("save.world"));
                        // Temporary while developing - print out saved world to Terminal window:
                        saveTheWorld.setOnAction(e -> {
                            saveTheWorldRecorder.writeCode(className -> ((ClassTarget) project.getUnnamedPackage().getTarget(className)).getEditor());
                        });
                        contextMenu.getItems().add(saveTheWorld);

                        Point2D screenLocation = worldDisplay.localToScreen(curPickPoint);
                        contextMenu.show(worldDisplay, screenLocation.getX(), screenLocation.getY());
                    }
                }
            }
            else if (curPickType == PickType.DRAG && !actors.isEmpty())
            {
                // Left-click drag, and there is an actor there, so begin drag:
                curDragRequest = pickId;
            }
            else if (curPickType == PickType.LEFT_CLICK && !actors.isEmpty())
            {
                DebuggerObject target = actors.get(0);
                PkgMgrFrame pmf = PkgMgrFrame.findFrame(project.getUnnamedPackage());

                // We must put the object on the bench so that it has a name and wrapper:
                String objInstanceName = pmf.putObjectOnBench(target.getClassName().toLowerCase(), target, target.getGenType(), null, null);
                // Then we can issue the event saying that it has been clicked:
                pmf.getObjectBench().fireObjectSelectedEvent(pmf.getObjectBench().getObject(objInstanceName));
            }
        });
    }

    /**
     * Hide the context menu if one is currently showing on the world.
     */
    private void hideContextMenu()
    {
        if (contextMenu != null)
        {
            // The onHidden handler sets the contextMenu field back to null:
            contextMenu.hide();
        }
    }

    /**
     * Makes a MenuItem with an Inspect command for the given debugger object
     */
    private MenuItem makeInspectMenuItem(DebuggerObject debuggerObject)
    {
        MenuItem inspectItem = new MenuItem(Config.getString("debugger.objectwrapper.inspect"));
        JavaFXUtil.addStyleClass(inspectItem, MENU_STYLE_INBUILT);
        inspectItem.setOnAction(e -> {
            InvokerRecord ir = new ObjectInspectInvokerRecord(debuggerObject.getClassName());
            project.getInspectorInstance(debuggerObject, debuggerObject.getClassName(), project.getUnnamedPackage(), ir, this, null);  // shows the inspector
        });
        return inspectItem;
    }

    /**
     * Overrides BlueJEventListener method
     */
    @Override
    public void blueJEvent(int eventId, Object arg)
    {
        // Look for results of actor/world/other-object construction:
        if (eventId == BlueJEvent.EXECUTION_RESULT)
        {
            ExecutionEvent executionEvent = (ExecutionEvent)arg;
            // Was it a constructor of a class in the default package?
            if (executionEvent.getMethodName() == null && executionEvent.getPackage().getQualifiedName().equals(""))
            {
                String className = executionEvent.getClassName();
                Target t = project.getTarget(className);
                // Should always be a ClassTarget but just in case:
                if (t instanceof ClassTarget)
                {
                    ClassTarget ct = (ClassTarget) t;
                    Reflective typeReflective = ct.getTypeReflective();
                    if (typeReflective != null && getActorReflective().isAssignableFrom(typeReflective))
                    {
                        // It's an actor!
                        ImageView imageView = getImageViewForClass(typeReflective);
                        if (imageView != null)
                        {
                            newActorProperty.set(new NewActor(imageView, executionEvent));
                        }
                    }
                    else if (typeReflective != null && getWorldReflective().isAssignableFrom(typeReflective))
                    {
                        // It's a world

                        // Not a good idea to call back debugger from a listener, so runLater:
                        JavaFXUtil.runAfterCurrent(() -> project.getDebugger().instantiateClass("greenfoot.core.SetWorldHelper", new String[]{"java.lang.Object"}, new DebuggerObject[]{executionEvent.getResultObject()}));
                    }
                    else
                    {
                        // If neither actor nor world, we just inspect the constructed object:
                        project.getInspectorInstance(executionEvent.getResultObject(), "<object>", executionEvent.getPackage(), null, this, null);
                    }
                }
            }
        }
    }

    /**
     * Gets the image, if one has been set, for the given class target, by looking at that
     * class and its ancestors.  Returns the loaded image, or null if none was found.
     */
    public Image getImageForClassTarget(ClassTarget classTarget)
    {
        return JavaFXUtil.loadImage(getImageFilename(classTarget.getTypeReflective()));
    }

    /**
     * Get an ImageView with the appropriate preview image for this class type.
     * If no suitable image found, the Greenfoot icon will be used.
     */
    private ImageView getImageViewForClass(Reflective typeReflective)
    {
        File file = getImageFilename(typeReflective);
        // If no image, use the default:
        if (file == null)
        {
            file = new File(getGreenfootLogoPath());
        }

        ImageView imageView = null;
        try
        {
            imageView = new ImageView(file.toURI().toURL().toExternalForm());
        }
        catch (MalformedURLException e)
        {
            Debug.reportError(e);
        }
        return imageView;
    }

    private static String getGreenfootLogoPath()
    {
        File libDir = Config.getGreenfootLibDir();
        return libDir.getAbsolutePath() + "/imagelib/other/greenfoot.png";
    }

    /**
     * Returns a file name for the image of the first class
     * in the given class' class hierarchy that has an image set.
     */
    private File getImageFilename(Reflective type)
    {
        while (type != null) {
            String className = type.getName();
            String imageFileName = project.getUnnamedPackage().getLastSavedProperties().getProperty("class." + className + ".image");
            if (imageFileName != null) {
                File imageDir = new File(project.getProjectDir(), "images");
                return new File(imageDir, imageFileName);
            }
            type = type.getSuperTypesR().stream().filter(t -> !t.isInterface()).findFirst().orElse(null);
        }
        return null;
    }


    @Override
    public void startCompile(CompileInputFile[] sources, CompileReason reason, CompileType type, int compilationSequence)
    {
        // Grey out the world display until compilation finishes:
        worldDisplay.greyOutWorld();
        stateProperty.set(State.UNCOMPILED);
    }

    @Override
    public boolean compilerMessage(Diagnostic diagnostic, CompileType type)
    {
        return false;
    }

    @Override
    public void endCompile(CompileInputFile[] sources, boolean succesful, CompileType type, int compilationSequence)
    {
        // We only create the world if the window is focused, otherwise
        // we let it remain greyed out:
        if (isFocused())
        {
            doReset();
        }
    }


    @Override
    @OnThread(Tag.Any)
    public void simulationStartedRunning()
    {
        Platform.runLater(() -> stateProperty.set(State.RUNNING));
    }

    @Override
    @OnThread(Tag.Any)
    public void simulationPaused()
    {
        Platform.runLater(() -> stateProperty.set(State.PAUSED));
    }

    @Override
    public @OnThread(Tag.Any) void simulationInitialisedWorld()
    {
        // This shows that compilation has finished, and we are back in the compiled but paused state:
        Platform.runLater(() -> stateProperty.set(State.PAUSED));
    }

    @Override
    public @OnThread(Tag.Any) void simulationDebugHalted()
    {
        atBreakpoint = true;
        updateGUIState(stateProperty.get());
    }

    @Override
    public @OnThread(Tag.Any) void simulationDebugResumed()
    {
        atBreakpoint = false;
        updateGUIState(stateProperty.get());
    }

    /**
     * Show a dialog to set the image for the given class target.  Will only be called
     * for classes which have Actor or World as an ancestor.
     */
    public void setImageFor(ClassTarget classTarget, ClassDisplay classDisplay)
    {
        final World currentWorld = WorldHandler.getInstance().getWorld();
        // save the original background if possible
        final GreenfootImage originalBackground = ((currentWorld == null) ?
                null : WorldVisitor.getBackgroundImage(currentWorld));

        // allow the previewing if we are setting the image of the current world.
        ImageSelectionWatcher watcher = null;
        if (currentWorld != null && currentWorld.getClass().getName().equals(classTarget.getQualifiedName()))
        {
            watcher = imageFile -> {
                if (imageFile != null)
                {
                    Simulation.getInstance().runLater(() -> {
                        if (WorldHandler.getInstance().getWorld() == currentWorld)
                        {
                            currentWorld.setBackground(imageFile.getAbsolutePath());
                        }
                    });
                }
            };
        }

        // initialise our image library frame
        Optional<File> result = new SelectImageFrame(this, classTarget, watcher).showAndWait();
        if (result.isPresent())
        {
            // set the image of the class to the selected file
            classDisplay.setImage(new Image(result.get().toURI().toString()));
        }
        else if (currentWorld != null)
        {
            // if cancelled, reset the world background to the original format
            // to avoid white screens or preview images being left there.
            Simulation.getInstance().runLater(() -> currentWorld.setBackground(originalBackground));
        }
    }

    /**
     * Show a dialog for a new name, and then duplicate the class target with that new name.
     */
    public void duplicateClass(ClassTarget originalClassTarget)
    {
        String originalClassName = originalClassTarget.getDisplayName();
        SourceType sourceType = originalClassTarget.getSourceType();
        NewClassDialog dialog = new NewClassDialog(this, sourceType);
        dialog.setSuggestedClassName("CopyOf" + originalClassName);
        dialog.disableLanguageBox(true);
        dialog.showAndWait().ifPresent(newClassInfo -> {
            String newClassName = newClassInfo.className;
            try
            {
                File dir = originalClassTarget.getPackage().getProject().getProjectDir();
                final String extension = sourceType.getExtension();
                File newFile = new File(dir, newClassName + "." + extension);
                File originalFile = new File(dir, originalClassName + "." + extension);
                GreenfootUtilDelegateIDE.getInstance().duplicate(originalClassName, newClassName, originalFile, newFile, sourceType);
                ClassTarget newClass = originalClassTarget.getPackage().addClass(newClassName);
                classDiagram.addClass(newClass);
                // TODO set the image property (recorded as part of GREENFOOT-641 ticket)
            }
            catch (IOException ioe)
            {
                Debug.reportError(ioe);
            }
        });
    }

    /**
     * Import a class using the import class dialog
     */
    public void doImportClass()
    {
        File srcFile = new ImportClassDialog(this).showAndWait().orElse(null);

        if (srcFile != null)
        {
            boolean librariesImportedFlag = false;
            String className = GreenfootUtil.removeExtension(srcFile.getName());
            
            // Check if a class of the same name already exists in the project.
            // Renaming would be too tricky, so just issue error and stop in that case:
            for (ClassTarget preexist : project.getUnnamedPackage().getClassTargets())
            {
                if (preexist.getQualifiedName().equals(className))
                {
                    DialogManager.showMessageFX(this, "import-class-exists", className);
                    return;
                }
            }
            File srcImage = ImportClassDialog.findImage(srcFile);
            File destImage = null;
            if (srcImage != null)
            {
                destImage = new File(new File(project.getProjectDir(), "images"), srcImage.getName());
                if (destImage.exists())
                {
                    DialogManager.showMessageFX(this, "import-image-exists", srcImage.getName());
                }
            }

            // Copy the java/class file cross:
            File destFile = new File(project.getProjectDir(), srcFile.getName());
            GreenfootUtil.copyFile(srcFile, destFile);

            // Copy the lib files cross:
            File libFolder = new File(srcFile.getParentFile(), className + "/lib");
            if ( (libFolder.exists()) && (libFolder.listFiles().length > 0) )
            {
                for (File srcLibFile : libFolder.listFiles())
                {
                    File destLibFile = new File(project.getProjectDir(), "+libs/" + srcLibFile.getName());
                    GreenfootUtil.copyFile(srcLibFile, destLibFile);
                }
                librariesImportedFlag = true;
            }

            // We must reload the package to be able to access the GClass object:
            project.getUnnamedPackage().reload();
            ClassTarget gclass = (ClassTarget)project.getUnnamedPackage().getTarget(className);

            if (gclass == null)
            {
                return;
            }

            // Copy the image across and set it as the class image:
            if (srcImage != null && destImage != null && !destImage.exists())
            {
                GreenfootUtil.copyFile(srcImage, destImage);
                // TODO set the image property (recorded as part of GREENFOOT-641 ticket)
                //project.setClassProperty("image", destImage.getName());
            }

            //Finally, update the class browser:
            classDiagram.addClass(gclass);

            if (librariesImportedFlag)
            {
                // Must restart debug VM to load the imported library:
                project.restartVM();
            }
        }
    }

    /**
     * Prompt the user by the NewClassDialog to create a new non-image class.
     *
     * @param pkg            The package that should contain the new class.
     * @param superClassName The super class's full qualified name.
     */
    public void newNonImageClass(Package pkg, String superClassName)
    {
        new NewClassDialog(this, project.getUnnamedPackage().getDefaultSourceType()).showAndWait()
                .ifPresent(result -> createNewClass(pkg, superClassName, result.className, result.sourceType));
    }

    /**
     * Create a new non-image class in the specified package.
     *
     * @param pkg            The package that should contain the new class.
     * @param superClassName The full qualified name of the super class.
     * @param className      The class's name, which will be created.
     * @param language       The source type of the class, e.g. Java or Stride.
     *
     * @return A class info reference for the class created.
     */
    private GClassNode createNewClass(Package pkg, String superClassName, String className, SourceType language)
    {
        try
        {
            File dir = project.getProjectDir();
            final String extension = language.getExtension();
            File newFile = new File(dir, className + "." + extension);
            String templateFileName = getNormalTemplateFileName(language);
            GreenfootUtilDelegateIDE.getInstance().createSkeleton(className, superClassName, newFile,
                    templateFileName, project.getProjectCharset().toString());
            ClassTarget newClass = new ClassTarget(pkg, className);
            return classDiagram.addClass(newClass);
        }
        catch (IOException ioe)
        {
            Debug.reportError(ioe);
            return null;
        }
    }

    private static String getNormalTemplateFileName(SourceType language)
    {
        return "std" + language + ".tmpl";
    }

    public static String getActorTemplateFileName(SourceType language)
    {
        return "actor" + language + ".tmpl";
    }

    public static String getWorldTemplateFileName(boolean makeDirectSubclassOfWorld, SourceType language)
    {
        if (!makeDirectSubclassOfWorld)
        {
            return "subworld" + language + ".tmpl";
        }
        else
        {
            return "world" + language + ".tmpl";
        }
    }

    /**
     * Show a dialog to ask for details, then make a new subclass of the given class
     * using those details. This is only for classes with images, i.e. Actor/World subclasses.
     *
     * @param parentName The fully qualified name of the parent class.
     */
    public void newImageSubClassOf(String parentName)
    {
        // initialise our image library frame
        new NewImageClassFrame(this, project).showAndWait().ifPresent(classInfo -> {
            GClassNode newClass = createNewClass(project.getUnnamedPackage(), parentName, classInfo.className, classInfo.sourceType);
            // set the image of the class to the selected file
            newClass.getDisplay(this).setImage(new Image(classInfo.imageFile.toURI().toString()));
        });
    }

    /**
     * Show a dialog to ask for details, then make a new subclass of the given class
     * using those details.
     */
    public void newSubClassOf(String fullyQualifiedName)
    {
        ClassTarget classTarget = classDiagram.getSelectedClassTarget();
        Package pkg = classTarget.getPackage();

        boolean imageClass = false;
        Class<?> cls = pkg.loadClass(classTarget.getQualifiedName());
        while (cls !=null)
        {
            if (cls.getCanonicalName().equals("greenfoot.World") || cls.getCanonicalName().equals("greenfoot.Actor") )
            {
                imageClass = true;

            }
            cls = cls.getSuperclass();
        }

        if (imageClass)
        {
            newImageSubClassOf(fullyQualifiedName);
        }
        else
        {
            newNonImageClass(pkg, fullyQualifiedName);
        }
    }


    /**
     * Gets a Reflective for the Actor class.
     */
    private Reflective getActorReflective()
    {
        return new JavaReflective(project.loadClass("greenfoot.Actor"));
    }

    /**
     * Gets a Reflective for the World class.
     */
    private Reflective getWorldReflective()
    {
        return new JavaReflective(project.loadClass("greenfoot.World"));
    }

    /**
     * An InvokeListener which also records the invocation for save-the-world purposes
     */
    private class RecordInvoke implements InvokeListener
    {
        private final DebuggerObject target;

        public RecordInvoke(DebuggerObject target)
        {
            this.target = target;
        }

        @Override
        public void executeMethod(MethodView mv)
        {
            PkgMgrFrame pmf = PkgMgrFrame.findFrame(project.getUnnamedPackage());

            // We must put the object on the bench so that it has a name on the debug VM
            // side.  Without a name, you can't call a method on it using the BlueJ workers.
            // Also, the object bench gets cleared on compile, so that takes care of clean-up:
            String objInstanceName = pmf.putObjectOnBench(target.getClassName().toLowerCase(), target, target.getGenType(), null, null);

            ResultWatcher watcher = new ObjectResultWatcher(target, objInstanceName, project.getUnnamedPackage(), pmf, mv) {
                @Override
                protected void addInteraction(InvokerRecord ir)
                {
                    saveTheWorldRecorder.callActorOrWorldMethod(target, mv.getMethod(), ir.getArgumentValues(), mv.getParamTypes(false));
                }
            };
            if (pmf.checkDebuggerState()) {
                Invoker invoker = new Invoker(pmf, mv, objInstanceName, target, watcher);
                invoker.invokeInteractive();
            }

        }

        @Override
        public void callConstructor(ConstructorView cv)
        {
            //We are not used for constructors, so this won't get called.
        }
    }

    /**
     * Opens a browser tab in the editor showing the given URL
     */
    public void openBrowser(String url)
    {
        project.getDefaultFXTabbedEditor().openWebViewTab(url);
    }

    /**
     * Called when a class has been modified, to notify us that the .class
     * files are now out of date.
     */
    public void classModified()
    {
        pendingCommands.add(new Command(COMMAND_DISCARD_WORLD));
        instantiateWorldAfterDiscarded = isFocused();
        stateProperty.set(State.UNCOMPILED);
    }
}
