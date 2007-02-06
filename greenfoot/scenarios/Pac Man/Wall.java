import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.awt.Color;

/**
 * Wall Class
 * @author Joseph Lenton
 * @date 06/02/07
 * 
 * A wall is simply to prevent both PacMan and Ghosts
 * instances from moving across it's space.
 * PacMan and Ghosts look for Wall instances at where they
 * are going to move to, and if they find one they
 * will not move there.
 */
public class Wall extends Actor
{
    /**
     * 
     */
    public Wall()
    {
        
    }
    
    /**
     * Be the Wall.
     */
    public void act()
    {
        // I am a wall, therefore I am.
    }
}