package greenfoot.core;

import greenfoot.Actor;

/**
 * A helper class which is used to remove an actor from the world.
 */
public class RemoveFromWorldHelper
{
    public RemoveFromWorldHelper(Object actor)
    {
        Simulation.getInstance().runLater(() -> {
            WorldHandler.getInstance().getWorld().removeObject((Actor) actor);
        });
    }
}
