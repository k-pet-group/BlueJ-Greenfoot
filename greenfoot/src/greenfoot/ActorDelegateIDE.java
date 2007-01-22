package greenfoot;

import greenfoot.core.GreenfootMain;
import greenfoot.core.WorldHandler;

/**
 * Delegate for the Actor when it is running in the Greenfoot IDE.
 * 
 * @author Poul Henriksen <polle@polle.org>
 *
 */
public class ActorDelegateIDE implements ActorDelegate
{
    private static ActorDelegateIDE  instance = new  ActorDelegateIDE();   
    
    /**
     * Register this class as the delegate for Actor.
     *
     */
    public static void setupAsActorDelegate()
    {
        Actor.setDelegate(instance);
    }
    
    public GreenfootImage getImage(String name)
    {
        return GreenfootMain.getProjectProperties().getImage(name);
    }

    public World getWorld()
    {
        return WorldHandler.getInstance().getWorld();
    }

}
