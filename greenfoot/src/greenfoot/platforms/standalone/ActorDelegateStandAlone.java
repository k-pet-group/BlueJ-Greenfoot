package greenfoot.platforms.standalone;

import greenfoot.ActorVisitor;
import greenfoot.GreenfootImage;
import greenfoot.World;
import greenfoot.core.ProjectProperties;
import greenfoot.platforms.ActorDelegate;

/**
 * Delegate for the Actor when it is running in a stand alone project created by
 * the "export" functionality in Greenfoot.
 * 
 * @author Poul Henriksen <polle@polle.org>
 * 
 */
public class ActorDelegateStandAlone
    implements ActorDelegate
{
    private static ActorDelegateStandAlone instance = new ActorDelegateStandAlone();
    private World world;
    private ProjectProperties properties;
    
    /**
     * Register this class as the delegate for Actor.
     *
     */
    public static void setupAsActorDelegate()
    {
        ActorVisitor.setDelegate(instance);
    }

    public static void initProperties(ProjectProperties properties)
    {
        instance.properties = properties;
    }

    public static void initWorld(World world)
    {
        instance.world = world;        
    }
    

    public GreenfootImage getImage(String name)
    {
        return properties.getImage(name);
    }
}
