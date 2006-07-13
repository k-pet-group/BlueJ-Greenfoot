import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.util.*;

/**
 * The maze carver will create a perfect maze using a backtracking algorithm.
 */
public class MazeCarver extends Actor
{
    /** Stack of cells that need to be backtracked through */
    private Stack backtrack = new Stack();
    
    /** Current location in the maze */
    private Cell currentCell;
    
    /**
     * Class to represent a cell in the maze. A cell in the maze is not the 
     * same as a cell in Greenfoot. Sinze half of the maze will be walls, 
     * the number of cells in the maze will be only half of the cells in 
     * greenfoot.
     */
    private class Cell {
        private int x;
        private int y;
        
        /**
         * Create a new cell with given maze-coordinates.
         */
        public Cell(int x, int y)  {
            this.x = x;
            this.y = y;
        }
        
        /**
         * Create a new cell at a location relative to another cell.
         */
        public Cell(Cell cell, int dx, int dy) {
            this(cell.x + dx,cell.y + dy);
        }
        
        /**
         * Get the Greenfoot x-coordinate of this cell.
         */
        public int getX() {
            return x*2 + 1;
        }               
        
        /**
         * Get the Greenfoot y-coordinate of this cell.
         */
        public int getY() {
            return y*2 + 1;
        }
    }

    public MazeCarver()
    {       
        //getImage().scale(Maze.getWallSize(), Maze.getWallSize());
    }

    /**
     * Initialise the maze carver.
     */
    protected void addedToWorld(World w) {        
        currentCell = new Cell(getX()/2,getY()/2);
        setVisited();
    }
    
    /**
     * Carve the maze
     */
    public void act()
    {
        Cell nextCell = getNonVisitedNeighbour();
        if( nextCell != null) {
            backtrack.push(currentCell);
            knockDownWall(currentCell, nextCell);
            goTo(nextCell);            
        } 
        else if (backtrack.isEmpty()) {   
            getWorld().removeObject(this);
        }
        else {
            goTo((Cell) backtrack.pop());
        }
    }

    
    /**
     * Mark the current location as visited.
     */
    private void setVisited() {
        List l = getObjectsAtOffset(0,0,Wall.class);
        if(!l.isEmpty()) {
            getWorld().removeObjects(l);
        }
    }
    
    /**
     * Move to the given cell.
     */
    public void goTo(Cell cell) {        
        currentCell = cell;
        setLocation(cell.getX(), cell.getY());
        setVisited();
    }
    
    /**
     * Remove the wall between the two cells.
     */
    private void knockDownWall(Cell from, Cell to) {
        int x = (from.getX() + to.getX())/2;
        int y = (from.getY() + to.getY())/2;     
        List l = getWorld().getObjectsAt(x, y, Wall.class);
        if(! l.isEmpty()) {            
           Actor a = (Actor) l.get(0);
           getWorld().removeObject(a);
       }
    }
    
    
    /**
     * Get a random neighbouring cell that have not yet been visited. Or null 
     * if every neighour have been visited.
     */
    private Cell getNonVisitedNeighbour() {
        List neighbours = new ArrayList();
        maybeAddNeighbour( 0,-1, neighbours);
        maybeAddNeighbour( 0, 1, neighbours);
        maybeAddNeighbour(-1, 0, neighbours);
        maybeAddNeighbour( 1, 0, neighbours);    
        if(neighbours.size() > 0) {
            int pick = Greenfoot.getRandomNumber(neighbours.size());
            return (Cell) neighbours.get(pick);
        }
        else {
            return null;
        }
    }
    
    /**
     * Add neighbour at given offset if it has not been visited.
     */
    private void maybeAddNeighbour(int dx, int dy, List neighbours) {
        Object o = getOneObjectAtOffset(dx*2 ,dy*2, Wall.class);
        int x = getX() + dx*2;
        int y = getY() + dy*2;
        
        if(o != null && withinBounds(x,y)) {
            neighbours.add(new Cell(currentCell, dx, dy));
        }
    }
    
    /**
     * Check whether the greenfoot coordinate is within bounds.
     */
    private boolean withinBounds(int x, int y) {
        if(x>=getWorld().getWidth()-1) {
            return false;
        }
        if(y>=getWorld().getHeight()-1) {
            return false;
        }
        if(x<1) {
            return false;
        }
        if(y<1) {
            return false;
        }
        return true;
    }
}