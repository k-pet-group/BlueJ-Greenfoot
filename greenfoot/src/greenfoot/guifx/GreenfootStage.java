package greenfoot.guifx;

import bluej.BlueJEvent;
import bluej.BlueJEventListener;
import bluej.Config;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerResult;
import bluej.debugger.gentype.Reflective;
import bluej.debugmgr.ExecutionEvent;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.pkgmgr.Project;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.Target;
import bluej.utility.Debug;
import bluej.utility.JavaReflective;
import bluej.utility.javafx.JavaFXUtil;
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
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import rmiextension.GreenfootDebugHandler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Greenfoot's main window: a JavaFX replacement for GreenfootFrame which lives on the server VM.
 */
public class GreenfootStage extends Stage implements BlueJEventListener
{
    // These are the constants passed in the shared memory between processes,
    // hence they cannot be enums.  They are not persisted anywhere, so can
    // be changed at will (as long as they don't overlap).
    public static final int KEY_DOWN = 1;
    public static final int KEY_UP = 2;
    public static final int KEY_TYPED = 3;
    public static final int FOCUS_LOST = 4;
    public static final int MOUSE_CLICKED = 5;
    public static final int MOUSE_PRESSED = 6;
    public static final int MOUSE_DRAGGED = 7;
    public static final int MOUSE_RELEASED = 8;
    public static final int MOUSE_MOVED = 9;

    public static final int COMMAND_RUN = 1;

    private final Project project;
    // The glass pane used to show a new actor while it is being placed:
    private final Pane glassPane;
    // Details of the new actor while it is being placed (null otherwise):
    private final ObjectProperty<NewActor> newActorProperty = new SimpleObjectProperty<>(null);
    private final BorderPane worldView;
    private final ClassDiagram classDiagram;
    private int nextPickId = 1;
    private int curPickRequest;
    private Point2D curPickPoint;

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
        // The object reference if the actor has already been created:
        private final DebuggerObject debugVMActorReference;
        // The type name if the actor has not been constructed:
        private final String typeName;

        private Region makePreviewNode(ImageView imageView)
        {
            ImageView cannotDropIcon = new ImageView(this.getClass().getClassLoader().getResource("noParking.png").toExternalForm());
            cannotDropIcon.visibleProperty().bind(cannotDrop);
            StackPane.setAlignment(cannotDropIcon, Pos.TOP_RIGHT);
            return new StackPane(imageView, cannotDropIcon);
        }

        public NewActor(ImageView imageView, DebuggerObject debugVMActorReference)
        {
            this.previewNode = makePreviewNode(imageView);
            this.debugVMActorReference = debugVMActorReference;
            this.typeName = null;
        }

