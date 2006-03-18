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
 * 
 * Modified by Poul Henriksen to make it work with Greenfoot
 */

import greenfoot.GreenfootWorld;
import greenfoot.GreenfootObject;

import java.util.ArrayList;

import java.util.List;
public class Grid<E> extends GreenfootWorld
{
// COPY FROM AbstractGrid
   public ArrayList<Location> getValidNeighborLocations(Location loc)
   {
      ArrayList<Location> locs = new ArrayList<Location>();

      int d = Location.NORTH;
      for (int i = 0; i < Location.FULL_CIRCLE / Location.HALF_RIGHT; i++)
      {
         Location neighborLoc = loc.getNeighborLocation(d);
         if (isValid(neighborLoc))
            locs.add(neighborLoc);
         d = d + Location.HALF_RIGHT;
      }
      return locs;
   }

   public ArrayList<Location> getEmptyNeighborLocations(Location loc)
   {
      ArrayList<Location> locs = new ArrayList<Location>();
      for (Location neighborLoc : getValidNeighborLocations(loc))
      {         
         if (get(neighborLoc) == null)
            locs.add(neighborLoc);
      }
      return locs;      
   }
   
   public ArrayList<Location> getOccupiedNeighborLocations(Location loc)
   {
      ArrayList<Location> locs = new ArrayList<Location>();
      for (Location neighborLoc : getValidNeighborLocations(loc))
      {         
         if (get(neighborLoc) != null)
            locs.add(neighborLoc);
      }
      return locs;      
   }
   
   public ArrayList<E> getNeighbors(Location loc)
   {
      ArrayList<E> neighbors = new ArrayList<E>();
      for (Location neighborLoc : getOccupiedNeighborLocations(loc))
         neighbors.add(get(neighborLoc));
      return neighbors;      
   }
   
   /**
    * Creates a string representing all the objects in this grid
    * (not necessarily in any particular order).
    * 
    * @return a string indicating all the objects in this environment
    */
   public String toString()
   {
      ArrayList<Location> locations = getOccupiedLocations();
      String s = "{";
      for (int index = 0; index < locations.size(); index++)
      {
         if (index > 0)
            s += ", ";
         Location loc = locations.get(index);
         s += loc + "=" + get(loc);
      }
      return s + "}";
   }   
   
 // END COPY
    

  // private Object[][] occupantArray; // the array storing the grid elements
   private static final int DEFAULT_ROWS = 10;
   private static final int DEFAULT_COLS = 10;
   private static final int DEFAULT_CELL_SIZE = 64;
   
   /**
    * Constructs an empty BoundedGrid object with the given dimensions.
    * (Precondition: <code>rows > 0</code> and <code>cols > 0</code>.)
    * 
    * @param rows
    *           number of rows in BoundedGrid
    * @param cols
    *           number of columns in BoundedGrid
    */
   public Grid()
   {
      super(DEFAULT_ROWS, DEFAULT_COLS, DEFAULT_CELL_SIZE);
      if (DEFAULT_ROWS <= 0) 
         throw new IllegalArgumentException("rows <= 0");
      if (DEFAULT_COLS <= 0) 
         throw new IllegalArgumentException("cols <= 0");
      //occupantArray = new Object[DEFAULT_ROWS][DEFAULT_COLS];
   }

   public int numRows()
   {
      return getHeight();//return occupantArray.length;
   }

   public int numCols()
   {
      // Note: according to the constructor precondition, numRows() > 0, so
      // theGrid[0] is non-null.
      return getWidth();//return occupantArray[0].length;
   }

