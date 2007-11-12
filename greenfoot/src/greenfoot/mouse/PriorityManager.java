package greenfoot.mouse;

import java.awt.event.MouseEvent;

/**
 * This class resolves the priorities of mouse events for the MouseManager.
 * <p>
 *
 * Priorities with highest priority first::
 * <ul>
 * <li> dragEnd </li>
 * <li> click </li>
 * <li> press </li>
 * <li> drag </li>
 * <li> move </li>
 * </ul>
 * 
 * If several of the same type of event happens, then the last one is used.
 * <p>
 * If two buttons are pressed at the same time, the behaviour is
 * undefined. Maybe we should define it so that button1 always have higher
 * priority than button2 and button2 always higher than button3. But not
 * necessarily documenting this to the user.
 * 
 * @author Poul Henriksen
 * 
 */
public class PriorityManager
{
    /**
     * Returns true if the new mouse event has higher or equal priority than the current.
     * @param newEven
     * @param currentData
     * @return
     */
    public static boolean isHigherPriority(MouseEvent newEvent, MouseEventData currentData)
    {
        int currentPriority = getPriority(currentData);
        int newPriority = getPriority(newEvent);
        return newPriority <= currentPriority;
    }
    /**
     * Priority 0 is highest.
     * @param event
     * @return
     */
    private static int getPriority(MouseEvent event)
    {
        if(event.getID() == MouseEvent.MOUSE_RELEASED) {
            return 0;
        }
        else if(event.getID() == MouseEvent.MOUSE_CLICKED) {
            return 1;
        }
        else if(event.getID() == MouseEvent.MOUSE_PRESSED) {
            return 2;
        }
        else if(event.getID() == MouseEvent.MOUSE_DRAGGED) {
            return 3;
        }
        else if(event.getID() == MouseEvent.MOUSE_MOVED) {
            return 4;
        }
        else {
            return Integer.MAX_VALUE;
        }
    }

    private static int getPriority(MouseEventData data)
    {
        if(data.isMouseDragEnded()) {
            return 0;
        }
        else if(data.isMouseClicked()) {
            return 1;
        }
        else if(data.isMousePressed()) {
            return 2;
        }
        else if(data.isMouseDragged()) {
            return 3;
        }
        else if(data.isMouseMoved()) {
            return 4;
        }
        else {
            return Integer.MAX_VALUE;
        }
    }
}
