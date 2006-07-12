// AP(r) Computer Science Marine Biology Simulation:
// The SquareEnvironment class is copyright(c) 2002 College Entrance
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

import java.util.ArrayList;
import java.util.Random;
import greenfoot.World;            //Needed for Greenfoot
import greenfoot.GreenfootImage;   //Needed for Greenfoot
import java.awt.Color;             //Needed for Greenfoot

/**
 *  AP&reg; Computer Science Marine Biology Simulation:<br>
 *  <code>SquareEnvironment</code> is an abstract class that implements
 *  only the navigational methods in the <code>Environment</code> interface.
 *  It considers the cells in the environment to be square, with sides
 *  to the north, south, east, and west, and navigates accordingly.
 *
 *  <p>
 *  The <code>SquareEnvironment</code> class is
 *  copyright&copy; 2002 College Entrance Examination Board
 *  (www.collegeboard.com).
 *
 *  @author Alyce Brady
 *  @author Chris Nevison
 *  @author Julie Zelenski
 *  @version 1 July 2002
 *  @see Direction
 *  @see Location
 **/

public abstract class SquareEnvironment extends World implements Environment
{
    // "extends World" above is needed for Greenfoot

    // The following 2 final static variables are needed for Greenfoot.
    private static final Color OCEAN_BLUE = new Color(75, 75, 255);
    private static final int CELL_SIZE = 30;

    private static final int NUM_SIDES_IN_SQUARE = 4;

    // Instance Variables: Encapsulated data for EACH square environment

    // If includeDiagonals is true, cells have 8 neighbors (neighbors on
    // 4 sides plus 4 diagonals); if false, cells have only 4 neighbors.
    private boolean includeDiagonals;


  // constructors

  // For Greenfoot, the following 2 constructors were replaced with the third
  // constructor below.

    /** Constructs a <code>SquareEnvironment</code> object in which cells
     *  have four adjacent neighbors -- those with which they share sides.
     *  These neighbors are in the four cardinal directions.
     **/
//    public SquareEnvironment()
//    {
//      includeDiagonals = false;
//    }
        
    /** Constructs a <code>SquareEnvironment</code> object in which cells
     *  have four or eight adjacent neighbors, depending on the value
     *  of the <code>includeDiagonalNeighbors</code> parameter.  If
     *  <code>includeDiagonalNeighbors</code> is <code>true</code>, cells
     *  have eight adjacent neighbors -- the immediately adjacent neighbors
     *  on all four sides and the four neighbors on the diagonals.
     *  If <code>includeDiagonalNeighbors</code> is <code>false</code>,
     *  cells have only the four neighbors they would have in an environment
     *  created with the default <code>SquareEnvironment</code> constructor.
     *  @param  includedDiagonalNeighbors    whether to include the four
     *                                       diagonal locations as neighbors
     **/
//    public SquareEnvironment(boolean includeDiagonalNeighbors)
//    {
//        includeDiagonals = includeDiagonalNeighbors;
//    }

    /** 
     *  For Greenfoot.
     *
     *  <p>
     *
     *  This constructor replaces the 2 constructors
     *  in the original Marine Biology Simulation. It constructs a
     *  <code>SquareEnvironment</code> object in which cells
     *  have four adjacent neighbors -- those with which they share sides.
     *  These neighbors are in the four cardinal directions. The object
     *  constructed has the number of rows and columns specified in the
     *  parameters.
     *  @param  numRows    Number of rows in the environment.
     *  @param  numCols    Number of columns in the environment.
     **/
    protected SquareEnvironment(int numRows, int numCols)
    {
        super(numCols, numRows, CELL_SIZE);
        includeDiagonals = false;
        drawBackground(numRows, numCols);
    }
    
    /** 
     *  For Greenfoot.
     *
     *  <p>
     *
     *  It draws a background for the environment, with a black
     *  rectangular grid of rows and columns, with the cell size
     *  specified in CELL_SIZE.
     **/
    private void drawBackground(int numRows, int numCols)
    {
        GreenfootImage bg = getBackground();
        bg.setColor(OCEAN_BLUE);
        bg.fill();
        bg.setColor(Color.BLACK);
        for(int i = 0; i < numCols; i++)   //draw vertical column lines
        {
            bg.drawLine(i * CELL_SIZE, 0, i * CELL_SIZE, CELL_SIZE * numRows);
        }
        for(int i = 0; i < numRows; i++)   //draw horizontal row lines
        {
            bg.drawLine(0, i * CELL_SIZE, CELL_SIZE * numCols, i * CELL_SIZE);
        }
    }

