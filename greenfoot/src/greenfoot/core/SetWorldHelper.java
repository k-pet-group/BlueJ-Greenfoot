package greenfoot.core;

import greenfoot.Greenfoot;
import greenfoot.World;

/**
 * A helper class instantiated on the debug VM by the
 * server VM in order to set a world.
 */
public class SetWorldHelper
{
    public SetWorldHelper(Object param)
    {
        World w = (World)param;
        Simulation.getInstance().runLater(() -> {
            WorldHandler.getInstance().setWorld(w, false);
            WorldHandler.getInstance().finishedInitialisingWorld();
        });
    }
}
