import greenfoot.*;  // (World, Actor, GreenfootImage, and Greenfoot)
import java.util.*;

/**
 * A thing that runs around randomly in a maze.
 * 
 * @author Poul Henriksen
 */
public class Agent extends Actor
{
    private static final int EAST = 0;
    private static final int WEST = 1;
    private static final int NORTH = 2;
    private static final int SOUTH = 3;

    private int direction;
    

    public Agent()
    {
        setDirection(EAST);
    }

    public void act() {
        if(canMove()) {
            if(Greenfoot.getRandomNumber(4) == 1) {
                turnRandom();
            }
            else {
                move();
            }
        }
        else {
            turnRandom();
        }
    }
    
    /**
     * Turn either left or right.
     */
    private void turnRandom() {        
            if(Greenfoot.getRandomNumber(2) == 1) {
                turnLeft();
            } else {
                turnRight();
            }
    }
    
    /**
     * Move one cell forward in the current direction.
     */
    public void move()
    {
        if (!canMove()) {
            return;
        }
        switch(direction) {
            case SOUTH :
                setLocation(getX(), getY() + 1);
                break;
            case EAST :
                setLocation(getX() + 1, getY());
                break;
            case NORTH :
                setLocation(getX(), getY() - 1);
                break;
            case WEST :
                setLocation(getX() - 1, getY());
                break;
        }
    }
    
    
      /**
     * Test if we can move forward. Return true if we can, false otherwise.
     */
    public boolean canMove()
    {
        World myWorld = getWorld();
        int x = getX();
        int y = getY();
        switch(direction) {
            case SOUTH :
                y++;
                break;
            case EAST :
                x++;
                break;
            case NORTH :
                y--;
                break;
            case WEST :
                x--;
                break;
        }
        
        // test for wall
        List walls = myWorld.getObjectsAt(x, y, null);
        if( ! walls.isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    
    /**
     * Turns towards the right.
     */
    public void turnRight() {
        turnLeft();
        turnLeft();
        turnLeft();
    }
    
    /**
     * Turns towards the left.
     */
    public void turnLeft()
    {
        switch(direction) {
            case SOUTH :
                setDirection(EAST);
                break;
            case EAST :
                setDirection(NORTH);
                break;
            case NORTH :
                setDirection(WEST);
                break;
            case WEST :
                setDirection(SOUTH);
                break;
        }
    }
    
    /**
     * Sets the direction we're facing.
     */
    public void setDirection(int direction)
    {
        this.direction = direction;
        switch(direction) {
            case SOUTH :
                setRotation(90);
                break;
            case EAST :
                setRotation(0);
                break;
            case NORTH :
                setRotation(270);
                break;
            case WEST :
                setRotation(180);
                break;
            default :
                break;
        }
    }


    
   

}