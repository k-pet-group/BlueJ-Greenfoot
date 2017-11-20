package greenfoot.core;

import bluej.utility.Debug;
import greenfoot.Actor;

/**
 * A helper class which can be instantiated to fetch a reference to the
 * actor at a particular mouse location, where the user has left/right-clicked
 * in the interface.
 */
public class PickActorHelper
{
    // Special fields examined by GreenfootDebugHandler.  Do not rename
    // without also renaming there:
    public Actor[] picks;
    public int pickId;

    /**
     * Finds an actor at the given location.
     *
     * @param sx The x position in world coordinates (will be an integer)
     * @param sy The y position in world coordinates (will be an integer)
     * @param spickId The pick request ID
     */
    public PickActorHelper(String sx, String sy, String spickId)
    {
        int x = Integer.parseInt(sx);
        int y = Integer.parseInt(sy);
        int pickId = Integer.parseInt(spickId);

        Simulation.getInstance().runLater(() -> {
            // The fields must be up to date and valid at the point we call picked():
            this.picks = WorldHandler.getInstance().getObjects(x, y).toArray(new Actor[0]);
            this.pickId = pickId;
            picked();
        });
    }

    /**
     * A special method which will have a breakpoint set by GreenfootDebugHandler.  Do
     * not remove/inline/rename without also editing that class.
     */
    public void picked()
    {
        // Used as a special breakpoint signifier so that JDI can be used to inspect the picks field
    }
}