   public boolean isValid(Location loc)
   {
      return 0 <= loc.row() && loc.row() < numRows()
            && 0 <= loc.col() && loc.col() < numCols();
   }

   
   public ArrayList<Location> getOccupiedLocations()
   {
      ArrayList<Location> theLocations = new ArrayList<Location>();

      // Look at all grid locations.
      for (int r = 0; r < numRows(); r++)
      {
         for (int c = 0; c < numCols(); c++)
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
         throw new IllegalArgumentException("Location " + loc + " is not valid");
     
      E occupant = null;
      List l = getObjectsAt(loc.col(), loc.row(),null);
      for(Object o : l) {
         GreenfootObject go = (GreenfootObject) o;
         if(go.getX() == loc.col() && go.getY() == loc.row()) {
            occupant = (E) o;
            break;
         }
      }
      return occupant;
         //return (E) occupantArray[loc.row()][loc.col()]; // unavoidable warning
      
   }

   public E put(Location loc, E obj)
   {
      // Check precondition. Location should be valid.
      if (!isValid(loc))
         throw new IllegalArgumentException("Location " + loc + " is not valid");
      if (obj == null)
         throw new NullPointerException("obj == null");

      GreenfootObject go = (GreenfootObject) obj;
      E oldOccupant = null;
      List l = getObjectsAt(loc.col(), loc.row(),null);
      if(!l.isEmpty()) {
          oldOccupant = (E) l.get(0);
          removeObject((GreenfootObject) oldOccupant);
      }
      addObject(go);
      go.setLocation(loc.col(), loc.row());
      return oldOccupant;
      /*
      // Add object to the grid.
      E oldOccupant = get(loc);
      occupantArray[loc.row()][loc.col()] = obj;
      return oldOccupant;*/
   }

   public E remove(Location loc)
   {
      if (!isValid(loc))
         throw new IllegalArgumentException("Location " + loc + " is not valid");
      // Remove the object from the grid.
      
      List l = getObjectsAt(loc.col(), loc.row(),null);
      GreenfootObject r = null;
      for(Object o : l) {
         GreenfootObject go = (GreenfootObject) o;
         if(go.getX() == loc.col() && go.getY() == loc.row()) {
            r = (GreenfootObject) o;
            break;
         }
      }
      removeObject(r);
   /*   if(!l.isEmpty()) {
          
          r = (E) l.get(0);
          removeObject((GreenfootObject) r);
      }*/
      return (E) r;
      
      /*E r = get(loc);
      occupantArray[loc.row()][loc.col()] = null;
      return r;*/
   }
   
   
    
    /// METHODS ADDED FOR GREENFOOT
    
   /* public void addObject(GreenfootObject o) {
        
        super.addObject( o);
        if(o instanceof Actor) {
            ((Actor) o).putSelfInGrid((Grid<Actor>)this, new Location(o.getX(), o.getY()));
        }
        // this.put(new Location(o.getX(), o.getY()), (E) o);
        System.out.println("obj added");
    }
    
    public void removeObject(GreenfootObject o) {
        System.out.println("RemoveObject");
    }
    */
    
}

class Location implements Comparable
{
    private int myRow; // row location in grid
    private int myCol; // column location in grid
   
    public static final int FULL_CIRCLE = 360;
    public static final int HALF_CIRCLE = 180;
    public static final int HALF_LEFT = -45;
    public static final int LEFT = -90;
    public static final int HALF_RIGHT = 45;
    public static final int RIGHT = 90;
    public static final int AHEAD = 0;

    public static final int NORTH = 0;
    public static final int NORTHEAST = 45;   
    public static final int EAST = 90;   
    public static final int SOUTHEAST = 135;
    public static final int SOUTH = 180;   
    public static final int SOUTHWEST = 225;   
    public static final int WEST = 270;   
    public static final int NORTHWEST = 315;  
   
    /** Constructs a <code>Location</code> object.
     *  @param row    location's row
     *  @param col    location's column
     **/
    public Location(int row, int col)
    {
        myRow = row;
        myCol = col;
    }

    /** Returns the row coordinate of this location.
     *  @return        row of this location
     **/
    public int row()
    {
        return myRow;
    }

    /** Returns the column coordinate of this location.
     *  @return        column of this location
     **/
    public int col()
    {
        return myCol;
    }

