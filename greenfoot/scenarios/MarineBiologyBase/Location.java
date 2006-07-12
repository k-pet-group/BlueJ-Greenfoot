// AP(r) Computer Science Marine Biology Simulation:
// The Location class is copyright(c) 2002 College Entrance
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

/**
 *  AP&reg; Computer Science Marine Biology Simulation:<br>
 *  A <code>Location</code> object represents the row and column of a
 *  location in a two-dimensional grid.
 *
 *  <p>
 *  The <code>Location</code> class is
 *  copyright&copy; 2002 College Entrance Examination Board
 *  (www.collegeboard.com).
 *
 *  @author Alyce Brady
 *  @author Chris Nevison
 *  @author APCS Development Committee
 *  @version 1 July 2002
 **/

public class Location implements Comparable
{
    // Instance Variables: Encapsulated data for each Location object
    private int myRow;            // row location in grid
    private int myCol;            // column location in grid

    /** Constructs a <code>Location</code> object.
     *  @param row    location's row
     *  @param col    location's column
     **/
    public Location(int row, int col)
    {
        myRow = row;
        myCol = col;
    }

  // accessor methods

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

    /** Compares this location to <code>other</code> for ordering.
     *  Returns a negative integer, zero, or a positive integer as this
     *  location is less than, equal to, or greater than <code>other</code>.
     *  Locations are ordered in row-major order.
     *  (Precondition: <code>other</code> is a <code>Location</code> object.)
     *  @param other    the other location to test
     *  @return     a negative integer if this location is less than
     *              <code>other</code>, zero if the two locations are equal,
     *              or a positive integer if this location is greater than
     *              <code>other</code>
     **/
    public int compareTo(Object other)
    {
        Location otherLoc = (Location) other;
        if ( equals(other) )
            return 0;
        if ( row() == otherLoc.row() )
            return col() - otherLoc.col();
        return row() - otherLoc.row();
    }

    /** Represents this location as a string.
     *  @return        a string indicating the row and column of the
     *                 location in (row, col) format
     **/
    public String toString()
    {
        return "(" + row() + ", " + col() + ")";
    }

}
