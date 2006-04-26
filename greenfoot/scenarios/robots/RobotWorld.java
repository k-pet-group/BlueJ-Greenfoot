import greenfoot.World;
import greenfoot.Actor;
import greenfoot.GreenfootImage;

import java.awt.Color;
import java.util.Collection;
import java.io.*;
import java.net.MalformedURLException;

/**
 * A world for the robots
 */
public class RobotWorld extends World
{

    private static int cellSize = 50;

    /**
     * Creates a new world with 16x8 cells and with a cell size of 50x50 pixels
     */
    public RobotWorld()
    {
        super(16, 8, cellSize);
        setBackground("images/road.gif");
    }


    public void populate()
    {
        Robot robot = new Harvester();
        addObject(robot, 3, 1);

        Beeper beeper1 = new Beeper();
        addObject(beeper1, 3, 6);

        Beeper beeper2 = new Beeper();
        addObject(beeper2, 3, 4);

        Beeper beeper3 = new Beeper();
        addObject(beeper3, 7, 7);

        Beeper beeper4 = new Beeper();
        addObject(beeper4, 5, 7);

    }

    public int getGridWidth()
    {
        return getWidth();
    }

    public int getGridHeight()
    {
        return getHeight();
    }

    public Collection getObjectsAtCell(int x, int y, Class cls)
    {
        //we assume that it is possible to get an object in a cell by looking
        // at the middle pixel in the cell
        return getObjectsAt(x, y, cls);
    }
}