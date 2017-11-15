package greenfoot.guifx;

import bluej.pkgmgr.Project;
import bluej.utility.Debug;
import javafx.animation.AnimationTimer;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;

/**
 * Greenfoot's main window: a JavaFX replacement for GreenfootFrame which lives on the server VM.
 */
public class GreenfootStage extends Stage
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
    public GreenfootStage(Project project, FileChannel sharedMemoryLock, MappedByteBuffer sharedMemoryByte)
    {
        ImageView imageView = new ImageView();
        BorderPane imageViewWrapper = new BorderPane(imageView);
        imageViewWrapper.setMinWidth(200);
        imageViewWrapper.setMinHeight(200);
        Button runButton = new Button("Run");
        Node buttonAndSpeedPanel = new HBox(runButton);
        List<Command> pendingCommands = new ArrayList<>();
        runButton.setOnAction(e -> {
            pendingCommands.add(new Command(COMMAND_RUN));
        });
        BorderPane root = new BorderPane(imageViewWrapper, null, new ClassDiagram(project), buttonAndSpeedPanel, null);
        setScene(new Scene(root));

        setupWorldDrawingAndEvents(sharedMemoryLock, sharedMemoryByte, imageView, pendingCommands);
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
                eventType = KEY_DOWN;
            }
            else if (e.getEventType() == KeyEvent.KEY_RELEASED)
            {
                eventType = KEY_UP;
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

        getScene().addEventFilter(MouseEvent.ANY, e -> {
            int eventType;
            if (e.getEventType() == MouseEvent.MOUSE_CLICKED)
            {
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
}
