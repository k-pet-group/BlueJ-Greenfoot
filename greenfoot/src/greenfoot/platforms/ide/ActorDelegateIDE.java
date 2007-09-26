package greenfoot.platforms.ide;

import greenfoot.ActorVisitor;
import greenfoot.GreenfootImage;
import greenfoot.core.GProject;
import greenfoot.platforms.ActorDelegate;

/**
 * Delegate for the Actor when it is running in the Greenfoot IDE.
 * 
 * @author Poul Henriksen <polle@polle.org>
 *
 */
public class ActorDelegateIDE implements ActorDelegate
{
    private GProject project;
    
    private ActorDelegateIDE(GProject project)
    {
    	this.project = project;
    }
    
    /**
     * Register this class as the delegate for Actor.
     */
    public static void setupAsActorDelegate(GProject project)
    {
        ActorVisitor.setDelegate(new ActorDelegateIDE(project));
    }
    
    public GreenfootImage getImage(String name)
    {
        return project.getProjectProperties().getImage(name);
    }
}
