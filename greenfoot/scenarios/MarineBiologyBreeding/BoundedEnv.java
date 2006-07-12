// AP(r) Computer Science Marine Biology Simulation:
// The BoundedEnv class is copyright(c) 2002 College Entrance
// Examination Board (www.collegeboard.com).
//
// This class is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation.
//
// This class is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

import java.util.List;    //Needed for Greenfoot
import greenfoot.Actor;   //Needed for Greenfoot
import java.awt.Color;    //Needed for Greenfoot
import java.util.Random;  //Needed for Greenfoot

/**
 *  AP&reg; Computer Science Marine Biology Simulation:<br>
 *  The <code>BoundedEnv</code> class models a bounded, two-dimensional,
 *  grid-like  environment containing locatable objects.  For example,
 *  it could be an environment of fish for a marine biology simulation.
 *
 *  <p>
 *  The <code>BoundedEnv</code> class is
 *  copyright&copy; 2002 College Entrance Examination Board
 *  (www.collegeboard.com).
 *
 *  @author Alyce Brady
 *  @author APCS Development Committee
 *  @version 1 July 2002
 *  @see Locatable
 *  @see Location
 **/

public class BoundedEnv extends SquareEnvironment
{
    private static final int ROWS = 15;  //Needed for Greenfoot
    private static final int COLS = 15;  //Needed for Greenfoot

    // Instance Variables: Encapsulated data for each BoundedEnv object
    private Locatable[][] theGrid;  // grid representing the environment
    private int objectCount;        // # of objects in current environment


  // constructors
    
    /** 
     *  For Greenfoot. 
     *
     *  <p>
     *  
     *  Constructs a BoundedEnv object, with a few fish in random
     *  locations, and of the dimensions specified in the static
     *  variables added for Greenfoot in class BoundedEnv.
     **/
    public BoundedEnv()   
    {
        this(ROWS, COLS);
        populate();
    }

    /** Constructs an empty BoundedEnv object with the given dimensions.
     *  (Precondition: <code>rows > 0</code> and <code>cols > 0</code>.)
     *  @param rows        number of rows in BoundedEnv
     *  @param cols        number of columns in BoundedEnv
     **/
    public BoundedEnv(int rows, int cols)
    {
        // Construct and initialize inherited attributes.
        super(rows, cols);   //Changed for Greenfoot, was super()

        theGrid = new Locatable[rows][cols];
        objectCount = 0;
    }
    

  // accessor methods

    /** Returns number of rows in the environment.
     *  @return   the number of rows, or -1 if this environment is unbounded
     **/
    public int numRows()
    {
        return theGrid.length;
    }

    /** Returns number of columns in the environment.
     *  @return   the number of columns, or -1 if this environment is unbounded
     **/
    public int numCols()
    {
        // Note: according to the constructor precondition, numRows() > 0, so
        // theGrid[0] is non-null.
        return theGrid[0].length;
    }

    /** Verifies whether a location is valid in this environment.
     *  @param  loc    location to check
     *  @return <code>true</code> if <code>loc</code> is valid;
     *          <code>false</code> otherwise
     **/
    public boolean isValid(Location loc)
    {
        if ( loc == null )
            return false;

        return (0 <= loc.row() && loc.row() < numRows()) &&
               (0 <= loc.col() && loc.col() < numCols());
    }

    /** Returns the number of objects in this environment.
     *  @return   the number of objects
     **/
    public int numObjects()
    {
        return objectCount;
    }

    /** Returns all the objects in this environment.
     *  @return    an array of all the environment objects
     **/
    public Locatable[] allObjects()
    {
        Locatable[] theObjects = new Locatable[numObjects()];
        int tempObjectCount = 0;

        // Look at all grid locations.
        for ( int r = 0; r < numRows(); r++ )
        {
            for ( int c = 0; c < numCols(); c++ )
            {
                // If there's an object at this location, put it in the array.
                Locatable obj = theGrid[r][c];
                if ( obj != null )
                {
                    theObjects[tempObjectCount] = obj;
                    tempObjectCount++;
                }
            }
        }

        return theObjects;
    }

    /** Determines whether a specific location in this environment is
     *  empty.
     *  @param loc  the location to test
     *  @return     <code>true</code> if <code>loc</code> is a
     *              valid location in the context of this environment
     *              and is empty; <code>false</code> otherwise
     **/
    public boolean isEmpty(Location loc)
    {
        return isValid(loc) && objectAt(loc) == null;
    }

    /** Returns the object at a specific location in this environment.
     *  @param loc    the location in which to look
     *  @return       the object at location <code>loc</code>;
     *                <code>null</code> if <code>loc</code> is not
     *                in the environment or is empty
     **/
    public Locatable objectAt(Location loc)
    {
        if ( ! isValid(loc) )
            return null;

        return theGrid[loc.row()][loc.col()];
    }

