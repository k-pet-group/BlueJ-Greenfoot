// This class is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation.
//
// This class is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import greenfoot.Actor;             
import greenfoot.World;             
import greenfoot.GreenfootImage;
import greenfoot.Greenfoot; 

/**
 * A fish actor in the Greenfoot world. It behaves just like the fish in the
 * AP Marine Biology Simulation. The code in that simulation was used as a 
 * base to write this class.
 *
 * @author Cecilia Vargas
 * @version 28 July 2006
 */

public class Fish extends Actor  
{
    private static int nextAvailableID = 1;  

    private int        id;            // unique ID for this fish
    private Color      color;         // color of this fish
    private BoundedEnv environment;   // world in which fish lives

    /** 
     *  Constructs a fish at the location where the user placed it with 
     *  the cursor. The fish is assigned a random direction and color.
     */
    public Fish() 
    {
        color = randomColor();
        Direction dir = Direction.randomDirection();
        setRotation(dir.inGreenfootDegrees());
    } 

    /**
     * Assigns the fish's id and world, and scales the image to cell size.
     */   
    protected void addedToWorld(World world)
    {
        id = nextAvailableID;
        nextAvailableID++;
        
        //Scale image to cell size.
        getImage().scale(world.getCellSize() - 2, world.getCellSize() - 2);
        
        environment = (BoundedEnv) world;
     }
    
    /**
     * Overrides getImage so that setting the color will change the image.
     * @return    the Greenfoot image of this fish.
     */
    public GreenfootImage getImage()
    {
        return ColoredImage.getImage(this, super.getImage(), getColor());
    }

    /** 
     * Returns this fish's ID.
     * @return    the unique ID of this fish
     */
    public int getId()
    {
        return id;
    }

    /** 
     * Returns this fish's color.
     * @return    the color of the fish.
     */
    public Color getColor()
    {
        return color;
    }

    /** 
     * Returns this fish's environment.
     * @return    the world in which this fish lives.
     */
    public BoundedEnv getEnvironment()
    {
        return environment;
    }
    
    /** 
     * Returns fish's direction. North is 0 degrees, East is 90, etc.
     * @return      direction in which this fish is facing
     */
    public Direction getDirection()
    {
        //Convert Greenfoot's rotation to MBS direction
        int degrees = getRotation();
        
        if ( degrees >= 270) {
           return new Direction(degrees - 270);
        } 
        else {
           return new Direction(degrees + 90); 
        }
    }

    /** 
     * Returns a string with some information about this fish.
     * @return  a string with the fish's id, cell, and direction
     */
    public String toString()
    {
        return getId() + "( row " + getY() + ", col " + getX() +  ") " + getDirection();
    }

    /** 
     * Acts for one step in the simulation.
     */
    public void act()
    {    
        //Get list of empty neighboring cells into which fish can move
        List list = getEnvironment().emptyNeighboringDirections(getX(), getY());
 
        String s = getDirection().reverse().toString();
        list.remove(s);  //Fish don't move backwards, to cell behind them

        if ( list.size() == 0 ) {
           return;  //Do nothing if there is nowhere to move to
        }

        //Select a random cell from the ones available to move into
        int index = Greenfoot.getRandomNumber(list.size());
        String directionToGoTo = (String) list.get(index);
        move(directionToGoTo);

        Direction newDir = new Direction(directionToGoTo);
        setRotation(newDir.inGreenfootDegrees());
   }
   
   /**
    * Move the fish to the specified neighboring location.
    * @param  directionToGoTo    neighboring cell to move into
    */
   private void move(String directionToGoTo)
   {
        int x = getX();
        int y = getY();
        if ( directionToGoTo.equalsIgnoreCase("North") ) {
           setLocation(x, y - 1);
        }
        else if ( directionToGoTo.equalsIgnoreCase("South") ) {
           setLocation(x, y + 1);
        }
        else if ( directionToGoTo.equalsIgnoreCase("East") ) {
           setLocation(x + 1, y);
        }
        else if ( directionToGoTo.equalsIgnoreCase("West") ) {
           setLocation(x - 1, y);
        }
    }
   
    /**
     * Overrides setLocation so that fish only move to an empty cell.
     * @param  x    column of cell of new location
     * @param  y    row of cell of new location
     */
    public void setLocation(int x, int y)
    {
        if (  (getEnvironment() != null)  &&  (! (getX() == x && getY() == y) ) ) {
            // Check if there are any objects at the new location. 
            Object o = getOneObjectAtOffset(x - getX(), y - getY(), null);
            if (o == null) {
                super.setLocation(x, y);
                setLocation(x, y);
            }
        } 
        else {                      
            super.setLocation(x, y);
        }
    }

    /** 
     * Generates a random color.
     * @return   a random color
     **/
    private Color randomColor()
    {
        return new Color(Greenfoot.getRandomNumber(256),    // amount of red
                         Greenfoot.getRandomNumber(256),    // amount of green
                         Greenfoot.getRandomNumber(256));   // amount of blue
    }
}
