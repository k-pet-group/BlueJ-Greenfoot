// AP(r) Computer Science Marine Biology Simulation:
// The SlowFish class is copyright(c) 2002 College Entrance
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
import java.util.ArrayList;
import java.util.Random;

/**
 *  AP&reg; Computer Science Marine Biology Simulation:<br>
 *  The <code>SlowFish</code> class represents a fish in the Marine Biology
 *  Simulation that moves very slowly.  It moves so slowly that it only has
 *  a 1 in 5 chance of moving out of its current cell into an adjacent cell
 *  in any given timestep in the simulation.  When it does move beyond its
 *  own cell, its movement behavior is the same as for objects of the
 *  <code>Fish</code> class.
 *
 *  <p>
 *  <code>SlowFish</code> objects inherit instance variables and much of
 *  their behavior from the <code>Fish</code> class.
 *
 *  <p>
 *  The <code>SlowFish</code> class is
 *  copyright&copy; 2002 College Entrance Examination Board
 *  (www.collegeboard.com).
 *
 *  @author Alyce Brady
 *  @version 1 July 2002
 **/

public class SlowFish extends Fish
{
    // Instance Variables: Encapsulated data for EACH slow fish
    private double probOfMoving;    // defines likelihood in each timestep


  // constructors
    
    /** 
     *  For Greenfoot.
     *
     *  <p>
     *
     *  Constructs a slow fish at the location where the user
     *  placed it with the cursor.
     **/
    
    public SlowFish()
    {
        probOfMoving = 1.0/5.0;       // 1 in 5 chance in each timestep

    }   

    /** Constructs a slow fish at the specified location in a
     *  given environment.   This slow fish is colored red.
     *  (Precondition: parameters are non-null; <code>loc</code> is valid
     *  for <code>env</code>.)
     *  @param env    environment in which fish will live
     *  @param loc    location of the new fish in <code>env</code>
     **/
    public SlowFish(Environment env, Location loc)
    {
        // Construct and initialize the attributes inherited from Fish.
        super(env, loc, env.randomDirection(), Color.red);

        // Define the likelihood that a slow fish will move in any given
        // timestep.  For now this is the same value for all slow fish.
        probOfMoving = 1.0/5.0;       // 1 in 5 chance in each timestep
    }

    /** Constructs a slow fish at the specified location and direction in a
     *  given environment.   This slow fish is colored red.
     *  (Precondition: parameters are non-null; <code>loc</code> is valid
     *  for <code>env</code>.)
     *  @param env    environment in which fish will live
     *  @param loc    location of the new fish in <code>env</code>
     *  @param dir    direction the new fish is facing
     **/
    public SlowFish(Environment env, Location loc, Direction dir)
    {
        // Construct and initialize the attributes inherited from Fish.
        super(env, loc, dir, Color.red);

        // Define the likelihood that a slow fish will move in any given
        // timestep.  For now this is the same value for all slow fish.
        probOfMoving = 1.0/5.0;       // 1 in 5 chance in each timestep
    }
        
    /** Constructs a slow fish of the specified color at the specified
     *  location and direction.
     *  (Precondition: parameters are non-null; <code>loc</code> is valid
     *  for <code>env</code>.)
     *  @param env    environment in which fish will live
     *  @param loc    location of the new fish in <code>env</code>
     *  @param dir    direction the new fish is facing
     *  @param col    color of the new fish
     **/
    public SlowFish(Environment env, Location loc, Direction dir, Color col)
    {
        // Construct and initialize the attributes inherited from Fish.
        super(env, loc, dir, col);

        // Define the likelihood that a slow fish will move in any given
        // timestep.  For now this is the same value for all slow fish.
        probOfMoving = 1.0/5.0;       // 1 in 5 chance in each timestep
    }


  // redefined methods

    /** Creates a new slow fish.
     *  @param loc    location of the new fish
     **/
    protected void generateChild(Location loc)
    {
        // Create new fish, which adds itself to the environment.
        SlowFish child = new SlowFish(environment(), loc,
                                      environment().randomDirection(),
                                      color());
        getWorld().addObject(child, loc.col(), loc.row());    // Needed for Greenfoot
        Debug.println("  New SlowFish created: " + child.toString());
    }

    /** Finds this fish's next location.  A slow fish moves so
     *  slowly that it might not move out of its current cell in
     *  the environment.
     **/
    protected Location nextLocation()
    {
        // There's only a small chance that a slow fish will actually
        // move in any given timestep, defined by probOfMoving.
        Random randNumGen = RandNumGenerator.getInstance();
        if ( randNumGen.nextDouble() < probOfMoving )
            return super.nextLocation();
        else
        {
            Debug.println("SlowFish " + toString() + 
                          " not attempting to move.");
            return location();
        }
    }

}