    /** Creates a single string representing all the objects in this
     *  environment (not necessarily in any particular order).
     *  @return    a string indicating all the objects in this environment
     **/
    public String toString()
    {
        Locatable[] theObjects = allObjects();
        String s = "Environment contains " + numObjects() + " objects: ";
        for ( int index = 0; index < theObjects.length; index++ )
            s += theObjects[index].toString() + " ";
        return s;
    }


  // modifier methods

    /** Adds a new object to this environment at the location it specifies.
     *  (Precondition: <code>obj.location()</code> is a valid empty location.)
     *  @param obj the new object to be added
     *  @throws    IllegalArgumentException if the precondition is not met
     **/
    public void add(Locatable obj)
    {
        // Check precondition.  Location should be empty.
        Location loc = obj.location();
        if ( ! isEmpty(loc) )
            throw new IllegalArgumentException("Location " + loc +
                                    " is not a valid empty location");

        // Add object to the environment.
        theGrid[loc.row()][loc.col()] = obj;
        objectCount++;
    }

    /** Removes the object from this environment.
     *  (Precondition: <code>obj</code> is in this environment.)
     *  @param obj     the object to be removed
     *  @throws    IllegalArgumentException if the precondition is not met
     **/
    public void remove(Locatable obj)
    {
        // Make sure that the object is there to remove.
        Location loc = obj.location();
        if ( objectAt(loc) != obj )
            throw new IllegalArgumentException("Cannot remove " + 
                                               obj + "; not there");

        // Remove the object from the grid.
        theGrid[loc.row()][loc.col()] = null;
        objectCount--;
        removeObject((Actor) obj);   //Needed for Greenfoot
    }

    /** Updates this environment to reflect the fact that an object moved.
     *  (Precondition: <code>obj.location()</code> is a valid location
     *  and there is no other object there.
     *  Postcondition: <code>obj</code> is at the appropriate location
     *  (<code>obj.location()</code>), and either <code>oldLoc</code> is 
     *  equal to <code>obj.location()</code> (there was no movement) or
     *  <code>oldLoc</code> is empty.)
     *  @param obj       the object that moved
     *  @param oldLoc    the previous location of <code>obj</code>
     *  @throws    IllegalArgumentException if the precondition is not met
     **/
    public void recordMove(Locatable obj, Location oldLoc)
    {
        // Simplest case: There was no movement.
        Location newLoc = obj.location();
        if ( newLoc.equals(oldLoc) )
            return;

        // Otherwise, oldLoc should contain the object that is
        //   moving and the new location should be empty.
        Locatable foundObject = objectAt(oldLoc);
        if ( ! (foundObject == obj && isEmpty(newLoc)) )
            throw new IllegalArgumentException("Precondition violation moving "
                + obj + " from " + oldLoc);

        // Move the object to the proper location in the grid.
        theGrid[newLoc.row()][newLoc.col()] = obj;
        theGrid[oldLoc.row()][oldLoc.col()] = null;
    }

    /** 
     *  For Greenfoot.
     * 
     *  <p>
     *  
     *  It removes an object from the MBS environment when the 
     *  user removes an actor from the world. 
     *  @param object      Object to remove from the Greenfoot world.
     **/
    public void removeObject(Actor object)
    { 
       if ( object instanceof Fish )
       {
           Fish fish = (Fish) object;
           if ( fish.isInEnv() )
           {
              Debug.println(fish.toString() + " about to die.");
              remove(fish);
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
    public void addObject(Actor obj, int x, int y) {
        List occupants = getObjectsAt(x, y, null);
        
        if(occupants.isEmpty()) {
            super.addObject(obj, x, y);
        } 
        else if ( occupants.get(0) instanceof greenfoot.core.ObjectDragProxy && occupants.size() == 1) {
            //the proxy object being dragged might be there already - that is OK.
            super.addObject(obj, x, y);
        } 
        else if ( obj instanceof greenfoot.core.ObjectDragProxy) {
            //we are allowed to drag the proxy around on top of other objects.
            super.addObject(obj, x, y);
        } 
    }
           
      
     /**
     * For Greenfoot.
     * 
     * <p>
     * 
     * Populates the world with randomly colored fish in random locations.
     */
    public void populate()
    {
        Fish fish;
        Color color;
        Location loc;
        int rows = numRows();
        int cols = numCols();
        int r,c;
        
        Random rand = RandNumGenerator.getInstance();

        for (int i = 1; i < rows; i++) {
           color = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
           r = rand.nextInt(rows);
           c = rand.nextInt(cols);
           loc =  new Location(r,c);
           if ( isEmpty(loc) ) {
              fish = new Fish(this, loc , randomDirection(), color); 
              addObject(fish, c, r);
           }
        }
    } 
}