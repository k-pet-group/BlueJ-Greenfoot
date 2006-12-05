import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

import java.awt.Color;

/**
 * A color that can be put into the world. Because of performance issues these
 * objects are not added to the world. Instead the colors are painted directly 
 * on the background image of the world.
 * 
 * The objects are still used by the Agent and Pirate class though.
 * 
 * @author Poul Henriksen
 * @version 0.5
 */
public abstract class AbstractColor extends Actor
{

    public AbstractColor() {
        GreenfootImage image = new GreenfootImage(5, 5);
        image.setColor(getColor());
        image.fill();
        setImage(image);
    }
    
    public void addedToWorld(World world) {
        GreenfootImage image = new GreenfootImage(getWorld().getCellSize(), getWorld().getCellSize());
        image.setColor(getColor());
        image.fill();
        setImage(image);
    }
    
    public void act()
    {
    }

    public abstract Color getColor();
    
    
}