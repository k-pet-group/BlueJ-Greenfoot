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

 

import java.util.ArrayList;

/**
 * <code>AbstractGrid</code> contains the methods that are common to grid
 * implementations. <br />
 * The implementation of this class is testable on the AP CS AB exam.
 */
public abstract class AbstractGrid<E> extends greenfoot.World implements Grid<E>  // Greenfoot: extends World added for greenfoot
{
   
    
    public ArrayList<E> getNeighbors(Location loc)
    {
        ArrayList<E> neighbors = new ArrayList<E>();
        for (Location neighborLoc : getOccupiedAdjacentLocations(loc))
            neighbors.add(get(neighborLoc));
        return neighbors;
    }

    public ArrayList<Location> getValidAdjacentLocations(Location loc)
    {
        ArrayList<Location> locs = new ArrayList<Location>();

        int d = Location.NORTH;
        for (int i = 0; i < Location.FULL_CIRCLE / Location.HALF_RIGHT; i++)
        {
            Location neighborLoc = loc.getAdjacentLocation(d);
            if (isValid(neighborLoc))
                locs.add(neighborLoc);
            d = d + Location.HALF_RIGHT;
        }
        return locs;
    }

    public ArrayList<Location> getEmptyAdjacentLocations(Location loc)
    {
        ArrayList<Location> locs = new ArrayList<Location>();
        for (Location neighborLoc : getValidAdjacentLocations(loc))
        {
            if (get(neighborLoc) == null)
                locs.add(neighborLoc);
        }
        return locs;
    }

    public ArrayList<Location> getOccupiedAdjacentLocations(Location loc)
    {
        ArrayList<Location> locs = new ArrayList<Location>();
        for (Location neighborLoc : getValidAdjacentLocations(loc))
        {
            if (get(neighborLoc) != null)
                locs.add(neighborLoc);
        }
        return locs;
    }

    /**
     * Creates a string that describes this grid.
     * @return a string with descriptions of all objects in this grid (not
     * necessarily in any particular order), in the format {loc=obj, loc=obj,
     * ...}
     */
    public String toString()
    {
        String s = "{";
        for (Location loc : getOccupiedLocations())
        {
            if (s.length() > 1)
                s += ", ";
            s += loc + "=" + get(loc);
        }
        return s + "}";
    }
     
    
    // The rest of the methods here are for running in Greenfoot, and is NOT testable on any of the AP CS exams.

    /**
     * Greenfoot: this constructor is added to make greenfoot compile. 
     * It also paints the grid.
     */
    public AbstractGrid(int width, int height, int cellSize)
    {
        super(width, height, cellSize);
        paintGrid();
    }

    /**
     * Greenfoot:
     * Paints the grid.
     */
    private void paintGrid()
    {
        greenfoot.GreenfootImage bg = getBackground();
        int cellSize = getCellSize();
        bg.setColor(java.awt.Color.BLACK);
        for (int x = 0; x < bg.getWidth(); x += cellSize) {
            bg.drawLine(x, 0, x, bg.getHeight());
        }
        for (int y = 0; y < bg.getHeight(); y += cellSize) {
            bg.drawLine(0, y, bg.getWidth(), y);
        }
        setBackground(bg);
    }

    /** 
     *  For Greenfoot.
     * 
     *  <p>
     *  
     *  It removes an object from the GridWorld environment when the 
     *  user removes an actor from the world. 
     *  @param object      Object to remove from the Greenfoot world.
     **/
    public void removeObject(greenfoot.Actor object)
    {
        if (object == null) {
            return;
        }
        if (object instanceof GridActor) {
            GridActor gridActor = (GridActor) object;
            if (gridActor.getGrid() != null) {
                gridActor.removeSelfFromGrid();
            }
        }
        super.removeObject(object);
    }

    /**
     * For Greenfoot.
     * 
     * <p>
     * 
     * Overridden to disallow adding an object to an occupied cell.
     * @param obj      Object to add to the Greenfoot world.
     * @param x        Column in which to add the object.
     * @param y        Row in which to add the object.
     */
    public void addObject(greenfoot.Actor obj, int x, int y)
    {
        //Get all objects that overlap the location (x,y)
        java.util.List occupants = getObjectsAt(x, y, null);

        //Remove objects that does not have their location at (x,y)
        java.util.Iterator iter = occupants.iterator();
        while (iter.hasNext()) {
            greenfoot.Actor a = (greenfoot.Actor) iter.next();
            if (!(a.getX() == x && a.getY() == y)) {
                iter.remove();
            }
        }

        if (occupants.isEmpty()) {
            super.addObject(obj, x, y);
        }
        else if (occupants.get(0) instanceof greenfoot.core.ObjectDragProxy && occupants.size() == 1) {
            //the proxy object being dragged might be there already - that is OK.
            super.addObject(obj, x, y);
        }
        else if (obj instanceof greenfoot.core.ObjectDragProxy) {
            //we are allowed to drag the proxy around on top of other objects.
            super.addObject(obj, x, y);
        }
    }        
}
