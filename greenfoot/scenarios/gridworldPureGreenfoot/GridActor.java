/* 
 * AP(r) Computer Science GridWorld Case Study:
 * Copyright(c) 2005-2006 Cay S. Horstmann (http://horstmann.com)
 *
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * @author Cay Horstmann
 * @author Poul Henriksen (Modifications to run in Greenfoot)
 */

 

import java.awt.Color;
import java.util.List;
/**
 * An <code>GridActor</code> is an entity with a color and direction that can act.
 * <br />
 * The API of this class is testable on the AP CS A and AB exams.
 */
public class GridActor extends greenfoot.Actor  
{

    private Location location;
    private Color color;

    /**
     * Constructs a blue actor that is facing north.
     */
    public GridActor()
    {
        color = Color.BLUE;
        setDirection(Location.NORTH);
        location = null;
    }

    /**
     * Gets the color of this actor.
     * @return the color of this actor
     */
    public Color getColor()
    {
        return color;
    }

    /**
     * Sets the color of this actor.
     * @param newColor the new color
     */
    public void setColor(Color newColor)
    {
        color = newColor;
    }

    /**
     * Gets the current direction of this actor.
     * @return the direction of this actor, an angle between 0 and 359 degrees
     */
    public int getDirection()
    {
        return getRotation();
    }

    /**
     * Sets the current direction of this actor.
     * @param newDirection the new direction. The direction of this actor is set
     * to the angle between 0 and 359 degrees that is equivalent to
     * <code>newDirection</code>.
     */
    public void setDirection(int newDirection)
    {
        int direction = newDirection % Location.FULL_CIRCLE;
        if (direction < 0)
            direction += Location.FULL_CIRCLE;
            
        setRotation(direction);  //Greenfoot: Set the rotation for greenfoot                       
    }

    /**
     * Gets the grid in which this actor is located.
     * @return the grid of this actor, or <code>null</code> if this actor is
     * not contained in a grid
     */
    public Grid<GridActor> getGrid()
    {
        return (Grid<GridActor>) getWorld();
    }

    /**
     * Gets the location of this actor. <br />
     * Precondition: This actor is contained in a grid
     * @return the location of this actor
     */
    public Location getLocation()
    {
        return location;
    }


    /**
     * Removes this actor from its grid. <br />
     * Precondition: This actor is contained in a grid
     */
    public void removeSelfFromGrid()
    {
        //Greenfoot: remove the object from the Greenfoot world
        if(getWorld() != null) {
            getWorld().removeObject(this);
        }
    }

    /**
     * Moves this actor to a new location. If there is another actor at the
     * given location, it is removed. <br />
     * Precondition: (1) This actor is contained in a grid (2)
     * <code>newLocation</code> is valid in the grid of this actor
     * @param newLocation the new location
     */
    public void moveTo(Location newLocation)
    {
        if (getGrid().get(location) != this)
            throw new IllegalStateException(
                    "The grid contains a different actor at location "
                            + location + "." +  "    THIS:" + this + "    OTHER: " +getGrid().get(location)) ;

        if (newLocation.equals(location))
            return;
       
        //Remove any actors at the new location
        List others = getGrid().getObjectsAt(newLocation.getCol(), newLocation.getRow(), null);
        getGrid().removeObjects(others);
      
        location = newLocation;
        
        setLocation(newLocation.getCol(), newLocation.getRow());
    }

    /**
     * Reverses the direction of this actor. Override this method in subclasses
     * of <code>GridActor</code> to define types of actors with different behavior
     * 
     */
    public void act()
    {
        setDirection(getDirection() + Location.HALF_CIRCLE);
        
        //Greenfoot: set the rotation for Greenfoot
        setRotation(getDirection());
    }

    /**
     * Creates a string that describes this actor.
     * @return a string with the location, direction, and color of this actor
     */
    public String toString()
    {
        return getClass().getName() + "[location=" + location + ",direction="
                + getRotation() + ",color=" + color + "]";
    }
    
    /**
     * For Greenfoot.
     * <p>
     * 
     * Overrides setLocation so that setting the location from greenfoot 
     * changes the location in the grid.
     * 
     */
    public void setLocation(int x, int y) {
        if (getGrid() != null && ! (getX() == x && getY() == y)) {
            // Check if there are any objects at the new location. 
            Object o = getOneObjectAtOffset(x - getX(), y - getY(), null);
            if(o == null) {
                // In GridWorld you can only put the Actor in a cell that is empty.
                super.setLocation(x, y);
                //moveTo(new Location(y, x));
            }
        } else if (getWorld() != null){  
            super.setLocation(x, y);
        }
    }
    
    /**
     * For Greenfoot.
     * <p>
     * 
     * Second initialization method in Greenfoot. Updates the
     * environment when objects are added to the world.
     * @param world    world where objects are added.
     */
    protected void addedToWorld(greenfoot.World world)  
    {
        // Scale image to cell size.
        getImage().scale(world.getCellSize() - 2, world.getCellSize() - 2);
       
        location = new Location(getY(), getX());
        /*if (grid== null )
        {
           Location loc = new Location(getY(), getX());
           Grid grid = (Grid) world;
           putSelfInGrid(grid, loc);
        }*/
    }
    
    /**
     * Overridden so we can ignore exception when an object is
     * dragged outside the world, and back into a cell that is
     * occupied.
     */
  /*  public int getX() {
        try {
            return super.getX();
        } catch  (IllegalStateException e ){
            return -1;
        }
    }*/
    
    /**
     * Overridden so we can ignore exception when an object is
     * dragged outside the world, and back into a cell that is
     * occupied.
     */
  /*  public int getY() {
        try {
            return super.getY();
        } catch  (IllegalStateException e ){
            return -1;
        }
    }*/
}