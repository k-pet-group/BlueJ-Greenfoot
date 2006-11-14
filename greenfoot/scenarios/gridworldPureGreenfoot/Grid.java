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

 
import greenfoot.*;
import java.util.List;
import java.util.ArrayList;

/**
 * This class is a merge between the APCS classes AbstractGrid and BoundedGrid.
 * <code>AbstractGrid</code> contains the methods that are common to grid
 * implementations. <br />
 * The implementation of this class is testable on the AP CS AB exam.
 */
public class Grid<E> extends greenfoot.World 
{
   
    //=====================================================
    // Methods from the APCS BoundedGrid class.
    //=====================================================
    
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
     
    //=====================================================
    // Methods from the APCS AbstractGrid class.
    //=====================================================
    

    public int getNumRows()
    {
        return getHeight();
    }

    public int getNumCols()
    {
        // Note: according to the constructor precondition, numRows() > 0, so
        // theGrid[0] is non-null.
        return getWidth();
    }

    public boolean isValid(Location loc)
    {
        return 0 <= loc.getRow() && loc.getRow() < getNumRows()
                && 0 <= loc.getCol() && loc.getCol() < getNumCols();
    }

    public ArrayList<Location> getOccupiedLocations()
    {
        ArrayList<Location> theLocations = new ArrayList<Location>();

        // Look at all grid locations.
        for (int r = 0; r < getNumRows(); r++)
        {
            for (int c = 0; c < getNumCols(); c++)
            {
                // If there's an object at this location, put it in the array.
                Location loc = new Location(r, c);
                if (get(loc) != null)
                    theLocations.add(loc);
            }
        }

        return theLocations;
    }

    public E get(Location loc)
    {
        if (!isValid(loc))
            throw new IllegalArgumentException("Location " + loc
                    + " is not valid");                    
        
        List objects = getObjectsAt(loc.getCol(), loc.getRow(), null);
        if(objects.isEmpty() ) {
            return null;
        }
        return (E) objects.get(0);  // unavoidable warning
    }

    public E put(Location loc, E obj)
    {
        if (!isValid(loc))
            throw new IllegalArgumentException("Location " + loc
                    + " is not valid");
                            
        if (obj == null)
            throw new NullPointerException("obj == null");

        // Add the object to the grid.
        addObject((Actor) obj, loc.getCol(), loc.getRow());
        
        E oldOccupant = get(loc);
        return oldOccupant;
    }
    
    

    //=====================================================
    // These methods are new for Greenfoot
    //=====================================================
    
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
     * For Greenfoot.
     * 
     * <p>
     * 
     * Overridden to disallow adding an object to an occupied cell.
     * 
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
    
    /**
     * Added for Greenfoot so that it can automatically instantiate a new world.
     */
    public Grid()
    {
        this(12,12,32);
    }
    
    public Grid(int rows, int cols, int cellSize) {
        super(cols, rows, cellSize);
        paintGrid();
    }
}
