// AP(r) Computer Science Marine Biology Simulation:
// The Fish class is copyright(c) 2002 College Entrance
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
import greenfoot.Actor;             //needed for Greenfoot
import greenfoot.World;             //needed for Greenfoot
import greenfoot.GreenfootImage;    //needed for Greenfoot

/**
 *  AP&reg; Computer Science Marine Biology Simulation:<br>
 *  A <code>Fish</code> object represents a fish in the Marine Biology
 *  Simulation. Each fish has a unique ID, which remains constant
 *  throughout its life.  A fish also maintains information about its
 *  location and direction in the environment.
 *
 *  <p>
 *  The <code>Fish</code> class is
 *  copyright&copy; 2002 College Entrance Examination Board
 *  (www.collegeboard.com).
 *
 *  <p>
 *  This class was modified by the Greefoot developers 
 *  to run the simulation in Greenfoot.

 *  @author Alyce Brady
 *  @author APCS Development Committee
 *  @author Cecilia Vargas (Greenfoot modifications)
 *  @version 1 July 2002
 *  @see Environment
 *  @see Direction
 *  @see Location
 **/

public class Fish extends Actor implements Locatable  
{
    //"extends Actor" above is needed for Greenfoot

    // Class Variable: Shared among ALL fish
    private static int nextAvailableID = 1;   // next avail unique identifier

    // Instance Variables: Encapsulated data for EACH fish
    private Environment theEnv;        // environment in which the fish lives
    private int myId;                  // unique ID for this fish
    private Location myLoc;            // fish's location
    private Direction myDir;           // fish's direction
    private Color myColor;             // fish's color


  // constructors and related helper methods

    /** 
     *  For Greenfoot.
     *  <p>
     *
     *  Constructs a fish at the location where the user placed it with 
     *  the cursor. The fish is assigned a random direction and random
     *  color.
     *
     **/
    public Fish() { } 

    /** Constructs a fish at the specified location in a given environment.
     *  The Fish is assigned a random direction and random color.
     *  (Precondition: parameters are non-null; <code>loc</code> is valid
     *  for <code>env</code>.)
     *  @param env    environment in which fish will live
     *  @param loc    location of the new fish in <code>env</code>
     **/
    public Fish(Environment env, Location loc)
    {
        initialize(env, loc, env.randomDirection(), randomColor());
    }

    /** Constructs a fish at the specified location and direction in a
     *  given environment.  The Fish is assigned a random color.
     *  (Precondition: parameters are non-null; <code>loc</code> is valid
     *  for <code>env</code>.)
     *  @param env    environment in which fish will live
     *  @param loc    location of the new fish in <code>env</code>
     *  @param dir    direction the new fish is facing
     **/
    public Fish(Environment env, Location loc, Direction dir)
    {
        initialize(env, loc, dir, randomColor());
    }

    /** Constructs a fish of the specified color at the specified location
     *  and direction.
     *  (Precondition: parameters are non-null; <code>loc</code> is valid
     *  for <code>env</code>.)
     *  @param env    environment in which fish will live
     *  @param loc    location of the new fish in <code>env</code>
     *  @param dir    direction the new fish is facing
     *  @param col    color of the new fish
     **/
    public Fish(Environment env, Location loc, Direction dir, Color col)
    {
        initialize(env, loc, dir, col);
    }

    /** Initializes the state of this fish.
     *  (Precondition: parameters are non-null; <code>loc</code> is valid
     *  for <code>env</code>.)
     *  @param env    environment in which this fish will live
     *  @param loc    location of this fish in <code>env</code>
     *  @param dir    direction this fish is facing
     *  @param col    color of this fish
     **/
    private void initialize(Environment env, Location loc, Direction dir,
                            Color col)
    {
        theEnv = env;
        myId = nextAvailableID;
        nextAvailableID++;
        myLoc = loc;
        myDir = dir;
        myColor = col;
        theEnv.add(this);
        setRotation(dir.inDegrees() - 90);  // Needed for Greenfoot 
            // Need -90 since 0 is NORTH in the MBS, but 0 is EAST in Greenfoot 

        // object is at location myLoc in environment
    }

    /**
     * For Greenfoot.
     * <p>
     * 
     * Second initialization method in Greenfoot. Updates the
     * environment when objects are added to the world.
     * @param world    world where objects are added.
     */
    protected void addedToWorld(World world)
    {
        // Scale image to cell size.
        getImage().scale(world.getCellSize() - 2, world.getCellSize() - 2);
        if ( theEnv == null )
        {
           Location loc = new Location(getY(), getX());
           Environment env = (Environment) world;
           initialize(env, loc, env.randomDirection(), randomColor());
        }
    }

    /**
     * For Greenfoot.
     * <p>
     * 
     * Overrides the getImage so that setting the color will change the image.
     * 
     */
    public GreenfootImage getImage()
    {
        return  ColoredImage.getImage(this, super.getImage(), myColor);
    }

    /** Generates a random color.
     *  @return       the new random color
     **/
    protected Color randomColor()
    {
        // There are 256 possibilities for the red, green, and blue attributes
        // of a color.  Generate random values for each color attribute.
        Random randNumGen = RandNumGenerator.getInstance();
        return new Color(randNumGen.nextInt(256),    // amount of red
                         randNumGen.nextInt(256),    // amount of green
                         randNumGen.nextInt(256));   // amount of blue
    }