        public NewActor(ImageView imageView, String typeName)
        {
            this.previewNode = makePreviewNode(imageView);
            this.debugVMActorReference = null;
            this.typeName = typeName;
        }
    }

    /**
     * A key event.  The eventType is one of KEY_DOWN etc from the
     * integer constants above, and extraInfo is the key code
     * (using the JavaFX KeyCode enum's ordinal method).
     */
    private static class KeyEventInfo
    {
        public final int eventType;
        public final int extraInfo;

        public KeyEventInfo(int eventType, int extraInfo)
        {
            this.eventType = eventType;
            this.extraInfo = extraInfo;
        }
    }

    /**
     * A mouse event.  They eventType is one of MOUSE_CLICKED etc
     * from the integer constants above, and extraInfo is an array
     * of information: X pos, Y pos, button index, click count
     */
    private static class MouseEventInfo
    {
        public final int eventType;
        public final int[] extraInfo;

        public MouseEventInfo(int eventType, int... extraInfo)
        {
            this.eventType = eventType;
            this.extraInfo = extraInfo;
        }
    }

    /**
     * A command from the server VM to the debug VM, such
     * as Run, Reset, etc
     */
    private static class Command
    {
        public final int commandType;
        public final int[] extraInfo;

        private Command(int commandType, int... extraInfo)
        {
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
        greenfootDebugHandler.setPickListener(this::pickResults);

        ImageView imageView = new ImageView();
        worldView = new BorderPane(imageView);
        worldView.setMinWidth(200);
        worldView.setMinHeight(200);
        Button runButton = new Button("Run");
        Node buttonAndSpeedPanel = new HBox(runButton);
        List<Command> pendingCommands = new ArrayList<>();
        runButton.setOnAction(e -> {
            pendingCommands.add(new Command(COMMAND_RUN));
        });
        classDiagram = new ClassDiagram(project);
        BorderPane root = new BorderPane(worldView, null, classDiagram, buttonAndSpeedPanel, null);
        glassPane = new Pane();
        glassPane.setMouseTransparent(true);
        StackPane stackPane = new StackPane(root, glassPane);
        setupMouseForPlacingNewActor(stackPane);
        setScene(new Scene(stackPane));

        setupWorldDrawingAndEvents(sharedMemoryLock, sharedMemoryByte, imageView, pendingCommands);
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
                    if (newActorProperty.get().debugVMActorReference != null)
                    {
                        // Place the already-constructed actor:
                        actor = newActorProperty.get().debugVMActorReference;
                        // Can't place same instance twice:
                        newActorProperty.set(null);
                    }
                    else
                    {
                        // Must be shift-clicking; will need to make a new instance:
                        DebuggerResult result = project.getDebugger().instantiateClass(newActorProperty.get().typeName).get();
                        if (result.getResultObject() != null)
                        {
                            actor = result.getResultObject();
                        }
                    }
                    if (actor != null)
                    {
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
     * @param imageView The ImageView to draw the remote-sent image to
     */
    private void setupWorldDrawingAndEvents(FileChannel sharedMemoryLock, MappedByteBuffer sharedMemoryByte, ImageView imageView, List<Command> pendingCommands)
    {
        IntBuffer sharedMemory = sharedMemoryByte.asIntBuffer();

        List<KeyEventInfo> keyEvents = new ArrayList<>();
        List<MouseEventInfo> mouseEvents = new ArrayList<>();


        getScene().addEventFilter(KeyEvent.ANY, e -> {
            int eventType;
            if (e.getEventType() == KeyEvent.KEY_PRESSED)
            {
                if (e.getCode() == KeyCode.ESCAPE && newActorProperty.get() != null)
                {
                    newActorProperty.set(null);
                    return;
                }
                if (e.getCode() == KeyCode.SHIFT && newActorProperty.get() == null && classDiagram.getSelectedClass() != null)
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

                if (e.getCode() == KeyCode.SHIFT && newActorProperty.get() != null && newActorProperty.get().debugVMActorReference == null)
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

            keyEvents.add(new KeyEventInfo(eventType, e.getCode().ordinal()));
            e.consume();
        });

        worldView.addEventFilter(MouseEvent.ANY, e -> {
            int eventType;
            if (e.getEventType() == MouseEvent.MOUSE_CLICKED)
            {
                boolean isRunning = false; // TODO actually have this state correctly in future.
                if (e.getButton() == MouseButton.SECONDARY && !isRunning)
                {
                    worldRightClick(e.getX(), e.getY());
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
            }
            else if (e.getEventType() == MouseEvent.MOUSE_DRAGGED)
            {
                eventType = MOUSE_DRAGGED;
            }
            else if (e.getEventType() == MouseEvent.MOUSE_MOVED)
            {
                eventType = MOUSE_MOVED;
            }
            else
            {
                return;
            }
            mouseEvents.add(new MouseEventInfo(eventType, (int)e.getX(), (int)e.getY(), e.getButton().ordinal(), e.getClickCount()));
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
                        lastSeq = seq;
                        sharedMemory.put(1, -seq);
                        sharedMemory.position(2);
                        int width = sharedMemory.get();
                        int height = sharedMemory.get();
                        if (img == null || img.getWidth() != width || img.getHeight() != height)
                        {
                            img = new WritableImage(width == 0 ? 1 : width, height == 0 ? 1 : height);
                            sizeToScene = true;
                        }
                        sharedMemoryByte.position(sharedMemory.position() * 4);
                        img.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getByteBgraPreInstance(), sharedMemoryByte, width * 4);
                        imageView.setImage(img);
                        writeKeyboardAndMouseEvents();
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
                    // Put size, including command type:
                    sharedMemory.put(pendingCommand.extraInfo.length + 1);
                    sharedMemory.put(pendingCommand.commandType);
                    sharedMemory.put(pendingCommand.extraInfo);
                }
                pendingCommands.clear();
            }

            /**
             * Writes keyboard and mouse events to the shared memory.
             */
            private void writeKeyboardAndMouseEvents()
            {
                sharedMemory.position(2);
                sharedMemory.put(keyEvents.size());
                for (KeyEventInfo event : keyEvents)
                {
                    sharedMemory.put(event.eventType);
                    sharedMemory.put(event.extraInfo);
                }
                keyEvents.clear();
                sharedMemory.put(mouseEvents.size());
                for (MouseEventInfo mouseEvent : mouseEvents)
                {
                    sharedMemory.put(mouseEvent.eventType);
                    sharedMemory.put(mouseEvent.extraInfo);
                }
                mouseEvents.clear();
            }
        }.start();
    }

    /**
     * Handle a right-click on the world at the given world coordinates.
     */
    private void worldRightClick(double x, double y)
    {
        // Bit hacky to pass positions as strings, but mirroring the values as integers
        // would have taken a lot of code changes to route through to VMReference:
        DebuggerObject xObject = project.getDebugger().getMirror("" + (int) x);
        DebuggerObject yObject = project.getDebugger().getMirror("" + (int) y);
        int thisPickId = nextPickId++;
        DebuggerObject pickIdObject = project.getDebugger().getMirror("" + thisPickId);
        // One pick at a time only:
        curPickRequest = thisPickId;
        curPickPoint = new Point2D(x, y);

        // Need to find out which actors are at the point:
        project.getDebugger().instantiateClass("greenfoot.core.PickActorHelper", new String[]{"java.lang.String", "java.lang.String", "java.lang.String"}, new DebuggerObject[]{xObject, yObject, pickIdObject});
        // Once that completes, pickResults(..) will be called.
    }

    /**
     * Callback when a pick has completed (i.e. a request to find actors at given position)
     * @param pickId The ID of the pick has requested
     * @param actors The list of actors found.  May be any size.
     */
    public void pickResults(int pickId, List<DebuggerObject> actors)
    {
        if (curPickRequest != pickId)
            return; // Pick has been cancelled by a more recent pick, so ignore

        // If single actor, show simple context menu:
        if (actors.size() == 1)
        {
            ContextMenu contextMenu = new ContextMenu();
            Target target = project.getTarget(actors.get(0).getClassName());
            // Should always be ClassTarget, but check in case:
            if (target instanceof ClassTarget)
            {
                ClassTarget classTarget = (ClassTarget)target;
                ObjectWrapper.createMethodMenuItems(contextMenu.getItems(), project.loadClass(actors.get(0).getClassName()), classTarget, actors.get(0), "", true);
            }
            Point2D screenLocation = worldView.localToScreen(curPickPoint);
            contextMenu.show(worldView, screenLocation.getX(), screenLocation.getY());
        }
        // TODO handle cases for multiple actors, and for zero actors
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
                            newActorProperty.set(new NewActor(imageView, executionEvent.getResultObject()));
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
}
