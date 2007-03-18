package greenfoot.platforms.ide;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.GreenfootImage;
import greenfoot.World;
import greenfoot.core.GreenfootMain;
import greenfoot.core.WorldHandler;
import greenfoot.platforms.ActorDelegate;

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
        ActorVisitor.setDelegate(instance);
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
