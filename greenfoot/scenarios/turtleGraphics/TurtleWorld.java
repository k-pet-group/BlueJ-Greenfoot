import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

import java.awt.Color;

/**
 * A white world that turtles can draw on.
 *  
 * @author Poul Henriksen
 * @version 1.0.1
 */
public class TurtleWorld extends World
{
    /**
     * Creates a new world with 800x600 cells and
     * with a cell size of 1x1 pixels
     */
    public TurtleWorld() {
        super(800,600, 1);
        getBackground().setColor(Color.WHITE);
        getBackground().fill();
    }
}