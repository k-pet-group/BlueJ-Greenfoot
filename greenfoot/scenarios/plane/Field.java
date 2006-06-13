import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)

import java.awt.Color;

/**
 * A green field.
 * 
 * @author Michael Kolling
 * @version 1.0.1
 */
public class Field extends World
{
    /**
     * Creates a new field
     */
    public Field() {
        super(600,600,1);
        getBackground().setColor(new Color(50,150,50));
        getBackground().fill();
    }
}