  // accessor methods for navigating around this environment

    /** Returns the number of sides around each cell.
     *  @return    the number of cell sides in this environment
     **/
    public int numCellSides()
    {
        return NUM_SIDES_IN_SQUARE;
    }
    
    /** Returns the number of adjacent neighbors around each cell.
     *  @return    the number of adjacent neighbors 
     **/
    public int numAdjacentNeighbors()
    {
        return (includeDiagonals ? NUM_SIDES_IN_SQUARE*2 : NUM_SIDES_IN_SQUARE);
    }

    /** Generates a random direction.  The direction returned by
     *  <code>randomDirection</code> reflects the direction from
     *  a cell in the environment to one of its adjacent neighbors.
     *  @return a direction
     **/
    public Direction randomDirection()
    {
        Random randNumGen = RandNumGenerator.getInstance();
        int randNum = randNumGen.nextInt(numAdjacentNeighbors());
        return new Direction(randNum * Direction.FULL_CIRCLE/numAdjacentNeighbors());
    }

    /** Returns the direction from one location to another.  If 
     *  <code>fromLoc</code> and <code>toLoc</code> are the same, 
     *  <code>getDirection</code> arbitrarily returns <code>Direction.NORTH</code>.  
     *  @param  fromLoc       starting location for search
     *  @param  toLoc         destination location
     *  @return direction from <code>fromLoc</code> to <code>toLoc</code>
     **/
    public Direction getDirection(Location fromLoc, Location toLoc)
    {
        if (fromLoc.equals(toLoc))
            return Direction.NORTH;
        int rowDifference = fromLoc.row() - toLoc.row(); // our coord system is upside down
        int colDifference = toLoc.col() - fromLoc.col();
        double inRads = Math.atan2(rowDifference, colDifference);
        double angle = 90 - Math.toDegrees(inRads); // convert to our sweep, North is 0
        Direction d = new Direction((int)angle);
        return d.roundedDir(numAdjacentNeighbors(), Direction.NORTH);
    }

    /** Returns the adjacent neighbor (whether valid or invalid) of a location
     *  in the specified direction.
     *  @param  fromLoc       starting location for search
     *  @param  compassDir    direction in which to look for adjacent neighbor
     *  @return neighbor of <code>fromLoc</code> in given direction
     *                        (whether valid or not)
     **/
    public Location getNeighbor(Location fromLoc, Direction compassDir)
    {
        Direction roundedDir = compassDir.roundedDir(numAdjacentNeighbors(),
                                                     Direction.NORTH);

        // Calculate neighboring location using sines and cosines.
        // First have to adjust because our 0 is North, not East.
        // The row change is the opposite of what is expected because
        // our row numbers increase as they go down, not up.
        int adjustedDegrees = 90 - roundedDir.inDegrees();
        double inRads = Math.toRadians(adjustedDegrees);
        int colDelta = (int)(Math.cos(inRads) * Math.sqrt(2));
        int rowDelta = -(int)(Math.sin(inRads) * Math.sqrt(2));
        return new Location(fromLoc.row() + rowDelta, fromLoc.col() + colDelta);
    }

    /** Returns the adjacent neighbors of a specified location.
     *  Only neighbors that are valid locations in the environment will be
     *  included.
     *  @param  ofLoc       location whose neighbors to get
     *  @return a list of locations that are neighbors of <code>ofLoc</code>
     **/
    public ArrayList neighborsOf(Location ofLoc)
    {
        ArrayList nbrs = new ArrayList();

        Direction d = Direction.NORTH;
        for (int i = 0; i < numAdjacentNeighbors(); i++)
        {
            Location neighbor = getNeighbor(ofLoc, d);
            if ( isValid(neighbor) )
                nbrs.add(neighbor);
            d = d.toRight(Direction.FULL_CIRCLE/numAdjacentNeighbors());
        } 
        return nbrs;
    }

}