    /** Indicates whether some other <code>Location</code> object is
     *  "equal to" this one.
     *  @param other    the other location to test
     *  @return     <code>true</code> if <code>other</code> is at the
     *              same row and column as the current location;
     *              <code>false</code> otherwise
     **/
    public boolean equals(Object other)
    {
        if ( ! (other instanceof Location) )
          return false;

        Location otherLoc = (Location) other;
        return row() == otherLoc.row() && col() == otherLoc.col();
    }

    /** Generates a hash code for this location
     *  (will not be tested on the Advanced Placement exam).
     *  @return     a hash code for a <code>Location</code> object
     **/
    public int hashCode()
    {
       return row() * 3737 + col();
    }

    /** Compares this location to <code>otherObject</code> for ordering.
     *  Returns a negative integer, zero, or a positive integer as this
     *  location is less than, equal to, or greater than <code>otherObject</code>.
     *  Locations are ordered in row-major order.
     *  (Precondition: <code>otherObject</code> is a <code>Location</code> object.)
     *  @param otherObject    the other location to test
     *  @return     a negative integer if this location is less than
     *              <code>otherObject</code>, zero if the two locations are equal,
     *              or a positive integer if this location is greater than
     *              <code>otherObject</code>
     **/
    public int compareTo(Object otherObject)
    {
        Location other = (Location) otherObject;
        if (row() < other.row()) 
           return -1;
        if (row() > other.row())
           return 1;
        if (col() < other.col()) 
           return -1;
        if (col() > other.col())
           return 1;
        return 0;
    }

    /** Represents this location as a string.
     *  @return        a string indicating the row and column of the
     *                 location in (row, col) format
     **/
    public String toString()
    {
        return "(" + row() + ", " + col() + ")";
    }

    /**
     * Gets the neighbor location in any one of the eight compass directions. 
     * @param direction the direction in which to find a neighbor location 
     * @return the neighbor location in the direction that is obtained by
     * rounding <tt>direction</tt> to the nearest multiple of 45 degrees
     */
    public Location getNeighborLocation(int direction)
    {
       // reduce mod 360 and round to nearest multiple of 45
       int adjustedDirection = (direction + HALF_RIGHT / 2) % FULL_CIRCLE;
       if (adjustedDirection < 0)
          adjustedDirection += FULL_CIRCLE;
       
       adjustedDirection = (adjustedDirection / HALF_RIGHT) * HALF_RIGHT;
       int dc = 0;
       int dr = 0;
       if (adjustedDirection == EAST)
          dc = 1;
       else if (adjustedDirection == SOUTHEAST)
       {
          dc = 1;
          dr = 1;
       }
       else if (adjustedDirection == SOUTH)
          dr = 1;
       else if (adjustedDirection == SOUTHWEST)
       {
          dc = -1;
          dr = 1;
       }
       else if (adjustedDirection == WEST)
          dc = -1;
       else if (adjustedDirection == NORTHWEST)
       {
          dc = -1;
          dr = -1;
       }
       else if (adjustedDirection == NORTH)
          dr = -1;
       else if (adjustedDirection == NORTHEAST)
       {
          dc = 1;
          dr = -1;
       }
       return new Location(row() + dr, col() + dc);
    }
    
    /**
     * Returns the direction from this location toward another location. 
     * The direction is rounded to the nearest compass direction.
     * @param target another location
     * @return the closest compass direction from this location toward target
     */
    public int directionToward(Location target)
    {
       int dx = target.col() - col();
       int dy = target.row() - row();
       // y axis points opposite to mathematical orientation 
       int angle = (int) Math.toDegrees(Math.atan2(-dy, dx));
       
       // mathematical angle is counterclockwise from x-axis, 
       // compass angle is clockwise from y-axis
       int compassAngle = RIGHT - angle; 
       // prepare for truncating division by 45 degrees
       compassAngle += HALF_RIGHT / 2;        
       // wrap negative angles    
       if (compassAngle < 0) 
          compassAngle += FULL_CIRCLE;
       // round to nearest multiple of 45
       return (compassAngle / HALF_RIGHT) * HALF_RIGHT;       
    }
    
    
}