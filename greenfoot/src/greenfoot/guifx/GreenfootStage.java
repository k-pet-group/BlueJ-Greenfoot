/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2017  Poul Henriksen and Michael Kolling 
 
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
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.Target;
import bluej.testmgr.record.InvokerRecord;
import bluej.testmgr.record.ObjectInspectInvokerRecord;
import bluej.utility.Debug;
import bluej.utility.JavaReflective;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformConsumer;
import bluej.utility.javafx.FXPlatformRunnable;
import bluej.utility.javafx.JavaFXUtil;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import greenfoot.record.GreenfootRecorder;
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
import java.util.List;
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
    
    public static boolean isKeyEvent(int event)
    {
        return event >= KEY_DOWN && event <= KEY_TYPED;
    }

    /**
     * Mouse events.  Followed by four integers:
     * X pos, Y pos, button index, click count
     */
    public static final int MOUSE_CLICKED = 11;
    public static final int MOUSE_PRESSED = 12;
    public static final int MOUSE_DRAGGED = 13;
    public static final int MOUSE_RELEASED = 14;
    public static final int MOUSE_MOVED = 15;
    public static boolean isMouseEvent(int event)
    {
        return event >= MOUSE_CLICKED && event <= MOUSE_MOVED;
    }

    /**
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
    public static final int COMMAND_RESET = 26;
    // Followed by one integer per character in String answer.
    public static final int COMMAND_ANSWERED = 27;
    // Followed by an integer count of key size, then that many integer codepoints,
    // Then same again for value.  If value count is -1,
    // that means value is null (and thus was removed)
    public static final int COMMAND_PROPERTY_CHANGED = 28;

    private final Project project;
    // The glass pane used to show a new actor while it is being placed:
    private final Pane glassPane;
    // Details of the new actor while it is being placed (null otherwise):
    private final ObjectProperty<NewActor> newActorProperty = new SimpleObjectProperty<>(null);
    private final WorldDisplay worldView;
    private final ClassDiagram classDiagram;
    // The currently-showing context menu, or null if none
    private ContextMenu contextMenu;

    private final Button actButton;
    private final Button runButton;
    private final Button resetButton;

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
     * Creates a GreenfootStage which receives a world image to draw from the
     * given shared memory buffer, protected by the given lock.
     * @param sharedMemoryLock The lock to claim before accessing sharedMemoryByte
     * @param sharedMemoryByte The shared memory buffer used to communicate with the debug VM
     */
    public GreenfootStage(Project project, GreenfootDebugHandler greenfootDebugHandler, FileChannel sharedMemoryLock, MappedByteBuffer sharedMemoryByte)
    {
        this.project = project;
        BlueJEvent.addListener(this);
        project.getUnnamedPackage().addCompileObserver(this);
        greenfootDebugHandler.setPickListener(this::pickResults);
        greenfootDebugHandler.setSimulationListener(this);
        this.saveTheWorldRecorder = new GreenfootRecorder();
        greenfootDebugHandler.setGreenfootRecorder(saveTheWorldRecorder);

        worldView = new WorldDisplay();
        actButton = new Button(Config.getString("run.once"));
        runButton = new Button(Config.getString("controls.run.button"));
        resetButton = new Button(Config.getString("reset.world"));
        Node buttonAndSpeedPanel = new HBox(actButton, runButton, resetButton);
        List<Command> pendingCommands = new ArrayList<>();
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
                pendingCommands.add(new Command(COMMAND_RESET));
                stateProperty.set(State.UNCOMPILED);
            }
        });
        classDiagram = new ClassDiagram(project);
        BorderPane root = new BorderPane(worldView, makeMenu(pendingCommands), classDiagram, buttonAndSpeedPanel, null);
        glassPane = new Pane();
        glassPane.setMouseTransparent(true);
        StackPane stackPane = new StackPane(root, glassPane);
        setupMouseForPlacingNewActor(stackPane);
        setScene(new Scene(stackPane));

        setupWorldDrawingAndEvents(sharedMemoryLock, sharedMemoryByte, worldView::setImage, pendingCommands);
        loadAndMirrorProperties(pendingCommands);
        // We send a reset to make a new world after the project properties have been sent across:
        pendingCommands.add(new Command(COMMAND_RESET));
        JavaFXUtil.addChangeListenerPlatform(stateProperty, this::updateGUIState);
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
        return new MenuBar(
            new Menu(Config.getString("menu.controls"), null,
                makeMenuItem("run.once", new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN), () -> act(pendingCommands))
            )
        );
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
        MenuItem menuItem = new MenuItem(Config.getString(nameKey));
        if (accelerator != null)
        {
            menuItem.setAccelerator(accelerator);
        }
        menuItem.setOnAction(e -> action.run());
        return menuItem;
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
        double[] lastMousePos = new double[2];
        stackPane.setOnMouseMoved(e -> {
            lastMousePos[0] = e.getX();
            lastMousePos[1] = e.getY();
            if (newActorProperty.get() != null)
            {
                // TranslateX/Y seems to have a bit less lag than LayoutX/Y:
                newActorProperty.get().previewNode.setTranslateX(e.getX() - newActorProperty.get().previewNode.getWidth() / 2.0);
                newActorProperty.get().previewNode.setTranslateY(e.getY() - newActorProperty.get().previewNode.getHeight() / 2.0);

                Point2D worldDest = worldView.parentToLocal(e.getX(), e.getY());
                newActorProperty.get().cannotDrop.set(!worldView.contains(worldDest));
            }
        });
        stackPane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1 && newActorProperty.get() != null)
            {
                Point2D dest = worldView.parentToLocal(e.getX(), e.getY());
                if (worldView.contains(dest))
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

                newVal.previewNode.setTranslateX(lastMousePos[0] - newVal.previewNode.getWidth() / 2.0);
                newVal.previewNode.setTranslateY(lastMousePos[1] - newVal.previewNode.getHeight() / 2.0);
                Point2D dest = worldView.parentToLocal(lastMousePos[0], lastMousePos[1]);
                newVal.cannotDrop.set(!worldView.contains(dest));
            }
        });
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
            if (worldView.isAsking())
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
                if (e.getCode() == KeyCode.SHIFT && newActorProperty.get() == null && classDiagram.getSelectedClass() != null && paused)
                {
                    // Holding shift, so show actor preview if it is an actor with no-arg constructor:
                    Reflective type = classDiagram.getSelectedClass().getTypeReflective();
                    if (getActorReflective().isAssignableFrom(type) && type.getDeclaredConstructors().stream().anyMatch(c -> c.getParamTypes().isEmpty() && !Modifier.isPrivate(c.getModifiers())))
                    {
                        newActorProperty.set(new NewActor(getImageViewForClass(type), classDiagram.getSelectedClass().getBaseName()));
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

        worldView.setOnContextMenuRequested(e -> {
            boolean paused = stateProperty.get() == State.PAUSED;
            if (paused)
            {
                // We don't want to block GUI while waiting for pick: 
                Utility.runBackground(() -> pickRequest(e.getX(), e.getY(), PickType.CONTEXT_MENU));
            }
        });
        worldView.addEventFilter(MouseEvent.ANY, e -> {
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
                        if (img == null || img.getWidth() != width || img.getHeight() != height)
                        {
                            img = new WritableImage(width == 0 ? 1 : width, height == 0 ? 1 : height);
                            sizeToScene = true;
                        }
                        sharedMemoryByte.position(sharedMemory.position() * 4);
                        img.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getByteBgraPreInstance(), sharedMemoryByte, width * 4);
                        setImage.accept(img);
                        // Have to move sharedMemory position manually because
                        // the sharedMemory buffer doesn't share position with sharedMemoryByte buffer:
                        sharedMemory.position(sharedMemory.position() + width * height);
                        int lastAckCommand = sharedMemory.get();
                        // Get rid of all commands that the client has confirmed it has seen:
                        if (lastAckCommand != -1)
                        {
                            pendingCommands.removeIf(c -> c.commandSequence <= lastAckCommand);
                        }
                        
                        int askId = sharedMemory.get();
                        if (askId >= 0 && askId > lastAnswer)
                        {
                            // Length followed by codepoints for the prompt string:
                            int askLength = sharedMemory.get();
                            int[] promptCodepoints = new int[askLength];
                            sharedMemory.get(promptCodepoints);
                            
                            // Tell worldView to ask:
                            worldView.ensureAsking(new String(promptCodepoints, 0, promptCodepoints.length), (String s) -> {
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
                            ClassTarget classTarget = (ClassTarget) target;
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
                    Point2D screenLocation = worldView.localToScreen(curPickPoint);
                    contextMenu.show(worldView, screenLocation.getX(), screenLocation.getY());
                }
                else
                {
                    Target target = project.getTarget(world.getClassName());
                    // Should always be ClassTarget, but check in case:
                    if (target instanceof ClassTarget)
                    {
                        ClassTarget classTarget = (ClassTarget) target;
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

                        Point2D screenLocation = worldView.localToScreen(curPickPoint);
                        contextMenu.show(worldView, screenLocation.getX(), screenLocation.getY());
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
        worldView.greyOutWorld();
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
}
