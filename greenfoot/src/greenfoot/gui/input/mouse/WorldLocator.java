package greenfoot.gui.input.mouse;

import java.awt.event.MouseEvent;

import greenfoot.Actor;


/**
 * Interface for locating actors and coordinates in the world.
 * 
 * @author Poul Henriksen
 *
 */
public interface WorldLocator
{
    /**
     * Gets the top most actor at the given location.
     * @param e TODO
     * @return The top most actor, or null if no actor.
     */
    public Actor getTopMostActorAt(MouseEvent e);

    /**
     * Translates the coordinates from the given source component into some other coordinate system.
     * @param e The event from which the x-coordinate should be translated
     * @return The new x-coordinate
     */
    public int getTranslatedX(MouseEvent e);
    

    /**
     * Translates the coordinates from the given source component into some other coordinate system.
     * @param e The event from which the y-coordinate should be translated
     * @return The new y-coordinate
     */
    public int getTranslatedY(MouseEvent e);

}