  // accessor methods

    /** Returns this fish's ID.
     *  @return        the unique ID for this fish
     **/
    public int id()
    {
        return myId;
    }

    /** Returns this fish's environment.
     *  @return        the environment in which this fish lives
     **/
    public Environment environment()
    {
        return theEnv;
    }

    /** Returns this fish's color.
     *  @return        the color of this fish
     **/
    public Color color()
    {
        return myColor;
    }

    /** Returns this fish's location.
     *  @return        the location of this fish in the environment
     **/
    public Location location()
    {
        return myLoc;
    }

    /** Returns this fish's direction.
     *  @return        the direction in which this fish is facing
     **/
    public Direction direction()
    {
        return myDir;
    }

    /** Checks whether this fish is in an environment.
     *  @return  <code>true</code> if the fish is in the environment
     *           (and at the correct location); <code>false</code> otherwise
     **/
    public boolean isInEnv()
    {
        return environment().objectAt(location()) == this;
    }

    /** Returns a string representing key information about this fish.
     *  @return  a string indicating the fish's ID, location, and direction
     **/
    public String toString()
    {
        return id() + location().toString() + direction().toString();
    }


  // modifier method

    /** Acts for one step in the simulation.
     **/
    public void act()
    {       
        // Make sure fish is alive and well in the environment -- fish
        // that have been removed from the environment shouldn't act.
        if ( isInEnv() ) 
            move(); 
    }


  // internal helper methods

    /** Moves this fish in its environment.
     **/
    protected void move()
    {
        // Find a location to move to.
        Debug.print("Fish " + toString() + " attempting to move.  ");
        Location nextLoc = nextLocation();

        // If the next location is different, move there.
        if ( ! nextLoc.equals(location()) )
        {
            // Move to new location.
            Location oldLoc = location();
            changeLocation(nextLoc);

            // Update direction in case fish had to turn to move.
            Direction newDir = environment().getDirection(oldLoc, nextLoc);
            changeDirection(newDir);
            Debug.println("  Moves to " + location() + direction());
        }
        else
            Debug.println("  Does not move.");
    }

    /** Finds this fish's next location.
     *  A fish may move to any empty adjacent locations except the one
     *  behind it (fish do not move backwards).  If this fish cannot
     *  move, <code>nextLocation</code> returns its current location.
     *  @return    the next location for this fish
     **/
    protected Location nextLocation()
    {
        // Get list of neighboring empty locations.
        ArrayList emptyNbrs = emptyNeighbors();

        // Remove the location behind, since fish do not move backwards.
        Direction oppositeDir = direction().reverse();
        Location locationBehind = environment().getNeighbor(location(),
                                                            oppositeDir);
        emptyNbrs.remove(locationBehind);
        Debug.print("Possible new locations are: " + emptyNbrs.toString());

        // If there are no valid empty neighboring locations, then we're done.
        if ( emptyNbrs.size() == 0 )
            return location();

        // Return a randomly chosen neighboring empty location.
        Random randNumGen = RandNumGenerator.getInstance();
        int randNum = randNumGen.nextInt(emptyNbrs.size());
        return (Location) emptyNbrs.get(randNum);
    }

    /** Finds empty locations adjacent to this fish.
     *  @return    an ArrayList containing neighboring empty locations
     **/
    protected ArrayList emptyNeighbors()
    {
        // Get all the neighbors of this fish, empty or not.
        ArrayList nbrs = environment().neighborsOf(location());

        // Figure out which neighbors are empty and add those to a new list.
        ArrayList emptyNbrs = new ArrayList();
        for ( int index = 0; index < nbrs.size(); index++ )
        {
            Location loc = (Location) nbrs.get(index);
            if ( environment().isEmpty(loc) )
                emptyNbrs.add(loc);
        }

        return emptyNbrs;
    }

    /** Modifies this fish's location and notifies the environment.
     *  @param  newLoc    new location value
     **/
    protected void changeLocation(Location newLoc)
    {
        Location oldLoc = location();
        if ( ! newLoc.equals(oldLoc) ) //if needed for Greenfoot
        {
           // Change location and notify the environment.
           myLoc = newLoc;
           environment().recordMove(this, oldLoc);
        }
        // object is again at location myLoc in environment
    }

    /**
     * For Greenfoot.
     * <p>
     * 
     * Overrides setLocation so that setting the location from greenfoot 
     * changes the location in the environment.
     * 
     */
    public void setLocation(int x, int y) {
        if (environment() != null && ! (getX() == x && getY() == y)) {
            // Check if there are any objects at the new location. 
            Object o = getOneObjectAtOffset(x - getX(), y - getY(), Locatable.class);
            if(o == null) {
                // In MBCS you can only put fish in an empty cell
                super.setLocation(x, y);
                changeLocation(new Location(y, x));
            }
        } else {                      
            super.setLocation(x, y);
        }
    }
    
    /** Modifies this fish's direction.
     *  @param  newDir    new direction value
     **/
    protected void changeDirection(Direction newDir)
    {
        // Change direction.
        myDir = newDir;
        setRotation(newDir.inDegrees() - 90);  // Needed for Greenfoot 
            // Need -90 since 0 is NORTH in the MBS, but 0 is EAST in Greenfoot 
    }

}
