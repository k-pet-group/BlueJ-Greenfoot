// This class is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation.
//
// This class is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

import java.awt.Color;  
import java.util.List; 
import java.util.ArrayList;   
import java.util.Iterator;
import greenfoot.GreenfootImage;
import greenfoot.Greenfoot;
import greenfoot.World; 
import greenfoot.Actor; 

/**
 *  The <code>BoundedEnv</code> class models a bounded, two-dimensional,
 *  grid-like environment. The behavior of this class is similar to
 *  the same class in the AP Marine Biology Simulation, but this class
 *  has been written expressely to run the simulation in Greenfoot.
 *  <p>
 *
 *  @author Cecilia Vargas
 *  @version 28 July 2006
 **/

public class BoundedEnv extends World
{
    private static final int   ROWS       = 15;  
    private static final int   COLS       = 15;  
    private static final int   CELL_SIZE  = 30;
    private static final Color OCEAN_BLUE = new Color(75, 75, 255);

    /** 
     *  Constructs an empty world.
     **/
    public BoundedEnv()   
    {
        super(ROWS, COLS, CELL_SIZE);
        drawBackground();
    }

    /** 
     *  Draws a background for the environment, with a black
     *  rectangular grid of rows and columns.
     **/
    private void drawBackground()
    {
        GreenfootImage bg = getBackground();
        bg.setColor(OCEAN_BLUE);
        bg.fill();
        bg.setColor(Color.BLACK);
        for(int i = 0; i < COLS; i++) {   //draw vertical column lines
            bg.drawLine(i * CELL_SIZE, 0, i * CELL_SIZE, CELL_SIZE * ROWS);
        }
        for(int i = 0; i < ROWS; i++) { //draw horizontal row lines
            bg.drawLine(0, i * CELL_SIZE, CELL_SIZE * COLS, i * CELL_SIZE);
        }
    }

    /** 
     * Verifies whether a cell exists in this world.
     *  @param  x    column of cell to check
     *  @param  y    row of cell to check
     *  @return <code>true</code> if cell is valid;
     *          <code>false</code> otherwise
     **/
    public boolean isValid(int x, int y)
    {
        return ( 0 <= y && y < getHeight() ) &&
               ( 0 <= x && x < getWidth()  );
    }

    /** 
     * Determines whether a specific cell in the world is empty.
     *  @param  x    column of cell to check
     *  @param  y    row of cell to check
     *  @return     <code>true</code> if cell is valid and empty;
     *              <code>false</code> otherwise
     **/
    public boolean isEmpty(int x, int y)
    {
        if ( ! isValid( x, y) ) {
           return false;
        }

        List list = getObjectsAt(x, y, null);
        return list.size() == 0;
    }

    /** 
     * Returns a list of directions of the (up to 4) empty cells
     *  that are neighbors of the specified cell.
     *  @param  x    column of cell to check
     *  @param  y    row of cell to check
     *  @return      a list of directions of empty neighboring cells.
     **/              
    public List emptyNeighboringDirections(int x, int y)
    {
        List result = new ArrayList();

        if ( isEmpty(x + 1, y) ) {
           result.add("East");
        }
        if ( isEmpty(x - 1, y) ) {
           result.add("West");
        }
        if ( isEmpty(x, y + 1) ) {
           result.add("South");
        }
        if ( isEmpty(x, y - 1) ) {
           result.add("North");
        }

        return result;
    }

    /** 
     * Creates a single string representing all the objects in this
     *  environment, in no particular order.
     *  @return    a string of all the objects in this environment
     **/
    public String toString()
    {
        List list = getObjects(null);      
        return "World contains " + list.size() + " objects: " + list;
    }
    
    /**
     * Overridden to disallow adding an object to an occupied cell.
     * @param obj      Object to add to the world.
     * @param x        Column in which to add the object.
     * @param y        Row in which to add the object.
     */
    public void addObject(Actor obj, int x, int y)
    {
        List occupants = getObjectsAt(x, y, null);
        
        //Remove objects that do not have their location at (x,y)
        Iterator iter = occupants.iterator();
        while(iter.hasNext()) {
            Actor a = (Actor) iter.next();
            if( ! (a.getX() == x && a.getY() == y) ) {
                iter.remove();
            }
        }
        
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
     * Populates the world with randomly colored fish in random locations.
     */
    public void populate()
    {
        int rows = getHeight();
        int cols = getWidth();
        
        for (int i = 1; i < rows; i++) {
           int r = Greenfoot.getRandomNumber(rows);
           int c = Greenfoot.getRandomNumber(cols);
           if ( isEmpty(c,r) ) {
              addObject(new Fish(), c, r);
           }
        }
    } 
}