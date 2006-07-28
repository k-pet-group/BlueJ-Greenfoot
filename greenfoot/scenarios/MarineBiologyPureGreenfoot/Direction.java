// AP(r) Computer Science Marine Biology Simulation:
// The Direction class is copyright(c) 2002 College Entrance
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

import greenfoot.Greenfoot;             

/**
 *  AP&reg; Computer Science Marine Biology Simulation:<br>
 *  The <code>Direction</code> class encapsulates a compass
 *  direction such as North, East, South, West. North is 0 degrees,
 *  East is 90, South is 180, and West is 270.
 *  <p>
 *  This class was simplified by Greenfoot developers
 *  to run the MBCS in Greenfoot.
 *  <p>
 *
 *  @author Alyce Brady
 *  @author Chris Nevison
 *  @author Julie Zelenski
 *  @author APCS Development Committee
 *  @author Cecilia Vargas (Simplified to run MBCS in Greenfoot)
 *  @version 1 July 2002
 **/

public class Direction
{
    public static final Direction NORTH = new Direction(0);
    public static final Direction EAST  = new Direction(90);
    public static final Direction SOUTH = new Direction(180);
    public static final Direction WEST  = new Direction(270);

    public static final int FULL_CIRCLE = 360;  

    public static final int NUM_ADJACENT_NEIGHBORS = 4;

    private static final String[] dirNames = {"North", "East","South", "West"};

    private int dirInDegrees;   // represents compass direction in degrees,
                                // with 0 degrees as North, 90 degrees as East, etc.

    /** Constructs a default <code>Direction</code> object facing North.
     **/
    public Direction()
    {
        dirInDegrees = 0;   
    }

    /** Constructs a <code>Direction</code> object with the specified degrees.
     *  North is 0, East is 90, South is 180, West is 270.
     *  @param degrees   compass direction in degrees
     **/
    public Direction(int degrees)
    {
        dirInDegrees = degrees % FULL_CIRCLE;
        if ( dirInDegrees < 0 )
            dirInDegrees += FULL_CIRCLE;
    }

    /** Constructs a <code>Direction</code> object.
     *  @param str    compass direction specified as a string, e.g. "North"
     *  @throws IllegalArgumentException if string doesn't match a known direction name 
     **/
    public Direction(String str)
    {
        int regionWidth = FULL_CIRCLE / dirNames.length;
        
	    for ( int k = 0; k < dirNames.length; k++ )
	    {
            if ( str.equalsIgnoreCase(dirNames[k]) )
            {
                 dirInDegrees = k * regionWidth;
                 return;
            }
        }
        throw new IllegalArgumentException("Illegal direction specified: \"" +
                                str + "\"");
    }

    /** Returns this direction value in degrees.
     *  @return  the value of this <code>Direction</code> object in degrees
     **/
    public int inDegrees()
    {
        return dirInDegrees;
    }

    /** Indicates whether some other <code>Direction</code> object
     *  is "equal to" this one.
     *  @param other   the other position to test
     *  @return        <code>true</code> if <code>other</code>
     *                 represents the same direction;
     *                 <code>false</code> otherwise
     **/
    public boolean equals(Object other)
    {
        if ( ! (other instanceof Direction) )
            return false;

        Direction d = (Direction) other;
        return inDegrees() == d.inDegrees();
    }

    /** Generates a hash code for this direction
     *  @return     a hash code for a <code>Direction</code> object
     **/
    public int hashCode()
    {
       return inDegrees();
    }

    /** Returns the direction that is the reverse of this
     *  <code>Direction</code> object.
     *  @return     the reverse direction
     **/
    public Direction reverse()
    {
       return new Direction(dirInDegrees + (FULL_CIRCLE / 2));
    }

    /** Represents this direction as a string.
     *  @return        a string indicating the direction
     **/
    public String toString()
    {
        // If the direction is one of the compass points for which we have
        // a name, provide it; otherwise report in degrees. 
        int regionWidth = FULL_CIRCLE / dirNames.length;
        if (dirInDegrees % regionWidth == 0)
            return dirNames[dirInDegrees / regionWidth];
        else
            return dirInDegrees + " degrees";
    }

    /** Returns a random direction reflecting the direction from  
     *  a cell in the environment to one of its adjacent neighbors.
     *  @return    a direction
     **/
    public static Direction randomDirection()
    {
        int randNum = Greenfoot.getRandomNumber(NUM_ADJACENT_NEIGHBORS );
        return new Direction(randNum * FULL_CIRCLE / NUM_ADJACENT_NEIGHBORS );
    }

    /** Translates the direction to Greenfoot degrees, where 0 is East.
     *  In the Marine Biology Simulation, North is 0.
     *  @return       the number of degrees in Greenfoot
     **/
    public int inGreenfootDegrees()
    {
       if ( dirInDegrees < 90 ) 
          return dirInDegrees + 270;
       else
          return dirInDegrees - 90;
    }
}