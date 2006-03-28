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
     * Creates a new world with 20x20 cells and with a cell size of 50x50 pixels
     */
    public RobotWorld()
    {
        super(19, 8, cellSize);

      //  GreenfootImage background = new GreenfootImage("road.gif");
        setBackground("road.gif");
       //background.setTiled(true);
    }

/*
 * public static int getCellSize() { return cellSize; }
 */

    public void populate()
    {
        Robot robot = new Harvester();
        robot.setLocation(3, 1);
        addObject(robot);

        Beeper beeper1 = new Beeper();
        beeper1.setLocation(3, 6);
        addObject(beeper1);

        Beeper beeper2 = new Beeper();
        beeper2.setLocation(3, 4);
        addObject(beeper2);

        Beeper beeper3 = new Beeper();
        beeper3.setLocation(7, 7);
        addObject(beeper3);

        Beeper beeper4 = new Beeper();
        beeper4.setLocation(5, 7);
        addObject(beeper4);

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
/*
    public static int toCell(int i)
    {
        return i / getCellSize();
    }

    public static int toPixel(int i)
    {
        return i * getCellSize();
    }*/
}