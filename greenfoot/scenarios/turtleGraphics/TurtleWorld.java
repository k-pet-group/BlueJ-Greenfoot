import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;

import java.awt.*;

public class TurtleWorld extends GreenfootWorld
{
    /**
     * Creates a new world with 800x600 cells and
     * with a cell size of 1x1 pixels
     */
    public TurtleWorld() {
        super(800,600, 1);
        getBackground().fill(Color.WHITE);
    }
}