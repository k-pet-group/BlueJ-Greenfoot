package bluej.editor.stride;

import javafx.scene.Node;

import bluej.stride.generic.FrameCursor;
import bluej.utility.javafx.FXConsumer;
import bluej.utility.javafx.FXRunnable;

/**
 * An interface implemented by ClassFrame/InterfaceFrame, providing methods
 * needed by FrameEditorTab to control the bird's eye view.
 */
public interface BirdseyeManager
{
    /**
     * Gets the graphical Node corresponding to the frame around which the bird's eye view
     * selection rectangle should be drawn
     */
    Node getNodeForRectangle();

    /**
     * Notify about a click at the given scene X/Y.
     *
     * The return value will be non-null if there was a frame, 
     * or null if click wasn't on a frame.  Either way, bird's eye view will have been closed.
     */
    FrameCursor getClickedTarget(double sceneX, double sceneY);

    /**
     * Get the frame cursor to focus after we expand the frame which is currently selected.
     */
    FrameCursor getCursorForCurrent();

    /**
     * Move selection up (in response to up arrow key)
     */
    void up();

    /**
     * Move selection down (in response to down arrow key)
     */
    void down();

    /**
     * Test if clicking at the given position would result in selecting a frame
     */
    boolean canClick(double sceneX, double sceneY);
}
