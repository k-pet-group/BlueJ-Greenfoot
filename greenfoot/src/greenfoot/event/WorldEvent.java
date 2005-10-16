package greenfoot.event;

import greenfoot.GreenfootWorld;

import java.util.EventObject;

public class WorldEvent extends EventObject
{
   
    public WorldEvent(GreenfootWorld world)
    {
        super(world);
    }

}