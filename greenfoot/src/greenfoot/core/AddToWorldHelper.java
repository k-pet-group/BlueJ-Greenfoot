package greenfoot.core;

import greenfoot.Actor;

/**
 * A helper class which is used to add an actor to the world by instantiating it.
 */
public class AddToWorldHelper
{
    public AddToWorldHelper(Object actor, String x, String y)
    {
        WorldHandler.getInstance().addActorAtPixel((Actor)actor, Integer.parseInt(x), Integer.parseInt(y));
    }
}
