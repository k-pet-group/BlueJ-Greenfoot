// AP(r) Computer Science Marine Biology Simulation:
// The DarterFish class is copyright(c) 2002 College Entrance
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

import java.awt.Color;

/**
 *  AP&reg; Computer Science Marine Biology Simulation:<br>
 *  The <code>DarterFish</code> class represents a fish in the Marine
 *  Biology Simulation that darts forward two spaces if it can, moves
 *  forward one space if it  can't move two, and reverses direction
 *  (without moving) if it cannot  move forward.  It can only "see" an
 *  empty location two cells away if the cell in between is empty also.
 *  In other words, if both the cell in front of the darter and the cell
 *  in front of that cell are empty, the darter fish will move forward
 *  two spaces.  If only the cell in front of the darter is empty, it
 *  will move there.  If neither forward cell is empty, the fish will turn
 *  around, changing its direction but not its location.
 *
 *  <p>
 *  <code>DarterFish</code> objects inherit instance variables and much
 *  of their behavior from the <code>Fish</code> class.
 *
 *  <p>
 *  The <code>DarterFish</code> class is
 *  copyright&copy; 2002 College Entrance Examination Board
 *  (www.collegeboard.com).
 *
 *  @author APCS Development Committee
 *  @author Alyce Brady
 *  @version 1 July 2002
 **/

public class DarterFish extends Fish
{

  // constructors

    /** 
     *  For Greenfoot.
     *
     *  <p>
     *
     *  Constructs a darter fish at the location where the user
     *  placed it with the cursor.
     **/
    public DarterFish() { }

    /** Constructs a darter fish at the specified location in a
     *  given environment.   This darter is colored yellow.
     *  (Precondition: parameters are non-null; <code>loc</code> is valid
     *  for <code>env</code>.)
     *  @param env    environment in which fish will live
     *  @param loc    location of the new fish in <code>env</code>
     **/
    public DarterFish(Environment env, Location loc)
    {
        // Construct and initialize the attributes inherited from Fish.
        super(env, loc, env.randomDirection(), Color.yellow);
    }

    /** Constructs a darter fish at the specified location and direction in a
     *  given environment.   This darter is colored yellow.
     *  (Precondition: parameters are non-null; <code>loc</code>
     *  is valid for <code>env</code>.)
     *  @param env    environment in which fish will live
     *  @param loc    location of the new fish in <code>env</code>
     *  @param dir    direction the new fish is facing
     **/
    public DarterFish(Environment env, Location loc, Direction dir)
    {
        // Construct and initialize the attributes inherited from Fish.
        super(env, loc, dir, Color.yellow);
    }
        
    /** Constructs a darter fish of the specified color at the specified
     *  location and direction.
     *  (Precondition: parameters are non-null; <code>loc</code> is valid
     *  for <code>env</code>.)
     *  @param env    environment in which fish will live
     *  @param loc    location of the new fish in <code>env</code>
     *  @param dir    direction the new fish is facing
     *  @param col    color of the new fish
     **/
    public DarterFish(Environment env, Location loc, Direction dir, Color col)
    {
        // Construct and initialize the attributes inherited from Fish.
        super(env, loc, dir, col);
    }


  // redefined methods

    /** Creates a new darter fish.
     *  @param loc    location of the new fish
     **/
    protected void generateChild(Location loc)
    {
        // Create new fish, which adds itself to the environment.
        DarterFish child = new DarterFish(environment(), loc,
                                          environment().randomDirection(),
                                          color());
        getWorld().addObject(child, loc.col(), loc.row());    // Needed for Greenfoot
        Debug.println("  New DarterFish created: " + child.toString());
    }

    /** Moves this fish in its environment.
     *  A darter fish darts forward (as specified in <code>nextLocation</code>)
     *  if possible, or reverses direction (without moving) if it cannot move
     *  forward.
     **/
    protected void move()
    {
        // Find a location to move to.
        Debug.print("DarterFish " + toString() + " attempting to move.  ");
        Location nextLoc = nextLocation();

        // If the next location is different, move there.
        if ( ! nextLoc.equals(location()) )
        {
            changeLocation(nextLoc);
            Debug.println("  Moves to " + location());
        }
        else
        {
            // Otherwise, reverse direction.
            changeDirection(direction().reverse());
            Debug.println("  Now facing " + direction());
        }
    }

    /** Finds this fish's next location.
     *  A darter fish darts forward two spaces if it can, otherwise it
     *  tries to move forward one space.  A darter fish can only move
     *  to empty locations, and it can only move two spaces forward if
     *  the intervening space is empty.  If the darter fish cannot move
     *  forward, <code>nextLocation</code> returns the fish's current
     *  location.
     *  @return    the next location for this fish
     **/
    protected Location nextLocation()
    {
        Environment env = environment();
        Location oneInFront = env.getNeighbor(location(), direction());
        Location twoInFront = env.getNeighbor(oneInFront, direction());
        Debug.println("  Location in front is empty? " + 
                        env.isEmpty(oneInFront));
        Debug.println("  Location in front of that is empty? " + 
                        env.isEmpty(twoInFront));
        if ( env.isEmpty(oneInFront) )
        {
            if ( env.isEmpty(twoInFront) )
                return twoInFront;
            else
                return oneInFront;
        }

        // Only get here if there isn't a valid location to move to.
        Debug.println("  Darter is blocked.");
        return location();
    }

}
